package com.example.geoagri.chat.input;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Voice input manager with runtime permission flow, speech recognizer lifecycle handling,
 * fallback recognizer intent support, and retry logic.
 */
public final class VoiceInputManager {
    private static final String TAG = "MIC_DEBUG";
    private static final long RETRY_DELAY_MS = 350L;
    private static final long LISTENING_HARD_TIMEOUT_MS = 10000L;
    private static final long FORCE_CANCEL_AFTER_STOP_MS = 1200L;
    private static final int MAX_AUTO_RETRIES = 3;
    private static final int ERROR_LANGUAGE_NOT_SUPPORTED = 12;
    private static final int ERROR_LANGUAGE_UNAVAILABLE = 13;
    private static final int MODE_DEVICE_LANGUAGE = 0;
    private static final int MODE_ENGLISH_US = 1;
    private static final int MODE_RECOGNIZER_DEFAULT = 2;
    private static final int MAX_MODE = MODE_RECOGNIZER_DEFAULT;

    public interface Callback {
        void onListeningStateChanged(boolean listening);

        void onTextRecognized(@NonNull String text);

        void onError(@NonNull String message);
    }

    private final Fragment fragment;
    private final Callback callback;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final MicPermissionHelper micPermissionHelper;
    private SpeechRecognizer speechRecognizer;
    private RecognitionListener recognitionListener;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private boolean listening;
    private boolean released;
    private boolean waitingForRetry;
    private boolean forceOffline;
    private boolean sessionInFlight;
    private int recognitionMode = MODE_DEVICE_LANGUAGE;
    private int consecutiveNoMatchCount;
    private int retryCount;
    private long activeSessionId;
    private final Runnable retryRunnable = this::startListeningInternalOnMainThread;

    public VoiceInputManager(@NonNull Fragment fragment, @NonNull Callback callback) {
        this.fragment = fragment;
        this.callback = callback;

        this.micPermissionHelper = new MicPermissionHelper(
                fragment,
                granted -> runOnMainThread(() -> {
                    if (released) {
                        return;
                    }
                    if (granted) {
                        Log.d(TAG, "Runtime permission granted. Starting listener.");
                        startListeningInternalOnMainThread();
                    } else {
                        Log.e(TAG, "Runtime permission denied for RECORD_AUDIO");
                        callback.onError("Microphone permission denied.");
                    }
                })
        );
    }

    public void startListening() {
        runOnMainThread(this::startListeningOnMainThread);
    }

    private void startListeningOnMainThread() {
        if (released) {
            return;
        }
        Context context = fragment.getContext();
        if (context == null) {
            callback.onError("Context not available for voice input.");
            return;
        }

        if (!micPermissionHelper.hasRecordAudioPermission()) {
            Log.d(TAG, "Requesting RECORD_AUDIO runtime permission.");
            micPermissionHelper.requestRecordAudioPermission();
            return;
        }

        waitingForRetry = false;
        forceOffline = !isNetworkConnected(context);
        recognitionMode = MODE_DEVICE_LANGUAGE;
        consecutiveNoMatchCount = 0;
        retryCount = 0;
        mainHandler.removeCallbacks(retryRunnable);

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            callback.onError("Speech recognizer service unavailable on this device.");
            return;
        }

        startListeningInternalOnMainThread();
    }

    private void startListeningInternalOnMainThread() {
        if (released) {
            return;
        }
        Context context = fragment.getContext();
        if (context == null) {
            callback.onError("Context not available for voice input.");
            return;
        }

        if (!micPermissionHelper.hasRecordAudioPermission()) {
            Log.d(TAG, "Permission missing while starting internal listener. Re-requesting.");
            micPermissionHelper.requestRecordAudioPermission();
            return;
        }

        if (speechRecognizer == null) {
            initSpeechRecognizer(context.getApplicationContext());
        }

        if (listening) {
            Log.d(TAG, "Cancelling stale listening session before restart.");
            speechRecognizer.cancel();
            listening = false;
            callback.onListeningStateChanged(false);
        }

        requestAudioFocus(context);

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now");
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1500L);
        applyRecognitionMode(intent);

        try {
            activeSessionId++;
            final long sessionId = activeSessionId;
            sessionInFlight = true;
            listening = true;
            callback.onListeningStateChanged(true);
            Log.d(TAG, "startListening() invoked mode=" + recognitionMode + ", forceOffline=" + forceOffline);
            speechRecognizer.startListening(intent);
            scheduleListeningTimeout(sessionId);
        } catch (Exception e) {
            Log.e(TAG, "startListening failed: " + e.getMessage(), e);
            finishSession();
            abandonAudioFocus();
            resetRecognizerAndRetry();
        }
    }

    private void initSpeechRecognizer(@NonNull Context context) {
        destroySpeechRecognizer();
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        recognitionListener = new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Log.d(TAG, "onReadyForSpeech");
                listening = true;
                callback.onListeningStateChanged(true);
            }

            @Override
            public void onBeginningOfSpeech() {
                Log.d(TAG, "onBeginningOfSpeech");
                listening = true;
                callback.onListeningStateChanged(true);
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                Log.d(TAG, "onRmsChanged: " + rmsdB);
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                Log.d(TAG, "onBufferReceived: bytes=" + (buffer == null ? 0 : buffer.length));
            }

            @Override
            public void onEndOfSpeech() {
                Log.d(TAG, "onEndOfSpeech");
                finishSession();
            }

            @Override
            public void onError(int error) {
                Log.e(TAG, "onError: code=" + error + " (" + speechErrorToString(error) + ")");
                finishSession();
                abandonAudioFocus();
                handleRecognizerError(error);
            }

            @Override
            public void onResults(Bundle results) {
                Log.d(TAG, "onResults");
                finishSession();
                abandonAudioFocus();
                String parsed = parseTopResult(results);
                if (parsed.isEmpty()) {
                    scheduleRetryListening();
                    return;
                }
                retryCount = 0;
                consecutiveNoMatchCount = 0;
                recognitionMode = MODE_DEVICE_LANGUAGE;
                callback.onTextRecognized(parsed);
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                String partial = parseTopResult(partialResults);
                if (!partial.isEmpty()) {
                    Log.d(TAG, "onPartialResults: " + partial);
                } else {
                    Log.d(TAG, "onPartialResults: <empty>");
                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
                Log.d(TAG, "onEvent: type=" + eventType);
            }
        };
        speechRecognizer.setRecognitionListener(recognitionListener);
        Log.d(TAG, "SpeechRecognizer initialized and listener attached.");
    }

    private void handleRecognizerError(int error) {
        if (released) {
            return;
        }
        if (error == SpeechRecognizer.ERROR_NO_MATCH) {
            consecutiveNoMatchCount++;
            if (consecutiveNoMatchCount >= 2 && advanceRecognitionMode()) {
                scheduleRetryListening();
                return;
            }
            scheduleRetryListening();
            return;
        }
        if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
            scheduleRetryListening();
            return;
        }
        if (error == SpeechRecognizer.ERROR_CLIENT) {
            resetRecognizerAndRetry();
            return;
        }
        if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
            micPermissionHelper.requestRecordAudioPermission();
            return;
        }
        if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_AUDIO) {
            resetRecognizerAndRetry();
            return;
        }
        if (error == SpeechRecognizer.ERROR_NETWORK || error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT) {
            forceOffline = true;
            if (advanceRecognitionMode()) {
                scheduleRetryListening();
                return;
            }
            scheduleRetryListening();
            return;
        }
        if (error == ERROR_LANGUAGE_NOT_SUPPORTED || error == ERROR_LANGUAGE_UNAVAILABLE) {
            if (advanceRecognitionMode()) {
                scheduleRetryListening();
                return;
            }
            callback.onError("Speech language unavailable on this device.");
            return;
        }

        callback.onError("Speech recognition failed. Code: " + error);
    }

    private void resetRecognizerAndRetry() {
        Context context = fragment.getContext();
        if (context == null) {
            callback.onError("Context not available for voice input.");
            return;
        }
        Log.w(TAG, "Resetting SpeechRecognizer after client/audio conflict.");
        finishSession();
        destroySpeechRecognizer();
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            initSpeechRecognizer(context.getApplicationContext());
            scheduleRetryListening();
            return;
        }
        callback.onError("Speech recognizer service unavailable on this device.");
    }

    private void scheduleRetryListening() {
        if (released) {
            return;
        }
        if (waitingForRetry) {
            return;
        }
        if (retryCount >= MAX_AUTO_RETRIES) {
            finishSession();
            callback.onError("Speech service is not responding on this device. Please verify emulator Google Speech Services or test on a real device.");
            return;
        }
        retryCount++;
        waitingForRetry = true;
        mainHandler.removeCallbacks(retryRunnable);
        mainHandler.postDelayed(() -> {
            waitingForRetry = false;
            startListeningInternalOnMainThread();
        }, RETRY_DELAY_MS);
        Log.d(TAG, "Retry scheduled in " + RETRY_DELAY_MS + "ms");
    }

    private void applyRecognitionMode(@NonNull Intent intent) {
        String deviceTag = Locale.getDefault().toLanguageTag();
        if (recognitionMode == MODE_DEVICE_LANGUAGE) {
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, deviceTag);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, deviceTag);
            intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, forceOffline);
            return;
        }
        if (recognitionMode == MODE_ENGLISH_US) {
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US");
            intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false);
            return;
        }

        intent.removeExtra(RecognizerIntent.EXTRA_LANGUAGE);
        intent.removeExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE);
        intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false);
    }

    private boolean advanceRecognitionMode() {
        if (recognitionMode < MAX_MODE) {
            recognitionMode++;
            Log.w(TAG, "Advancing recognition mode to " + recognitionMode);
            return true;
        }
        return false;
    }

    @NonNull
    private String parseTopResult(Bundle results) {
        if (results == null) {
            return "";
        }
        ArrayList<String> texts = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (texts == null || texts.isEmpty()) {
            return "";
        }
        String top = texts.get(0);
        return top == null ? "" : top.trim();
    }

    private boolean isNetworkConnected(@NonNull Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }
        Network active = cm.getActiveNetwork();
        if (active == null) {
            return false;
        }
        NetworkCapabilities capabilities = cm.getNetworkCapabilities(active);
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private void requestAudioFocus(@NonNull Context context) {
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener(focusChange ->
                            Log.d(TAG, "Audio focus change: " + focusChange))
                    .build();
            int result = audioManager.requestAudioFocus(audioFocusRequest);
            Log.d(TAG, "Audio focus request result=" + result);
        } else {
            int result = audioManager.requestAudioFocus(
                    focusChange -> Log.d(TAG, "Audio focus change: " + focusChange),
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            );
            Log.d(TAG, "Audio focus request result=" + result);
        }
    }

    private void abandonAudioFocus() {
        if (audioManager == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest != null) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest);
            }
        } else {
            audioManager.abandonAudioFocus(null);
        }
    }

    private void destroySpeechRecognizer() {
        finishSession();
        if (speechRecognizer != null) {
            try {
                speechRecognizer.cancel();
            } catch (Exception ignored) {
            }
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        recognitionListener = null;
        listening = false;
    }

    private void runOnMainThread(@NonNull Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            mainHandler.post(runnable);
        }
    }

    private void scheduleListeningTimeout(long sessionId) {
        mainHandler.postDelayed(() -> {
            if (released || !sessionInFlight || sessionId != activeSessionId) {
                return;
            }
            Log.e(TAG, "Listening timeout reached for session=" + sessionId + ". Forcing stop.");
            try {
                if (speechRecognizer != null) {
                    speechRecognizer.stopListening();
                }
            } catch (Exception stopEx) {
                Log.e(TAG, "stopListening failed in timeout: " + stopEx.getMessage(), stopEx);
            }

            mainHandler.postDelayed(() -> {
                if (released || !sessionInFlight || sessionId != activeSessionId) {
                    return;
                }
                Log.e(TAG, "Forced cancel after timeout for session=" + sessionId);
                finishSession();
                abandonAudioFocus();
                try {
                    if (speechRecognizer != null) {
                        speechRecognizer.cancel();
                    }
                } catch (Exception cancelEx) {
                    Log.e(TAG, "cancel failed after timeout: " + cancelEx.getMessage(), cancelEx);
                }
                handleRecognizerError(SpeechRecognizer.ERROR_SPEECH_TIMEOUT);
            }, FORCE_CANCEL_AFTER_STOP_MS);
        }, LISTENING_HARD_TIMEOUT_MS);
    }

    private void finishSession() {
        sessionInFlight = false;
        listening = false;
        callback.onListeningStateChanged(false);
        activeSessionId++;
    }

    @NonNull
    private String speechErrorToString(int code) {
        switch (code) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "ERROR_AUDIO";
            case SpeechRecognizer.ERROR_CLIENT:
                return "ERROR_CLIENT";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "ERROR_INSUFFICIENT_PERMISSIONS";
            case SpeechRecognizer.ERROR_NETWORK:
                return "ERROR_NETWORK";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "ERROR_NETWORK_TIMEOUT";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "ERROR_NO_MATCH";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "ERROR_RECOGNIZER_BUSY";
            case SpeechRecognizer.ERROR_SERVER:
                return "ERROR_SERVER";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "ERROR_SPEECH_TIMEOUT";
            case ERROR_LANGUAGE_NOT_SUPPORTED:
                return "ERROR_LANGUAGE_NOT_SUPPORTED";
            case ERROR_LANGUAGE_UNAVAILABLE:
                return "ERROR_LANGUAGE_UNAVAILABLE";
            default:
                return "UNKNOWN(" + code + ")";
        }
    }

    public void release() {
        released = true;
        mainHandler.removeCallbacksAndMessages(null);
        callback.onListeningStateChanged(false);
        abandonAudioFocus();
        destroySpeechRecognizer();
    }
}

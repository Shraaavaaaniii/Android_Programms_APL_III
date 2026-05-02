package com.example.geoagri.chat;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.geoagri.R;
import com.example.geoagri.chat.input.CameraImageInputManager;
import com.example.geoagri.chat.input.ImageCompressionUtil;
import com.example.geoagri.chat.input.VoiceInputManager;
import com.example.geoagri.chat.vision.GeminiVisionApiService;
import com.example.geoagri.chatbot.GeminiApiService;
import com.example.geoagri.model.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatFragment extends Fragment {

    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("h:mm a", Locale.getDefault());
    private static final String ARG_NDVI = "ndvi";
    private static final String ARG_SOIL_MOISTURE = "soil_moisture";
    private static final String ARG_TEMPERATURE = "temperature";
    private static final String ARG_CROP = "crop";

    private final List<ChatMessage> messages = new ArrayList<>();
    private ChatAdapter chatAdapter;
    private RecyclerView chatRecycler;
    private EditText messageBox;
    private ImageButton sendBtn;
    private ImageButton attachBtn;
    private ImageButton micBtn;

    private GeminiApiService geminiApiService;
    private GeminiVisionApiService geminiVisionApiService;
    private VoiceInputManager voiceInputManager;
    private CameraImageInputManager cameraImageInputManager;

    private int typingIndex = -1;
    private int listeningIndex = -1;

    @Nullable
    private ImageCompressionUtil.EncodedImage activeImage;
    private boolean imageContextActive;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_chat, container, false);

        chatRecycler = root.findViewById(R.id.chatRecycler);
        messageBox = root.findViewById(R.id.messageBox);
        sendBtn = root.findViewById(R.id.sendBtn);

        geminiApiService = new GeminiApiService();
        geminiVisionApiService = new GeminiVisionApiService();

        chatAdapter = new ChatAdapter(messages);
        chatRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        chatRecycler.setAdapter(chatAdapter);

        initInputButtons();
        initVoiceAndImageManagers();

        if (messages.isEmpty()) {
            addBotMessage("Ask me by text, voice, or plant image for farming help.", false);
        }

        sendBtn.setOnClickListener(v -> sendCurrentMessage());

        if (attachBtn != null) {
            attachBtn.setOnClickListener(v -> cameraImageInputManager.openChooser());
        }

        if (micBtn != null) {
            micBtn.setOnClickListener(v -> voiceInputManager.startListening());
        }

        return root;
    }

    private void initInputButtons() {
        View parent = (View) messageBox.getParent();
        if (!(parent instanceof ViewGroup)) {
            return;
        }
        ViewGroup inputRow = (ViewGroup) parent;

        for (int i = 0; i < inputRow.getChildCount(); i++) {
            View child = inputRow.getChildAt(i);
            if (child instanceof ImageButton && child != sendBtn) {
                attachBtn = (ImageButton) child;
                break;
            }
        }

        if (attachBtn != null) {
            attachBtn.setImageResource(android.R.drawable.ic_menu_camera);
            attachBtn.setContentDescription("Camera");
        }

        micBtn = new ImageButton(requireContext());
        micBtn.setImageResource(android.R.drawable.ic_btn_speak_now);
        micBtn.setBackgroundResource(R.drawable.chat_plus_bg);
        micBtn.setContentDescription("Voice Input");
        int size = dp(38);
        ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(size, size);
        lp.setMarginEnd(dp(10));
        micBtn.setLayoutParams(lp);

        int sendIndex = inputRow.indexOfChild(sendBtn);
        if (sendIndex < 0) {
            inputRow.addView(micBtn);
        } else {
            inputRow.addView(micBtn, sendIndex);
        }
    }

    private void initVoiceAndImageManagers() {
        voiceInputManager = new VoiceInputManager(this, new VoiceInputManager.Callback() {
            @Override
            public void onListeningStateChanged(boolean listening) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    if (listening) {
                        addListeningIndicator();
                    } else {
                        removeListeningIndicator();
                    }
                });
            }

            @Override
            public void onTextRecognized(@NonNull String text) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    removeListeningIndicator();
                    messageBox.setText(text);
                    sendCurrentMessage();
                });
            }

            @Override
            public void onError(@NonNull String message) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    removeListeningIndicator();
                    addBotMessage(message, false);
                });
            }
        });

        cameraImageInputManager = new CameraImageInputManager(this, new CameraImageInputManager.Callback() {
            @Override
            public void onImageReady(@NonNull ImageCompressionUtil.EncodedImage image) {
                activeImage = image;
                imageContextActive = true;

                String currentText = messageBox.getText().toString().trim();
                if (currentText.isEmpty()) {
                    analyzeImageWithVision(null);
                } else {
                    sendCurrentMessage();
                }
            }

            @Override
            public void onError(@NonNull String message) {
                addBotMessage(message, false);
            }
        });
    }

    private void sendCurrentMessage() {
        final String userMessage = messageBox.getText().toString().trim();

        if (userMessage.isEmpty() && (activeImage == null || !imageContextActive)) {
            return;
        }

        if (!userMessage.isEmpty()) {
            addUserMessage(userMessage);
            messageBox.setText("");
        } else if (activeImage != null) {
            addUserMessage("[Plant image uploaded for analysis]");
        }

        if (activeImage != null && imageContextActive) {
            analyzeImageWithVision(userMessage.isEmpty() ? null : userMessage);
            imageContextActive = false;
            return;
        }

        requestGeminiTextReply(userMessage);
    }

    private void analyzeImageWithVision(@Nullable String question) {
        if (activeImage == null) {
            addBotMessage("No image available for analysis.", false);
            return;
        }

        setInputEnabled(false);
        addTypingIndicator();

        String q = question;
        if (q == null || q.trim().isEmpty()) {
            q = "Analyze this plant image and identify crop type and health issues.";
        }

        geminiVisionApiService.analyzeImage(activeImage.base64, activeImage.mimeType, q,
                new GeminiVisionApiService.VisionCallback() {
                    @Override
                    public void onSuccess(@NonNull String responseText) {
                        if (!isAdded()) {
                            return;
                        }
                        requireActivity().runOnUiThread(() -> {
                            removeTypingIndicator();
                            setInputEnabled(true);
                            addBotMessage(responseText, false);
                        });
                    }

                    @Override
                    public void onError(@NonNull String errorMessage) {
                        if (!isAdded()) {
                            return;
                        }
                        requireActivity().runOnUiThread(() -> {
                            removeTypingIndicator();
                            setInputEnabled(true);
                            addBotMessage("Image analysis failed. " + errorMessage, false);
                        });
                    }
                });
    }

    private void requestGeminiTextReply(@NonNull String userMessage) {
        setInputEnabled(false);
        addTypingIndicator();

        geminiApiService.askGemini(userMessage, buildFarmDataContext(), new GeminiApiService.GeminiCallback() {
            @Override
            public void onSuccess(@NonNull String aiResponse) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    removeTypingIndicator();
                    setInputEnabled(true);
                    addBotMessage(aiResponse, false);
                });
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    removeTypingIndicator();
                    setInputEnabled(true);
                    String userFacingError;
                    String lower = errorMessage.toLowerCase(Locale.getDefault());
                    if (lower.contains("dns") || lower.contains("unable to resolve host") || lower.contains("network")) {
                        userFacingError = "I couldn't reach Gemini service. Please check emulator internet/DNS and try again.";
                    } else if (lower.contains("api key") || lower.contains("permission") || lower.contains("forbidden")) {
                        userFacingError = "Gemini API key or API permission issue detected. Please verify key restrictions and enabled APIs.";
                    } else {
                        userFacingError = "I couldn't fetch a response right now. Please try again.";
                    }
                    addBotMessage(userFacingError + "\n\nDetails: " + errorMessage, false);
                    android.widget.Toast.makeText(requireContext(), errorMessage, android.widget.Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void addUserMessage(String text) {
        messages.add(new ChatMessage(text, true, currentTime(), false));
        if (chatAdapter != null) {
            chatAdapter.notifyItemInserted(messages.size() - 1);
        }
        scrollToLatest();
    }

    private void addBotMessage(String text, boolean showInsightCard) {
        messages.add(new ChatMessage(text, false, currentTime(), showInsightCard));
        if (chatAdapter != null) {
            chatAdapter.notifyItemInserted(messages.size() - 1);
        }
        scrollToLatest();
    }

    private void addTypingIndicator() {
        if (typingIndex >= 0) {
            return;
        }
        typingIndex = messages.size();
        messages.add(new ChatMessage("", false, currentTime(), false, true));
        if (chatAdapter != null) {
            chatAdapter.notifyItemInserted(typingIndex);
        }
        scrollToLatest();
    }

    private void removeTypingIndicator() {
        if (typingIndex < 0 || typingIndex >= messages.size()) {
            typingIndex = -1;
            return;
        }
        messages.remove(typingIndex);
        if (chatAdapter != null) {
            chatAdapter.notifyItemRemoved(typingIndex);
        }
        typingIndex = -1;
    }

    private void addListeningIndicator() {
        if (listeningIndex >= 0) {
            return;
        }
        listeningIndex = messages.size();
        messages.add(new ChatMessage("Listening...", false, currentTime(), false));
        if (chatAdapter != null) {
            chatAdapter.notifyItemInserted(listeningIndex);
        }
        scrollToLatest();
    }

    private void removeListeningIndicator() {
        if (listeningIndex < 0 || listeningIndex >= messages.size()) {
            listeningIndex = -1;
            return;
        }
        messages.remove(listeningIndex);
        if (chatAdapter != null) {
            chatAdapter.notifyItemRemoved(listeningIndex);
        }
        listeningIndex = -1;
    }

    private void scrollToLatest() {
        if (chatRecycler != null && chatAdapter != null && !messages.isEmpty()) {
            chatRecycler.scrollToPosition(messages.size() - 1);
        }
    }

    private void setInputEnabled(boolean enabled) {
        if (messageBox != null) {
            messageBox.setEnabled(enabled);
        }
        if (sendBtn != null) {
            sendBtn.setEnabled(enabled);
        }
        if (attachBtn != null) {
            attachBtn.setEnabled(enabled);
        }
        if (micBtn != null) {
            micBtn.setEnabled(enabled);
        }
    }

    private String currentTime() {
        return TIME_FORMAT.format(new Date());
    }

    @Nullable
    private String buildFarmDataContext() {
        Bundle args = getArguments();
        if (args == null) {
            return null;
        }

        StringBuilder data = new StringBuilder();

        if (args.containsKey(ARG_NDVI)) {
            data.append(String.format(Locale.US, "NDVI=%.2f", args.getDouble(ARG_NDVI)));
        }
        if (args.containsKey(ARG_SOIL_MOISTURE)) {
            appendComma(data);
            data.append(String.format(Locale.US, "Soil Moisture=%.1f%%", args.getDouble(ARG_SOIL_MOISTURE)));
        }
        if (args.containsKey(ARG_TEMPERATURE)) {
            appendComma(data);
            data.append(String.format(Locale.US, "Temperature=%.1f°C", args.getDouble(ARG_TEMPERATURE)));
        }
        if (args.containsKey(ARG_CROP)) {
            String crop = args.getString(ARG_CROP, "");
            if (!crop.trim().isEmpty()) {
                appendComma(data);
                data.append("Crop=").append(crop.trim());
            }
        }

        return data.length() == 0 ? null : data.toString();
    }

    private void appendComma(@NonNull StringBuilder builder) {
        if (builder.length() > 0) {
            builder.append(", ");
        }
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                requireContext().getResources().getDisplayMetrics()
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (voiceInputManager != null) {
            voiceInputManager.release();
        }
        if (cameraImageInputManager != null) {
            cameraImageInputManager.release();
        }
    }
}

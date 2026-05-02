package com.example.geoagri.chatbot;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.geoagri.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * AgriAssist AI chat screen.
 *
 * Responsibilities:
 * - Render chat messages in RecyclerView.
 * - Send user question + optional farm analysis context to Gemini.
 * - Show typing indicator while waiting.
 * - Auto-scroll to newest message.
 * - Handle API/network errors without crashing.
 */
public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_NDVI = "extra_ndvi";
    public static final String EXTRA_SOIL_MOISTURE = "extra_soil_moisture";
    public static final String EXTRA_TEMPERATURE = "extra_temperature";
    public static final String EXTRA_CROP_NAME = "extra_crop_name";

    private RecyclerView recyclerChat;
    private EditText editMessage;
    private ImageButton buttonSend;
    private TextView textStatus;

    private final List<MessageModel> messages = new ArrayList<>();
    private ChatAdapter chatAdapter;
    private GeminiApiService geminiApiService;

    private int typingIndicatorIndex = RecyclerView.NO_POSITION;

    @Nullable
    private String farmContext;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        recyclerChat = findViewById(R.id.recyclerChat);
        editMessage = findViewById(R.id.editMessage);
        buttonSend = findViewById(R.id.buttonSend);
        textStatus = findViewById(R.id.textStatus);

        geminiApiService = new GeminiApiService();
        farmContext = buildFarmContextFromIntent();

        setupRecycler();
        setupListeners();
        seedWelcomeMessage();
    }

    private void setupRecycler() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerChat.setLayoutManager(layoutManager);

        chatAdapter = new ChatAdapter(messages);
        recyclerChat.setAdapter(chatAdapter);

        // Auto-scroll whenever a new row is inserted.
        chatAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                recyclerChat.scrollToPosition(chatAdapter.getItemCount() - 1);
            }
        });
    }

    private void setupListeners() {
        buttonSend.setOnClickListener(v -> sendMessage());
    }

    private void seedWelcomeMessage() {
        addMessage(MessageModel.system(
                "Ask me anything about your crop health, NDVI, irrigation, or soil moisture."
        ));
    }

    private void sendMessage() {
        final String userInput = editMessage.getText().toString().trim();
        if (TextUtils.isEmpty(userInput)) {
            return;
        }

        addMessage(MessageModel.user(userInput));
        editMessage.setText("");

        showTypingIndicator(true);
        setInputEnabled(false);

        geminiApiService.askGemini(userInput, farmContext, new GeminiApiService.GeminiCallback() {
            @Override
            public void onSuccess(@NonNull String aiResponse) {
                runOnUiThread(() -> {
                    showTypingIndicator(false);
                    setInputEnabled(true);
                    addMessage(MessageModel.ai(aiResponse));
                    textStatus.setText(R.string.chat_status_online);
                });
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                runOnUiThread(() -> {
                    showTypingIndicator(false);
                    setInputEnabled(true);
                    addMessage(MessageModel.ai("I could not fetch guidance right now. Please try again."));
                    textStatus.setText(R.string.chat_status_error);
                    Toast.makeText(ChatActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setInputEnabled(boolean enabled) {
        editMessage.setEnabled(enabled);
        buttonSend.setEnabled(enabled);
    }

    private void addMessage(@NonNull MessageModel message) {
        messages.add(message);
        chatAdapter.notifyItemInserted(messages.size() - 1);
    }

    private void showTypingIndicator(boolean show) {
        if (show) {
            if (typingIndicatorIndex != RecyclerView.NO_POSITION) {
                return;
            }
            messages.add(MessageModel.typing());
            typingIndicatorIndex = messages.size() - 1;
            chatAdapter.notifyItemInserted(typingIndicatorIndex);
            textStatus.setText(R.string.chat_status_typing);
            return;
        }

        if (typingIndicatorIndex == RecyclerView.NO_POSITION) {
            return;
        }

        int removeAt = typingIndicatorIndex;
        if (removeAt >= 0 && removeAt < messages.size()) {
            messages.remove(removeAt);
            chatAdapter.notifyItemRemoved(removeAt);
        }
        typingIndicatorIndex = RecyclerView.NO_POSITION;
    }

    @Nullable
    private String buildFarmContextFromIntent() {
        Bundle extras = getIntent() != null ? getIntent().getExtras() : null;
        if (extras == null) {
            return null;
        }

        boolean hasAny = false;
        StringBuilder builder = new StringBuilder();

        if (extras.containsKey(EXTRA_NDVI)) {
            double ndvi = extras.getDouble(EXTRA_NDVI);
            builder.append(String.format(Locale.US, "NDVI=%.2f", ndvi));
            hasAny = true;
        }

        if (extras.containsKey(EXTRA_SOIL_MOISTURE)) {
            if (hasAny) {
                builder.append(", ");
            }
            double soilMoisture = extras.getDouble(EXTRA_SOIL_MOISTURE);
            builder.append(String.format(Locale.US, "Soil Moisture=%.1f%%", soilMoisture));
            hasAny = true;
        }

        if (extras.containsKey(EXTRA_TEMPERATURE)) {
            if (hasAny) {
                builder.append(", ");
            }
            double temperature = extras.getDouble(EXTRA_TEMPERATURE);
            builder.append(String.format(Locale.US, "Temperature=%.1f°C", temperature));
            hasAny = true;
        }

        String cropName = extras.getString(EXTRA_CROP_NAME);
        if (!TextUtils.isEmpty(cropName)) {
            if (hasAny) {
                builder.append(", ");
            }
            builder.append("Crop=").append(cropName);
            hasAny = true;
        }

        return hasAny ? builder.toString() : null;
    }

    public static void openWithFarmData(
            @NonNull android.content.Context context,
            double ndvi,
            double soilMoisture,
            double temperature,
            @Nullable String crop
    ) {
        android.content.Intent intent = new android.content.Intent(context, ChatActivity.class);
        intent.putExtra(EXTRA_NDVI, ndvi);
        intent.putExtra(EXTRA_SOIL_MOISTURE, soilMoisture);
        intent.putExtra(EXTRA_TEMPERATURE, temperature);
        if (crop != null) {
            intent.putExtra(EXTRA_CROP_NAME, crop);
        }
        context.startActivity(intent);
    }
}

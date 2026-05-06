package cacom.example.smarthome;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AIChatActivity extends AppCompatActivity {

    private RecyclerView recyclerChat;
    private EditText etInput;
    private Button btnSend;
    private ImageView btnBack;

    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList;

    // DeepSeek API配置
    private static final String API_KEY = "";
    private static final String API_URL = "https://api.deepseek.com/chat/completions";
    private static final String MODEL_NAME = "deepseek-v4-pro";
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    private boolean isRequesting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        initView();
        setupRecyclerView();
        setupClickListener();
        
        // 添加欢迎消息
        addAIMessage("Hello! I'm your AI agricultural assistant. You can ask me about:\n\n" +
                "• Crop cultivation techniques\n" +
                "• Pest & disease identification\n" +
                "• Greenhouse management\n" +
                "• Soil & fertilizer advice\n" +
                "• Irrigation methods\n\n" +
                "How can I help you today?");
    }

    private void initView() {
        recyclerChat = findViewById(R.id.recycler_chat);
        etInput = findViewById(R.id.et_input);
        btnSend = findViewById(R.id.btn_send);
        btnBack = findViewById(R.id.btn_back);
    }

    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList);
        recyclerChat.setLayoutManager(new LinearLayoutManager(this));
        recyclerChat.setAdapter(chatAdapter);
    }

    private void setupClickListener() {
        btnBack.setOnClickListener(v -> finish());

        btnSend.setOnClickListener(v -> sendMessage());

        etInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
    }

    private void sendMessage() {
        if (isRequesting) {
            Toast.makeText(this, "Please wait for the current response", Toast.LENGTH_SHORT).show();
            return;
        }

        String message = etInput.getText().toString().trim();
        if (message.isEmpty()) {
            Toast.makeText(this, "Please enter your question", Toast.LENGTH_SHORT).show();
            return;
        }

        // 添加用户消息
        addUserMessage(message);
        etInput.setText("");

        // 调用AI API
        callDeepSeekAPI(message);
    }

    private void addUserMessage(String message) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.isUser = true;
        chatMessage.message = message;
        messageList.add(chatMessage);
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        recyclerChat.scrollToPosition(messageList.size() - 1);
    }

    private void addAIMessage(String message) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.isUser = false;
        chatMessage.message = message;
        messageList.add(chatMessage);
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        recyclerChat.scrollToPosition(messageList.size() - 1);
    }

    private void callDeepSeekAPI(String userMessage) {
        isRequesting = true;
        btnSend.setEnabled(false);

        // 显示加载中
        addAIMessage("Thinking...");
        final int loadingIndex = messageList.size() - 1;

        try {
            // 构建请求体
            JSONObject json = new JSONObject();
            json.put("model", MODEL_NAME);

            JSONArray messages = new JSONArray();

            // 系统提示词
            JSONObject systemMsg = new JSONObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", "You are a professional agricultural expert assistant. Provide accurate, practical advice on crop cultivation, pest control, greenhouse management, soil health, and irrigation. Keep responses concise and actionable.");
            messages.put(systemMsg);

            // 用户消息
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.put(userMsg);

            json.put("messages", messages);
            json.put("temperature", 0.7);
            json.put("max_tokens", 1000);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        removeLoadingMessage(loadingIndex);
                        addAIMessage("Sorry, network error: " + e.getLocalizedMessage());
                        Toast.makeText(AIChatActivity.this,
                                "Request failed: " + e.getLocalizedMessage(),
                                Toast.LENGTH_LONG).show();
                        resetRequestState();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            JSONArray choices = jsonResponse.getJSONArray("choices");
                            String aiResponse = choices.getJSONObject(0)
                                    .getJSONObject("message")
                                    .getString("content");

                            runOnUiThread(() -> {
                                removeLoadingMessage(loadingIndex);
                                addAIMessage(aiResponse);
                                resetRequestState();
                            });
                        } catch (Exception e) {
                            runOnUiThread(() -> {
                                removeLoadingMessage(loadingIndex);
                                addAIMessage("Sorry, failed to parse response. Raw: " +
                                        responseBody.substring(0, Math.min(200, responseBody.length())));
                                resetRequestState();
                            });
                        }
                    } else {
                        String errorBody = "";
                        try {
                            errorBody = response.body().string();
                        } catch (Exception ignored) {}
                        final String errorDetail = errorBody;
                        runOnUiThread(() -> {
                            removeLoadingMessage(loadingIndex);
                            addAIMessage("API Error (HTTP " + response.code() + "): " +
                                    (errorDetail.isEmpty() ? "Please check your API key and endpoint." : errorDetail));
                            Toast.makeText(AIChatActivity.this,
                                    "API Error: " + response.code(),
                                    Toast.LENGTH_LONG).show();
                            resetRequestState();
                        });
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            removeLoadingMessage(loadingIndex);
            addAIMessage("Sorry, an error occurred: " + e.getLocalizedMessage());
            resetRequestState();
        }
    }

    private void removeLoadingMessage(int index) {
        if (index >= 0 && index < messageList.size()) {
            messageList.remove(index);
            chatAdapter.notifyItemRemoved(index);
        }
    }

    private void resetRequestState() {
        isRequesting = false;
        btnSend.setEnabled(true);
    }

    // 聊天消息数据类
    static class ChatMessage {
        boolean isUser;
        String message;
    }

    // RecyclerView适配器
    static class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
        private List<ChatMessage> messageList;

        public ChatAdapter(List<ChatMessage> messageList) {
            this.messageList = messageList;
        }

        @Override
        public ChatViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_message, parent, false);
            return new ChatViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ChatViewHolder holder, int position) {
            ChatMessage message = messageList.get(position);
            
            if (message.isUser) {
                holder.layoutUser.setVisibility(View.VISIBLE);
                holder.layoutAI.setVisibility(View.GONE);
                holder.tvUserMessage.setText(message.message);
            } else {
                holder.layoutUser.setVisibility(View.GONE);
                holder.layoutAI.setVisibility(View.VISIBLE);
                holder.tvAIMessage.setText(message.message);
            }
        }

        @Override
        public int getItemCount() {
            return messageList.size();
        }

        static class ChatViewHolder extends RecyclerView.ViewHolder {
            LinearLayout layoutUser, layoutAI;
            TextView tvUserMessage, tvAIMessage;

            public ChatViewHolder(View itemView) {
                super(itemView);
                layoutUser = itemView.findViewById(R.id.layout_user);
                layoutAI = itemView.findViewById(R.id.layout_ai);
                tvUserMessage = itemView.findViewById(R.id.tv_user_message);
                tvAIMessage = itemView.findViewById(R.id.tv_ai_message);
            }
        }
    }
}

package com.example.emeowtions.activities.user;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.emeowtions.R;
import com.example.emeowtions.adapters.ChatAdapter;
import com.example.emeowtions.adapters.ChatMessageAdapter;
import com.example.emeowtions.databinding.ActivityUserChatBinding;
import com.example.emeowtions.models.Chat;
import com.example.emeowtions.models.ChatMessage;
import com.example.emeowtions.models.User;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class UserChatActivity extends AppCompatActivity {

    private static final String TAG = "UserChatActivity";
    private static final String MESSAGE_TYPE_TEXT = "Text";
    private static final String MESSAGE_TYPE_ANALYSIS_ATTACHMENT = "Analysis Attachment";

    // Firebase variables
    private FirebaseAuthUtils firebaseAuthUtils;
    private FirebaseFirestore db;
    private CollectionReference usersRef;
    private CollectionReference chatsRef;
    private CollectionReference chatMessagesRef;

    // Layout variables
    private ActivityUserChatBinding binding;
    private boolean isAtBottom;

    // Private variables
    private String chatId;
    private FirestoreRecyclerOptions<ChatMessage> options;
    private ChatMessageAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get intent data
        Intent passedIntent = getIntent();
        chatId = passedIntent.getStringExtra(ChatAdapter.KEY_CHAT_ID);

        // Initialize Firebase service instances
        firebaseAuthUtils = new FirebaseAuthUtils();
        db = FirebaseFirestore.getInstance();
        // Initialize Firestore references
        usersRef = db.collection("users");
        chatsRef = db.collection("chats");
        chatMessagesRef = chatsRef.document(chatId).collection("chatMessages");

        // Get ViewBinding and set content view
        binding = ActivityUserChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Enable edge-to-edge layout
        EdgeToEdge.enable(this);

        setupUi();
        loadData();
        bindListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuthUtils.checkSignedIn(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Update unread count and read status
        chatsRef.document(chatId)
                .update(
                        "userUnreadCount", 0,
                        "readByUser", true
                )
                .addOnSuccessListener(unused -> {
                    // COMPLETE
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "bindNavigationListeners: Failed to update Chat details", e);
                    Toast.makeText(this, "Error occurred while updating Chat.", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupUi() {
        isAtBottom = false;
        toggleSendButton(false);
    }

    private void loadData() {
        // Load user data
        chatsRef.document(chatId)
                .addSnapshotListener((value, error) -> {
                    // Error
                    if (error != null) {
                        Log.w(TAG, "loadData: Failed to listen on Chat changes", error);
                        return;
                    }
                    // Success
                    if (value != null && value.exists()) {
                        Chat chat = value.toObject(Chat.class);

                        loadProfilePicture(chat.getVetPfpUrl());
                        binding.txtVetDisplayName.setText(chat.getVetDisplayName());
                    }
                });

        // Load chat messages
        Query query = chatMessagesRef.orderBy("updatedAt", Query.Direction.ASCENDING);

        options = new FirestoreRecyclerOptions.Builder<ChatMessage>()
                .setQuery(query, ChatMessage.class)
                .setLifecycleOwner(this)
                .build();

        adapter = new ChatMessageAdapter(options, this);
        binding.recyclerviewChatMessages.setAdapter(adapter);

        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                updateResultsView();
            }
        });
    }

    private void loadProfilePicture(String profilePictureUrl) {
        if (profilePictureUrl == null) {
            // Load default picture if no profile picture exists
            Glide.with(getApplicationContext())
                    .load(R.drawable.baseline_account_circle_24)
                    .into(binding.imgVetPfp);
        } else {
            // Load original profile picture
            Glide.with(getApplicationContext())
                    .load(profilePictureUrl)
                    .into(binding.imgVetPfp);
        }
    }

    private void bindListeners() {
        bindNavigationListeners();
        bindOnClickListeners();
        bindTextChangeListeners();
        bindOtherListeners();
    }

    private void bindNavigationListeners() {
        binding.appBarUserChat.setNavigationOnClickListener(view -> {
            finish();
        });
    }

    private void bindOnClickListeners() {
        // Attach analysis report
        binding.btnAttach.setOnClickListener(view -> {
            // Add new "analysis" type ChatMessage
        });

        // Send message
        binding.btnSend.setOnClickListener(view -> {
            // Get message input
            String message = binding.edtMessage.getText().toString().trim();

            usersRef.document(firebaseAuthUtils.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (!documentSnapshot.exists()) {
                            Log.w(TAG, "bindOnClickListeners: No User with the uid exists");
                            Toast.makeText(this, "Unable to send message, please try again later.", Toast.LENGTH_SHORT).show();
                        } else {
                            User user = documentSnapshot.toObject(User.class);
                            ChatMessage newMessage = new ChatMessage(
                                    documentSnapshot.getId(),
                                    user.getProfilePicture(),
                                    user.getDisplayName(),
                                    message,
                                    MESSAGE_TYPE_TEXT,
                                    null,
                                    Timestamp.now(),
                                    Timestamp.now()
                            );

                            // Add new "text" type ChatMessage
                            chatMessagesRef.add(newMessage)
                                    .addOnSuccessListener(documentReference -> {
                                        // Update Chat
                                        chatsRef.document(chatId)
                                                .update(
                                                        "userDisplayName", user.getDisplayName(),
                                                        "userPfpUrl", user.getProfilePicture(),
                                                        "vetUnreadCount", FieldValue.increment(1),
                                                        "readByVet", false,
                                                        "latestMessageText", message,
                                                        "latestMessageSenderUid", firebaseAuthUtils.getUid(),
                                                        "updatedAt", Timestamp.now()
                                                )
                                                .addOnSuccessListener(unused -> {
                                                    // COMPLETE
                                                    // Clear message field
                                                    binding.edtMessage.getText().clear();
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.w(TAG, "bindOnClickListeners: Failed to update Chat details", e);
                                                    Toast.makeText(this, "Unable to update chat, please try again later.", Toast.LENGTH_SHORT).show();
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.w(TAG, "bindOnClickListeners: Failed to add new ChatMessage", e);
                                        Toast.makeText(this, "Unable to send message, please try again later.", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "bindOnClickListeners: Failed to retrieve User data", e);
                        Toast.makeText(this, "Unable to send message, please try again later.", Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void bindTextChangeListeners() {
        binding.edtMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Enable or disable send button based on whether message input is blank
                String text = binding.edtMessage.getText().toString().trim();
                toggleSendButton(!text.isBlank());
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
    }

    private void bindOtherListeners() {
        // Keep track of scroll position
        binding.recyclerviewChatMessages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                // Check if the user is at the bottom of the RecyclerView
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    int lastVisibleItemPosition = layoutManager.findLastCompletelyVisibleItemPosition();
                    int itemCount = layoutManager.getItemCount();

                    // If the last item is visible, set isAtBottom to true
                    isAtBottom = (lastVisibleItemPosition == itemCount - 1);
                }
            }
        });

        binding.edtMessage.setOnClickListener(view -> {
            scrollToBottomIfAtBottom();
        });

        binding.edtMessage.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                scrollToBottomIfAtBottom();
            }
        });
    }

    private void toggleSendButton(boolean enabled) {
        binding.btnSend.setEnabled(enabled);
        binding.btnSend.setIconTint(ContextCompat.getColorStateList(this, R.color.white));

        if (enabled) {
            binding.btnSend.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.primary_400));
        } else {
            binding.btnSend.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.primary_200));
        }
    }

    private void updateResultsView() {
        binding.recyclerviewChatMessages.setVisibility(View.GONE);
        binding.layoutNoChatMessages.setVisibility(View.GONE);

        if (adapter == null || adapter.getItemCount() == 0) {
            binding.layoutNoChatMessages.setVisibility(View.VISIBLE);
        } else {
            binding.recyclerviewChatMessages.setVisibility(View.VISIBLE);
            binding.recyclerviewChatMessages.scrollToPosition(adapter.getItemCount() - 1);
        }
    }

    private void scrollToBottomIfAtBottom() {
        if (isAtBottom) {
            binding.recyclerviewChatMessages.postDelayed(() -> {
                binding.recyclerviewChatMessages.smoothScrollToPosition(adapter.getItemCount() - 1);
            }, 250);
        }
    }
}
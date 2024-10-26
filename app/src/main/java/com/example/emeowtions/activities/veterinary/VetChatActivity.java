package com.example.emeowtions.activities.veterinary;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.emeowtions.R;
import com.example.emeowtions.adapters.ChatAdapter;
import com.example.emeowtions.databinding.ActivityVetChatBinding;
import com.example.emeowtions.models.Chat;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

public class VetChatActivity extends AppCompatActivity {

    private static final String TAG = "VetChatActivity";

    // Firebase variables
    private FirebaseAuthUtils firebaseAuthUtils;
    private FirebaseFirestore db;
    private CollectionReference chatsRef;
    private CollectionReference chatMessagesRef;

    // Layout variables
    private ActivityVetChatBinding binding;

    // Private variables
    private String chatId;

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
        chatsRef = db.collection("chats");
        chatMessagesRef = chatsRef.document(chatId).collection("chatMessages");

        // Get ViewBinding and set content view
        binding = ActivityVetChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Enable edge-to-edge layout
        EdgeToEdge.enable(this);
        // Adjust main layout padding based on system bar size to prevent obstruction
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupUi();
        loadData();
        bindListeners();
    }

    private void setupUi() {

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

                        loadProfilePicture(chat.getUserPfpUrl());
                        binding.txtUserDisplayName.setText(chat.getUserDisplayName());
                    }
                });

        // Load chat messages
        chatMessagesRef.addSnapshotListener((values, error) -> {
            // Error
            if (error != null) {
                Log.w(TAG, "loadData: Failed to listen on ChatMessage changes", error);
                return;
            }
            // Success
            if (values != null && !values.isEmpty()) {

            }
        });
    }

    private void loadProfilePicture(String profilePictureUrl) {
        if (profilePictureUrl == null) {
            // Load default picture if no profile picture exists
            Glide.with(getApplicationContext())
                    .load(R.drawable.baseline_account_circle_24)
                    .into(binding.imgUserPfp);
        } else {
            // Load original profile picture
            Glide.with(getApplicationContext())
                    .load(profilePictureUrl)
                    .into(binding.imgUserPfp);
        }
    }

    private void bindListeners() {
        bindNavigationListeners();
        bindOnClickListeners();
        bindTextChangeListeners();
    }

    private void bindNavigationListeners() {
        binding.appBarVetChat.setNavigationOnClickListener(view -> finish());
    }

    private void bindOnClickListeners() {
        // Attach analysis report
        binding.btnAttach.setOnClickListener(view -> {
            // Add new "analysis" type ChatMessage
        });

        // Send message
        binding.btnSend.setOnClickListener(view -> {
            // Add new "text" type ChatMessage
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
                binding.btnSend.setEnabled(!text.isBlank());
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
    }
}
package com.example.emeowtions.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.emeowtions.R;
import com.example.emeowtions.activities.user.SavedAnalysesActivity;
import com.example.emeowtions.activities.user.UserChatActivity;
import com.example.emeowtions.activities.user.ViewSavedAnalysisActivity;
import com.example.emeowtions.activities.veterinary.VetChatActivity;
import com.example.emeowtions.enums.Role;
import com.example.emeowtions.models.Analysis;
import com.example.emeowtions.models.ChatMessage;
import com.example.emeowtions.models.User;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class AnalysisAdapter extends FirestoreRecyclerAdapter<Analysis, AnalysisAdapter.AnalysisHolder> {

    private static final String TAG = "AnalysisAdapter";
    private SharedPreferences sharedPreferences;

    // Public variables
    public static final String KEY_ANALYSIS_ID = "analysisId";

    // Firebase variables
    private FirebaseAuthUtils firebaseAuthUtils;
    private FirebaseFirestore db;
    private CollectionReference usersRef;
    private CollectionReference chatsRef;
    private CollectionReference chatMessagesRef;

    // Private variables
    private Context context;
    private String currentUserRole;
    private String chatId;
    private AlertDialog attachmentDialog;

    // Default constructor
    public AnalysisAdapter(FirestoreRecyclerOptions<Analysis> options, Context context) {
        super(options);
        this.context = context;
        this.sharedPreferences = context.getSharedPreferences("com.emeowtions", Context.MODE_PRIVATE);
        this.currentUserRole = sharedPreferences.getString("role", "User");
    }

    // Constructor for Analysis Report Attachment in UserChatActivity or VetChatActivity
    public AnalysisAdapter(FirestoreRecyclerOptions<Analysis> options, Context context, String chatId, AlertDialog attachmentDialog) {
        super(options);
        this.context = context;
        this.sharedPreferences = context.getSharedPreferences("com.emeowtions", Context.MODE_PRIVATE);
        this.currentUserRole = sharedPreferences.getString("role", "User");
        this.chatId = chatId;
        this.firebaseAuthUtils = new FirebaseAuthUtils();
        this.db = FirebaseFirestore.getInstance();
        this.usersRef = db.collection("users");
        this.chatsRef = db.collection("chats");
        this.chatMessagesRef = chatsRef.document(this.chatId).collection("chatMessages");
        this.attachmentDialog = attachmentDialog;
    }

    @Override
    protected void onBindViewHolder(@NonNull AnalysisHolder holder, int position, @NonNull Analysis model) {
        SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault());

        // Populate data
        Glide.with(context)
                .load(model.getImageUrl())
                .into(holder.imgAnalyzedImage);
        holder.txtCatName.setText(model.getCatName() == null ? "Unspecified" : model.getCatName());
        holder.txtEmotion.setText(model.getEmotion());
        holder.txtCreatedDate.setText(sdf.format(model.getCreatedAt().toDate()));

        // Determine onClick action based on context
        // SavedAnalysesActivity
        if (context instanceof SavedAnalysesActivity) {
            // Redirect to ViewSavedAnalysisActivity
            holder.body.setOnClickListener(view -> {
                Intent intent = new Intent(context, ViewSavedAnalysisActivity.class);
                intent.putExtra(KEY_ANALYSIS_ID, getSnapshots().getSnapshot(position).getId());
                context.startActivity(intent);
            });
        }
        // UserChatActivity or VetChatActivity
        else if (context instanceof UserChatActivity || context instanceof VetChatActivity) {
            // Create new chat message with Analysis as attachment
            holder.body.setOnClickListener(view -> {
                createAnalysisAttachmentMessage(getSnapshots().getSnapshot(position).getId(), model, chatId);
                this.attachmentDialog.dismiss();
            });
        }
    }

    @NonNull
    @Override
    public AnalysisAdapter.AnalysisHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.holder_analysis, parent, false);
        return new AnalysisHolder(view);
    }

    @Override
    public void onDataChanged() {
        super.onDataChanged();
        notifyDataSetChanged();
    }

    @Override
    public void onError(@NonNull FirebaseFirestoreException e) {
        super.onError(e);
        Log.e(TAG, e.getMessage());
    }

    // Create Analysis Attachment type chat message for UserChatActivity or VetChatActivity
    private void createAnalysisAttachmentMessage(String analysisId, Analysis analysis, String chatId) {
        // Retrieve user data
        usersRef.document(firebaseAuthUtils.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Log.w(TAG, "bindOnClickListeners: No User with the uid exists");
                        Toast.makeText(context, "Unable to send message, please try again later.", Toast.LENGTH_SHORT).show();
                    } else {
                        User user = documentSnapshot.toObject(User.class);
                        ChatMessage newMessage = new ChatMessage(
                                documentSnapshot.getId(),
                                user.getProfilePicture(),
                                user.getDisplayName(),
                                analysis.getImageUrl(),
                                ChatMessageAdapter.MESSAGE_TYPE_ANALYSIS_ATTACHMENT,
                                analysisId,
                                Timestamp.now(),
                                Timestamp.now()
                        );

                        // Add new "Text" type ChatMessage
                        chatMessagesRef.add(newMessage)
                                .addOnSuccessListener(documentReference -> {
                                    // Update Chat based on current user's role
                                    if (currentUserRole.equals(Role.USER.getTitle()) || currentUserRole.equals(Role.ADMIN.getTitle()) || currentUserRole.equals(Role.SUPER_ADMIN.getTitle())) {
                                        // Update Vet (recipient) fields
                                        chatsRef.document(chatId)
                                                .update(
                                                        "userDisplayName", user.getDisplayName(),
                                                        "userPfpUrl", user.getProfilePicture(),
                                                        "vetUnreadCount", FieldValue.increment(1),
                                                        "readByVet", false,
                                                        "latestMessageText", "Analysis Report (Attachment)",
                                                        "latestMessageSenderUid", firebaseAuthUtils.getUid(),
                                                        "updatedAt", Timestamp.now()
                                                )
                                                .addOnSuccessListener(unused -> {
                                                    // COMPLETE
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.w(TAG, "bindOnClickListeners: Failed to update Chat details", e);
                                                    Toast.makeText(context, "Unable to update chat, please try again later.", Toast.LENGTH_SHORT).show();
                                                });
                                    } else {
                                        // Update User (recipient) fields
                                        chatsRef.document(chatId)
                                                .update(
                                                        "vetDisplayName", user.getDisplayName(),
                                                        "vetPfpUrl", user.getProfilePicture(),
                                                        "userUnreadCount", FieldValue.increment(1),
                                                        "readByUser", false,
                                                        "latestMessageText", "Analysis Report (Attachment)",
                                                        "latestMessageSenderUid", firebaseAuthUtils.getUid(),
                                                        "updatedAt", Timestamp.now()
                                                )
                                                .addOnSuccessListener(unused -> {
                                                    // COMPLETE
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.w(TAG, "bindOnClickListeners: Failed to update Chat details", e);
                                                    Toast.makeText(context, "Unable to update chat, please try again later.", Toast.LENGTH_SHORT).show();
                                                });
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.w(TAG, "bindOnClickListeners: Failed to add new ChatMessage", e);
                                    Toast.makeText(context, "Unable to send message, please try again later.", Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "bindOnClickListeners: Failed to retrieve User data", e);
                    Toast.makeText(context, "Unable to send message, please try again later.", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadImage(Drawable image, ImageView imageView) {
        Glide.with(context)
                .load(image)
                .into(imageView);
    }

    protected class AnalysisHolder extends RecyclerView.ViewHolder {
        MaterialCardView body;
        ImageView imgAnalyzedImage;
        TextView txtCatName;
        TextView txtEmotion;
        TextView txtCreatedDate;

        public AnalysisHolder(@NonNull View itemView) {
            super(itemView);
            body = itemView.findViewById(R.id.body);
            imgAnalyzedImage = itemView.findViewById(R.id.img_analyzed_image);
            txtCatName = itemView.findViewById(R.id.txt_cat_name);
            txtEmotion = itemView.findViewById(R.id.txt_emotion);
            txtCreatedDate = itemView.findViewById(R.id.txt_created_date);
        }
    }
}

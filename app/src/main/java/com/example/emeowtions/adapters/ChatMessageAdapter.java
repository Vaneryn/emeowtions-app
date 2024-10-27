package com.example.emeowtions.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.emeowtions.R;
import com.example.emeowtions.activities.user.ViewSavedAnalysisActivity;
import com.example.emeowtions.models.Analysis;
import com.example.emeowtions.models.ChatMessage;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class ChatMessageAdapter extends FirestoreRecyclerAdapter<ChatMessage, ChatMessageAdapter.ChatMessageHolder> {

    private static final String TAG = "ChatMessageAdapter";
    private static final int VIEW_TYPE_INCOMING_TEXT = 0;
    private static final int VIEW_TYPE_OUTGOING_TEXT = 1;
    private static final int VIEW_TYPE_INCOMING_ANALYSIS_ATTACHMENT = 2;
    private static final int VIEW_TYPE_OUTGOING_ANALYSIS_ATTACHMENT = 3;


    // Public variables
    public static final String MESSAGE_TYPE_TEXT = "Text";
    public static final String MESSAGE_TYPE_ANALYSIS_ATTACHMENT = "Analysis Attachment";
    public static final String KEY_ANALYSIS_ID = "analysisId";
    public static final String KEY_ACTIVITY_CONTEXT = "activityContext";
    public static final String KEY_ANALYSIS_ATTACHMENT_NAME = "analysisAttachmentName";

    // Firebase variables
    private FirebaseAuthUtils firebaseAuthUtils;
    private FirebaseFirestore db;
    private CollectionReference analysesRef;

    // Private variables
    private Context context;

    public ChatMessageAdapter(FirestoreRecyclerOptions<ChatMessage> options, Context context) {
        super(options);
        this.context = context;
        this.firebaseAuthUtils = new FirebaseAuthUtils();
        this.db = FirebaseFirestore.getInstance();
        this.analysesRef = db.collection("analyses");
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatMessageHolder holder, int position, @NonNull ChatMessage model) {
        SimpleDateFormat sdfDatetime = new SimpleDateFormat("d/MM/yyyy  h:mm a", Locale.getDefault());
        holder.txtDatetime.setText(String.format("%s", sdfDatetime.format(model.getUpdatedAt().toDate())));

        if (model.getMessageType().equals(MESSAGE_TYPE_TEXT)) {
            // Populate data for Text message
            holder.txtMessage.setText(model.getMessageText());
        } else if (model.getMessageType().equals(MESSAGE_TYPE_ANALYSIS_ATTACHMENT)) {
            analysesRef.document(model.getAnalysisId())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Analysis analysis = documentSnapshot.toObject(Analysis.class);

                            // Populate data for Analysis Attachment message
                            String analysisAttachmentName = String.format("Analysis Report: %s", (analysis.getCatName() == null ? "Unspecified" : analysis.getCatName()));

                            Glide.with(context.getApplicationContext())
                                    .load(analysis.getImageUrl())
                                    .into(holder.imgAnalysisImage);
                            holder.txtAttachmentName.setText(analysisAttachmentName);

                            // Set onClick listener for attachment
                            holder.cardAttachment.setOnClickListener(view -> {
                                // Redirect to ViewSavedAnalysisActivity
                                Intent intent = new Intent(context, ViewSavedAnalysisActivity.class);
                                intent.putExtra(KEY_ANALYSIS_ID, documentSnapshot.getId());
                                intent.putExtra(KEY_ACTIVITY_CONTEXT, "ChatActivity");
                                intent.putExtra(KEY_ANALYSIS_ATTACHMENT_NAME, analysisAttachmentName);
                                context.startActivity(intent);
                            });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "onBindViewHolder: Failed to retrieve Analysis data", e);
                        Toast.makeText(context, "Unable to load analysis report attachment data.", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    @NonNull
    @Override
    public ChatMessageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;

        // Check the type of message
        if (viewType == VIEW_TYPE_INCOMING_TEXT) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.holder_chat_message_incoming, parent, false);
        } else if (viewType == VIEW_TYPE_OUTGOING_TEXT) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.holder_chat_message_outgoing, parent, false);
        } else if (viewType == VIEW_TYPE_INCOMING_ANALYSIS_ATTACHMENT) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.holder_chat_message_attachment_incoming, parent, false);
        } else if (viewType == VIEW_TYPE_OUTGOING_ANALYSIS_ATTACHMENT) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.holder_chat_message_attachment_outgoing, parent, false);
        } else {
            // Default
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.holder_chat_message_outgoing, parent, false);
        }

        return new ChatMessageHolder(view);
    }

    @Override
    public void onDataChanged() {
        super.onDataChanged();
        notifyDataSetChanged();
    }

    @Override
    public void onError(@NonNull FirebaseFirestoreException e) {
        super.onError(e);
        Log.e(TAG, "onError: ", e);
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage chatMessage = getItem(position);

        // Check the type of message
        if (chatMessage.getMessageType().equals(MESSAGE_TYPE_TEXT)) {
            // Text
            if (!chatMessage.getSenderUid().equals(firebaseAuthUtils.getUid())) {
                // Incoming Text
                return VIEW_TYPE_INCOMING_TEXT;
            } else {
                // Outgoing Text
                return VIEW_TYPE_OUTGOING_TEXT;
            }
        } else if (chatMessage.getMessageType().equals(MESSAGE_TYPE_ANALYSIS_ATTACHMENT)) {
            // Analysis Attachment
            // Text
            if (!chatMessage.getSenderUid().equals(firebaseAuthUtils.getUid())) {
                // Incoming Text
                return VIEW_TYPE_INCOMING_ANALYSIS_ATTACHMENT;
            } else {
                // Outgoing Text
                return VIEW_TYPE_OUTGOING_ANALYSIS_ATTACHMENT;
            }
        }

        // Default
        return VIEW_TYPE_OUTGOING_TEXT;
    }

    private void loadImage(Drawable icon, ImageView imageView) {
        Glide.with(context)
                .load(icon)
                .into(imageView);
    }

    protected class ChatMessageHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardAttachment;
        ImageView imgAnalysisImage;
        TextView txtAttachmentName;
        TextView txtMessage;
        TextView txtDatetime;

        public ChatMessageHolder(@NonNull View itemView) {
            super(itemView);
            cardAttachment = itemView.findViewById(R.id.card_attachment);
            imgAnalysisImage = itemView.findViewById(R.id.img_analysis_image);
            txtAttachmentName = itemView.findViewById(R.id.txt_attachment_name);
            txtMessage = itemView.findViewById(R.id.txt_message);
            txtDatetime = itemView.findViewById(R.id.txt_datetime);
        }
    }
}

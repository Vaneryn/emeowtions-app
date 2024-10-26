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
import com.example.emeowtions.activities.user.UserChatActivity;
import com.example.emeowtions.activities.user.UserMainActivity;
import com.example.emeowtions.activities.veterinary.VetChatActivity;
import com.example.emeowtions.activities.veterinary.VetMainActivity;
import com.example.emeowtions.models.Chat;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class ChatAdapter extends FirestoreRecyclerAdapter<Chat, ChatAdapter.ChatHolder> {

    private static final String TAG = "ChatAdapter";
    public static final String KEY_CHAT_ID = "chatId";

    private Context context;

    // Firebase variables
    private FirebaseAuthUtils firebaseAuthUtils;
    private FirebaseFirestore db;
    private CollectionReference chatsRef;

    public ChatAdapter(FirestoreRecyclerOptions<Chat> options, Context context) {
        super(options);
        this.context = context;
        this.firebaseAuthUtils = new FirebaseAuthUtils();
        this.db = FirebaseFirestore.getInstance();
        this.chatsRef = db.collection("chats");
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatHolder holder, int position, @NonNull Chat model) {
        SimpleDateFormat sdfDate = new SimpleDateFormat("d/MM/yyyy", Locale.getDefault());
        SimpleDateFormat sdfDatetime = new SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault());

        // Different logic based on user role
        if (context instanceof UserMainActivity) {
            // User
            // Populate data
            if (model.getVetPfpUrl() == null) {
                loadImage(ContextCompat.getDrawable(context, R.drawable.baseline_person_24), holder.imgPfp);
            } else {
                Glide.with(context.getApplicationContext())
                        .load(model.getVetPfpUrl())
                        .into(holder.imgPfp);
            }

            holder.txtDisplayName.setText(model.getVetDisplayName());
            holder.txtLatestMessageText.setText(
                    model.getLatestMessageSenderUid() != null && model.getLatestMessageSenderUid().equals(firebaseAuthUtils.getUid())
                    ? String.format("You: %s", model.getLatestMessageText())
                    : model.getLatestMessageText()
            );
            holder.txtLatestMessageDate.setText(String.format("%s", sdfDate.format(model.getUpdatedAt().toDate())));
            holder.txtUnreadCount.setText(String.format("%s", model.getUserUnreadCount()));

            // Set style based on read status
            toggleChatStyle(holder, model.isReadByUser());

            // body onClick: open UserChatActivity
            holder.body.setOnClickListener(view -> {
                Intent intent = new Intent(context, UserChatActivity.class);
                intent.putExtra(KEY_CHAT_ID, getSnapshots().getSnapshot(position).getId());
                context.startActivity(intent);
            });
        } else if (context instanceof VetMainActivity) {
            // Veterinarian or VeterinaryStaff
            // Populate data
            if (model.getUserPfpUrl() == null) {
                loadImage(ContextCompat.getDrawable(context, R.drawable.baseline_person_24), holder.imgPfp);
            } else {
                Glide.with(context.getApplicationContext())
                        .load(model.getUserPfpUrl())
                        .into(holder.imgPfp);
            }

            holder.txtDisplayName.setText(model.getUserDisplayName());
            holder.txtLatestMessageText.setText(
                    model.getLatestMessageSenderUid() != null && model.getLatestMessageSenderUid().equals(firebaseAuthUtils.getUid())
                            ? String.format("You: %s", model.getLatestMessageText())
                            : model.getLatestMessageText()
            );
            holder.txtLatestMessageDate.setText(String.format("%s", sdfDate.format(model.getUpdatedAt().toDate())));
            holder.txtUnreadCount.setText(String.format("%s", model.getVetUnreadCount()));

            // Set style based on read status
            toggleChatStyle(holder, model.isReadByVet());

            // body onClick: open VetChatActivity
            holder.body.setOnClickListener(view -> {
                Intent intent = new Intent(context, VetChatActivity.class);
                intent.putExtra(KEY_CHAT_ID, getSnapshots().getSnapshot(position).getId());
                context.startActivity(intent);
            });
        }
    }

    @NonNull
    @Override
    public ChatHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.holder_chat, parent, false);
        return new ChatHolder(view);
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

    private void toggleChatStyle(ChatHolder holder, boolean isRead) {
        if (isRead) {
            holder.txtDisplayName.setTypeface(null, Typeface.NORMAL);
            holder.txtLatestMessageText.setTextColor(ContextCompat.getColor(context, R.color.gray_300));
            holder.txtLatestMessageText.setTypeface(null, Typeface.NORMAL);
            holder.txtLatestMessageDate.setTextColor(ContextCompat.getColor(context, R.color.gray_300));
            holder.txtLatestMessageDate.setTypeface(null, Typeface.NORMAL);
            holder.txtUnreadCount.setVisibility(View.GONE);
        } else {
            holder.txtDisplayName.setTypeface(null, Typeface.BOLD);
            holder.txtLatestMessageText.setTextColor(ContextCompat.getColor(context, R.color.gray_400));
            holder.txtLatestMessageText.setTypeface(null, Typeface.BOLD);
            holder.txtLatestMessageDate.setTextColor(ContextCompat.getColor(context, R.color.primary_300));
            holder.txtLatestMessageDate.setTypeface(null, Typeface.BOLD);
            holder.txtUnreadCount.setVisibility(View.VISIBLE);
        }
    }

    private void loadImage(Drawable icon, ImageView imageView) {
        Glide.with(context)
                .load(icon)
                .into(imageView);
    }

    protected class ChatHolder extends RecyclerView.ViewHolder {
        LinearLayout body;
        ImageView imgPfp;
        TextView txtDisplayName;
        TextView txtLatestMessageText;
        TextView txtLatestMessageDate;
        TextView txtUnreadCount;

        public ChatHolder(@NonNull View itemView) {
            super(itemView);
            body = itemView.findViewById(R.id.body);
            imgPfp = itemView.findViewById(R.id.img_pfp);
            txtDisplayName = itemView.findViewById(R.id.txt_display_name);
            txtLatestMessageText = itemView.findViewById(R.id.txt_latest_message_text);
            txtLatestMessageDate = itemView.findViewById(R.id.txt_latest_message_date);
            txtUnreadCount = itemView.findViewById(R.id.txt_unread_count);
        }
    }
}

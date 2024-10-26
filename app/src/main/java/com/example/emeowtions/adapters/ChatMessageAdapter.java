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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class ChatMessageAdapter extends FirestoreRecyclerAdapter<ChatMessage, ChatMessageAdapter.ChatMessageHolder> {

    private static final String TAG = "ChatMessageAdapter";
    private static final int VIEW_TYPE_INCOMING = 0;
    private static final int VIEW_TYPE_OUTGOING = 1;

    private Context context;

    // Firebase variables
    private FirebaseAuthUtils firebaseAuthUtils;

    public ChatMessageAdapter(FirestoreRecyclerOptions<ChatMessage> options, Context context) {
        super(options);
        this.context = context;
        this.firebaseAuthUtils = new FirebaseAuthUtils();
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatMessageHolder holder, int position, @NonNull ChatMessage model) {
        SimpleDateFormat sdfDatetime = new SimpleDateFormat("d/MM/yyyy  h:mm a", Locale.getDefault());

        holder.txtMessage.setText(model.getMessageText());
        holder.txtDatetime.setText(String.format("%s", sdfDatetime.format(model.getUpdatedAt().toDate())));
    }

    @NonNull
    @Override
    public ChatMessageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;

        // Check if it is incoming or outgoing message
        if (viewType == VIEW_TYPE_INCOMING) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.holder_chat_message_incoming, parent, false);
        } else {
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

        // Check if it is incoming or outgoing message
        if (!chatMessage.getSenderUid().equals(firebaseAuthUtils.getUid())) {
            // Incoming
            return VIEW_TYPE_INCOMING;
        } else {
            // Outgoing
            return VIEW_TYPE_OUTGOING;
        }
    }

    private void loadImage(Drawable icon, ImageView imageView) {
        Glide.with(context)
                .load(icon)
                .into(imageView);
    }

    protected class ChatMessageHolder extends RecyclerView.ViewHolder {
        TextView txtMessage;
        TextView txtDatetime;

        public ChatMessageHolder(@NonNull View itemView) {
            super(itemView);
            txtMessage = itemView.findViewById(R.id.txt_message);
            txtDatetime = itemView.findViewById(R.id.txt_datetime);
        }
    }
}

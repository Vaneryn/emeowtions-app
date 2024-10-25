package com.example.emeowtions.adapters;

import android.content.Context;
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
import com.example.emeowtions.models.Chat;
import com.example.emeowtions.models.ChatRequest;
import com.example.emeowtions.models.User;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class ChatRequestAdapter extends FirestoreRecyclerAdapter<ChatRequest, ChatRequestAdapter.ChatRequestHolder> {

    private static final String TAG = "ChatRequestAdapter";
    private Context context;

    private FirebaseAuthUtils firebaseAuthUtils;
    private FirebaseFirestore db;
    private CollectionReference usersRef;
    private CollectionReference chatRequestsRef;
    private CollectionReference chatsRef;

    public ChatRequestAdapter(FirestoreRecyclerOptions<ChatRequest> options, Context context) {
        super(options);
        this.context = context;
        this.firebaseAuthUtils = new FirebaseAuthUtils();
        this.db = FirebaseFirestore.getInstance();
        this.usersRef = db.collection("users");
        this.chatRequestsRef = db.collection("chatRequests");
        this.chatsRef = db.collection("chats");
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatRequestHolder holder, int position, @NonNull ChatRequest model) {
        SimpleDateFormat sdfDate = new SimpleDateFormat("d MMM yyyy", Locale.getDefault());
        SimpleDateFormat sdfDatetime = new SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault());

        // Set style based on accepted status
        if (model.isAccepted()) {
            setViewOpacity(holder, 0.7f);
        } else {
            setViewOpacity(holder, 1);
        }

        // Populate chat request data
        if (model.getUserPfpUrl() == null) {
            loadImage(ContextCompat.getDrawable(context, R.drawable.baseline_person_24), holder.imgPfp);
        } else {
            Glide.with(context.getApplicationContext())
                    .load(model.getUserPfpUrl())
                    .into(holder.imgPfp);
        }

        holder.txtDisplayName.setText(model.getUserDisplayName() == null ? model.getUserEmail().split("@")[0] : model.getUserDisplayName());
        holder.txtDescription.setText(model.getDescription());
        holder.txtSubmittedDate.setText(String.format("Submitted: %s",sdfDatetime.format(model.getCreatedAt().toDate())));
        holder.txtAcceptedDate.setText(
                model.isAccepted()
                ? String.format("Accepted: %s", sdfDatetime.format(model.getUpdatedAt().toDate()))
                : ""
        );

        // When chat request is clicked
        holder.body.setOnClickListener(view -> {
            // Open details dialog
            View chatRequestDialogLayout = LayoutInflater.from(context).inflate(R.layout.dialog_chat_request, null);
            TextView txtDisplayName = chatRequestDialogLayout.findViewById(R.id.txt_display_name);
            TextView txtEmail = chatRequestDialogLayout.findViewById(R.id.txt_email);
            TextView txtSubmittedDate = chatRequestDialogLayout.findViewById(R.id.txt_submitted_date);
            TextView txtAcceptedDate = chatRequestDialogLayout.findViewById(R.id.txt_accepted_date);
            TextView txtDescription = chatRequestDialogLayout.findViewById(R.id.txt_description);
            LinearLayout lytSubmittedDate = chatRequestDialogLayout.findViewById(R.id.layout_submitted_date);
            LinearLayout lytAcceptedDate = chatRequestDialogLayout.findViewById(R.id.layout_accepted_date);

            // Set fields
            txtDisplayName.setText(model.getUserDisplayName());
            txtEmail.setText(model.getUserEmail());
            txtSubmittedDate.setText(String.format("%s", sdfDatetime.format(model.getCreatedAt().toDate())));
            txtAcceptedDate.setText(String.format("%s", sdfDatetime.format(model.getUpdatedAt().toDate())));
            txtDescription.setText(model.getDescription());

            MaterialAlertDialogBuilder chatRequestDialogBuilder =
                    new MaterialAlertDialogBuilder(context)
                            .setView(chatRequestDialogLayout)
                            .setTitle(R.string.request_details)
                            .setNegativeButton(R.string.close, (dialogInterface, i) -> {})
                            .setPositiveButton(R.string.accept, (dialogInterface, i) -> {
                                processRequest(getSnapshots().getSnapshot(position).getId(), model);
                            });

            AlertDialog chatRequestDialog = chatRequestDialogBuilder.create();
            chatRequestDialog.show();

            // Show or hide accepted date
            if (model.isAccepted()) {
                lytAcceptedDate.setVisibility(View.VISIBLE);
            } else {
                lytAcceptedDate.setVisibility(View.GONE);
            }
        });
    }

    @NonNull
    @Override
    public ChatRequestHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.holder_chat_request, parent, false);
        return new ChatRequestHolder(view);
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

    // Checks if the vet has already accepted this request before (ie. the vet already has a chat with this user)
    private void processRequest(String chatRequestId, ChatRequest chatRequest) {
        chatsRef.whereEqualTo("vetId", firebaseAuthUtils.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // COMPLETE
                    if (queryDocumentSnapshots.isEmpty()) {
                        // No existing Chat
                        acceptRequest(chatRequestId, chatRequest);
                    } else {
                        // Existing Chat
                        redirectToChat();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "processRequest: Failed to retrieve Chat data", e);
                    Toast.makeText(context, "Unable to accept request, please try again later.", Toast.LENGTH_SHORT).show();
                });
    }

    // Updates ChatRequest to accepted
    private void acceptRequest(String chatRequestId, ChatRequest chatRequest) {
        chatRequestsRef.document(chatRequestId)
                .update("accepted", true, "updatedAt", Timestamp.now())
                .addOnSuccessListener(unused -> {
                    //createChat(chatRequestId, chatRequest);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "acceptRequest: Failed to accept Request", e);
                    Toast.makeText(context, "Unable to accept request, please try again later.", Toast.LENGTH_SHORT).show();
                });
    }

    // Creates new Chat
    private void createChat(String chatRequestId, ChatRequest chatRequest) {
        // Retrieve user data
        usersRef.document(firebaseAuthUtils.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);

                        Chat newChat = new Chat(
                                chatRequestId,
                                firebaseAuthUtils.getUid(),
                                user.getDisplayName(),
                                user.getProfilePicture(),
                                0,
                                true,
                                chatRequest.getUid(),
                                chatRequest.getUserDisplayName(),
                                chatRequest.getUserPfpUrl(),
                                0,
                                true,
                                "",
                                Timestamp.now(),
                                Timestamp.now()
                        );

                        // Add new Chat
                        chatsRef.add(newChat)
                                .addOnSuccessListener(documentReference -> {
                                    // TODO: Redirect to new chat
                                })
                                .addOnFailureListener(e -> {
                                    Log.w(TAG, "createChat: Failed to add new Chat", e);
                                    Toast.makeText(context, "Unable to create new chat, please try again later.", Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "createChat: Failed to retrieve User data", e);
                    Toast.makeText(context, "Unable to create new chat, please try again later.", Toast.LENGTH_SHORT).show();
                });
    }

    // Redirect to existing Chat on confirmation
    private void redirectToChat() {
        MaterialAlertDialogBuilder redirectDialog =
                new MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.existing_chat)
                        .setMessage(R.string.redirect_existing_chat_message)
                        .setNegativeButton(R.string.no, (dialogInterface, i) -> {})
                        .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                            // TODO: Redirect to Chat
                        });
    }

    private void setViewOpacity(ChatRequestHolder holder, float alpha) {
        holder.body.setAlpha(alpha);
        holder.imgPfp.setAlpha(alpha);
        holder.txtDisplayName.setAlpha(alpha);
        holder.txtDescription.setAlpha(alpha);
        holder.txtSubmittedDate.setAlpha(alpha);
        holder.txtAcceptedDate.setAlpha(alpha);
    }

    private void loadImage(Drawable icon, ImageView imageView) {
        Glide.with(context)
                .load(icon)
                .into(imageView);
    }

    protected class ChatRequestHolder extends RecyclerView.ViewHolder {
        MaterialCardView body;
        ImageView imgPfp;
        TextView txtDisplayName;
        TextView txtDescription;
        TextView txtSubmittedDate;
        TextView txtAcceptedDate;

        public ChatRequestHolder(@NonNull View itemView) {
            super(itemView);
            body = itemView.findViewById(R.id.body);
            imgPfp = itemView.findViewById(R.id.img_pfp);
            txtDisplayName = itemView.findViewById(R.id.txt_display_name);
            txtDescription = itemView.findViewById(R.id.txt_description);
            txtSubmittedDate = itemView.findViewById(R.id.txt_submitted_date);
            txtAcceptedDate = itemView.findViewById(R.id.txt_accepted_date);
        }
    }
}

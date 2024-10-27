package com.example.emeowtions.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.emeowtions.R;
import com.example.emeowtions.models.ChatRequest;
import com.example.emeowtions.models.VeterinaryClinic;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class UserChatRequestAdapter extends FirestoreRecyclerAdapter<ChatRequest, UserChatRequestAdapter.UserChatRequestHolder> {
    private static final String TAG = "ChatRequestAdapter";

    public static final String KEY_CHAT_ID = "chatId";

    // Private variables
    private Context context;

    // Firebase variables
    private FirebaseAuthUtils firebaseAuthUtils;
    private FirebaseFirestore db;
    private CollectionReference usersRef;
    private CollectionReference clinicsRef;
    private CollectionReference chatRequestsRef;
    private CollectionReference chatsRef;

    public UserChatRequestAdapter(FirestoreRecyclerOptions<ChatRequest> options, Context context) {
        super(options);
        this.context = context;
        this.firebaseAuthUtils = new FirebaseAuthUtils();
        this.db = FirebaseFirestore.getInstance();
        this.usersRef = db.collection("users");
        this.clinicsRef = db.collection("veterinaryClinics");
        this.chatRequestsRef = db.collection("chatRequests");
        this.chatsRef = db.collection("chats");
    }

    @Override
    protected void onBindViewHolder(@NonNull UserChatRequestHolder holder, int position, @NonNull ChatRequest model) {
        SimpleDateFormat sdfDatetime = new SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault());

        // Set styles based on accepted state
        if (!model.isAccepted()) {
            holder.txtAcceptedDate.setVisibility(View.GONE);
        }

        // Populate chat request data
        clinicsRef.document(model.getVeterinaryClinicId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        VeterinaryClinic veterinaryClinic = documentSnapshot.toObject(VeterinaryClinic.class);

                        // Set fields
                        Glide.with(context.getApplicationContext())
                                .load(veterinaryClinic.getLogoUrl())
                                .into(holder.imgClinicLogo);
                        holder.txtClinicName.setText(veterinaryClinic.getName());
                        holder.txtDescription.setText(model.getDescription());
                        holder.txtSubmittedDate.setText(String.format("Submitted: %s", sdfDatetime.format(model.getCreatedAt().toDate())));
                        holder.txtAcceptedDate.setText(
                                model.isAccepted()
                                ? String.format("Accepted: %s", sdfDatetime.format(model.getUpdatedAt().toDate()))
                                : ""
                        );

                        // When chat request is clicked
                        holder.body.setOnClickListener(view -> {
                            // Open details dialog
                            View chatRequestDialogLayout = LayoutInflater.from(context).inflate(R.layout.dialog_user_chat_request, null);
                            TextView txtClinicName = chatRequestDialogLayout.findViewById(R.id.txt_clinic_name);
                            TextView txtClinicEmail = chatRequestDialogLayout.findViewById(R.id.txt_clinic_email);
                            TextView txtSubmittedDate = chatRequestDialogLayout.findViewById(R.id.txt_submitted_date);
                            TextView txtAcceptedDate = chatRequestDialogLayout.findViewById(R.id.txt_accepted_date);
                            TextView txtDescription = chatRequestDialogLayout.findViewById(R.id.txt_description);
                            LinearLayout lytSubmittedDate = chatRequestDialogLayout.findViewById(R.id.layout_submitted_date);
                            LinearLayout lytAcceptedDate = chatRequestDialogLayout.findViewById(R.id.layout_accepted_date);

                            // Set fields
                            txtClinicName.setText(veterinaryClinic.getName());
                            txtClinicEmail.setText(veterinaryClinic.getEmail());
                            txtSubmittedDate.setText(String.format("%s", sdfDatetime.format(model.getCreatedAt().toDate())));
                            txtAcceptedDate.setText(String.format("%s", sdfDatetime.format(model.getUpdatedAt().toDate())));
                            txtDescription.setText(model.getDescription());

                            MaterialAlertDialogBuilder chatRequestDialogBuilder =
                                    new MaterialAlertDialogBuilder(context)
                                            .setView(chatRequestDialogLayout)
                                            .setTitle(R.string.request_details)
                                            .setPositiveButton(R.string.close, (dialogInterface, i) -> {
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
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "onBindViewHolder: Failed to retrieve VeterinaryClinic data", e);
                });
    }

    @NonNull
    @Override
    public UserChatRequestHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.holder_user_chat_request, parent, false);
        return new UserChatRequestHolder(view);
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

    protected class UserChatRequestHolder extends RecyclerView.ViewHolder {
        MaterialCardView body;
        ImageView imgClinicLogo;
        TextView txtClinicName;
        TextView txtDescription;
        TextView txtSubmittedDate;
        TextView txtAcceptedDate;

        public UserChatRequestHolder(@NonNull View itemView) {
            super(itemView);
            body = itemView.findViewById(R.id.body);
            imgClinicLogo = itemView.findViewById(R.id.img_clinic_logo);
            txtClinicName = itemView.findViewById(R.id.txt_clinic_name);
            txtDescription = itemView.findViewById(R.id.txt_description);
            txtSubmittedDate = itemView.findViewById(R.id.txt_submitted_date);
            txtAcceptedDate = itemView.findViewById(R.id.txt_accepted_date);
        }
    }
}

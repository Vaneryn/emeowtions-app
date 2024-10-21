package com.example.emeowtions.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.emeowtions.R;
import com.example.emeowtions.models.User;
import com.example.emeowtions.models.Veterinarian;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

public class VeterinarianAdapter extends FirestoreRecyclerAdapter<Veterinarian, VeterinarianAdapter.VeterinarianHolder> {

    private static final String TAG = "VeterinarianAdapter";
    private Context context;

    // Firestore variables
    private FirebaseFirestore db;
    private CollectionReference usersRef;

    public VeterinarianAdapter(FirestoreRecyclerOptions<Veterinarian> options, Context context) {
        super(options);
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.usersRef = db.collection("users");
    }

    @Override
    protected void onBindViewHolder(@NonNull VeterinarianHolder holder, int position, @NonNull Veterinarian model) {
        // Query Veterinarian's user data
        usersRef.document(model.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "onBindViewHolder: Failed to retrieve Veterinarian's user data", task.getException());
                        return;
                    } else {
                        // Populate User data
                        User user = task.getResult().toObject(User.class);

                        if (user.getProfilePicture() == null) {
                            holder.imgPfp.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.baseline_person_24));
                            holder.imgPfp.setImageTintList(AppCompatResources.getColorStateList(context, R.color.white));
                        } else {
                            Glide.with(context)
                                    .load(user.getProfilePicture())
                                    .into(holder.imgPfp);
                        }

                        holder.txtDisplayName.setText(user.getDisplayName());
                    }
                });

        // Populate Veterinarian data
        holder.txtJobTitle.setText(model.getJobTitle());
        holder.txtQualification.setText(model.getQualification());
    }

    @NonNull
    @Override
    public VeterinarianHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.holder_veterinarian, parent, false);
        return new VeterinarianHolder(view);
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

    private void loadImage(Drawable icon, ImageView imageView) {
        Glide.with(context)
                .load(icon)
                .into(imageView);
    }

    protected class VeterinarianHolder extends RecyclerView.ViewHolder {
        ImageView imgPfp;
        TextView txtDisplayName;
        TextView txtJobTitle;
        TextView txtQualification;

        public VeterinarianHolder(@NonNull View itemView) {
            super(itemView);
            imgPfp = itemView.findViewById(R.id.img_profile_picture);
            txtDisplayName = itemView.findViewById(R.id.txt_display_name);
            txtJobTitle = itemView.findViewById(R.id.txt_job_title);
            txtQualification = itemView.findViewById(R.id.txt_qualification);
        }
    }
}

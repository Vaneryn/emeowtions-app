package com.example.emeowtions.adapters;

import android.content.Context;
import android.content.Intent;
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
import com.example.emeowtions.activities.admin.EditUserActivity;
import com.example.emeowtions.models.User;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestoreException;

public class UserAdapter extends FirestoreRecyclerAdapter<User, UserAdapter.UserHolder> {

    private static final String TAG = "UserAdapter";
    public static final String KEY_UID = "userId";

    private Context context;

    public UserAdapter(FirestoreRecyclerOptions<User> options, Context context) {
        super(options);
        this.context = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull UserHolder holder, int position, @NonNull User model) {
        // Populate data
        if (model.getProfilePicture() == null) {
            holder.imgProfilePicture.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.baseline_person_24));
            holder.imgProfilePicture.setImageTintList(AppCompatResources.getColorStateList(context, R.color.white));
        } else {
            Glide.with(context)
                    .load(model.getProfilePicture())
                    .into(holder.imgProfilePicture);
        }

        if (!model.isVerified()) {
            holder.imgVerified.setVisibility(View.GONE);
        }

        holder.txtDisplayName.setText(model.getDisplayName());
        holder.txtEmail.setText(model.getEmail());
        holder.txtRole.setText(model.getRole());

        // Redirect to EditUserActivity
        holder.userHolderBody.setOnClickListener(view -> {
            Intent intent = new Intent(context, EditUserActivity.class);
            intent.putExtra(KEY_UID, getSnapshots().getSnapshot(position).getId());
            context.startActivity(intent);
        });
    }

    @NonNull
    @Override
    public UserHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.holder_user, parent, false);
        return new UserHolder(view);
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

    protected class UserHolder extends RecyclerView.ViewHolder {
        MaterialCardView userHolderBody;
        ImageView imgProfilePicture;
        ImageView imgVerified;
        TextView txtDisplayName;
        TextView txtEmail;
        TextView txtRole;

        public UserHolder(@NonNull View itemView) {
            super(itemView);
            userHolderBody = itemView.findViewById(R.id.user_holder_body);
            imgProfilePicture = itemView.findViewById(R.id.img_profile_picture);
            imgVerified = itemView.findViewById(R.id.img_verified);
            txtDisplayName = itemView.findViewById(R.id.txt_display_name);
            txtEmail = itemView.findViewById(R.id.txt_email);
            txtRole = itemView.findViewById(R.id.txt_role);
        }
    }
}

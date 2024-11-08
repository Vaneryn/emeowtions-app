package com.example.emeowtions.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.emeowtions.R;
import com.example.emeowtions.activities.admin.AdminClinicProfileActivity;
import com.example.emeowtions.activities.admin.AdminMainActivity;
import com.example.emeowtions.activities.user.UserClinicProfileActivity;
import com.example.emeowtions.activities.user.UserMainActivity;
import com.example.emeowtions.models.VeterinaryClinic;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

public class VeterinaryClinicAdapter extends FirestoreRecyclerAdapter<VeterinaryClinic, VeterinaryClinicAdapter.VeterinaryClinicHolder> {

    private static final String TAG = "VeterinaryClinicAdapter";

    public static final int VIEW_TYPE_NORMAL = 0;
    public static final int VIEW_TYPE_TOP = 1;

    // Public variables
    public static final String KEY_CLINIC_ID = "clinicId";

    // Private variables
    private Context context;
    private int viewType;

    public VeterinaryClinicAdapter(FirestoreRecyclerOptions<VeterinaryClinic> options, Context context, int viewType) {
        super(options);
        this.context = context;
        this.viewType = viewType;
    }

    @Override
    protected void onBindViewHolder(@NonNull VeterinaryClinicHolder holder, int position, @NonNull VeterinaryClinic model) {
        // Populate data
        Glide.with(context)
                .load(model.getLogoUrl())
                .into(holder.imgClinicLogo);
        holder.txtName.setText(model.getName());
        holder.txtRating.setText(String.format("%.2f", model.getAverageRating()));
        holder.txtAddress.setText(model.getAddress());

        // Redirect to UserClinicProfileActivity (User) or AdminClinicProfileActivity (Admin)
        holder.clinicHolderBody.setOnClickListener(view -> {
            Intent intent;

            if (context instanceof UserMainActivity) {
                intent = new Intent(context, UserClinicProfileActivity.class);
            } else if (context instanceof AdminMainActivity) {
                intent = new Intent(context, AdminClinicProfileActivity.class);
            } else {
                // Default
                intent = new Intent(context, UserClinicProfileActivity.class);
            }

            intent.putExtra(KEY_CLINIC_ID, getSnapshots().getSnapshot(position).getId());
            context.startActivity(intent);
        });
    }

    @NonNull
    @Override
    public VeterinaryClinicAdapter.VeterinaryClinicHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;

        // Check the type of holder
        if (viewType == VIEW_TYPE_NORMAL) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.holder_clinic, parent, false);
        } else if (viewType == VIEW_TYPE_TOP) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.holder_top_clinic, parent, false);
        } else {
            // Default
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.holder_clinic, parent, false);
        }

        return new VeterinaryClinicHolder(view);
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

    @Override
    public int getItemViewType(int position) {
        if (this.viewType == VIEW_TYPE_NORMAL) {
            return VIEW_TYPE_NORMAL;
        } else if (this.viewType == VIEW_TYPE_TOP) {
            return VIEW_TYPE_TOP;
        }

        // Default
        return VIEW_TYPE_NORMAL;
    }

    private void loadImage(Drawable icon, ImageView imageView) {
        Glide.with(context)
                .load(icon)
                .into(imageView);
    }

    protected class VeterinaryClinicHolder extends RecyclerView.ViewHolder {
        MaterialCardView clinicHolderBody;
        ImageView imgClinicLogo;
        TextView txtName;
        TextView txtRating;
        TextView txtAddress;

        public VeterinaryClinicHolder(@NonNull View itemView) {
            super(itemView);
            clinicHolderBody = itemView.findViewById(R.id.clinic_holder_body);
            imgClinicLogo = itemView.findViewById(R.id.img_clinic_logo);
            txtName = itemView.findViewById(R.id.txt_name);
            txtRating = itemView.findViewById(R.id.txt_rating);
            txtAddress = itemView.findViewById(R.id.txt_address);
        }
    }
}

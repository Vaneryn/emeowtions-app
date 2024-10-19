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
import com.example.emeowtions.models.VeterinaryClinic;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestoreException;

public class VeterinaryClinicAdapter extends FirestoreRecyclerAdapter<VeterinaryClinic, VeterinaryClinicAdapter.VeterinaryClinicHolder> {

    private static final String TAG = "VeterinaryClinicAdapter";

    private Context context;
    public static final String KEY_CLINIC_ID = "clinicId";

    public VeterinaryClinicAdapter(FirestoreRecyclerOptions<VeterinaryClinic> options, Context context) {
        super(options);
        this.context = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull VeterinaryClinicHolder holder, int position, @NonNull VeterinaryClinic model) {
        // Populate data
        Glide.with(context)
                .load(model.getLogoUrl())
                .into(holder.imgClinicLogo);
        holder.txtName.setText(model.getName());
        holder.txtAddress.setText(model.getAddress());

        // Redirect to Clinic Profile when clicked
        holder.clinicHolderBody.setOnClickListener(view -> {
            Intent intent = new Intent(context, AdminClinicProfileActivity.class);
            intent.putExtra(KEY_CLINIC_ID, getSnapshots().getSnapshot(position).getId());
            context.startActivity(intent);
        });
    }

    @NonNull
    @Override
    public VeterinaryClinicAdapter.VeterinaryClinicHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.holder_clinic, parent, false);
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

    private void loadImage(Drawable icon, ImageView imageView) {
        Glide.with(context)
                .load(icon)
                .into(imageView);
    }

    protected class VeterinaryClinicHolder extends RecyclerView.ViewHolder {
        MaterialCardView clinicHolderBody;
        ImageView imgClinicLogo;
        TextView txtName;
        TextView txtAddress;

        public VeterinaryClinicHolder(@NonNull View itemView) {
            super(itemView);
            clinicHolderBody = itemView.findViewById(R.id.clinic_holder_body);
            imgClinicLogo = itemView.findViewById(R.id.img_clinic_logo);
            txtName = itemView.findViewById(R.id.txt_name);
            txtAddress = itemView.findViewById(R.id.txt_address);
        }
    }
}

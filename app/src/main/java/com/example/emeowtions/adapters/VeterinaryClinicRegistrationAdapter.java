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
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.emeowtions.R;
import com.example.emeowtions.activities.admin.ClinicRegistrationDetailsActivity;
import com.example.emeowtions.models.VeterinaryClinicRegistration;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class VeterinaryClinicRegistrationAdapter extends FirestoreRecyclerAdapter<VeterinaryClinicRegistration, VeterinaryClinicRegistrationAdapter.VeterinaryClinicRegistrationHolder> {

    private static final String TAG = "VeterinaryClinicRegistrationAdapter";
    public static final String KEY_VET_REG_ID = "vetRegId";

    private Context context;

    public VeterinaryClinicRegistrationAdapter(FirestoreRecyclerOptions<VeterinaryClinicRegistration> options, Context context) {
        super(options);
        this.context = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull VeterinaryClinicRegistrationHolder holder, int position, @NonNull VeterinaryClinicRegistration model) {
        // Populate data
        SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault());

        Glide.with(context)
                .load(model.getLogoUrl())
                .into(holder.imgClinicLogo);
        holder.txtName.setText(model.getName());
        holder.txtAddress.setText(model.getAddress());
        holder.txtUpdatedDate.setText(sdf.format(model.getUpdatedAt().toDate()));

        // Redirect to VeterinaryClinicRegistrationActivity
        holder.vetRegHolderBody.setOnClickListener(view -> {
            Intent intent = new Intent(context, ClinicRegistrationDetailsActivity.class);
            intent.putExtra(KEY_VET_REG_ID, getSnapshots().getSnapshot(position).getId());
            context.startActivity(intent);
        });
    }

    @NonNull
    @Override
    public VeterinaryClinicRegistrationAdapter.VeterinaryClinicRegistrationHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.holder_vet_clinic_registration, parent, false);
        return new VeterinaryClinicRegistrationHolder(view);
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

    protected class VeterinaryClinicRegistrationHolder extends RecyclerView.ViewHolder {
        MaterialCardView vetRegHolderBody;
        ImageView imgClinicLogo;
        TextView txtName;
        TextView txtAddress;
        TextView txtUpdatedDate;

        public VeterinaryClinicRegistrationHolder(@NonNull View itemView) {
            super(itemView);
            vetRegHolderBody = itemView.findViewById(R.id.vet_clinic_registration_holder_body);
            imgClinicLogo = itemView.findViewById(R.id.img_clinic_logo);
            txtName = itemView.findViewById(R.id.txt_name);
            txtAddress = itemView.findViewById(R.id.txt_address);
            txtUpdatedDate = itemView.findViewById(R.id.txt_updated_date);
        }
    }
}

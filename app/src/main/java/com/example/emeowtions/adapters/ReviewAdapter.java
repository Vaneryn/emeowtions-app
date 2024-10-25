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
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.emeowtions.R;
import com.example.emeowtions.activities.veterinary.ClinicReviewsActivity;
import com.example.emeowtions.models.Review;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class ReviewAdapter extends FirestoreRecyclerAdapter<Review, ReviewAdapter.ReviewHolder> {

    private static final String TAG = "ReviewAdapter";
    private Context context;

    private FirebaseFirestore db;
    private CollectionReference clinicsRef;
    private CollectionReference reviewsRef;

    public ReviewAdapter(FirestoreRecyclerOptions<Review> options, Context context) {
        super(options);
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.clinicsRef = db.collection("veterinaryClinics");
    }

    @Override
    protected void onBindViewHolder(@NonNull ReviewHolder holder, int position, @NonNull Review model) {
        SimpleDateFormat sdfDate = new SimpleDateFormat("d MMM yyyy", Locale.getDefault());
        SimpleDateFormat sdfDatetime = new SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault());

        // Set style based on read status (only for ClinicReviewsActivity)
        if (context instanceof ClinicReviewsActivity && model.isRead())
            setViewOpacity(holder, 0.6f);
        else
            setViewOpacity(holder, 1);

        // Populate review data
        if (model.getUserPfpUrl() == null) {
            loadImage(ContextCompat.getDrawable(context, R.drawable.baseline_person_24), holder.imgPfp);
        } else {
            Glide.with(context.getApplicationContext())
                    .load(model.getUserPfpUrl())
                    .into(holder.imgPfp);
        }

        holder.txtDisplayName.setText(model.getUserDisplayName() == null ? model.getUserEmail().split("@")[0] : model.getUserDisplayName());
        holder.txtDescription.setText(model.getDescription());
        holder.txtRating.setText(String.format("%s", model.getRating()));
        holder.txtCreatedDate.setText(String.format("%s", sdfDate.format(model.getCreatedAt().toDate())));

        // When review is clicked
        holder.body.setOnClickListener(view -> {
            // Open details dialog
            View reviewDialogLayout = LayoutInflater.from(context).inflate(R.layout.dialog_review_details, null);
            TextView txtDisplayName = reviewDialogLayout.findViewById(R.id.txt_display_name);
            TextView txtEmail = reviewDialogLayout.findViewById(R.id.txt_email);
            TextView txtSubmitted = reviewDialogLayout.findViewById(R.id.txt_created_date);
            TextView txtRating = reviewDialogLayout.findViewById(R.id.txt_rating);
            TextView txtDescription = reviewDialogLayout.findViewById(R.id.txt_description);

            // Set fields
            txtDisplayName.setText(model.getUserDisplayName());
            txtEmail.setText(model.getUserEmail());
            txtSubmitted.setText(String.format("%s", sdfDatetime.format(model.getCreatedAt().toDate())));
            txtRating.setText(String.format("%s", model.getRating()));
            txtDescription.setText(model.getDescription());

            MaterialAlertDialogBuilder reviewDialogBuilder =
                    new MaterialAlertDialogBuilder(context)
                            .setView(reviewDialogLayout)
                            .setTitle(R.string.review_details)
                            .setPositiveButton(R.string.close, (dialogInterface, i) -> {
                                // Unused
                            });

            AlertDialog reviewDialog = reviewDialogBuilder.create();
            reviewDialog.show();

            // Logic related to user role
            if (context instanceof ClinicReviewsActivity) {
                // Only update "read" status in Veterinary view of clinic reviews
                if (!model.isRead()) {
                    this.reviewsRef = clinicsRef.document(model.getVeterinaryClinicId()).collection("reviews");

                    reviewsRef.document(getSnapshots().getSnapshot(position).getId())
                            .update("read", true, "updatedAt", Timestamp.now())
                            .addOnSuccessListener(unused -> {
                                notifyItemChanged(position);
                                setViewOpacity(holder, 0.6f);
                            })
                            .addOnFailureListener(e -> {
                                Log.w(TAG, "onBindViewHolder: Failed to update Review read status", e);
                                Toast.makeText(context, "Unable to update status of Review to read.", Toast.LENGTH_SHORT).show();
                            });
                }
            } else {
                // Hide certain fields for User view of clinic review dialog
                LinearLayout layoutEmail = reviewDialogLayout.findViewById(R.id.layout_email);
                LinearLayout layoutReviewDatetimes = reviewDialogLayout.findViewById(R.id.layout_review_datetimes);
                layoutEmail.setVisibility(View.GONE);
                layoutReviewDatetimes.setVisibility(View.GONE);
            }
        });
    }

    @NonNull
    @Override
    public ReviewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.holder_clinic_review, parent, false);
        return new ReviewHolder(view);
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

    private void setViewOpacity(ReviewHolder holder, float alpha) {
        holder.body.setAlpha(alpha);
        holder.imgPfp.setAlpha(alpha);
        holder.imgStar.setAlpha(alpha);
        holder.txtDisplayName.setAlpha(alpha);
        holder.txtDescription.setAlpha(alpha);
        holder.txtRating.setAlpha(alpha);
        holder.txtCreatedDate.setAlpha(alpha);
    }

    private void loadImage(Drawable icon, ImageView imageView) {
        Glide.with(context)
                .load(icon)
                .into(imageView);
    }

    protected class ReviewHolder extends RecyclerView.ViewHolder {
        MaterialCardView body;
        ImageView imgPfp;
        ImageView imgStar;
        TextView txtDisplayName;
        TextView txtDescription;
        TextView txtRating;
        TextView txtCreatedDate;

        public ReviewHolder(@NonNull View itemView) {
            super(itemView);
            body = itemView.findViewById(R.id.body);
            imgPfp = itemView.findViewById(R.id.img_pfp);
            imgStar = itemView.findViewById(R.id.img_star);
            txtDisplayName = itemView.findViewById(R.id.txt_display_name);
            txtDescription = itemView.findViewById(R.id.txt_description);
            txtRating = itemView.findViewById(R.id.txt_rating);
            txtCreatedDate = itemView.findViewById(R.id.txt_created_date);
        }
    }
}

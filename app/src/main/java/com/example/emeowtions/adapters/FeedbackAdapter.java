package com.example.emeowtions.adapters;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.emeowtions.R;
import com.example.emeowtions.models.Feedback;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class FeedbackAdapter extends FirestoreRecyclerAdapter<Feedback, FeedbackAdapter.FeedbackHolder> {

    private static final String TAG = "FeedbackAdapter";

    // Public variables
    public static final String KEY_FEEDBACK_ID = "feedbackId";

    // Private variables
    private Context context;

    public FeedbackAdapter(FirestoreRecyclerOptions<Feedback> options, Context context) {
        super(options);
        this.context = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull FeedbackHolder holder, int position, @NonNull Feedback model) {
        SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault());

        // Populate data
        if (model.getUserProfilePicture() == null) {
            loadImage(ContextCompat.getDrawable(context, R.drawable.baseline_person_24), holder.imgPfp);
        } else {
            Glide.with(context)
                    .load(model.getUserProfilePicture())
                    .into(holder.imgPfp);
        }

        holder.txtDisplayName.setText(model.getUserDisplayName());
        holder.txtRating.setText(String.format("%s", model.getRating()));
        holder.txtDescription.setText(model.getDescription());
        holder.txtCreatedDate.setText(sdf.format(model.getCreatedAt().toDate()));

        // Open details dialog
        holder.body.setOnClickListener(view -> {
            View feedbackDialogLayout = LayoutInflater.from(context).inflate(R.layout.dialog_general_feedback, null);
            TextView txtDisplayName = feedbackDialogLayout.findViewById(R.id.txt_display_name);
            TextView txtRating = feedbackDialogLayout.findViewById(R.id.txt_rating);
            TextView txtDescription = feedbackDialogLayout.findViewById(R.id.txt_description);

            txtDisplayName.setText(model.getUserDisplayName());
            txtRating.setText(String.format("%s", model.getRating()));
            txtDescription.setText(model.getDescription());

            MaterialAlertDialogBuilder feedbackDialogBuilder =
                    new MaterialAlertDialogBuilder(context)
                            .setView(feedbackDialogLayout)
                            .setTitle(R.string.feedback_details)
                            .setPositiveButton(R.string.close, (dialogInterface, i) -> {
                                // Unused
                            });

            AlertDialog feedbackDialog = feedbackDialogBuilder.create();
            feedbackDialog.show();
        });
    }

    @NonNull
    @Override
    public FeedbackAdapter.FeedbackHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.holder_general_feedback, parent, false);
        return new FeedbackHolder(view);
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

    private void loadImage(Drawable image, ImageView imageView) {
        Glide.with(context)
                .load(image)
                .into(imageView);
    }

    protected class FeedbackHolder extends RecyclerView.ViewHolder {
        MaterialCardView body;
        ImageView imgPfp;
        TextView txtDisplayName;
        TextView txtRating;
        TextView txtDescription;
        TextView txtCreatedDate;

        public FeedbackHolder(@NonNull View itemView) {
            super(itemView);
            body = itemView.findViewById(R.id.body);
            imgPfp = itemView.findViewById(R.id.img_pfp);
            txtDisplayName = itemView.findViewById(R.id.txt_display_name);
            txtRating = itemView.findViewById(R.id.txt_rating);
            txtDescription = itemView.findViewById(R.id.txt_description);
            txtCreatedDate = itemView.findViewById(R.id.txt_created_date);
        }
    }
}

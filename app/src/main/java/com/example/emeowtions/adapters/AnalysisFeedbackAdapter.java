package com.example.emeowtions.adapters;

import static androidx.core.content.ContextCompat.startActivity;

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
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.emeowtions.R;
import com.example.emeowtions.activities.admin.ViewAnalysisFeedbackActivity;
import com.example.emeowtions.models.AnalysisFeedback;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class AnalysisFeedbackAdapter extends FirestoreRecyclerAdapter<AnalysisFeedback, AnalysisFeedbackAdapter.AnalysisFeedbackHolder> {

    private static final String TAG = "AnalysisFeedbackAdapter";

    // Public variables
    public static final String KEY_ANALYSIS_ID = "analysisId";
    public static final String KEY_ANALYSIS_FEEDBACK_ID = "analysisFeedbackId";
    public static final String KEY_USER_DISPLAY_NAME = "userDisplayName";
    public static final String KEY_RATING = "rating";
    public static final String KEY_DESCRIPTION = "description";
    public static final String KEY_READ = "read";

    // Firebase variables
    private FirebaseFirestore db;
    private CollectionReference analysisFeedbackRef;

    // Private variables
    private Context context;

    public AnalysisFeedbackAdapter(FirestoreRecyclerOptions<AnalysisFeedback> options, Context context) {
        super(options);
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.analysisFeedbackRef = db.collection("analysisFeedback");
    }

    @Override
    protected void onBindViewHolder(@NonNull AnalysisFeedbackHolder holder, int position, @NonNull AnalysisFeedback model) {
        SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault());

        // Set style based on read status
        if (model.isRead())
            setViewOpacity(holder, 0.6f);
        else
            setViewOpacity(holder, 1);

        // Populate data
        if (model.getUserPfpUrl() == null) {
            loadImage(ContextCompat.getDrawable(context, R.drawable.baseline_person_24), holder.imgPfp);
        } else {
            Glide.with(context)
                    .load(model.getUserPfpUrl())
                    .into(holder.imgPfp);
        }

        holder.txtDisplayName.setText(model.getUserDisplayName());
        holder.txtRating.setText(String.format("%s", model.getRating()));
        holder.txtDescription.setText(model.getDescription());
        holder.txtCreatedDate.setText(sdf.format(model.getCreatedAt().toDate()));

        holder.body.setOnClickListener(view -> {
            // Redirect to ViewAnalysisFeedbackActivity
            Intent intent = new Intent(context, ViewAnalysisFeedbackActivity.class);
            intent.putExtra(KEY_ANALYSIS_ID, model.getAnalysisId());
            intent.putExtra(KEY_ANALYSIS_FEEDBACK_ID, getSnapshots().getSnapshot(position).getId());
            intent.putExtra(KEY_USER_DISPLAY_NAME, model.getUserDisplayName());
            intent.putExtra(KEY_RATING, model.getRating());
            intent.putExtra(KEY_DESCRIPTION, model.getDescription());
            intent.putExtra(KEY_READ, model.isRead());
            context.startActivity(intent);
        });
    }

    @NonNull
    @Override
    public AnalysisFeedbackAdapter.AnalysisFeedbackHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.holder_analysis_feedback, parent, false);
        return new AnalysisFeedbackHolder(view);
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

    private void setViewOpacity(AnalysisFeedbackHolder holder, float alpha) {
        holder.body.setAlpha(alpha);
        holder.imgPfp.setAlpha(alpha);
        holder.imgStar.setAlpha(alpha);
        holder.txtDisplayName.setAlpha(alpha);
        holder.txtDescription.setAlpha(alpha);
        holder.txtRating.setAlpha(alpha);
        holder.txtCreatedDate.setAlpha(alpha);
    }

    private void loadImage(Drawable image, ImageView imageView) {
        Glide.with(context)
                .load(image)
                .into(imageView);
    }

    protected class AnalysisFeedbackHolder extends RecyclerView.ViewHolder {
        MaterialCardView body;
        ImageView imgPfp;
        ImageView imgStar;
        TextView txtDisplayName;
        TextView txtRating;
        TextView txtDescription;
        TextView txtCreatedDate;

        public AnalysisFeedbackHolder(@NonNull View itemView) {
            super(itemView);
            body = itemView.findViewById(R.id.body);
            imgPfp = itemView.findViewById(R.id.img_pfp);
            imgStar = itemView.findViewById(R.id.img_star);
            txtDisplayName = itemView.findViewById(R.id.txt_display_name);
            txtRating = itemView.findViewById(R.id.txt_rating);
            txtDescription = itemView.findViewById(R.id.txt_description);
            txtCreatedDate = itemView.findViewById(R.id.txt_created_date);
        }
    }
}

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
import com.example.emeowtions.activities.user.ViewSavedAnalysisActivity;
import com.example.emeowtions.models.Analysis;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class AnalysisAdapter extends FirestoreRecyclerAdapter<Analysis, AnalysisAdapter.AnalysisHolder> {

    private static final String TAG = "AnalysisAdapter";

    // Public variables
    public static final String KEY_ANALYSIS_ID = "analysisId";

    // Private variables
    private Context context;

    public AnalysisAdapter(FirestoreRecyclerOptions<Analysis> options, Context context) {
        super(options);
        this.context = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull AnalysisHolder holder, int position, @NonNull Analysis model) {
        SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy", Locale.getDefault());

        // Populate data
        Glide.with(context)
                .load(model.getImageUrl())
                .into(holder.imgAnalyzedImage);
        holder.txtCatName.setText(model.getCatName());
        holder.txtEmotion.setText(model.getEmotion());
        holder.txtCreatedDate.setText(sdf.format(model.getCreatedAt()));

        // Redirect to ViewSavedAnalysisActivity
        holder.body.setOnClickListener(view -> {
            Intent intent = new Intent(context, ViewSavedAnalysisActivity.class);
            intent.putExtra(KEY_ANALYSIS_ID, getSnapshots().getSnapshot(position).getId());
            context.startActivity(intent);
        });
    }

    @NonNull
    @Override
    public AnalysisAdapter.AnalysisHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.holder_analysis, parent, false);
        return new AnalysisHolder(view);
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

    protected class AnalysisHolder extends RecyclerView.ViewHolder {
        MaterialCardView body;
        ImageView imgAnalyzedImage;
        TextView txtCatName;
        TextView txtEmotion;
        TextView txtCreatedDate;

        public AnalysisHolder(@NonNull View itemView) {
            super(itemView);
            body = itemView.findViewById(R.id.body);
            imgAnalyzedImage = itemView.findViewById(R.id.img_analyzed_image);
            txtCatName = itemView.findViewById(R.id.txt_cat_name);
            txtEmotion = itemView.findViewById(R.id.txt_emotion);
            txtCreatedDate = itemView.findViewById(R.id.txt_created_date);
        }
    }
}

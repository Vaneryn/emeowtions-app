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
import com.example.emeowtions.models.BehaviourStrategy;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class RecommendationRatingAdapter extends FirestoreRecyclerAdapter<BehaviourStrategy, RecommendationRatingAdapter.BehaviourStrategyHolder> {

    private static final String TAG = "RecommendationRatingAdapter";

    // Public variables
    public static final String KEY_STRAT_ID = "stratId";

    // Private variables
    private Context context;

    public RecommendationRatingAdapter(FirestoreRecyclerOptions<BehaviourStrategy> options, Context context) {
        super(options);
        this.context = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull BehaviourStrategyHolder holder, int position, @NonNull BehaviourStrategy model) {
        // Set icons
        if (model.getFactorType() == null) {
            loadRecommendationIcon(AppCompatResources.getDrawable(context, R.drawable.outline_happy_cat), holder.imgRecommendationIcon);
        } else {
            if (model.getFactorType().equals("Age")) {
                loadRecommendationIcon(AppCompatResources.getDrawable(context, R.drawable.baseline_event_24), holder.imgRecommendationIcon);
            } else if (model.getFactorType().equals("Gender")) {
                loadRecommendationIcon(AppCompatResources.getDrawable(context, R.drawable.baseline_transgender_24), holder.imgRecommendationIcon);
            } else if (model.getFactorType().equals("Temperament")) {
                loadRecommendationIcon(AppCompatResources.getDrawable(context, R.drawable.baseline_pets_24), holder.imgRecommendationIcon);
            } else if (model.getFactorType().equals("Activity Level")) {
                loadRecommendationIcon(AppCompatResources.getDrawable(context, R.drawable.baseline_directions_run_24), holder.imgRecommendationIcon);
            } else if (model.getFactorType().equals("Background")) {
                loadRecommendationIcon(AppCompatResources.getDrawable(context, R.drawable.baseline_nature_people_24), holder.imgRecommendationIcon);
            } else if (model.getFactorType().equals("Medical Conditions")) {
                loadRecommendationIcon(AppCompatResources.getDrawable(context, R.drawable.baseline_medical_information_24), holder.imgRecommendationIcon);
            }
        }

        if (model.getEmotionType().equals("Positive")) {
            holder.imgEmotionType.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.baseline_add_circle_outline_24));
            holder.imgEmotionType.setImageTintList(AppCompatResources.getColorStateList(context, R.color.quaternary_300));
        } else if (model.getEmotionType().equals("Negative")) {
            holder.imgEmotionType.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.baseline_remove_circle_outline_24));
            holder.imgEmotionType.setImageTintList(AppCompatResources.getColorStateList(context, R.color.error_3));
        }

        // Populate data
        long likeCount = model.getLikeCount();
        long dislikeCount = model.getDislikeCount();
        long totalCount = likeCount + dislikeCount;
        int likePercentage = totalCount == 0 ? 0 : ((int) (likeCount / totalCount) * 100);
        int dislikePercentage = totalCount == 0 ? 0 : ((int) (dislikeCount / totalCount) * 100);

        if (model.getFactorType() == null) {
            holder.txtFactorType.setText(R.string.none);
            holder.txtFactorValue.setText(R.string.general);
        } else {
            holder.txtFactorType.setText(model.getFactorType());
            holder.txtFactorValue.setText(model.getFactorValue());
        }

        holder.txtDescription.setText(model.getDescription());
        holder.txtLikeCount.setText(String.format("%s", likeCount));
        holder.txtLikePercentage.setText(String.format("%s%%", likePercentage));
        holder.txtDislikeCount.setText(String.format("%s", dislikeCount));
        holder.txtDislikePercentage.setText(String.format("%s%%", dislikePercentage));
    }

    @NonNull
    @Override
    public RecommendationRatingAdapter.BehaviourStrategyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.holder_recommendation_rating, parent, false);
        return new BehaviourStrategyHolder(view);
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

    private void loadRecommendationIcon(Drawable icon, ImageView imageView) {
        imageView.setImageDrawable(icon);
        imageView.setImageTintList(AppCompatResources.getColorStateList(context, R.color.white));
    }

    protected class BehaviourStrategyHolder extends RecyclerView.ViewHolder {
        ImageView imgRecommendationIcon;
        ImageView imgEmotionType;
        TextView txtFactorType;
        TextView txtFactorValue;
        TextView txtDescription;
        TextView txtLikeCount;
        TextView txtLikePercentage;
        TextView txtDislikeCount;
        TextView txtDislikePercentage;

        public BehaviourStrategyHolder(@NonNull View itemView) {
            super(itemView);
            imgRecommendationIcon = itemView.findViewById(R.id.img_recommendation_icon);
            imgEmotionType = itemView.findViewById(R.id.img_emotion_type);
            txtFactorType = itemView.findViewById(R.id.txt_factor_type);
            txtFactorValue = itemView.findViewById(R.id.txt_factor_value);
            txtDescription = itemView.findViewById(R.id.txt_description);
            txtLikeCount = itemView.findViewById(R.id.txt_like_count);
            txtLikePercentage = itemView.findViewById(R.id.txt_like_percentage);
            txtDislikeCount = itemView.findViewById(R.id.txt_dislike_count);
            txtDislikePercentage = itemView.findViewById(R.id.txt_dislike_percentage);
        }
    }
}

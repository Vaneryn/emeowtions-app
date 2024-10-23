package com.example.emeowtions.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import com.example.emeowtions.R;
import com.example.emeowtions.models.BehaviourStrategy;

import java.util.ArrayList;

public class BehaviourStrategyAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "BehaviourStrategyAdapter";

    private ArrayList<BehaviourStrategy> stratList;
    private Context context;

    public BehaviourStrategyAdapter(ArrayList<BehaviourStrategy> stratList, Context context) {
        this.stratList = stratList;
        this.context = context;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.holder_behaviour_strategy, parent, false);
        return new BehaviourStrategyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        BehaviourStrategy item = stratList.get(position);
        BehaviourStrategyHolder stratHolder = (BehaviourStrategyHolder) holder;

        // Set icon
        if (item.getFactorType() == null) {
            loadIcon(AppCompatResources.getDrawable(context, R.drawable.outline_happy_cat), stratHolder.imgIcon);
        } else {
            if (item.getFactorType().equals("Age")) {
                loadIcon(AppCompatResources.getDrawable(context, R.drawable.baseline_event_24), stratHolder.imgIcon);
            } else if (item.getFactorType().equals("Gender")) {
                loadIcon(AppCompatResources.getDrawable(context, R.drawable.baseline_transgender_24), stratHolder.imgIcon);
            } else if (item.getFactorType().equals("Temperament")) {
                loadIcon(AppCompatResources.getDrawable(context, R.drawable.baseline_pets_24), stratHolder.imgIcon);
            } else if (item.getFactorType().equals("Activity Level")) {
                loadIcon(AppCompatResources.getDrawable(context, R.drawable.baseline_directions_run_24), stratHolder.imgIcon);
            } else if (item.getFactorType().equals("Background")) {
                loadIcon(AppCompatResources.getDrawable(context, R.drawable.baseline_nature_people_24), stratHolder.imgIcon);
            } else if (item.getFactorType().equals("Medical Conditions")) {
                loadIcon(AppCompatResources.getDrawable(context, R.drawable.baseline_medical_information_24), stratHolder.imgIcon);
            }
        }

        // Populate data
        long totalCount = item.getLikeCount() + item.getDislikeCount();
        int percentage = totalCount == 0 ? 0 : ((int) (item.getLikeCount() / totalCount) * 100);

        if (item.getFactorType() == null) {
            stratHolder.txtFactorType.setText(R.string.none);
            stratHolder.txtFactorValue.setText(R.string.general);
        } else {
            stratHolder.txtFactorType.setText(item.getFactorType());
            stratHolder.txtFactorValue.setText(item.getFactorValue());
        }

        stratHolder.txtDescription.setText(item.getDescription());
        stratHolder.txtRating.setText(String.format("%s%%", percentage));
    }

    private void loadIcon(Drawable icon, ImageView imageView) {
        imageView.setImageDrawable(icon);
        imageView.setImageTintList(AppCompatResources.getColorStateList(context, R.color.white));
    }

    @Override
    public int getItemCount() {
        return stratList.size();
    }

    private class BehaviourStrategyHolder extends RecyclerView.ViewHolder {
        ImageView imgIcon;
        TextView txtFactorType;
        TextView txtFactorValue;
        TextView txtDescription;
        TextView txtRating;

        public BehaviourStrategyHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.img_recommendation_icon);
            txtFactorType = itemView.findViewById(R.id.txt_factor_type);
            txtFactorValue = itemView.findViewById(R.id.txt_factor_value);
            txtDescription = itemView.findViewById(R.id.txt_description);
            txtRating = itemView.findViewById(R.id.txt_rating);
        }
    }
}

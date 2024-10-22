package com.example.emeowtions.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.emeowtions.R;
import com.example.emeowtions.models.BodyLanguage;
import com.example.emeowtions.utils.BodyLanguageUtils;

import java.util.ArrayList;

public class BodyLanguageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "BodyLanguageAdapter";

    private ArrayList<BodyLanguage> bodyLanguageList;
    private Context context;

    public BodyLanguageAdapter(ArrayList<BodyLanguage> bodyLanguageList, Context context) {
        this.bodyLanguageList = bodyLanguageList;
        this.context = context;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.holder_body_language, parent, false);
        return new BodyLanguageHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        BodyLanguage item = bodyLanguageList.get(position);
        BodyLanguageHolder bodyLanguageHolder = (BodyLanguageHolder) holder;

        // Set icon
        BodyLanguageUtils bodyLanguageUtils = new BodyLanguageUtils(context);

        Log.d(TAG, "onBindViewHolder: " + item.getKey().split("_")[0]);

        if (bodyLanguageUtils.isEar(item.getKey())) {
            loadIcon(AppCompatResources.getDrawable(context, R.drawable.outline_ear_24), bodyLanguageHolder.imgIcon);
        } else if (bodyLanguageUtils.isEye(item.getKey())) {
            loadIcon(AppCompatResources.getDrawable(context, R.drawable.baseline_remove_red_eye_24), bodyLanguageHolder.imgIcon);
        } else if (bodyLanguageUtils.isFang(item.getKey())) {
            loadIcon(AppCompatResources.getDrawable(context, R.drawable.baseline_fangs_24), bodyLanguageHolder.imgIcon);
        } else if (bodyLanguageUtils.isPosture(item.getKey())) {
            loadIcon(AppCompatResources.getDrawable(context, R.drawable.baseline_full_cat_24), bodyLanguageHolder.imgIcon);
        } else if (bodyLanguageUtils.isTail(item.getKey())) {
            loadIcon(AppCompatResources.getDrawable(context, R.drawable.outline_foxtail_24), bodyLanguageHolder.imgIcon);
        }

        // Populate data
        bodyLanguageHolder.txtValue.setText(item.getValue());
        bodyLanguageHolder.txtDescription.setText(item.getDescription());
        bodyLanguageHolder.txtProbability.setText(String.format("%s%%", item.getProbability()));
    }

    private void loadIcon(Drawable icon, ImageView imageView) {
        imageView.setImageDrawable(icon);
        imageView.setImageTintList(AppCompatResources.getColorStateList(context, R.color.white));
    }

    @Override
    public int getItemCount() {
        return bodyLanguageList.size();
    }

    private class BodyLanguageHolder extends RecyclerView.ViewHolder {
        ImageView imgIcon;
        TextView txtValue;
        TextView txtDescription;
        TextView txtProbability;

        public BodyLanguageHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.img_body_language_icon);
            txtValue = itemView.findViewById(R.id.txt_value);
            txtDescription = itemView.findViewById(R.id.txt_description);
            txtProbability = itemView.findViewById(R.id.txt_probability);
        }
    }
}

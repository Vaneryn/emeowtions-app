package com.example.emeowtions.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.emeowtions.R;
import com.example.emeowtions.activities.user.GuidesActivity;

import java.util.ArrayList;

public class CarouselDiscoverItemAdapter extends RecyclerView.Adapter<CarouselDiscoverItemAdapter.ViewHolder> {

    private static final String TAG = "CarouselDiscoverItemAdapter";

    // Public variables
    public static final int POSITION_EMOTION_ANALYSIS = 0;
    public static final int POSITION_BODY_LANGUAGE = 1;
    public static final int POSITION_RECOMMENDATIONS = 2;
    public static final String KEY_POSITION = "position";

    // Private variables
    private Context context;
    private ArrayList<String> imagePaths;

    public CarouselDiscoverItemAdapter(Context context, ArrayList<String> imagePaths) {
        this.context = context;
        this.imagePaths = imagePaths;
    }

    @NonNull
    @Override
    public CarouselDiscoverItemAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.holder_carousel_discover_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Glide.with(context).load(imagePaths.get(position)).into(holder.imageView);

        if (position == POSITION_EMOTION_ANALYSIS) {
            holder.title.setText(R.string.emotion_analysis);
        } else if (position == POSITION_BODY_LANGUAGE) {
            holder.title.setText(R.string.body_language);
        } else if (position == POSITION_RECOMMENDATIONS) {
            holder.title.setText(R.string.recommendations);
        }

        holder.itemView.setOnClickListener(view -> {
            Intent intent = new Intent(context, GuidesActivity.class);
            intent.putExtra(KEY_POSITION, position);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return imagePaths.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView title;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.carousel_image_view);
            title = itemView.findViewById(R.id.txt_carousel_item_title);
        }
    }
}

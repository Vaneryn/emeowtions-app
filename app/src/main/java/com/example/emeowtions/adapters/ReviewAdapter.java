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
import com.example.emeowtions.models.Review;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class ReviewAdapter extends FirestoreRecyclerAdapter<Review, ReviewAdapter.ReviewHolder> {

    private static final String TAG = "ReviewAdapter";
    private Context context;

    public ReviewAdapter(FirestoreRecyclerOptions<Review> options, Context context) {
        super(options);
        this.context = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull ReviewHolder holder, int position, @NonNull Review model) {
        SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy", Locale.getDefault());

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
        holder.txtDate.setText(String.format("%s", sdf.format(model.getUpdatedAt().toDate())));
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

    private void loadImage(Drawable icon, ImageView imageView) {
        Glide.with(context)
                .load(icon)
                .into(imageView);
    }

    protected class ReviewHolder extends RecyclerView.ViewHolder {
        ImageView imgPfp;
        TextView txtDisplayName;
        TextView txtDescription;
        TextView txtRating;
        TextView txtDate;

        public ReviewHolder(@NonNull View itemView) {
            super(itemView);
            imgPfp = itemView.findViewById(R.id.img_pfp);
            txtDisplayName = itemView.findViewById(R.id.txt_display_name);
            txtDescription = itemView.findViewById(R.id.txt_description);
            txtRating = itemView.findViewById(R.id.txt_rating);
            txtDate = itemView.findViewById(R.id.txt_created_date);
        }
    }
}

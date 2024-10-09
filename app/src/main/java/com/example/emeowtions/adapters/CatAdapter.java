package com.example.emeowtions.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.emeowtions.R;
import com.example.emeowtions.activities.user.CatProfileActivity;
import com.example.emeowtions.enums.Gender;
import com.example.emeowtions.models.Cat;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class CatAdapter extends FirestoreRecyclerAdapter<Cat, CatAdapter.CatHolder> {

    private static final String TAG = "CatAdapter";

    private Context context;
    public static final String KEY_CAT_ID = "catId";

    public CatAdapter(FirestoreRecyclerOptions<Cat> options, Context context) {
        super(options);
        this.context = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull CatHolder holder, int position, @NonNull Cat model) {
        // Populate data
        if (model.getProfilePicture() == null) {
            holder.imgCatProfilePicture.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.baseline_emeowtions_24));
            holder.imgCatProfilePicture.setImageTintList(AppCompatResources.getColorStateList(context, R.color.white));
        } else {
            Glide.with(context)
                    .load(model.getProfilePicture())
                    .into(holder.imgCatProfilePicture);
        }

        if (model.getGender().equals(Gender.MALE.getTitle())) {
            loadImage(AppCompatResources.getDrawable(context, R.drawable.baseline_male_24), holder.imgGenderIcon);
            holder.imgGenderIcon.setImageTintList(AppCompatResources.getColorStateList(context, R.color.sky_blue));
        } else if (model.getGender().equals(Gender.FEMALE.getTitle())) {
            loadImage(AppCompatResources.getDrawable(context, R.drawable.baseline_female_24), holder.imgGenderIcon);
            holder.imgGenderIcon.setImageTintList(AppCompatResources.getColorStateList(context, R.color.secondary_300));
        } else {
            loadImage(AppCompatResources.getDrawable(context, R.drawable.baseline_transgender_24), holder.imgGenderIcon);
            holder.imgGenderIcon.setImageTintList(AppCompatResources.getColorStateList(context, R.color.gray_500));
        }

        holder.txtName.setText(model.getName());
        holder.txtBreed.setText(model.getBreed());
        holder.txtAge.setText(getCatAge(model.getDateOfBirth()));

        // Redirect to Cat Profile when clicked
        holder.catHolderBody.setOnClickListener(view -> {
            Intent intent = new Intent(context, CatProfileActivity.class);
            intent.putExtra(KEY_CAT_ID, getSnapshots().getSnapshot(position).getId());
            context.startActivity(intent);
        });
    }

    @NonNull
    @Override
    public CatAdapter.CatHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.holder_cat, parent, false);
        return new CatHolder(view);
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

    private String getCatAge(Timestamp dob) {
        if (dob == null) {
            return "";
        } else {
            // Convert the Firestore Timestamp to a Date object
            Date birthDate = dob.toDate();
            Date currentDate = new Date(); // Get the current date

            // Calculate the time difference in milliseconds
            long timeDifferenceMillis = currentDate.getTime() - birthDate.getTime();

            // Convert time difference to days
            long daysDifference = TimeUnit.MILLISECONDS.toDays(timeDifferenceMillis);

            // Calculate the years, months, and weeks
            int years = (int) (daysDifference / 365);   // Approximate days in a year
            int months = (int) (daysDifference / 30);   // Approximate days in a month
            int weeks = (int) (daysDifference / 7);     // Days in a week

            // If the cat is 1 year or older, show the age in years
            if (years >= 1) {
                return years + "yo";
            }
            // If the cat is less than 1 year but more than or equal to 1 month, show the age in months
            else if (months >= 1) {
                return months + "mo";
            }
            // If the cat is less than 1 month, show the age in weeks
            else {
                return weeks + "wo";
            }
        }
    }

    private void loadImage(Drawable icon, ImageView imageView) {
        Glide.with(context)
                .load(icon)
                .into(imageView);
    }

    protected class CatHolder extends RecyclerView.ViewHolder {
        MaterialCardView catHolderBody;
        ImageView imgCatProfilePicture;
        ImageView imgGenderIcon;
        TextView txtName;
        TextView txtBreed;
        TextView txtAge;

        public CatHolder(@NonNull View itemView) {
            super(itemView);
            catHolderBody = itemView.findViewById(R.id.cat_holder_body);
            imgCatProfilePicture = itemView.findViewById(R.id.img_cat_profile_picture);
            imgGenderIcon = itemView.findViewById(R.id.img_gender_icon);
            txtName = itemView.findViewById(R.id.txt_name);
            txtBreed = itemView.findViewById(R.id.txt_breed);
            txtAge = itemView.findViewById(R.id.txt_age);
        }
    }
}

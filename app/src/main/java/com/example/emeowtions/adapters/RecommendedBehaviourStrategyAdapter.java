package com.example.emeowtions.adapters;

import android.content.Context;
import android.content.SharedPreferences;
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

import com.example.emeowtions.R;
import com.example.emeowtions.enums.Role;
import com.example.emeowtions.models.BehaviourStrategy;
import com.example.emeowtions.models.Recommendation;
import com.example.emeowtions.models.RecommendedBehaviourStrategy;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;

public class RecommendedBehaviourStrategyAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "RecommendedBehaviourStrategyAdapter";
    private SharedPreferences sharedPreferences;
    private String currentUserRole;

    private FirebaseFirestore db;
    private CollectionReference behaviourStratsRef;
    private CollectionReference recommendationsRef;

    private ArrayList<RecommendedBehaviourStrategy> stratList;
    private Context context;

    public RecommendedBehaviourStrategyAdapter(ArrayList<RecommendedBehaviourStrategy> stratList, Context context) {
        this.sharedPreferences = context.getSharedPreferences("com.emeowtions", Context.MODE_PRIVATE);
        this.currentUserRole = this.sharedPreferences.getString("role", "User");
        this.db = FirebaseFirestore.getInstance();
        this.behaviourStratsRef = db.collection("behaviourStrategies");
        this.recommendationsRef = db.collection("recommendations");
        this.stratList = stratList;
        this.context = context;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.holder_recommended_behaviour_strategy, parent, false);
        return new RecommendedBehaviourStrategyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        RecommendedBehaviourStrategy item = stratList.get(position);
        RecommendedBehaviourStrategyHolder stratHolder = (RecommendedBehaviourStrategyHolder) holder;

        // Rating controls
        final boolean[] liked = {item.isLiked()};
        final boolean[] disliked = {item.isDisliked()};
        final boolean[] rated = {item.isRated()};

        // Set recommendation type icon
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

        // Populate text fields
        if (item.getFactorType() == null) {
            stratHolder.txtFactorType.setText(R.string.none);
            stratHolder.txtFactorValue.setText(R.string.general);
        } else {
            stratHolder.txtFactorType.setText(item.getFactorType());
            stratHolder.txtFactorValue.setText(item.getFactorValue());
        }
        stratHolder.txtDescription.setText(item.getDescription());

        // Set like and dislike icon buttons
        if (item.isRated() && item.isLiked()) {
            toggleButtons(stratHolder.imgLike, stratHolder.imgDislike);
        } else if (item.isRated() && item.isDisliked()) {
            toggleButtons(stratHolder.imgDislike, stratHolder.imgLike);
        }

        // Retrieve likes and dislikes for percentage of users who found it useful
        behaviourStratsRef.document(item.getStratId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        BehaviourStrategy behaviourStrategy = documentSnapshot.toObject(BehaviourStrategy.class);
                        long likeCount = behaviourStrategy.getLikeCount();
                        long dislikeCount = behaviourStrategy.getDislikeCount();
                        long totalCount = likeCount + dislikeCount;
                        int percentage = totalCount == 0 ? 0 : ((int) (((double) likeCount / (double) totalCount) * 100));

                        stratHolder.txtRating.setText(String.format("%s%%", percentage));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "onBindViewHolder: Failed to retrieve BehaviourStrategy data", e);
                });

        // Listeners for like and dislike button
        // Do not allow Admin or Super Admin to use these buttons
        if (!currentUserRole.equals(Role.ADMIN.getTitle()) && !currentUserRole.equals(Role.SUPER_ADMIN.getTitle())) {
            // Like
            stratHolder.imgLike.setOnClickListener(view -> {
                toggleButtons(stratHolder.imgLike, stratHolder.imgDislike);

                Log.d(TAG, "onBindViewHolder: position=" + position);
                Log.d(TAG, "onBindViewHolder: liked=" + liked[0]);
                Log.d(TAG, "onBindViewHolder: disliked=" + disliked[0]);
                Log.d(TAG, "onBindViewHolder: rated=" + rated[0]);

                // rated = false    (LIKE 1: Not Liked neither Disliked)
                if (!rated[0]) {
                    // set like=true, set rated=true, increment like count
                    liked[0] = true;
                    disliked[0] = false;
                    rated[0] = true;

                    // Retrieve the parent Recommendation document that contains this strategy
                    recommendationsRef.document(item.getRecommendationId())
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    // Update the liked strategy item
                                    Recommendation recommendation = documentSnapshot.toObject(Recommendation.class);
                                    List<RecommendedBehaviourStrategy> recommendedBehaviourStrategyList = recommendation.getStrategies();

                                    recommendedBehaviourStrategyList.get(position).setLiked(liked[0]);
                                    recommendedBehaviourStrategyList.get(position).setDisliked(disliked[0]);
                                    recommendedBehaviourStrategyList.get(position).setRated(rated[0]);

                                    // Update the entire strategies list in the parent Recommendation document
                                    recommendationsRef.document(item.getRecommendationId())
                                            .update("strategies", recommendedBehaviourStrategyList, "updatedAt", Timestamp.now())
                                            .addOnCompleteListener(updateStratsTask -> {
                                                if (updateStratsTask.isSuccessful()) {
                                                    // Update Like count
                                                    behaviourStratsRef.document(item.getStratId())
                                                            .update("likeCount", FieldValue.increment(1), "updatedAt", Timestamp.now())
                                                            .addOnCompleteListener(updateLikeCountTask -> {
                                                                if (updateLikeCountTask.isSuccessful()) {
                                                                    Log.d(TAG, "onBindViewHolder: Successfully updated likeCount");
                                                                } else {
                                                                    Log.w(TAG, "onBindViewHolder: Failed to update likeCount", updateLikeCountTask.getException());
                                                                }
                                                            });
                                                } else {
                                                    Log.w(TAG, "onBindViewHolder: Failed to update Recommendation strategies after liking", updateStratsTask.getException());
                                                }
                                            });
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.w(TAG, "onBindViewHolder: Failed to retrieve Recommendation data", e);
                            });
                }
                // rated = true; liked = true   (LIKE 2: Unlike)
                else if (rated[0] && liked[0]) {
                    toggleButtons(stratHolder.imgLike, stratHolder.imgLike);

                    // set liked=false, set rated=false, decrement like count
                    liked[0] = false;
                    disliked[0] = false;
                    rated[0] = false;

                    // Retrieve the parent Recommendation document that contains this strategy
                    recommendationsRef.document(item.getRecommendationId())
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    // Update the liked strategy item
                                    Recommendation recommendation = documentSnapshot.toObject(Recommendation.class);
                                    List<RecommendedBehaviourStrategy> recommendedBehaviourStrategyList = recommendation.getStrategies();

                                    recommendedBehaviourStrategyList.get(position).setLiked(liked[0]);
                                    recommendedBehaviourStrategyList.get(position).setDisliked(disliked[0]);
                                    recommendedBehaviourStrategyList.get(position).setRated(rated[0]);

                                    // Update the entire strategies list in the parent Recommendation document
                                    recommendationsRef.document(item.getRecommendationId())
                                            .update("strategies", recommendedBehaviourStrategyList, "updatedAt", Timestamp.now())
                                            .addOnCompleteListener(updateStratsTask -> {
                                                if (updateStratsTask.isSuccessful()) {
                                                    // Update Like count
                                                    behaviourStratsRef.document(item.getStratId())
                                                            .update("likeCount", FieldValue.increment(-1), "updatedAt", Timestamp.now())
                                                            .addOnCompleteListener(updateLikeCountTask -> {
                                                                if (updateLikeCountTask.isSuccessful()) {
                                                                    Log.d(TAG, "onBindViewHolder: Successfully updated likeCount");
                                                                } else {
                                                                    Log.w(TAG, "onBindViewHolder: Failed to update likeCount", updateLikeCountTask.getException());
                                                                }
                                                            });
                                                } else {
                                                    Log.w(TAG, "onBindViewHolder: Failed to update Recommendation strategies after unliking", updateStratsTask.getException());
                                                }
                                            });
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.w(TAG, "onBindViewHolder: Failed to retrieve Recommendation data", e);
                            });
                }
                // rated = true; disliked = true    (LIKE 3: Change from Dislike to Like)
                else if (rated[0] && disliked[0]) {
                    // set liked=true, disliked=false, increment like count, decrement dislike count
                    liked[0] = true;
                    disliked[0] = false;
                    rated[0] = true;

                    // Retrieve the parent Recommendation document that contains this strategy
                    recommendationsRef.document(item.getRecommendationId())
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    // Update the liked strategy item
                                    Recommendation recommendation = documentSnapshot.toObject(Recommendation.class);
                                    List<RecommendedBehaviourStrategy> recommendedBehaviourStrategyList = recommendation.getStrategies();

                                    recommendedBehaviourStrategyList.get(position).setLiked(liked[0]);
                                    recommendedBehaviourStrategyList.get(position).setDisliked(disliked[0]);
                                    recommendedBehaviourStrategyList.get(position).setRated(rated[0]);

                                    // Update the entire strategies list in the parent Recommendation document
                                    recommendationsRef.document(item.getRecommendationId())
                                            .update("strategies", recommendedBehaviourStrategyList, "updatedAt", Timestamp.now())
                                            .addOnCompleteListener(updateStratsTask -> {
                                                if (updateStratsTask.isSuccessful()) {
                                                    // Update Like count
                                                    behaviourStratsRef.document(item.getStratId())
                                                            .update("likeCount", FieldValue.increment(1), "dislikeCount", FieldValue.increment(-1), "updatedAt", Timestamp.now())
                                                            .addOnCompleteListener(updateLikeCountTask -> {
                                                                if (updateLikeCountTask.isSuccessful()) {
                                                                    Log.d(TAG, "onBindViewHolder: Successfully updated likeCount and dislikeCount");
                                                                } else {
                                                                    Log.w(TAG, "onBindViewHolder: Failed to update likeCount and dislikeCount", updateLikeCountTask.getException());
                                                                }
                                                            });
                                                } else {
                                                    Log.w(TAG, "onBindViewHolder: Failed to update Recommendation strategies after changing dislike to like", updateStratsTask.getException());
                                                }
                                            });
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.w(TAG, "onBindViewHolder: Failed to retrieve Recommendation data", e);
                            });
                }
            });

            // Dislike
            stratHolder.imgDislike.setOnClickListener(view -> {
                toggleButtons(stratHolder.imgDislike, stratHolder.imgLike);

                Log.d(TAG, "onBindViewHolder: position=" + position);
                Log.d(TAG, "onBindViewHolder: liked=" + liked[0]);
                Log.d(TAG, "onBindViewHolder: disliked=" + disliked[0]);
                Log.d(TAG, "onBindViewHolder: rated=" + rated[0]);

                // rated = false    (DISLIKE 1: Not Liked neither Disliked)
                if (!rated[0]) {
                    // set disliked=true, set rated=true, increment dislike count
                    liked[0] = false;
                    disliked[0] = true;
                    rated[0] = true;

                    // Retrieve the parent Recommendation document that contains this strategy
                    recommendationsRef.document(item.getRecommendationId())
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    // Update the liked strategy item
                                    Recommendation recommendation = documentSnapshot.toObject(Recommendation.class);
                                    List<RecommendedBehaviourStrategy> recommendedBehaviourStrategyList = recommendation.getStrategies();

                                    recommendedBehaviourStrategyList.get(position).setLiked(liked[0]);
                                    recommendedBehaviourStrategyList.get(position).setDisliked(disliked[0]);
                                    recommendedBehaviourStrategyList.get(position).setRated(rated[0]);

                                    // Update the entire strategies list in the parent Recommendation document
                                    recommendationsRef.document(item.getRecommendationId())
                                            .update("strategies", recommendedBehaviourStrategyList, "updatedAt", Timestamp.now())
                                            .addOnCompleteListener(updateStratsTask -> {
                                                if (updateStratsTask.isSuccessful()) {
                                                    // Update Like count
                                                    behaviourStratsRef.document(item.getStratId())
                                                            .update("dislikeCount", FieldValue.increment(1), "updatedAt", Timestamp.now())
                                                            .addOnCompleteListener(updateLikeCountTask -> {
                                                                if (updateLikeCountTask.isSuccessful()) {
                                                                    Log.d(TAG, "onBindViewHolder: Successfully updated dislikeCount");
                                                                } else {
                                                                    Log.w(TAG, "onBindViewHolder: Failed to update dislikeCount", updateLikeCountTask.getException());
                                                                }
                                                            });
                                                } else {
                                                    Log.w(TAG, "onBindViewHolder: Failed to update Recommendation strategies after disliking", updateStratsTask.getException());
                                                }
                                            });
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.w(TAG, "onBindViewHolder: Failed to retrieve Recommendation data", e);
                            });
                }
                // rated = true; disliked = true (DISLIKE 2: Un-dislike)
                else if (rated[0] && disliked[0]) {
                    toggleButtons(stratHolder.imgDislike, stratHolder.imgDislike);

                    // Set disliked=false, set rated=false, decrement dislike count
                    liked[0] = false;
                    disliked[0] = false;
                    rated[0] = false;

                    // Retrieve the parent Recommendation document that contains this strategy
                    recommendationsRef.document(item.getRecommendationId())
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    // Update the liked strategy item
                                    Recommendation recommendation = documentSnapshot.toObject(Recommendation.class);
                                    List<RecommendedBehaviourStrategy> recommendedBehaviourStrategyList = recommendation.getStrategies();

                                    recommendedBehaviourStrategyList.get(position).setLiked(liked[0]);
                                    recommendedBehaviourStrategyList.get(position).setDisliked(disliked[0]);
                                    recommendedBehaviourStrategyList.get(position).setRated(rated[0]);

                                    // Update the entire strategies list in the parent Recommendation document
                                    recommendationsRef.document(item.getRecommendationId())
                                            .update("strategies", recommendedBehaviourStrategyList, "updatedAt", Timestamp.now())
                                            .addOnCompleteListener(updateStratsTask -> {
                                                if (updateStratsTask.isSuccessful()) {
                                                    // Update Like count
                                                    behaviourStratsRef.document(item.getStratId())
                                                            .update("dislikeCount", FieldValue.increment(-1), "updatedAt", Timestamp.now())
                                                            .addOnCompleteListener(updateLikeCountTask -> {
                                                                if (updateLikeCountTask.isSuccessful()) {
                                                                    Log.d(TAG, "onBindViewHolder: Successfully updated dislikeCount");
                                                                } else {
                                                                    Log.w(TAG, "onBindViewHolder: Failed to update dislikeCount", updateLikeCountTask.getException());
                                                                }
                                                            });
                                                } else {
                                                    Log.w(TAG, "onBindViewHolder: Failed to update Recommendation strategies after un-disliking", updateStratsTask.getException());
                                                }
                                            });
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.w(TAG, "onBindViewHolder: Failed to retrieve Recommendation data", e);
                            });
                }
                // rated = true; liked = true (LIKE 3: Change from Like to Dislike)
                else if (rated[0] && liked[0]) {
                    // Set liked=false, disliked=true, decrement like count, increment dislike count
                    liked[0] = false;
                    disliked[0] = true;
                    rated[0] = true;

                    // Retrieve the parent Recommendation document that contains this strategy
                    recommendationsRef.document(item.getRecommendationId())
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    // Update the liked strategy item
                                    Recommendation recommendation = documentSnapshot.toObject(Recommendation.class);
                                    List<RecommendedBehaviourStrategy> recommendedBehaviourStrategyList = recommendation.getStrategies();

                                    recommendedBehaviourStrategyList.get(position).setLiked(liked[0]);
                                    recommendedBehaviourStrategyList.get(position).setDisliked(disliked[0]);
                                    recommendedBehaviourStrategyList.get(position).setRated(rated[0]);

                                    // Update the entire strategies list in the parent Recommendation document
                                    recommendationsRef.document(item.getRecommendationId())
                                            .update("strategies", recommendedBehaviourStrategyList, "updatedAt", Timestamp.now())
                                            .addOnCompleteListener(updateStratsTask -> {
                                                if (updateStratsTask.isSuccessful()) {
                                                    // Update Like count
                                                    behaviourStratsRef.document(item.getStratId())
                                                            .update("likeCount", FieldValue.increment(-1), "dislikeCount", FieldValue.increment(1), "updatedAt", Timestamp.now())
                                                            .addOnCompleteListener(updateLikeCountTask -> {
                                                                if (updateLikeCountTask.isSuccessful()) {
                                                                    Log.d(TAG, "onBindViewHolder: Successfully updated likeCount and dislikeCount");
                                                                } else {
                                                                    Log.w(TAG, "onBindViewHolder: Failed to update likeCount and dislikeCount", updateLikeCountTask.getException());
                                                                }
                                                            });
                                                } else {
                                                    Log.w(TAG, "onBindViewHolder: Failed to update Recommendation strategies after changing like to dislike", updateStratsTask.getException());
                                                }
                                            });
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.w(TAG, "onBindViewHolder: Failed to retrieve Recommendation data", e);
                            });
                }
            });
        }
    }

    private void loadIcon(Drawable icon, ImageView imageView) {
        imageView.setImageDrawable(icon);
        imageView.setImageTintList(AppCompatResources.getColorStateList(context, R.color.white));
    }

    private void toggleButtons(ImageView btnOn, ImageView btnOff) {
        btnOn.setImageTintList(ContextCompat.getColorStateList(context, R.color.primary_400));
        btnOff.setImageTintList(ContextCompat.getColorStateList(context, R.color.gray_200));
    }

    @Override
    public int getItemCount() {
        return stratList.size();
    }

    private class RecommendedBehaviourStrategyHolder extends RecyclerView.ViewHolder {
        ImageView imgIcon;
        ImageView imgLike;
        ImageView imgDislike;
        TextView txtFactorType;
        TextView txtFactorValue;
        TextView txtDescription;
        TextView txtRating;

        public RecommendedBehaviourStrategyHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.img_recommendation_icon);
            imgLike = itemView.findViewById(R.id.img_like);
            imgDislike = itemView.findViewById(R.id.img_dislike);
            txtFactorType = itemView.findViewById(R.id.txt_factor_type);
            txtFactorValue = itemView.findViewById(R.id.txt_factor_value);
            txtDescription = itemView.findViewById(R.id.txt_description);
            txtRating = itemView.findViewById(R.id.txt_rating);
        }
    }
}

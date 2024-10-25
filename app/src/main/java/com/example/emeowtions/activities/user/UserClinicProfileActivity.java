package com.example.emeowtions.activities.user;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.emeowtions.R;
import com.example.emeowtions.adapters.ReviewAdapter;
import com.example.emeowtions.adapters.VeterinarianAdapter;
import com.example.emeowtions.adapters.VeterinaryClinicAdapter;
import com.example.emeowtions.databinding.ActivityUserClinicProfileBinding;
import com.example.emeowtions.enums.Role;
import com.example.emeowtions.models.ChatRequest;
import com.example.emeowtions.models.Review;
import com.example.emeowtions.models.User;
import com.example.emeowtions.models.Veterinarian;
import com.example.emeowtions.models.VeterinaryClinic;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class UserClinicProfileActivity extends AppCompatActivity {

    private static final String TAG = "UserClinicProfileActivity";
    private SharedPreferences sharedPreferences;

    // Firebase variables
    private FirebaseAuthUtils firebaseAuthUtils;
    private FirebaseFirestore db;
    private CollectionReference usersRef;
    private CollectionReference clinicsRef;
    private CollectionReference vetsRef;
    private CollectionReference reviewsRef;
    private CollectionReference chatRequestsRef;

    // Layout variables
    private ActivityUserClinicProfileBinding binding;
    private VeterinarianAdapter vetAdapter;
    private ReviewAdapter reviewAdapter;

    // Private variables
    private String currentUserRole;
    private String clinicId;
    private String clinicName;
    private String clinicAddress;
    private float currentTotalRating;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Shared preferences
        sharedPreferences = getSharedPreferences("com.emeowtions", Context.MODE_PRIVATE);
        currentUserRole = sharedPreferences.getString("role", "User");

        // Get intent data
        Intent passedIntent = getIntent();
        clinicId = passedIntent.getStringExtra(VeterinaryClinicAdapter.KEY_CLINIC_ID);

        // Initialize Firebase service instances
        firebaseAuthUtils = new FirebaseAuthUtils();
        db = FirebaseFirestore.getInstance();
        // Initialize Firestore references
        usersRef = db.collection("users");
        clinicsRef = db.collection("veterinaryClinics");
        vetsRef = db.collection("veterinarians");
        reviewsRef = clinicsRef.document(clinicId).collection("reviews");
        chatRequestsRef = db.collection("chatRequests");

        // Get ViewBinding and set content view
        binding = ActivityUserClinicProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Enable edge-to-edge layout
        EdgeToEdge.enable(this);
        // Adjust main layout padding based on system bar size to prevent obstruction
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //region UI Setups
        // Hide review and request consultation button if the user is a Veterinarian or Veterinary Staff
        if (currentUserRole.equals(Role.VETERINARIAN.getTitle()) || currentUserRole.equals(Role.VETERINARY_STAFF.getTitle())) {
            binding.appBarClinicProfile.getMenu().removeItem(R.id.action_review_clinic);
            binding.appBarClinicProfile.getMenu().removeItem(R.id.action_request_consultation);
        }
        //endregion

        //region Navigation Listeners
        binding.appBarClinicProfile.setNavigationOnClickListener(view -> finish());

        binding.appBarClinicProfile.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.action_get_directions) {
                openGoogleMaps(clinicName, clinicAddress);
            } else if (itemId == R.id.action_review_clinic) {
                showReviewDialog();
            } else if (itemId == R.id.action_request_consultation) {
                checkDuplicateRequest();
            }

            return false;
        });
        //endregion

        //region Load Data
        // Load veterinary clinic data
        clinicsRef.document(clinicId)
                .addSnapshotListener((value, error) -> {
                    // Error
                    if (error != null) {
                        Log.w(TAG, "onCreate: Failed to listen on current VeterinaryClinic changes", error);
                        return;
                    }
                    // Success
                    if (value != null && value.exists()) {
                        VeterinaryClinic clinic = value.toObject(VeterinaryClinic.class);
                        SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy", Locale.getDefault());

                        // Get field values
                        String logoUrl = clinic.getLogoUrl();
                        clinicName = clinic.getName();
                        String email = clinic.getEmail();
                        String phoneNumber = clinic.getPhoneNumber();
                        clinicAddress = clinic.getAddress();
                        String description = clinic.getDescription();
                        currentTotalRating = clinic.getTotalRating();
                        float averageRating = clinic.getAverageRating();

                        // Load banner
                        loadLogo(logoUrl);
                        binding.txtBannerName.setText(clinicName);
                        binding.txtBannerJoinedDate.setText(String.format("Joined %s", sdf.format(clinic.getCreatedAt().toDate())));
                        binding.txtBannerUpdatedDate.setText(String.format("Updated %s", sdf.format(clinic.getUpdatedAt().toDate())));
                        binding.txtRating.setText(String.format("%s", averageRating));

                        // Load fields
                        binding.txtEmail.setText(email);
                        binding.txtPhoneNumber.setText(phoneNumber);
                        binding.txtAddress.setText(clinicAddress);
                        binding.txtDescription.setText(description);
                    }
                });

        // Load veterinarians and reviews data
        Query vetQuery = vetsRef.whereEqualTo("veterinaryClinicId", clinicId).whereEqualTo("deleted", false);
        Query reviewQuery = reviewsRef.orderBy("updatedAt", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<Veterinarian> vetOptions =
                new FirestoreRecyclerOptions.Builder<Veterinarian>()
                        .setQuery(vetQuery, Veterinarian.class)
                        .setLifecycleOwner(this)
                        .build();
        FirestoreRecyclerOptions<Review> reviewOptions =
                new FirestoreRecyclerOptions.Builder<Review>()
                        .setQuery(reviewQuery, Review.class)
                        .setLifecycleOwner(this)
                        .build();

        vetAdapter = new VeterinarianAdapter(vetOptions, this);
        reviewAdapter = new ReviewAdapter(reviewOptions, this);

        binding.recyclerviewVets.setAdapter(vetAdapter);
        binding.recyclerviewReviews.setAdapter(reviewAdapter);

        vetAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                updateResultsView();
            }
        });
        reviewAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                updateResultsView();
            }
        });
        //endregion
    }

    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuthUtils.checkSignedIn(this);
    }

    public void openGoogleMaps(String name, String address) {
        String query = name + " " + address;
        Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(query));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        // Check if Google Maps is installed
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Toast.makeText(this, "Google Maps is not installed", Toast.LENGTH_SHORT).show();
        }
    }

    private void showReviewDialog() {
        View reviewDialogLayout = LayoutInflater.from(this).inflate(R.layout.dialog_review_clinic, null);
        RatingBar ratingBar = reviewDialogLayout.findViewById(R.id.rating_bar);
        TextInputLayout txtfieldDescription = reviewDialogLayout.findViewById(R.id.txtfield_description);
        TextInputEditText edtDescription = reviewDialogLayout.findViewById(R.id.edt_description);

        MaterialAlertDialogBuilder reviewDialogBuilder =
                new MaterialAlertDialogBuilder(this)
                        .setView(reviewDialogLayout)
                        .setTitle(R.string.review)
                        .setMessage(R.string.review_dialog_message)
                        .setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
                            // Do nothing
                        })
                        .setPositiveButton(R.string.confirm, (dialogInterface, i) -> {
                            // Unused
                        });

        AlertDialog reviewDialog = reviewDialogBuilder.create();
        reviewDialog.show();

        // Override the positive button's click behavior
        reviewDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            // Validate inputs
            float rating = ratingBar.getRating();
            String description = edtDescription.getText().toString();

            if (rating == 0) {
                // No rating
                Toast.makeText(this, "Please select a rating", Toast.LENGTH_LONG).show();
            } else if (description.isBlank()) {
                txtfieldDescription.setError(getString(R.string.description_required_error));
            } else {
                // Valid
                reviewClinic(rating, description);
                reviewDialog.dismiss();
            }
        });

        // Reset error when text is changed
        edtDescription.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                txtfieldDescription.setErrorEnabled(false);
            }
            @Override
            public void afterTextChanged(Editable editable) {}
        });
    }

    // Creates a new Review
    private void reviewClinic(float rating, String description) {
        int reviewCount = reviewAdapter.getItemCount();

        // Retrieve user data
        usersRef.document(firebaseAuthUtils.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        Review newReview = new Review(
                                clinicId,
                                firebaseAuthUtils.getUid(),
                                user.getEmail(),
                                user.getDisplayName(),
                                user.getProfilePicture(),
                                rating,
                                description,
                                false,
                                Timestamp.now(),
                                Timestamp.now()
                        );

                        // Add new Review
                        reviewsRef.add(newReview)
                                .addOnCompleteListener(task -> {
                                    if (!task.isSuccessful()) {
                                        Log.w(TAG, "reviewClinic: Failed to add new Review", task.getException());
                                        Toast.makeText(this, "Failed to submit review, please try again later.", Toast.LENGTH_SHORT).show();
                                        return;
                                    } else {
                                        // Update VeterinaryClinic reviewCount and rating
                                        clinicsRef.document(clinicId)
                                                .update(
                                                        "reviewCount", FieldValue.increment(1),
                                                        "totalRating", FieldValue.increment(rating),
                                                        "averageRating", (currentTotalRating + rating) / (reviewCount + 1)
                                                )
                                                .addOnCompleteListener(task1 -> {
                                                    // COMPLETE
                                                    if (!task.isSuccessful()) {
                                                        Log.w(TAG, "reviewClinic: Failed to updated VeterinaryClinic reviewCount and rating", task.getException());
                                                        Toast.makeText(this, "Failed to update Veterinary Clinic rating, please try again later.", Toast.LENGTH_SHORT).show();
                                                        return;
                                                    } else {
                                                        Toast.makeText(this, "Successfully submitted review.", Toast.LENGTH_SHORT).show();
                                                        disableMenuItem(R.id.action_review_clinic);
                                                    }
                                                });
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "reviewClinic: Failed to retrieve User data", e);
                });
    }

    // Checks if user has already submitted a request before
    private void checkDuplicateRequest() {
        chatRequestsRef
                .whereEqualTo("veterinaryClinicId", clinicId)
                .whereEqualTo("uid", firebaseAuthUtils.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // COMPLETE
                    if (queryDocumentSnapshots.isEmpty()) {
                        // No prior requests
                        showConsultDialog();
                    } else {
                        // Existing request
                        showConsultConfirmDialog();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "checkDuplicateRequest: Failed to retrieve ChatRequests", e);
                    Toast.makeText(this, "Consultation requests are unavailable at the moment. Please try again later.", Toast.LENGTH_SHORT).show();
                });
    }

    // Opens request consultation dialog if user has already requested before
    private void showConsultConfirmDialog() {
        MaterialAlertDialogBuilder confirmConsultDialog =
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.request_consultation)
                        .setMessage(R.string.request_consultation_duplicate_confirmation)
                        .setNegativeButton(R.string.no, (dialogInterface, i) -> {})
                        .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                            showConsultDialog();
                        });

        confirmConsultDialog.show();
    }

    // Opens request consultation dialog
    private void showConsultDialog() {
        View consultDialogLayout = LayoutInflater.from(this).inflate(R.layout.dialog_request_consultation, null);
        TextInputLayout txtfieldDescription = consultDialogLayout.findViewById(R.id.txtfield_description);
        TextInputEditText edtDescription = consultDialogLayout.findViewById(R.id.edt_description);

        MaterialAlertDialogBuilder consultDialogBuilder =
                new MaterialAlertDialogBuilder(this)
                        .setView(consultDialogLayout)
                        .setTitle(R.string.request_consultation)
                        .setMessage(R.string.request_consultation_instruction)
                        .setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
                            // Do nothing
                        })
                        .setPositiveButton(R.string.confirm, (dialogInterface, i) -> {
                            // Unused
                        });

        AlertDialog consultDialog = consultDialogBuilder.create();
        consultDialog.show();

        // Override the positive button's click behavior
        consultDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            // Validate inputs
            String description = edtDescription.getText().toString();

            if (description.isBlank()) {
                txtfieldDescription.setError(getString(R.string.description_required_error));
            } else {
                // Valid
                requestConsultation(description);
                consultDialog.dismiss();
            }
        });

        // Reset error when text is changed
        edtDescription.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                txtfieldDescription.setErrorEnabled(false);
            }
            @Override
            public void afterTextChanged(Editable editable) {}
        });
    }

    // Creates a new ChatRequest
    private void requestConsultation(String description) {
        // Retrieve user data
        usersRef.document(firebaseAuthUtils.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);

                        ChatRequest newChatRequest = new ChatRequest(
                                clinicId,
                                firebaseAuthUtils.getUid(),
                                user.getEmail(),
                                user.getDisplayName(),
                                user.getProfilePicture(),
                                description,
                                false,
                                Timestamp.now(),
                                Timestamp.now()
                        );

                        // Add new ChatRequest
                        chatRequestsRef.add(newChatRequest)
                                .addOnCompleteListener(task -> {
                                    if (!task.isSuccessful()) {
                                        Log.w(TAG, "requestConsultation: Failed to add new ChatRequest", task.getException());
                                        Toast.makeText(this, "Failed to submit consultation request, please try again later.", Toast.LENGTH_SHORT).show();
                                        return;
                                    } else {
                                        // COMPLETE
                                        Toast.makeText(this, "Successfully submitted consultation request. Please await response from the clinic and refrain from spamming requests.", Toast.LENGTH_LONG).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "requestConsultation: Failed to retrieve User data", e);
                });
    }

    // Disables menu action item
    private void disableMenuItem(int itemId) {
        MenuItem menuItem = binding.appBarClinicProfile.getMenu().findItem(itemId);
        menuItem.setEnabled(false);
        menuItem.setIconTintList(ContextCompat.getColorStateList(this, R.color.gray_200));
    }

    // Loads logo into ImageView
    private void loadLogo(String logoUrl) {
        if (logoUrl == null) {
            // Load default picture if no profile picture exists
            Glide.with(getApplicationContext())
                    .load(R.drawable.baseline_local_hospital_24)
                    .into(binding.imgClinicLogo);
        } else {
            // Load original profile picture
            Glide.with(getApplicationContext())
                    .load(logoUrl)
                    .into(binding.imgClinicLogo);
        }
    }

    // Updates veterinarian or review lists results view
    private void updateResultsView() {
        // Reset visibility
        binding.layoutNoVets.setVisibility(View.GONE);
        binding.layoutNoReviews.setVisibility(View.GONE);

        // Determine no existing items or no query results
        if (vetAdapter == null || vetAdapter.getItemCount() == 0) {
            binding.layoutNoVets.setVisibility(View.VISIBLE);
        } else {
            binding.txtVetCount.setText(String.format("%s", vetAdapter.getItemCount()));
        }

        if (reviewAdapter == null || reviewAdapter.getItemCount() == 0) {
            binding.layoutNoReviews.setVisibility(View.VISIBLE);
        } else {
            // Check if user has already reviewed this clinic
            for (int i = 0; i < reviewAdapter.getItemCount(); i++) {
                Review review = reviewAdapter.getItem(i);
                if (review.getUid().equals(firebaseAuthUtils.getUid())) {
                    disableMenuItem(R.id.action_review_clinic);
                }
                binding.txtReviewCount.setText(String.format("%s", reviewAdapter.getItemCount()));
            }
        }
    }
}
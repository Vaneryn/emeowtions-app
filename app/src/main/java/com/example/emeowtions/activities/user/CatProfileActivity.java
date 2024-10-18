package com.example.emeowtions.activities.user;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.emeowtions.R;
import com.example.emeowtions.adapters.CatAdapter;
import com.example.emeowtions.databinding.ActivityCatProfileBinding;
import com.example.emeowtions.fragments.user.UserMyCatsFragment;
import com.example.emeowtions.models.Cat;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CatProfileActivity extends AppCompatActivity {

    private static final String TAG = "CatProfileActivity";
    private ActivityCatProfileBinding catProfileBinding;

    private FirebaseAuthUtils firebaseAuthUtils;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private CollectionReference catsRef;
    private StorageReference storageRef;
    private StorageReference profilePictureRef;

    private String originalProfilePictureUrl;
    private String originalName;
    private String originalDescription;
    private Timestamp originalDateOfBirth;
    private String originalGender;
    private String originalBreed;
    private String originalBackground;
    private String originalMedicalConditions;

    private String catId;
    private Date selectedDateOfBirth;
    private boolean isProfilePictureChanged;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get Intent data
        Intent passedIntent = getIntent();
        catId = passedIntent.getStringExtra(CatAdapter.KEY_CAT_ID);

        // Initialize Firebase service instances
        firebaseAuthUtils = new FirebaseAuthUtils();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        // Initialize Firestore references
        catsRef = db.collection("cats");
        // Initialize Storage references
        storageRef = storage.getReference();
        profilePictureRef = storageRef.child( "images/cats/" + catId + "/profile_picture.jpg");

        // Get ViewBinding and set content view
        catProfileBinding = ActivityCatProfileBinding.inflate(getLayoutInflater());
        setContentView(catProfileBinding.getRoot());

        // Enable edge-to-edge layout
        EdgeToEdge.enable(this);
        // Adjust main layout padding based on system bar size to prevent obstruction
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //region UI setups
        toggleEditMode(false);

        // Delete cat dialog
        MaterialAlertDialogBuilder deleteDialog =
                new MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.delete_cat))
                        .setNegativeButton(getString(R.string.no), (dialogInterface, i) -> {
                            // Do nothing
                        })
                        .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                            deleteCat();
                            finish();
                        });
        
        // Create photo picker
        ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
                registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                    if (uri != null) {
                        Glide.with(getApplicationContext())
                                .load(uri)
                                .into(catProfileBinding.imgProfilePicture);
                        isProfilePictureChanged = true;
                    } else {
                        Log.d(TAG, "PhotoPicker - No media selected");
                    }
                });

        // DatePicker dialog
        MaterialDatePicker<Long> datePicker =
                MaterialDatePicker.Builder.datePicker()
                        .setTitleText("Select date")
                        .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                        .build();

        // Cancel edit profile dialog
        MaterialAlertDialogBuilder cancelDialog =
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.unsaved_changes)
                        .setMessage(R.string.edit_profile_cancel_message)
                        .setNegativeButton(getString(R.string.no), (dialogInterface, i) -> {
                            // Do nothing
                        })
                        .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                            discardChanges();
                        });
        //endregion

        //region Load data
        isProfilePictureChanged = false;

        catsRef.document(catId)
                .addSnapshotListener((value, error) -> {
                   if (error != null) {
                       Log.w(TAG, "Failed to listen on current cat profile changes", error);
                       return;
                   }
                   if (value != null && value.exists()) {
                       Cat cat = value.toObject(Cat.class);
                       SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy", Locale.getDefault());

                       // Save original field values
                       originalProfilePictureUrl = cat.getProfilePicture();
                       originalName = cat.getName();
                       originalDescription = cat.getDescription();
                       originalDateOfBirth = cat.getDateOfBirth();
                       originalGender = cat.getGender();
                       originalBreed = cat.getBreed();
                       originalBackground = cat.getBackground();
                       originalMedicalConditions = cat.getMedicalConditions();

                       selectedDateOfBirth = originalDateOfBirth == null ? null : originalDateOfBirth.toDate() ;

                       // Profile Banner
                       loadProfilePicture(originalProfilePictureUrl);
                       catProfileBinding.txtBannerName.setText(originalName);;
                       catProfileBinding.txtBannerCreatedDate.setText(String.format("Created %s", sdf.format(cat.getCreatedAt().toDate())));
                       catProfileBinding.txtBannerUpdatedDate.setText(String.format("Updated %s", sdf.format(cat.getUpdatedAt().toDate())));

                       // Profile Fields
                       catProfileBinding.edtName.setText(originalName);
                       catProfileBinding.edtDescription.setText(originalDescription.isBlank() ? getString(R.string.no_description) : originalDescription);
                       catProfileBinding.edtDateofbirth.setText(originalDateOfBirth == null ? getString(R.string.not_set) : sdf.format(originalDateOfBirth.toDate()));
                       catProfileBinding.edmGender.setText(originalGender, false);
                       catProfileBinding.edmBreed.setText(originalBreed, false);
                       catProfileBinding.edmBackground.setText(originalBackground, false);
                       catProfileBinding.edmMedicalConditions.setText(originalMedicalConditions, false);
                   }
                });
        //endregion

        //region onClick listeners
        // appBarCatProfile back button: return to My Cats screen
        catProfileBinding.appBarCatProfile.setNavigationOnClickListener(view -> finish());

        // appBarCatProfile edit button: enable text fields
        catProfileBinding.appBarCatProfile.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.action_delete_cat) {
                deleteDialog.setMessage(getString(R.string.delete_cat_message, originalName));
                deleteDialog.show();
            } else if (itemId == R.id.action_edit_cat_profile) {
                toggleEditMode(true);
            }

            return false;
        });

        // Profile picture action buttons
        catProfileBinding.btnRevertProfilePic.setOnClickListener(view -> {
            // Revert profile picture
            isProfilePictureChanged = false;
            loadProfilePicture(originalProfilePictureUrl);
        });
        catProfileBinding.btnEditProfilePic.setOnClickListener(view -> {
            // Launch photo picker
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        // edtDateofbirth: DatePicker
        datePicker.addOnPositiveButtonClickListener(selection -> {
            // Get selected date
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(selection);
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            int month = calendar.get(Calendar.MONTH) + 1;
            int year = calendar.get(Calendar.YEAR);
            String dateText = day + "/" + month + "/" + year;

            // Set selected date
            selectedDateOfBirth = new Date(selection);
            catProfileBinding.edtDateofbirth.setText(dateText);
            catProfileBinding.txtfieldDateofbirth.setEndIconVisible(true);
        });

        catProfileBinding.edtDateofbirth.setOnClickListener(view -> {
            // Prevent app crash from double DatePicker opening
            if (!datePicker.isAdded()) {
                datePicker.show(getSupportFragmentManager(), TAG);
            }
        });

        catProfileBinding.edtDateofbirth.setOnLongClickListener(view -> true);

        catProfileBinding.txtfieldDateofbirth.setEndIconOnClickListener(view -> {
            selectedDateOfBirth = null;
            catProfileBinding.txtfieldDateofbirth.setEndIconVisible(false);
            catProfileBinding.edtDateofbirth.getText().clear();
            catProfileBinding.edtDateofbirth.setText(R.string.not_set);
        });

        // btnCancelEditProfile: disable text fields
        catProfileBinding.btnCancelEditProfile.setOnClickListener(view -> {
            String name = catProfileBinding.edtName.getText().toString();
            String description = catProfileBinding.edtDescription.getText().toString();
            Date dateOfBirth = selectedDateOfBirth;
            String gender = catProfileBinding.edmGender.getText().toString();
            String breed = catProfileBinding.edmBreed.getText().toString();
            String background = catProfileBinding.edmBackground.getText().toString();
            String medicalConditions = catProfileBinding.edmMedicalConditions.getText().toString();

            // Proceed only if any changes are detected
            if (isProfilePictureChanged ||
                    !name.equals(originalName) ||
                    !description.equals(originalDescription) ||
                    !gender.equals(originalGender) ||
                    !breed.equals(originalBreed) ||
                    !background.equals(originalBackground) ||
                    !medicalConditions.equals(originalMedicalConditions)
            ) {
                // Any field updated
                cancelDialog.show();
            } else if (originalDateOfBirth == null) {
                // Original date of birth was not set and is now updated
                if (dateOfBirth != null) {
                    cancelDialog.show();
                } else {
                    discardChanges();
                }
            } else if (originalDateOfBirth != null) {
                // Original date of birth was already set and is now updated
                if (dateOfBirth == null) {
                    cancelDialog.show();
                } else {
                    if (!dateOfBirth.equals(originalDateOfBirth.toDate())) {
                        cancelDialog.show();
                    } else {
                        discardChanges();
                    }
                }
            } else {
                discardChanges();
            }
        });

        // btnConfirmEditProfile: update profile
        catProfileBinding.btnConfirmEditProfile.setOnClickListener(view -> {
            // Get inputs
            byte[] profilePictureData = null;
            if (isProfilePictureChanged) {
                catProfileBinding.imgProfilePicture.setDrawingCacheEnabled(true);
                catProfileBinding.imgProfilePicture.buildDrawingCache();
                Bitmap bitmap = ((BitmapDrawable) catProfileBinding.imgProfilePicture.getDrawable()).getBitmap();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                profilePictureData = baos.toByteArray();
            }

            String name = catProfileBinding.edtName.getText().toString();
            String description = catProfileBinding.edtDescription.getText().toString();
            Date dateOfBirth = selectedDateOfBirth;
            String gender = catProfileBinding.edmGender.getText().toString();
            String breed = catProfileBinding.edmBreed.getText().toString();
            String background = catProfileBinding.edmBackground.getText().toString();
            String medicalConditions = catProfileBinding.edmMedicalConditions.getText().toString();

            // Validate inputs
            boolean isValid = validateInputs(name);

            // Update profile if inputs are valid
            if (isValid) {
                updateProfile(profilePictureData, name, description, dateOfBirth, gender, breed, background, medicalConditions);
            }
        });
        //endregion

        //region Other listeners
        catProfileBinding.edtName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                catProfileBinding.txtfieldName.setErrorEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
    }

    private void loadProfilePicture(String profilePictureUrl) {
        if (profilePictureUrl == null) {
            // Load default picture if no profile picture exists
            Glide.with(getApplicationContext())
                    .load(R.drawable.baseline_emeowtions_24)
                    .into(catProfileBinding.imgProfilePicture);
        } else {
            // Load original profile picture
            Glide.with(getApplicationContext())
                    .load(profilePictureUrl)
                    .into(catProfileBinding.imgProfilePicture);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuthUtils.checkSignedIn(this);
    }

    private void deleteCat() {
        // Soft delete
        catsRef.document(catId)
                .update(
                        "updatedAt", Timestamp.now(),
                        "deleted", true
                )
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, String.format("Successfully deleted %s.", originalName), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to delete cat");
                });
    }

    private boolean validateInputs(String name) {
        // Reset errors
        catProfileBinding.txtfieldName.setErrorEnabled(false);

        // Validate name
        // Empty input
        if (name.isBlank()) {
            catProfileBinding.txtfieldName.setError(getString(R.string.name_required_error));
            catProfileBinding.txtfieldName.requestFocus();
            return false;
        }

        return true;
    }

    private void updateProfile(byte[] profilePictureData, String name, String description, Date dateOfBirth, String gender, String breed, String background, String medicalConditions) {
        // Check if profile picture was updated
        if (!isProfilePictureChanged) {
            // Update Firestore document
            updateCat(originalProfilePictureUrl, name, description, dateOfBirth, gender, breed, background, medicalConditions);
        } else {
            // Upload new profile picture before updating Firestore document
            profilePictureRef.putBytes(profilePictureData)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful())
                            throw task.getException();
                        return profilePictureRef.getDownloadUrl();
                    })
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Uri newProfilePictureUrl = task.getResult();
                            updateCat(newProfilePictureUrl.toString(), name, description, dateOfBirth, gender, breed, background, medicalConditions);
                        }
                    });
        }
    }

    private void updateCat(String profilePictureUrl, String name, String description, Date dateOfBirth, String gender, String breed, String background, String medicalConditions) {
        catsRef.document(catId)
                .update(
                        "profilePicture", profilePictureUrl,
                        "name", name,
                        "description", description,
                        "dateOfBirth", dateOfBirth == null ? null : new Timestamp(dateOfBirth),
                        "gender", gender,
                        "breed", breed,
                        "background", background,
                        "medicalConditions", medicalConditions,
                        "updatedAt", Timestamp.now()
                )
                .addOnSuccessListener(unused -> {
                    toggleEditMode(false);
                    Toast.makeText(this, "Successfully updated cat profile.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error updating cat profile", e);
                    Toast.makeText(this, "Failed to update cat profile.", Toast.LENGTH_SHORT).show();
                });
    }

    private void toggleEditMode(boolean enabled) {
        int textColor;

        // Check mode
        if (enabled) {
            textColor = getResources().getColor(R.color.black);
            catProfileBinding.appBarCatProfile.getMenu().clear(); // Remove edit button in menu actions
            catProfileBinding.btnRevertProfilePic.setVisibility(View.VISIBLE);     // Show profile picture action buttons
            catProfileBinding.btnEditProfilePic.setVisibility(View.VISIBLE);
            catProfileBinding.txtfieldDateofbirth.setEndIconVisible(originalDateOfBirth != null);  // Show clear date button if dateOfBirth is set
            catProfileBinding.layoutEditProfileButtons.setVisibility(View.VISIBLE);    // Show edit profile action buttons

            if (originalDescription.isBlank())
                catProfileBinding.edtDescription.setText("");

            // Set selections
            selectedDateOfBirth = originalDateOfBirth == null ? null : originalDateOfBirth.toDate();
        } else {
            textColor = getResources().getColor(R.color.gray_400);
            catProfileBinding.appBarCatProfile.getMenu().clear();
            catProfileBinding.appBarCatProfile.inflateMenu(R.menu.top_app_bar_cat_profile);  // Show edit button in menu actions
            catProfileBinding.btnRevertProfilePic.setVisibility(View.GONE);     // Hide profile picture action buttons
            catProfileBinding.btnEditProfilePic.setVisibility(View.GONE);
            catProfileBinding.txtfieldDateofbirth.setEndIconVisible(false);    // Hide clear date button
            catProfileBinding.layoutEditProfileButtons.setVisibility(View.GONE);   // Hide edit profile action buttons

            // Clear errors
            catProfileBinding.txtfieldName.setErrorEnabled(false);
            // Clear selections
            selectedDateOfBirth = null;
        }

        // Toggle fields
        catProfileBinding.txtfieldName.setCounterEnabled(enabled);
        catProfileBinding.txtfieldName.setEnabled(enabled);
        catProfileBinding.txtfieldDescription.setCounterEnabled(enabled);
        catProfileBinding.txtfieldDescription.setEnabled(enabled);
        catProfileBinding.txtfieldDateofbirth.setEnabled(enabled);
        catProfileBinding.txtfieldGender.setEnabled(enabled);
        catProfileBinding.txtfieldBreed.setEnabled(enabled);
        catProfileBinding.txtfieldBackground.setEnabled(enabled);
        catProfileBinding.txtfieldMedicalConditions.setEnabled(enabled);

        catProfileBinding.txtfieldGender.setEndIconVisible(enabled);
        catProfileBinding.txtfieldBreed.setEndIconVisible(enabled);
        catProfileBinding.txtfieldBackground.setEndIconVisible(enabled);
        catProfileBinding.txtfieldMedicalConditions.setEndIconVisible(enabled);

        catProfileBinding.edtName.setTextColor(textColor);
        catProfileBinding.edtDescription.setTextColor(textColor);
        catProfileBinding.edtDateofbirth.setTextColor(textColor);
        catProfileBinding.edmGender.setTextColor(textColor);
        catProfileBinding.edmBreed.setTextColor(textColor);
        catProfileBinding.edmBackground.setTextColor(textColor);
        catProfileBinding.edmMedicalConditions.setTextColor(textColor);
    }

    private void discardChanges() {
        SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy", Locale.getDefault());

        toggleEditMode(false);

        // Reset fields to original values
        loadProfilePicture(originalProfilePictureUrl);
        catProfileBinding.edtName.setText(originalName);
        catProfileBinding.edtDateofbirth.setText(originalDateOfBirth == null ? getString(R.string.not_set) : sdf.format(originalDateOfBirth.toDate()));
        catProfileBinding.edmGender.setText(originalGender, false);
        catProfileBinding.edmBreed.setText(originalBreed, false);
        catProfileBinding.edmBackground.setText(originalBackground, false);
        catProfileBinding.edmMedicalConditions.setText(originalMedicalConditions, false);

        if (originalDescription == null)
            catProfileBinding.edtDescription.setText(getString(R.string.no_description));
        else if (originalDescription.isEmpty())
            catProfileBinding.edtDescription.setText(getString(R.string.no_description));
        else
            catProfileBinding.edtDescription.setText(originalDescription);

        selectedDateOfBirth = null;
    }
}

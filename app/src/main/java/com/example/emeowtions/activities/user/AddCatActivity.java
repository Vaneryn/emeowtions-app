package com.example.emeowtions.activities.user;

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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;

import com.bumptech.glide.Glide;
import com.example.emeowtions.R;
import com.example.emeowtions.databinding.ActivityAddCatBinding;
import com.example.emeowtions.models.Cat;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.annotation.Nonnegative;

public class AddCatActivity extends AppCompatActivity {

    private static final String TAG = "AddCatActivity";
    private ActivityAddCatBinding addCatBinding;

    private FirebaseAuthUtils firebaseAuthUtils;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private CollectionReference catsRef;
    private StorageReference storageRef;
    private StorageReference catProfilePictureRef;

    private boolean isProfilePictureUploaded;
    private Date selectedDateOfBirth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase service instances
        firebaseAuthUtils = new FirebaseAuthUtils();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        // Initialize Firestore references
        catsRef = db.collection("cats");
        // Initialize Storage references
        storageRef = storage.getReference();

        // Get ViewBinding and set content view
        addCatBinding = ActivityAddCatBinding.inflate(getLayoutInflater());
        setContentView(addCatBinding.getRoot());

        // Enable edge-to-edge layout
        EdgeToEdge.enable(this);
        // Adjust main layout padding based on system bar size to prevent obstruction
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //region UI setups
        // Hide date of birth clear button
        addCatBinding.txtfieldDateofbirth.setEndIconVisible(false);

        // Create photo picker
        ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
                registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                    if (uri != null) {
                        isProfilePictureUploaded = true;
                        loadProfilePicture(uri.toString());
                    } else {
                        Log.d(TAG, "PhotoPicker - No media selected");
                    }
                });

        // Cancel dialog
        MaterialAlertDialogBuilder cancelDialog =
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.cancel_cat_creation)
                        .setMessage(R.string.cancel_dialog_generic_message)
                        .setNegativeButton(getString(R.string.no), (dialogInterface, i) -> {
                            // Unused
                        })
                        .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                            // Return to Add Cat screen
                            finish();
                        });

        // Load default dropdown menu selections
        addCatBinding.edmGender.setText(getString(R.string.unspecified), false);
        addCatBinding.edmBreed.setText(getString(R.string.unspecified), false);
        addCatBinding.edmBackground.setText(getString(R.string.unspecified), false);
        addCatBinding.edmMedicalConditions.setText(getString(R.string.unspecified), false);
        //endregion

        //region onClick listeners
        // appBarAddCat back button: return to Add Cat screen
        addCatBinding.appBarAddCat.setNavigationOnClickListener(view -> finish());

        // Profile picture action buttons
        addCatBinding.btnRevertProfilePic.setOnClickListener(view -> {
            // Revert profile picture
            isProfilePictureUploaded = false;
            loadProfilePicture(null);
        });
        addCatBinding.btnEditProfilePic.setOnClickListener(view -> {
            // Launch photo picker
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        // edtDateofbirth: DatePicker
        MaterialDatePicker<Long> datePicker =
                MaterialDatePicker.Builder.datePicker()
                        .setTitleText("Select date")
                        .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                        .build();

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
            addCatBinding.edtDateofbirth.setText(dateText);
            addCatBinding.txtfieldDateofbirth.setEndIconVisible(true);
        });

        addCatBinding.edtDateofbirth.setOnClickListener(view -> {
            // Prevent app crash from double DatePicker opening
            if (!datePicker.isAdded()) {
                datePicker.show(getSupportFragmentManager(), TAG);
            }
        });

        addCatBinding.edtDateofbirth.setOnLongClickListener(view -> true);

        addCatBinding.txtfieldDateofbirth.setEndIconOnClickListener(view -> {
            addCatBinding.edtDateofbirth.getText().clear();
            addCatBinding.txtfieldDateofbirth.setEndIconVisible(false);
        });

        // btnCancelAddCat: return to My Cats screen
        addCatBinding.btnCancelAddCat.setOnClickListener(view -> {
            cancelDialog.show();
        });

        // btnConfirmAddCat: add new cat
        addCatBinding.btnConfirmAddCat.setOnClickListener(view -> {
            // Get inputs
            byte[] profilePictureData = null;
            if (isProfilePictureUploaded) {
                addCatBinding.imgCatProfilePicture.setDrawingCacheEnabled(true);
                addCatBinding.imgCatProfilePicture.buildDrawingCache();
                Bitmap bitmap = ((BitmapDrawable) addCatBinding.imgCatProfilePicture.getDrawable()).getBitmap();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                profilePictureData = baos.toByteArray();
            }
            
            String name = addCatBinding.edtName.getText().toString();
            String description = addCatBinding.edtDescription.getText().toString();
            Timestamp dateOfBirth = selectedDateOfBirth == null ? null : new Timestamp(selectedDateOfBirth);
            String gender = addCatBinding.edmGender.getText().toString();
            String breed = addCatBinding.edmBreed.getText().toString();
            String background = addCatBinding.edmBackground.getText().toString();
            String medicalConditions = addCatBinding.edmMedicalConditions.getText().toString();

            // Validate inputs
            boolean isValid = validateInputs(name);

            // Create new cat if inputs are valid
            if (isValid) {
                createCat(profilePictureData, name, description, dateOfBirth, gender, breed, background, medicalConditions);
            }
        });
        //endregion

        //region Other listeners
        // txtCatName - TextChanged: update name in profile banner
        addCatBinding.edtName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                addCatBinding.txtfieldName.setErrorEnabled(false);
                String catName = addCatBinding.edtName.getText().toString();

                if (catName.isBlank()) {
                    addCatBinding.txtCatName.setText(R.string.name);
                } else {
                    addCatBinding.txtCatName.setText(catName);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        // txtCatDescription - TextChanged: update description in profile banner
        addCatBinding.edtDescription.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String catDescription = addCatBinding.edtDescription.getText().toString();

                if (catDescription.isBlank()) {
                    addCatBinding.txtCatDescription.setText(R.string.description_of_cat);
                } else {
                    addCatBinding.txtCatDescription.setText(catDescription);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
        //endregion

        //region Load data
        catsRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.w(TAG, "Failed to listen on user's cat changes", error);
                return;
            }
            if (value != null && !value.isEmpty()) {
                // Convert query snapshot to a list of Cats
                List<Cat> cats = value.toObjects(Cat.class);


            }
        });
        //endregion
    }

    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuthUtils.checkSignedIn(this);
    }

    private void loadProfilePicture(String profilePictureUrl) {
        if (profilePictureUrl == null) {
            // Load default picture if no profile picture exists
            Glide.with(getApplicationContext())
                    .load(R.drawable.baseline_emeowtions_24)
                    .into(addCatBinding.imgCatProfilePicture);
        } else {
            // Load original profile picture
            Glide.with(getApplicationContext())
                    .load(profilePictureUrl)
                    .into(addCatBinding.imgCatProfilePicture);
        }
    }

    private boolean validateInputs(String name) {
        // Reset errors
        addCatBinding.txtfieldName.setErrorEnabled(false);

        // Validate cat name
        // Empty input
        if (name.isBlank()) {
            addCatBinding.txtfieldName.setError("Name is required");
            addCatBinding.txtfieldName.requestFocus();
            return false;
        }

        return true;
    }

    private void createCat(byte[] profilePictureData, String name, String description, Timestamp dateOfBirth, String gender, String breed, String background, String medicalConditions) {
        Cat newCat = new Cat(
                name,
                gender,
                breed,
                dateOfBirth,
                background,
                medicalConditions,
                description,
                null,
                firebaseAuthUtils.getUid(),
                Timestamp.now(),
                Timestamp.now(),
                false
        );

        // Add Cat document
        catsRef.add(newCat)
                .addOnSuccessListener(documentReference -> {
                    // Check if profile picture was uploaded
                    if (!isProfilePictureUploaded) {
                        // No profile picture
                        Toast.makeText(this, "Successfully added new cat (" + name + ")." , Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        // Upload profile picture to storage
                        catProfilePictureRef = storageRef.child("images/cats/" + documentReference.getId() + "/profile_picture.jpg");
                        catProfilePictureRef.putBytes(profilePictureData)
                                .continueWithTask(imageUploadTask -> {
                                    if (!imageUploadTask.isSuccessful()) {
                                        throw imageUploadTask.getException();
                                    }
                                    // Continue with the task to get the download URL
                                    return catProfilePictureRef.getDownloadUrl();
                                })
                                .addOnCompleteListener(imageUploadTask -> {
                                    Uri profilePictureUrl = imageUploadTask.getResult();

                                    // Update Cat's profilePicture
                                    catsRef.document(documentReference.getId())
                                            .update("profilePicture", profilePictureUrl)
                                            .addOnCompleteListener(imageUpdateTask -> {
                                                Toast.makeText(this, "Successfully added new cat (" + name + ")." , Toast.LENGTH_LONG).show();
                                                finish();
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.w(TAG, "Failed to update Cat profile picture", e);
                                            });
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding Cat document", e);
                    Toast.makeText(this, "Unable to create new cat, please try again later.", Toast.LENGTH_LONG).show();
                });
    }
}
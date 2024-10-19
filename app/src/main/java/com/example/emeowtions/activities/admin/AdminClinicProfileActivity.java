package com.example.emeowtions.activities.admin;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.emeowtions.R;
import com.example.emeowtions.adapters.VeterinaryClinicAdapter;
import com.example.emeowtions.databinding.ActivityAdminClinicProfileBinding;
import com.example.emeowtions.models.VeterinaryClinic;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class AdminClinicProfileActivity extends AppCompatActivity {

    private static final String TAG = "AdminClinicProfileActivity";

    // Firebase variables
    private FirebaseAuthUtils firebaseAuthUtils;
    private FirebaseFirestore db;
    private CollectionReference clinicsRef;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private StorageReference clinicLogoRef;

    // Layout variables
    private ActivityAdminClinicProfileBinding binding;

    // Private variables
    private String originalLogoUrl;
    private String originalName;
    private String originalEmail;
    private String originalPhoneNumber;
    private String originalAddress;
    private String originalDescription;

    private String clinicId;
    private boolean isLogoChanged;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get intent data
        Intent passedIntent = getIntent();
        clinicId = passedIntent.getStringExtra(VeterinaryClinicAdapter.KEY_CLINIC_ID);

        // Initialize Firebase service instances
        firebaseAuthUtils = new FirebaseAuthUtils();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        // Initialize Firestore references
        clinicsRef = db.collection("veterinaryClinics");
        // Initialize Storage references
        storageRef = storage.getReference();
        clinicLogoRef = storageRef.child("images/veterinaryClinics/" + clinicId + "/logo.jpg");

        // Get ViewBinding and set content view
        binding = ActivityAdminClinicProfileBinding.inflate(getLayoutInflater());
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
        MaterialAlertDialogBuilder deleteDialog =
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.delete)
                        .setMessage(R.string.delete_clinic_dialog_message)
                        .setNegativeButton(R.string.no, (dialogInterface, i) -> {
                            // Do nothing
                        })
                        .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                            deleteClinic();
                            finish();
                        });

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

        ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
                registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                    if (uri != null) {
                        Glide.with(getApplicationContext())
                                .load(uri)
                                .into(binding.imgClinicLogo);
                        isLogoChanged = true;
                    } else {
                        Log.d(TAG, "PhotoPicker - No media selected");
                    }
                });
        //endregion

        //region Navigation Listeners
        binding.appBarClinicProfile.setNavigationOnClickListener(view -> finish());

        binding.appBarClinicProfile.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.action_edit_clinic_profile) {
                toggleEditMode(true);
            } else if (itemId == R.id.action_delete_clinic) {
                deleteDialog.show();
            }

            return false;
        });
        //endregion

        //region Load Data
        isLogoChanged = false;
        toggleEditMode(false);

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
                        SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault());

                        // Save original field values
                        originalLogoUrl = clinic.getLogoUrl();
                        originalName = clinic.getName();
                        originalEmail = clinic.getEmail();
                        originalPhoneNumber = clinic.getPhoneNumber();
                        originalAddress = clinic.getAddress();
                        originalDescription = clinic.getDescription();

                        // Load banner
                        loadLogo(originalLogoUrl);
                        binding.txtBannerName.setText(originalName);
                        binding.txtBannerJoinedDate.setText(String.format("Joined %s", sdf.format(clinic.getCreatedAt().toDate())));
                        binding.txtBannerUpdatedDate.setText(String.format("Updated %s", sdf.format(clinic.getUpdatedAt().toDate())));

                        // Load fields
                        binding.edtName.setText(originalName);
                        binding.edtEmail.setText(originalEmail);
                        binding.edtPhoneNumber.setText(originalPhoneNumber);
                        binding.edtAddress.setText(originalAddress);
                        binding.edtDescription.setText(originalDescription);
                    }
                });
        //endregion

        //region onClick Listeners
        binding.btnRevertLogo.setOnClickListener(view -> {
            isLogoChanged = false;
            loadLogo(originalLogoUrl);
        });

        binding.btnEditLogo.setOnClickListener(view -> {
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        binding.btnCancel.setOnClickListener(view -> {
            // Get inputs
            String name = binding.edtName.getText().toString();
            String email = binding.edtEmail.getText().toString();
            String phoneNumber = binding.edtPhoneNumber.getText().toString();
            String address = binding.edtAddress.getText().toString();
            String description = binding.edtDescription.getText().toString();

            // Check for changes
            if (isLogoChanged || !name.equals(originalName) || !email.equals(originalEmail) ||
                    !phoneNumber.equals(originalPhoneNumber) || !address.equals(originalAddress) || !description.equals(originalDescription)) {
                cancelDialog.show();
            } else {
                discardChanges();
            }
        });

        binding.btnConfirm.setOnClickListener(view -> {
            // Get inputs
            byte[] logoData = null;
            if (isLogoChanged) {
                binding.imgClinicLogo.setDrawingCacheEnabled(true);
                binding.imgClinicLogo.buildDrawingCache();
                Bitmap bitmap = ((BitmapDrawable) binding.imgClinicLogo.getDrawable()).getBitmap();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                logoData = baos.toByteArray();
            }

            String name = binding.edtName.getText().toString();
            String email = binding.edtEmail.getText().toString();
            String phoneNumber = binding.edtPhoneNumber.getText().toString();
            String address = binding.edtAddress.getText().toString();
            String description = binding.edtDescription.getText().toString();

            // Validate inputs
            boolean isValid = validateInputs(logoData, name, email, phoneNumber, address, description);

            // Update clinic if inputs are valid
            if (isValid) {
                updateClinicProfile(logoData, name, email, phoneNumber, address, description);
            }
        });
        //endregion
    }

    // Consolidates profile changes
    private void updateClinicProfile(byte[] logoData, String name, String email, String phoneNumber, String address, String description) {
        // Check if logo was updated
        if (!isLogoChanged) {
            // Update Firestore document
            updateVeterinaryClinic(originalLogoUrl, name, email, phoneNumber, address, description);
        } else {
            // Upload new logo before updating Firestore document
            clinicLogoRef.putBytes(logoData)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful())
                            throw task.getException();
                        return clinicLogoRef.getDownloadUrl();
                    })
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Uri newLogoUrl = task.getResult();
                            updateVeterinaryClinic(newLogoUrl.toString(), name, email, phoneNumber, address, description);
                        }
                    });
        }
    }

    // Updates VeterinaryClinic document
    private void updateVeterinaryClinic(String logoUrl, String name, String email, String phoneNumber, String address, String description) {
        clinicsRef.document(clinicId)
                .update(
                        "logoUrl", logoUrl,
                        "name", name,
                        "email", email,
                        "phoneNumber", phoneNumber,
                        "address", address,
                        "description", description,
                        "updatedAt", Timestamp.now()
                )
                .addOnSuccessListener(unused -> {
                    isLogoChanged = false;
                    toggleEditMode(false);
                    Toast.makeText(this, "Successfully updated clinic profile.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "updateVeterinaryClinic: Failed to update VeterinaryClinic", e);
                    Toast.makeText(this, "Failed to update clinic profile.", Toast.LENGTH_SHORT).show();
                });
    }

    // Soft deletes clinic
    private void deleteClinic() {
        clinicsRef.document(clinicId)
                .update(
                        "updatedAt", Timestamp.now(),
                        "deleted", true
                )
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, String.format("Successfully deleted %s.", originalName), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "deleteClinic: Failed to delete cat", e);
                    Toast.makeText(this, "Failed to delete clinic, please try again later.", Toast.LENGTH_SHORT).show();
                });
    }

    private boolean validateInputs(byte[] logoData, String name, String email, String phoneNumber, String address, String description) {
        // Reset errors
        binding.txtfieldName.setErrorEnabled(false);
        binding.txtfieldEmail.setErrorEnabled(false);
        binding.txtfieldPhoneNumber.setErrorEnabled(false);
        binding.txtfieldAddress.setErrorEnabled(false);
        binding.txtfieldDescription.setErrorEnabled(false);

        // Validate logo
        // Empty input
        if (isLogoChanged && logoData == null) {
            Toast.makeText(this, "Logo is required", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Validate name
        // Empty input
        if (name.isBlank()) {
            binding.txtfieldName.setError(getString(R.string.name_required_error));
            binding.txtfieldName.requestFocus();
            return false;
        }

        // Validate email
        // Empty input
        if (email.isBlank()) {
            binding.txtfieldEmail.setError(getString(R.string.email_required_error));
            binding.txtfieldEmail.requestFocus();
            return false;
        }
        // Email format
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.txtfieldEmail.setError("Email format is invalid");
            binding.txtfieldEmail.requestFocus();
            return false;
        }

        // Validate phone number
        // Empty input
        if (phoneNumber.isBlank()) {
            binding.txtfieldPhoneNumber.setError(getString(R.string.phone_number_required_error));
            binding.txtfieldPhoneNumber.requestFocus();
            return false;
        }

        // Validate address
        // Empty input
        if (address.isBlank()) {
            binding.txtfieldAddress.setError(getString(R.string.address_required_error));
            binding.txtfieldAddress.requestFocus();
            return false;
        }

        // Validate description
        // Empty input
        if (description.isBlank()) {
            binding.txtfieldDescription.setError(getString(R.string.description_required_error));
            binding.txtfieldDescription.requestFocus();
            return false;
        }

        return true;
    }

    // Toggles edit mode on and off
    private void toggleEditMode(boolean enabled) {
        int textColor;

        binding.appBarClinicProfile.getMenu().clear();

        // Check mode
        if (enabled) {
            textColor = getResources().getColor(R.color.black);
            binding.btnRevertLogo.setVisibility(View.VISIBLE);
            binding.btnEditLogo.setVisibility(View.VISIBLE);
            binding.layoutEditButtons.setVisibility(View.VISIBLE);
        } else {
            textColor = getResources().getColor(R.color.gray_400);
            binding.appBarClinicProfile.inflateMenu(R.menu.top_app_bar_admin_clinic_profile);
            binding.btnRevertLogo.setVisibility(View.GONE);
            binding.btnEditLogo.setVisibility(View.GONE);
            binding.layoutEditButtons.setVisibility(View.GONE);
            // Clear errors
            binding.txtfieldName.setErrorEnabled(false);
            binding.txtfieldEmail.setErrorEnabled(false);
            binding.txtfieldPhoneNumber.setErrorEnabled(false);
            binding.txtfieldAddress.setErrorEnabled(false);
            binding.txtfieldDescription.setErrorEnabled(false);
        }

        // Toggle fields
        binding.txtfieldName.setEnabled(enabled);
        binding.txtfieldEmail.setEnabled(enabled);
        binding.txtfieldPhoneNumber.setEnabled(enabled);
        binding.txtfieldAddress.setEnabled(enabled);
        binding.txtfieldDescription.setEnabled(enabled);

        binding.txtfieldName.setCounterEnabled(enabled);
        binding.txtfieldAddress.setCounterEnabled(enabled);
        binding.txtfieldDescription.setCounterEnabled(enabled);

        binding.edtName.setTextColor(textColor);
        binding.edtEmail.setTextColor(textColor);
        binding.edtPhoneNumber.setTextColor(textColor);
        binding.edtAddress.setTextColor(textColor);
        binding.edtDescription.setTextColor(textColor);
    }

    private void discardChanges() {
        toggleEditMode(false);

        // Reset fields to original values
        isLogoChanged = false;
        loadLogo(originalLogoUrl);
        binding.edtName.setText(originalName);
        binding.edtEmail.setText(originalEmail);
        binding.edtPhoneNumber.setText(originalPhoneNumber);
        binding.edtAddress.setText(originalAddress);
        binding.edtDescription.setText(originalDescription);
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
}
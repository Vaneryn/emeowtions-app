package com.example.emeowtions.fragments.veterinary;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.emeowtions.R;
import com.example.emeowtions.activities.veterinary.VetMainActivity;
import com.example.emeowtions.databinding.FragmentVetClinicProfileBinding;
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

public class VetClinicProfileFragment extends Fragment {

    private static final String TAG = "VetClinicProfileFragment";
    private SharedPreferences sharedPreferences;
    private VetMainActivity parentActivity;

    // Firebase variables
    private FirebaseFirestore db;
    private CollectionReference clinicsRef;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private StorageReference clinicLogoRef;

    // Layout variables
    private FragmentVetClinicProfileBinding binding;

    // Private variables
    private String originalLogoUrl;
    private String originalName;
    private String originalEmail;
    private String originalPhoneNumber;
    private String originalAddress;
    private String originalDescription;

    private String veterinaryClinicId;
    private boolean isLogoChanged;

    public VetClinicProfileFragment() {
        super(R.layout.fragment_vet_clinic_profile);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentVetClinicProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get parent activity
        parentActivity = (VetMainActivity) getActivity();

        // Shared preferences
        sharedPreferences = parentActivity.getSharedPreferences("com.emeowtions", Context.MODE_PRIVATE);
        veterinaryClinicId = sharedPreferences.getString("veterinaryClinicId", null);

        // Initialize Firebase service instances
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        // Initialize Firestore references
        clinicsRef = db.collection("veterinaryClinics");
        // Initialize Storage references
        storageRef = storage.getReference();
        clinicLogoRef = storageRef.child("images/veterinaryClinics/" + veterinaryClinicId + "/logo.jpg");

        //region UI Setups
        ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
                registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                    if (uri != null) {
                        Glide.with(parentActivity.getApplicationContext())
                                .load(uri)
                                .into(binding.imgClinicLogo);
                        isLogoChanged = true;
                    } else {
                        Log.d(TAG, "PhotoPicker - No media selected");
                    }
                });

        MaterialAlertDialogBuilder cancelDialog =
                new MaterialAlertDialogBuilder(parentActivity)
                        .setTitle(R.string.unsaved_changes)
                        .setMessage(R.string.edit_profile_cancel_message)
                        .setNegativeButton(getString(R.string.no), (dialogInterface, i) -> {
                            // Do nothing
                        })
                        .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                            discardChanges();
                        });
        //endregion

        //region Load Data
        isLogoChanged = false;

        clinicsRef.document(veterinaryClinicId)
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
        binding.btnRevertLogo.setOnClickListener(view1 -> {
            isLogoChanged = false;
            loadLogo(originalLogoUrl);
        });

        binding.btnEditLogo.setOnClickListener(view1 -> {
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        binding.btnCancel.setOnClickListener(view1 -> {
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

        binding.btnConfirm.setOnClickListener(view1 -> {
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

        //region Other Listeners
        bindTextChangeListeners();
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
        clinicsRef.document(veterinaryClinicId)
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
                    Toast.makeText(parentActivity, "Successfully updated clinic profile.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "updateVeterinaryClinic: Failed to update VeterinaryClinic", e);
                    Toast.makeText(parentActivity, "Failed to update clinic profile.", Toast.LENGTH_SHORT).show();
                });
    }

    // Validates inputs for errors
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
            Toast.makeText(parentActivity, "Logo is required", Toast.LENGTH_SHORT).show();
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
    public void toggleEditMode(boolean enabled) {
        int textColor;

        if (parentActivity.binding == null)
            return;

        parentActivity.binding.topAppBar.getMenu().clear();

        // Check mode
        if (enabled) {
            textColor = ContextCompat.getColor(parentActivity, R.color.black);
            binding.btnRevertLogo.setVisibility(View.VISIBLE);
            binding.btnEditLogo.setVisibility(View.VISIBLE);
            binding.layoutEditButtons.setVisibility(View.VISIBLE);
        } else {
            textColor = ContextCompat.getColor(parentActivity, R.color.gray_400);
            parentActivity.binding.topAppBar.inflateMenu(R.menu.top_app_bar_vet_clinic_profile);
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

    // Discards all input changes
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
            Glide.with(parentActivity.getApplicationContext())
                    .load(R.drawable.baseline_local_hospital_24)
                    .into(binding.imgClinicLogo);
        } else {
            // Load original profile picture
            Glide.with(parentActivity.getApplicationContext())
                    .load(logoUrl)
                    .into(binding.imgClinicLogo);
        }
    }

    private void bindTextChangeListeners() {
        binding.edtName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                binding.txtfieldName.setErrorEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        binding.edtEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                binding.txtfieldEmail.setErrorEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        binding.edtPhoneNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                binding.txtfieldPhoneNumber.setErrorEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        binding.edtAddress.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                binding.txtfieldAddress.setErrorEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        binding.edtDescription.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                binding.txtfieldDescription.setErrorEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
    }
}
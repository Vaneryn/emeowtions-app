package com.example.emeowtions.activities.admin;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.emeowtions.R;
import com.example.emeowtions.adapters.VeterinaryClinicRegistrationAdapter;
import com.example.emeowtions.databinding.ActivityClinicRegistrationDetailsBinding;
import com.example.emeowtions.enums.VeterinaryClinicRegistrationStatus;
import com.example.emeowtions.models.VeterinaryClinic;
import com.example.emeowtions.models.VeterinaryClinicRegistration;
import com.example.emeowtions.utils.FirebaseAuthUtils;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class ClinicRegistrationDetailsActivity extends AppCompatActivity {

    private static final String TAG = "ClinicRegistrationDetailsActivity";

    // Firebase variables
    private FirebaseAuthUtils firebaseAuthUtils;
    private FirebaseFirestore db;
    private CollectionReference vetRegsRef;
    private CollectionReference clinicsRef;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private StorageReference vetRegLogoRef;
    private StorageReference clinicLogoRef;

    // Layout variables
    private ActivityClinicRegistrationDetailsBinding binding;

    // Private variables
    private String originalLogoUrl;
    private String originalName;
    private String originalEmail;
    private String originalPhoneNumber;
    private String originalAddress;
    private String originalDescription;
    private String originalRejectionReason;

    private String vetRegId;
    private boolean isLogoChanged;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get intent data
        Intent passedIntent = getIntent();
        vetRegId = passedIntent.getStringExtra(VeterinaryClinicRegistrationAdapter.KEY_VET_REG_ID);

        // Initialize Firebase service instances
        firebaseAuthUtils = new FirebaseAuthUtils();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        // Initialize Firestore references
        vetRegsRef = db.collection("veterinaryClinicRegistrations");
        clinicsRef = db.collection("veterinaryClinics");
        // Initialize Storage references
        storageRef = storage.getReference();
        vetRegLogoRef = storageRef.child("images/veterinaryClinicRegistrations/" + vetRegId + "/logo.jpg");

        // Get ViewBinding and set content view
        binding = ActivityClinicRegistrationDetailsBinding.inflate(getLayoutInflater());
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
        MaterialAlertDialogBuilder approveDialog =
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.approve)
                        .setMessage("Approve this clinic?")
                        .setNegativeButton(R.string.no, (dialogInterface, i) -> {
                            // Do nothing
                        })
                        .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                            approveRegistration();
                        });

        MaterialAlertDialogBuilder restoreDialog =
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.restore)
                        .setMessage("Restore this registration to Pending status?")
                        .setNegativeButton(R.string.no, (dialogInterface, i) -> {
                            // Do nothing
                        })
                        .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                            restoreRegistration();
                        });

        // Photo picker
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

        //region Navigation Listeners
        binding.appBarRegistrationDetails.setNavigationOnClickListener(view -> finish());

        binding.appBarRegistrationDetails.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            // Pending mode
            if (itemId == R.id.action_edit_clinic_details) {
                toggleEditMode(true);
            } else if (itemId == R.id.action_reject_clinic) {
                showRejectDialog();
            } else if (itemId == R.id.action_approve_clinic) {
                approveDialog.show();
            }
            // Rejected mode
            else if (itemId == R.id.action_restore) {
                restoreDialog.show();
            }

            return false;
        });
        //endregion

        //region Load Data
        isLogoChanged = false;

        vetRegsRef.document(vetRegId)
                .addSnapshotListener((value, error) -> {
                    // Error
                    if (error != null) {
                        Log.w(TAG, "onCreate: Failed to listen on current VeterinaryClinicRegistration changes", error);
                        return;
                    }
                    // Success
                    if (value != null && value.exists()) {
                        VeterinaryClinicRegistration vetReg = value.toObject(VeterinaryClinicRegistration.class);
                        SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault());

                        // Switch to Pending, Approved, or Rejected view mode
                        switchViewMode(vetReg.getStatus());

                        // Save original field values
                        originalLogoUrl = vetReg.getLogoUrl();
                        originalName = vetReg.getName();
                        originalEmail = vetReg.getEmail();
                        originalPhoneNumber = vetReg.getPhoneNumber();
                        originalAddress = vetReg.getAddress();
                        originalDescription = vetReg.getDescription();
                        originalRejectionReason = vetReg.getRejectionReason();

                        // Load banner
                        loadLogo(originalLogoUrl);
                        binding.txtBannerName.setText(originalName);
                        binding.txtBannerCreatedDate.setText(String.format("Registered %s", sdf.format(vetReg.getCreatedAt().toDate())));
                        binding.txtBannerUpdatedDate.setText(
                                String.format("%s%s",
                                        (vetReg.getStatus().equals(VeterinaryClinicRegistrationStatus.APPROVED.getTitle()) ? "Approved " :
                                        vetReg.getStatus().equals(VeterinaryClinicRegistrationStatus.REJECTED.getTitle()) ? "Rejected " :
                                        "Updated "),
                                        sdf.format(vetReg.getUpdatedAt().toDate())
                                )
                        );

                        // Load fields
                        binding.edtName.setText(originalName);
                        binding.edtEmail.setText(originalEmail);
                        binding.edtPhoneNumber.setText(originalPhoneNumber);
                        binding.edtAddress.setText(originalAddress);
                        binding.edtDescription.setText(originalDescription);
                        binding.edtRejectionReason.setText(originalRejectionReason);
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

            // Update registration if inputs are valid
            if (isValid) {
                updateRegistration(logoData, name, email, phoneNumber, address, description);
            }
        });
        //endregion
    }

    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuthUtils.checkSignedIn(this);
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

    private void updateRegistration(byte[] logoData, String name, String email, String phoneNumber, String address, String description) {
        // Check if logo was updated
        if (!isLogoChanged) {
            // Update Firestore document
            updateVeterinaryClinicRegistration(originalLogoUrl, name, email, phoneNumber, address, description);
        } else {
            // Upload new logo before updating Firestore document
            vetRegLogoRef.putBytes(logoData)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful())
                            throw task.getException();
                        return vetRegLogoRef.getDownloadUrl();
                    })
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Uri newLogoUrl = task.getResult();
                            updateVeterinaryClinicRegistration(newLogoUrl.toString(), name, email, phoneNumber, address, description);
                        }
                    });
        }
    }

    private void updateVeterinaryClinicRegistration(String logoUrl, String name, String email, String phoneNumber, String address, String description) {
        vetRegsRef.document(vetRegId)
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
                    Toast.makeText(this, "Successfully updated registration details.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "updateVeterinaryClinicRegistration: Failed to update VeterinaryClinicRegistration", e);
                    Toast.makeText(this, "Failed to update registration details.", Toast.LENGTH_SHORT).show();
                });

    }

    // Sets registration status to Rejected
    private void rejectRegistration(String reason) {
        vetRegsRef.document(vetRegId)
                .update(
                        "status", VeterinaryClinicRegistrationStatus.REJECTED.getTitle(),
                        "rejectionReason", reason,
                        "updatedAt", Timestamp.now()
                )
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Successfully rejected.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "rejectRegistration: Failed to update VeterinaryClinicRegistration status", e);
                });
    }

    // Sets registration status to Approved
    private void approveRegistration() {
        vetRegsRef.document(vetRegId)
                .update(
                        "status", VeterinaryClinicRegistrationStatus.APPROVED.getTitle(),
                        "updatedAt", Timestamp.now()
                )
                .addOnSuccessListener(unused -> {
                    // Create new VeterinaryClinic
                    createVeterinaryClinic();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "approveRegistration: Failed to update VeterinaryClinicRegistration status", e);
                });
    }

    // Creates new VeterinaryClinic document
    private void createVeterinaryClinic() {
        // Get inputs
        String name = binding.edtName.getText().toString();
        String email = binding.edtEmail.getText().toString();
        String phoneNumber = binding.edtPhoneNumber.getText().toString();
        String address = binding.edtAddress.getText().toString();
        String description = binding.edtDescription.getText().toString();

        // Add document
        VeterinaryClinic newClinic = new VeterinaryClinic(
                name,
                description,
                address,
                email,
                phoneNumber,
                null,
                Timestamp.now(),
                Timestamp.now(),
                false
        );

        clinicsRef.add(newClinic)
                .addOnSuccessListener(documentReference -> {
                    // Copy clinic logo to new path for officially approved clinic
                    vetRegLogoRef.getBytes(1024 * 1024)
                            .addOnSuccessListener(bytes -> {
                                // Upload to new path
                                clinicLogoRef = storageRef.child("images/veterinaryClinics/" + documentReference.getId() + "/logo.jpg");
                                clinicLogoRef.putBytes(bytes)
                                        .continueWithTask(logoCopyTask -> {
                                            // Get URL
                                            if (!logoCopyTask.isSuccessful()) {
                                                throw logoCopyTask.getException();
                                            }
                                            return clinicLogoRef.getDownloadUrl();
                                        })
                                        .addOnCompleteListener(task -> {
                                            // Update logoUrl
                                            Uri logoUrl = task.getResult();
                                            clinicsRef.document(documentReference.getId())
                                                    .update("logoUrl", logoUrl)
                                                    .addOnSuccessListener(unused -> {
                                                        Toast.makeText(this, "Successfully approved " + name + ".", Toast.LENGTH_SHORT).show();
                                                        finish();
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Log.w(TAG, "createVeterinaryClinic: Failed to update logoUrl", e);
                                                        Toast.makeText(this, "Failed to approve, please try again later.", Toast.LENGTH_SHORT).show();
                                                    });
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.w(TAG, "createVeterinaryClinic: Failed to download clinic logo", e);
                                Toast.makeText(this, "Failed to approve, please try again later.", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "createVeterinaryClinic: Failed to create new VeterinaryClinic document", e);
                    Toast.makeText(this, "Failed to approve, please try again later.", Toast.LENGTH_SHORT).show();
                });
    }

    // Restores registration to Pending status
    private void restoreRegistration() {
        vetRegsRef.document(vetRegId)
                .update(
                        "status", VeterinaryClinicRegistrationStatus.PENDING.getTitle(),
                        "rejectionReason", null,
                        "updatedAt", Timestamp.now()
                )
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Successfully restored.", Toast.LENGTH_SHORT).show();
                    switchViewMode(VeterinaryClinicRegistrationStatus.PENDING.getTitle());
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "restoreRegistration: Failed to update VeterinaryClinicRegistration status", e);
                });
    }

    // Switches between Pending, Approved, or Rejected view mode
    private void switchViewMode(String status) {
        if (status.equals(VeterinaryClinicRegistrationStatus.PENDING.getTitle())) {
            toggleEditMode(false);
            binding.appBarRegistrationDetails.getMenu().clear();
            binding.appBarRegistrationDetails.inflateMenu(R.menu.top_app_bar_clinic_registration_details);
            binding.txtfieldRejectionReason.setVisibility(View.GONE);
        } else if (status.equals(VeterinaryClinicRegistrationStatus.APPROVED.getTitle())) {
            toggleEditMode(false);
            binding.appBarRegistrationDetails.getMenu().clear();
            binding.txtfieldRejectionReason.setVisibility(View.GONE);
        } else if (status.equals(VeterinaryClinicRegistrationStatus.REJECTED.getTitle())) {
            toggleEditMode(false);
            binding.appBarRegistrationDetails.getMenu().clear();
            binding.appBarRegistrationDetails.inflateMenu(R.menu.top_app_bar_clinic_registration_details_rejected);
            binding.txtfieldRejectionReason.setVisibility(View.VISIBLE);
        }
    }

    // Toggles edit mode on and off
    private void toggleEditMode(boolean enabled) {
        int textColor;

        binding.appBarRegistrationDetails.getMenu().clear();

        // Check mode
        if (enabled) {
            textColor = getResources().getColor(R.color.black);
            binding.btnRevertLogo.setVisibility(View.VISIBLE);
            binding.btnEditLogo.setVisibility(View.VISIBLE);
            binding.layoutEditButtons.setVisibility(View.VISIBLE);
        } else {
            textColor = getResources().getColor(R.color.gray_400);
            binding.appBarRegistrationDetails.inflateMenu(R.menu.top_app_bar_clinic_registration_details);
            binding.btnRevertLogo.setVisibility(View.GONE);
            binding.btnEditLogo.setVisibility(View.GONE);
            binding.layoutEditButtons.setVisibility(View.GONE);
            // Clear errors
            binding.txtfieldName.setErrorEnabled(false);
            binding.txtfieldEmail.setErrorEnabled(false);
            binding.txtfieldPhoneNumber.setErrorEnabled(false);
            binding.txtfieldAddress.setErrorEnabled(false);
            binding.txtfieldDescription.setErrorEnabled(false);
            binding.txtfieldRejectionReason.setErrorEnabled(false);
        }

        // Toggle fields
        binding.txtfieldName.setEnabled(enabled);
        binding.txtfieldEmail.setEnabled(enabled);
        binding.txtfieldPhoneNumber.setEnabled(enabled);
        binding.txtfieldAddress.setEnabled(enabled);
        binding.txtfieldDescription.setEnabled(enabled);
        binding.txtfieldRejectionReason.setEnabled(enabled);

        binding.txtfieldName.setCounterEnabled(enabled);
        binding.txtfieldAddress.setCounterEnabled(enabled);
        binding.txtfieldDescription.setCounterEnabled(enabled);
        binding.txtfieldRejectionReason.setCounterEnabled(enabled);

        binding.edtName.setTextColor(textColor);
        binding.edtEmail.setTextColor(textColor);
        binding.edtPhoneNumber.setTextColor(textColor);
        binding.edtAddress.setTextColor(textColor);
        binding.edtDescription.setTextColor(textColor);
        binding.edtRejectionReason.setTextColor(textColor);
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

    private void showRejectDialog() {
        // Dialog for getting rejection reason
        View reasonDialogLayout = LayoutInflater.from(this).inflate(R.layout.dialog_reject, null);
        TextInputLayout txtfieldReason = reasonDialogLayout.findViewById(R.id.txtfield_reason);
        TextInputEditText edtReason = reasonDialogLayout.findViewById(R.id.edt_reason);

        MaterialAlertDialogBuilder reasonDialogBuilder =
                new MaterialAlertDialogBuilder(this)
                        .setView(reasonDialogLayout)
                        .setTitle(getString(R.string.reject))
                        .setMessage("Provide rejection reason.")
                        .setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
                            // Do nothing
                        })
                        .setPositiveButton(R.string.confirm, ((dialogInterface, i) -> {
                            // Unused
                        }));

        AlertDialog reasonDialog = reasonDialogBuilder.create();
        reasonDialog.show();

        // Override the positive button's click behavior
        reasonDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            // Validate reason
            String reason = edtReason.getText().toString();
            if (reason.isBlank()) {
                txtfieldReason.setError(getString(R.string.reason_required_error));
            } else {
                rejectRegistration(reason);
                reasonDialog.dismiss();
            }
        });

        // Reset error when text is changed
        edtReason.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                txtfieldReason.setErrorEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
    }
}
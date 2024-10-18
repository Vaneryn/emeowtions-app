package com.example.emeowtions.activities.common;

import android.content.DialogInterface;
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
import com.example.emeowtions.databinding.ActivityAdminMainBinding;
import com.example.emeowtions.databinding.ActivityRegisterBinding;
import com.example.emeowtions.databinding.ActivityRegisterClinicBinding;
import com.example.emeowtions.enums.VeterinaryClinicRegistrationStatus;
import com.example.emeowtions.models.VeterinaryClinicRegistration;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;

public class RegisterClinicActivity extends AppCompatActivity {

    private static final String TAG = "RegisterClinicActivity";
    private ActivityRegisterClinicBinding registerClinicBinding;
    private TextInputLayout txtfieldClinicEmail;
    private TextInputEditText edtClinicEmail;

    private FirebaseAuthUtils firebaseAuthUtils;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private CollectionReference vetRegsRef;
    private StorageReference storageRef;
    private StorageReference vetRegLogoRef;

    // Private variables
    private boolean isLogoUploaded;

    // Public constants
    public static final String KEY_VET_REG_ID = "veterinaryClinicRegistrationId";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase service instances
        firebaseAuthUtils = new FirebaseAuthUtils();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        // Initialize Firestore references
        vetRegsRef = db.collection("veterinaryClinicRegistrations");
        // Initialize Storage references
        storageRef = storage.getReference();

        // Get ViewBinding and set content view
        registerClinicBinding = ActivityRegisterClinicBinding.inflate(getLayoutInflater());
        setContentView(registerClinicBinding.getRoot());

        // Enable edge-to-edge layout
        EdgeToEdge.enable(this);
        // Adjust main layout padding based on system bar size to prevent obstruction
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //region UI Setups
        isLogoUploaded = false;

        // Create photo picker
        ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
                registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                    if (uri != null) {
                        isLogoUploaded = true;
                        loadLogo(uri.toString());
                    } else {
                        Log.d(TAG, "PhotoPicker - No media selected");
                    }
                });

        // Cancellation dialog
        MaterialAlertDialogBuilder cancelDialog =
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.cancel)
                        .setMessage(R.string.cancel_register_clinic_message)
                        .setNegativeButton(getString(R.string.no), (dialogInterface, i) -> {
                            // Unused
                        })
                        .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                            // Return to previous screen
                            finish();
                        });
        //endregion

        //region onClick Listeners
        // Logo action butotns
        registerClinicBinding.btnClearImage.setOnClickListener(view -> {
            // Clear logo
            isLogoUploaded = false;
            loadLogo(null);
        });
        registerClinicBinding.btnUploadImage.setOnClickListener(view -> {
            // Launch photo picker
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        // btnRegister: create registration
        registerClinicBinding.btnRegister.setOnClickListener(view -> {
            // Get inputs
            byte[] logoData = null;
            if (isLogoUploaded) {
                registerClinicBinding.imgClinicLogo.setDrawingCacheEnabled(true);
                registerClinicBinding.imgClinicLogo.buildDrawingCache();
                Bitmap bitmap = ((BitmapDrawable) registerClinicBinding.imgClinicLogo.getDrawable()).getBitmap();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                logoData = baos.toByteArray();
            }

            String name = registerClinicBinding.edtName.getText().toString();
            String email = registerClinicBinding.edtEmail.getText().toString();
            String phoneNumber = registerClinicBinding.edtPhoneNumber.getText().toString();
            String address = registerClinicBinding.edtAddress.getText().toString();
            String description = registerClinicBinding.edtDescription.getText().toString();

            // Validate inputs
            boolean isValid = validateInputs(logoData, name, email, phoneNumber, address, description);

            // Check if email is already registered
            if (isValid) {
                checkEmailDuplicate(logoData, name, email, phoneNumber, address, description);
            }
        });

        // btnCancelRegisterClinic: return to previous screen
        registerClinicBinding.btnCancelRegisterClinic.setOnClickListener(view -> {
            // Get inputs
            String name = registerClinicBinding.edtName.getText().toString();
            String email = registerClinicBinding.edtEmail.getText().toString();
            String phoneNumber = registerClinicBinding.edtPhoneNumber.getText().toString();
            String address = registerClinicBinding.edtAddress.getText().toString();
            String description = registerClinicBinding.edtDescription.getText().toString();

            // Prompt confirmation via cancellation dialog if any input not empty
            if (isLogoUploaded || !name.isBlank() || !email.isBlank() || !phoneNumber.isBlank() || !address.isBlank() || !description.isBlank()) {
                cancelDialog.show();
            } else {
                finish();
            }
        });

        // txttbnCheckClinicRegStatus: redirect to registration status page
        registerClinicBinding.txtbtnCheckClinicRegStatus.setOnClickListener(view -> {
            // Email dialog for checking registration status
            View emailDialogLayout = LayoutInflater.from(this).inflate(R.layout.dialog_get_clinic_email, null);
            txtfieldClinicEmail = emailDialogLayout.findViewById(R.id.txtfield_clinic_email);
            edtClinicEmail = emailDialogLayout.findViewById(R.id.edt_clinic_email);

            MaterialAlertDialogBuilder emailDialogBuilder =
                    new MaterialAlertDialogBuilder(this)
                            .setView(emailDialogLayout)
                            .setTitle(getString(R.string.see_registration_status))
                            .setMessage(R.string.clinic_email_dialog_message)
                            .setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
                                // Do nothing
                            })
                            .setPositiveButton(R.string.confirm, (dialogInterface, i) -> {
                                // Validate email
                                String email = edtClinicEmail.getText().toString();
                                if (email.isBlank()) {
                                    edtClinicEmail.setError(getString(R.string.email_required_error));
                                } else {
                                    // Check email
                                    checkEmailExistence(email);
                                }
                            });

            AlertDialog emailDialog = emailDialogBuilder.create();
            emailDialog.show();

            // Override the positive button's click behavior
            emailDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                // Validate email
                String email = edtClinicEmail.getText().toString();
                if (email.isBlank()) {
                    txtfieldClinicEmail.setError(getString(R.string.email_required_error));
                } else {
                    // Check email
                    checkEmailExistence(email);
                    emailDialog.dismiss();
                }
            });

            // Reset error when text is changed
            edtClinicEmail.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    txtfieldClinicEmail.setErrorEnabled(false);
                }

                @Override
                public void afterTextChanged(Editable editable) {}
            });
        });
        //endregion

        //region Other Listeners
        registerClinicBinding.edtName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Reset error
                registerClinicBinding.txtfieldName.setErrorEnabled(false);

                // Update name in banner
                String name = registerClinicBinding.edtName.getText().toString();
                if (name.isBlank()) {
                    registerClinicBinding.txtBannerName.setText(R.string.name);
                } else {
                    registerClinicBinding.txtBannerName.setText(name);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
        registerClinicBinding.edtEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                registerClinicBinding.txtfieldEmail.setErrorEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
        registerClinicBinding.edtPhoneNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                registerClinicBinding.txtfieldPhoneNumber.setErrorEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
        registerClinicBinding.edtAddress.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                registerClinicBinding.txtfieldAddress.setErrorEnabled(false);

                // Update address in banner
                String address = registerClinicBinding.edtAddress.getText().toString();
                if (address.isBlank()) {
                    registerClinicBinding.txtBannerAddress.setText(R.string.no_address);
                } else {
                    registerClinicBinding.txtBannerAddress.setText(address);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
        registerClinicBinding.edtDescription.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                registerClinicBinding.txtfieldDescription.setErrorEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
        //endregion
    }

    private void loadLogo(String imageUrl) {
        if (imageUrl == null) {
            // Load default icon if no logo uploaded
            Glide.with(getApplicationContext())
                    .load(R.drawable.baseline_local_hospital_24)
                    .into(registerClinicBinding.imgClinicLogo);
        } else {
            // Load uploaded logo
            Glide.with(getApplicationContext())
                    .load(imageUrl)
                    .into(registerClinicBinding.imgClinicLogo);
        }
    }

    private boolean validateInputs(byte[] logoData, String name, String email, String phoneNumber, String address, String description) {
        // Reset errors
        registerClinicBinding.txtfieldName.setErrorEnabled(false);
        registerClinicBinding.txtfieldEmail.setErrorEnabled(false);
        registerClinicBinding.txtfieldPhoneNumber.setErrorEnabled(false);
        registerClinicBinding.txtfieldAddress.setErrorEnabled(false);
        registerClinicBinding.txtfieldDescription.setErrorEnabled(false);

        // Validate logo
        if (logoData == null) {
            Toast.makeText(this, "Logo is required.", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Validate name
        // Empty input
        if (name.isBlank()) {
            registerClinicBinding.txtfieldName.setError(getString(R.string.clinic_name_required_error));
            registerClinicBinding.txtfieldName.requestFocus();
            return false;
        }

        // Validate email
        // Empty input
        if (email.isBlank()) {
            registerClinicBinding.txtfieldEmail.setError(getString(R.string.email_required_error));
            registerClinicBinding.txtfieldEmail.requestFocus();
            return false;
        }
        // Email format
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            registerClinicBinding.txtfieldEmail.setError(getString(R.string.email_format_invalid));
            registerClinicBinding.txtfieldEmail.requestFocus();
            return false;
        }

        // Validate phoneNumber
        // Empty input
        if (phoneNumber.isBlank()) {
            registerClinicBinding.txtfieldPhoneNumber.setError(getString(R.string.phone_number_required_error));
            registerClinicBinding.txtfieldPhoneNumber.requestFocus();
            return false;
        }
        // Phone number format
        if (!Patterns.PHONE.matcher(phoneNumber).matches()) {
            registerClinicBinding.txtfieldPhoneNumber.setError(getString(R.string.phone_number_format_invalid));
            registerClinicBinding.txtfieldPhoneNumber.requestFocus();
            return false;
        }

        // Validate address
        // Empty input
        if (address.isBlank()) {
            registerClinicBinding.txtfieldAddress.setError(getString(R.string.address_required_error));
            registerClinicBinding.txtfieldAddress.requestFocus();
            return false;
        }

        // Validate description
        // Empty input
        if (description.isBlank()) {
            registerClinicBinding.txtfieldDescription.setError(getString(R.string.description_required_error));
            registerClinicBinding.txtfieldDescription.requestFocus();
            return false;
        }

        return true;
    }

    private void checkEmailDuplicate(byte[] logoData, String name, String email, String phoneNumber, String address, String description) {
        vetRegsRef.whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Check if email exists in any registrations
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "Unable to register. There is already a clinic registered with this email.", Toast.LENGTH_LONG).show();
                    } else {
                        createVeterinaryClinicRegistration(logoData, name, email, phoneNumber, address, description);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "checkEmailExistence: Failed to check email duplicate", e);
                });
    }

    private void createVeterinaryClinicRegistration(byte[] logoData, String name, String email, String phoneNumber, String address, String description) {
        VeterinaryClinicRegistration newRegistration = new VeterinaryClinicRegistration(
                name,
                description,
                address,
                email,
                phoneNumber,
                null,
                VeterinaryClinicRegistrationStatus.PENDING.getTitle(),
                null,
                Timestamp.now(),
                Timestamp.now()
        );

        // Add VeterinaryClinicRegistration document
        vetRegsRef.add(newRegistration)
                .addOnSuccessListener(documentReference -> {
                    // Upload logo to storage
                    vetRegLogoRef = storageRef.child("images/veterinaryClinicRegistrations/" + documentReference.getId() + "/logo.jpg");
                    vetRegLogoRef.putBytes(logoData)
                            .continueWithTask(imageUploadTask -> {
                                if (!imageUploadTask.isSuccessful()) {
                                    throw imageUploadTask.getException();
                                }
                                // Continue with the task to get the download URL
                                return vetRegLogoRef.getDownloadUrl();
                            })
                            .addOnCompleteListener(imageUploadTask -> {
                                Uri logoUrl = imageUploadTask.getResult();

                                // Update VeterinaryClinicRegistration's logoUrl
                                vetRegsRef.document(documentReference.getId())
                                        .update("logoUrl", logoUrl)
                                        .addOnCompleteListener(imageUpdateTask -> {
                                            // Clear inputs
                                            isLogoUploaded = false;
                                            Glide.with(this)
                                                    .load(R.drawable.baseline_local_hospital_24)
                                                    .into(registerClinicBinding.imgClinicLogo);
                                            registerClinicBinding.edtName.getText().clear();
                                            registerClinicBinding.edtEmail.getText().clear();
                                            registerClinicBinding.edtPhoneNumber.getText().clear();
                                            registerClinicBinding.edtAddress.getText().clear();
                                            registerClinicBinding.edtDescription.getText().clear();

                                            // Registration success, redirect to success screen
                                            Intent intent = new Intent(this, RegisterClinicSuccessActivity.class);
                                            intent.putExtra(KEY_VET_REG_ID, documentReference.getId()); // Pass registration ID to be used for status checking
                                            Toast.makeText(this, "Successfully registered.", Toast.LENGTH_SHORT).show();
                                            startActivity(intent);
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.w(TAG, "createVeterinaryClinicRegistration: Failed to update VeterinaryClinicRegistration logoUrl", e);
                                        });
                            });
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "createVeterinaryClinicRegistration: Error adding VeterinaryClinicRegistration document", e);
                    Toast.makeText(this, "Unable to register, please try again later.", Toast.LENGTH_SHORT).show();
                });
    }

    private void checkEmailExistence(String email) {
        vetRegsRef.whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Check if email exists
                    if (queryDocumentSnapshots.isEmpty()) {
                        // No registration with this email
                        Toast.makeText(this, getString(R.string.no_vet_clinic_email_error), Toast.LENGTH_SHORT).show();
                    } else {
                        // Email exists, redirect to ClinicRegistrationStatusActivity
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        Intent intent = new Intent(this, ClinicRegistrationStatusActivity.class);
                        intent.putExtra(KEY_VET_REG_ID, document.getId());  // Pass registration ID to be used for status checking
                        startActivity(intent);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "checkEmailExistence: Failed to check email existence", e);
                });
    }
}
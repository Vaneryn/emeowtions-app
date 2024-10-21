package com.example.emeowtions.activities.veterinary;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.emeowtions.R;
import com.example.emeowtions.adapters.UserAdapter;
import com.example.emeowtions.databinding.ActivityEditStaffBinding;
import com.example.emeowtions.enums.Role;
import com.example.emeowtions.models.User;
import com.example.emeowtions.models.Veterinarian;
import com.example.emeowtions.models.VeterinaryStaff;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EditStaffActivity extends AppCompatActivity {

    private static final String TAG = "EditStaffActivity";
    private SharedPreferences sharedPreferences;
    private static final String VET_ROLE = Role.VETERINARIAN.getTitle();
    private static final String VET_STAFF_ROLE = Role.VETERINARY_STAFF.getTitle();

    // Firebase variables
    private FirebaseAuthUtils firebaseAuthUtils;
    private FirebaseFirestore db;
    private CollectionReference usersRef;
    private CollectionReference vetsRef;
    private CollectionReference vetStaffRef;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private StorageReference pfpRef;

    // Layout variables
    private ActivityEditStaffBinding binding;

    // Private variables
    private String originalPfpUrl;
    private String originalDisplayName;
    private String originalGender;
    private Timestamp originalDob;
    private String originalRole;
    private String originalJobTitle;
    private String originalQualification;
    private String originalEmail;

    private String uid;
    private String veterinaryClinicId;
    private String selectedUserRole;
    private Date selectedDob;
    private boolean isPfpChanged;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // Shared preferences
        sharedPreferences = getSharedPreferences("com.emeowtions", Context.MODE_PRIVATE);
        veterinaryClinicId = sharedPreferences.getString("veterinaryClinicId", null);

        // Get intent data
        Intent passedIntent = getIntent();
        uid = passedIntent.getStringExtra(UserAdapter.KEY_UID);
        selectedUserRole = passedIntent.getStringExtra(UserAdapter.KEY_ROLE);

        // Initialize Firebase service instances
        firebaseAuthUtils = new FirebaseAuthUtils();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        // Initialize Firestore references
        usersRef = db.collection("users");
        vetsRef = db.collection("veterinarians");
        vetStaffRef = db.collection("veterinaryStaff");
        // Initialize Storage references
        storageRef = storage.getReference();
        pfpRef = storageRef.child("images/users/" + uid + "/profile_picture.jpg");

        // Get ViewBinding and set content view
        binding = ActivityEditStaffBinding.inflate(getLayoutInflater());
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
        // Delete dialog
        MaterialAlertDialogBuilder deleteDialog =
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.delete)
                        .setMessage("Delete this staff? This action is irreversible!")
                        .setNegativeButton(R.string.no, (dialogInterface, i) -> {
                            // Do nothing
                        })
                        .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                            deleteStaff();
                        });

        // DatePicker dialog
        selectedDob = null;
        MaterialDatePicker<Long> datePicker =
                MaterialDatePicker.Builder.datePicker()
                        .setTitleText("Select date")
                        .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                        .build();

        // Photo picker
        ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
                registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                    if (uri != null) {
                        Glide.with(getApplicationContext())
                                .load(uri)
                                .into(binding.imgPfp);
                        isPfpChanged = true;
                    } else {
                        Log.d(TAG, "PhotoPicker - No media selected");
                    }
                });

        // Cancel dialog
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
        binding.appBarEditstaff.setNavigationOnClickListener(view -> finish());

        binding.appBarEditstaff.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.action_edit_staff) {
                toggleEditMode(true);
            } else if (itemId == R.id.action_delete_staff) {
                deleteDialog.show();
            }

            return false;
        });
        //endregion

        //region Load Data
        isPfpChanged = false;
        loadUser();
        //endregion

        //region onClick Listeners
        // pfp controls
        binding.btnRevert.setOnClickListener(view -> {
            isPfpChanged = false;
            loadPfp(originalPfpUrl);
        });
        binding.btnUpload.setOnClickListener(view -> {
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        // datePicker controls
        datePicker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy", Locale.getDefault());

            selectedDob = new Date(selection);
            binding.edtDob.setText(sdf.format(selectedDob));
            binding.txtfieldDob.setEndIconVisible(true);
        });
        binding.edtDob.setOnClickListener(view -> {
            // Prevent app crash from double DatePicker opening
            if (!datePicker.isAdded()) {
                datePicker.show(getSupportFragmentManager(), TAG);
            }
        });
        binding.edtDob.setOnLongClickListener(view -> true);
        binding.txtfieldDob.setEndIconOnClickListener(view -> {
            selectedDob = null;
            binding.edtDob.getText().clear();
            binding.txtfieldDob.setEndIconVisible(false);
        });

        // Cancel button
        binding.btnCancel.setOnClickListener(view -> {
            // Get inputs
            String displayName = binding.edtDisplayName.getText().toString();
            String gender = binding.edmGender.getText().toString();
            Timestamp dob = selectedDob == null ? null : new Timestamp(selectedDob);
            String role = binding.edmRole.getText().toString();
            String jobTitle = binding.edtJobtitle.getText().toString();
            String qualification = binding.edtQualification.getText().toString();

            // Check for changes
            if (isPfpChanged || !displayName.equals(originalDisplayName) || !gender.equals(originalGender) ||
                    (dob != null && !dob.equals(originalDob)) || !role.equals(originalRole) || !jobTitle.equals(originalJobTitle) ||
                    (originalQualification != null && !qualification.equals(originalQualification))) {
                cancelDialog.show();
            } else {
                discardChanges();
            }
        });

        // Confirm button
        binding.btnConfirm.setOnClickListener(view -> {
            // Get inputs
            byte[] pfpData = null;
            if (isPfpChanged) {
                binding.imgPfp.setDrawingCacheEnabled(true);
                binding.imgPfp.buildDrawingCache();
                Bitmap bitmap = ((BitmapDrawable) binding.imgPfp.getDrawable()).getBitmap();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                pfpData = baos.toByteArray();
            }

            String displayName = binding.edtDisplayName.getText().toString();
            String gender = binding.edmGender.getText().toString();
            Timestamp dob = selectedDob == null ? null : new Timestamp(selectedDob);
            String role = binding.edmRole.getText().toString();
            String jobTitle = binding.edtJobtitle.getText().toString();
            String qualification = binding.edtQualification.getText().toString();

            // Validate inputs
            boolean isValid = validateInputs(role, jobTitle, qualification);

            // Update user if inputs are valid
            if (isValid) {
                updatePfp(pfpData, displayName, gender, dob, role, jobTitle, qualification);
            }
        });
        //endregion

        //region Other Listeners
        bindTextChangeListeners();
        //endregion
    }

    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuthUtils.checkSignedIn(this);
    }

    // Loads user data
    private void loadUser() {
        usersRef.document(uid)
                .get()
                .addOnCompleteListener(task -> {
                    // Error
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "loadUser: Failed to retrieve User data", task.getException());
                        return;
                    }
                    // Success
                    if (task.isSuccessful()) {
                        User user = task.getResult().toObject(User.class);
                        SimpleDateFormat sdf_date = new SimpleDateFormat("d MMM yyyy", Locale.getDefault());
                        SimpleDateFormat sdf_datetime = new SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault());

                        // Switch view mode based on selected user's role
                        switchViewMode(selectedUserRole);
                        toggleEditMode(false);

                        // Get veterinarian or veterinaryStaff data
                        if (selectedUserRole.equals(VET_ROLE)) {
                            vetsRef.whereEqualTo("uid", uid)
                                    .get()
                                    .addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()) {
                                            Veterinarian vet = task1.getResult().getDocuments().get(0).toObject(Veterinarian.class);
                                            // Save original field values
                                            originalJobTitle = vet.getJobTitle();
                                            originalQualification = vet.getQualification();
                                            // Load fields
                                            binding.edtJobtitle.setText(originalJobTitle);
                                            binding.edtQualification.setText(originalQualification);
                                        } else {
                                            Log.w(TAG, "loadUser: Failed to retrieve Veterinarian data", task.getException());
                                        }
                                    });
                        } else if (selectedUserRole.equals(VET_STAFF_ROLE)) {
                            vetStaffRef.whereEqualTo("uid", uid)
                                    .get()
                                    .addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()) {
                                            VeterinaryStaff vetStaff = task1.getResult().getDocuments().get(0).toObject(VeterinaryStaff.class);
                                            // Save original field values
                                            originalJobTitle = vetStaff.getJobTitle();
                                            originalQualification = null;
                                            // Load fields
                                            binding.edtJobtitle.setText(originalJobTitle);
                                        } else {
                                            Log.w(TAG, "loadUser: Failed to retrieve VeterinaryStaff data", task.getException());
                                        }
                                    });
                        }

                        // Save original field values
                        originalPfpUrl = user.getProfilePicture();
                        originalDisplayName = user.getDisplayName();
                        originalGender = user.getGender();
                        originalDob = user.getDateOfBirth();
                        selectedDob = originalDob == null ? null : originalDob.toDate();
                        originalRole = user.getRole();
                        originalEmail = user.getEmail();

                        // Load banner
                        loadPfp(originalPfpUrl);
                        binding.txtBannerDisplayName.setText(originalDisplayName);
                        binding.txtBannerCreated.setText(String.format("Created %s", sdf_datetime.format(user.getCreatedAt().toDate())));
                        binding.txtBannerUpdated.setText(String.format("Updated %s", sdf_datetime.format(user.getUpdatedAt().toDate())));

                        // Load fields
                        binding.edtDisplayName.setText(originalDisplayName);
                        binding.edmGender.setText(originalGender, false);
                        binding.edtDob.setText(
                                originalDob == null
                                        ? "Not set"
                                        : String.format("%s", sdf_date.format(originalDob.toDate()))
                        );
                        binding.edmRole.setText(originalRole, false);
                        binding.edtEmail.setText(originalEmail);
                    }
                });
    }

    // Determines whether pfp needs to be updated in storage
    private void updatePfp(byte[] pfpData, String displayName, String gender, Timestamp dob, String role, String jobTitle, String qualification) {
        if (isPfpChanged) {
            // Upload image to storage
            pfpRef.putBytes(pfpData)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }
                        return pfpRef.getDownloadUrl();
                    })
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Uri pfpUrl = task.getResult();
                            updateUser(pfpUrl.toString(), displayName, gender, dob, role, jobTitle, qualification);
                        }
                    });
        } else {
            updateUser(originalPfpUrl, displayName, gender, dob, role, jobTitle, qualification);
        }
    }

    // Update Firestore user document
    private void updateUser(String pfpUrl, String displayName, String gender, Timestamp dob, String role, String jobTitle, String qualification) {
        // Store copy of original role
        String initialRole = originalRole;

        // Update Firestore User first
        usersRef.document(uid)
                .update(
                        "profilePicture", pfpUrl,
                        "displayName", displayName,
                        "gender", gender,
                        "dateOfBirth", dob,
                        "role", role,
                        "verified", true,
                        "updatedAt", Timestamp.now()
                )
                .addOnSuccessListener(unused -> {
                    processRoleUpdate(initialRole, role, jobTitle, qualification);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "updateUser: Failed to update User", e);
                });
    }

    // Handles role changes
    private void processRoleUpdate(String initialRole, String newRole, String jobTitle, String qualification) {
        // Check updated role
        // REMAIN ROLE
        if (newRole.equals(initialRole)) {
            updateStaff(initialRole, jobTitle, qualification);
        }
        // VET / VET_STAFF -> INTERCHANGE
        else {
            updateStaffInterchange(initialRole, newRole, jobTitle, qualification);
        }
    }

    // Update Firestore veterinarian or veterinaryStaff document
    private void updateStaff(String role, String jobTitle, String qualification) {
        // VET
        if (role.equals(VET_ROLE)) {
            vetsRef.whereEqualTo("uid", uid)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            vetsRef.document(task.getResult().getDocuments().get(0).getId())
                                    .update("jobTitle", jobTitle, "qualification", qualification, "updatedAt", Timestamp.now())
                                    .addOnSuccessListener(unused1 -> {
                                        Toast.makeText(this, String.format("Successfully updated %s.", role), Toast.LENGTH_SHORT).show();
                                        reloadActivity(role);
                                    })
                                    .addOnFailureListener(e -> Log.w(TAG, "updateStaff: Failed to update Veterinarian", e));
                        }
                    });
        }
        // VET_STAFF
        else if (role.equals(VET_STAFF_ROLE)) {
            vetStaffRef.whereEqualTo("uid", uid)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            vetStaffRef.document(task.getResult().getDocuments().get(0).getId())
                                    .update("jobTitle", jobTitle, "updatedAt", Timestamp.now())
                                    .addOnSuccessListener(unused1 -> {
                                        Toast.makeText(this, String.format("Successfully updated %s.", role), Toast.LENGTH_SHORT).show();
                                        reloadActivity(role);
                                    })
                                    .addOnFailureListener(e -> Log.w(TAG, "updateStaff: Failed to update VeterinaryStaff", e));
                        }
                    });
        }
    }

    private void updateStaffInterchange(String initialRole, String newRole, String jobTitle, String qualification) {
        // VET -> VET_STAFF
        if (initialRole.equals(VET_ROLE) && newRole.equals(VET_STAFF_ROLE)) {
            // Full delete then create new document
            vetsRef.whereEqualTo("uid", uid)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Delete
                            Veterinarian vet = task.getResult().getDocuments().get(0).toObject(Veterinarian.class);

                            vetsRef.document(task.getResult().getDocuments().get(0).getId())
                                    .delete()
                                    .addOnSuccessListener(unused1 -> {
                                        // Create new VeterinaryStaff
                                        VeterinaryStaff newVetStaff = new VeterinaryStaff(
                                                vet.getUid(),
                                                vet.getVeterinaryClinicId(),
                                                jobTitle,
                                                Timestamp.now(),
                                                Timestamp.now(),
                                                false
                                        );

                                        vetStaffRef.add(newVetStaff)
                                                .addOnCompleteListener(task1 -> {
                                                    if (task.isSuccessful()) {
                                                        Toast.makeText(this, String.format("Successfully updated %s to %s.", initialRole, newRole), Toast.LENGTH_SHORT).show();
                                                        reloadActivity(newRole);
                                                    } else {
                                                        Log.w(TAG, "updateVetInterchange: Failed to update Veterinarian to VeterinaryStaff", task.getException());
                                                    }
                                                });
                                    })
                                    .addOnFailureListener(e -> Log.w(TAG, "updateVetInterchange: Failed to update Veterinarian to Veterinary Staff", e));
                        }
                    });
        }
        // VET_STAFF -> VET
        else if (initialRole.equals(VET_STAFF_ROLE) && newRole.equals(VET_ROLE)) {
            // Full delete then create new document
            vetStaffRef.whereEqualTo("uid", uid)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Delete
                            VeterinaryStaff vetStaff = task.getResult().getDocuments().get(0).toObject(VeterinaryStaff.class);

                            vetStaffRef.document(task.getResult().getDocuments().get(0).getId())
                                    .delete()
                                    .addOnSuccessListener(unused1 -> {
                                        // Create new Veterinarian
                                        Veterinarian newVet = new Veterinarian(
                                                vetStaff.getUid(),
                                                vetStaff.getVeterinaryClinicId(),
                                                jobTitle,
                                                qualification,
                                                Timestamp.now(),
                                                Timestamp.now(),
                                                false
                                        );

                                        vetsRef.add(newVet)
                                                .addOnCompleteListener(task1 -> {
                                                    if (task.isSuccessful()) {
                                                        Toast.makeText(this, String.format("Successfully updated %s to %s.", initialRole, newRole), Toast.LENGTH_SHORT).show();
                                                        reloadActivity(newRole);
                                                    } else {
                                                        Log.w(TAG, "updateVetInterchange: Failed to update VeterinaryStaff to Veterinarian", task.getException());
                                                    }
                                                });
                                    })
                                    .addOnFailureListener(e -> Log.w(TAG, "updateVetInterchange: Failed to update VeterinaryStaff to Veterinarian", e));
                        }
                    });
        }
    }

    // Soft deletes staff along with linked user account
    private void deleteStaff() {
        usersRef.document(uid)
                .update("deleted", true, "updatedAt", Timestamp.now())
                .addOnSuccessListener(unused -> {
                    if (selectedUserRole.equals(VET_ROLE)) {
                        // Delete veterinarian
                        vetsRef.whereEqualTo("uid", uid)
                                .get()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        vetsRef.document(task.getResult().getDocuments().get(0).getId())
                                                .update("deleted", true, "updatedAt", Timestamp.now())
                                                .addOnSuccessListener(unused1 -> {
                                                    Toast.makeText(this, String.format("Successfully deleted %s", originalRole), Toast.LENGTH_SHORT).show();
                                                    finish();
                                                })
                                                .addOnFailureListener(e -> Log.w(TAG, "deleteUser: Failed to delete Veterinarian", e));
                                    }
                                });
                    } else if (selectedUserRole.equals(VET_STAFF_ROLE)) {
                        // Delete veterinaryStaff
                        vetStaffRef.whereEqualTo("uid", uid)
                                .get()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        vetStaffRef.document(task.getResult().getDocuments().get(0).getId())
                                                .update("deleted", true, "updatedAt", Timestamp.now())
                                                .addOnSuccessListener(unused1 -> {
                                                    Toast.makeText(this, String.format("Successfully deleted %s", originalRole), Toast.LENGTH_SHORT).show();
                                                    finish();
                                                })
                                                .addOnFailureListener(e -> Log.w(TAG, "deleteUser: Failed to delete VeterinaryStaff", e));
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "deleteStaff: Failed to delete User", e);
                });
    }

    // Validates inputs
    private boolean validateInputs(String role, String jobTitle, String qualification) {
        // Validate job title
        // Empty input
        if (jobTitle.isBlank()) {
            binding.txtfieldJobtitle.setError(getString(R.string.job_title_required_error));
            binding.txtfieldJobtitle.requestFocus();
            return false;
        }

        // Validate qualification (Veterinarian)
        // Empty input
        if (role.equals(VET_ROLE)) {
            if (qualification.isBlank()) {
                binding.txtfieldQualification.setError(getString(R.string.qualification_required_error));
                binding.txtfieldQualification.requestFocus();
                return false;
            }
        }

        return true;
    }

    // Switches between view mode for Veterinarian profile or VeterinaryStaff profile
    private void switchViewMode(String selectedUserRole) {
        // Show "Qualification" field for VET
        if (selectedUserRole.equals(VET_ROLE)) {
            binding.txtfieldQualification.setVisibility(View.VISIBLE);
        } else {
            binding.txtfieldQualification.setVisibility(View.GONE);
        }
    }

    // Toggles edit mode on and off
    private void toggleEditMode(boolean enabled) {
        int textColor;

        binding.appBarEditstaff.getMenu().clear();

        // Check mode
        if (enabled) {
            textColor = ContextCompat.getColor(this, R.color.black);

            // Controls
            binding.btnRevert.setVisibility(View.VISIBLE);
            binding.btnUpload.setVisibility(View.VISIBLE);
            binding.txtCredentialTip.setVisibility(View.VISIBLE);
            binding.btnSendPasswordReset.setVisibility(View.VISIBLE);
            binding.layoutEditButtons.setVisibility(View.VISIBLE);

            // datePicker
            binding.txtfieldDob.setEndIconVisible(originalDob != null);
        } else {
            textColor = ContextCompat.getColor(this, R.color.gray_400);

            // Controls
            binding.appBarEditstaff.inflateMenu(R.menu.top_app_bar_editstaff);
            binding.btnRevert.setVisibility(View.GONE);
            binding.btnUpload.setVisibility(View.GONE);
            binding.txtCredentialTip.setVisibility(View.GONE);
            binding.btnSendPasswordReset.setVisibility(View.GONE);
            binding.layoutEditButtons.setVisibility(View.GONE);

            // Hide delete button if it is your own profile
            if (uid.equals(firebaseAuthUtils.getUid())) {
                binding.appBarEditstaff.getMenu().removeItem(R.id.action_delete_staff);
            }

            // dateOfBirth
            binding.txtfieldDob.setEndIconVisible(false);

            // Clear errors
            binding.txtfieldRole.setErrorEnabled(false);
            binding.txtfieldJobtitle.setErrorEnabled(false);
            binding.txtfieldQualification.setErrorEnabled(false);
        }

        // Toggle fields
        binding.txtfieldDisplayName.setEnabled(enabled);
        binding.txtfieldGender.setEnabled(enabled);
        binding.txtfieldDob.setEnabled(enabled);
        binding.txtfieldRole.setEnabled(enabled);
        binding.txtfieldJobtitle.setEnabled(enabled);
        binding.txtfieldQualification.setEnabled(enabled);

        // Text counters
        binding.txtfieldDisplayName.setCounterEnabled(enabled);
        binding.txtfieldJobtitle.setCounterEnabled(enabled);
        binding.txtfieldQualification.setCounterEnabled(enabled);

        // End icons
        binding.txtfieldDob.setEndIconVisible(enabled);

        // Text colors
        binding.edtDisplayName.setTextColor(textColor);
        binding.edmGender.setTextColor(textColor);
        binding.edtDob.setTextColor(textColor);
        binding.edmRole.setTextColor(textColor);
        binding.edtJobtitle.setTextColor(textColor);
        binding.edtQualification.setTextColor(textColor);
    }

    // Discards any input changes
    private void discardChanges() {
        SimpleDateFormat sdf_date = new SimpleDateFormat("d MMM yyyy", Locale.getDefault());

        toggleEditMode(false);

        // Reset fields to original values
        selectedUserRole = originalRole;
        isPfpChanged = false;
        loadPfp(originalPfpUrl);
        binding.edtDisplayName.setText(originalDisplayName);
        binding.edmGender.setText(originalGender, false);
        binding.edtDob.setText(
                originalDob == null
                        ? "Not set"
                        : sdf_date.format(originalDob.toDate())
        );
        binding.edmRole.setText(originalRole, false);
        binding.edtJobtitle.setText(originalJobTitle);
        binding.edtQualification.setText(originalQualification);
        binding.edtEmail.setText(originalEmail);
    }

    // Binds TextWatchers to input fields
    private void bindTextChangeListeners() {
        // Role
        binding.edmRole.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                selectedUserRole = binding.edmRole.getText().toString();
                switchViewMode(selectedUserRole);

                // Hide qualification field if select VeterinaryStaff
                if (selectedUserRole.equals(VET_STAFF_ROLE)) {
                    binding.txtfieldQualification.setVisibility(View.GONE);
                } else {
                    binding.txtfieldQualification.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void afterTextChanged(Editable editable) {}
        });

        binding.edtJobtitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                binding.txtfieldJobtitle.setErrorEnabled(false);
            }
            @Override
            public void afterTextChanged(Editable editable) {}
        });

        binding.edtQualification.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                binding.txtfieldQualification.setErrorEnabled(false);
            }
            @Override
            public void afterTextChanged(Editable editable) {}
        });
    }

    // Loads profile picture image
    private void loadPfp(String pfpUrl) {
        if (pfpUrl == null) {
            // Load default picture if no profile picture exists
            Glide.with(getApplicationContext())
                    .load(R.drawable.baseline_person_24)
                    .into(binding.imgPfp);
        } else {
            // Load original profile picture
            Glide.with(getApplicationContext())
                    .load(pfpUrl)
                    .into(binding.imgPfp);
        }
    }

    // Reloads the activity
    private void reloadActivity(String role) {
        Intent intent = new Intent(this, EditStaffActivity.class);
        intent.putExtra(UserAdapter.KEY_UID, uid);
        intent.putExtra(UserAdapter.KEY_ROLE, role);

        finish();
        startActivity(intent);
    }
}
package com.example.emeowtions.activities.admin;

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
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.emeowtions.R;
import com.example.emeowtions.adapters.UserAdapter;
import com.example.emeowtions.databinding.ActivityEditUserBinding;
import com.example.emeowtions.enums.Role;
import com.example.emeowtions.models.User;
import com.example.emeowtions.models.Veterinarian;
import com.example.emeowtions.models.VeterinaryClinic;
import com.example.emeowtions.models.VeterinaryStaff;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.FirebaseApp;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EditUserActivity extends AppCompatActivity {

    private static final String TAG = "EditUserActivity";
    private SharedPreferences sharedPreferences;
    private static final String USER_ROLE = Role.USER.getTitle();
    private static final String VET_ROLE = Role.VETERINARIAN.getTitle();
    private static final String VET_STAFF_ROLE = Role.VETERINARY_STAFF.getTitle();
    private static final String ADMIN_ROLE = Role.ADMIN.getTitle();

    // Firebase variables
    private FirebaseAuthUtils firebaseAuthUtils;
    private FirebaseFirestore db;
    private CollectionReference usersRef;
    private CollectionReference vetsRef;
    private CollectionReference vetStaffRef;
    private CollectionReference clinicsRef;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private StorageReference pfpRef;

    // Layout variables
    private ActivityEditUserBinding binding;

    // Private variables
    private String originalPfpUrl;
    private String originalEmail;
    private String originalRole;
    private String originalClinicId;
    private String originalDisplayName;
    private String originalGender;
    private Timestamp originalDob;

    private String uid;
    private String selectedUserRole;
    private String currentUserRole;
    private List<String> clinicIdList;
    private List<String> clinicNameList;
    private int selectedClinicIndex;
    private Date selectedDob;
    private boolean isPfpChanged;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Top level
        sharedPreferences = getSharedPreferences("com.emeowtions", Context.MODE_PRIVATE);
        currentUserRole = sharedPreferences.getString("role", "Admin");

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
        clinicsRef = db.collection("veterinaryClinics");
        // Initialize Storage references
        storageRef = storage.getReference();
        pfpRef = storageRef.child("images/users/" + uid + "/profile_picture.jpg");

        // Get ViewBinding and set content view
        binding = ActivityEditUserBinding.inflate(getLayoutInflater());
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
                        .setMessage("Delete this user? This action is irreversible!")
                        .setNegativeButton(R.string.no, (dialogInterface, i) -> {
                            // Do nothing
                        })
                        .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                            deleteUser();
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
        binding.appBarEdituser.setNavigationOnClickListener(view -> finish());

        binding.appBarEdituser.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.action_edit_user) {
                toggleEditMode(true);
            } else if (itemId == R.id.action_delete_user) {
                deleteDialog.show();
            }

            return false;
        });
        //endregion

        //region Load Data
        isPfpChanged = false;

        // Load Role options
        if (currentUserRole.equals(Role.SUPER_ADMIN.getTitle())) {
            // Only Super Admin can select Admins
            String[] roleList = getResources().getStringArray(R.array.role_items_super_admin);
            ArrayAdapter<String> rolesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, roleList);
            binding.edmRole.setAdapter(rolesAdapter);
            binding.edmRole.setText(roleList[0], false);
        }

        // Load clinic names into edmClinic
        clinicsRef.whereEqualTo("deleted", false)
                .addSnapshotListener((values, error) -> {
                    // Error
                    if (error != null) {
                        Log.w(TAG, "onCreate: Failed to listen on VeterinaryClinic changes", error);
                        return;
                    }
                    // Success
                    if (values != null && !values.getDocuments().isEmpty()) {
                        clinicIdList = new ArrayList<>();
                        clinicNameList = new ArrayList<>();

                        for (DocumentSnapshot document : values.getDocuments()) {
                            VeterinaryClinic clinic = document.toObject(VeterinaryClinic.class);
                            clinicIdList.add(document.getId());
                            clinicNameList.add(clinic.getName());
                        }
                        Collections.sort(clinicNameList);

                        ArrayAdapter<String> clinicNamesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, clinicNameList);
                        binding.edmClinic.setAdapter(clinicNamesAdapter);
                        binding.edmClinic.setText(clinicNameList.get(0), false);
                        selectedClinicIndex = 0;

                        // Load user data
                        loadUser();
                    }
                });
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

        // Cancel
        binding.btnCancel.setOnClickListener(view -> {
            // Get inputs
            String displayName = binding.edtDisplayName.getText().toString();
            String gender = binding.edmGender.getText().toString();
            Timestamp dob = selectedDob == null ? null : new Timestamp(selectedDob);
            String role = binding.edmRole.getText().toString();
            String clinicName = binding.edmClinic.getText().toString();
            String clinicId = clinicIdList.get(clinicNameList.indexOf(clinicName));

            // Check for changes
            if (isVet(selectedUserRole)) {
                if (!clinicId.equals(originalClinicId) || isPfpChanged || !displayName.equals(originalDisplayName) ||
                        !gender.equals(originalGender) || (dob != null && !dob.equals(originalDob)) || !role.equals(originalRole)) {
                    cancelDialog.show();
                } else {
                    discardChanges();
                }
            } else {
                if (isPfpChanged || !displayName.equals(originalDisplayName) || !gender.equals(originalGender) ||
                        (dob != null && !dob.equals(originalDob)) || !role.equals(originalRole)) {
                    cancelDialog.show();
                } else {
                    discardChanges();
                }
            }
        });

        // Confirm
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
            String clinicName = binding.edmClinic.getText().toString();
            String clinicId = clinicIdList.get(clinicNameList.indexOf(clinicName));

            // Validate inputs
            boolean isValid = validateInputs();

            // Update user if inputs are valid
            if (isValid) {
                updatePfp(pfpData, displayName, gender, dob, role, clinicId);
            }
        });
        //endregion

        //region Other Listeners
        bindTextChangeListeners();;
        //endregion
    }

    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuthUtils.checkSignedIn(this);
    }

    private void loadUser() {
        // Get user data
        usersRef.document(uid)
                .get()
                .addOnCompleteListener(task -> {
                    // Error
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "onCreate: Failed to listen on current User changes", task.getException());
                        return;
                    }
                    // Success
                    if (task.isSuccessful()) {
                        User user = task.getResult().toObject(User.class);
                        SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy", Locale.getDefault());

                        // Switch view mode based on selected user and current user's role
                        switchViewMode(selectedUserRole, currentUserRole);
                        toggleEditMode(false);

                        // Get vet info if selected user is VET or VET_STAFF
                        if (selectedUserRole.equals(Role.VETERINARIAN.getTitle())) {
                            vetsRef.whereEqualTo("uid", uid)
                                    .get()
                                    .addOnCompleteListener(task1 -> {
                                        Veterinarian vet = task1.getResult().getDocuments().get(0).toObject(Veterinarian.class);
                                        originalClinicId = vet.getVeterinaryClinicId();
                                        selectedClinicIndex = clinicIdList.indexOf(originalClinicId);
                                        binding.edmClinic.setText(clinicNameList.get(selectedClinicIndex), false);
                                    });

                        } else if (selectedUserRole.equals(Role.VETERINARY_STAFF.getTitle())) {
                            vetStaffRef.whereEqualTo("uid", uid)
                                    .get()
                                    .addOnCompleteListener(task1 -> {
                                        VeterinaryStaff vetStaff = task1.getResult().getDocuments().get(0).toObject(VeterinaryStaff.class);
                                        originalClinicId = vetStaff.getVeterinaryClinicId();
                                        selectedClinicIndex = clinicIdList.indexOf(originalClinicId);
                                        binding.edmClinic.setText(clinicNameList.get(selectedClinicIndex), false);
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
                        binding.txtBannerCreated.setText(String.format("Created %s", sdf.format(user.getCreatedAt().toDate())));
                        binding.txtBannerUpdated.setText(String.format("Updated %s", sdf.format(user.getUpdatedAt().toDate())));

                        // Load fields
                        binding.edtDisplayName.setText(originalDisplayName);
                        binding.edmGender.setText(originalGender, false);
                        binding.edtDob.setText(
                                originalDob == null
                                        ? "Not set"
                                        : String.format("%s", sdf.format(originalDob.toDate()))
                        );
                        binding.edmRole.setText(originalRole, false);
                        binding.edtEmail.setText(originalEmail);
                    }
                });
    }

    private void updatePfp(byte[] pfpData, String displayName, String gender, Timestamp dob, String role, String clinicId) {
        // Check if profile picutre is updated
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
                            updateUser(pfpUrl.toString(), displayName, gender, dob, role, clinicId);
                        }
                    });
        } else {
            updateUser(originalPfpUrl, displayName, gender, dob, role, clinicId);
        }
    }

    private void updateUser(String pfpUrl, String displayName, String gender, Timestamp dob, String role, String clinicId) {
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
                    processRoleUpdate(initialRole, role, clinicId);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "updateUser: Failed to update User", e);
                });
    }

    private void processRoleUpdate(String initialRole, String newRole, String clinicId) {
        // Check updated role
        // USER -> VET / VET_STAFF
        if (!newRole.equals(initialRole) && (initialRole.equals(USER_ROLE) || initialRole.equals(ADMIN_ROLE)) && isVet(newRole)) {
            updateUserToVet(initialRole, newRole, clinicId);
        }
        // VET / VET_STAFF -> REMAIN
        else if (newRole.equals(initialRole) && isVet(initialRole)) {
            updateVet(initialRole, newRole, clinicId);
        }
        // VET / VET_STAFF -> INTERCHANGE
        else if (!newRole.equals(initialRole) && isVet(initialRole) && isVet(newRole)) {
            updateVetInterchange(initialRole, newRole, clinicId);
        }
        // VET / VET_STAFF -> USER / ADMIN
        else if (!newRole.equals(initialRole) && isVet(initialRole) && !isVet(newRole)) {
            updateVetToUser(initialRole, newRole);
        }
        // REMAIN ROLE
        else if (newRole.equals(initialRole)) {
            Toast.makeText(this, String.format("Successfully updated %s.", initialRole), Toast.LENGTH_SHORT).show();
            reloadActivity(newRole);
        }
        // USER -> ADMIN / ADMIN -> USER
        else {
            Toast.makeText(this, String.format("Successfully updated %s to %s.", initialRole, newRole), Toast.LENGTH_SHORT).show();
            reloadActivity(newRole);
        }
    }

    // Create new VET or VET_STAFF from User or enable existing VET or VET_STAFF
    private void updateUserToVet(String initialRole, String newRole, String clinicId) {
        // VET
        if (newRole.equals(VET_ROLE)) {
            // Check if there is already existing Veterinarian document with this uid
            vetsRef.whereEqualTo("uid", uid)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        // If exists, just set the existing document's "deleted" to false
                        if (!queryDocumentSnapshots.getDocuments().isEmpty()) {
                            vetsRef.document(queryDocumentSnapshots.getDocuments().get(0).getId())
                                    .update("deleted", false, "veterinaryClinicId", clinicId)
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(this, String.format("Successfully updated %s to %s.", initialRole, newRole), Toast.LENGTH_SHORT).show();
                                            reloadActivity(newRole);
                                        } else {
                                            Log.w(TAG, "updateUserToVet: Failed to update User to existing Veterinarian", task.getException());
                                        }
                                    });
                        }
                        // Does not exist, create new Veterinarian
                        else {
                            Veterinarian newVet = new Veterinarian(
                                    uid,
                                    clinicId,
                                    VET_ROLE,
                                    VET_ROLE,
                                    Timestamp.now(),
                                    Timestamp.now(),
                                    false
                            );
                            vetsRef.add(newVet)
                                    .addOnSuccessListener(documentReference -> {
                                        Toast.makeText(this, String.format("Successfully updated %s to %s.", initialRole, newRole), Toast.LENGTH_SHORT).show();
                                        reloadActivity(newRole);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.w(TAG, "updateUser: Failed to create new Veterinarian", e);
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "updateUserToVet: Failed to retrieve Veterinarian data", e);
                    });
        }
        // VET_STAFF
        else if (newRole.equals(VET_STAFF_ROLE)) {
            // Check if there is already existing VeterinaryStaff document with this uid
            vetStaffRef.whereEqualTo("uid", uid)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        // If exists, just set the existing document's "deleted" to false
                        if (!queryDocumentSnapshots.getDocuments().isEmpty()) {
                            vetStaffRef.document(queryDocumentSnapshots.getDocuments().get(0).getId())
                                    .update("deleted", false, "veterinaryClinicId", clinicId)
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(this, String.format("Successfully updated %s to %s.", initialRole, newRole), Toast.LENGTH_SHORT).show();
                                            reloadActivity(newRole);
                                        } else {
                                            Log.w(TAG, "updateUserToVet: Failed to update User to existing VeterinaryStaff", task.getException());
                                        }
                                    });
                        }
                        // Does not exist, create new VeterinaryStaff
                        else {
                            VeterinaryStaff newVetStaff = new VeterinaryStaff(
                                    uid,
                                    clinicId,
                                    VET_ROLE,
                                    Timestamp.now(),
                                    Timestamp.now(),
                                    false
                            );
                            vetStaffRef.add(newVetStaff)
                                    .addOnSuccessListener(documentReference -> {
                                        Toast.makeText(this, String.format("Successfully updated %s to %s.", initialRole, newRole), Toast.LENGTH_SHORT).show();
                                        reloadActivity(newRole);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.w(TAG, "updateUser: Failed to create new VeterinaryStaff", e);
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "updateUserToVet: Failed to retrieve Veterinarian data", e);
                    });
        }
    }

    // Update VET or VET_STAFF directly with no role change
    private void updateVet(String initialRole, String newRole, String clinicId) {
        // VET
        if (newRole.equals(VET_ROLE)) {
            vetsRef.whereEqualTo("uid", uid)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            vetsRef.document(task.getResult().getDocuments().get(0).getId())
                                    .update("veterinaryClinicId", clinicId, "updatedAt", Timestamp.now())
                                    .addOnSuccessListener(unused1 -> {
                                        Toast.makeText(this, String.format("Successfully updated %s.", initialRole), Toast.LENGTH_SHORT).show();
                                        reloadActivity(initialRole);
                                    })
                                    .addOnFailureListener(e -> Log.w(TAG, "updateUser: Failed to update Veterinarian", e));
                        }
                    });
        }
        // VET_STAFF
        else if (newRole.equals(VET_STAFF_ROLE)) {
            vetStaffRef.whereEqualTo("uid", uid)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            vetStaffRef.document(task.getResult().getDocuments().get(0).getId())
                                    .update("veterinaryClinicId", clinicId, "updatedAt", Timestamp.now())
                                    .addOnSuccessListener(unused1 -> {
                                        Toast.makeText(this, String.format("Successfully updated %s.", initialRole), Toast.LENGTH_SHORT).show();
                                        reloadActivity(initialRole);
                                    })
                                    .addOnFailureListener(e -> Log.w(TAG, "updateUser: Failed to update VeterinaryStaff", e));
                        }
                    });
        }
    }

    // Handles interchange between VET and VET_STAFF
    private void updateVetInterchange(String initialRole, String newRole, String clinicId) {
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
                                                clinicId,
                                                VET_STAFF_ROLE,
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
                                                clinicId,
                                                VET_ROLE,
                                                VET_ROLE,
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

    // Soft deletes VET or VET_STAFF if changed to User
    private void updateVetToUser(String initialRole, String newRole) {
        // VET
        if (initialRole.equals(VET_ROLE)) {
            vetsRef.whereEqualTo("uid", uid)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            vetsRef.document(task.getResult().getDocuments().get(0).getId())
                                    .update("deleted", true, "updatedAt", Timestamp.now())
                                    .addOnSuccessListener(unused1 -> {
                                        Toast.makeText(this, String.format("Successfully updated %s to %s.", initialRole, newRole), Toast.LENGTH_SHORT).show();
                                        reloadActivity(newRole);
                                    })
                                    .addOnFailureListener(e -> Log.w(TAG, "updateUser: Failed to update Veterinarian to User/Admin", e));
                        }
                    });
        }
        // VET_STAFF
        else if (initialRole.equals(VET_STAFF_ROLE)) {
            vetStaffRef.whereEqualTo("uid", uid)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            vetStaffRef.document(task.getResult().getDocuments().get(0).getId())
                                    .update("deleted", true, "updatedAt", Timestamp.now())
                                    .addOnSuccessListener(unused1 -> {
                                        Toast.makeText(this, String.format("Successfully updated %s to %s.", initialRole, newRole), Toast.LENGTH_SHORT).show();
                                        reloadActivity(newRole);
                                    })
                                    .addOnFailureListener(e -> Log.w(TAG, "updateUser: Failed to update VeterinaryStaff to User/Admin", e));
                        }
                    });
        }
    }

    private void deleteUser() {
        usersRef.document(uid)
                .update(
                        "deleted", true,
                        "updatedAt", Timestamp.now()
                )
                .addOnSuccessListener(unused -> {
                    // If VET or VET_STAFF, need to update respective documents as well
                    if (isVet(selectedUserRole)) {
                        if (selectedUserRole.equals(Role.VETERINARIAN.getTitle())) {
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
                        } else if (selectedUserRole.equals(Role.VETERINARY_STAFF.getTitle())) {
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
                    } else {
                        Toast.makeText(this, String.format("Successfully deleted %s", originalRole), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "deleteUser: Failed to delete User", e);
                });
    }

    // UNUSED
    private boolean validateInputs() {
        return true;
    }

    private void bindTextChangeListeners() {
        // Role
        binding.edmRole.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                selectedUserRole = binding.edmRole.getText().toString();
                switchViewMode(selectedUserRole, currentUserRole);
            }
            @Override
            public void afterTextChanged(Editable editable) {}
        });
    }

    private void switchViewMode(String selectedUserRole, String currentUserRole) {
        // Admin not allowed to modify another admin
        if (currentUserRole.equals(ADMIN_ROLE) && selectedUserRole.equals(ADMIN_ROLE)) {
            binding.appBarEdituser.getMenu().clear();
        }

        // Show "Veterinary Clinic" field for VET and VET_STAFF
        if (selectedUserRole.equals(VET_ROLE) || selectedUserRole.equals(VET_STAFF_ROLE)) {
            binding.txtfieldClinic.setVisibility(View.VISIBLE);
        } else {
            binding.txtfieldClinic.setVisibility(View.GONE);
        }
    }

    private void toggleEditMode(boolean enabled) {
        int textColor;

        binding.appBarEdituser.getMenu().clear();

        // Check mode
        if (enabled) {
            textColor = getResources().getColor(R.color.black);

            // Controls
            binding.btnRevert.setVisibility(View.VISIBLE);
            binding.btnUpload.setVisibility(View.VISIBLE);
            binding.txtCredentialTip.setVisibility(View.VISIBLE);
            binding.btnSendPasswordReset.setVisibility(View.VISIBLE);
            binding.layoutEditButtons.setVisibility(View.VISIBLE);

            // datePicker
            binding.txtfieldDob.setEndIconVisible(originalDob != null);
        } else {
            textColor = getResources().getColor(R.color.gray_400);

            // Controls
            binding.appBarEdituser.inflateMenu(R.menu.top_app_bar_edituser);
            binding.btnRevert.setVisibility(View.GONE);
            binding.btnUpload.setVisibility(View.GONE);
            binding.txtCredentialTip.setVisibility(View.GONE);
            binding.btnSendPasswordReset.setVisibility(View.GONE);
            binding.layoutEditButtons.setVisibility(View.GONE);

            // dateOfBirth
            binding.txtfieldDob.setEndIconVisible(false);

            // Clear errors
            binding.txtfieldRole.setErrorEnabled(false);
            binding.txtfieldClinic.setErrorEnabled(false);
        }

        // Toggle fields
        binding.txtfieldDisplayName.setEnabled(enabled);
        binding.txtfieldGender.setEnabled(enabled);
        binding.txtfieldDob.setEnabled(enabled);
        binding.txtfieldRole.setEnabled(enabled);
        binding.txtfieldClinic.setEnabled(enabled);

        // Text counters
        binding.txtfieldDisplayName.setCounterEnabled(enabled);

        // End icons
        binding.txtfieldDob.setEndIconVisible(enabled);

        // Text colors
        binding.edtDisplayName.setTextColor(textColor);
        binding.edmGender.setTextColor(textColor);
        binding.edtDob.setTextColor(textColor);
        binding.edmRole.setTextColor(textColor);
        binding.edmClinic.setTextColor(textColor);
    }

    private void discardChanges() {
        SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy", Locale.getDefault());

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
                : sdf.format(originalDob.toDate())
        );
        binding.edmRole.setText(originalRole, false);
        binding.edtEmail.setText(originalEmail);

        if (isVet(selectedUserRole)) {
            binding.edmClinic.setText(clinicNameList.get(clinicIdList.indexOf(originalClinicId)), false);
        }
    }

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

    private boolean isVet(String role) {
        return role.equals(Role.VETERINARIAN.getTitle()) || role.equals(Role.VETERINARY_STAFF.getTitle());
    }

    private void reloadActivity(String role) {
        Intent intent = new Intent(this, EditUserActivity.class);
        intent.putExtra(UserAdapter.KEY_UID, uid);
        intent.putExtra(UserAdapter.KEY_ROLE, role);

        finish();
        startActivity(intent);
    }
}
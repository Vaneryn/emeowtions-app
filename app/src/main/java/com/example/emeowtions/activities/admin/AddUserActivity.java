package com.example.emeowtions.activities.admin;

import android.content.Context;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
import com.example.emeowtions.databinding.ActivityAddUserBinding;
import com.example.emeowtions.enums.Role;
import com.example.emeowtions.models.User;
import com.example.emeowtions.models.Veterinarian;
import com.example.emeowtions.models.VeterinaryClinic;
import com.example.emeowtions.models.VeterinaryStaff;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.FirebaseApp;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddUserActivity extends AppCompatActivity {

    private static final String TAG = "AddUserActivity";
    private SharedPreferences sharedPreferences;

    // Firebase variables
    private FirebaseAuthUtils firebaseAuthUtils;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private CollectionReference usersRef;
    private CollectionReference clinicsRef;
    private CollectionReference vetStaffRef;
    private CollectionReference vetsRef;
    private StorageReference storageRef;
    private StorageReference userPfpRef;

    // Layout variables
    private ActivityAddUserBinding binding;

    // Private variables
    private String currentUserRole;
    private boolean isPfpUploaded;
    private List<String> clinicIds;
    private List<String> clinicNames;
    private int selectedClinicIndex;
    private Date selectedDob;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Shared preferences
        sharedPreferences = getSharedPreferences("com.emeowtions", Context.MODE_PRIVATE);
        currentUserRole = sharedPreferences.getString("role", "Admin");

        // Initialize Firebase service instances
        firebaseAuthUtils = new FirebaseAuthUtils();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        // Initialize Firestore references
        usersRef = db.collection("users");
        clinicsRef = db.collection("veterinaryClinics");
        vetStaffRef = db.collection("veterinaryStaff");
        vetsRef = db.collection("veterinarians");
        // Initialize Storage references
        storageRef = storage.getReference();

        // Get ViewBinding and set content view
        binding = ActivityAddUserBinding.inflate(getLayoutInflater());
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
        // Load banner text
        binding.txtRole.setText(Role.USER.getTitle());

        // Load default dropdown menu selections
        binding.edmRole.setText(Role.USER.getTitle(), false);
        binding.edmGender.setText(getString(R.string.unspecified), false);

        // Hide date of birth clear button
        binding.txtfieldDateofbirth.setEndIconVisible(false);

        // Create photo picker
        ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
                registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                    if (uri != null) {
                        Glide.with(getApplicationContext())
                                .load(uri)
                                .into(binding.imgProfilePicture);
                        isPfpUploaded = true;
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

        // Cancel dialog
        MaterialAlertDialogBuilder cancelDialog =
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.cancel)
                        .setMessage(R.string.cancel_dialog_generic_message)
                        .setNegativeButton(getString(R.string.no), (dialogInterface, i) -> {
                            // Do nothing
                        })
                        .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                            finish();
                        });
        //endregion

        //region Navigation Listeners
        binding.appBarAddUser.setNavigationOnClickListener(view -> finish());
        //endregion

        //region Load Data
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
                        clinicIds = new ArrayList<>();
                        clinicNames = new ArrayList<>();

                        for (DocumentSnapshot document : values.getDocuments()) {
                            VeterinaryClinic clinic = document.toObject(VeterinaryClinic.class);
                            clinicIds.add(document.getId());
                            clinicNames.add(clinic.getName());
                        }

                        ArrayAdapter<String> clinicNamesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, clinicNames);
                        binding.edmClinic.setAdapter(clinicNamesAdapter);
                        binding.edmClinic.setText(clinicNames.get(0), false);
                        selectedClinicIndex = 0;
                    }
        });
        //endregion

        //region onClick Listeners
        // Profile picture action buttons
        binding.btnRevert.setOnClickListener(view -> {
            isPfpUploaded = false;
            loadPfp(null);
        });
        binding.btnUpload.setOnClickListener(view -> {
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        // edmClinic: update selected item index
        binding.edmClinic.setOnItemClickListener((adapterView, view, i, l) -> {
            selectedClinicIndex = i;
        });

        // datePicker stuff
        datePicker.addOnPositiveButtonClickListener(selection -> {
            // Get selected date
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(selection);
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            int month = calendar.get(Calendar.MONTH) + 1;
            int year = calendar.get(Calendar.YEAR);
            String dateText = day + "/" + month + "/" + year;

            // Set selected date
            selectedDob = new Date(selection);
            binding.edtDateofbirth.setText(dateText);
            binding.txtfieldDateofbirth.setEndIconVisible(true);
        });

        binding.edtDateofbirth.setOnClickListener(view -> {
            // Prevent app crash from double DatePicker opening
            if (!datePicker.isAdded()) {
                datePicker.show(getSupportFragmentManager(), TAG);
            }
        });

        binding.edtDateofbirth.setOnLongClickListener(view -> true);

        binding.txtfieldDateofbirth.setEndIconOnClickListener(view -> {
            binding.edtDateofbirth.getText().clear();
            binding.txtfieldDateofbirth.setEndIconVisible(false);
        });

        // btnCancel: return to AdminUsersFragment
        binding.btnCancel.setOnClickListener(view -> {
            cancelDialog.show();
        });

        // btnConfirm: add new user
        binding.btnConfirm.setOnClickListener(view -> {
            // Get inputs
            byte[] pfpData = null;
            if (isPfpUploaded) {
                binding.imgProfilePicture.setDrawingCacheEnabled(true);
                binding.imgProfilePicture.buildDrawingCache();
                Bitmap bitmap = ((BitmapDrawable) binding.imgProfilePicture.getDrawable()).getBitmap();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                pfpData = baos.toByteArray();
            }

            String email = binding.edtEmail.getText().toString();
            String password = binding.edtPassword.getText().toString();
            String confirmPassword = binding.edtConfirmPassword.getText().toString();
            String role = binding.edmRole.getText().toString();
            String clinicName = binding.edmClinic.getText().toString();
            String displayName = binding.edtDisplayName.getText().toString();
            String gender = binding.edmGender.getText().toString();
            Timestamp dob = selectedDob == null ? null : new Timestamp(selectedDob);

            // Validate inputs
            boolean isValid = validateInputs(email, password, confirmPassword, role, clinicName);

            // Create new user if inputs are valid
            if (isValid) {
                createAuthUser(pfpData, email, password, role, clinicIds.get(selectedClinicIndex), displayName, gender, dob);
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

    private void createAuthUser(byte[] pfpData, String email, String password, String role, String clinicId, String displayName, String gender, Timestamp dob) {
        FirebaseApp adminApp = FirebaseApp.getInstance("admin");
        FirebaseAuth adminAuth = FirebaseAuth.getInstance(adminApp);

        adminAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    if (authResult != null) {
                        // Retrieve new FirebaseAuth user
                        FirebaseUser newAuthUser = authResult.getUser();

                        if (newAuthUser == null) {
                            Log.w(TAG, "createAuthUser: No authResult");
                            Toast.makeText(this, "Authenticator returned no results.", Toast.LENGTH_SHORT).show();
                        } else {
                            createFirestoreUser(newAuthUser.getUid(), pfpData, email, role, clinicId, displayName, gender, dob);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "createAuthUser: Failed to create Firebase Authentication user", e);
                    Toast.makeText(this, "Failed to create user account." + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void createFirestoreUser(String uid, byte[] pfpData, String email, String role, String clinicId, String displayName, String gender, Timestamp dob) {
        User newUser = new User(
                displayName.isBlank() ? email.split("@")[0] : displayName,
                email,
                gender,
                role,
                null,
                dob,
                true,   // TODO: User accounts are still unverified in FirebaseAuth, but for testing purposes we set Firestore User.verified to true
                Timestamp.now(),
                Timestamp.now(),
                false
        );

        // Create new Firestore user
        usersRef.document(uid)
                .set(newUser)
                .addOnSuccessListener(unused -> {
                    // Upload and set profile picture if uploaded
                    if (!isPfpUploaded) {
                        // Create new document for VeterinaryStaff or Veterinarian account
                        if (role.equals(Role.VETERINARY_STAFF.getTitle()) || role.equals(Role.VETERINARIAN.getTitle())) {
                            createVet(role, uid, clinicId);
                        } else {
                            Toast.makeText(this, "Successfully created new " + role + ".", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        userPfpRef = storageRef.child("images/users/" + uid + "/profile_picture.jpg");

                        // Upload pfpData
                        userPfpRef.putBytes(pfpData)
                                .continueWithTask(task -> {
                                    if (!task.isSuccessful()) {
                                        throw task.getException();
                                    }
                                    return userPfpRef.getDownloadUrl();
                                })
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Uri pfpUrl = task.getResult();

                                        // Update Firestore User with profile picture
                                        usersRef.document(uid)
                                                .update("profilePicture", pfpUrl)
                                                .addOnSuccessListener(unused1 -> {
                                                    // Create new document for VeterinaryStaff or Veterinarian account
                                                    if (role.equals(Role.VETERINARY_STAFF.getTitle()) || role.equals(Role.VETERINARIAN.getTitle())) {
                                                        createVet(role, uid, clinicId);
                                                    } else {
                                                        Toast.makeText(this, "Successfully created new " + role + ".", Toast.LENGTH_SHORT).show();
                                                        finish();
                                                    }
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.w(TAG, "createFirestoreUser: Failed to update User profile picture", e);
                                                });
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "createFirestoreUser: Failed to create new User", e);
                });
    }

    private void createVet(String role, String uid, String clinicId) {
        // Create new document for VeterinaryStaff or Veterinarian account
        if (role.equals(Role.VETERINARY_STAFF.getTitle())) {
            VeterinaryStaff newVetStaff = new VeterinaryStaff(
                    uid,
                    clinicId,
                    Role.VETERINARY_STAFF.getTitle(),
                    Timestamp.now(),
                    Timestamp.now(),
                    false
            );

            vetStaffRef.add(newVetStaff)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Successfully created new " + role + ".", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "createVet: Failed to create VeterinaryStaff", e);
                    });
        } else if (role.equals(Role.VETERINARIAN.getTitle())) {
            Veterinarian newVet = new Veterinarian(
                    uid,
                    clinicId,
                    Role.VETERINARIAN.getTitle(),
                    Role.VETERINARIAN.getTitle(),
                    Timestamp.now(),
                    Timestamp.now(),
                    false
            );

            vetsRef.add(newVet)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Successfully created new " + role + ".", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "createVet: Failed to create Veterinarian", e);
                    });
        }
    }

    private boolean validateInputs(String email, String password, String confirmPassword, String role, String clinicName) {
        // Reset errors
        binding.txtfieldEmail.setErrorEnabled(false);
        binding.txtfieldPassword.setErrorEnabled(false);
        binding.txtfieldConfirmPassword.setErrorEnabled(false);
        binding.txtfieldRole.setErrorEnabled(false);
        binding.txtfieldClinic.setErrorEnabled(false);

        // Validate email
        // Empty input
        if (email.isBlank()) {
            binding.txtfieldEmail.setError(getString(R.string.email_required_error));
            binding.txtfieldEmail.requestFocus();
            return false;
        }
        // Email format
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.txtfieldEmail.setError(getString(R.string.email_format_invalid));
            binding.txtfieldEmail.requestFocus();
            return false;
        }
        // Unique email
        usersRef.whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult() != null && !task.getResult().isEmpty()) {
                            // Email already exists
                            binding.txtfieldEmail.setError("Email is already in use");
                        }
                    } else {
                        Toast.makeText(this, "Error checking email: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

        // Validate password
        // Empty input
        if (password.isBlank()) {
            binding.txtfieldPassword.setError(getString(R.string.password_required_error));
            binding.txtfieldPassword.requestFocus();
            return false;
        }
        /* Password strength
           - At least 8 characters
           - Contains at least 1 letter, 1 number, and 1 special character
        */
        String passwordRegex = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,}$";
        if (!password.matches(passwordRegex)) {
            binding.txtfieldPassword.setError("Password should be at least 8 characters, containing at least 1 letter, 1 number, and 1 special character");
            binding.txtfieldPassword.requestFocus();
            return false;
        }

        // Validate confirm password
        // Empty input
        if (confirmPassword.isBlank()) {
            binding.txtfieldConfirmPassword.setError(getString(R.string.confirm_password_required_error));
            binding.txtfieldConfirmPassword.requestFocus();
            return false;
        }
        // Check if password and confirm password match
        if (!password.equals(confirmPassword)) {
            binding.txtfieldConfirmPassword.setError("Passwords do not match");
            binding.txtfieldConfirmPassword.requestFocus();
            return false;
        }

        // Validate role
        // Empty input
        if (role.isBlank()) {
            binding.txtfieldRole.setError(getString(R.string.role_required_error));
            binding.txtfieldRole.requestFocus();
            return false;
        }

        // Validate clinic (only if Role is VETERINARY_STAFF or VETERINARIAN)
        // Empty input
        if (role.equals(Role.VETERINARY_STAFF.getTitle()) || role.equals(Role.VETERINARIAN.getTitle())) {
            if (clinicName.isBlank()) {
                binding.txtfieldClinic.setError(getString(R.string.veterinary_clinic_required_error));
                binding.txtfieldClinic.requestFocus();
                return false;
            }
        }

        return true;
    }

    private void loadPfp(String pfpUrl) {
        if (pfpUrl == null) {
            // Load default picture if no profile picture exists
            Glide.with(getApplicationContext())
                    .load(R.drawable.baseline_person_24)
                    .into(binding.imgProfilePicture);
        } else {
            // Load original profile picture
            Glide.with(getApplicationContext())
                    .load(pfpUrl)
                    .into(binding.imgProfilePicture);
        }
    }

    private void bindTextChangeListeners() {
        // Account Credentials
        binding.edtEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                binding.txtfieldEmail.setErrorEnabled(false);

                // Update banner text
                String email = binding.edtEmail.getText().toString();
                String displayName = binding.edtDisplayName.getText().toString();

                binding.txtEmail.setText(binding.edtEmail.getText().toString());

                if (email.isBlank()) {
                    binding.txtEmail.setText(R.string.email);
                }

                if (displayName.isBlank()) {
                    if (email.isBlank()) {
                        binding.txtDisplayName.setText(R.string.name);
                    } else {
                        // Use front part of email as displayName if displayName input not provided
                        binding.txtDisplayName.setText(email.split("@")[0]);
                    }
                }
            }
            @Override
            public void afterTextChanged(Editable editable) {}
        });

        binding.edtPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                binding.txtfieldPassword.setErrorEnabled(false);
            }
            @Override
            public void afterTextChanged(Editable editable) {}
        });

        binding.edtConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                binding.txtfieldConfirmPassword.setErrorEnabled(false);
            }
            @Override
            public void afterTextChanged(Editable editable) {}
        });

        // Role
        binding.edmRole.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                binding.txtfieldRole.setErrorEnabled(false);

                // Update banner text
                String role = binding.edmRole.getText().toString();
                binding.txtRole.setText(role);

                // Show Clinic dropdown only if VETERINARY_STAFF or VETERINARIAN role selected
                if (role.equals(Role.VETERINARY_STAFF.getTitle()) || role.equals(Role.VETERINARIAN.getTitle())) {
                    binding.txtfieldClinic.setVisibility(View.VISIBLE);
                } else {
                    binding.txtfieldClinic.setVisibility(View.GONE);
                }
            }
            @Override
            public void afterTextChanged(Editable editable) {}
        });

        binding.edmClinic.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                binding.txtfieldClinic.setErrorEnabled(false);
            }
            @Override
            public void afterTextChanged(Editable editable) {}
        });

        // Personal Information
        binding.edtDisplayName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String email = binding.edtEmail.getText().toString();
                String displayName = binding.edtDisplayName.getText().toString();

                if (!displayName.isBlank()) {
                    // displayName input always overrides email for displayName
                    binding.txtDisplayName.setText(displayName);
                } else {
                    if (email.isBlank()) {
                        binding.txtEmail.setText(R.string.email);
                    } else {
                        // Use front part of email as displayName if displayName input not provided
                        binding.txtDisplayName.setText(email.split("@")[0]);
                    }
                }
            }
            @Override
            public void afterTextChanged(Editable editable) {}
        });
    }
}
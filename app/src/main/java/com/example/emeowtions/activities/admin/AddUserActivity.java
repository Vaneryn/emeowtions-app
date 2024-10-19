package com.example.emeowtions.activities.admin;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
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
import com.example.emeowtions.databinding.ActivityAddUserBinding;
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
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddUserActivity extends AppCompatActivity {

    private static final String TAG = "AddUserActivity";

    // Firebase variables
    private FirebaseAuthUtils firebaseAuthUtils;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private CollectionReference usersRef;
    private StorageReference storageRef;
    private StorageReference userPfpRef;

    // Layout variables
    private ActivityAddUserBinding binding;

    // Private variables
    private boolean isPfpUploaded;
    private Date selectedDob;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase service instances
        firebaseAuthUtils = new FirebaseAuthUtils();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        // Initialize Firestore references
        usersRef = db.collection("users");
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
                        .setTitle(R.string.unsaved_changes)
                        .setMessage(R.string.edit_profile_cancel_message)
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
        //

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

        // btnCancel: return to
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
            String clinic = binding.edmClinic.getText().toString();
            String displayName = binding.edtDisplayName.getText().toString();
            String gender = binding.edmGender.getText().toString();
            Timestamp dob = selectedDob == null ? null : new Timestamp(selectedDob);

            // Validate inputs
            boolean isValid = validateInputs(email, password, confirmPassword, role, clinic);

            // Create new user if inputs are valid
            if (isValid) {
                createUser(pfpData, email, password, confirmPassword, role, displayName, gender, dob);
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

    private void createUser(byte[] pfpData, String email, String password, String confirmPassword, String role, String displayName, String gender, Timestamp dob) {

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

        // Validate clinic
        // Empty input
        if (clinicName.isBlank()) {
            binding.txtfieldClinic.setError(getString(R.string.veterinary_clinic_required_error));
            binding.txtfieldClinic.requestFocus();
            return false;
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

        binding.edmRole.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                binding.txtfieldRole.setErrorEnabled(false);
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
    }
}
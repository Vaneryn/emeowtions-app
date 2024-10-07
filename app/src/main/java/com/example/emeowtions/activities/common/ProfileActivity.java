package com.example.emeowtions.activities.common;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.emeowtions.R;
import com.example.emeowtions.databinding.ActivityProfileBinding;
import com.example.emeowtions.models.User;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    private ActivityProfileBinding profileBinding;

    private FirebaseAuthUtils firebaseAuthUtils;
    private FirebaseFirestore db;
    private CollectionReference usersRef;

    private String originalDisplayName;
    private String originalGender;
    private Timestamp originalDateOfBirth;
    private String originalEmail;
    private Date selectedDateOfBirth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase service instances
        firebaseAuthUtils = new FirebaseAuthUtils();
        db = FirebaseFirestore.getInstance();
        // Initialize Firestore references
        usersRef = db.collection("users");

        // Get ViewBinding and set content view
        profileBinding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(profileBinding.getRoot());

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
        profileBinding.txtfieldDateofbirth.setEndIconVisible(false);

        // DatePicker dialog
        MaterialDatePicker<Long> datePicker =
                MaterialDatePicker.Builder.datePicker()
                        .setTitleText("Select date")
                        .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                        .build();

        // Cancel profile edit dialog
        MaterialAlertDialogBuilder cancelDialog =
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.edit_profile)
                        .setMessage(R.string.edit_profile_cancel_message)
                        .setNegativeButton(getString(R.string.no), (dialogInterface, i) -> {
                            // Do nothing
                        })
                        .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                            discardChanges();
                        });
        //endregion

        //region onClick listeners
        // appBarProfile back button: return to Home screen
        profileBinding.appBarProfile.setNavigationOnClickListener(view -> finish());

        // appBarProfile edit button: enable text fields
        profileBinding.appBarProfile.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.action_edit_profile) {
                toggleEditMode(true);
            }

            return false;
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
            profileBinding.edtDateofbirth.setText(dateText);
            profileBinding.txtfieldDateofbirth.setEndIconVisible(true);
        });

        profileBinding.edtDateofbirth.setOnClickListener(view -> {
            // Prevent app crash from double DatePicker opening
            if (!datePicker.isAdded()) {
                datePicker.show(getSupportFragmentManager(), TAG);
            }
        });

        profileBinding.edtDateofbirth.setOnLongClickListener(view -> true);

        profileBinding.txtfieldDateofbirth.setEndIconOnClickListener(view -> {
            selectedDateOfBirth = null;
            profileBinding.txtfieldDateofbirth.setEndIconVisible(false);
            profileBinding.edtDateofbirth.getText().clear();
            profileBinding.edtDateofbirth.setText(R.string.not_set);
        });

        // btnCancelEditProfile: disable text fields
        profileBinding.btnCancelEditProfile.setOnClickListener(view -> {
            String displayName = profileBinding.edtDisplayName.getText().toString();
            String gender = profileBinding.edmGender.getText().toString();
            Date dateOfBirth = selectedDateOfBirth;
            String email = profileBinding.edtEmail.getText().toString();

            Log.d(TAG, "originalDateOfBirth: " + originalDateOfBirth);

            // Proceed only if any changes are detected
            if (!displayName.equals(originalDisplayName) ||
                    !gender.equals(originalGender) ||
                    !email.equals(originalEmail)) {
                // Display name, gender, or email updated
                cancelDialog.show();
            } else if (originalDateOfBirth == null) {
                Log.d(TAG, "originalDateOfBirth == null");
                // Original date of birth was not set and is now updated
                if (dateOfBirth != null) {
                    cancelDialog.show();
                } else {
                    discardChanges();
                }
            } else if (originalDateOfBirth != null) {
                Log.d(TAG, "originalDateOfBirth != null");
                // Original date of birth was already set and is now updated
                if (dateOfBirth == null) {
                    cancelDialog.show();
                } else if (!dateOfBirth.equals(originalDateOfBirth.toDate())) {
                    cancelDialog.show();
                } else {
                    discardChanges();
                }
            } else {
                discardChanges();
            }
        });

        // btnConfirmEditProfile: update profile
        profileBinding.btnConfirmEditProfile.setOnClickListener(view -> {
            String displayName = profileBinding.edtDisplayName.getText().toString();
            String gender = profileBinding.edmGender.getText().toString();
            Date dateOfBirth = selectedDateOfBirth;
            String email = profileBinding.edtEmail.getText().toString();

            // Validate inputs
            boolean isValid = validateInputs(displayName, gender, dateOfBirth, email);

            // Update profile if inputs are valid
            if (isValid) {
                updateProfile(displayName, gender, dateOfBirth, email);
            }
        });
        //endregion

        //region Other listeners
        profileBinding.edtDisplayName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                profileBinding.txtfieldDisplayName.setErrorEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
        profileBinding.edtEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                profileBinding.txtfieldEmail.setErrorEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
        //endregion

        //region Load data
        selectedDateOfBirth = null;

        usersRef.document(firebaseAuthUtils.getUid())
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Failed to listen on current user profile changes", error);
                        return;
                    }
                    if (value != null && value.exists()) {
                        User user = value.toObject(User.class);
                        SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy", Locale.getDefault());

                        // Save original field values
                        originalDisplayName = user.getDisplayName();
                        originalGender = user.getGender();
                        originalDateOfBirth = user.getDateOfBirth();
                        originalEmail = firebaseAuthUtils.getFirebaseEmail();

                        // Profile Banner
                        profileBinding.txtBannerDisplayName.setText(user.getDisplayName());
                        profileBinding.txtBannerVerified.setText(user.isVerified() ? "Verified" : "Not verified");
                        profileBinding.txtJoinedDate.setText(String.format("Joined %s", sdf.format(user.getCreatedAt().toDate())));

                        // Profile Fields
                        profileBinding.edtDisplayName.setText(user.getDisplayName());
                        profileBinding.edmGender.setText(user.getGender(), false);
                        profileBinding.txtfieldDateofbirth.setEndIconVisible(false);
                        profileBinding.edtDateofbirth.setText(user.getDateOfBirth() == null ? getString(R.string.not_set) : sdf.format(user.getDateOfBirth().toDate()));
                        profileBinding.edtEmail.setText(firebaseAuthUtils.getFirebaseEmail());
                    }
                });
        //endregion
    }

    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuthUtils.checkSignedIn(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        firebaseAuthUtils.refreshVerification(this);
    }

    private boolean validateInputs(String displayName, String gender, Date dateOfBirth, String email) {
        // Reset errors
        profileBinding.txtfieldDisplayName.setErrorEnabled(false);
        profileBinding.txtfieldGender.setErrorEnabled(false);
        profileBinding.txtfieldDateofbirth.setErrorEnabled(false);
        profileBinding.txtfieldEmail.setErrorEnabled(false);

        // Validate display name
        // Empty input
        if (displayName.isBlank()) {
            profileBinding.txtfieldDisplayName.setError("Display Name is required");
            profileBinding.txtfieldDisplayName.requestFocus();
            return false;
        }

        // Validate email
        // Empty input
        if (email.isBlank()) {
            profileBinding.txtfieldEmail.setError("Email is required");
            profileBinding.txtfieldEmail.requestFocus();
            return false;
        }
        // Email format
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            profileBinding.txtfieldEmail.setError("Enter a valid email address");
            profileBinding.txtfieldEmail.requestFocus();
            return false;
        }

        return true;
    }

    private void updateProfile(String displayName, String gender, Date dateOfBirth, String email) {
        boolean isEmailUpdated = !email.equals(originalEmail);

        // Reauthentication is required to update email
        if (!isEmailUpdated) {
            updateFirestoreUser(displayName, gender, dateOfBirth, email, false);
        } else {
            View passwordDialogLayout = LayoutInflater.from(this).inflate(R.layout.dialog_get_password, null);
            TextInputEditText edtPassword = passwordDialogLayout.findViewById(R.id.edt_password);

            MaterialAlertDialogBuilder passwordDialog =
                    new MaterialAlertDialogBuilder(this)
                            .setView(passwordDialogLayout)
                            .setTitle(getString(R.string.enter_password))
                            .setMessage(R.string.dialog_password_message)
                            .setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
                                // Do nothing
                            })
                            .setPositiveButton(R.string.confirm, (dialogInterface, i) -> {
                                // Validate password
                                String password = edtPassword.getText().toString();
                                if (password.isBlank()) {
                                    Toast.makeText(this, "Profile update failed. Password is required to update email.", Toast.LENGTH_LONG).show();
                                } else {
                                    // Reauthenticate user
                                    AuthCredential credential = EmailAuthProvider.getCredential(firebaseAuthUtils.getFirebaseEmail(), password);

                                    firebaseAuthUtils.getFirebaseUser()
                                            .reauthenticate(credential)
                                            .addOnCompleteListener(reauthenticateTask -> {
                                                // Check if reauthentication succeeded
                                                if (!reauthenticateTask.isSuccessful()) {
                                                    Toast.makeText(this, "Profile update failed. Unable to update email due to incorrect password.", Toast.LENGTH_LONG).show();
                                                } else {
                                                    // Check for email uniqueness
                                                    usersRef.whereEqualTo("email", email)
                                                            .get()
                                                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                                                if (!queryDocumentSnapshots.isEmpty()) {
                                                                    // Email already registered
                                                                    profileBinding.txtfieldEmail.setError("Email is already registered");
                                                                    Toast.makeText(this, "Profiled update failed. Email is already registered.", Toast.LENGTH_SHORT).show();
                                                                } else {
                                                                    // Unique email
                                                                    updateFirestoreUser(displayName, gender, dateOfBirth, email, true);
                                                                }
                                                            })
                                                            .addOnFailureListener(e -> {
                                                                Log.w(TAG, "Error checking for registered email", e);
                                                            });
                                                }
                                            });
                                }
                            });

            passwordDialog.show();
        }
    }

    private void updateFirestoreUser(String displayName, String gender, Date dateOfBirth, String email, boolean isEmailUpdated) {
        usersRef.document(firebaseAuthUtils.getUid())
                .update(
                        "displayName", displayName,
                        "gender", gender,
                        "dateOfBirth", dateOfBirth == null ? null : new Timestamp(dateOfBirth),
                        "email", email,
                        "verified", !isEmailUpdated,    // set account to unverified while pending new email verification
                        "updatedAt", Timestamp.now()
                )
                .addOnCompleteListener(task -> {
                    // Update email only if it was changed (requires verification)
                    if (isEmailUpdated)
                        firebaseAuthUtils.updateEmail(this, email);

                    toggleEditMode(false);

                    Toast.makeText(this, "Successfully updated your profile.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error updating profile", e);
                    Toast.makeText(this, "Failed to update your profile.", Toast.LENGTH_SHORT).show();
                });
    }

    private void toggleEditMode(boolean enabled) {
        int textColor;

        // Check mode
        if (enabled) {
            textColor = getResources().getColor(R.color.black);
            profileBinding.appBarProfile.getMenu().clear(); // Remove edit button in menu actions
            profileBinding.txtfieldDateofbirth.setEndIconVisible(originalDateOfBirth != null);  // Show clear date button if dateOfBirth is set
            profileBinding.layoutEditProfileButtons.setVisibility(View.VISIBLE);    // Show edit profile action buttons
        } else {
            textColor = getResources().getColor(R.color.gray_400);
            profileBinding.appBarProfile.inflateMenu(R.menu.top_app_bar_user_profile);  // Show edit button in menu actions
            profileBinding.txtfieldDateofbirth.setEndIconVisible(false);    // Hide clear date button
            profileBinding.layoutEditProfileButtons.setVisibility(View.GONE);   // Hide edit profile action buttons
            // Clear errors
            profileBinding.txtfieldDisplayName.setErrorEnabled(false);
            profileBinding.txtfieldGender.setErrorEnabled(false);
            profileBinding.txtfieldDateofbirth.setErrorEnabled(false);
            profileBinding.txtfieldEmail.setErrorEnabled(false);
        }

        // Toggle fields
        profileBinding.txtfieldDisplayName.setCounterEnabled(enabled);
        profileBinding.txtfieldDisplayName.setEnabled(enabled);
        profileBinding.txtfieldGender.setEnabled(enabled);
        profileBinding.txtfieldDateofbirth.setEnabled(enabled);
        profileBinding.txtfieldEmail.setEnabled(enabled);
        profileBinding.edtDisplayName.setTextColor(textColor);
        profileBinding.edmGender.setTextColor(textColor);
        profileBinding.edtDateofbirth.setTextColor(textColor);
        profileBinding.edtEmail.setTextColor(textColor);
    }

    private void discardChanges() {
        SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy", Locale.getDefault());

        toggleEditMode(false);

        // Reset fields to original values
        profileBinding.edtDisplayName.setText(originalDisplayName);
        profileBinding.edmGender.setText(originalGender, false);
        profileBinding.edtDateofbirth.setText(originalDateOfBirth == null ? getString(R.string.not_set) : sdf.format(originalDateOfBirth.toDate()));
        selectedDateOfBirth = null;
        profileBinding.edtEmail.setText(originalEmail);
    }
}
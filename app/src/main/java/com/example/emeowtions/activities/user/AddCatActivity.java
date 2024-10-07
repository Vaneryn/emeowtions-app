package com.example.emeowtions.activities.user;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;

import com.example.emeowtions.R;
import com.example.emeowtions.databinding.ActivityAddCatBinding;
import com.example.emeowtions.models.Cat;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Date;

import javax.annotation.Nonnegative;

public class AddCatActivity extends AppCompatActivity {

    private static final String TAG = "AddCatActivity";
    private ActivityAddCatBinding addCatBinding;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private CollectionReference catsRef;

    private Date selectedDateOfBirth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get ViewBinding and set content view
        addCatBinding = ActivityAddCatBinding.inflate(getLayoutInflater());
        setContentView(addCatBinding.getRoot());

        // Initialize Firebase service instances
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        // Initialize Firestore references
        catsRef = db.collection("cats");

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

        // Load default dropdown menu selections
        addCatBinding.edmGender.setText(getString(R.string.unspecified), false);
        addCatBinding.edmBreed.setText(getString(R.string.unspecified), false);
        addCatBinding.edmBackground.setText(getString(R.string.unspecified), false);
        addCatBinding.edmMedicalConditions.setText(getString(R.string.unspecified), false);
        //endregion

        //region onClick listeners
        // appBarAddCat back button: return to Add Cat screen
        addCatBinding.appBarAddCat.setNavigationOnClickListener(view -> finish());

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

        // btnCancelAddCat: return to Add Cat screen
        addCatBinding.btnCancelAddCat.setOnClickListener(view -> finish());

        // btnConfirmAddCat: add new cat
        addCatBinding.btnConfirmAddCat.setOnClickListener(view -> {
            // Get inputs
            String name = addCatBinding.edtName.getText().toString();
            String description = addCatBinding.edtDescription.getText().toString();
            Timestamp dateOfBirth = selectedDateOfBirth == null ? null : new Timestamp(selectedDateOfBirth);
            String gender = addCatBinding.edmGender.getText().toString();
            String breed = addCatBinding.edmBreed.getText().toString();
            String background = addCatBinding.edmBackground.getText().toString();
            boolean hasMedicalConditions = addCatBinding.edmMedicalConditions.getText().toString().equals("Yes");

            // Validate inputs
            boolean isValid = validateInputs(name);

            // Create new cat if inputs are valid
            if (isValid) {
                createCat(null, name, description, dateOfBirth, gender, breed, background, hasMedicalConditions);
                finish();
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
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseAuthUtils firebaseAuthUtils = new FirebaseAuthUtils();
        firebaseAuthUtils.checkSignedIn(this);
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

    private void createCat(String profilePicture, String name, String description, Timestamp dateOfBirth, String gender, String breed, String background, boolean hasMedicalConditions) {
        FirebaseAuthUtils firebaseAuthUtils = new FirebaseAuthUtils();
        firebaseAuthUtils.checkSignedIn(this);

        Cat newCat = new Cat(
                name,
                gender,
                breed,
                dateOfBirth,
                background,
                hasMedicalConditions,
                description,
                profilePicture,
                firebaseAuthUtils.getUid(),
                Timestamp.now(),
                Timestamp.now()
        );

        catsRef.add(newCat)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Successfully added new cat (" + name + ")." , Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding Cat document", e);
                    Toast.makeText(this, "Unable to create new cat, please try again later.", Toast.LENGTH_SHORT).show();
                });
    }
}
package com.example.emeowtions.activities.common;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.emeowtions.R;
import com.example.emeowtions.databinding.ActivityRegisterClinicSuccessBinding;

public class RegisterClinicSuccessActivity extends AppCompatActivity {

    private static final String TAG = "RegisterClinicSuccessActivity";
    private ActivityRegisterClinicSuccessBinding regSuccessBinding;

    // Private variables
    private String vetRegId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get Intent data
        Intent passedIntent = getIntent();
        vetRegId = passedIntent.getStringExtra(RegisterClinicActivity.KEY_VET_REG_ID);

        // Get ViewBinding and set content view
        regSuccessBinding = ActivityRegisterClinicSuccessBinding.inflate(getLayoutInflater());
        setContentView(regSuccessBinding.getRoot());

        // Enable edge-to-edge layout
        EdgeToEdge.enable(this);
        // Adjust main layout padding based on system bar size to prevent obstruction
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //region onClick Listeners
        // btnSeeStatus: redirect to ClinicRegistrationStatusActivity
        regSuccessBinding.btnSeeStatus.setOnClickListener(view -> {
            Intent intent = new Intent(this, ClinicRegistrationStatusActivity.class);
            intent.putExtra(RegisterClinicActivity.KEY_VET_REG_ID, vetRegId);   // Pass registration ID to be used for status checking
            startActivity(intent);
        });

        // btnBack: return to previous screen
        regSuccessBinding.btnBack.setOnClickListener(view -> {
            finish();
        });
        //endregion
    }
}
package com.example.emeowtions.activities.user;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.emeowtions.R;
import com.example.emeowtions.databinding.ActivityAnalysisHistoryBinding;

public class AnalysisHistoryActivity extends AppCompatActivity {

    private static final String TAG = "AnalysisHistoryActivity";
    private ActivityAnalysisHistoryBinding analysisHistoryBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get ViewBinding and set content view
        analysisHistoryBinding = ActivityAnalysisHistoryBinding.inflate(getLayoutInflater());
        setContentView(analysisHistoryBinding.getRoot());

        // Enable edge-to-edge layout
        EdgeToEdge.enable(this);
        // Adjust main layout padding based on system bar size to prevent obstruction
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //region onClick listeners
        analysisHistoryBinding.appBarAnalysisHistory.setNavigationOnClickListener(view -> finish());
        //endregion
    }
}
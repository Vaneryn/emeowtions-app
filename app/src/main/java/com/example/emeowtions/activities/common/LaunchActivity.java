package com.example.emeowtions.activities.common;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.example.emeowtions.activities.admin.AdminMainActivity;
import com.example.emeowtions.activities.user.UserMainActivity;
import com.example.emeowtions.activities.veterinary.VetMainActivity;

public class LaunchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Handle splash screen transition
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);

        // Check if the user is authenticated
        boolean isAuthenticated = false;

        if (!isAuthenticated) {
            // Redirect to LoginActivity if not authenticated
            startActivity(new Intent(this, LoginActivity.class));
        } else {
            // Get user role
            String userRole = "user";

            // Redirect to appropriate MainActivity based on role
            Intent redirectRoleIntent;

            switch (userRole) {
                case "user":
                    redirectRoleIntent = new Intent(this, UserMainActivity.class);
                    break;
                case "veterinary_staff":
                    redirectRoleIntent = new Intent(this, VetMainActivity.class);
                    break;
                case "admin":
                    redirectRoleIntent = new Intent(this, AdminMainActivity.class);
                    break;
                default:
                    // Handle unknown roles or errors
                    redirectRoleIntent = new Intent(this, LoginActivity.class);
                    break;
            }

            startActivity(redirectRoleIntent);
        }

        finish();
    }
}
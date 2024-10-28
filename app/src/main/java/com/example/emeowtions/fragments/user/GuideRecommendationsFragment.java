package com.example.emeowtions.fragments.user;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.emeowtions.R;
import com.example.emeowtions.databinding.FragmentGuideRecommendationsBinding;

public class GuideRecommendationsFragment extends Fragment {

    private static final String TAG = "GuideRecommendationsFragment";

    // Layout variables
    private FragmentGuideRecommendationsBinding binding;

    public GuideRecommendationsFragment() {
        super(R.layout.fragment_guide_recommendations);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentGuideRecommendationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
}
package com.example.emeowtions.fragments.user;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.emeowtions.R;
import com.example.emeowtions.databinding.FragmentGuideEmotionAnalysisBinding;

public class GuideEmotionAnalysisFragment extends Fragment {

    private static final String TAG = "GuideEmotionAnalysisFragment";

    // Layout variables
    private FragmentGuideEmotionAnalysisBinding binding;

    public GuideEmotionAnalysisFragment() {
        super(R.layout.fragment_guide_emotion_analysis);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentGuideEmotionAnalysisBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
}
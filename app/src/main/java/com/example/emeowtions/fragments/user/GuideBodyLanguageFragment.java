package com.example.emeowtions.fragments.user;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.example.emeowtions.R;
import com.example.emeowtions.databinding.FragmentGuideBodyLanguageBinding;

public class GuideBodyLanguageFragment extends Fragment {

    private static final String TAG = "GuideBodyLanguageFragment";

    // Layout variables
    private FragmentGuideBodyLanguageBinding binding;

    public GuideBodyLanguageFragment() {
        super(R.layout.fragment_guide_body_language);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentGuideBodyLanguageBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadData();
    }

    private void loadData() {
        // Load stock images into body language image views
        // Facial Expressions
        loadImage("https://as2.ftcdn.net/v2/jpg/02/65/24/31/1000_F_265243179_LUgED0G5wyhhiHUFmf8rkl5s7gGHVROh.jpg", binding.imgEarsFlat);
        loadImage("https://png.pngtree.com/background/20230519/original/pngtree-cat-with-striped-ears-is-looking-up-at-something-in-the-picture-image_2652355.jpg", binding.imgEarsUp);
        loadImage("https://cdn.shopify.com/s/files/1/0311/9598/8101/files/scared-kitten-with-dilated-pupils_480x480.jpg?v=1638283147", binding.imgLargePupils);
        loadImage("https://d2zp5xs5cp8zlg.cloudfront.net/image-35009-800.jpg", binding.imgSmallPupils);
        loadImage("https://icatcare.org/app/uploads/2020/05/joppe-spaa-I5LTVH2mkrA-unsplash-1200x600.jpg", binding.imgEyesNarrowed);
        loadImage("https://lh6.googleusercontent.com/proxy/K6MTOrYbvO3m5kG9BlbNHgwZW6x66lHfNfHRWlZMzM9IuQx3ZtY8Z3OI2e_oQ1AuwXL3d16yiP5XO2pOY2Q6NVwL830hY6wzY-Hy4SMuiqHi0CxTI-0A", binding.imgMouthFangs);

        // Posture
        loadImage("https://cdn-fastly.petguide.com/media/2022/02/16/8259958/what-does-a-cats-arched-back-mean.jpg?size=1200x628", binding.imgPostureArchedBack);
        loadImage("https://external-preview.redd.it/gfNWJWCMW6IjybGyxRxKInu11Bz4w-E8KKYmHqCY0o0.jpg?width=1080&crop=smart&auto=webp&s=75071465f73003334501e6f203ddf79d42d95e1d", binding.imgPostureExposedBelly);
        loadImage("https://media.istockphoto.com/id/1443562748/photo/cute-ginger-cat.jpg?s=612x612&w=0&k=20&c=vvM97wWz-hMj7DLzfpYRmY2VswTqcFEKkC437hxm3Cg=", binding.imgPostureNeutral);
        loadImage("https://images.squarespace-cdn.com/content/v1/5d5208bebb45570001e085c7/54276227-4560-48a6-8ed8-c8b41673120c/scared-cat-Ringwood-Vet-Clinic.jpg", binding.imgPostureSmall);
        loadImage("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQVW_TKSuBcOldoHkOGBv4F_BXCxd6_Z1gmJw&s", binding.imgPostureStretch);

        // Key Body Parts
        loadImage("https://bowwowinsurance.com.au/wp-content/uploads/2021/05/shutterstock_1048661324-Ginger-Maine-Coon-Cat-Standing-in-Pose-with-down-tail.jpg", binding.imgTailNeutral);
        loadImage("https://thumbs.dreamstime.com/b/scared-cat-10216650.jpg", binding.imgTailTucked);
        loadImage("https://thevillagevets.com/wp-content/uploads/2023/05/cats-tail-shaped-like-question-mark.jpg", binding.imgTailUp);
    }

    private void loadImage(String url, ImageView imageView) {
        Glide.with(getContext().getApplicationContext()).load(url).into(imageView);
    }
}
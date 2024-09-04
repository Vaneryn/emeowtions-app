package com.example.emeowtions.fragments;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.hardware.camera2.CameraCaptureSession;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.emeowtions.ObjectDetectorHelper;
import com.example.emeowtions.R;
import com.example.emeowtions.databinding.FragmentEmotionBinding;
import com.google.common.util.concurrent.ListenableFuture;

import org.tensorflow.lite.task.vision.detector.Detection;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EmotionFragment extends Fragment implements ObjectDetectorHelper.DetectorListener {

    private static final String TAG = "ObjectDetection";

    private FragmentEmotionBinding fragmentEmotionBinding;

    private ObjectDetectorHelper objectDetectorHelper;
    private Bitmap bitmapBuffer;
    private Preview preview;
    private ImageAnalysis imageAnalyzer;
    private Camera camera;
    private ProcessCameraProvider cameraProvider;

    private ExecutorService cameraExecutor;

    public EmotionFragment() {
        super(R.layout.fragment_emotion);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        fragmentEmotionBinding = FragmentEmotionBinding.inflate(inflater, container, false);
        return fragmentEmotionBinding.getRoot();
    }

    @Override
    @SuppressLint("MissingPermission")
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        objectDetectorHelper = new ObjectDetectorHelper(requireContext(), this);

        // Initialize background executor
        cameraExecutor = Executors.newSingleThreadExecutor();

        fragmentEmotionBinding.viewFinder.post(() -> setUpCamera());

        //initBottomSheetControls();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Make sure all permissions are still present
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();

            fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, new PermissionsFragment())
                    .commit();
        }
    }

    @Override
    public void onDestroyView() {
        fragmentEmotionBinding = null;
        super.onDestroyView();

        // Shut down background executor
        cameraExecutor.shutdown();
    }

    private void updateControlsUi() {
        objectDetectorHelper.clearObjectDetector();
    }

    private void setUpCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    cameraProvider = cameraProviderFuture.get();
                    bindCameraUseCases();
                } catch (Exception e) {
                    Log.e(TAG, "Camera initialization failed", e);
                }
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void bindCameraUseCases() {
        ProcessCameraProvider cameraProvider = this.cameraProvider;
        if (cameraProvider == null) {
            throw new IllegalStateException("Camera initialization failed.");
        }

        // Use back camera
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        // Build Preview
        preview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentEmotionBinding.viewFinder.getDisplay().getRotation())
                .build();

        // Build ImageAnalyzer
        imageAnalyzer = new ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentEmotionBinding.viewFinder.getDisplay().getRotation())
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build();

        imageAnalyzer.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {
                if (bitmapBuffer == null) {
                    bitmapBuffer = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
                }

                detectObjects(image);
            }
        });

        cameraProvider.unbindAll();

        try {
            // A variety of use cases can be passed here
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer);

            // Attach viewfinder's surface provider to preview use case
            if (preview != null) {
                preview.setSurfaceProvider(fragmentEmotionBinding.viewFinder.getSurfaceProvider());
            }
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }
    }

    private void detectObjects(@NonNull ImageProxy image) {
        try {
            // Copy RGB bits out to the shared bitmap buffer
            if (bitmapBuffer == null) {
                bitmapBuffer = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
            }

            bitmapBuffer.copyPixelsFromBuffer(image.getPlanes()[0].getBuffer());
            int imageRotation = image.getImageInfo().getRotationDegrees();

            // Pass Bitmap and rotation to the object detector helper for processing and detection
            objectDetectorHelper.detect(bitmapBuffer, imageRotation);
        } finally {
            image.close();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (imageAnalyzer != null) {
            imageAnalyzer.setTargetRotation(fragmentEmotionBinding.viewFinder.getDisplay().getRotation());
        }
    }

    @Override
    public void onError(String error) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public void onResults(List<Detection> results, long inferenceTime, int imageHeight, int imageWidth) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                // todo
            });
        }
    }
}
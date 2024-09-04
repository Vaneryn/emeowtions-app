package com.example.emeowtions.fragments;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.emeowtions.BoundingBox;
import com.example.emeowtions.ObjectDetector;
import com.example.emeowtions.R;
import com.example.emeowtions.databinding.FragmentEmotionBinding;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EmotionFragment extends Fragment implements ObjectDetector.DetectorListener {

    private FragmentEmotionBinding fragmentEmotionBinding;
    private boolean isFrontCamera = false;

    private Preview preview;
    private ImageAnalysis imageAnalyzer;
    private Camera camera;
    private ProcessCameraProvider cameraProvider;
    private ObjectDetector objectDetector;

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

        // Initialize background executor
        cameraExecutor = Executors.newSingleThreadExecutor();

        cameraExecutor.execute(() -> objectDetector = new ObjectDetector(requireContext(), MODEL_PATH, LABELS_PATH, this));

        if (PermissionsFragment.hasPermissions(requireContext())) {
            startCamera();
        } else {
            FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, new PermissionsFragment())
                    .commit();
        }

        bindListeners();
    }

    private void bindListeners() {
        // TODO
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void bindCameraUseCases() {
        if (cameraProvider == null) {
            throw new IllegalStateException("Camera initialization failed.");
        }

        int rotation = fragmentEmotionBinding.viewFinder.getDisplay().getRotation();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(rotation)
                .build();

        imageAnalyzer = new ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetRotation(fragmentEmotionBinding.viewFinder.getDisplay().getRotation())
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build();

        imageAnalyzer.setAnalyzer(cameraExecutor, imageProxy -> {
            Bitmap bitmapBuffer = Bitmap.createBitmap(
                    imageProxy.getWidth(),
                    imageProxy.getHeight(),
                    Bitmap.Config.ARGB_8888
            );

            try {
                bitmapBuffer.copyPixelsFromBuffer(imageProxy.getPlanes()[0].getBuffer());
            } finally {
                imageProxy.close();
            }

            Matrix matrix = new Matrix();
            matrix.postRotate(imageProxy.getImageInfo().getRotationDegrees());

            if (isFrontCamera) {
                matrix.postScale(-1f, 1f, imageProxy.getWidth(), imageProxy.getHeight());
            }

            Bitmap rotatedBitmap = Bitmap.createBitmap(
                    bitmapBuffer, 0, 0, bitmapBuffer.getWidth(), bitmapBuffer.getHeight(),
                    matrix, true
            );

            objectDetector.detect(rotatedBitmap);
        });

        cameraProvider.unbindAll();

        try {
            camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalyzer
            );

            preview.setSurfaceProvider(fragmentEmotionBinding.viewFinder.getSurfaceProvider());;
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }
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
    public void onDestroy() {
        super.onDestroy();
        objectDetector.close();
        cameraExecutor.shutdown();
    }

    @Override
    public void onEmptyDetect() {
        getActivity().runOnUiThread(() -> {
            fragmentEmotionBinding.overlay.clear();
        });
    }

    @Override
    public void onDetect(List<BoundingBox> boundingBoxes, long inferenceTime) {
        getActivity().runOnUiThread(() -> {
            fragmentEmotionBinding.inferenceTime.setText(inferenceTime + "ms");
            fragmentEmotionBinding.overlay.setResults(boundingBoxes);
            fragmentEmotionBinding.overlay.invalidate();
        });
    }

    // Constants
    private static final String TAG = "EmotionFragment";
    private static final String MODEL_PATH = "yolov8n.tflite";
    private static final String LABELS_PATH = "yolov8n_labels.txt";
}
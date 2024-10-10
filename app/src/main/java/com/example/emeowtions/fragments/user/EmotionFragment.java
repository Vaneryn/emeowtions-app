package com.example.emeowtions.fragments.user;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.emeowtions.activities.user.EmotionAnalysisActivity;
import com.example.emeowtions.utils.BoundingBox;
import com.example.emeowtions.utils.EmotionClassifier;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.example.emeowtions.utils.ObjectDetector;
import com.example.emeowtions.R;
import com.example.emeowtions.databinding.FragmentEmotionBinding;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EmotionFragment extends Fragment implements ObjectDetector.DetectorListener {

    private FragmentEmotionBinding emotionBinding;

    private FirebaseAuthUtils firebaseAuthUtils;
    private FirebaseFirestore db;
    private CollectionReference catsRef;

    // Mode Control
    private boolean isUploadMode;
    private boolean isCameraMode;

    // Upload Mode Variables
    private boolean isImageUploaded;
    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia;

    // Camera Mode + ObjectDetector Variables
    private boolean isFrontCamera = false;
    private Preview preview;
    private ImageAnalysis imageAnalyzer;
    private Camera camera;
    private ProcessCameraProvider cameraProvider;
    private ObjectDetector objectDetector;
    private ExecutorService cameraExecutor;

    // EmotionClassifier Variables
    EmotionClassifier emotionClassifier;
    private ArrayList<String> predictedLabels;
    private boolean isCatDetected;

    public EmotionFragment() {
        super(R.layout.fragment_emotion);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        emotionBinding = FragmentEmotionBinding.inflate(inflater, container, false);
        return emotionBinding.getRoot();
    }

    @Override
    @SuppressLint("MissingPermission")
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase service instances
        firebaseAuthUtils = new FirebaseAuthUtils();
        db = FirebaseFirestore.getInstance();
        // Initialize Firestore references
        catsRef = db.collection("cats");

        //region UI Setups
        // Default mode
        isUploadMode = true;
        isCameraMode = !isUploadMode;
        isImageUploaded = false;
        isCatDetected = false;

        // Load default dropdown menu selections
        emotionBinding.edmMode.setText(getResources().getStringArray(R.array.emotion_analysis_mode_items)[0], false);
        emotionBinding.edmSelectedCat.setText(getString(R.string.unspecified), false);

        // Start emotion classifier, object detector, and camera executor
        emotionClassifier = new EmotionClassifier(requireContext(), EMEOWTIONS_MODEL_PATH, EMEOWTIONS_LABELS_PATH);
        toggleCameraExecutor(true);

        // Create photo picker
        pickMedia =
                registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                    if (uri != null) {
                        emotionBinding.imgUploadView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        Glide.with(getActivity().getApplicationContext())
                                .load(uri)
                                .into(emotionBinding.imgUploadView);
                        isImageUploaded = true;
                    } else {
                        Log.d(TAG, "PhotoPicker - No media selected");
                    }
                });
        //endregion

        // Listeners
        bindListeners();
    }

    private void bindListeners() {
        // TODO

        // edmMode: switch mode
        emotionBinding.edmMode.setOnItemClickListener((adapterView, view, i, l) -> {
            String selectedMode = adapterView.getItemAtPosition(i).toString();

            if (selectedMode.equals(getResources().getStringArray(R.array.emotion_analysis_mode_items)[0])) {
                // Upload Mode
                isUploadMode = true;
                isCameraMode = !isUploadMode;
                emotionBinding.layoutUploadMode.setVisibility(View.VISIBLE);
                emotionBinding.layoutCameraMode.setVisibility(View.GONE);
            } else {
                // Camera Mode
                isCameraMode = true;
                isUploadMode = !isCameraMode;
                emotionBinding.layoutCameraMode.setVisibility(View.VISIBLE);
                emotionBinding.layoutUploadMode.setVisibility(View.GONE);
            }
        });

        // btnClear: clear image
        emotionBinding.btnClear.setOnClickListener(view -> {
            emotionBinding.imgUploadView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            emotionBinding.imgUploadView.setImageDrawable(AppCompatResources.getDrawable(getActivity().getApplicationContext(), R.drawable.baseline_emeowtions_24));
            isImageUploaded = false;
        });

        // btnUpload: upload image
        emotionBinding.btnUpload.setOnClickListener(view -> {
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        // btnGenerateAnalysis: generate analysis report
        emotionBinding.btnGenerateAnalysis.setOnClickListener(view -> {
            if (isCatDetected) {
                Intent intent = new Intent(getContext(), EmotionAnalysisActivity.class);
                intent.putExtra(KEY_CAT_ID, "");
                intent.putStringArrayListExtra(KEY_PREDICTED_LABELS, predictedLabels);
                startActivity(intent);
            } else {
                Toast.makeText(getContext(), "No cat detected. Unable to generate analysis.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleCameraExecutor(boolean enabled) {
        if (enabled) {
            // Initialize background camera executor and object detector
            cameraExecutor = Executors.newSingleThreadExecutor();

            cameraExecutor.execute(() -> objectDetector = new ObjectDetector(requireContext(), YOLOV8N_MODEL_PATH, YOLOV8N_LABELS_PATH, this));

            if (CameraPermissionsFragment.hasPermissions(requireContext())) {
                startCamera();
            } else {
                FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
                fragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, new CameraPermissionsFragment())
                        .commit();
            }
        } else {
            // Shut down object detector and camera executor
            cameraProvider.unbindAll();

            if (objectDetector != null)
                objectDetector.close();

            if (cameraExecutor != null && !cameraExecutor.isShutdown())
                cameraExecutor.shutdownNow();
        }
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

        int rotation = emotionBinding.viewFinder.getDisplay().getRotation();

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
                .setTargetRotation(emotionBinding.viewFinder.getDisplay().getRotation())
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

            preview.setSurfaceProvider(emotionBinding.viewFinder.getSurfaceProvider());;
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Make sure all permissions are still present
        if (!CameraPermissionsFragment.hasPermissions(requireContext())) {
            FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, new CameraPermissionsFragment())
                    .commit();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (emotionClassifier != null)
            emotionClassifier.close();

        toggleCameraExecutor(false);
    }

    @Override
    public void onEmptyDetect() {
        getActivity().runOnUiThread(() -> {
            toggleDetectionText(true);
            emotionBinding.overlay.clear();
            isCatDetected = false;
            emotionBinding.btnGenerateAnalysis.setEnabled(false);
        });
    }

    @Override
    public void onDetect(List<BoundingBox> boundingBoxes, long inferenceTime) {
        getActivity().runOnUiThread(() -> {
            // Set detection results in overlay
            toggleDetectionText(false);
            emotionBinding.inferenceTime.setText(inferenceTime + "ms");
            emotionBinding.overlay.setResults(boundingBoxes);
            emotionBinding.overlay.invalidate();

            // Classify cat emotion and body langauge
            for (BoundingBox boundingBox : boundingBoxes) {
                Bitmap sourceImage = emotionBinding.viewFinder.getBitmap();

                if (sourceImage != null) {
                    Bitmap croppedBitmap = cropBitmap(sourceImage, boundingBox);
                    predictedLabels = emotionClassifier.predict(croppedBitmap);
                }
            }

            isCatDetected = true;
            emotionBinding.btnGenerateAnalysis.setEnabled(true);
        });
    }

    private Bitmap cropBitmap(Bitmap sourceBitmap, BoundingBox boundingBox) {
        // Get the width and height of the source image
        int imageWidth = sourceBitmap.getWidth();
        int imageHeight = sourceBitmap.getHeight();

        // Convert normalized float coordinates to pixel values
        int left = (int) (boundingBox.getX1() * imageWidth);
        int top = (int) (boundingBox.getY1() * imageHeight);
        int right = (int) (boundingBox.getX2() * imageWidth);
        int bottom = (int) (boundingBox.getY2() * imageHeight);

        // Ensure the crop coordinates are within the image bounds
        left = Math.max(0, left);
        top = Math.max(0, top);
        right = Math.min(imageWidth, right);
        bottom = Math.min(imageHeight, bottom);

        // Calculate the width and height of the cropped area
        int cropWidth = right - left;
        int cropHeight = bottom - top;

        // Create and return the cropped bitmap
        return Bitmap.createBitmap(sourceBitmap, left, top, cropWidth, cropHeight);
    }

    // Toggles detection overlay text
    private void toggleDetectionText(boolean enabled) {
        if (enabled) {
            emotionBinding.inferenceTime.setVisibility(View.VISIBLE);
            emotionBinding.txtDetection.setVisibility(View.VISIBLE);
        } else {
            emotionBinding.inferenceTime.setVisibility(View.GONE);
            emotionBinding.txtDetection.setVisibility(View.GONE);
        }
    }

    // Constants
    private static final String TAG = "EmotionFragment";
    public static final String KEY_CAT_ID = "catId";
    public static final String KEY_PREDICTED_LABELS = "predictedLabels";
    private static final String YOLOV8N_MODEL_PATH = "yolov8n.tflite";
    private static final String YOLOV8N_LABELS_PATH = "yolov8n_labels.txt";
    private static final String EMEOWTIONS_MODEL_PATH = "emeowtions.tflite";
    private static final String EMEOWTIONS_LABELS_PATH = "emeowtions_labels.txt";
}
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
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.emeowtions.activities.user.EmotionAnalysisActivity;
import com.example.emeowtions.models.Cat;
import com.example.emeowtions.utils.BoundingBox;
import com.example.emeowtions.utils.EmotionClassifier;
import com.example.emeowtions.utils.EmotionUtils;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.example.emeowtions.utils.ObjectDetector;
import com.example.emeowtions.R;
import com.example.emeowtions.databinding.FragmentEmotionBinding;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EmotionFragment extends Fragment implements ObjectDetector.DetectorListener {

    // Firebase variables
    private FirebaseAuthUtils firebaseAuthUtils;
    private FirebaseFirestore db;
    private CollectionReference catsRef;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private StorageReference tempImageRef;

    // Layout variables
    private FragmentEmotionBinding binding;

    // Private variables
    private int selectedCatIndex;
    private List<String> catIdList;
    private List<String> catNameList;

    // Mode Control
    private boolean isUploadMode;
    private boolean isCameraMode;

    // Upload Mode Variables
    private boolean isImageUploaded;
    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia;
    private Bitmap uploadedImage;

    // Camera Mode + ObjectDetector Variables
    private boolean isFrontCamera = false;
    private Preview preview;
    private ImageAnalysis imageAnalyzer;
    private Camera camera;
    private ProcessCameraProvider cameraProvider;
    private ObjectDetector objectDetector;
    private ExecutorService cameraExecutor;
    private Bitmap detectedCatImage;

    // EmotionClassifier Variables
    EmotionClassifier emotionClassifier;
    private HashMap<String, Float> predictedLabels;
    private boolean isCatDetected;

    // Speed test
    private int totalInferenceCount = 0;
    private long objectDetectorTotalInferenceTime = 0;
    private long classifierTotalInferenceTime = 0;

    public EmotionFragment() {
        super(R.layout.fragment_emotion);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEmotionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    @SuppressLint("MissingPermission")
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase service instances
        firebaseAuthUtils = new FirebaseAuthUtils();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        // Initialize Firestore references
        catsRef = db.collection("cats");
        // Initialize Storage references
        storageRef = storage.getReference();
        tempImageRef = storageRef.child( "images/users/" + firebaseAuthUtils.getUid() + "/temp_detected_cat_image.jpg");

        //region UI Setups
        // Default mode
        isUploadMode = true;
        isCameraMode = false;
        isImageUploaded = false;
        isCatDetected = false;

        // Load dropdown menus
        binding.edmMode.setText(getResources().getStringArray(R.array.emotion_analysis_mode_items)[0], false);

        loadCats(null);
        catsRef.whereEqualTo("ownerId", firebaseAuthUtils.getUid())
                .whereEqualTo("deleted", false)
                .addSnapshotListener((values, error) -> {
                    // Error
                    if (error != null) {
                        Log.w(TAG, "onViewCreated: Failed to listen on Cat changes", error);
                        return;
                    }
                    // Success
                    if (values != null && !values.getDocuments().isEmpty()) {
                        loadCats(values.getDocuments());
                    }
                });

        // Start emotion classifier
        emotionClassifier = new EmotionClassifier(requireContext(), EMEOWTIONS_MODEL_PATH, EMEOWTIONS_LABELS_PATH);

        // Create photo picker
        pickMedia =
                registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                    if (uri != null) {
                        binding.imgUploadView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        Glide.with(getActivity().getApplicationContext())
                                .load(uri)
                                .into(binding.imgUploadView);
                        isImageUploaded = true;

                        try {
                            uploadedImage = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), uri);
                            detectCatInImage(uploadedImage);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        Log.d(TAG, "PhotoPicker - No media selected");
                    }
                });
        //endregion

        // Listeners
        bindListeners();
    }

    private void bindListeners() {
        // edmMode: switch mode
        binding.edmMode.setOnItemClickListener((adapterView, view, i, l) -> {
            String selectedMode = adapterView.getItemAtPosition(i).toString();

            if (isCameraMode && selectedMode.equals(getResources().getStringArray(R.array.emotion_analysis_mode_items)[0])) {
                // Upload Mode
                isUploadMode = true;
                isCameraMode = false;
                binding.txtUploadDetection.setVisibility(View.GONE);
                binding.layoutUploadMode.setVisibility(View.VISIBLE);
                binding.layoutCameraMode.setVisibility(View.GONE);
                toggleCameraExecutor(false);

                // Clear
                detectedCatImage = null;
                binding.imgUploadView.setImageDrawable(AppCompatResources.getDrawable(getContext(), R.drawable.baseline_emeowtions_24));
                binding.btnGenerateAnalysis.setEnabled(false);
            } else if (isUploadMode && selectedMode.equals(getResources().getStringArray(R.array.emotion_analysis_mode_items)[1])) {
                // Camera Mode
                isCameraMode = true;
                isUploadMode = false;
                binding.layoutCameraMode.setVisibility(View.VISIBLE);
                binding.layoutUploadMode.setVisibility(View.GONE);
                toggleCameraExecutor(true);

                // Clear
                detectedCatImage = null;
                binding.imgUploadView.setImageDrawable(AppCompatResources.getDrawable(getContext(), R.drawable.baseline_emeowtions_24));
                binding.btnGenerateAnalysis.setEnabled(false);
            }
        });

        // edmSelectedCat: change selected cat
        binding.edmSelectedCat.setOnItemClickListener((adapterView, view, i, l) -> {
            selectedCatIndex = i;
        });

        // btnClear: clear image
        binding.btnClear.setOnClickListener(view -> {
            isImageUploaded = false;
            isCatDetected = false;

            toggleDetectionText(false);
            toggleEmotionText(false);
            binding.uploadOverlay.clear();
            binding.uploadOverlay.invalidate();
            binding.imgUploadView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            binding.imgUploadView.setImageDrawable(AppCompatResources.getDrawable(getActivity().getApplicationContext(), R.drawable.baseline_emeowtions_24));
            binding.btnGenerateAnalysis.setEnabled(false);
        });

        // btnUpload: upload image
        binding.btnUpload.setOnClickListener(view -> {
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        // btnGenerateAnalysis: generate analysis report
        binding.btnGenerateAnalysis.setOnClickListener(view -> {
            generateAnalysis();
        });
    }

    private void detectCatInImage(Bitmap image) {
        ObjectDetector detector = new ObjectDetector(requireContext(), YOLOV8N_MODEL_PATH, YOLOV8N_LABELS_PATH, this);
        detector.detect(image);
        detector.close();
    }

    private void generateAnalysis() {
        if (isCatDetected) {
            // Temporarily store detected cat image in Firebase storage
            byte[] imageData = null;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            detectedCatImage.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            imageData = baos.toByteArray();

            tempImageRef.putBytes(imageData)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }
                        // Continue with the task to get the download URL
                        return tempImageRef.getDownloadUrl();
                    })
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Pass catId (if cat selected) and temp image URL
                            Intent intent = new Intent(getContext(), EmotionAnalysisActivity.class);
                            intent.putExtra(KEY_CAT_ID, catIdList.get(selectedCatIndex));
                            intent.putExtra(KEY_TEMP_CAT_IMAGE_URL, task.getResult().toString());
                            intent.putExtra(KEY_PREDICTED_LABELS, predictedLabels);
                            startActivity(intent);
                        }
                    });
        } else {
            Toast.makeText(getContext(), "No cat detected. Unable to generate analysis.", Toast.LENGTH_SHORT).show();
        }
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
                        .replace(R.id.userScreenFragmentContainer, new CameraPermissionsFragment())
                        .commit();
            }
        } else {
            // Shut down object detector and camera executor
            if (cameraProvider != null)
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

        int rotation = binding.viewFinder.getDisplay().getRotation();

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
                .setTargetRotation(binding.viewFinder.getDisplay().getRotation())
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

            preview.setSurfaceProvider(binding.viewFinder.getSurfaceProvider());;
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
                    .replace(R.id.userScreenFragmentContainer, new CameraPermissionsFragment())
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
            toggleEmotionText(false);
            isCatDetected = false;
            binding.btnGenerateAnalysis.setEnabled(false);

            if (isUploadMode) {
                binding.uploadOverlay.clear();
            } else if (isCameraMode) {
                binding.cameraOverlay.clear();
            }
        });
    }

    @Override
    public void onDetect(List<BoundingBox> boundingBoxes, long inferenceTime) {
        getActivity().runOnUiThread(() -> {
            //totalInferenceCount++;
            //objectDetectorTotalInferenceTime += inferenceTime;

            toggleDetectionText( false);
            toggleEmotionText(true);

            if (isUploadMode) {
                // Set detection results in overlay
                binding.uploadOverlay.setResults(boundingBoxes);
                binding.uploadOverlay.invalidate();

                // Classify cat emotion and body language
                for (BoundingBox boundingBox : boundingBoxes) {
                    Bitmap sourceImage = uploadedImage;
                    detectedCatImage = sourceImage;

                    if (sourceImage != null) {
                        Bitmap croppedBitmap = cropBitmap(sourceImage, boundingBox);
                        predictedLabels = emotionClassifier.predict(croppedBitmap);
                    }
                }

                // Set emotion classification result in overlay
                Pair<String, Float> detectedEmotion = EmotionUtils.getTrueEmotion(predictedLabels);
                if (detectedEmotion != null && detectedEmotion.first != null && detectedEmotion.second != null) {
                    String emotion = detectedEmotion.first;
                    binding.txtUploadEmotion.setText(String.format("%s", emotion.substring(0, 1).toUpperCase() + emotion.substring(1)));
                } else {
                    binding.txtUploadEmotion.setText(String.format("%s", "Emotion Undetectable"));
                }
            } else if (isCameraMode) {
                // Set detection results in overlay
                binding.inferenceTime.setText(inferenceTime + "ms");
                binding.cameraOverlay.setResults(boundingBoxes);
                binding.cameraOverlay.invalidate();

                // Classify cat emotion and body language
                for (BoundingBox boundingBox : boundingBoxes) {
                    Bitmap sourceImage = binding.viewFinder.getBitmap();
                    detectedCatImage = sourceImage;

                    if (sourceImage != null) {
                        Bitmap croppedBitmap = cropBitmap(sourceImage, boundingBox);

                        // TODO: Uncomment code for Speed test
                        //long classifierInferenceTime = SystemClock.uptimeMillis();
                        predictedLabels = emotionClassifier.predict(croppedBitmap);
                        //classifierInferenceTime = SystemClock.uptimeMillis() - classifierInferenceTime;
                        //classifierTotalInferenceTime += classifierInferenceTime;
//
//                        Log.d(TAG, "onDetect: totalInferenceCount=" + totalInferenceCount);
//
//                        if (totalInferenceCount == 500) {
//                            long totalInferenceTime = objectDetectorTotalInferenceTime + classifierTotalInferenceTime;
//                            Toast.makeText(getContext(), "Speed Test Done", Toast.LENGTH_SHORT).show();
//                            Log.d(TAG, "onDetect: totalInferenceTime=" + totalInferenceTime);
//                        }
                    }
                }

                // Set emotion classification result in overlay
                Pair<String, Float> detectedEmotion = EmotionUtils.getTrueEmotion(predictedLabels);
                if (detectedEmotion != null && detectedEmotion.first != null && detectedEmotion.second != null) {
                    String emotion = detectedEmotion.first;
                    binding.txtCameraEmotion.setText(String.format("%s", emotion.substring(0, 1).toUpperCase() + emotion.substring(1)));
                }  else {
                    binding.txtUploadEmotion.setText(String.format("%s", "Emotion Undetectable"));
                }
            }

            // Flag to keep track of whether cat is detected
            isCatDetected = true;

            // Enable analysis generation button
            binding.btnGenerateAnalysis.setEnabled(true);
        });
    }

    private void loadCats(List<DocumentSnapshot> documentSnapshots) {
        // Add dummy values for "Unspecified"
        catIdList = new ArrayList<>();
        catNameList = new ArrayList<>();
        catIdList.add(null);
        catNameList.add("Unspecified");

        // Add cat names to options
        if (documentSnapshots != null) {
            for (DocumentSnapshot document : documentSnapshots) {
                Cat cat = document.toObject(Cat.class);
                catIdList.add(document.getId());
                catNameList.add(cat.getName());
            }
        }

        // Set adapter
        ArrayAdapter<String> catNamesAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, catNameList);
        binding.edmSelectedCat.setAdapter(catNamesAdapter);
        binding.edmSelectedCat.setText(catNameList.get(0), false);
        selectedCatIndex = 0;
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
        if (isUploadMode) {
            if (enabled) {
                binding.txtUploadDetection.setVisibility(View.VISIBLE);
            } else {
                binding.txtUploadDetection.setVisibility(View.GONE);
            }
        } else if (isCameraMode) {
            if (enabled) {
                binding.inferenceTime.setVisibility(View.VISIBLE);
                binding.txtCameraDetection.setVisibility(View.VISIBLE);
            } else {
                binding.inferenceTime.setVisibility(View.GONE);
                binding.txtCameraDetection.setVisibility(View.GONE);
            }
        }
    }

    // Toggles emotion overlay text
    private void toggleEmotionText(boolean enabled) {
        if (isUploadMode) {
            if (enabled) {
                binding.txtUploadEmotion.setVisibility(View.VISIBLE);
            } else {
                binding.txtUploadEmotion.setVisibility(View.GONE);
            }
        } else if (isCameraMode) {
            if (enabled) {
                binding.txtCameraEmotion.setVisibility(View.VISIBLE);
            } else {
                binding.txtCameraEmotion.setVisibility(View.GONE);
            }
        }
    }

    // Constants
    private static final String TAG = "EmotionFragment";
    public static final String KEY_CAT_ID = "catId";
    public static final String KEY_TEMP_CAT_IMAGE_URL = "tempCatImageUrl";
    public static final String KEY_PREDICTED_LABELS = "predictedLabels";
    private static final String YOLOV8N_MODEL_PATH = "yolov8n.tflite";
    private static final String YOLOV8N_LABELS_PATH = "yolov8n_labels.txt";
    private static final String EMEOWTIONS_MODEL_PATH = "emeowtions.tflite";
    private static final String EMEOWTIONS_LABELS_PATH = "emeowtions_labels.txt";
}
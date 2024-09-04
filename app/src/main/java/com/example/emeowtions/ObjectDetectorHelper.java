package com.example.emeowtions;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.gpu.CompatibilityList;
import org.tensorflow.lite.support.common.ops.CastOp;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.Rot90Op;
import org.tensorflow.lite.task.core.BaseOptions;
import org.tensorflow.lite.task.vision.detector.Detection;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;

import java.io.IOException;
import java.util.List;

public class ObjectDetectorHelper {
    private float threshold = 0.5f;
    private int numThreads = 2;
    private int maxResults = 1;
    private int currentDelegate = 0;
    private int currentModel = 0;
    private Context context;
    private DetectorListener objectDetectorListener;

    private ObjectDetector objectDetector;

    // Constructor
    public ObjectDetectorHelper(float threshold, int numThreads, int maxResults, int currentDelegate, int currentModel, Context context, DetectorListener objectDetectorListener) {
        this.threshold = threshold;
        this.numThreads = numThreads;
        this.maxResults = maxResults;
        this.currentDelegate = currentDelegate;
        this.currentModel = currentModel;
        this.context = context;
        this.objectDetectorListener = objectDetectorListener;
    }

    public ObjectDetectorHelper(Context context, DetectorListener objectDetectorListener) {
        this.context = context;
        this.objectDetectorListener = objectDetectorListener;
    }

    public void clearObjectDetector() {
        objectDetector = null;
    }

    public void setupObjectDetector() {
        // Create object detector options builder with score threshold and max results
        ObjectDetector.ObjectDetectorOptions.Builder optionsBuilder =
                ObjectDetector.ObjectDetectorOptions.builder()
                        .setScoreThreshold(threshold)
                        .setMaxResults(maxResults);

        // Create base options builder with number of threads
        BaseOptions.Builder baseOptionsBuilder = BaseOptions.builder().setNumThreads(numThreads);

        // Configure hardware accelerator
        switch (currentDelegate) {
            case DELEGATE_CPU:
                // Default
                break;
            case DELEGATE_GPU:
                // Check if GPU hardware acceleration is supported
                if (new CompatibilityList().isDelegateSupportedOnThisDevice()) {
                    baseOptionsBuilder.useGpu();
                } else {
                    if (objectDetectorListener != null)
                        objectDetectorListener.onError("GPU is not supported on this device");
                }
                break;
            case DELEGATE_NNAPI:
                baseOptionsBuilder.useNnapi();
                break;
        }

        // Set detector base options
        optionsBuilder.setBaseOptions(baseOptionsBuilder.build());

        // Set model name
        String modelName = "";
        switch (currentModel) {
            case MODEL_YOLOV8N:
                modelName = "yolov8n.tflite";
                break;
        }

        // Initialize object detector
        try {
            objectDetector = ObjectDetector.createFromFileAndOptions(context, modelName, optionsBuilder.build());
        } catch (IllegalStateException e) {
            if (objectDetectorListener != null) {
                objectDetectorListener.onError("Object detector failed to initialize. See error logs for details");
            }
            Log.e("ObjectDetectorHelper", "TFLite failed to load model with error: " + e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void detect(Bitmap image, int imageRotation) {
        if (objectDetector == null) {
            setupObjectDetector();
        }

        // Difference between system time at the start and end of the detection process
        long inferenceTime = SystemClock.uptimeMillis();

        // Create preprocessor for image
        ImageProcessor imageProcessor =
                new ImageProcessor.Builder()
                        .add(new Rot90Op(-imageRotation / 90))
                        .build();

        // Preprocess image and convert it into TensorImage
        TensorImage tensorImage = imageProcessor.process(TensorImage.fromBitmap(image));

        // Get detection results
        List<Detection> results = objectDetector.detect(tensorImage);
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime;

        if (objectDetectorListener != null) {
            objectDetectorListener.onResults(results, inferenceTime, tensorImage.getHeight(), tensorImage.getWidth());
        }
    }

    public interface DetectorListener {
        void onError(String error);
        void onResults(List<Detection> results, long inferenceTime, int imageHeight, int imageWidth);
    }

    public static final int DELEGATE_CPU = 0;
    public static final int DELEGATE_GPU = 1;
    public static final int DELEGATE_NNAPI = 2;
    public static final int MODEL_YOLOV8N = 0;

    private static final float INPUT_MEAN = 127.5f;
    private static final float INPUT_STANDARD_DEVIATION = 127.5f;
    private static final DataType INPUT_IMAGE_TYPE = DataType.FLOAT32;
    private static final DataType OUTPUT_IMAGE_TYPE = DataType.FLOAT32;
}

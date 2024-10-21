package com.example.emeowtions.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.CompatibilityList;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.CastOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class EmotionClassifier {

    private Interpreter interpreter;
    private GpuDelegate gpuDelegate;

    private final List<String> LABELS;

    private DataType inputDataType;
    private DataType outputDataType;
    private int tensorWidth;
    private int tensorHeight;
    private int numClasses;

    private ImageProcessor imageProcessor;

    public EmotionClassifier(Context context, String modelPath, String labelsPath) {
        // Configure delegate hardware
        Interpreter.Options options;
        try (CompatibilityList compatibilityList = new CompatibilityList()) {
            options = new Interpreter.Options();

            if (compatibilityList.isDelegateSupportedOnThisDevice()) {
                GpuDelegate.Options delegateOptions = compatibilityList.getBestOptionsForThisDevice();
                gpuDelegate = new GpuDelegate(delegateOptions);
                options.addDelegate(gpuDelegate);
            } else {
                options.setNumThreads(NUM_THREADS);
            }
        }

        // Load model
        try {
            MappedByteBuffer model = FileUtil.loadMappedFile(context, modelPath);
            interpreter = new Interpreter(model, options);
        } catch (IOException e) {
            Log.e(TAG, "EmotionClassifier: Error occurred while loading model", e);
            throw new RuntimeException(e);
        }

        int[] inputShape = interpreter.getInputTensor(0).shape();
        int[] outputShape = interpreter.getOutputTensor(0).shape();
        inputDataType = interpreter.getInputTensor(0).dataType();
        outputDataType = interpreter.getOutputTensor(0).dataType();

        if (inputShape != null) {
            tensorWidth = inputShape[1];
            tensorHeight = inputShape[2];

            if (inputShape[1] == 3) {
                tensorWidth = inputShape[2];
                tensorHeight = inputShape[3];
            }
        }
        if (outputShape != null) {
            numClasses = outputShape[1];
        }

        // Initialize image processor
        imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(tensorHeight, tensorWidth, ResizeOp.ResizeMethod.BILINEAR))
                .add(new CastOp(inputDataType))
                .build();

        // Load labels
        try {
            LABELS = FileUtil.loadLabels(context, labelsPath);
        } catch (IOException e) {
            Log.e(TAG, "EmotionClassifier: Error occurred while loading labels", e);
            throw new RuntimeException(e);
        }
    }

    // Runs inference and returns predicted labels
    public synchronized HashMap<String, Float> predict(Bitmap croppedCat) {
        // Keep track of inference time
        long inferenceTime = SystemClock.uptimeMillis();

        // Create TensorImage from Bitmap
        TensorImage tensorImage = new TensorImage(inputDataType);
        tensorImage.load(croppedCat);

        // Process image
        TensorImage processedImage = imageProcessor.process(tensorImage);
        // Output buffer
        TensorBuffer outputBuffer = TensorBuffer.createFixedSize(new int[]{1, numClasses}, outputDataType);

        // Run inference
        interpreter.run(processedImage.getBuffer(), outputBuffer.getBuffer());
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime;

        return processOutput(outputBuffer.getFloatArray());
    }

    // Processes model output and returns labels that meet the threshold
    private HashMap<String, Float> processOutput(float[] output) {
        HashMap<String, Float> predictedLabels = new HashMap<>();

        for (int i = 0; i < output.length; i++) {
            Log.d(TAG, String.format("processOutput: label=%s | conf=%s | %s", LABELS.get(i), output[i], output[i] > THRESHOLD));
            if (output[i] > THRESHOLD) {
                predictedLabels.put(LABELS.get(i), output[i]);
            }
        }

        return predictedLabels;
    }

    // Cleans up interpreter and GPU delegate resources
    public synchronized void close() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
        }
        if (gpuDelegate != null) {
            gpuDelegate.close();
            gpuDelegate = null;
        }
    }

    // Constants
    private static final String TAG = "EmotionClassifier";
    private static final int NUM_THREADS = 4;
    private static final int INPUT_SIZE = 224;
    private static final float INPUT_MEAN = 0f;
    private static final float INPUT_STANDARD_DEVIATION = 255f;
    private static final float THRESHOLD = 0.5f;
}

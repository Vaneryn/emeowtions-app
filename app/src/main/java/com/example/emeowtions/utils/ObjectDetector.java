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
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ObjectDetector {

    private Interpreter interpreter;
    private List<String> labels = new ArrayList<>();

    private int tensorWidth = 0;
    private int tensorHeight = 0;
    private int numChannel = 0;
    private int numElements = 0;

    private ImageProcessor imageProcessor = new ImageProcessor.Builder()
            .add(new NormalizeOp(INPUT_MEAN, INPUT_STANDARD_DEVIATION))
            .add(new CastOp(INPUT_IMAGE_TYPE))
            .build();

    // Attributes
    private Context context;
    private String modelPath;
    private String labelPath;
    private DetectorListener detectorListener;
    private boolean isClosed;

    // Constructor
    public ObjectDetector(Context context, String modelPath, String labelPath, DetectorListener detectorListener) {
        // Set attributes
        this.context = context;
        this.modelPath = modelPath;
        this.labelPath = labelPath;
        this.detectorListener = detectorListener;
        this.isClosed = false;

        CompatibilityList compatibilityList = new CompatibilityList();
        Interpreter.Options options = new Interpreter.Options();

        // Configure delegate hardware
        if (compatibilityList.isDelegateSupportedOnThisDevice()) {
            GpuDelegate.Options delegateOptions = compatibilityList.getBestOptionsForThisDevice();
            options.addDelegate(new GpuDelegate(delegateOptions));
        } else {
            options.setNumThreads(NUM_THREADS);
        }

        // Load model
        try {
            MappedByteBuffer model = FileUtil.loadMappedFile(context, modelPath);
            interpreter = new Interpreter(model, options);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        int[] inputShape = interpreter.getInputTensor(0).shape();
        int[] outputShape = interpreter.getOutputTensor(0).shape();

        if (inputShape != null) {
            tensorWidth = inputShape[1];
            tensorHeight = inputShape[2];

            if (inputShape[1] == 3) {
                tensorWidth = inputShape[2];
                tensorHeight = inputShape[3];
            }
        }

        if (outputShape != null) {
            numChannel = outputShape[1];
            numElements = outputShape[2];
        }

        // Load labels
        try {
            InputStream inputStream = context.getAssets().open(labelPath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                labels.add(line);
            }

            reader.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void restart(boolean isGpu) {
        interpreter.close();

        Interpreter.Options options = new Interpreter.Options();

        if (isGpu) {
            CompatibilityList compatibilityList = new CompatibilityList();

            if (compatibilityList.isDelegateSupportedOnThisDevice()) {
                GpuDelegate.Options delegateOptions = compatibilityList.getBestOptionsForThisDevice();
                options.addDelegate(new GpuDelegate(delegateOptions));
            } else {
                options.setNumThreads(NUM_THREADS);
            }
        } else {
            options.setNumThreads(NUM_THREADS);
        }

        // Load model
        try {
            MappedByteBuffer model = FileUtil.loadMappedFile(context, modelPath);
            interpreter = new Interpreter(model, options);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public synchronized void close() {
        isClosed = true;
        interpreter.close();
    }

    public synchronized void detect(Bitmap frame) {
        if (interpreter == null || isClosed) {
            return;
        }

        if (tensorWidth == 0 || tensorHeight == 0 || numChannel == 0 || numElements == 0) {
            return;
        }

        try {
            if (interpreter != null) {
                long inferenceTime = SystemClock.uptimeMillis();

                Bitmap resizedBitmap = Bitmap.createScaledBitmap(frame, tensorWidth, tensorHeight, false);

                TensorImage tensorImage = new TensorImage(INPUT_IMAGE_TYPE);
                tensorImage.load(resizedBitmap);
                TensorImage processedImage = imageProcessor.process(tensorImage);
                TensorBuffer output = TensorBuffer.createFixedSize(new int[]{1, numChannel, numElements}, OUTPUT_IMAGE_TYPE);

                interpreter.run(processedImage.getBuffer(), output.getBuffer());

                List<BoundingBox> bestBoxes = bestBox(output.getFloatArray());
                inferenceTime = SystemClock.uptimeMillis() - inferenceTime;

                if (bestBoxes == null || bestBoxes.isEmpty()) {
                    detectorListener.onEmptyDetect();
                } else {
                    detectorListener.onDetect(bestBoxes, inferenceTime);
                }
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private List<BoundingBox> bestBox(float[] array) {
        List<BoundingBox> boundingBoxes = new ArrayList<>();

        for (int i = 0; i < numElements; i++) {
            float maxConf = CONFIDENCE_THRESHOLD;
            int maxIdx = -1;
            int j = 4;
            int arrayIdx = i + numElements * j;

            while (j < numChannel) {
                if (array[arrayIdx] > maxConf) {
                    maxConf = array[arrayIdx];
                    maxIdx = j - 4;
                }

                j++;
                arrayIdx += numElements;
            }

            if (maxConf > CONFIDENCE_THRESHOLD) {
                String clsName = labels.get(maxIdx);
                float cx = array[i]; // 0
                float cy = array[i + numElements]; // 1
                float w = array[i + numElements * 2];
                float h = array[i + numElements * 3];
                float x1 = cx - (w / 2F);
                float y1 = cy - (h / 2F);
                float x2 = cx + (w / 2F);
                float y2 = cy + (h / 2F);
                //Log.i(TAG, clsName);
                if (x1 < 0F || x1 > 1F || y1 < 0F || y1 > 1F || x2 < 0F || x2 > 1F || y2 < 0F || y2 > 1F)
                    continue;

                boundingBoxes.add(
                        new BoundingBox(x1, y1, x2, y2, cx, cy, w, h, maxConf, maxIdx, clsName)
                );
            }
        }

        if (boundingBoxes.isEmpty())
            return null;

        return applyNMS(boundingBoxes);
    }

    private List<BoundingBox> applyNMS(List<BoundingBox> boxes) {
        List<BoundingBox> sortedBoxes = new ArrayList<>(boxes);
        sortedBoxes.sort((box1, box2) -> Float.compare(box2.getConf(), box1.getConf()));
        List<BoundingBox> selectedBoxes = new ArrayList<>();

        while (!sortedBoxes.isEmpty()) {
            BoundingBox first = sortedBoxes.remove(0);
            selectedBoxes.add(first);

            Iterator iterator = sortedBoxes.iterator();
            while (iterator.hasNext()) {
                BoundingBox nextBox = (BoundingBox) iterator.next();
                float iou = calculateIoU(first, nextBox);

                if (iou >= IOU_THRESHOLD) {
                    iterator.remove();
                }
            }
        }

        return selectedBoxes;
    }

    private float calculateIoU(BoundingBox box1, BoundingBox box2) {
        float x1 = Math.max(box1.getX1(), box2.getX1());
        float y1 = Math.max(box1.getY1(), box2.getY1());
        float x2 = Math.max(box1.getX2(), box2.getX2());
        float y2 = Math.max(box1.getY1(), box2.getY2());
        float intersectionArea = Math.max(0f, x2 - x1) * Math.max(0f, y2 - y1);
        float box1Area = box1.getWidth() * box1.getHeight();
        float box2Area = box2.getWidth() * box2.getHeight();

        return intersectionArea / (box1Area + box2Area - intersectionArea);
    }

    public interface DetectorListener {
        void onEmptyDetect();
        void onDetect(List<BoundingBox> boundingBoxes, long inferenceTime);
    }

    // Constants
    private static final String TAG = "ObjectDetector";
    private static final int NUM_THREADS = 4;
    private static final float INPUT_MEAN = 0f;
    private static final float INPUT_STANDARD_DEVIATION = 255f;
    private static final DataType INPUT_IMAGE_TYPE = DataType.FLOAT32;
    private static final DataType OUTPUT_IMAGE_TYPE = DataType.FLOAT32;
    private static final float CONFIDENCE_THRESHOLD = 0.3f;
    private static final float IOU_THRESHOLD = 0.5f;
}

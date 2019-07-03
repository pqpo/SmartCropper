package me.pqpo.smartcropperlib;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.text.TextUtils;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class ImageDetector {

    private static final String MODEL_FILE = "models/hed_lite_model_quantize.tflite";

    private int desiredSize = 256;


    private int[] intValues = new int[desiredSize * desiredSize];

    protected ByteBuffer imgData = null;
    protected ByteBuffer outImgData = null;

    protected Interpreter tflite;

    public ImageDetector(Context context) throws IOException {
        this(context, MODEL_FILE);
    }

    public ImageDetector(Context context, String modelFile) throws IOException {
        if (TextUtils.isEmpty(modelFile)) {
            modelFile = MODEL_FILE;
        }
        MappedByteBuffer tfliteModel = loadModelFile(context, modelFile);
        Interpreter.Options tfliteOptions = new Interpreter.Options();
        tflite = new Interpreter(tfliteModel, tfliteOptions);
        imgData = ByteBuffer.allocateDirect(desiredSize * desiredSize * 3 * Float.SIZE / Byte.SIZE);
        imgData.order(ByteOrder.nativeOrder());

        outImgData = ByteBuffer.allocateDirect(desiredSize * desiredSize * Float.SIZE / Byte.SIZE);
        outImgData.order(ByteOrder.nativeOrder());
    }

    public synchronized Bitmap detectImage(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        imgData.clear();
        outImgData.clear();
        bitmap = Bitmap.createScaledBitmap(bitmap, desiredSize, desiredSize, false);
        convertBitmapToByteBuffer(bitmap);
        tflite.run(imgData, outImgData);
        return convertOutputBufferToBitmap(outImgData);
    }

    private void convertBitmapToByteBuffer(Bitmap bitmap) {
        if (imgData == null) {
            return;
        }
        bitmap.getPixels(intValues, 0, desiredSize, 0, 0, desiredSize, desiredSize);
        imgData.rewind();
        // Convert the image to floating point.
        int pixel = 0;
        for (int i = 0; i < desiredSize; ++i) {
            for (int j = 0; j < desiredSize; ++j) {
                final int pixelValue = intValues[pixel++];
                imgData.putFloat(((pixelValue >> 16) & 0xFF));
                imgData.putFloat(((pixelValue >> 8) & 0xFF));
                imgData.putFloat((pixelValue & 0xFF));
            }
        }
    }

    private Bitmap convertOutputBufferToBitmap(ByteBuffer outImgData) {
        if (outImgData == null) {
            return null;
        }
        outImgData.rewind();
        Bitmap bitmap_out = Bitmap.createBitmap(desiredSize , desiredSize, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[desiredSize * desiredSize];
        for (int i = 0; i < desiredSize * desiredSize; i++) {
            float val = outImgData.getFloat();
            if (val > 0.2) {
                pixels[i] = 0xFFFFFFFF;
            } else {
                pixels[i] = 0xFF000000;
            }
        }
        bitmap_out.setPixels(pixels, 0, desiredSize, 0, 0, desiredSize, desiredSize);
        return bitmap_out;
    }

    private MappedByteBuffer loadModelFile(Context activity, String modelFile) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(modelFile);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

}

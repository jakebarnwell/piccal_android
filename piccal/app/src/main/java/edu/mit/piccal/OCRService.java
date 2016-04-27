package edu.mit.piccal;

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Environment;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by anthony on 4/26/16.
 */
public class OCRService {
    private static final String TAG = "piccal_log";

    private static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/piccal/";
    private static final String LANG = "eng";
    private static TessBaseAPI baseApi = new TessBaseAPI();


    /**
     * Initializes Tesseract OCR service.
     * @param EditResultActivity
     */
    public OCRService(Activity EditResultActivity) {
        String[] paths = new String[] { DATA_PATH, DATA_PATH + "tessdata/" };

        for (String path : paths) {
            File dir = new File(path);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Log.v(TAG, "ERROR: Creation of directory " + path + " on sdcard failed");
                    return;
                } else {
                    Log.v(TAG, "Created directory " + path + " on sdcard");
                }
            }

        }

        if (!(new File(DATA_PATH + "tessdata/" + LANG + ".traineddata")).exists()) {
            try {
                AssetManager assetManager = EditResultActivity.getAssets();
                InputStream in = assetManager.open("tessdata/" + LANG + ".traineddata");
                OutputStream out = new FileOutputStream(DATA_PATH
                        + "tessdata/" + LANG + ".traineddata");

                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();

                Log.v(TAG, "Copied " + LANG + " traineddata");
            } catch (IOException e) {
                Log.e(TAG, "Was unable to copy " + LANG + " traineddata " + e.toString());
            }
        }

        baseApi.setDebug(true);
        baseApi.init(DATA_PATH, LANG); // Loads language training data
    }

    public static String extractText(String imagePath) {
        Bitmap processedImage = processImage(imagePath);

        baseApi.setImage(processedImage);
        String recognizedText = baseApi.getUTF8Text();

        baseApi.clear();
        processedImage.recycle();

        recognizedText = cleanText(recognizedText);
        return recognizedText;
    }

    private static Bitmap processImage(String imagePath) {
        // OpenCV pre-processing
        Mat originalImage = Imgcodecs.imread(imagePath);
        Imgproc.pyrDown(originalImage, originalImage);
        Mat image = new Mat();
        Imgproc.cvtColor(originalImage, image, Imgproc.COLOR_BGRA2GRAY, 1);
        Size window = new Size(3, 3);
        Imgproc.GaussianBlur(image, image, window, 0);
        Imgproc.adaptiveThreshold(image, image, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 75, 10);
        Core.bitwise_not(image, image);
        Log.i(TAG, "Prepressesing Camera Image (END)" + image.size().toString());
        Bitmap bitmap = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(image, bitmap);
        return bitmap;
    }

    private static String cleanText(String extractedText) {
        String cleanText = extractedText.replaceAll("[^\\w\\s-]", " ");
        cleanText = cleanText.replaceAll("[^\\p{ASCII}]", " ");
        cleanText = cleanText.replaceAll("_", " ");
        cleanText = cleanText.replaceAll("\n", " ");
        cleanText = cleanText.replaceAll("(\\s+[^aAiI0-9](?=\\s))","");
        cleanText = cleanText.replaceAll("( )+", " ");
        cleanText = cleanText.trim();
        Log.d(TAG, cleanText);
        return cleanText;
    }
}

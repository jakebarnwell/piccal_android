package edu.mit.piccal;

import android.content.ClipData;
import android.content.ClipDescription;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.io.IOException;

public class PickCornersActivity extends AppCompatActivity {

    private String mCurrentPhotoPath;
    private boolean mPicLoaded = false;

    // The currently shown image
    private ImageView imview;

    // The four corner imageview sprites
    private ImageView tl, tr, br, bl;

    private static final String TAG = "PickCorners";

    private int _xDelta, _yDelta;

    RelativeLayout.LayoutParams layoutParams;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pick_corners);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mCurrentPhotoPath = extras.getString("PHOTO_PATH");
        }

        imview = (ImageView)findViewById(R.id.imview);

        tl = (ImageView)findViewById(R.id.corner_topleft);
        tr = (ImageView)findViewById(R.id.corner_topright);
        br = (ImageView)findViewById(R.id.corner_bottomright);
        bl = (ImageView)findViewById(R.id.corner_bottomleft);

        tl.setOnTouchListener(makeCornerOnTouchListener((ImageView)findViewById(R.id.corner_topleft)));
        tr.setOnTouchListener(makeCornerOnTouchListener((ImageView)findViewById(R.id.corner_topright)));
        br.setOnTouchListener(makeCornerOnTouchListener((ImageView)findViewById(R.id.corner_bottomright)));
        bl.setOnTouchListener(makeCornerOnTouchListener((ImageView)findViewById(R.id.corner_bottomleft)));
    }

    private View.OnTouchListener makeCornerOnTouchListener(final ImageView iv_corner) {
        return new View.OnTouchListener() {
            public boolean onTouch(View view, MotionEvent event) {
                final int X = (int) event.getRawX();
                final int Y = (int) event.getRawY();
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        RelativeLayout.LayoutParams lParams = (RelativeLayout.LayoutParams) view.getLayoutParams();
                        _xDelta = (int) (X - iv_corner.getTranslationX());
                        _yDelta = (int) (Y - iv_corner.getTranslationY());
                        break;
                    case MotionEvent.ACTION_UP:
                        break;
                    case MotionEvent.ACTION_POINTER_DOWN:
                        break;
                    case MotionEvent.ACTION_POINTER_UP:
                        break;
                    case MotionEvent.ACTION_MOVE:
                        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) view.getLayoutParams();

                        iv_corner.setTranslationX(X - _xDelta);
                        iv_corner.setTranslationY(Y - _yDelta);
                        break;
                }

                return true;

            }
        };
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        // Assign the bitmap in the imageView
        if(!mPicLoaded && mCurrentPhotoPath != null) {
            Bitmap bitmap = getBitmap(mCurrentPhotoPath);
            Bitmap rotatedBitmap = getRotatedBitmap(bitmap, mCurrentPhotoPath);
            if(bitmap != rotatedBitmap) bitmap.recycle();
            Bitmap imviewBitmap = getImageViewBitmap(rotatedBitmap, imview);
            if(rotatedBitmap != imviewBitmap) rotatedBitmap.recycle();
            imview.setImageBitmap(imviewBitmap);

            mPicLoaded = true;
        }
//
//        // Move four corner images to the corners of the underlying imageView
//        RelativeLayout.LayoutParams params =
//                new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
//                        RelativeLayout.LayoutParams.WRAP_CONTENT);
//
//        int corner_width = tl.getWidth(), corner_height = tl.getHeight();
//
//        int     left_margin = -corner_width / 2,
//                top_margin = -corner_height / 2,
//                right_margin = -corner_width / 2,
//                bottom_margin = -corner_height / 2;
//
//        params.setMargins(left_margin, top_margin, 0, 0);
//        tl.setLayoutParams(params);
//
//
//        RelativeLayout.LayoutParams params2 =
//                new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
//                        RelativeLayout.LayoutParams.WRAP_CONTENT);
//        params2.setMargins(0, 0, right_margin, bottom_margin);
//
//        br.setLayoutParams(params2);

    }



    private static Bitmap getBitmap(String path) {
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inPurgeable = true;
        bmOptions.inSampleSize = 4;
        Bitmap bitmap = BitmapFactory.decodeFile(path, bmOptions);

        return bitmap;
    }

    private Bitmap getRotatedBitmap(Bitmap bitmap, String path){
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED);

        Bitmap rotatedBitmap = do_rotateBitmap(bitmap, orientation);

        // Recycle this bitch
        if(rotatedBitmap != bitmap) {
            bitmap.recycle();
        }

        return rotatedBitmap;
    }

    public static Bitmap do_rotateBitmap(Bitmap bitmap, int orientation) {
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                return bitmap;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default:
                return bitmap;
        }
        try {
            Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if(bmRotated != bitmap) bitmap.recycle();
            return bmRotated;
        }
        catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }
    }

    private Bitmap getImageViewBitmap(Bitmap rotatedBitmap, ImageView imview){
        int targetW = imview.getWidth();
        int targetH = imview.getHeight();
        String s = "w, h = " + targetW + ", " + targetH;
        Log.d(TAG, s);

        Bitmap resizedBitmap = getResizedBitmap(rotatedBitmap, targetW, targetH);
        if(resizedBitmap != rotatedBitmap) rotatedBitmap.recycle();
        return resizedBitmap;
    }

    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);

        if(resizedBitmap != bm) bm.recycle();

        return resizedBitmap;
    }


}

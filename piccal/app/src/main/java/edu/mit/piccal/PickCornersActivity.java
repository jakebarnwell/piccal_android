package edu.mit.piccal;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
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

    public void onClick_confirm(View v) {
        String corners_string = "";
        int im_left = imview.getLeft(), im_top = imview.getTop(), im_right = imview.getRight(),
                im_bottom = imview.getBottom();
//        Log.d(TAG, "im left top right bottom: " + im_left + " " + im_top + " " + im_right + " " + im_bottom);
        int im_width = im_right - im_left, im_height = im_bottom - im_top;
//        Log.d(TAG, "im width height: " + im_width + " " + im_height);

        float px_50 = pixels(50);
        float tl_cx = tl.getLeft() + tl.getTranslationX() + px_50,
                tl_cy = tl.getTop() + tl.getTranslationY() + px_50;
        float tr_cx = tr.getLeft() + tr.getTranslationX() + px_50,
                tr_cy = tr.getTop() + + tr.getTranslationY() + px_50;
        float br_cx = br.getLeft() + br.getTranslationX() + px_50,
                br_cy = br.getTop() + br.getTranslationY() + px_50;
        float bl_cx = bl.getLeft() + bl.getTranslationX() + px_50,
                bl_cy = bl.getTop() + bl.getTranslationY() + px_50;
//        Log.d(TAG, "TL TR BR BL centers x, y: " + tl_cx + " " + tl_cy + " " + tr_cx + " " + tr_cy + " " +
//        br_cx + " " + br_cy + " " + bl_cx + " " + bl_cy);

        corners_string += clamp((1.0*tl_cx - im_left) / im_width) + " " + clamp((1.0*tl_cy - im_top) / im_height) + " ";
        corners_string += clamp((1.0*tr_cx - im_left) / im_width) + " " + clamp((1.0*tr_cy - im_top) / im_height) + " ";
        corners_string += clamp((1.0*br_cx - im_left) / im_width) + " " + clamp((1.0*br_cy - im_top) / im_height) + " ";
        corners_string += clamp((1.0*bl_cx - im_left) / im_width) + " " + clamp((1.0*bl_cy - im_top) / im_height);
        Log.d(TAG, "Corners string: " + corners_string);
        Intent intent = new Intent(this, EditResultActivity.class);
        intent.putExtra("PHOTO_PATH", mCurrentPhotoPath);
        intent.putExtra("CORNERS", corners_string);
        startActivity(intent);
    }

    private float pixels(int dp) {
        Resources r = getResources();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
        return px;
    }

    private double clamp(double x) {
        if(x < 0) {
            return 0;
        } else if(x > 1) {
            return 1;
        } else {
            return x;
        }
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

package edu.mit.piccal;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class ServerCommunicator {

    private static final String SERVER_URL = "http://54.174.172.48/upload/";
    private static final String TAG = "ServerCommunicator: ";

    private ProgressDialog mProgDialog;
    private Context mContext;
    private EditResultActivity that;

    /**
     * Constructs a new ServerCommunicator object to send images to the server with a given
     * context and calling activity class.
     * @param context the context of the calling Activity
     * @param callingActivity the instance of the activity calling this constructor
     */
    public ServerCommunicator(Context context, EditResultActivity callingActivity) {
        Log.d(TAG, "Creating a ServerCommunicator for context " + context.toString());
        mContext = context;
        that = callingActivity;
    }

    public void send(Bitmap bitmap) {
        Communicator comm = new Communicator(bitmap);
        comm.execute();
    }

    public void shutdown() {
        ;
    }

    public class Communicator extends AsyncTask<String, Integer, String> {

        private final Bitmap mBitmap;

        public Communicator(Bitmap bitmap) {
            mBitmap = bitmap;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.d(TAG, "Sending image to server (pre-execute)");
            mProgDialog = new ProgressDialog(mContext);
            mProgDialog.setMessage("Please Wait. Analyzing image...");
            mProgDialog.setIndeterminate(false);
            mProgDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgDialog.setCancelable(true);
            mProgDialog.show();
        }

        protected String doInBackground(String... strings) {
            Log.d(TAG, "Image sent to server; background.");

            String response = null;
            try {
                response = sendPost(mBitmap);
            } catch (Exception e) {
                Log.e(TAG, "Error communicating with the server...");
                e.printStackTrace();
            }
            return response;
        }

        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            mBitmap.recycle();

            if(result != null) {
                //Context context = getApplicationContext();
                //Toast.makeText(context, result, Toast.LENGTH_LONG).show();
                Log.d(TAG, "Server result: + " + result.replaceAll("\n", "\\n"));
            }

            mProgDialog.dismiss();
            that.onOcrResult(result);
        }

        // HTTP POST request
        private String sendPost(Bitmap bitmap) throws Exception {
            // static stuff
            String attachmentName = "file";
            String attachmentFileName = "bitmap.bmp";
            String crlf = "\r\n";
            String twoHyphens = "--";
            String boundary = "*****";

            Log.d(TAG, "Attempting to establish HTTP connection...");
            // setup request
            HttpURLConnection httpUrlConnection = null;
            URL url = new URL(SERVER_URL);
            httpUrlConnection = (HttpURLConnection) url.openConnection();
            httpUrlConnection.setUseCaches(false);
            httpUrlConnection.setDoOutput(true);

            httpUrlConnection.setRequestMethod("POST");
            httpUrlConnection.setRequestProperty("Connection", "Keep-Alive");
            httpUrlConnection.setRequestProperty("Cache-Control", "no-cache");
            httpUrlConnection.setRequestProperty(
                    "Content-Type", "multipart/form-data;boundary=" + boundary);


            // start content wrapper
            DataOutputStream request = new DataOutputStream(
                    httpUrlConnection.getOutputStream());

            request.writeBytes(twoHyphens + boundary + crlf);
            request.writeBytes("Content-Disposition: form-data; name=\"" +
                    attachmentName + "\";filename=\"" +
                    attachmentFileName + "\"" + crlf);
            request.writeBytes(crlf);

            // convert bitmap to byte buffer
            //I want to send only 8 bit black & white bitmaps
            byte[] pixels = new byte[bitmap.getWidth() * bitmap.getHeight()];
            for (int i = 0; i < bitmap.getWidth(); ++i) {
                for (int j = 0; j < bitmap.getHeight(); ++j) {
                    //we're interested only in the MSB of the first byte,
                    //since the other 3 bytes are identical for B&W images
                    pixels[i + j] = (byte) ((bitmap.getPixel(i, j) & 0x80) >> 7);
                }
            }
            request.write(pixels);


            // end content wrapper
            request.writeBytes(crlf);
            request.writeBytes(twoHyphens + boundary +
                    twoHyphens + crlf);


            // flush output buffer
            request.flush();
            request.close();

            Log.d(TAG, "Image successfully sent. Waiting for response...");

            // get response
            InputStream responseStream = new
                    BufferedInputStream(httpUrlConnection.getInputStream());

            BufferedReader responseStreamReader =
                    new BufferedReader(new InputStreamReader(responseStream));

            String line = "";
            StringBuilder stringBuilder = new StringBuilder();

            while ((line = responseStreamReader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            responseStreamReader.close();

            String response = stringBuilder.toString();

            Log.d(TAG, "Response received.");

            // close response stream
            responseStream.close();

            // close connection
            httpUrlConnection.disconnect();

            // recycle bitmap
            mBitmap.recycle();

            return response;

        }

    }
}


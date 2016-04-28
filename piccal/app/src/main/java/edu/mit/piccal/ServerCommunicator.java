package edu.mit.piccal;


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

    private Bitmap mBitmap;
    private String mResult;
    private String mHeader = " ";
    private String mServerURL = "piccal.fandys.com/post/pic/";
    private ProgressDialog mProgDialog;
    private Context mContext;
    private String TAG = "ServerCommunicator - ";

    public ServerCommunicator(Bitmap bitmap){
        mBitmap = bitmap;
    }



    public class Communicator extends AsyncTask<String, Integer, String> {

//        Bitmap mBitmap;

        public Communicator(Context context, Bitmap bitmap) {
            mContext = context;
//            mBitmap = bitmap;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgDialog = new ProgressDialog(mContext);
            mProgDialog.setMessage("Please Wait. Analyzing image...");
            mProgDialog.setIndeterminate(false);
            mProgDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgDialog.setCancelable(true);
            mProgDialog.show();
        }

        protected String doInBackground(String... strings) {

            String response = null;
            try {
                response = sendPost(mBitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return response;
        }

        protected void onPostExecute(String result) {
            //Context context = getApplicationContext();
            //Toast.makeText(context, result, Toast.LENGTH_LONG).show();
            Log.d(TAG, result.replaceAll("\n", " "));
            super.onPostExecute(result);
            mResult = result;
            mProgDialog.dismiss();
        }

        // HTTP POST request
        private String sendPost(Bitmap bitmap) throws Exception {
            // static stuff
            String attachmentName = "bitmap";
            String attachmentFileName = "bitmap.bmp";
            String crlf = "\r\n";
            String twoHyphens = "--";
            String boundary = "*****";


            // setup request
            HttpURLConnection httpUrlConnection = null;
            URL url = new URL(mServerURL);
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


            // close response stream
            responseStream.close();


            // close connection
            httpUrlConnection.disconnect();

            return response;

        }

    }
}


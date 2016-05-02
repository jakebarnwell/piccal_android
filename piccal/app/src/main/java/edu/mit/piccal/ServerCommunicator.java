package edu.mit.piccal;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
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

    public void send(String path) {
        Communicator comm = new Communicator(path);
        comm.execute();
    }

    public class Communicator extends AsyncTask<String, Integer, String> {

        private final String mPathToFile;

        public Communicator(String pathToFile) {
            this.mPathToFile = pathToFile;
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
            Log.d(TAG, "Image sending to server; background.");

            String response = null;
            try {
                response = sendPost(mPathToFile);
            } catch (Exception e) {
                Log.e(TAG, "Error communicating with the server...");
                e.printStackTrace();
            }
            return response;
        }

        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if(result != null) {
                //Context context = getApplicationContext();
                //Toast.makeText(context, result, Toast.LENGTH_LONG).show();
                Log.d(TAG, "Server result: + " + result.replaceAll("\n", "\\n"));
            }

            mProgDialog.dismiss();
            that.onOcrResult(result);
        }

        private String sendPost(String pathToFile) {
            HttpURLConnection connection = null;
            DataOutputStream outputStream = null;
            DataInputStream inputStream = null;
            String pathToOurFile = pathToFile;
            String urlServer = SERVER_URL;
            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary =  "*****";

            int bytesRead, bytesAvailable, bufferSize;
            byte[] buffer;
            int maxBufferSize = 1*1024*1024;

            try {
                FileInputStream fileInputStream = new FileInputStream(new File(pathToOurFile));

                URL url = new URL(urlServer);
                connection = (HttpURLConnection) url.openConnection();

                // Allow Inputs &amp; Outputs.
                connection.setDoInput(true);
                connection.setDoOutput(true);
                connection.setUseCaches(false);

                // Set HTTP method to POST.
                connection.setRequestMethod("POST");

                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

                outputStream = new DataOutputStream(connection.getOutputStream());
                outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                outputStream.writeBytes("Content-Disposition: form-data; name=\"file\";filename=\"" + pathToOurFile + "\"" + lineEnd);
                outputStream.writeBytes(lineEnd);

                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // Read file
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {
                    outputStream.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }

                outputStream.writeBytes(lineEnd);
                outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                Log.d(TAG, "Done writing image to server.");
                // Responses from the server (code and message)
                int serverResponseCode = connection.getResponseCode();
                String serverResponseMessage = connection.getResponseMessage();

                fileInputStream.close();
                outputStream.flush();
                outputStream.close();

                // get response
                InputStream responseStream = new
                        BufferedInputStream(connection.getInputStream());

                BufferedReader responseStreamReader =
                        new BufferedReader(new InputStreamReader(responseStream));

                String line = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ((line = responseStreamReader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                responseStreamReader.close();

                String response = stringBuilder.toString();

                Log.d(TAG, "Response received:\n" + response.replace("\n","\\n"));

                // close response stream
                responseStream.close();

                // close connection
                connection.disconnect();

                return response;

            } catch(Exception e) {
                e.printStackTrace();
            }

            return "";
        }

    }
}


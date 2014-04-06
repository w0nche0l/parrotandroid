package com.ddd.ClipSync;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;

//http://developer.android.com/google/auth/http-auth.html#ExtendAsyncTask
public class GetSubTask extends AsyncTask<Void, Void, Void>{
    private static final String TAG = "GetSubTask";
	private static final String ID_KEY = "id";
	
	private static final int REQUEST_AUTHORIZATION = 1000;
	
	MainActivity mActivity;
    String mScope;
    String mEmail;
     

    GetSubTask(MainActivity activity, String name, String scope) {
        this.mActivity = activity;
        this.mScope = scope;
        this.mEmail = name;
    }

    /**
     * Executes the asynchronous job. This runs when you call execute()
     * on the AsyncTask instance.
     */
    @Override
    protected Void doInBackground(Void... params) {
        try {
            String token = fetchToken();
            if (token != null) {
                // Insert the good stuff here.
                // Use the token to access the user's Google data.
            	//http://developer.android.com/google/play-services/auth.html#use
            	Log.d(TAG, "token is " + token);
            	URL url = new URL("https://www.googleapis.com/oauth2/v1/userinfo?access_token="
            	        + token);
            	HttpURLConnection con = (HttpURLConnection) url.openConnection();
            	int serverCode = con.getResponseCode();
            	//successful query
            	if (serverCode == 200) {
            	    InputStream is = con.getInputStream();
            	    String sub = getSub(readResponse(is));
            	    mActivity.setSub(sub);
            	    is.close();
            	    return null;
            	//bad token, invalidate and get a new one
            	} else if (serverCode == 401) {
            	    GoogleAuthUtil.invalidateToken(mActivity, token);
            	    //onError("Server auth error, please try again.", null);
            	    Log.e(TAG, "Server auth error: " + readResponse(con.getErrorStream()));
            	    return null;
            	//unknown error, do something else
            	} else {
            	    Log.e("Server returned the following error code: " + serverCode, null);
            	    return null;
            	}
            }
        } catch (IOException e) {
            // The fetchToken() method handles Google-specific exceptions,
            // so this indicates something went wrong at a higher level.
            // TIP: Check for network connectivity before starting the AsyncTask.
    	    Log.e(e.getMessage(), null);
        } catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return null;
    }

    /**
     * Gets an authentication token from Google and handles any
     * GoogleAuthException that may occur.
     */
    protected String fetchToken() throws IOException {
        try {
            return GoogleAuthUtil.getToken(mActivity, mEmail, mScope);
        } catch (UserRecoverableAuthException userRecoverableException) {
            // GooglePlayServices.apk is either old, disabled, or not present
            // so we need to show the user some UI in the activity to recover.
        	mActivity.startActivityForResult(userRecoverableException.getIntent(), REQUEST_AUTHORIZATION);
        	userRecoverableException.printStackTrace();
        } catch (GoogleAuthException fatalException) {
            // Some other type of unrecoverable exception has occurred.
            // Report and log the error as appropriate for your app.
        	fatalException.printStackTrace();
        }
        return null;
    }
    
    /**
     * Reads the response from the input stream and returns it as a string.
     */
    private static String readResponse(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int len = 0;
        while ((len = is.read(data, 0, data.length)) >= 0) {
            bos.write(data, 0, len);
        }
        return new String(bos.toByteArray(), "UTF-8");
    }

    /**
     * Parses the response and returns the first name of the user.
     * @throws JSONException if the response is not JSON or if first name does not exist in response
     */
    private String getSub(String jsonResponse) throws JSONException {
      JSONObject profile = new JSONObject(jsonResponse);
      Log.d(TAG, jsonResponse);
      return profile.getString(ID_KEY);
    }


}

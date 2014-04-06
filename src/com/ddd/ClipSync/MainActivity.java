package com.ddd.ClipSync;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.gcm.GoogleCloudMessaging;

public class MainActivity extends ActionBarActivity {
	
	
    public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final String EMAIL_ID = "email";
    private static final String ACCOUNT_ID = "regId";
    private static final String SCOPE = "oauth2:https://www.googleapis.com/auth/userinfo.profile";
    
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private final static int ACCOUNT_REQUEST_CODE = 8000;
	
    
    String SENDER_ID = "967441572737";
    static final String TAG = "Parrot";

    String accountEmail;
    String accountId;
    String gcmRegId;
    TextView mDisplay;
    AtomicInteger msgId = new AtomicInteger();
    GoogleCloudMessaging gcm;
    Context context;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
		
        context = getApplicationContext();
        
        final SharedPreferences prefs = getPreferences(context);
	    accountEmail = prefs.getString(EMAIL_ID, "");
	    accountId = prefs.getString(ACCOUNT_ID, "");
        
	    if(accountEmail.isEmpty() || accountId.isEmpty())
	    	showGoogleAccountPicker();
	    else
	    	startGCM();
	    
//	    if(!accountEmail.isEmpty())
//			sendSelfEmail(); 
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	
	//http://developer.android.com/google/auth/http-auth.html#AccountName
	 protected void onActivityResult(final int requestCode, final int resultCode,
	         final Intent data) {
	     if (requestCode == ACCOUNT_REQUEST_CODE && resultCode == RESULT_OK) {
	         accountEmail = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
	         storeEmail(context, accountEmail);
	         //Toast.makeText(this, accountEmail, Toast.LENGTH_SHORT).show();
	         GetSubTask getSubTask = new GetSubTask(MainActivity.this, accountEmail, SCOPE);
	         getSubTask.execute();
	     }
	     else{
	            Toast.makeText(this, "You need to pick an account.", Toast.LENGTH_SHORT).show();
	     }
	 }
	
	private void showGoogleAccountPicker() {
		 Intent googlePicker = AccountPicker.newChooseAccountIntent(null, null, new String[]{"com.google"},
		         false, null, null, null, null);
	    startActivityForResult(googlePicker, ACCOUNT_REQUEST_CODE);
	}
	
	public void setSub(String gotSub){
		accountId = gotSub;
		storeAccountId(context, accountId);
		startGCM();
        //start receiver
	}
	
	public void startGCM(){
		gcm = GoogleCloudMessaging.getInstance(this);
        gcmRegId = getRegistrationId(context);
        Log.d(TAG, "gcmRegId is " + gcmRegId);
        if (gcmRegId.isEmpty()) {
        	Log.d(TAG, "registering in background");
            registerInBackground();
        }
        Toast.makeText(this, "Connected!", Toast.LENGTH_SHORT).show();
	}
	
	private String getRegistrationId(Context context) {
	    final SharedPreferences prefs = getPreferences(context);
	    String registrationId = prefs.getString(PROPERTY_REG_ID, "");
	    if (registrationId.isEmpty()) {
	        Log.i(TAG, "Registration not found.");
	        return "";
	    }
	    // Check if app was updated; if so, it must clear the registration ID
	    // since the existing regID is not guaranteed to work with the new
	    // app version.
	    int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
	    int currentVersion = getAppVersion(context);
	    if (registeredVersion != currentVersion) {
	        Log.i(TAG, "App version changed.");
	        return "";
	    }
	    return registrationId;
	}
	
	private SharedPreferences getPreferences(Context context) {
	    // This sample app persists the registration ID in shared preferences, but
	    // how you store the regID in your app is up to you.
	    return getSharedPreferences(MainActivity.class.getSimpleName(),
	            Context.MODE_PRIVATE);
	}

	private static int getAppVersion(Context context) {
	    try {
	        PackageInfo packageInfo = context.getPackageManager()
	                .getPackageInfo(context.getPackageName(), 0);
	        return packageInfo.versionCode;
	    } catch (NameNotFoundException e) {
	        // should never happen
	        throw new RuntimeException("Could not get package name: " + e);
	    }
	}
	
	private void registerInBackground() {
		Log.d(TAG, "called register");
	    new AsyncTask<Void, Void, String>() {
	        @Override
	        protected String doInBackground(Void... params) {
	            String msg = "";
	            try {
	                if (gcm == null) {
	                    gcm = GoogleCloudMessaging.getInstance(context);
	                }
	                gcmRegId = gcm.register(SENDER_ID);
	                msg = "Device registered, registration ID=" + gcmRegId;

	                // You should send the registration ID to your server over HTTP,
	                // so it can use GCM/HTTP or CCS to send messages to your app.
	                // The request to your server should be authenticated if your app
	                // is using accounts.
	                sendRegistrationIdToBackend();

	                // Persist the regID - no need to register again.
	                storeRegistrationId(context, gcmRegId);
	            } catch (IOException ex) {
	                msg = "Error :" + ex.getMessage();
	                // If there is an error, don't just keep trying to register.
	                // Require the user to click a button again, or perform
	                // exponential back-off.
	            }
	            return msg;
	        }

	        @Override
	        protected void onPostExecute(String msg) {
	        	Log.d(TAG, msg);
	        }
	    }.execute(null, null, null);
	}

	private void sendRegistrationIdToBackend() {
		Log.i(TAG, "sending");
		HttpClient client = new DefaultHttpClient();
		HttpPost post 	  = new HttpPost("https://clipsync.herokuapp.com/m/register");
		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
		pairs.add(new BasicNameValuePair("google_id", accountId));
		pairs.add(new BasicNameValuePair("gcm_id", gcmRegId));
		
		try {
			post.setEntity(new UrlEncodedFormEntity(pairs));
			HttpResponse response = client.execute(post);
			Log.i(TAG, "Sent");
		}catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void storeRegistrationId(Context context, String regId) {
	    final SharedPreferences prefs = getPreferences(context);
	    int appVersion = getAppVersion(context);
	    Log.i(TAG, "Saving regId on app version " + appVersion);
	    SharedPreferences.Editor editor = prefs.edit();
	    editor.putString(PROPERTY_REG_ID, regId);
	    editor.putInt(PROPERTY_APP_VERSION, appVersion);
	    editor.commit();
	}
	
	private void storeEmail(Context context, String email) {
	    final SharedPreferences prefs = getPreferences(context);
	    SharedPreferences.Editor editor = prefs.edit();
	    editor.putString(EMAIL_ID, email);
	    editor.commit();
	}

	private void storeAccountId(Context context, String accId) {
	    final SharedPreferences prefs = getPreferences(context);
	    SharedPreferences.Editor editor = prefs.edit();
	    editor.putString(ACCOUNT_ID, accId);
	    editor.commit();
	}
	
//	private void storeAccessToken(Context context, String token){
//		final SharedPreferences prefs = getPreferences(context);
//	    int appVersion = getAppVersion(context);
//	    Log.i(TAG, "Saving token on app version " + appVersion);
//	    SharedPreferences.Editor editor = prefs.edit();
//	    editor.putString(ACCESS_TOKEN_ID, token);
//	    editor.putInt(PROPERTY_APP_VERSION, appVersion);
//	    editor.commit();
//	}

	private void sendSelfEmail(){
		Intent send = new Intent(Intent.ACTION_SENDTO);
		String uriText = "mailto:" + Uri.encode(accountEmail) + 
		          "?subject=" + Uri.encode("Get Parrot") + 
		          "&body=" + Uri.encode("");
		Uri uri = Uri.parse(uriText);

		send.setData(uri);
		startActivity(Intent.createChooser(send, "Send mail..."));
	}
	
	
	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			return rootView;
		}
	}
}

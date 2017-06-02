package com.ggec.voice.assistservice.login.quicklogin;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import com.amazon.identity.auth.device.AuthError;
import com.amazon.identity.auth.device.api.authorization.AuthCancellation;
import com.amazon.identity.auth.device.api.authorization.AuthorizationManager;
import com.amazon.identity.auth.device.api.authorization.AuthorizeListener;
import com.amazon.identity.auth.device.api.authorization.AuthorizeRequest;
import com.amazon.identity.auth.device.api.authorization.AuthorizeResult;
import com.amazon.identity.auth.device.api.authorization.Scope;
import com.amazon.identity.auth.device.api.authorization.ScopeFactory;
import com.amazon.identity.auth.device.api.workflow.RequestContext;
import com.ggec.voice.assistservice.AssistService;
import com.ggec.voice.assistservice.BuildConfig;
import com.ggec.voice.assistservice.R;
import com.ggec.voice.assistservice.data.BackGroundProcessServiceControlCommand;
import com.willblaschko.android.alexa.SharedPreferenceUtil;
import com.willblaschko.android.alexa.TokenManager;
import com.willblaschko.android.alexa.keep.TokenResponse;
import com.willblaschko.android.alexa.utility.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static android.Manifest.permission.RECORD_AUDIO;

/**
 * Created by ggec on 2017/3/29.
 */

public class SampleLoginWithAmazonActivity extends Activity {

    private static final String TAG = SampleLoginWithAmazonActivity.class.getName();

    private RequestContext requestContext;
    private View mLoginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startAssistIfGetPermission();
        Log.i(TAG, "onCreate");
        requestContext = RequestContext.create(this);
        requestContext.registerListener(new AuthorizeListener() {
            /* Authorization was completed successfully. */
            @Override
            public void onSuccess(final AuthorizeResult authorizeResult) {
                Log.e(TAG, "auth success: token:"+ authorizeResult.getAccessToken());
                TokenManager.getAccessToken(SampleLoginWithAmazonActivity.this, authorizeResult.getAuthorizationCode(),
                        getCodeVerifier(SampleLoginWithAmazonActivity.this), authorizeResult.getRedirectURI(), authorizeResult.getClientId(),
                        new TokenManager.TokenResponseCallback() {
                            @Override
                            public void onSuccess(TokenResponse response) {
                                Log.d(TAG, "get alexa success,"+response.token_type+"\n"+response.access_token+"\n"+response.refresh_token+"\n"+response.expires_in);
                                showAuthToast("auth success");
                            }

                            @Override
                            public void onFailure(Exception error) {
                                Log.e(TAG, "get alexa onFailure",error);
                                showAuthToast("auth fail");
                            }
                        });
            }

            /* There was an error during the attempt to authorize the application */
            @Override
            public void onError(AuthError authError) {
                Log.e(TAG, "AuthError during authorization", authError);
                showAuthToast("auth onError");
            }

            /* Authorization was cancelled before it could be completed. */
            @Override
            public void onCancel(AuthCancellation authCancellation) {
                Log.e(TAG, "User cancelled authorization");
            }
        });


        setContentView(R.layout.activity_simple_login);
        initializeUI();


        findViewById(R.id.btn_start_record).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent it = BackGroundProcessServiceControlCommand.createIntentByType(v.getContext(), 1);
                startService(it);

//                cmd.type = 2;
//                startService(it);

            }
        });
        findViewById(R.id.btn_start_record_t).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent it = BackGroundProcessServiceControlCommand.createIntentByType(v.getContext(), 2);
                startService(it);

//                cmd.type = 2;
//                startService(it);

            }
        });

        findViewById(R.id.btn_start_record_file).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent it = BackGroundProcessServiceControlCommand.createIntentByType(v.getContext(), 3);
                it.putExtra("token", "a");
                it.putExtra("messageId", "321");
                startService(it);

//                cmd.type = 2;
//                startService(it);

            }
        });

        View.OnClickListener sss = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AudioManager audioManager = (AudioManager) v.getContext().getSystemService(Context.AUDIO_SERVICE);
                if(v.getId() == R.id.btn_volume) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
                } else {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
                }
            }
        };
        findViewById(R.id.btn_volume).setOnClickListener(sss);
        findViewById(R.id.btn_silent).setOnClickListener(sss);
    }


    @Override
    protected void onResume() {
        super.onResume();
        requestContext.onResume();
    }


    /**
     * Initializes all of the UI elements in the activity
     */
    private void initializeUI() {
        mLoginButton = findViewById(R.id.login_with_amazon);
        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doAuth(BuildConfig.PRODUCT_ID, com.ggec.voice.toollibrary.Util.getProductId(view.getContext()));
            }
        });
    }

    private void showAuthToast(String authToastMessage) {
        Toast authToast = Toast.makeText(getApplicationContext(), authToastMessage, Toast.LENGTH_LONG);
        authToast.setGravity(Gravity.CENTER, 0, 0);
        authToast.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode ==3001) {
            startAssistIfGetPermission();
        }

    }

    private void startAssistIfGetPermission(){
        if(PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this, RECORD_AUDIO)){
            // start service
            startService(new Intent(this, AssistService.class));
        } else {
            ActivityCompat.requestPermissions(this, new String[]{RECORD_AUDIO}, 3001);
        }

        if(PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION))
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 3002);
    }

    private void doAuth(String productId, String dsn){
        final JSONObject scopeData = new JSONObject();
        final JSONObject productInstanceAttributes = new JSONObject();
        try {
            productInstanceAttributes.put("deviceSerialNumber", dsn);
            scopeData.put("productInstanceAttributes", productInstanceAttributes);
            scopeData.put("productID", productId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Scope scope = ScopeFactory.scopeNamed("alexa:all", scopeData);
        String challenge = getCodeChallenge(this);
        AuthorizeRequest authorizeRequest = new AuthorizeRequest
                .Builder(requestContext)
                .addScopes(/*ProfileScope.profile(), ProfileScope.postalCode(),*/ scope)
                .forGrantType(AuthorizeRequest.GrantType.AUTHORIZATION_CODE)
                .shouldReturnUserData(false)
                .withProofKeyParameters(challenge, "S256")
                .build();

        Log.d(TAG, "auth alexa , challenge:"+challenge);
        AuthorizationManager.authorize(authorizeRequest);
    }

    /**
     * Create a String hash based on the code verifier, this is used to verify the Token exchanges
     * @return
     */
    public static String getCodeChallenge(Context context){
        String verifier = getCodeVerifier(context);
        return base64UrlEncode(getHash(verifier));
    }


    /**
     * Encode a byte array into a string, while trimming off the last characters, as required by the Amazon token server
     *
     * See: http://brockallen.com/2014/10/17/base64url-encoding/
     *
     * @param arg our hashed string
     * @return a new Base64 encoded string based on the hashed string
     */
    public static String base64UrlEncode(byte[] arg)
    {
        String s = Base64.encodeToString(arg, 0); // Regular base64 encoder
        s = s.split("=")[0]; // Remove any trailing '='s
        s = s.replace('+', '-'); // 62nd char of encoding
        s = s.replace('/', '_'); // 63rd char of encoding
        return s;
    }

    /**
     * Hash a string based on the SHA-256 message digest
     * @param password
     * @return
     */
    public static byte[] getHash(String password) {
        MessageDigest digest=null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
        }
        digest.reset();
        byte[] response = digest.digest(password.getBytes());

        return response;
    }

    /**
     * Return our stored code verifier, which needs to be consistent, if this doesn't exist, we create a new one and store the new result
     * @return the String code verifier
     */
    private static String getCodeVerifier(Context context){
        if(SharedPreferenceUtil.contains(context, com.willblaschko.android.alexa.AuthorizationManager.CODE_VERIFIER)){
            return SharedPreferenceUtil.getStringByKey(context, com.willblaschko.android.alexa.AuthorizationManager.CODE_VERIFIER, "");
        }

        //no verifier found, make and store the new one
        String verifier = com.willblaschko.android.alexa.AuthorizationManager.createCodeVerifier();
        Util.getPreferences(context).edit().putString( com.willblaschko.android.alexa.AuthorizationManager.CODE_VERIFIER, verifier).apply();
        return verifier;
    }

}

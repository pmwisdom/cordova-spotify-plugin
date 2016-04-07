package com.pmwisdom.spotify;

import android.content.Intent;
import android.os.Debug;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.PlayerStateCallback;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;

import java.net.URL;

/**
 * Created by Paul on 4/4/2016.
 */
public class CDVSpotify extends CordovaPlugin implements
        PlayerNotificationCallback, ConnectionStateCallback{
    private static final String TAG = "CDVSpotify";

    private static final String ACTION_LOGIN = "login";
    private static final String ACTION_PLAY = "play";
    private static final String ACTION_PAUSE = "pause";
    private static final String ACTION_RESUME = "resume";

    private static final int REQUEST_CODE = 1337;

    private String clientId;
    private String redirectUri;


    private CallbackContext loginCallback;
    private String currentAccessToken;
    private Boolean isLoggedIn = false;

    private Player currentPlayer;


    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        Log.i(TAG, "Initializing...");

        int clientResId = cordova.getActivity().getResources().getIdentifier("client_id", "string", cordova.getActivity().getPackageName());
        int cbResId = cordova.getActivity().getResources().getIdentifier("redirect_uri", "string", cordova.getActivity().getPackageName());

        Log.i(TAG, "Client ID ID" + clientResId);
        Log.i(TAG, "cb ID ID" + cbResId);

        clientId = cordova.getActivity().getString(clientResId);
        redirectUri = cordova.getActivity().getString(cbResId) + "://callback";

        Log.i(TAG, "Set up local vars" + clientId + redirectUri);

        cordova.setActivityResultCallback(this);

    }

    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) {
        Boolean success = false;

        if(ACTION_LOGIN.equalsIgnoreCase(action)) {
            Log.i(TAG, "LOGIN");
            JSONArray scopes = new JSONArray();
            Boolean fetchTokenManually = false;

            try {
                scopes = data.getJSONArray(0);
                fetchTokenManually = data.getBoolean(1);
            } catch(JSONException e) {
                Log.e(TAG, e.toString());
            }

            cordova.setActivityResultCallback(this);
            loginCallback = callbackContext;
            this.login(scopes, fetchTokenManually);
            success = true;
        } else if(ACTION_PLAY.equalsIgnoreCase(action)) {
            String uri = "";

            try {
                 uri = data.getString(0);
            } catch(JSONException e) {
                Log.e(TAG, e.toString());
            }

            this.play(uri);
            success = true;
        } else if(ACTION_PAUSE.equalsIgnoreCase(action)) {
            this.pause();
            success = true;
        } else if(ACTION_RESUME.equalsIgnoreCase(action)) {
            this.resume();
            success = true;
        }


        return success;
    }

    //scopes -- self exaplanatory
    //manualMode -- If you set manualMode = true, the login process will generate a CODE instead of a TOKEN, so you can manually refresh and obtain a refresh
    //token. AGAIN : YOU MUST OBTAIN A ACCESS TOKEN MANUALLY IF you SET THiS TO TRUE
    private void login(JSONArray scopes, Boolean fetchTokenManually) {
        AuthenticationResponse.Type authType = fetchTokenManually ? AuthenticationResponse.Type.CODE :AuthenticationResponse.Type.TOKEN;


        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(clientId,
                authType,
                redirectUri);
        builder.setScopes(new String[]{"user-read-private", "streaming"});
        builder.setShowDialog(true);
        AuthenticationRequest request = builder.build();

        Log.d(TAG, "Client ID " + clientId + "AUTH RESPONSE TYPE " + AuthenticationResponse.Type.TOKEN + "REDIRECT URI " + redirectUri + " Scopes " + scopes + " manual " + fetchTokenManually);

        AuthenticationClient.openLoginActivity(cordova.getActivity(), REQUEST_CODE, request);
    }

    private void logout() {
        AuthenticationClient.clearCookies(cordova.getActivity());

        this.clearPlayerState();
        isLoggedIn = false;
        currentAccessToken = null;
    }

    private void clearPlayerState() {
        if(currentPlayer != null) {
            currentPlayer.pause();
            currentPlayer.logout();
        }

        currentPlayer = null;
    }

    private void play(String uri) {
        Log.i(TAG, "Play: Is logged in -" + isLoggedIn + "Current Access" + currentAccessToken + "Current player" + currentPlayer);
        if(clientId == null || isLoggedIn == false || currentAccessToken == null || currentPlayer == null) return;

        if(currentPlayer.isLoggedIn()) {
            Log.e(TAG, "Current Player is initialized but player is not logged in, set access token manually or call login with fetchTokenManually : false");
            return;
        }

        Log.i(TAG, "Playing URI: " + uri);

        currentPlayer.play(uri);
    }

    private void getPlayerState() {
        currentPlayer.getPlayerState(new PlayerStateCallback() {
            @Override
            public void onPlayerState(PlayerState playerState) {
                Log.i(TAG, "Player State" + playerState.toString());
            }
        });
    }

    private void pause() {
        if(clientId == null || isLoggedIn == false || currentAccessToken == null || currentPlayer == null) return;

        currentPlayer.pause();
    }

    private void resume() {
        currentPlayer.resume();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        Log.i(TAG, "ACTIVITY RESULT: ");
        Log.i(TAG, "Request Code " + requestCode);
        Log.i(TAG, "Result Code " + resultCode);

        JSONObject ret = new JSONObject();

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            switch(response.getType()) {
                case TOKEN :
                    isLoggedIn = true;
                    Log.i(TAG, "TOKEN " + response.getAccessToken() );

                    try {
                        ret.put("authToken", response.getAccessToken());
                    } catch(JSONException e) {
                        Log.e(TAG, e.getMessage());
                    }

                    currentAccessToken = response.getAccessToken();

                    loginCallback.success(ret);
                    loginCallback = null;
                    break;
                case CODE :
                    isLoggedIn = false;
                    Log.i(TAG, "RECEIVED CODE" + response.getCode());

                    try {
                        ret.put("authCode", response.getCode());
                    } catch(JSONException e) {
                        Log.e(TAG, e.getMessage());
                    }

                    loginCallback.success(ret);
                    loginCallback = null;
                    break;
                case ERROR :
                    Log.e(TAG, response.getError());
                    loginCallback.error(response.getError());
                    break;
            }
        }

        if(isLoggedIn) {
            Config playerConfig = new Config(cordova.getActivity(), currentAccessToken, clientId);

            Spotify.getPlayer(playerConfig, this, new Player.InitializationObserver() {
                @Override
                public void onInitialized(Player player) {
                    Log.i(TAG, "Player Initialized");
                    currentPlayer = player;
                }

                @Override
                public void onError(Throwable throwable) {
                    Log.e("MainActivity", "Could not initialize player: " + throwable.getMessage());
                }
            });
        }
    }

    @Override
    public void onLoggedIn() {
        Log.d("MainActivity", "User logged in");
    }

    @Override
    public void onLoggedOut() {
        Log.d("MainActivity", "User logged out");
    }

    @Override
    public void onLoginFailed(Throwable error) {
        Log.d("MainActivity", "Login failed");
    }

    @Override
    public void onTemporaryError() {
        Log.d("MainActivity", "Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d("MainActivity", "Received connection message: " + message);
    }

    @Override
    public void onPlaybackEvent(EventType eventType, PlayerState playerState) {
        Log.d("MainActivity", "Playback event received: " + eventType.name());
    }

    @Override
    public void onPlaybackError(ErrorType errorType, String errorDetails) {
        Log.d("MainActivity", "Playback error received: " + errorType.name());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}

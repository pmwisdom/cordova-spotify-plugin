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

    // TODO: Replace with your client ID
    private String clientId;
    // TODO: Replace with your redirect URI
    private String redirectUri = "com.pmwisdom.spotify.AuthCallback://callback";


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

            this.login();
            loginCallback = callbackContext;
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

    private void login() {
        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(clientId,
                AuthenticationResponse.Type.TOKEN,
                redirectUri);
        builder.setScopes(new String[]{"user-read-private", "streaming"});
        AuthenticationRequest request = builder.build();

        Log.d(TAG, "Client ID " + clientId + "AUTH RESPONSE TYPE " + AuthenticationResponse.Type.TOKEN + "REDIRECT URI " +  redirectUri);

        AuthenticationClient.openLoginActivity(cordova.getActivity(), REQUEST_CODE, request);
    }

    private void play(String uri) {
        if(clientId == null || isLoggedIn == false || currentAccessToken == null || currentPlayer == null) return;

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

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            switch(response.getType()) {
                case TOKEN :
                    isLoggedIn = true;
                    Log.i(TAG, "TOKEN " + response.getAccessToken() );
                    currentAccessToken = response.getAccessToken();
                    loginCallback.success(response.getAccessToken());

                    Config playerConfig = new Config(cordova.getActivity(), currentAccessToken, clientId);

                    Spotify.getPlayer(playerConfig, this, new Player.InitializationObserver() {
                        @Override
                        public void onInitialized(Player player) {
                            currentPlayer = player;
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            Log.e("MainActivity", "Could not initialize player: " + throwable.getMessage());
                        }
                    });

                    break;
                case ERROR :
                    Log.e(TAG, response.getError());
                    loginCallback.error(response.getError());
                    break;
            }
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

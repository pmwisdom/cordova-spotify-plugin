# cordova-spotify-plugin
Cordova Spotify Integration for Android [Simple API]

## NOTE: I am not currently working on this, nor do I plan to in the future. I've put this here so anyone that wants to continue my work can. If you have a problem with the plugin you're on your own. I will accept PR's. 


NOTE: ANDROID ONLY (for now)
I wanted a really simple way to A) login to spotify natively and B) play / pause / resume natively. There is another IOS plugin but I felt the api was too complicate for my uses. 

To Get Credentials:
  Coming soon...

To Install: 
````
cordova add https://github.com/pmwisdom/cordova-spotify-plugin.git --variable CLIENT_ID_ANDROID="yourandroidclientid" --variable REDIRECT_URI_ANDROID="yourandroidredirecturi"
````

###To Use:###

````
var Spotify = window.plugins.spotify;
````

Login : 
````
var options = {
  scopes: []
};

//Log in natively
Spotify.login(options, success, failure);
````

Play : 
````
//you can give it any spotify URI
Spotify.play("spotify:track:7oXRMDUzBPekkLRTJhSGvC");
````

Pause : 
````
Spotify.pause();
````

Resume: 
````
Spotify.resume();
````

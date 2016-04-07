var exec = require('cordova/exec');

module.exports = {
  login : function(options, success, failure) {
    var scopes = options && options.scopes || ["user-read-private", "streaming"];
    var fetchTokenManually = options && options.fetchTokenManually || false;


    var successCB = success || function() {},
      failureCB = failure || function() {}

    exec(
      successCB,
      failureCB,
      "CDVSpotify",
      "login",
      [scopes, fetchTokenManually]
    )
  },
  logout : function(success) {

    exec(
      success || function() {},
      function() {},
      "CDVSpotify",
      "logout",
      []
    )
  }
}
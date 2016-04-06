var exec = require('cordova/exec');

module.exports = {
  login : function() {
    exec(
      function() {},
      function() {},
      "CDVSpotify",
      "login",
      []
    )
  }
}
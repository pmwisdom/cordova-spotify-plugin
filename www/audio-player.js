var exec = require('cordova/exec');

module.exports = {
  play : function(songId) {
    exec(
      function() {},
      function() {},
      "CDVSpotify",
      "play",
      [songId]
    )
  },
  pause : function() {
    exec(
      function() {},
      function() {},
      "CDVSpotify",
      "pause",
      []
    )
  },
  resume : function() {
    exec(
      function() {},
      function() {},
      "CDVSpotify",
      "resume",
      []
    )
  }
};
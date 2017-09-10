// Saving support for TiddlyWiki5

(function() {

"use strict";

var path = app.currentPath();

// Hook into TiddlyWiki's saving mechanism
enableSaving();

function enableSaving() {
    // Create the message box where TiddlyWiki will post messages
    var messageBox = document.getElementById("tiddlyfox-message-box");
    if(!messageBox) {
        messageBox = document.createElement("div");
        messageBox.id = "tiddlyfox-message-box";
        messageBox.style.display = "none";
        document.body.appendChild(messageBox);
    }
    // Listen for save events
    messageBox.addEventListener("tiddlyfox-save-file", onSave,false);
}

function onSave(event) {
    // This is the message element posted to the message box.
    var message = event.target;

    // Cache the message arguments
    var content = message.getAttribute("data-tiddlyfox-content");

    // Remove the message form the message box
    message.parentNode.removeChild(message);

    // Use the app's JavaScript interface to write the file
    var success = app.write(path, content);
    if(success) {
        // Notify TiddlyWiki after successful processing
        confirmSave(message, path);
    }

    // Disable default behaviour
    event.preventDefault ? event.preventDefault() : event.returnValue = false;
    return false;
}

function confirmSave(message, path) {
    var event = document.createEvent("Events");
    event.initEvent("tiddlyfox-have-saved-file", true, false);
    event.savedFilePath = path;
    message.dispatchEvent(event);
}

})();

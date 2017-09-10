// Extracts the application-name meta value from the document

(function() {

"use strict";

var tag = document.head.querySelector("meta[name=application-name]");
if(tag) {
    var value = tag.getAttribute("content");
    if(value) {
        return value;
    }
}
return "";

})();

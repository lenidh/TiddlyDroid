// Extracts the application-name meta value from the document

(function() {

"use strict";

var result = new Object();
result.title = getTitle();
result.subtitle = getSubtitle();
return result;

function getTitle() {
    var tag = document.querySelector("div[title=\"$:/SiteTitle\"] pre");
    return tag ? tag.textContent : ""
}

function getSubtitle() {
    var tag = document.querySelector("div[title=\"$:/SiteSubtitle\"] pre");
    return tag ? tag.textContent : ""
}

})();

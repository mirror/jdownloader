package org.jdownloader.api.toolbar.specialurls;

public class YouTubeSpecialUrlHandling {

    public static String handle(String url) {

        String ret = " <button onclick=\";return false;\" title=\"Narf\" type=\"button\" class=\"yt-uix-tooltip-reverse yt-uix-button yt-uix-button-default yt-uix-tooltip\" id=\"watch-share\" data-button-action=\"neeee\" role=\"button\" data-tooltip-text=\"Aber sicher\"><span class=\"yt-uix-button-content\">Herunterladen </span></button>";

        return "document.getElementById('watch-headline-user-info').innerHTML+='" + ret + "';";
    }
}

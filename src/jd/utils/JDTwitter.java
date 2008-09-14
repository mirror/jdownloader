package jd.utils;

import java.io.IOException;

import jd.http.Browser;

public class JDTwitter {

    public static String RefreshTwitterMessage() {

        long onedayback = System.currentTimeMillis() - 1000 * 60 * 60 * 24;
        Browser br = new Browser();
        String status = null;
        try {
            br.getPage(JDLocale.L("main.twitter.url", "http://twitter.com/statuses/user_timeline/jdownloader.xml") + "?count=1&since=" + JDUtilities.formatTime(onedayback));
            status = br.getRegex("<status>[\\s\\S]*?<text>(.*?)</text>[\\s\\S]*?</status>").getMatch(0);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (status.matches(".*defaultmessage.*")) status = "";

        if ((status == null) || (status == "")) {
            status = JDLocale.L("sys.message.welcome", "Welcome to JDownloader");
        } else {
            if (status.length() > 70) status = status.substring(0, 70) + "...";
        }

        return status;
    }

}
//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.utils;

import java.io.IOException;
import java.util.logging.Logger;

import jd.http.Browser;

public class JDTwitter {
    
    private static Logger logger = JDUtilities.getLogger();
    
    public static String RefreshTwitterMessage() {

        long onedayback = System.currentTimeMillis() - 1000 * 60 * 60 * 24;
        Browser br = new Browser();
        String status = null;
        try {
            br.getPage(JDLocale.L("main.twitter.url", "http://twitter.com/statuses/user_timeline/jdownloader.xml") + "?count=1&since=" + JDUtilities.formatTime(onedayback));
            status = br.getRegex("<status>[\\s\\S]*?<text>(.*?)</text>[\\s\\S]*?</status>").getMatch(0);

        } catch (IOException e) {
                logger.warning("twitter.com unreachable. This doesnt affect your Downloads, though it could be a clue that your internet connection is down.");
        }
     

        if ((status == null) || (status == "")) {
            status = JDLocale.L("sys.message.welcome", "Welcome to JDownloader");
        } else {
            if (status.matches(".*defaultmessage.*")) status = "";
            if (status.length() > 70) status = status.substring(0, 70) + "...";
        }

        return status;
    }

}
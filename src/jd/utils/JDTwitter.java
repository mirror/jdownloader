package jd.utils;

import java.io.IOException;

import jd.http.Browser;

public class JDTwitter {

    public static String RefreshTwitterMessage() {
    	
    	long onedayback = System.currentTimeMillis();
    	onedayback = onedayback - 1000*60*60*24;
    	//System.out.println(JDUtilities.formatTime(onedayback));
    	Browser br = new Browser();
    	String status = null;
    	try { 
			br.getPage(JDLocale.L("main.twitter.url", "http://twitter.com/statuses/user_timeline/jdownloader.xml") + "?count=1&since="+JDUtilities.formatTime(onedayback));
			//System.out.println(br);
			status = br.getRegex("<status>[\\s\\S]*?<text>(.*?)</text>[\\s\\S]*?</status>").getMatch(0);

			//System.out.println(status);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if(status.matches(".*defaultmessage.*")){ status = ""; }
		
    	if((status==null)||(status=="")){status = JDLocale.L("sys.message.welcome", "Welcome to JDownloader");}
    	else {if(status.length() > 70) { status = status.substring(0, 70) + "..." ;}}
    	
    	
    	return status;
    }
	
}
package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.http.Browser;
import jd.http.GetRequest;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;

public class YouPornCom extends PluginForDecrypt {
	private static final String CODER = "JD-Team";
	private static final String HOST = "youporn.com";
	private static final Pattern patternSupported = Pattern.compile("(http://[\\w\\.]*?youporn\\.com/watch/[\\d]{3,9}/.*/)|(http://[\\w\\.]*?youporn\\.com/\\?page=[\\d]{1,3})", Pattern.CASE_INSENSITIVE);
	private static final Pattern DOWNLOADFILE =  Pattern.compile("download\\.youporn\\.com/download/[\\d]{3,9}/flv/[^\\?]*", Pattern.CASE_INSENSITIVE);
	private static final Pattern PAGEENTRY =  Pattern.compile("<h1><a href=\"/watch/[\\d]{3,9}/.*/[^\">.*\\</a></h1>]", Pattern.CASE_INSENSITIVE);
	
	@Override
	public ArrayList<DownloadLink> decryptIt(String parameter) {
		RequestInfo loader;
		ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
		if(new Regex(parameter, Pattern.compile("http://[\\w\\.]*?youporn\\.com/\\?page=[\\d]{1,3}")).count() == 1) {
			try {
				loader = HTTP.getRequest(new URL(parameter), "age_check=1", "", true);
				String[] matches = new Regex(loader.getHtmlCode(), Pattern.compile("<h1><a href=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE)).getMatches(1);
				for(String link : matches) {
						DownloadLink dlink = this.createDownloadlink("http://youporn.com" + link);
						dlink.setBrowserUrl(parameter);
						decryptedLinks.add(dlink);
				}
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			try {
				loader = HTTP.getRequest(new URL(parameter), "age_check=1", "", true);
				String matches = new Regex(loader.getHtmlCode(), DOWNLOADFILE).getFirstMatch();
				DownloadLink dlink = this.createDownloadlink("httpviajd://" + matches);
				dlink.setBrowserUrl(parameter);
				decryptedLinks.add(dlink);
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
        
        return decryptedLinks;
	}

	@Override
	public boolean doBotCheck(File file) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getCoder() {
		// TODO Auto-generated method stub
		return CODER;
	}

	@Override
	public String getHost() {
		// TODO Auto-generated method stub
		return HOST;
	}

	@Override
	public String getPluginName() {
		// TODO Auto-generated method stub
		return HOST;
	}

	@Override
	public Pattern getSupportedLinks() {
		// TODO Auto-generated method stub
		return patternSupported;
	}

	@Override
	public String getVersion() {
		// TODO Auto-generated method stub
		String ret = new Regex("$Revision: 2107 $", "\\$Revision: ([\\d]*?) \\$").getFirstMatch();
        return ret == null ? "0.0" : ret;
	}

}

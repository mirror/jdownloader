package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import jd.config.Configuration;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;
import jd.utils.JDUtilities;

public class YouPornCom extends PluginForHost {
	private static final String CODER = "JD-Team";
	private static final String HOST = "youporn.com";
	private static final Pattern patternSupported =  Pattern.compile("http://download\\.youporn\\.com/download/[\\d]{3,9}/flv/.*", Pattern.CASE_INSENSITIVE);
	
	@Override
	public String getAGBLink() {
		// TODO Auto-generated method stub
		return "http://youporn.com/terms";
	}

	@Override
	public boolean getFileInformation(DownloadLink parameter) {
		try {
			requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(parameter.getDownloadURL()), null, null, true);
			HTTPConnection urlConnection = requestInfo.getConnection();
			parameter.setName(getFileNameFormHeader(urlConnection));
			parameter.setDownloadMax(urlConnection.getContentLength());
			return true;
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		parameter.getLinkStatus().addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
		return false;
	}

	@Override
	public int getMaxSimultanDownloadNum() {
		// TODO Auto-generated method stub
		return Integer.MAX_VALUE;
	}

	@Override
	public void handleFree(DownloadLink link) throws Exception {
        if (!getFileInformation(link)) {
            link.getLinkStatus().addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;
        }

        requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(link.getDownloadURL()), null, null, true);
        HTTPConnection urlConnection = requestInfo.getConnection();        
        if (urlConnection.getContentLength() == 0) {
            link.getLinkStatus().addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            return;
        }

        dl = new RAFDownload(this, link, urlConnection);
        dl.setChunkNum(1);
        dl.setResume(false);
        dl.startDownload();
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
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
		String ret = new Regex("$Revision: 2070 $", "\\$Revision: ([\\d]*?) \\$").getFirstMatch();
        return ret == null ? "0.0" : ret;
	}

}

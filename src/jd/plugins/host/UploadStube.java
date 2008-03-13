package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import jd.plugins.Download;
import jd.plugins.DownloadLink;
import jd.plugins.HTTPConnection;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.Regexp;

public class UploadStube extends PluginForHost {
	private static final String HOST = "uploadstube.de";
	private static final String VERSION = "1.0.0.0";
	static private final Pattern patternSupported = Pattern.compile(
			"http://.*?uploadstube\\.de/download.php\\?file=.*",
			Pattern.CASE_INSENSITIVE);

	//
	@Override
	public boolean doBotCheck(File file) {
		return false;
	} // kein BotCheck

	@Override
	public String getCoder() {
		return "JD-Team";
	}

	@Override
	public String getPluginName() {
		return HOST;
	}

	@Override
	public String getHost() {
		return HOST;
	}

	@Override
	public String getVersion() {
		return VERSION;
	}

	@Override
	public String getPluginID() {
		return HOST + "-" + VERSION;
	}

	@Override
	public Pattern getSupportedLinks() {
		return patternSupported;
	}

	public UploadStube() {
		super();
		steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
	}

	public PluginStep doStep(PluginStep step, DownloadLink downloadLink) {
		if (aborted) {
			logger.warning("Plugin abgebrochen");
			downloadLink.setStatus(DownloadLink.STATUS_TODO);
			step.setStatus(PluginStep.STATUS_TODO);
			return step;
		}
		try {
			
			requestInfo = getRequest(new URL(downloadLink
					.getDownloadURL()));
			String dlurl = new Regexp(requestInfo.getHtmlCode(), "onClick=\"window.location=..(http://www.uploadstube.de/.*?)..\">.;").getFirstMatch();
			if(dlurl==null)
			{
				logger.severe("Datei nicht gefunden");
				downloadLink
						.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
				step.setStatus(PluginStep.STATUS_ERROR);
				return step;
			}
			requestInfo=getRequestWithoutHtmlCode(new URL(dlurl), requestInfo.getCookie(), downloadLink
					.getDownloadURL(), true);
			HTTPConnection urlConnection = requestInfo.getConnection();
			downloadLink.setName(getFileNameFormHeader(urlConnection));
			downloadLink.setDownloadMax(urlConnection.getContentLength());
			if (!hasEnoughHDSpace(downloadLink)) {
				downloadLink.setStatus(DownloadLink.STATUS_ERROR_NO_FREE_SPACE);
				step.setStatus(PluginStep.STATUS_ERROR);
				return step;
			}
		    Download dl = new Download(this, downloadLink, urlConnection);
		  
            dl.startDownload();
			        
			    
			        
		return step;

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		 step.setStatus(PluginStep.STATUS_ERROR);
		 downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
		return step;
	}

	@Override
	public boolean getFileInformation(DownloadLink downloadLink) {
		try {
			requestInfo = getRequest(new URL(downloadLink
					.getDownloadURL()));
			downloadLink.setName(new Regexp(requestInfo.getHtmlCode(), "<b>Dateiname: </b>(.*?) <br>").getFirstMatch());


				try {
					String[] fileSize = new Regexp(requestInfo.getHtmlCode(), "<b>Dateigr..e:</b> ([0-9\\.]*) (.*?)<br>").getMatches()[0];
					double length = Double.parseDouble(fileSize[0]
							.trim());
					int bytes;
					String type = fileSize[1].toLowerCase();
					if (type.equalsIgnoreCase("kb")) {
						bytes = (int) (length * 1024);
					} else if (type.equalsIgnoreCase("mb")) {
						bytes = (int) (length * 1024 * 1024);
					} else {
						bytes = (int) length;
					}
					downloadLink.setDownloadMax(bytes);
				} catch (Exception e) {
				}
			return true;
		} catch (Exception e) {
			// TODO: handle exception
		}
		return false;
	}

	@Override
	public int getMaxSimultanDownloadNum() {
		return Integer.MAX_VALUE;
	}

	@Override
	public void reset() {
		// TODO Automatisch erstellter Methoden-Stub
	}

	@Override
	public void resetPluginGlobals() {
		// TODO Automatisch erstellter Methoden-Stub
	}

	@Override
	public String getAGBLink() {

		return "http://www.uploadstube.de/regeln.php";
	}
}

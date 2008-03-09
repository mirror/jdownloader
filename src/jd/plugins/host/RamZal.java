package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import java.util.regex.Pattern;

import jd.plugins.DownloadLink;
import jd.plugins.HTTPConnection;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;

public class RamZal extends PluginForHost {
	private static final String HOST = "ramzal.com";
	private static final String VERSION = "1.0.0.0";
	// http://ramzal.com//upload_files/1280838337_wallpaper-1280x1024-007.jpg
	static private final Pattern patternSupported = Pattern.compile(
			"http://.*?ramzal\\.com//?upload_files/.*",
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

	public RamZal() {
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
			requestInfo = getRequestWithoutHtmlCode(new URL(downloadLink
					.getDownloadURL()), null, null, true);

			HTTPConnection urlConnection = requestInfo.getConnection();
			downloadLink.setName(getFileNameFormHeader(urlConnection));
			downloadLink.setDownloadMax(urlConnection.getContentLength());
			if (!hasEnoughHDSpace(downloadLink)) {
				downloadLink.setStatus(DownloadLink.STATUS_ERROR_NO_FREE_SPACE);
				step.setStatus(PluginStep.STATUS_ERROR);
				return step;
			}
			if (download(downloadLink, urlConnection)==DOWNLOAD_SUCCESS) {
				step.setStatus(PluginStep.STATUS_DONE);
				downloadLink.setStatus(DownloadLink.STATUS_DONE);
				return step;
			} else if (aborted) {
				logger.warning("Plugin abgebrochen");
				downloadLink.setStatus(DownloadLink.STATUS_TODO);
				step.setStatus(PluginStep.STATUS_TODO);
			} else {
		
			
				step.setStatus(PluginStep.STATUS_ERROR);
			}
			return step;

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		step.setStatus(PluginStep.STATUS_ERROR);
		downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
		return step;
	}

	@Override
	public boolean getFileInformation(DownloadLink downloadLink) {
		try {
			requestInfo = getRequestWithoutHtmlCode(new URL(downloadLink
					.getDownloadURL()), null, null, true);
HTTPConnection urlConnection = requestInfo.getConnection();
			downloadLink.setName(getFileNameFormHeader(urlConnection));
			downloadLink.setDownloadMax(urlConnection.getContentLength());
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

		return "http://ramzal.com/";
	}
}

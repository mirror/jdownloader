package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Pattern;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.Regexp;

public class ArchivTo extends PluginForHost {
	private static final String HOST = "archiv.to";
	private static final String VERSION = "1.0.0.0";
	static private final Pattern patternSupported = Pattern.compile(
			"http://.*?archiv\\.to/\\?Module\\=Details\\&HashID\\=.*",
			Pattern.CASE_INSENSITIVE);

	//
	@Override
	public boolean doBotCheck(File file) {
		return false;
	} // kein BotCheck

	@Override
	public String getCoder() {
		return "GforE";
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

	public ArchivTo() {
		super();
		steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
	}

	public PluginStep doStep(PluginStep step, final DownloadLink downloadLink) {
		if (aborted) {
			logger.warning("Plugin abgebrochen");
			downloadLink.setStatus(DownloadLink.STATUS_TODO);
			step.setStatus(PluginStep.STATUS_TODO);
			return step;
		}
		try {
			String url = downloadLink.getDownloadURL();

			requestInfo = getRequestWithoutHtmlCode(new URL("http://archiv.to/Get/?System=Download&Hash="+new Regexp(url, ".*HashID=(.*)").getFirstMatch()), null, url, true);

			URLConnection urlConnection = requestInfo.getConnection();
			if(!getFileInformation(downloadLink)) {
				logger.severe("Datei nicht gefunden");
				downloadLink
						.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
				step.setStatus(PluginStep.STATUS_ERROR);
				return step;
			}
			final long length = downloadLink.getDownloadMax();
			downloadLink.setName(getFileNameFormHeader(urlConnection));
			//workaround
			new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					downloadLink.setDownloadMax((int) length);
					
				}}).start();
				
			if (!hasEnoughHDSpace(downloadLink)) {
				downloadLink.setStatus(DownloadLink.STATUS_ERROR_NO_FREE_SPACE);
				step.setStatus(PluginStep.STATUS_ERROR);
				return step;
			}
			if (download(downloadLink, urlConnection)) {
				step.setStatus(PluginStep.STATUS_DONE);
				downloadLink.setStatus(DownloadLink.STATUS_DONE);
				return null;
			} else if (aborted) {
				logger.warning("Plugin abgebrochen");
				downloadLink.setStatus(DownloadLink.STATUS_TODO);
				step.setStatus(PluginStep.STATUS_TODO);
			} else {
				logger.severe("Datei nicht gefunden");
				downloadLink
						.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
				step.setStatus(PluginStep.STATUS_ERROR);
			}

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return step;
	}
	// http://archiv.to/Get/?System=Download&Hash=FILE4799F3EC23328
	// http://archiv.to/?Module=Details&HashID=FILE4799F3EC23328
	@Override
	public boolean getFileInformation(DownloadLink downloadLink) {
		try {
			String url = downloadLink.getDownloadURL();
			requestInfo = getRequest(new URL(url));
			downloadLink.setName(new Regexp(requestInfo.getHtmlCode(), "<td class=\"DetailsRowValue\">: <a href=\"./Get/\\?System\\=Download.*?\">(.*?)</a>").getFirstMatch());
			downloadLink.setDownloadMax(Integer.parseInt(new Regexp(requestInfo.getHtmlCode(), "<td class=\"DetailsRowValue\">: ([\\d]*) Bytes").getFirstMatch()));
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

		return "http://archiv.to/?Module=Policy";
	}
}

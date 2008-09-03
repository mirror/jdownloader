package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import jd.config.Configuration;
import jd.http.HTTPConnection;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;
import jd.utils.JDUtilities;

public class MySpaceCom extends PluginForHost{
//	private static final Pattern PATTERN_SUPPORTET = Pattern.compile("http://cache\\d+-music\\d+.myspacecdn\\.com/\\d+/std_.+\\.mp3",Pattern.CASE_INSENSITIVE);
	private static final Pattern PATTERN_SUPPORTET = Pattern.compile("httpmyspace://.+",Pattern.CASE_INSENSITIVE);
	private static final String CODER = "ToKaM";
	private static final String HOST = "myspace.com";
	private static final String AGB_LINK  = "http://www.myspace.com/index.cfm?fuseaction=misc.terms";
	

    public MySpaceCom() {
		super();
	}
    private String getDownloadUrl(DownloadLink link){
    	return link.getDownloadURL().replaceAll("httpmyspace://", "");
    }
    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public String getAGBLink() {
        return AGB_LINK;
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
    	downloadLink.setUrlDownload(getDownloadUrl(downloadLink));
        try {
        	HTTPConnection urlConnection = br.openGetConnection(downloadLink.getDownloadURL());
            if (!urlConnection.isOK()) return false;
            downloadLink.setDownloadSize(urlConnection.getContentLength());
            return true;
		} catch (IOException e) {
			logger.severe(e.getMessage());
			downloadLink.getLinkStatus().setStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
			return false;
		}

    }

    @Override
    public String getPluginName() {
        return HOST;
    }

    @Override
    public Pattern getSupportedLinks() {
        return PATTERN_SUPPORTET;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision: 2588 $", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
    	downloadLink.setUrlDownload(getDownloadUrl(downloadLink));
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        /* Nochmals das File überprüfen */
        if (!getFileInformation(downloadLink)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;
        }
        HTTPConnection urlConnection = br.openGetConnection(downloadLink.getDownloadURL());

        if (urlConnection.getContentLength() == 0) {
            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
            return;
        }

        dl = new RAFDownload(this, downloadLink, urlConnection);
        dl.setChunkNum(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 2));
        dl.setResume(true);
        dl.startDownload();
    }

}

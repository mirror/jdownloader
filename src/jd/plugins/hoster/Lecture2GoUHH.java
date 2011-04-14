package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.Lctr2GoUHH;
import jd.utils.locale.JDL;

/**
 * {@link HostPlugin} zum herunterladen von .mp4 Dateien von
 * http://lecture2go.uni-hamburg.de<br>
 * Links zu den .mp4 Dateien werden von {@link Lctr2GoUHH} decrypted.
 * 
 * @author stonedsquirrel
 * @version 14.04.2011
 */
@HostPlugin(flags = { 0 }, interfaceVersion = 2, names = { "lecture2go.uni-hamburg.de" }, revision = "", urls = { "http://lecture2go\\.uni-hamburg\\.de/.*\\.mp4" })
public class Lecture2GoUHH extends PluginForHost {

    public Lecture2GoUHH(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://lecture2go.uni-hamburg.de/disclaimer";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws PluginException {
        this.setBrowserExclusive();
        this.br.getHeaders().put("Accept-Encoding", "");
        this.br.setFollowRedirects(true);
        URLConnectionAdapter urlConnection = null;
        try {
            urlConnection = br.openGetConnection(downloadLink.getDownloadURL());
            if (urlConnection.getResponseCode() == 404 || !urlConnection.isOK()) {
                urlConnection.disconnect();
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (downloadLink.getFinalFileName() == null) {
                downloadLink.setFinalFileName(Plugin.getFileNameFromHeader(urlConnection));
            }
            downloadLink.setDownloadSize(urlConnection.getLongContentLength());
            String contentType = urlConnection.getContentType();
            if (contentType.startsWith("text/html") && downloadLink.getBooleanProperty(DirectHTTP.TRY_ALL, false) == false) {
                return downloadLink.getAvailableStatus();
            } else {
                urlConnection.disconnect();
            }
            return AvailableStatus.TRUE;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br = new Browser();
        br.setFollowRedirects(true);
        br.setDebug(true);
        boolean resume = true;
        int chunks = 0;

        if (downloadLink.getBooleanProperty(DirectHTTP.NORESUME, false) || downloadLink.getBooleanProperty(DirectHTTP.FORCE_NORESUME, false)) {
            resume = false;
        }
        if (downloadLink.getBooleanProperty(DirectHTTP.NOCHUNKS, false) || downloadLink.getBooleanProperty(DirectHTTP.FORCE_NOCHUNKS, false) || resume == false) {
            chunks = 1;
        }
        if (downloadLink.getStringProperty("post", null) != null) {
            this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, downloadLink.getDownloadURL(), downloadLink.getStringProperty("post", null), resume, chunks);
        } else {
            this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, downloadLink.getDownloadURL(), resume, chunks);
        }
        if (!this.dl.startDownload()) {
            if (downloadLink.getLinkStatus().getErrorMessage() != null && downloadLink.getLinkStatus().getErrorMessage().startsWith(JDL.L("download.error.message.rangeheaders", "Server does not support chunkload"))) {
                if (downloadLink.getBooleanProperty(DirectHTTP.NORESUME, false) == false) {
                    downloadLink.setChunksProgress(null);
                    downloadLink.setProperty(DirectHTTP.NORESUME, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            } else {
                if (downloadLink.getBooleanProperty(DirectHTTP.NOCHUNKS, false) == false) {
                    downloadLink.setProperty(DirectHTTP.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        }
    }

    @Override
    public void reset() {
        // Leer?
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        link.setProperty(DirectHTTP.NORESUME, false);
        link.setProperty(DirectHTTP.NOCHUNKS, false);
    }
}

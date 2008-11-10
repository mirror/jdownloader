package jd.plugins.host;

import java.io.IOException;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.HTTPConnection;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

public class DlFreeFr extends PluginForHost {

    public DlFreeFr(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://dl.free.fr/";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        HTTPConnection con = br.openGetConnection(downloadLink.getDownloadURL());
        if (con.isContentDisposition()) {
            downloadLink.setFinalFileName(Plugin.getFileNameFormHeader(con));
            downloadLink.setDownloadSize(con.getContentLength());
            con.disconnect();
            return true;
        } else {
            br.followConnection();
        }
        String filename = br.getRegex(Pattern.compile("Fichier:</td>.*?<td.*?>(.*?)<", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        String filesize = br.getRegex(Pattern.compile("Taille:</td>.*?<td.*?>(.*?)soit", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (filename == null || filesize == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        downloadLink.setName(filename);
        downloadLink.setDownloadSize(Regex.getSize(filesize.replaceAll("Mo", "Mb")));
        return true;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        /* Nochmals das File überprüfen */
        getFileInformation(downloadLink);
        String dlLink = br.getRegex("<tr><td colspan=\"2\" style=\"text-align: center; border: 1px solid #ffffff\"><a style=\"text-decoration: underline\" href=\"(.*?free.*?fr.*?)\">").getMatch(0);
        if (dlLink == null) { throw new PluginException(LinkStatus.ERROR_FATAL); }
        br.setFollowRedirects(true);
        /* Datei herunterladen */
        dl = br.openDownload(downloadLink, dlLink, true, 1);
        if (!dl.getConnection().isContentDisposition()) {
            dl.getConnection().disconnect();
            if (br.getURL().contains("overload")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l); }
            throw new PluginException(LinkStatus.ERROR_FATAL);
        }
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }
}

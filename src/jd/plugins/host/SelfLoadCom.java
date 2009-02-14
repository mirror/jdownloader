package jd.plugins.host;

import java.io.IOException;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

public class SelfLoadCom extends PluginForHost {
    // Info: Hoster übergibt weder korrekten Dateinamen
    // (selbst wählbar über die aufrufende Url)
    // noch wird eine Dateigröße angegeben!
    public SelfLoadCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://self-load.com/impressum.html";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws PluginException, IOException {
        setBrowserExclusive();
        br.setDebug(true);
        br.getPage(downloadLink.getDownloadURL());
        br.getPage(downloadLink.getDownloadURL().replaceAll("self-load.com", "heiker.pro"));
        String filename = br.getRegex(Pattern.compile("<br>.*?<b>(.*?)</b>&nbsp;", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
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
        br.setFollowRedirects(true);
        Form form = br.getForm(0);
        if (form == null) throw new PluginException(LinkStatus.ERROR_FATAL);
        form.setAction("/redirect.php"); // *FIXME: ohne / wird eine falsche url
        // in der Form.getAction berechnet!
        // hoster schuld oder Form fixen?
        dl = br.openDownload(downloadLink, form, false, 1);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

}

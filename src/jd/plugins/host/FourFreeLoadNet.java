package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

public class FourFreeLoadNet extends PluginForHost {

    private String captchaCode;
    private String passCode = null;

    public FourFreeLoadNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public String getAGBLink() {
        return "http://4freeload.net/rules.php";
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCookie("http://4freeload.net", "yab_mylang", "de");
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("konnte leider nicht gefunden")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex(Pattern.compile("Dateiname:</b></td>\\s+<td[^>]*>(.*?)</td>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        String filesize = br.getRegex(Pattern.compile("Dateigr[^<]*</b></td>\\s+<td[^>]*>(.*?)</td>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (filename == null || filesize == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize.trim()));
        return AvailableStatus.TRUE;
    }

    // @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        // url = downloadLink.getDownloadURL();
        /* Nochmals das File überprüfen */
        requestFileInformation(downloadLink);

        File captchaFile = this.getLocalCaptchaFile();
        try {
            Browser.download(captchaFile, br.cloneBrowser().openGetConnection("http://4freeload.net/captcha.php"));
        } catch (Exception e) {
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        /* CaptchaCode holen */
        captchaCode = getCaptchaCode(captchaFile, downloadLink);
        Form form = br.getFormbyProperty("name", "myform");
        if (form == null) form = br.getForm(1);
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        form.setMethod(MethodType.POST);
        if (form.containsHTML("name=downloadpw")) {
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput(null, downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
            form.put("downloadpw", passCode);
        }

        /* Überprüfen(Captcha,Password) */
        form.put("captchacode", captchaCode);
        br.submitForm(form);
        if (br.containsHTML("Code fehler oder abgelaufen") || br.containsHTML("asswort")) {
            if (br.containsHTML("asswort")) {
                /* PassCode war falsch, also Löschen */
                downloadLink.setProperty("pass", null);
            }
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }

        /* Downloadlimit erreicht */
        String finalurl;
        if (br.containsHTML("Sie haben die max. Download") || br.containsHTML("Download Gr")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 30 * 60 * 1000l); }
        /* PassCode war richtig, also Speichern */
        downloadLink.setProperty("pass", passCode);
        finalurl = br.getRegex("document\\.location=\"(.*?)\"").getMatch(0);
        if (finalurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);

        /* 20 seks warten */
        sleep(20000, downloadLink);
        br.setFollowRedirects(true);
        /* Datei herunterladen */
        dl = br.openDownload(downloadLink, finalurl, false, 1);
        if (!dl.getConnection().isContentDisposition()) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dl.startDownload();
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    // @Override
    public void reset() {
    }

    // @Override
    public void resetPluginGlobals() {
    }

    // @Override
    public void reset_downloadlink(DownloadLink link) {
        // TODO Auto-generated method stub

    }
}

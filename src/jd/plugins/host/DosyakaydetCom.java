package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;

public class DosyakaydetCom extends PluginForHost {

    public DosyakaydetCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.dosyakaydet.com/index/p_faq/";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        String filename = br.getRegex(Pattern.compile("strong>Dosya Ad.*?</strong></div></td>.*?<td width=.*?><div align=\"left\" class=.*?>(.*?)<", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        String filesize = br.getRegex(Pattern.compile("<strong>Dosya Boyutu:</strong></div></td>.*?<td width=.*?><div align=\"left\" class=.*?>(.*?)<", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (filename == null || filesize == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize));
        return true;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        /* Nochmals das File überprüfen */
        // 15 Cpatcha versuche
        downloadLink.getLinkStatus().setStatusText(JDLocale.L("plugins.host.dosyakaydet.breakcaptcha", "Waiting for OCR"));
        downloadLink.requestGuiUpdate();
        for (int i = 0; i < 15; i++) {
            getFileInformation(downloadLink);
            String captchaUrl = br.getRegex("<img class=\"captchapict\" src=\"(.*?)\"").getMatch(0);
            File captchaFile = this.getLocalCaptchaFile(this);
            try {
                Browser.download(captchaFile, br.cloneBrowser().openGetConnection(captchaUrl));
            } catch (Exception e) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            String captchaCode = Plugin.getCaptchaCode(captchaFile, this, downloadLink);
            if (captchaCode == null) break;
            Form form = br.getForm(0);
            form.put("private_key", captchaCode);
            br.setFollowRedirects(false);
            br.submitForm(form);
            if (!br.containsHTML("Kod yanl&#305;&#351;. Tekrar deneyin:")) {
                break;

            }
        }
        String dlLink = br.getRedirectLocation();
        if (dlLink == null) {
            if (br.containsHTML("Kod yanl&#305;&#351;. Tekrar deneyin:")) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            } else {
                throw new PluginException(LinkStatus.ERROR_FATAL);
            }
        }
        br.setFollowRedirects(true);
        /* Datei herunterladen */
        dl = br.openDownload(downloadLink, dlLink, false, 1);
        if (!dl.getConnection().isContentDisposition()) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FATAL);
        }
        dl.startDownload();
    }

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

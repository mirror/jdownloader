//    jDownloader - Downloadmanager
//    Copyright (C) 2010  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.File;
import java.io.IOException;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "gigamax.ch" }, urls = { "http://(www\\.)?gigamax\\.ch/\\?d=[A-Z0-9]+" }, flags = { 0 })
public class GigaMaxCh extends PluginForHost {

    public GigaMaxCh(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium(COOKIE_HOST + "/service.php");
    }

    @Override
    public String getAGBLink() {
        return COOKIE_HOST + "/rules.php";
    }

    private static final String COOKIE_HOST     = "http://gigamax.ch";
    private static final int    DEFAULTWAITTIME = 130;

    // MhfScriptBasic 1.9
    // FREE limits: 1 * 1
    // PREMIUM limits: Chunks * Maxdls
    // Captchatype: reCaptcha
    // Other notes: modified filename/size regexes
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("(en|ru|fr|es|de)/file/", "file/"));
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie(COOKIE_HOST, "mfh_mylang", "en");
        br.setCookie(COOKIE_HOST, "yab_mylang", "en");
        br.getPage(parameter.getDownloadURL());
        if (br.getURL().contains("&code=DL_FileNotFound") || br.containsHTML("(Your requested file is not found|No file found)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<h1>([^<>\"]*?)</h1>").getMatch(0);
        String filesize = getData("File size");
        if (filename == null || filename.matches("")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setFinalFileName(filename.trim());
        if (filesize != null) parameter.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        requestFileInformation(downloadLink);
        if (br.containsHTML("value=\"Free Users\""))
            br.postPage(downloadLink.getDownloadURL(), "Free=Free+Users");
        else if (br.getFormbyProperty("name", "entryform1") != null) br.submitForm(br.getFormbyProperty("name", "entryform1"));
        final Browser ajaxBR = br.cloneBrowser();
        ajaxBR.getHeaders().put("X-Requested-With", "XMLHttpRequest");

        final String rcID = br.getRegex("challenge\\?k=([^<>\"]*?)\"").getMatch(0);
        if (rcID != null) {
            PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.setId(rcID);
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, downloadLink);
            ajaxBR.postPage(downloadLink.getDownloadURL(), "downloadverify=1&d=1&recaptcha_response_field=" + c + "&recaptcha_challenge_field=" + rc.getChallenge());
            if (ajaxBR.containsHTML("incorrect\\-captcha\\-sol")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        } else if (br.containsHTML(this.getHost() + "/captcha\\.php\"")) {
            final File cf = downloadCaptcha(getLocalCaptchaFile(), COOKIE_HOST + "/captcha.php");
            final String code = getCaptchaCode("mhfstandard", cf, downloadLink);
            ajaxBR.postPage(downloadLink.getDownloadURL(), "downloadverify=1&d=1&captchacode=" + code);
            if (ajaxBR.containsHTML("Captcha number error or expired")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        } else {
            ajaxBR.postPage(downloadLink.getDownloadURL(), "downloadverify=1&d=1");
        }
        final String reconnectWaittime = ajaxBR.getRegex("You must wait (\\d+) mins\\. for next download.").getMatch(0);
        if (reconnectWaittime != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(reconnectWaittime) * 60 * 1001l);
        if (ajaxBR.containsHTML("The allowed download sessions assigned to your IP is used up")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);
        final String finalLink = findLink(ajaxBR);
        if (finalLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        int wait = DEFAULTWAITTIME;
        String waittime = ajaxBR.getRegex("countdown\\((\\d+)\\);").getMatch(0);
        // Fol older versions it's usually skippable
        if (waittime == null) waittime = ajaxBR.getRegex("var timeout=\\'(\\d+)\\';").getMatch(0);
        if (waittime != null) wait = Integer.parseInt(waittime);
        // Waittime can be skipped
        // sleep(wait * 1001l, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finalLink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();

            if (br.containsHTML(">AccessKey is expired, please request")) throw new PluginException(LinkStatus.ERROR_FATAL, "FATAL server error, waittime skipped?");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private File downloadCaptcha(final File captchaFile, final String captchaAdress) throws Exception {
        final Browser br2 = br.cloneBrowser();
        URLConnectionAdapter con = null;
        try {
            Browser.download(captchaFile, con = br2.openGetConnection(captchaAdress));
        } catch (IOException e) {
            captchaFile.delete();
            throw e;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        return captchaFile;
    }

    private String findLink(final Browser br) throws Exception {
        return br.getRegex("(http://[a-z0-9\\-\\.]{5,30}/getfile\\.php\\?id=\\d+[^<>\"\\']*?)(\"|\\')").getMatch(0);
    }

    private String getData(final String data) {
        String result = br.getRegex("<strong>" + data + "</strong> <span class=\"violet\">([^<>\"]*?)<span>").getMatch(0);
        return result;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
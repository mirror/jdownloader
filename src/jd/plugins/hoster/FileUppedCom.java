//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.File;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fileupped.com" }, urls = { "http://(www\\.)?fileupped\\.com/((\\?d|download\\.php\\?id)=[A-Z0-9]+|((en|ru|fr|es)/)?file/[0-9]+/)" }, flags = { 0 })
public class FileUppedCom extends PluginForHost {

    public FileUppedCom(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium(COOKIE_HOST + "/register.php?g=3");
    }

    // MhfScriptBasic 1.4
    @Override
    public String getAGBLink() {
        return COOKIE_HOST + "/rules.php";
    }

    private static final String COOKIE_HOST   = "http://fileupped.com";
    private static final String IPBLOCKED     = "The allowed download sessions assigned to your IP is used up";
    private static final String RECAPTCHATEXT = "(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie(COOKIE_HOST, "mfh_mylang", "en");
        br.setCookie(COOKIE_HOST, "yab_mylang", "en");
        br.getPage(parameter.getDownloadURL());
        if (br.containsHTML("(name=\"uploadframe\" id=\"uploadframe\"|\"http://fileupped\\.com/cgi\\-bin/upload\\.cgi\\?)") || br.getURL().contains("&code=DL_FileNotFound") || br.containsHTML("(Your requested file is not found|No file found)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<div class=\"content_header_middle widebox_outer_width\">[\t\n\r ]+<h2 class=\"float\\-left\">([^<>\"]*?)</h2>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
        }
        String filesize = br.getRegex(">File size:</b></td>[\t\n\r ]+<td align=left>([^<>\"]*?)</td>").getMatch(0);
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
        String passCode = null;
        Form captchaform = br.getFormbyProperty("name", "verifyform");
        if (br.containsHTML("class=textinput name=downloadpw") || br.containsHTML(RECAPTCHATEXT)) {
            if (captchaform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            boolean captchaActive = false;
            for (int i = 0; i <= 3; i++) {
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                if (br.containsHTML(RECAPTCHATEXT) || captchaActive) {
                    if (!captchaActive) {
                        logger.info("Found reCaptcha");
                        rc.parse();
                        captchaActive = true;
                    }
                    rc.load();
                    File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    captchaform.put("recaptcha_challenge_field", rc.getChallenge());
                    captchaform.put("recaptcha_response_field", getCaptchaCode(cf, downloadLink));
                }
                if (br.containsHTML("class=textinput name=downloadpw")) {
                    if (downloadLink.getStringProperty("pass", null) == null) {
                        passCode = Plugin.getUserInput("Password?", downloadLink);

                    } else {
                        /* gespeicherten PassCode holen */
                        passCode = downloadLink.getStringProperty("pass", null);
                    }
                    captchaform.put("downloadpw", passCode);
                }
                br.submitForm(captchaform);
                if (br.containsHTML("Password Error")) {
                    logger.warning("Wrong password!");
                    downloadLink.setProperty("pass", null);
                    continue;
                }
                if (br.containsHTML(IPBLOCKED)) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 10 * 60 * 1001l);
                if (br.containsHTML("incorrect\\-captcha\\-sol") || br.containsHTML(RECAPTCHATEXT)) {
                    logger.warning("Wrong captcha or wrong password!");
                    downloadLink.setProperty("pass", null);
                    rc.reload();
                    continue;
                }
                break;
            }
        }
        if (br.containsHTML(IPBLOCKED)) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 10 * 60 * 1001l);
        if (br.containsHTML("Password Error")) {
            logger.warning("Wrong password!");
            downloadLink.setProperty("pass", null);
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (br.containsHTML("incorrect\\-captcha\\-sol") || br.containsHTML(RECAPTCHATEXT)) {
            logger.warning("Wrong captcha or wrong password!");
            downloadLink.setProperty("pass", null);
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        String finalLink = findLink();
        if (finalLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        int wait = 15;
        final String waittime = br.getRegex("countdown\\((\\d+)\\);").getMatch(0);
        if (waittime != null) wait = Integer.parseInt(waittime);
        sleep(wait * 1001l, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finalLink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String findLink() throws Exception {
        String finalLink = br.getRegex("(http://.{5,30}getfile\\.php\\?id=\\d+[^<>\"\\']*?)(\"|\\')").getMatch(0);
        if (finalLink == null) {
            String[] sitelinks = HTMLParser.getHttpLinks(br.toString(), null);
            if (sitelinks == null || sitelinks.length == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            for (String alink : sitelinks) {
                alink = Encoding.htmlDecode(alink);
                if (alink.contains("access_key=") || alink.contains("getfile.php?")) {
                    finalLink = alink;
                    break;
                }
            }
        }
        return finalLink;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return false;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

}

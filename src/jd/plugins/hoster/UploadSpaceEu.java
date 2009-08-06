//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.pluginUtils.Recaptcha;

//uploadspace by pspzockerscene
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uploadspace.eu" }, urls = { "http://[\\w\\.]*?uploadspace\\.eu/[a-z|0-9]+/.+" }, flags = { 0 })
public class UploadSpaceEu extends PluginForHost {

    public UploadSpaceEu(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://uploadspace.eu/tos.html";
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCookie("http://www.uploadspace.eu", "lang", "english");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("No such file")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("No such user exist")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("File Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("Filename:</b></td><td nowrap>(.*?)</td></tr>").getMatch(0);
        String filesize = br.getRegex("Size:</b></td><td>.*? <small>\\((.*?)\\)</small>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename);
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    // @Override
    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        if (br.containsHTML("is already downloading a file")) {                
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 10 * 60 * 1001l);
        }
        if (br.containsHTML("You have reached the download limit")) {
            int minutes = 0, seconds = 0, hours = 0;
            String tmphrs = br.getRegex("\\s+(\\d+)\\s+hours?").getMatch(0);
            if (tmphrs != null) hours = Integer.parseInt(tmphrs);
            String tmpmin = br.getRegex("\\s+(\\d+)\\s+minutes?").getMatch(0);
            if (tmpmin != null) minutes = Integer.parseInt(tmpmin);
            String tmpsec = br.getRegex("\\s+(\\d+)\\s+seconds?").getMatch(0);
            if (tmpsec != null) seconds = Integer.parseInt(tmpsec);
            int waittime = ((3600 * hours) + (60 * minutes) + seconds + 1) * 1000;
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, waittime);
        } else {
            // Form um auf "Datei herunterladen" zu klicken
            Form DLForm = br.getFormbyProperty("name", "F1");
            if (DLForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
// anderer Teil der Passwort-Handlings
//            String passCode = null;
//            if (br.containsHTML("valign=top><b>Password:</b></td>")) {
//                if (downloadLink.getStringProperty("pass", null) == null) {
//                    passCode = Plugin.getUserInput("Password?", downloadLink);
//                } else {
//                    /* gespeicherten PassCode holen */
//                    passCode = downloadLink.getStringProperty("pass", null);
//                }
//                DLForm.put("password", passCode);
//            }
            Recaptcha rc = new Recaptcha(br);
            rc.parse();
            String k = br.getRegex("src=\"http://api.recaptcha.net/challenge\\?k=(.*?)\\+\" type").getMatch(0);
            if (k != null) {
                /* recaptcha */

                Browser rcBr = br.cloneBrowser();
                rcBr.getPage("http://api.recaptcha.net/challenge?k=" + k);
                String challenge = rcBr.getRegex("challenge : '(.*?)',").getMatch(0);
                String server = rcBr.getRegex("server : '(.*?)',").getMatch(0);
                String captchaAddress = server + "image?c=" + challenge;
                File captchaFile = this.getLocalCaptchaFile();
                Browser.download(captchaFile, rcBr.openGetConnection(captchaAddress));
                String code = getCaptchaCode(captchaFile, downloadLink);
                // if (code == null) continue;
                DLForm.put("recaptcha_challenge_field", challenge);
                DLForm.put("recaptcha_response_field", code);
            }
            int tt = Integer.parseInt(br.getRegex("countdown\">(\\d+)</span>").getMatch(0));
            sleep(tt * 1001, downloadLink);
            jd.plugins.BrowserAdapter.openDownload(br,downloadLink, DLForm, false, 1);
            if (!(dl.getConnection().isContentDisposition())) {
                br.followConnection();
                if (br.containsHTML("Wrong captcha")) { throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
            }
//Passwort-Handling, aber da ich keine Files mit PW gefunden hab ist das halt auskommentiert
//                if (br.containsHTML("Wrong password")) {
//                    logger.warning("Wrong password!");
//                    downloadLink.setProperty("pass", null);
//                    throw new PluginException(LinkStatus.ERROR_RETRY);
//                }
//            }
//            if (passCode != null) {
//                downloadLink.setProperty("pass", passCode);
//            }

            dl.startDownload();
        }
    }

    @Override
    public void reset() {
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
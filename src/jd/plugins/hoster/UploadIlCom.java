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
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "upload-il.com" }, urls = { "http://[\\w\\.]*?upload-il\\.com(/.{2})?/[A-Z0-9]+" }, flags = { 0 })
public class UploadIlCom extends PluginForHost {

    public UploadIlCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    // MhfScriptBasic 1.0, added filename/size regexes, added waittime handling
    @Override
    public String getAGBLink() {
        return COOKIE_HOST + "/rules.php";
    }

    private static final String COOKIE_HOST = "http://upload-il.com";

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL() + "?setlang=en");
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie(COOKIE_HOST, "mfh_mylang", "en");
        br.setCookie(COOKIE_HOST, "yab_mylang", "en");
        br.getPage(parameter.getDownloadURL());
        if (br.containsHTML("Your requested file is not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<b>File name:</b></td>.*?<td align=.*?width=.*?>(.*?)</td>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("\"Click this to report for(.*?)\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("content=\"(.*?), The best file hosting service").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("<b>File name:</b></td>[\n\r\t ]+<td class=\"data\" dir=\"ltr\">(.*?)</td>").getMatch(0);
                    if (filename == null) {
                        filename = br.getRegex("\"http://upload-il\\.com/en/report\\.php\\?id=[A-Z0-9]+\" target=\"_blank\" title=\"Report abuse: (.*?)\"").getMatch(0);
                        if (filename == null) {
                            filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
                        }
                    }
                }
            }
        }
        String filesize = br.getRegex("<b>File size:</b></td>.*?<td align=.*?>(.*?)</td>").getMatch(0);
        if (filesize == null) filesize = br.getRegex("<b>File size:</b></td>[\n\r\t ]+<td class=\"data\">(.*?)</td>").getMatch(0);
        if (filename == null || filename.matches("")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setFinalFileName(filename.trim());
        if (filesize != null) parameter.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        requestFileInformation(link);
        if (br.containsHTML("value=\"Free Users\"")) br.postPage(link.getDownloadURL(), "Free=Free+Users");
        String passCode = null;
        Form captchaform = br.getFormbyProperty("name", "myform");
        if (captchaform == null) {
            captchaform = br.getFormbyProperty("name", "validateform");
            if (captchaform == null) {
                captchaform = br.getFormbyProperty("name", "valideform");
            }
        }
        if (br.containsHTML("(captcha.php|class=textinput name=downloadpw)")) {
            if (captchaform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            for (int i = 0; i <= 3; i++) {
                if (br.containsHTML("captcha.php")) {
                    String captchaurl = COOKIE_HOST + "/captcha.php";
                    String code = getCaptchaCode(captchaurl, link);
                    captchaform.put("captchacode", code);
                }
                if (br.containsHTML("class=textinput name=downloadpw")) {
                    if (link.getStringProperty("pass", null) == null) {
                        passCode = Plugin.getUserInput("Password?", link);

                    } else {
                        /* gespeicherten PassCode holen */
                        passCode = link.getStringProperty("pass", null);
                    }
                    captchaform.put("downloadpw", passCode);
                }
                br.submitForm(captchaform);
                if (br.containsHTML("Password Error")) {
                    logger.warning("Wrong password!");
                    link.setProperty("pass", null);
                    continue;
                }
                if (br.containsHTML("Captcha number error") || br.containsHTML("captcha.php") && !br.containsHTML("You have got max allowed bandwidth size per hour")) {
                    logger.warning("Wrong captcha or wrong password!");
                    link.setProperty("pass", null);
                    continue;
                }
                break;
            }
        }
        if (br.containsHTML("Password Error")) {
            logger.warning("Wrong password!");
            link.setProperty("pass", null);
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (br.containsHTML("Captcha number error") || br.containsHTML("captcha.php") && !br.containsHTML("You have got max allowed bandwidth size per hour")) {
            logger.warning("Wrong captcha or wrong password!");
            link.setProperty("pass", null);
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        if (passCode != null) {
            link.setProperty("pass", passCode);
        }
        if (br.containsHTML("Due to high demand at the moment, this file is available only to registered users")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.host.UploadIlCom.nofreeslotsavailable", "No free slots available for this file"));
        if (br.containsHTML("You have got max allowed bandwidth size per hour")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 10 * 60 * 1001l);
        String finalLink = findLink();
        if (finalLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String waittime = br.getRegex("var timeout='(\\d+)';").getMatch(0);
        int secondsToWait = 80;
        if (waittime != null) {
            secondsToWait = Integer.parseInt(waittime);
            logger.info("Regexed waittime equals " + secondsToWait + " seconds.");
        }
        sleep((secondsToWait + 1) * 1000l, link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, finalLink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public String findLink() throws Exception {
        String finalLink = null;
        String[] sitelinks = HTMLParser.getHttpLinks(br.toString(), null);
        if (sitelinks == null || sitelinks.length == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        for (String alink : sitelinks) {
            alink = Encoding.htmlDecode(alink);
            if (alink.contains("access_key=") || alink.contains("getfile.php?")) {
                finalLink = alink;
                break;
            }
        }
        return finalLink;
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

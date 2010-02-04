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

import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "midupload.com" }, urls = { "http://[\\w\\.]*?midupload\\.com/[0-9a-z]{12}" }, flags = { 0 })
public class MidUploadCom extends PluginForHost {

    public MidUploadCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.midupload.com/tos.html";
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        requestFileInformation(link);
        if (br.containsHTML("This server is in maintenance mode")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "This server is in maintenance mode");
        if (br.containsHTML("You have to wait")) {
            if (br.containsHTML("minute")) {
                int minute = Integer.parseInt(br.getRegex("You have to wait (\\d+) minute, (\\d+) seconds till next download").getMatch(0));
                int sec = Integer.parseInt(br.getRegex("You have to wait (\\d+) minute, (\\d+) seconds till next download").getMatch(1));
                int wait = minute * 60 + sec;
                if (wait * 1001l < 13120) {
                    sleep(wait * 1000l, link);
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, wait * 1001);
            } else {
                sleep(60 * 1000l, link);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
        }
        if (br.containsHTML("(File Not Found|The file you were looking for could not be found)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Form form = br.getFormbyProperty("name", "F1");
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Ticket Time
        String ttt = br.getRegex("countdown\">.*?(\\d+).*?</span>").getMatch(0);
        if (ttt != null) {
            logger.info("Waittime detected, waiting " + ttt.trim() + " seconds from now on...");
            int tt = Integer.parseInt(ttt);
            sleep(tt * 1001, link);
        }
        String captcha = br.getRegex(Pattern.compile("Bitte Code eingeben:</b></td></tr>.*<tr><td align=right>.*<img src=\"(.*?)\">.*class=\"captcha_code\">", Pattern.DOTALL)).getMatch(0);
        if (captcha == null) captcha = br.getRegex(Pattern.compile("Enter code below:</b></td></tr>.*<tr><td align=right>.*<img src=\"(.*?)\">.*class=\"captcha_code\">", Pattern.DOTALL)).getMatch(0);
        if (captcha == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String code = getCaptchaCode(captcha, link);
        form.put("code", code);
        String passCode = null;
        if (br.containsHTML("<br><b>Password:</b>")) {
            if (link.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput("Password?", link);
            } else {
                /* gespeicherten PassCode holen */
                passCode = link.getStringProperty("pass", null);
            }
            form.put("password", passCode);
        }
        br.submitForm(form);
        if (br.containsHTML("Wrong captcha") || br.containsHTML("Expired session") || br.containsHTML("Wrong password")) {
            link.setProperty("pass", null);
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        if (passCode != null) {
            link.setProperty("pass", passCode);
        }
        String dllink = null;
        dllink = br.getRegex(Pattern.compile("<br>.*<a href=\"(.*?)\"><img src=\"http://www.midupload.com/images/download-button.gif\" border=\"0\">", Pattern.DOTALL)).getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1).startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.setCookie("http://www.midupload.com", "lang", "english");
        br.getPage(parameter.getDownloadURL());
        Form freeform = br.getFormBySubmitvalue("Kostenloser+Download");
        if (freeform == null) {
            freeform = br.getFormBySubmitvalue("Free+Download");
            if (freeform == null) {
                freeform = br.getFormbyKey("download1");
            }
        }
        if (freeform != null) {
            freeform.remove("method_premium");
            br.submitForm(freeform);
        }
        if (br.containsHTML("This server is in maintenance mode")) return AvailableStatus.UNCHECKABLE;
        if (br.containsHTML("You have reached the download-limit")) {
            logger.warning("Waittime detected, please reconnect to make the linkchecker work!");
            return AvailableStatus.UNCHECKABLE;
        }
        if (br.containsHTML("(No such file|No such user exist|File not found)")) {
            logger.warning("file is 99,99% offline, throwing \"file not found\" now...");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("Filename:</b></td><td>(.*?)</td>").getMatch(0);
        String filesize = br.getRegex("<small>\\((.*?)\\)</small>").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("\\(([0-9]+ bytes)\\)").getMatch(0);
            if (filesize == null) {
                filesize = br.getRegex("</font>[ ]+\\((.*?)\\)(.*?)</font>").getMatch(0);
            }
        }
        if (filename == null) {
            logger.warning("The filename equals null, throwing \"file not found\" now...");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        filename = filename.replaceAll("(</b>|<b>|\\.html)", "");
        parameter.setName(filename.trim());
        if (filesize != null) {
            logger.info("Filesize found, filesize = " + filesize);
            parameter.setDownloadSize(Regex.getSize(filesize));
        }
        return AvailableStatus.TRUE;
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

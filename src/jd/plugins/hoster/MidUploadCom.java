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
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "midupload.com" }, urls = { "http://[\\w\\.]*?midupload\\.com/[0-9a-z]+" }, flags = { 0 })
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
        br.getPage(link.getDownloadURL());
        Form form = br.getFormBySubmitvalue("Kostenloser+Download");
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        form.remove("method_premium");
        br.submitForm(form);
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
        form = br.getFormbyProperty("name", "F1");
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        // waittime
        int tt = Integer.parseInt(br.getRegex("countdown\">(\\d+)</span>").getMatch(0));
        sleep(tt * 1001l, link);
        String captcha = br.getRegex(Pattern.compile("Bitte Code eingeben:</b></td></tr>.*<tr><td align=right>.*<img src=\"(.*?)\">.*class=\"captcha_code\">", Pattern.DOTALL)).getMatch(0);
        if (captcha == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
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
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1).startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.setCookie("http://www.midupload.com", "lang", "english");
        br.getPage(parameter.getDownloadURL());
        if (br.containsHTML("No such user exist")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("Datei nicht gefunden")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("No such file with this filename")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("\"fname\" value=\"(.*?)\"").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setName(filename.trim());
        return AvailableStatus.TRUE;
    }

    public void reset() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

}

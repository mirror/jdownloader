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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "evilshare.com" }, urls = { "http://[\\w\\.]*?evilshare\\.com/[0-9a-z]+" }, flags = { 0 })
public class EvilShareCom extends PluginForHost {

    public EvilShareCom(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    @Override
    public String getAGBLink() {
        return "http://evilshare.com/tos.html";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.setCookie("http://www.evilshare.com", "lang", "english");
        br.getPage(parameter.getDownloadURL());
        if (br.containsHTML("File does not exist")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("Datei nicht gefunden")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("No such file")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("\">You have requested <font color=\"[a-z]+\">http://evilshare\\.com/[0-9a-z]+/(.*?)</font>").getMatch(0);
        String filesize = br.getRegex("</font> \\((.*?)\\)</font>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setName(filename);
        parameter.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        requestFileInformation(link);
        br.getPage(link.getDownloadURL());
        Form form = br.getFormBySubmitvalue("Free+Download");
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
                int sec = Integer.parseInt(br.getRegex("You have to wait (\\d+) minute, (\\d+) seconds till next download").getMatch(1));
                int wait = sec * 10001;
                sleep(wait * 1000l, link);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
        }
        form = br.getFormbyProperty("name", "F1");
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        String captcha = br.getRegex(Pattern.compile("\"(http://evilshare.com/captchas.*?)\"", Pattern.DOTALL)).getMatch(0);
        if (captcha == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
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
        // waittime
        int tt = Integer.parseInt(br.getRegex("countdown\">(\\d+)</span>").getMatch(0));
        sleep(tt * 1001l, link);
        String code = getCaptchaCode(captcha, link);
        form.put("code", code);
        br.setFollowRedirects(true);
        jd.plugins.BrowserAdapter.openDownload(br, link, form, false, 1);
        if (!(dl.getConnection().isContentDisposition())) {
            br.followConnection();
            if (br.containsHTML("Wrong captcha") || br.containsHTML("Expired session") || br.containsHTML("Wrong password")) {
                logger.warning("Wrong captcha or wrong password");
                link.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        }
        if (passCode != null) {
            link.setProperty("pass", passCode);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

}

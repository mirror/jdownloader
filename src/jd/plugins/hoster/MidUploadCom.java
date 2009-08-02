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
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "midupload.com" }, urls = { "http://[\\w\\.]*?midupload\\.com/[0-9a-z]+" }, flags = { 0 })
public class MidUploadCom extends PluginForHost {

    public MidUploadCom(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    @Override
    public String getAGBLink() {
        return "http://www.midupload.com/tos.html";
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        Form form = br.getForm(0);
        form.remove("method_premium");
        br.submitForm(form);
        if (br.containsHTML("You have to wait")) {
            if (br.containsHTML("minute")) {
                int minute = Integer.parseInt(br.getRegex("You have to wait (\\d+) minute, (\\d+) seconds till next download").getMatch(0));
                int sec = Integer.parseInt(br.getRegex("You have to wait (\\d+) minute, (\\d+) seconds till next download").getMatch(1));
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, (minute * 60 + sec) * 1001);
            } else {
                int sec = Integer.parseInt(br.getRegex("You have to wait (\\d+) minute, (\\d+) seconds till next download").getMatch(1));
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, sec * 1001);
            }
        }
        form = br.getForm(0);
        sleep(40 * 1001, link);
        String captcha = null;
        captcha = br.getRegex(Pattern.compile("Bitte Code eingeben:</b></td></tr>.*<tr><td align=right>.*<img src=\"(.*?)\">.*class=\"captcha_code\">", Pattern.DOTALL)).getMatch(0);
        if (captcha == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        String code = getCaptchaCode(captcha, link);
        form.put("code", code);
        br.submitForm(form);
        if (br.containsHTML("Wrong captcha") || br.containsHTML("Expired session")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String dllink = null;
        dllink = br.getRegex(Pattern.compile("<br>.*<a href=\"(.*?)\"><img src=\"http://www.midupload.com/images/download-button.gif\" border=\"0\">", Pattern.DOTALL)).getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.openDownload(link, dllink, true, 1).startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.getPage(parameter.getDownloadURL());
        if (br.containsHTML("No such user exist")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("Datei nicht gefunden")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<h2>Datei herunterladen (.*?)</h2>").getMatch(0);
        String filesize = br.getRegex("Sie haben angefordert <font color=\"red\">.*</font> \\((.*?)\\)</font>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setName(filename.trim());
        parameter.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
        // TODO Auto-generated method stub

    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        // TODO Auto-generated method stub

    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

}

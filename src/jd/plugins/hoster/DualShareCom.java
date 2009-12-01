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

import java.io.IOException;

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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dualshare.com" }, urls = { "http://[\\w\\.]*?dualshare\\.com/[0-9a-z]{12}" }, flags = { 0 })
public class DualShareCom extends PluginForHost {

    public DualShareCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://dualshare.com/tos.html";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCookie("http://www.dualshare.com", "lang", "english");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("No such file")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("No such user")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("File Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<h2>Download File(.*?)</h2>").getMatch(0);
        String filesize = br.getRegex("<h2>Download File.*?</h2>.*?</font>.*?\\((.*?)\\)").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename.trim());
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        if (br.containsHTML("This file reached max downloads limit")) throw new PluginException(LinkStatus.ERROR_FATAL, "File reached max download limit, try to open it in the browser, maybe it's a stream so you can still watch it.");
        // Waittime check by ManiacMansion
        if (br.containsHTML("You have to wait")) {
            int minutes = 0, seconds = 0, hours = 0;
            String tmphrs = br.getRegex("\\s+(\\d+)\\s+hours?").getMatch(0);
            if (tmphrs != null) hours = Integer.parseInt(tmphrs);
            String tmpmin = br.getRegex("\\s+(\\d+)\\s+minutes?").getMatch(0);
            if (tmpmin != null) minutes = Integer.parseInt(tmpmin);
            String tmpsec = br.getRegex("\\s+(\\d+)\\s+seconds?").getMatch(0);
            if (tmpsec != null) seconds = Integer.parseInt(tmpsec);
            int waittime = ((3600 * hours) + (60 * minutes) + seconds + 1) * 1000;
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, waittime);
        }
        // Form um auf "Datei herunterladen" zu klicken
        Form dlForm = br.getForm(0);
        if (dlForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Ticket Time
        dlForm.setPreferredSubmit("Free+Download");
        /* download button */
        br.submitForm(dlForm);
        dlForm = br.getForm(0);
        if (dlForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (br.containsHTML("You have to wait")) {
            int waitt = 30;
            String wait = br.getRegex("You have to wait (\\d+) seconds").getMatch(0);
            if (wait != null) {
                waitt = Integer.parseInt(wait);
            }
            sleep(waitt * 1001, downloadLink);
            br.getPage(br.getURL());
        }
        String ttt = br.getRegex("countdown\">(\\d+)</span>").getMatch(0);
        if (ttt != null) {
            int tt = Integer.parseInt(ttt);
            sleep(tt * 1001, downloadLink);
        }
        String captchaurl = br.getRegex("\"(http://dualshare\\.com/captchas/.*?)\"").getMatch(0);
        if (captchaurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String code = getCaptchaCode(captchaurl, downloadLink);
        dlForm.put("code", code);
        // Password handling
        String passCode = null;
        if (br.containsHTML("name=\"password\"")) {
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput("Password?", downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
            dlForm.put("password", passCode);
        }
        br.submitForm(dlForm);
        String dllink = br.getRedirectLocation();
        if (br.containsHTML("Wrong password") || br.containsHTML("Wrong captcha")) {
            logger.warning("Wrong password!");
            downloadLink.setProperty("pass", null);
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        if (dllink == null) dllink = br.getRegex("padding:[0-9]+px;\">\\s+<a\\s+href=\"(.*?)\">").getMatch(0);
        br.setFollowRedirects(true);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
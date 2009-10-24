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
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "flyfile.us" }, urls = { "http://[\\w\\.]*?flyfile\\.us/[a-z|0-9]+/.+" }, flags = { 0 })
public class FlyFileUs extends PluginForHost {

    public FlyFileUs(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://flyfile.us/tos.html";
    }

    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setCookie("http://www.flyfile.us", "lang", "english");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("No such file with this filename")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("No such user exist")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("File Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // old filesize handling
        // String filesize =
        // br.getRegex("</font> \\((.*?)\\)</font>").getMatch(0);
        String filename = Encoding.htmlDecode(br.getRegex("<font style=\"font-size:12px;\">You have requested <font color=\"red\">http://flyfile.us/[a-z|0-9]+/(.*?)</font>").getMatch(0));
        if (filename == null) filename = br.getRegex("<h2>Download File (.*?)</h2>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Form DLForm0 = br.getFormBySubmitvalue("Free+Download");
        if (DLForm0 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        DLForm0.remove("method_premium");
        br.submitForm(DLForm0);
        if (br.containsHTML(">File was deleted by administrator<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filesize = br.getRegex("<center.*Size.*?small.*?\\((.*?) bytes\\)").getMatch(0);
        link.setName(filename);
        if (filesize != null) link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        Form DLForm = br.getFormbyProperty("name", "F1");
        if (br.containsHTML("You have reached the download-limit")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 30 * 60 * 1000l);
        if (DLForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        String passCode = null;
        if (br.containsHTML("<br><b>Password:</b>")) {
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput("Password?", downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
            DLForm.put("password", passCode);
        }
        if (br.containsHTML("flyfile.us/captchas/")) {
            String captcha = br.getRegex("\"(http://flyfile.us/captchas.*?)\"").getMatch(0);
            if (captcha == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            String code = getCaptchaCode(captcha, downloadLink);
            DLForm.put("code", code);
        }
        jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLForm, true, 1);
        if (!(dl.getConnection().getContentType().contains("octet"))) {
            /*
             * server does not send disposition header, therefore we must check
             * content-type
             */
            br.followConnection();
            if (br.containsHTML("Wrong password") || br.containsHTML("Wrong captcha")) {
                logger.warning("Wrong password or wrong captcha!");
                downloadLink.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            if (br.containsHTML("404 Not Found")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 60 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        }
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
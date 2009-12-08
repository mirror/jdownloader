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
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "usershare.net" }, urls = { "http://[\\w\\.]*?usershare\\.net/[0-9a-zA-Z]{12}" }, flags = { 0 })
public class Usershare extends PluginForHost {

    public Usershare(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    @Override
    public String getAGBLink() {
        return "http://usershare.net/tos.html";
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        br.setFollowRedirects(false);
        String passCode = null;
        String linkurl = null;
        String loginpw = br.getRegex("value=\"login\">(.*?)value=\" Login\"").getMatch(0);
        if (br.containsHTML("name=\"password\"") && !(loginpw != null && loginpw.contains("password"))) {
            logger.info("The downloadlink seems to be password protected.");
            Form pwform = br.getFormbyProperty("name", "F1");
            if (pwform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            if (link.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput("Password?", link);
            } else {
                /* gespeicherten PassCode holen */
                passCode = link.getStringProperty("pass", null);
            }
            pwform.put("password", passCode);
            logger.info("Put password \"" + passCode + "\" entered by user in the DLForm and submitted it.");
            br.submitForm(pwform);
            if (br.containsHTML("(name=\"password\"|Wrong password)") && !(loginpw != null && loginpw.contains("password"))) {
                logger.warning("Wrong password, the entered password \"" + passCode + "\" is wrong, retrying...");
                link.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            linkurl = br.getRedirectLocation();
        } else {
            linkurl = br.getRegex("</a></TD><TD width=\"1%\" nowrap align=right.*?/TD>.*?</Table>.*?<a href=\"(http.*?)\"").getMatch(0);
            if (linkurl == null) {
                linkurl = br.getRegex("\"(http://[a-zA-Z0-9]+\\.usershare\\.net/files/.*?/.*?/.*?)\"").getMatch(0);
            }
        }
        if (passCode != null) {
            link.setProperty("pass", passCode);
        }
        if (linkurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, linkurl, true, -4);
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.getPage(parameter.getDownloadURL());
        if (br.containsHTML("No such user exist") || br.containsHTML("File Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
        String filesize = br.getRegex("Size:</b></td><td>(.*?)<small>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setName(filename.trim());
        parameter.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    // TODO: Hoster allows 4 connections (1 download with 4 chunks or 4 simultan
    // downloads) at all, a controller to set the max.number of the connections
    // would be nice for that
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }
}

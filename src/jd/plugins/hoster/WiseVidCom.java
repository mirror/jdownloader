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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wisevid.com" }, urls = { "http://(www\\.)?wisevid\\.com/((play|gate-way)\\?v=[A-Za-z0-9_-]+|gateway\\.php\\?viewkey=[A-Za-z0-9_-]+)" }, flags = { 0 })
public class WiseVidCom extends PluginForHost {

    public WiseVidCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.wisevid.com/terms";
    }

    private String DLLINK = null;

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("gate-way", "play"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        Form human = br.getFormbyProperty("name", "ff");
        if (human != null) {
            if (!downloadLink.getDownloadURL().contains("play")) {
                String action = br.getRegex("value=\"Yes, let me watch\" onclick=\"javascript:location\\.href=\\'(http://.*?)\\'\"").getMatch(0);
                if (action != null) human.setAction(action);
            }
            br.submitForm(human);
        }
        if (br.getURL().contains("/notfound") || br.containsHTML("\">Sorry we couldn\\'t find what you were looking for")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<span style=\"font-size: 11px;\" class=\"gray10\"><strong>(.*?)</strong></span>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("widgetTitle: \\'Wisevid - Watch (.*?)\\',").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>(.*?) - Wisevid</title>").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("<h1>(.*?)</h1>").getMatch(0);
                }
            }
        }
        DLLINK = br.getRegex("getF\\(\\'(.*?)\\'\\)").getMatch(0);
        if (filename == null || DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (br.getURL().contains("play") && !downloadLink.getDownloadURL().contains("play")) downloadLink.setUrlDownload(br.getURL());
        DLLINK = Encoding.Base64Decode(DLLINK);
        filename = filename.trim();
        downloadLink.setFinalFileName(filename + ".flv");
        Browser br2 = br.cloneBrowser();
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(DLLINK);
            if (!con.getContentType().contains("html"))
                downloadLink.setDownloadSize(con.getLongContentLength());
            else
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, -16);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
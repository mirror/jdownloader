//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(names = { "box.net" }, urls = { "(http://www\\.box\\.net/(shared/static/|rssdownload/).*)|(http://www\\.box\\.net/index\\.php\\?rm=box_download_shared_file\\&file_id=.+?\\&shared_name=\\w+)" }, flags = { 0 }, revision = "$Revision$", interfaceVersion = 2)
public class BoxNet extends PluginForHost {
    private static final String TOS_LINK = "https://www.box.net/static/html/terms.html";

    private static final String OUT_OF_BANDWITH_MSG = "out of bandwidth";
    private static final String REDIRECT_DOWNLOAD_LINK = "http://www\\.box\\.net/index\\.php\\?rm=box_download_shared_file\\&file_id=.+?\\&shared_name=\\w+";

    public BoxNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return TOS_LINK;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        // setup referer and cookies for single file downloads
        if (link.getDownloadURL().matches(REDIRECT_DOWNLOAD_LINK)) {
            br.getPage(link.getBrowserUrl());
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getDownloadURL(), true, 0);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        // setup referer and cookies for single file downloads
        if (parameter.getDownloadURL().matches(REDIRECT_DOWNLOAD_LINK)) {
            br.getPage(parameter.getBrowserUrl());
        }
        URLConnectionAdapter urlConnection = br.openGetConnection(parameter.getDownloadURL());
        if (urlConnection.getResponseCode() == 404 || !urlConnection.isOK()) {
            urlConnection.disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (!urlConnection.isContentDisposition()) {
            br.followConnection();
            if (br.containsHTML(OUT_OF_BANDWITH_MSG)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            String originalpage = br.getRegex("please visit: <a href=\"(.*?)\"").getMatch(0);
            if (originalpage == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.getPage(originalpage);
            String dlpage = br.getRegex("href=\"(http://www\\.box\\.net/index\\.php\\?rm=box_download_shared_file\\&amp;file_id=.*?)\"").getMatch(0);
            if (dlpage == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            dlpage = dlpage.replace("amp;", "");
            urlConnection = br.openGetConnection(dlpage);
        }
        parameter.setFinalFileName(Plugin.getFileNameFromHeader(urlConnection));
        parameter.setDownloadSize(urlConnection.getLongContentLength());
        urlConnection.disconnect();
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
        return 20;
    }

}

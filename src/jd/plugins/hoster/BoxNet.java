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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(names = { "box.net" }, urls = { "(https?://(www|[a-z0-9\\-_]+)\\.box\\.(net|com)(/(shared/static/|rssdownload/).*|/index\\.php\\?rm=box_download_shared_file\\&file_id=.+?\\&shared_name=\\w+)|https?://www\\.boxdecrypted\\.(net|com)/s/[a-z0-9]+)" }, flags = { 0 }, revision = "$Revision$", interfaceVersion = 2)
public class BoxNet extends PluginForHost {
    private static final String TOS_LINK               = "https://www.box.net/static/html/terms.html";

    private static final String OUT_OF_BANDWITH_MSG    = "out of bandwidth";
    private static final String REDIRECT_DOWNLOAD_LINK = "https?://(www|[a-z0-9\\-_]+)\\.box\\.(net|com)/index\\.php\\?rm=box_download_shared_file\\&file_id=.+?\\&shared_name=\\w+";
    private static final String DLLINKREGEX            = "href=\"(https?://(www|[a-z0-9\\-_]+)\\.box\\.(net|com)/index\\.php\\?rm=box_download_shared_file\\&amp;file_id=[^<>\"\\']+)\"";
    private static final String SLINK                  = "https?://www\\.box\\.(net|com)/s/[a-z0-9]+";
    private String              DLLINK                 = null;

    public BoxNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return TOS_LINK;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("boxdecrypted\\.(net|com)/s/", "box.com/s/"));
        link.setUrlDownload(link.getDownloadURL().replace("box\\.net/", "box.com/"));
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        // setup referer and cookies for single file downloads
        requestFileInformation(link);
        if (link.getDownloadURL().matches(SLINK) && DLLINK == null) {
            String fid = br.getRegex("var file_id = \\'(\\d+)\\';").getMatch(0);
            if (fid == null) {
                fid = br.getRegex(",typed_id: \\'f_(\\d+)\\'").getMatch(0);
                if (fid == null) {
                    fid = br.getRegex("\\&amp;file_id=f_(\\d+)\\&amp").getMatch(0);
                    if (fid == null) {
                        fid = br.getRegex("var single_item_collection = \\{ (\\d+) : item \\};").getMatch(0);
                    }
                }
            }
            if (fid == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            DLLINK = "http://www.box.com/download/external/f_" + fid + "/0/" + link.getName() + "?shared_file_page=1&shared_name=" + new Regex(link.getDownloadURL(), "http://www\\.box\\.[^/]+/s/(.+)").getMatch(0);
        } else if (DLLINK == null) {
            DLLINK = link.getDownloadURL();
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(DLLINK), true, 0);
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
        /** Correct old box.NET links */
        correctDownloadLink(parameter);
        // setup referer and cookies for single file downloads
        if (parameter.getDownloadURL().matches(REDIRECT_DOWNLOAD_LINK)) {
            br.getPage(parameter.getBrowserUrl());
        } else if (parameter.getDownloadURL().matches(SLINK)) {
            br.getPage(parameter.getBrowserUrl());
            if (br.containsHTML("(this shared file or folder link has been removed|<title>Box \\- Free Online File Storage, Internet File Sharing, RSS Sharing, Access Documents \\&amp; Files Anywhere, Backup Data, Share Files</title>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            Regex fileInfo = br.getRegex("<h2 class=\"gallery_pseudo_icon_text_title bolder white ellipsis ellipsis_\\d+\">(.*?)\\&nbsp;\\(([a-z0-9\\. ]+)\\)");
            String filename = fileInfo.getMatch(0);
            if (filename == null) filename = br.getRegex("<title>(.*?) \\- File Shared from Box \\- Free Online File Storage</title>").getMatch(0);
            if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String filesize = fileInfo.getMatch(1);
            parameter.setName(filename.trim());
            if (filesize != null) parameter.setDownloadSize(SizeFormatter.getSize(filesize));
            DLLINK = br.getRegex(DLLINKREGEX).getMatch(0);
            return AvailableStatus.TRUE;
        }
        URLConnectionAdapter urlConnection = null;
        try {
            urlConnection = br.openGetConnection(parameter.getDownloadURL());
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
                DLLINK = br.getRegex(DLLINKREGEX).getMatch(0);
                if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                DLLINK = Encoding.htmlDecode(DLLINK);
                urlConnection = br.openGetConnection(DLLINK);
            }
            String name = urlConnection.getHeaderField("Content-Disposition");
            if (name != null) {
                /* workaround for old core */
                name = new Regex(name, "filename=\"([^\"]+)").getMatch(0);
                if (name != null) parameter.setFinalFileName(name);
            }
            parameter.setDownloadSize(urlConnection.getLongContentLength());
            urlConnection.disconnect();
            return AvailableStatus.TRUE;
        } finally {
            try {
                urlConnection.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
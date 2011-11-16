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
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(names = { "box.net" }, urls = { "(http://www\\.box\\.net/(shared/static/|rssdownload/).*)|(http://www\\.box\\.net/index\\.php\\?rm=box_download_shared_file\\&file_id=.+?\\&shared_name=\\w+|http://www\\.box\\.net/s/[a-z0-9]+)" }, flags = { 0 }, revision = "$Revision$", interfaceVersion = 2)
public class BoxNet extends PluginForHost {
    private static final String TOS_LINK               = "https://www.box.net/static/html/terms.html";

    private static final String OUT_OF_BANDWITH_MSG    = "out of bandwidth";
    private static final String REDIRECT_DOWNLOAD_LINK = "http://www\\.box\\.net/index\\.php\\?rm=box_download_shared_file\\&file_id=.+?\\&shared_name=\\w+";
    private static final String SLINK                  = "http://www\\.box.net/s/[a-z0-9]+";

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
        requestFileInformation(link);
        String finallink = link.getDownloadURL();
        if (link.getDownloadURL().matches(SLINK)) {
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
            finallink = "http://www.box.net/download/external/f_" + fid + "/0/" + link.getName() + "?shared_file_page=1&shared_name=" + new Regex(link.getDownloadURL(), "http://www\\.box\\.net/s/(.+)").getMatch(0);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, finallink, true, 0);
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
                String dlpage = br.getRegex("href=\"(http://www\\.box\\.net/index\\.php\\?rm=box_download_shared_file\\&amp;file_id=.*?)\"").getMatch(0);
                if (dlpage == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                dlpage = Encoding.htmlDecode(dlpage);
                urlConnection = br.openGetConnection(dlpage);
            }
            String name = urlConnection.getHeaderField("Content-Disposition");
            if (name != null) {
                /* workaround for old core */
                name = name.replaceAll("filename=.*?;", "");
                parameter.setFinalFileName(Plugin.getFileNameFromDispositionHeader(name));
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

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

}

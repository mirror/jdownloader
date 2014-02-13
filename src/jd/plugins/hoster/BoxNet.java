//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "box.net" }, urls = { "(https?://(www|[a-z0-9\\-_]+)\\.box\\.(net|com)(/(shared/static/|rssdownload/).*|/index\\.php\\?rm=box_download_shared_file\\&file_id=.+?\\&shared_name=\\w+)|https?://www\\.boxdecrypted\\.(net|com)/shared/[a-z0-9]+|https?://www\\.boxdecrypted\\.com/s/[a-z0-9]+/\\d+/\\d+/\\d+/\\d+)" }, flags = { 0 })
public class BoxNet extends PluginForHost {
    private static final String TOS_LINK               = "https://www.box.net/static/html/terms.html";

    private static final String OUT_OF_BANDWITH_MSG    = "out of bandwidth";
    private static final String REDIRECT_DOWNLOAD_LINK = "https?://(www|[a-z0-9\\-_]+)\\.box\\.(net|com)/index\\.php\\?rm=box_download_shared_file\\&file_id=.+?\\&shared_name=\\w+";
    private static final String DLLINKREGEX            = "href=\"(https?://(www|[a-z0-9\\-_]+)\\.box\\.(net|com)/index\\.php\\?rm=box_download_shared_file\\&amp;file_id=[^<>\"\\']+)\"";
    private static final String SLINK                  = "https?://www\\.box\\.com/shared/[a-z0-9]+";
    private static final String DECRYPTEDFOLDERLINK    = "https?://www\\.box\\.com/s/[a-z0-9]+/\\d+/\\d+/\\d+/\\d+";
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
        link.setUrlDownload(link.getDownloadURL().replaceAll("boxdecrypted\\.(net|com)/", "box.com/"));
        link.setUrlDownload(link.getDownloadURL().replace("box.net/", "box.com/"));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie("http://box.com", "country_code", "US");
        /** Correct old box.NET links */
        correctDownloadLink(parameter);
        if (parameter.getBooleanProperty("offline", false)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // setup referrer and cookies for single file downloads
        if (parameter.getDownloadURL().matches(REDIRECT_DOWNLOAD_LINK)) {
            br.setFollowRedirects(false);
            br.getPage(parameter.getDownloadURL());
            if (br.getRedirectLocation() != null) {
                DLLINK = br.getRedirectLocation();
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } else if (parameter.getDownloadURL().matches(DECRYPTEDFOLDERLINK)) {
            final String plainfilename = parameter.getStringProperty("plainfilename", null);
            final Regex dlIds = new Regex(parameter.getDownloadURL(), "box\\.com/s/([a-z0-9]+)/\\d+/\\d+/(\\d+)/\\d+");
            String sharedname = parameter.getStringProperty("sharedname", null);
            String fileid = parameter.getStringProperty("fileid", null);
            if (sharedname == null) sharedname = dlIds.getMatch(0);
            if (fileid == null) fileid = dlIds.getMatch(1);
            br.getPage(parameter.getDownloadURL());
            br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
            parameter.setFinalFileName(Encoding.htmlDecode(plainfilename.trim()));
            parameter.setDownloadSize(SizeFormatter.getSize(parameter.getStringProperty("plainfilesize", null)));
            DLLINK = "https://www.box.com/index.php?rm=box_download_shared_file&shared_name=" + sharedname + "&file_id=f_" + fileid;
            return AvailableStatus.TRUE;
        } else if (parameter.getDownloadURL().matches(SLINK)) {
            // Last compare link: https://app.box.com/s/ubpk2k11ttpcq40vbrww
            String fileID = parameter.getStringProperty("fileid", null);
            if (fileID == null) {
                br.getPage(parameter.getDownloadURL());
                if (br.getURL().contains("box.com/login")) {
                    parameter.getLinkStatus().setStatusText("Only downloadable for registered users");
                    parameter.setAvailable(true);
                    return AvailableStatus.TRUE;
                } else if (br.containsHTML("id=\"shared_password\"")) {
                    parameter.getLinkStatus().setStatusText("This link is password protected");
                    parameter.setAvailable(true);
                    return AvailableStatus.TRUE;
                }
                if (br.containsHTML("(this shared file or folder link has been removed|<title>Box \\- Free Online File Storage, Internet File Sharing, RSS Sharing, Access Documents \\&amp; Files Anywhere, Backup Data, Share Files</title>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                if (br.getURL().equals("https://www.box.com/freeshare")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                fileID = findFID();
            }
            final String sharedName = new Regex(parameter.getDownloadURL(), "([a-z0-9]+)$").getMatch(0);
            br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.getPage("https://app.box.com/preview/info/f_" + fileID + ".json?shared_name=" + sharedName);
            final String filename = br.getRegex("\"title\":\"([^<>\"]*?)\"").getMatch(0);
            final String filesize = br.getRegex("\"size\":\"([^<>\"]*?)\"").getMatch(0);
            parameter.setProperty("fileid", fileID);
            if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            parameter.setName(filename.trim());
            if (filesize != null) parameter.setDownloadSize(SizeFormatter.getSize(filesize));
            DLLINK = "https://app.box.com/index.php?rm=box_download_shared_file&shared_name=" + sharedName + "&file_id=f_" + fileID;
            br.setFollowRedirects(false);
            return AvailableStatus.TRUE;
        } else {
            // Unsupported link, maybe direct link
            DLLINK = parameter.getDownloadURL();
        }
        URLConnectionAdapter urlConnection = null;
        try {
            urlConnection = br.openGetConnection(DLLINK);
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
    public void handleFree(final DownloadLink link) throws Exception {
        // setup referer and cookies for single file downloads
        requestFileInformation(link);
        // site has many redirects, it could be set off from
        // requestFileInformation...
        br.setFollowRedirects(true);
        if (link.getDownloadURL().matches(SLINK) && DLLINK == null) {
            if (br.getURL().contains("box.com/login")) {
                try {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
                } catch (final Throwable e) {
                    if (e instanceof PluginException) throw (PluginException) e;
                }
                throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadable for registered users");
            } else if (br.containsHTML("id=\"shared_password\"")) { throw new PluginException(LinkStatus.ERROR_FATAL, "Password protected links are not supported yet"); }
            if (DLLINK == null) {
                final String fid = findFID();
                if (fid == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                DLLINK = "http://www.box.com/download/external/f_" + fid + "/0/" + link.getName() + "?shared_file_page=1&shared_name=" + new Regex(link.getDownloadURL(), "http://www\\.box\\.[^/]+/s/(.+)").getMatch(0);
            }
        } else if (DLLINK == null) {
            DLLINK = link.getDownloadURL();
        }
        DLLINK = Encoding.htmlDecode(DLLINK);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, DLLINK, true, 0);
        if (!dl.getConnection().isContentDisposition()) {
            if (dl.getConnection().getResponseCode() == 500) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
            logger.info("The final downloadlink seems not to be a file");
            br.followConnection();
            if (br.containsHTML("error_message_bandwidth")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "The uploader of this file doesn't have enough bandwidth left!", 3 * 60 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String findFID() throws PluginException {
        String fid = br.getRegex("var file_id = \\'(\\d+)\\';").getMatch(0);
        if (fid == null) {
            fid = br.getRegex(",typed_id: \\'f_(\\d+)\\'").getMatch(0);
            if (fid == null) {
                fid = br.getRegex("\\&amp;file_id=f_(\\d+)\\&amp").getMatch(0);
                if (fid == null) {
                    fid = br.getRegex("var single_item_collection = \\{ (\\d+) : item \\};").getMatch(0);
                    if (fid == null) {
                        fid = br.getRegex("itemTypedID: \"f_(\\d+)\"").getMatch(0);
                    }
                }
            }
        }
        if (fid == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        return fid;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
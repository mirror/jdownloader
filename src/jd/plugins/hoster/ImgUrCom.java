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
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "imgur.com" }, urls = { "https?://imgurdecrypted\\.com/download/[A-Za-z0-9]+" }, flags = { 0 })
public class ImgUrCom extends PluginForHost {

    public ImgUrCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://imgur.com/tos";
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) throws Exception {
        link.setUrlDownload(link.getDownloadURL().replace("imgurdecrypted.com/", "imgur.com/"));
    }

    private String  DLLINK                         = null;
    private boolean DL_IMPOSSIBLE_APILIMIT_REACHED = false;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final String imgUID = link.getStringProperty("imgUID", null);
        String filetype = link.getStringProperty("filetype", null);
        String finalfilename = link.getStringProperty("decryptedfinalfilename", null);
        DLLINK = link.getStringProperty("directlink", null);

        URLConnectionAdapter con = null;
        br.setFollowRedirects(true);
        if (DLLINK != null) {
            try {
                con = br.openGetConnection(DLLINK);
                if (!con.getContentType().contains("html")) {
                    link.setDownloadSize(con.getLongContentLength());
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        } else {
            boolean apilimit_reached = false;
            br.getHeaders().put("Authorization", Encoding.Base64Decode(jd.plugins.decrypter.ImgurCom.OAUTH_AUTH));
            try {
                br.getPage("https://api.imgur.com/3/image/" + imgUID);
            } catch (final BrowserException e) {
                if (br.getHttpConnection().getResponseCode() == 429) {
                    apilimit_reached = true;
                } else {
                    throw e;
                }
            }
            if (!apilimit_reached) {
                br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
                if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("Unable to find an image with the id")) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                final long filesize = Long.parseLong(getJson(br.toString(), "size"));
                if (filesize == 0) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                final String title = getJson(br.toString(), "title");
                filetype = br.getRegex("\"type\":\"image/([^<>\"]*?)\"").getMatch(0);
                if (filetype == null) {
                    filetype = "jpeg";
                }
                if (title != null) {
                    finalfilename = title + "." + filetype;
                } else {
                    finalfilename = imgUID + "." + filetype;
                }
                link.setDownloadSize(filesize);
                DLLINK = getJson(br.toString(), "link");
            } else {
                /*
                 * Workaround for API limit reached - second way does return 503 response in case API limit is reached:
                 * http://imgur.com/download/ + imgUID This code should never be reached!
                 */
                if (imgUID == null || filetype == null) {
                    DL_IMPOSSIBLE_APILIMIT_REACHED = true;
                    return AvailableStatus.UNCHECKABLE;
                }
                br.clearCookies("http://imgur.com/");
                br.getHeaders().put("Referer", null);
                br.getHeaders().put("Authorization", null);
                try {
                    DLLINK = "http://i.imgur.com/" + imgUID + "." + filetype;
                    con = br.openGetConnection(DLLINK);
                    if (!con.getContentType().contains("html")) {
                        if (finalfilename == null) {
                            finalfilename = Encoding.htmlDecode(getFileNameFromHeader(con));
                        }
                        link.setDownloadSize(con.getLongContentLength());
                    } else {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                } finally {
                    try {
                        con.disconnect();
                    } catch (Throwable e) {
                    }
                }
            }
            if (finalfilename == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setFinalFileName(finalfilename);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (DL_IMPOSSIBLE_APILIMIT_REACHED) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Api limit reached", 10 * 60 * 1000l);
        }
        br.setFollowRedirects(true);
        if (DLLINK == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getJson(final String source, final String parameter) {
        String result = new Regex(source, "\"" + parameter + "\":([\t\n\r ]+)?([0-9\\.]+)").getMatch(1);
        if (result == null) {
            result = new Regex(source, "\"" + parameter + "\":([\t\n\r ]+)?\"([^<>\"]*?)\"").getMatch(1);
        }
        return result;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
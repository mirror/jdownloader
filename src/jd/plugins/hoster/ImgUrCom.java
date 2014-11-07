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
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.SubConfiguration;
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
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "imgur.com" }, urls = { "https?://imgurdecrypted\\.com/download/[A-Za-z0-9]+" }, flags = { 2 })
public class ImgUrCom extends PluginForHost {

    public ImgUrCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://imgur.com/tos";
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) throws Exception {
        link.setUrlDownload(link.getDownloadURL().replace("imgurdecrypted.com/", "imgur.com/"));
    }

    private static final String SETTING_CLIENTID               = "SETTING_CLIENTID";
    private static final String SETTING_USE_API                = "SETTING_USE_API";

    private String              DLLINK                         = null;
    private boolean             DL_IMPOSSIBLE_APILIMIT_REACHED = false;

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
            boolean api_failed = false;
            if (!this.getPluginConfig().getBooleanProperty(SETTING_USE_API, false)) {
                api_failed = true;
            } else {
                br.getHeaders().put("Authorization", getAuthorization());
                try {
                    br.getPage("https://api.imgur.com/3/image/" + imgUID);
                } catch (final BrowserException e) {
                    if (br.getHttpConnection().getResponseCode() == 429) {
                        api_failed = true;
                    } else {
                        throw e;
                    }
                }
            }
            if (!api_failed) {
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
                /*
                 * Note that for pictures/especially GIFs over 20 MB, the "link" value will only contain a link which leads to a preview or
                 * low quality version of the picture. This is why we need a little workaround for this case (works from 19.5++ MB).
                 */
                if (filesize > 20447232l) {
                    logger.info("File is bigger than 20 (19.5) MB --> Using /downloadlink as API-workaround");
                    DLLINK = "http://imgur.com/download/" + imgUID;
                } else {
                    DLLINK = getJson(br.toString(), "link");
                }
            } else {
                /*
                 * Workaround for API limit reached or in case user disabled API - second way does return 503 response in case API limit is
                 * reached: http://imgur.com/download/ + imgUID. This code should never be reached!
                 */
                if (imgUID == null || filetype == null) {
                    /*
                     * TODO: In this case, access site and grab json API response which is also located in the normal html code --> Why? -->
                     * Because /download/ links sometimes simply do n't work.
                     */
                    DLLINK = "http://imgur.com/download/" + imgUID;
                } else {
                    DLLINK = "http://i.imgur.com/" + imgUID + "." + filetype;
                }
                br.clearCookies("http://imgur.com/");
                br.getHeaders().put("Referer", null);
                br.getHeaders().put("Authorization", null);
                try {
                    con = br.openGetConnection(DLLINK);
                    if (con.getContentType().contains("image")) {
                        if (finalfilename == null) {
                            finalfilename = Encoding.htmlDecode(getFileNameFromHeader(con));
                            /* Host tags filenames, remove tags here */
                            finalfilename = finalfilename.replace(" - Imgur", "");
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
        link.setProperty("filetype", filetype);
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

    public static final String OAUTH_CLIENTID = "Mzc1YmE4Y2FmNjA0ZDQy";

    public static final String getAuthorization() {
        String authorization;
        final String clientid = SubConfiguration.getConfig("imgur.com").getStringProperty(SETTING_CLIENTID, defaultClientID);
        if (clientid.contains("JDDEFAULT")) {
            authorization = Encoding.Base64Decode(OAUTH_CLIENTID);
        } else {
            authorization = clientid;
        }
        authorization = "Client-ID " + authorization;
        return authorization;
    }

    @Override
    public String getDescription() {
        return "This Plugin can download galleries/albums/images from imgur.com.";
    }

    private final static String defaultClientID = "JDDEFAULT";

    private void setConfigElements() {
        final ConfigEntry cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), SETTING_USE_API, JDL.L("plugins.hoster.ImgUrCom.useAPI", "Use API (recommended!)")).setDefaultValue(true);
        getConfig().addEntry(cfg);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), SETTING_CLIENTID, JDL.L("plugins.hoster.ImgUrCom.oauthClientID", "Oauth Client-ID:")).setDefaultValue(defaultClientID).setEnabledCondidtion(cfg, true));
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
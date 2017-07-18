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

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "500px.com" }, urls = { "https?://(?:www\\.)?500px\\.com/photo/\\d+" })
public class FivehundretPxCom extends PluginForHost {

    public FivehundretPxCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:

    private String dllink = null;

    @Override
    public String getAGBLink() {
        return "https://500px.com/";
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        if (link.getSetLinkID() == null) {
            final String ID = new Regex(link.getPluginPatternMatcher(), "photo/(\\d+)").getMatch(0);
            link.setLinkID(ID);
        }
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        dllink = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String json = getJson();
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json);
        String title = null, user_firstname = null, user_lastname = null, ext = null;
        final Object photoo = entries.get("photo");
        if (photoo != null && photoo instanceof LinkedHashMap) {
            entries = (LinkedHashMap<String, Object>) photoo;
        }
        if (photoo == null && entries.get("offers") != null) {
            title = (String) entries.get("name");
            // no first last and last name seperately
            user_firstname = (String) entries.get("creator"); // or you could use copyrightHolder
            // seems to be only one link because they want you buy it!
            dllink = (String) entries.get("image");
            ext = getFileNameExtensionFromString(dllink);
        } else {
            title = (String) entries.get("name");
            user_firstname = (String) JavaScriptEngineFactory.walkJson(entries, "user/firstname");
            user_lastname = (String) JavaScriptEngineFactory.walkJson(entries, "user/lastname");
            /*
             * this will show jpeg when its actually jpg on server. this is because content distribution filename is real name and not
             * abbreviated.
             */
            // ext = (String) entries.get("image_format");
            if (ext != null) {
                ext = "." + ext;
            }
            // array full of images, we need to analyse for best
            ArrayList<Object> images = (ArrayList<Object>) entries.get("images");
            if (images != null) {
                int size = -1;
                for (Object o : images) {
                    LinkedHashMap<String, Object> data = (LinkedHashMap<String, Object>) o;
                    if (data.containsKey("size")) {
                        int s = ((Number) data.get("size")).intValue();
                        if (s > size) {
                            size = s;
                            dllink = (String) data.get("https_url");
                            if (dllink == null) {
                                dllink = (String) data.get("url");
                            }
                        }
                    }
                }
            }
            if (dllink == null) {
                // old raztoki20160430
                dllink = (String) JavaScriptEngineFactory.walkJson(entries, "images/{4}/https_url");
                if (dllink == null) {
                    dllink = (String) JavaScriptEngineFactory.walkJson(entries, "images/{3}/https_url");
                }
            }
        }
        if (title == null || dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String filename = (user_firstname != null ? user_firstname : "");
        filename += (user_lastname != null ? (filename.equals("") ? user_lastname : " " + user_lastname) : "");
        filename += (title != null ? (filename.equals("") ? title : " - " + title) : "");
        filename += (ext != null ? ext : "");
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        if (!downloadLink.isNameSet()) {
            // set part name if not set previously
            downloadLink.setFinalFileName(filename);
        }
        dllink = Encoding.htmlOnlyDecode(dllink);
        final Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            try {
                con = br2.openHeadConnection(dllink);
                if (ext == null) {
                    // update info
                    ext = getFileNameExtensionFromString(getFileNameFromHeader(con), ".jpg");
                    filename += (ext != null ? ext : "");
                }
                // set filename here.
                downloadLink.setFinalFileName(filename);
            } catch (final BrowserException e) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            downloadLink.setProperty("directlink", dllink);
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    private String getJson() throws PluginException {
        String json = br.getRegex("PxInitialData\\[\"photo\"\\] = (\\{.*?\\});\n").getMatch(0);
        if (json == null) {
            json = br.getRegex("window\\.PxPreloadedData = (\\{.*?\\});\n").getMatch(0);
            if (json == null) {
                // for offers
                json = br.getRegex("<script type='application/ld\\+json'>(.*?)</script>").getMatch(0);
                if (json == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        return json;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            try {
                dl.getConnection().disconnect();
            } catch (final Throwable e) {
            }
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

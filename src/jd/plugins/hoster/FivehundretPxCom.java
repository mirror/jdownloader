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

import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "500px.com" }, urls = { "https?://(?:www\\.)?500px\\.com/photo/(\\d+)(/[^/]+)?" })
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
    public String getLinkID(final DownloadLink link) {
        final String photoID = getPhotoID(link);
        if (photoID != null) {
            return this.getHost() + "://" + photoID;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public void init() {
        Browser.setRequestIntervalLimitGlobal("https://api.500px.com", 250);
    }

    private String getPhotoID(final DownloadLink link) {
        final String photoID = link.getStringProperty("photoID", null);
        if (photoID == null) {
            return getFID(link);
        } else {
            return photoID;
        }
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        dllink = null;
        link.setMimeHint(CompiledFiletypeFilter.ImageExtensions.JPG);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String photoID = this.getPhotoID(link);
        if (photoID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage("https://api." + this.getHost() + "/v1/photos?ids=" + photoID + "&image_size%5B%5D=1&image_size%5B%5D=2&image_size%5B%5D=32&image_size%5B%5D=31&image_size%5B%5D=33&image_size%5B%5D=34&image_size%5B%5D=35&image_size%5B%5D=36&image_size%5B%5D=2048&image_size%5B%5D=4&image_size%5B%5D=14&include_states=1&expanded_user_info=true&include_tags=true&include_geo=true&is_following=true&include_equipment_info=true&include_licensing=true&include_releases=true&liked_by=1&include_vendor_photos=true");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final Map<String, Object> photo = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "photos/" + photoID);
        if (photo == null) {
            /* 2020-09-09: E.g. {"photos":{}} --> Content offline */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String photoName = (String) photo.get("name");
        final String userName = (String) JavaScriptEngineFactory.walkJson(photo, "user/username");
        // String user_firstname = (String) JavaScriptEngineFactory.walkJson(photo, "user/firstname");
        // String user_lastname = (String) JavaScriptEngineFactory.walkJson(photo, "user/lastname");
        // array full of images, we need to analyse for best
        List<Object> images = (List<Object>) photo.get("images");
        if (images != null) {
            int sizeMax = -1;
            for (Object o : images) {
                Map<String, Object> data = (Map<String, Object>) o;
                if (data.containsKey("size")) {
                    int s = ((Number) data.get("size")).intValue();
                    if (s > sizeMax) {
                        sizeMax = s;
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
            dllink = (String) JavaScriptEngineFactory.walkJson(photo, "images/{4}/https_url");
            if (dllink == null) {
                dllink = (String) JavaScriptEngineFactory.walkJson(photo, "images/{3}/https_url");
            }
        }
        String fileName = userName;
        if (StringUtils.isNotEmpty(photoName)) {
            fileName = fileName + " - " + photoName;
        }
        fileName = fileName + " - " + photoID;
        fileName = fileName + ".jpg";
        if (!link.isNameSet()) {
            // set part name if not set previously
            link.setFinalFileName(fileName);
        } else {
            link.setName(fileName);
        }
        if (!StringUtils.isEmpty(dllink) && !(Thread.currentThread() instanceof SingleDownloadController)) {
            dllink = Encoding.htmlOnlyDecode(dllink);
            final Browser br2 = br.cloneBrowser();
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br2.openHeadConnection(dllink);
                if (looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setDownloadSize(con.getCompleteContentLength());
                    }
                    link.setProperty("directlink", dllink);
                    return AvailableStatus.TRUE;
                } else {
                    return AvailableStatus.UNCHECKABLE;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, false, 1);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
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

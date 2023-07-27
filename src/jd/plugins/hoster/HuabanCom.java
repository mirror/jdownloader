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

import java.util.Map;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "huaban.com" }, urls = { "https?://(?:www\\.)?huaban\\.com/pins/(\\d+)" })
public class HuabanCom extends PluginForHost {
    public HuabanCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_GALLERY, LazyPlugin.FEATURE.IMAGE_HOST };
    }

    @Override
    public String getAGBLink() {
        return "http://huaban.com/";
    }

    /* Site constants */
    public static final String default_extension = ".jpg";
    /* don't touch the following! */
    private String             dllink            = null;

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @SuppressWarnings({ "deprecation" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        String filename = null;
        final String pin_id = getFID(link);
        /* Display ids for offline links */
        if (!link.isNameSet()) {
            link.setName(pin_id + ".jpg");
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        String site_title = null;
        dllink = checkDirectLink(link, "free_directlink");
        if (dllink != null) {
            /* Avoid unnecessary site requests. */
            site_title = link.getFinalFileName();
            if (site_title == null) {
                site_title = pin_id;
            }
        } else {
            // final String source_url = link.getStringProperty("source_url", null);
            // final String boardid = link.getStringProperty("boardid", null);
            // final String username = link.getStringProperty("username", null);
            br.getPage(link.getDownloadURL());
            if (this.br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            /* 2nd check for offline */
            final String contentID = br.getRegex("data-content-id=\"(\\d+)\"").getMatch(0);
            if (!StringUtils.equals(pin_id, contentID)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            /*
             * Site actually contains similar json compared to API --> Grab that and get the final link via that as it is not always present
             * in the normal html code.
             */
            final String json = br.getRegex("id=\"__NEXT_DATA__\" type=\"application/json\"\\s*>(\\{.*?\\})</script>").getMatch(0);
            if (json == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(json);
            final String key = (String) JavaScriptEngineFactory.walkJson(entries, "props/pageProps/pin/file/key");
            if (key == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = "https://hbimg.huaban.com/" + key;
            link.setProperty("free_directlink", dllink);
        }
        final String ext = getFileNameExtensionFromString(dllink, default_extension);
        filename = pin_id;
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        link.setFinalFileName(filename);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        doFree(link, false, 1, "free_directlink");
    }

    private void doFree(final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // br.setCurrentURL("");
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty(directlinkproperty, dllink);
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        final String dllink = getStoredDirecturl(link, property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = new Browser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    return dllink;
                } else {
                    return null;
                }
            } catch (final Exception e) {
                return null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return null;
    }

    private String getStoredDirecturl(final DownloadLink link, final String property) {
        String url = link.getStringProperty(property);
        if (url != null) {
            /* 2022-09-16: Fix old stored URLs from up to rev 45207 */
            url = url.replaceFirst("https?://img\\.hb\\.aicdn\\.com/", "https://hbimg.huaban.com/");
            return url;
        }
        return null;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public String getDescription() {
        return "JDownloader's huaban.com plugin helps downloading pictures from huaban.com.";
    }

    public static final String  ENABLE_DESCRIPTION_IN_FILENAMES        = "ENABLE_DESCRIPTION_IN_FILENAMES";
    public static final boolean defaultENABLE_DESCRIPTION_IN_FILENAMES = false;

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ENABLE_DESCRIPTION_IN_FILENAMES, JDL.L("plugins.hoster.HuabanCom.enableDescriptionInFilenames", "Add pind-escription to filenames?\r\nNOTE: If enabled, Filenames might get very long!")).setDefaultValue(defaultENABLE_DESCRIPTION_IN_FILENAMES));
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
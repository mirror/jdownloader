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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.DebugMode;
import org.jdownloader.plugins.components.config.MydaddyCcConfig;
import org.jdownloader.plugins.components.config.MydaddyCcConfig.PreferredStreamQuality;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class MydaddyCc extends PluginForHost {
    public MydaddyCc(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "mydaddy.cc" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/video/([a-z0-9]+)");
        }
        return ret.toArray(new String[0]);
    }

    /* DEV NOTES */
    // Tags: porn plugin
    // protocol: no https
    // other:
    /* Connection stuff */
    private static final boolean  free_resume             = true;
    private static final int      free_maxchunks          = 0;
    private static final int      free_maxdownloads       = -1;
    private String                dllink                  = null;
    protected static final String PROPERTY_CHOSEN_QUALITY = "chosen_quality";
    public static final String    PROPERTY_ACTRESS_NAME   = "actress_name";
    public static final String    PROPERTY_CRAWLER_TITLE  = "decryptertitle";

    @Override
    public String getAGBLink() {
        return "https://mydaddy.cc/";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFID(link);
        if (linkid != null) {
            return this.getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        /* Host itself has no filenames so if we know the title from the website which embedded the content, prefer that! */
        final String accressName = link.getStringProperty(PROPERTY_ACTRESS_NAME);
        String title = link.getStringProperty(PROPERTY_CRAWLER_TITLE);
        if (title == null) {
            /* Fallback to videoID is filename. */
            title = getFID(link);
        }
        if (accressName != null && !title.contains(accressName)) {
            title = accressName + " - " + title;
        }
        link.setFinalFileName(title + ".mp4");
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404 || this.br.toString().length() < 500) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.getRequest().setHtmlCode(PluginJSonUtils.unescape(br.toString()));
        final HashMap<Integer, String> qualityMap = new HashMap<Integer, String>();
        final String[] sources = br.getRegex("<source src.*?/>").getColumn(-1);
        int qualityMax = -1;
        for (final String source : sources) {
            final Regex finfo = new Regex(source, "src=\"([^\"]*?(\\d+)\\.mp4)");
            final String url = finfo.getMatch(0);
            final String qualityStr = finfo.getMatch(1);
            if (qualityStr == null || url == null) {
                continue;
            }
            final int qualityTmp = Integer.parseInt(qualityStr);
            if (qualityTmp > qualityMax) {
                qualityMax = qualityTmp;
            }
            qualityMap.put(qualityTmp, url);
        }
        this.dllink = handleQualitySelection(link, qualityMap);
        final Browser br2 = br.cloneBrowser();
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openHeadConnection(dllink);
            if (this.looksLikeDownloadableContent(con)) {
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    /** Returns user preferred quality inside given quality map. Returns best, if user selection is not present in map. */
    protected final String handleQualitySelection(final DownloadLink link, final HashMap<Integer, String> qualityMap) {
        if (qualityMap.isEmpty()) {
            return null;
        }
        logger.info("Total found qualities: " + qualityMap.size());
        final Iterator<Entry<Integer, String>> iterator = qualityMap.entrySet().iterator();
        int maxQuality = 0;
        String maxQualityDownloadurl = null;
        final int userSelectedQuality = this.getPreferredStreamQuality();
        String selectedQualityDownloadurl = null;
        while (iterator.hasNext()) {
            final Entry<Integer, String> entry = iterator.next();
            final int qualityTmp = entry.getKey();
            if (qualityTmp > maxQuality) {
                maxQuality = entry.getKey();
                maxQualityDownloadurl = entry.getValue();
            }
            if (qualityTmp == userSelectedQuality) {
                selectedQualityDownloadurl = entry.getValue();
                break;
            }
        }
        final int chosenQuality;
        final String downloadurl;
        if (selectedQualityDownloadurl != null) {
            logger.info("Found user selected quality: " + userSelectedQuality + "p");
            chosenQuality = userSelectedQuality;
            downloadurl = selectedQualityDownloadurl;
        } else {
            logger.info("Auto-Chosen quality: " + maxQuality + "p");
            chosenQuality = maxQuality;
            downloadurl = maxQualityDownloadurl;
        }
        link.setProperty(PROPERTY_CHOSEN_QUALITY, chosenQuality);
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            link.setComment("ChosenQuality: " + chosenQuality + "p");
        }
        return downloadurl;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, free_resume, free_maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
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

    /** Returns user selected stream quality. -1 = BEST/default */
    private final int getPreferredStreamQuality() {
        final MydaddyCcConfig cfg = PluginJsonConfig.get(this.getConfigInterface());
        final PreferredStreamQuality quality = cfg.getPreferredStreamQuality();
        switch (quality) {
        case Q2160P:
            return 2160;
        case Q1440P:
            return 1440;
        case Q1080P:
            return 1080;
        case Q720P:
            return 720;
        case Q480P:
            return 480;
        case Q360P:
            return 360;
        case BEST:
        default:
            return -1;
        }
    }

    @Override
    public Class<? extends MydaddyCcConfig> getConfigInterface() {
        return MydaddyCcConfig.class;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
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

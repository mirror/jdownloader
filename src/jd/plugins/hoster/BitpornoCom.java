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
import java.util.List;
import java.util.Map;

import org.appwork.utils.StringUtils;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.HTMLSearch;
import jd.parser.html.InputField;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class BitpornoCom extends PluginForHost {
    public BitpornoCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        ret.add(new String[] { "bitporno.to", "bitporno.sx", "bitporno.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:\\?v=|v/|embed/)([A-Za-z0-9]+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String rewriteHost(final String host) {
        /* 2022-07-18: Main domain changed from upfiles.io to upfiles.com (upfiles.app) */
        return this.rewriteHost(getPluginDomains(), host);
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:
    /* Connection stuff */
    private static final boolean free_resume         = true;
    private static final int     free_maxchunks      = 0;
    private static final int     free_maxdownloads   = -1;
    private static final String  html_video_encoding = "(?i)>\\s*This video is still in encoding progress";
    private String               dllink              = null;

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
    public String getAGBLink() {
        return "http://www.bitporno.to/?c=tos";
    }

    private boolean handleConfirm(Browser br) throws IOException {
        final Form f = br.getForm(0);
        if (f != null) {
            final InputField confirm = f.getInputField("confirm");
            if (confirm != null && confirm.isType(InputField.InputType.IMAGE)) {
                f.put("confirm.x", "62");
                f.put("confirm.y", "70");
            }
            br.submitForm(f);
            return true;
        }
        return false;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    /** 2016-05-18: playernaut.com uses crypted js, bitporno.sx doesn't! */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        dllink = null;
        final String extDefault = ".mp4";
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String fid = getFID(link);
        if (!link.isNameSet()) {
            /* Better filenames for offline case */
            link.setName(fid + extDefault);
        }
        String title = null;
        String json_source = null;
        /* Only use one of their domains */
        br.getPage("https://www." + this.getHost() + "/?v=" + fid);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        title = br.getRegex("<title>(.*?)</title>").getMatch(0);
        if (title == null) {
            title = br.getRegex("<span itemprop=\"name\" title=\"(.*?)\"").getMatch(0);
        }
        if (title == null) {
            /* Fallback */
            title = fid;
        }
        if (title.length() > 212) {
            int dash = title.indexOf('-', 200);
            if (dash >= 0) {
                title = title.substring(0, dash);
            } else {
                title = title.substring(0, 212);
            }
        }
        final String description = HTMLSearch.searchMetaTag(br, "og:description");
        if (!StringUtils.isEmpty(description) && link.getComment() == null) {
            link.setComment(description);
        }
        if (br.containsHTML(html_video_encoding)) {
            return AvailableStatus.TRUE;
        }
        // only available until hls version is transcoded/available
        final String mp4File = br.getRegex("file\\s*:\\s*\"((?:https?://|/)[^<>\"]+\\.mp4)\"").getMatch(0);
        // from iframe
        br.getPage("/embed/" + fid);
        handleConfirm(br);
        final String decode = new org.jdownloader.encoding.AADecoder(br.toString()).decode();
        json_source = new Regex(decode != null ? decode : br.toString(), "sources(?:\")?[\t\n\r ]*?:[\t\n\r ]*?(\\[.*?\\])").getMatch(0);
        if (title == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (json_source != null) {
            final String userPreferredVideoquality = getConfiguredVideoQuality();
            logger.info("userPreferredVideoquality: " + userPreferredVideoquality);
            String dllink_user_prefered = null;
            String dllink_temp = null;
            String dllink_best = null;
            final List<Object> ressourcelist = (List) JavaScriptEngineFactory.jsonToJavaObject(json_source);
            Map<String, Object> entries = null;
            int maxvalue = 0;
            int tempvalue = 0;
            String tempquality = null;
            for (final Object videoo : ressourcelist) {
                if (videoo instanceof Map) {
                    entries = (Map<String, Object>) videoo;
                    tempquality = (String) entries.get("label");
                    dllink_temp = (String) entries.get("file");
                    logger.info("label: " + tempquality + " file: " + dllink_temp);
                    if (StringUtils.isEmpty(tempquality) || StringUtils.isEmpty(dllink_temp)) {
                        /* Skip invalid objects */
                        continue;
                    }
                    if (tempquality.equalsIgnoreCase(userPreferredVideoquality)) {
                        logger.info("Found user selected videoquality: " + tempquality);
                        dllink_user_prefered = dllink_temp;
                        break;
                    }
                    // if ("Source( File)?".equalsIgnoreCase(tempquality)) {
                    if (tempquality.contains("Source")) {
                        /* That IS the highest quality */
                        tempvalue = 100000;
                        dllink_best = dllink_temp;
                    } else {
                        /* Look for the highest quality! */
                        tempvalue = Integer.parseInt(new Regex(tempquality, "(\\d+)p?").getMatch(0));
                    }
                    if (tempvalue > maxvalue) {
                        maxvalue = tempvalue;
                        dllink_best = dllink_temp;
                    }
                }
            }
            if (dllink_user_prefered != null) {
                logger.info("Downloading user-selected quality");
                dllink = dllink_user_prefered;
            } else {
                logger.info("Downloading highest quality possible");
                dllink = dllink_best;
                logger.info("file: " + dllink_best);
            }
        } else {
            // final String userPreferredVideoquality = getConfiguredVideoQuality();
            // String embed = null;
            // could be <source>, seems that it also shows highest quality to change you do another page grab to '&q=480p | &q=360p'
            final String[] source = br.getRegex("<source .*?/\\s*>").getColumn(-1);
            if (source != null) {
                int best = 0;
                for (String s : source) {
                    final String d = new Regex(s, "src=(\"|')(.*?)\\1").getMatch(1);
                    final String q = new Regex(s, "data-res=(\"|')(\\d+)p?\\1").getMatch(1);
                    logger.info("d: " + d + ", q: " + q);
                    if (d == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    if (q != null && d != null) {
                        if (best < Integer.parseInt(q)) {
                            dllink = d;
                            best = Integer.parseInt(q);
                        }
                    }
                    if (q == null && d != null && s.contains("data-res=\"Source File\"")) {
                        dllink = d;
                    }
                }
            }
        }
        if (StringUtils.isEmpty(this.dllink)) {
            /* 2020-08-25: HLS */
            dllink = br.getRegex("file\\s*:\\s*\"((?:https?://|/)[^<>\"]+)\"").getMatch(0);
        }
        if (title != null) {
            title = Encoding.htmlDecode(title);
            title = title.trim();
            link.setName(title + extDefault);
        }
        if (!StringUtils.isEmpty(dllink) && !dllink.contains(".m3u8") && !isDownload) {
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(this.dllink);
                handleConnectionErrors(br, con);
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
                final String ext = Plugin.getExtensionFromMimeTypeStatic(con.getContentType());
                if (ext != null) {
                    link.setFinalFileName(this.correctOrApplyFileNameExtension(title, "." + ext));
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
        requestFileInformation(link, true);
        if (br.containsHTML(html_video_encoding)) {
            /*
             * 2016-06-16, psp: I guess if this message appears longer than some hours, such videos can never be downloaded/streamed or only
             * the original file via premium account.
             */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Not downloadable (yet) because 'This video is still in encoding progress - Please patient'", 60 * 60 * 1000l);
        } else if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown error occured");
        } else if (dllink.contains(".m3u8")) {
            /* HLS download */
            br.getPage(dllink);
            final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, hlsbest.getDownloadurl());
            dl.startDownload();
        } else {
            /* http download */
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, free_resume, free_maxchunks);
            handleConnectionErrors(br, dl.getConnection());
            dl.startDownload();
        }
    }

    private void handleConnectionErrors(final Browser br, final URLConnectionAdapter con) throws PluginException, IOException {
        if (!this.looksLikeDownloadableContent(con)) {
            br.followConnection(true);
            if (con.getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Video broken?");
            }
        }
    }

    private String getConfiguredVideoQuality() {
        final int selection = this.getPluginConfig().getIntegerProperty(SELECTED_VIDEO_FORMAT, 0);
        final String selectedQuality = FORMATS[selection];
        return selectedQuality;
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), SELECTED_VIDEO_FORMAT, FORMATS, "Select preferred videoquality:").setDefaultValue(0));
    }

    /* The list of qualities displayed to the user */
    private final String[] FORMATS               = new String[] { "Source", "360p", "480p", "720p", "1080p HD" };
    private final String   SELECTED_VIDEO_FORMAT = "SELECTED_VIDEO_FORMAT";

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

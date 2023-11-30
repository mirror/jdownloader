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
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.config.FreeM3DownloadNetConfig;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class FreeM3DownloadNet extends PluginForHost {
    public FreeM3DownloadNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://free-mp3-download.net/";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "free-mp3-download.net" });
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
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/download\\.php\\?id=(\\d+)(?:\\&q=[a-zA-Z0-9_/\\+\\=\\-%]+)?");
        }
        return ret.toArray(new String[0]);
    }

    /* Connection stuff */
    private static final boolean              FREE_RESUME          = false;
    private static final int                  FREE_MAXCHUNKS       = 1;
    /* 2021-09-06: Only allow one download as a captcha may be required once per session. */
    private static final int                  FREE_MAXDOWNLOADS    = 20;
    private static final String               PROPERTY_PREFER_FLAC = "prefer_flac";
    private static final String               API_QUERIED          = "api_queried";
    protected static HashMap<String, Cookies> antiCaptchaCookies   = new HashMap<String, Cookies>();
    /* don't touch the following! */
    private static Map<String, AtomicInteger> freeRunning          = new HashMap<String, AtomicInteger>();

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

    protected void loadAntiCaptchaCookies(final Browser prepBr, final String host) {
        synchronized (antiCaptchaCookies) {
            if (!antiCaptchaCookies.isEmpty()) {
                for (final Map.Entry<String, Cookies> cookieEntry : antiCaptchaCookies.entrySet()) {
                    final String key = cookieEntry.getKey();
                    if (key != null && key.equals(host)) {
                        try {
                            prepBr.setCookies(key, cookieEntry.getValue(), false);
                        } catch (final Throwable e) {
                        }
                    }
                }
            }
        }
    }

    private String getFallbackFilename(final DownloadLink link) throws MalformedURLException, UnsupportedEncodingException {
        final UrlQuery query = UrlQuery.parse(link.getPluginPatternMatcher());
        final String searchTermB64 = query.get("q");
        final String fid = this.getFID(link);
        String title;
        if (searchTermB64 != null) {
            title = fid + "_" + Encoding.htmlDecode(Encoding.htmlDecode(Encoding.Base64Decode(searchTermB64)).trim());
        } else {
            title = fid;
        }
        if (PluginJsonConfig.get(FreeM3DownloadNetConfig.class).isPreferFLAC()) {
            title += ".flac";
        } else {
            title += ".mp3";
        }
        return title;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (!link.isNameSet()) {
            /* Fallback */
            link.setName(getFallbackFilename(link));
        }
        loadAntiCaptchaCookies(this.br, this.getHost());
        br.setFollowRedirects(true);
        /* 2021-09-14: Without main page as Referer, all URLs will redirect to main page. */
        br.getHeaders().put("Referer", "https://" + this.getHost() + "/");
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!br.getURL().contains(this.getFID(link))) {
            /* E.g. redirect to mainpage */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /*
         * 2022-09-14: In browser they're loading this information from search context so for us filename will usually not be available here
         * until download is started.
         */
        String filename = link.getStringProperty(API_QUERIED, null);
        if (filename == null) {
            filename = br.getRegex("(?i)<p>Name\\s*:([^<>\"]+)</p>").getMatch(0);
            if (filename == null && !link.hasProperty(API_QUERIED)) {
                final String fid = getFID(link);
                try {
                    final Browser brc = br.cloneBrowser();
                    brc.setRequest(null);
                    brc.getPage("https://api.deezer.com/track/" + fid + "?output=jsonp&callback=jQuery" + System.currentTimeMillis() + "_" + System.currentTimeMillis() + "&_=" + System.currentTimeMillis());
                    final String json = brc.getRegex("jQuery\\d+_\\d+\\((\\{.+\\})\\)").getMatch(0);
                    final Map<String, Object> entries = restoreFromString(json, TypeRef.MAP);
                    final Map<String, Object> artist = (Map<String, Object>) entries.get("artist");
                    filename = artist.get("name") + " - " + entries.get("title").toString();
                    link.setProperty(API_QUERIED, StringUtils.valueOrEmpty(filename));
                } catch (final Exception e) {
                    logger.log(e);
                    link.setProperty(API_QUERIED, "");
                    logger.info("API query failed");
                }
            }
        }
        final String[] formatInfo = getFormatInfo(br);
        if (StringUtils.isNotEmpty(filename)) {
            filename = Encoding.htmlDecode(filename).trim();
            link.setFinalFileName(filename + "." + formatInfo[3]);
        }
        final String filesizeStr = formatInfo[2];
        if (filesizeStr != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesizeStr));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        handleDownload(link);
    }

    /** Returns array with information about preferred format. */
    private String[] getFormatInfo(final Browser br) throws PluginException {
        final String[] formathtmls = br.getRegex("(<input[^<>]*name=\"format-type\".*?</label>)").getColumn(0);
        final String[] flacInfo = new String[4];
        flacInfo[3] = "flac";
        final String[] mp3Info = new String[4];
        mp3Info[3] = "mp3";
        for (final String formathtml : formathtmls) {
            final String id = new Regex(formathtml, "id=\"([^\"]+)\"").getMatch(0);
            if (id == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final Regex labelAndFilesize = new Regex(formathtml, "class=\"quality-label\"[^>]*>([^<>]+) \\((\\d+(\\.\\d{1,2})? [^<]*)\\)</label>");
            final String label = labelAndFilesize.getMatch(0);
            final String filesizeStr = labelAndFilesize.getMatch(1);
            if (id.equalsIgnoreCase("flac")) {
                flacInfo[0] = id;
                flacInfo[1] = label;
                flacInfo[2] = filesizeStr;
            } else {
                /* MP3 can be available in multiple qualities. Last = Best/highest bitrate. */
                mp3Info[0] = id;
                mp3Info[1] = label;
                mp3Info[2] = filesizeStr;
            }
        }
        if (PluginJsonConfig.get(FreeM3DownloadNetConfig.class).isPreferFLAC()) {
            return flacInfo;
        } else {
            return mp3Info;
        }
    }

    private void handleDownload(final DownloadLink link) throws Exception, PluginException {
        final boolean preferFlac = this.getPreferFlac(link);
        final String directlinkproperty;
        if (preferFlac) {
            directlinkproperty = "directurl_flac";
        } else {
            directlinkproperty = "directurl_mp3";
        }
        if (!attemptStoredDownloadurlDownload(link, directlinkproperty, FREE_RESUME, FREE_MAXCHUNKS)) {
            String dllink = null;
            String formatLabel = null;
            synchronized (antiCaptchaCookies) {
                requestFileInformation(link);
                final Map<String, Object> postdata = new HashMap<String, Object>();
                postdata.put("i", Long.parseLong(this.getFID(link)));
                /* Random 20 char lowercase string --> We'll just use an UUID */
                // final String str = UUID.randomUUID().toString();
                // postdata.put("ch", str);
                final String[] formatInfo = getFormatInfo(br);
                final String formatInternalName = formatInfo[0];
                formatLabel = formatInfo[1];
                if (formatInternalName == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                postdata.put("f", Encoding.urlEncode(formatInternalName));
                boolean captchaRequiredInThisRun;
                if (br.containsHTML("class=\"g-recaptcha\"")) {
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                    captchaRequiredInThisRun = true;
                    postdata.put("h", recaptchaV2Response);
                } else {
                    captchaRequiredInThisRun = false;
                    postdata.put("h", "");
                }
                br.postPageRaw("/dl.php?", JSonStorage.serializeToJson(postdata));
                dllink = br.getRedirectLocation();
                if (dllink == null || br.getRequest().getHtmlCode().startsWith("http")) {
                    dllink = br.getRequest().getHtmlCode();
                }
                if (StringUtils.isEmpty(dllink)) {
                    if (br.containsHTML("^https?://free-mp3-download\\.net/?$")) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    } else {
                        logger.warning("Failed to find final downloadurl");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
                if (captchaRequiredInThisRun) {
                    antiCaptchaCookies.put(this.getHost(), br.getCookies(br.getHost()));
                }
                if (preferFlac) {
                    /*
                     * Set property so next time flac will be auto preferred so user can't change preferred format to mp3 and then resume a
                     * previously started flac download which would result in a corrupt file.
                     */
                    link.setProperty(PROPERTY_PREFER_FLAC, true);
                }
                link.setProperty(directlinkproperty, dllink);
            }
            final String textForBrokenOrUnavailableAudioFiles = "Broken audio file or chosen format " + formatLabel + " is not available";
            if (dllink.matches("(?i)^https?://[^/]+/?$")) {
                /* 2023-11-30: This can happen via browser too. */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, textForBrokenOrUnavailableAudioFiles);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, FREE_RESUME, FREE_MAXCHUNKS);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 5 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 5 * 60 * 1000l);
                } else {
                    /* 2023-11-30: This can happen via browser too. */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, textForBrokenOrUnavailableAudioFiles);
                }
            }
        }
        /* add a download slot */
        controlMaxFreeDownloads(null, link, +1);
        try {
            /* start the dl */
            dl.startDownload();
        } finally {
            /* remove download slot */
            controlMaxFreeDownloads(null, link, -1);
        }
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
        /* Requires one captcha per session. */
        return true;
    }

    private boolean getPreferFlac(final DownloadLink link) {
        if (link.hasProperty(PROPERTY_PREFER_FLAC)) {
            /* Return last saved property. */
            return true;
        } else {
            /* Return current user selection. */
            return PluginJsonConfig.get(FreeM3DownloadNetConfig.class).isPreferFLAC();
        }
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link, final String directlinkproperty, final boolean resumable, final int maxchunks) throws Exception {
        final String url = link.getStringProperty(directlinkproperty);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, resumable, maxchunks);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                return true;
            } else {
                brc.followConnection(true);
                link.removeProperty(directlinkproperty);
                throw new IOException();
            }
        } catch (final Throwable e) {
            logger.log(e);
            try {
                dl.getConnection().disconnect();
            } catch (Throwable ignore) {
            }
            return false;
        }
    }

    /**
     * Prevents more than one free download from starting at a given time. One step prior to dl.startDownload(), it adds a slot to maxFree
     * which allows the next singleton download to start, or at least try.
     *
     * This is needed because this website may ask for a session captcha once per session (per X time) so starting multiple downloads at the
     * same time could result in multiple captchas -> We want to avoid that.
     *
     * @param num
     *            : (+1|-1)
     */
    protected void controlMaxFreeDownloads(final Account account, final DownloadLink link, final int num) {
        if (account == null) {
            final AtomicInteger freeRunning = getFreeRunning();
            synchronized (freeRunning) {
                final int before = freeRunning.get();
                final int after = before + num;
                freeRunning.set(after);
                logger.info("freeRunning(" + link.getName() + ")|max:" + getMaxSimultanFreeDownloadNum() + "|before:" + before + "|after:" + after + "|num:" + num);
            }
        }
    }

    protected AtomicInteger getFreeRunning() {
        synchronized (freeRunning) {
            AtomicInteger ret = freeRunning.get(getHost());
            if (ret == null) {
                ret = new AtomicInteger(0);
                freeRunning.put(getHost(), ret);
            }
            return ret;
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        final int max = getMaxSimultaneousFreeAnonymousDownloads();
        if (max == -1) {
            return -1;
        } else {
            final int running = getFreeRunning().get();
            final int ret = Math.min(running + 1, max);
            return ret;
        }
    }

    public int getMaxSimultaneousFreeAnonymousDownloads() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        link.removeProperty(PROPERTY_PREFER_FLAC);
        link.removeProperty(API_QUERIED);
    }

    @Override
    public Class<? extends FreeM3DownloadNetConfig> getConfigInterface() {
        return FreeM3DownloadNetConfig.class;
    }
}
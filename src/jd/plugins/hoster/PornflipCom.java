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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.downloader.hds.HDSDownloader;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hds.HDSContainer;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "playvids.com", "pornflip.com" }, urls = { "http://playviddecrypted\\.com/\\d+", "http://playviddecrypted\\.com/\\d+" })
public class PornflipCom extends PluginForHost {
    public PornflipCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.pornflip.com/de/account/join");
        this.setConfigElements();
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    private String dllink = null;

    @Override
    public String getAGBLink() {
        return "https://www.playvids.com/de/terms";
    }

    /** Settings stuff */
    private static final String FASTLINKCHECK = "FASTLINKCHECK";
    public static final String  ALLOW_BEST    = "ALLOW_BEST";
    public static final String  ALLOW_360P    = "ALLOW_360P";
    public static final String  ALLOW_480P    = "ALLOW_480P";
    public static final String  ALLOW_720P    = "ALLOW_720P";
    public static final String  ALLOW_1080    = "ALLOW_1080";
    public static final String  ALLOW_2160    = "ALLOW_2160";
    public static final String  quality_360   = "360p";
    public static final String  quality_480   = "480p";
    public static final String  quality_720   = "720p";
    public static final String  quality_1080  = "1080p";
    public static final String  quality_2160  = "2160p";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, AccountController.getInstance().getValidAccount(this.getHost()));
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        if (link.getBooleanProperty("offline", false)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String qualityvalue = link.getStringProperty("qualityvalue", null);
        this.setBrowserExclusive();
        if (account != null) {
            login(this.br, account, false);
        }
        final String stored_directurl = link.getStringProperty("directlink");
        if (!StringUtils.containsIgnoreCase(qualityvalue, "hds_") && !StringUtils.containsIgnoreCase(stored_directurl, ".m3u8")) {
            br.setFollowRedirects(true);
            String filename = link.getStringProperty("directname", null);
            dllink = checkDirectLink(link, "directlink");
            if (dllink == null) {
                /* Refresh directlink */
                br.getPage(link.getStringProperty("mainlink", null));
                if (isOffline(this.br)) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                final String videosource = getVideosource(this.br);
                if (videosource == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (quality_720.equals(qualityvalue) && account == null) {
                    logger.info("User is not logged in but tries to download a quality which needs login");
                    return AvailableStatus.TRUE;
                }
                dllink = getQuality(qualityvalue, videosource);
            }
            if (filename == null || dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setFinalFileName(filename);
            // In case the link redirects to the finallink
            if (link.getKnownDownloadSize() == -1) {
                URLConnectionAdapter con = null;
                try {
                    con = br.openHeadConnection(dllink);
                    if (!con.getContentType().contains("html") && con.isOK()) {
                        link.setDownloadSize(con.getLongContentLength());
                    } else {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    link.setProperty("directlink", dllink);
                } finally {
                    try {
                        if (con != null) {
                            con.disconnect();
                        }
                    } catch (Throwable e) {
                    }
                }
            }
        } else if (StringUtils.containsIgnoreCase(stored_directurl, ".m3u8")) {
            this.dllink = stored_directurl;
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        doDownload(link, null);
    }

    private void doDownload(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, account);
        final String qualityvalue = link.getStringProperty("qualityvalue");
        if (qualityvalue == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (StringUtils.containsIgnoreCase(qualityvalue, "hds_")) {
            final HDSContainer container = HDSContainer.read(link);
            if (container == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                br.setFollowRedirects(true);
                br.getPage(link.getStringProperty("mainlink", null).replace("http://", "https://"));
                final LinkedHashMap<String, String> foundQualities = getQualities(br);
                final String f4m = foundQualities.get(qualityvalue);
                if (f4m == null) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                br.getPage(f4m);
                br.followRedirect();
                final List<HDSContainer> all = HDSContainer.getHDSQualities(br);
                if (all == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    final HDSContainer hit = HDSContainer.getBestMatchingContainer(all, container);
                    if (hit == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    } else {
                        hit.write(link);
                        final HDSDownloader dl = new HDSDownloader(link, br, hit.getFragmentURL());
                        this.dl = dl;
                        dl.setEstimatedDuration(hit.getDuration());
                    }
                }
                dl.startDownload();
            }
        } else if (StringUtils.containsIgnoreCase(dllink, ".m3u8")) {
            /* HLS download */
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, dllink);
            dl.startDownload();
        } else {
            if (quality_720.equals(qualityvalue) && account == null) {
                /* Should never happen! */
                logger.info("User is not logged in but tries to download a quality which needs login");
                throw new AccountRequiredException();
            } else {
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
                if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                    try {
                        br.followConnection(true);
                    } catch (final IOException e) {
                        logger.log(e);
                    }
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            dl.startDownload();
        }
    }

    public static final boolean isOffline(final Browser br) {
        if (br.containsHTML("Video not found<|>This video has been removed|class=\"play\\-error\"|class=\"error\\-sorry\"") || br.getHttpConnection().getResponseCode() == 404) {
            return true;
        } else {
            return false;
        }
    }

    public void login(final Browser br, final Account account, final boolean verify) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    br.setCookies(cookies);
                    if (!verify) {
                        logger.info("Set cookies without check");
                        return;
                    } else {
                        logger.info("Attempting cookie login");
                        br.getPage("https://www." + this.getHost() + "/");
                        if (this.isLoggedIN(br)) {
                            logger.info("Cookie login successful");
                            account.saveCookies(br.getCookies(br.getURL()), "");
                            return;
                        } else {
                            logger.info("Cookie login failed");
                            br.clearCookies(null);
                        }
                    }
                }
                logger.info("Performing full login");
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.postPage("https://www." + this.getHost() + "/de/account/login", "remember_me=on&back_url=&login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                if (br.containsHTML("\"status\":\"error\"")) {
                    throw new AccountInvalidException();
                }
                String continuelink = null;
                try {
                    final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                    continuelink = entries.get("redirect").toString();
                } catch (final Throwable ignore) {
                }
                if (continuelink != null) {
                    if (continuelink.isEmpty()) {
                        continuelink = "/";
                    }
                    br.getPage(continuelink);
                } else {
                    br.getPage("/");
                }
                if (!isLoggedIN(br)) {
                    throw new AccountInvalidException();
                }
                account.saveCookies(br.getCookies(br.getURL()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    public boolean isLoggedIN(final Browser br) {
        return br.getCookie(br.getHost(), "sunsid", Cookies.NOTDELETEDPATTERN) != null && br.containsHTML("account/logout");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(this.br, account, true);
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        doDownload(link, account);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    public static String getVideosource(final Browser br) {
        String videosource = br.getRegex("flashvars=\"(.*?)\"").getMatch(0);
        if (videosource != null) {
            /* Old */
            videosource = Encoding.htmlDecode(videosource);
        } else {
            /* 2019-07-26: New */
            videosource = br.toString();
        }
        return videosource;
    }

    public static LinkedHashMap<String, String> getQualities(final Browser br) {
        final String videosource = getVideosource(br);
        if (videosource == null) {
            return null;
        }
        final LinkedHashMap<String, String> foundqualities = new LinkedHashMap<String, String>();
        /** Decrypt qualities START */
        /** First, find all available qualities */
        final String[] qualities = { "hds_manifest", "hds_manifest_720", "hds_manifest_480", "hds_manifest_360", "2160p", "1080p", "720p", "480p", "360p", "data-hls-src1080", "data-hls-src720", "data-hls-src360", "data-hls-src480" };
        for (final String quality : qualities) {
            final String currentQualityUrl = getQuality(quality, videosource);
            if (currentQualityUrl != null) {
                foundqualities.put(quality, currentQualityUrl);
            }
        }
        /** Decrypt qualities END */
        return foundqualities;
    }

    public static String getQuality(final String quality, final String videosource) {
        String videourl = new Regex(videosource, "video_vars(?:\\[video_urls\\])?\\[" + quality + "\\]= ?(https?://[^<>\"]*?)(\\&(?!sec)|$)").getMatch(0);
        if (videourl == null) {
            /* 2020-09-01 */
            videourl = new Regex(videosource, quality + "=\"(https?://[^\"]+)\"").getMatch(0);
            if (videourl == null) {
                /* 2019-07-26 */
                final String qualityP = new Regex(quality, "(\\d+)p").getMatch(0);
                if (qualityP != null) {
                    videourl = new Regex(videosource, "data\\-src" + qualityP + "=\"(https?[^\"]+)\"").getMatch(0);
                }
            }
        }
        if (Encoding.isHtmlEntityCoded(videourl)) {
            videourl = Encoding.htmlDecode(videourl);
        }
        return videourl;
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    return dllink;
                } else {
                    throw new IOException();
                }
            } catch (final Exception e) {
                link.removeProperty(property);
                logger.log(e);
                return null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return null;
    }

    @Override
    public String getDescription() {
        return "JDownloader's PlayVid Plugin helps downloading Videoclips from pornflip.com. There are different video formats and qualities.";
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FASTLINKCHECK, "Fast linkcheck (filesize won't be shown in linkgrabber)?").setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        final ConfigEntry hq = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_BEST, "Only grab the best available resolution").setDefaultValue(false);
        getConfig().addEntry(hq);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_360P, "Grab 360p?").setDefaultValue(true).setEnabledCondidtion(hq, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_480P, "Grab 480p?").setDefaultValue(true).setEnabledCondidtion(hq, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_720P, "Grab 720p?").setDefaultValue(true).setEnabledCondidtion(hq, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_1080, "Grab 1080p?").setDefaultValue(true).setEnabledCondidtion(hq, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_2160, "Grab 4k?").setDefaultValue(true).setEnabledCondidtion(hq, false));
    }

    public Browser prepBrowser(final Browser prepBr) {
        prepBr.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        prepBr.setAllowedResponseCodes(429);
        return prepBr;
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

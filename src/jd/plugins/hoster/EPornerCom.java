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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.config.EpornerComConfig;
import org.jdownloader.plugins.components.config.EpornerComConfig.PreferredStreamQuality;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class EPornerCom extends PluginForHost {
    public EPornerCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.eporner.com/");
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(410);
        return br;
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "eporner.com" });
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
            ret.add("https?://(?:\\w+\\.)?" + buildHostsPatternPart(domains) + "/((?:hd\\-porn/|video-)\\w+(/([^/]+))?|photo/[A-Za-z0-9]+(/[\\w\\-]+/)?)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "https://www.eporner.com/terms/";
    }

    private final Pattern      PATTERN_VIDEO      = Pattern.compile("(?i)https?://[^/]+/(?:hd\\-porn/|video-)(\\w+)(/([^/]+))?");
    private final Pattern      PATTERN_PHOTO      = Pattern.compile("(?i)https?://[^/]+/photo/([A-Za-z0-9]+)(/([\\w\\-]+)/)?");
    private String             vq                 = null;
    public static final String PROPERTY_DIRECTURL = "directurl";

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

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
        final Regex videoRegex = new Regex(link.getPluginPatternMatcher(), PATTERN_VIDEO);
        if (videoRegex.patternFind()) {
            return videoRegex.getMatch(0);
        } else {
            return new Regex(link.getPluginPatternMatcher(), PATTERN_PHOTO).getMatch(0);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, null, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        final String extDefault;
        final boolean isVideo;
        final Regex videoRegex = new Regex(link.getPluginPatternMatcher(), PATTERN_VIDEO);
        String titleByURL = null;
        if (videoRegex.patternFind()) {
            extDefault = ".mp4";
            isVideo = true;
            titleByURL = videoRegex.getMatch(2);
        } else {
            extDefault = ".jpg";
            isVideo = false;
            titleByURL = new Regex(link.getPluginPatternMatcher(), PATTERN_PHOTO).getMatch(2);
        }
        String fallbackFilename;
        if (titleByURL != null) {
            fallbackFilename = titleByURL.replace("-", " ").trim() + extDefault;
        } else {
            fallbackFilename = this.getFID(link) + extDefault;
        }
        if (!link.isNameSet()) {
            link.setName(fallbackFilename);
        }
        if (account != null) {
            this.login(account, false);
        }
        br.getPage(link.getPluginPatternMatcher());
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (this.br.getHttpConnection().getResponseCode() == 410) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("id=\"deletedfile\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!this.br.getURL().contains(this.getFID(link))) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        long filesize = -1;
        String dllink = null;
        long filesizeBestH264 = 0;
        long filesizeSelectedH264 = 0;
        String dllinkBestH264 = null;
        String dllinkSelectedH264 = null;
        long filesizeBestAV1 = 0;
        long filesizeSelectedAV1 = -1;
        String dllinkBestAV1 = null;
        String dllinkSelectedAV1 = null;
        String title = br.getRegex("(?i)<title>([^<]+)</title>").getMatch(0);
        if (isVideo) {
            final String betterTitleByURL = new Regex(br.getURL(), PATTERN_VIDEO).getMatch(2);
            if (betterTitleByURL != null) {
                fallbackFilename = betterTitleByURL.replace("-", " ").trim() + extDefault;
            }
            vq = getPreferredStreamQuality();
            /* Official downloadurls */
            final String[][] dloadinfo = br.getRegex("(?i)href=\"(/dload/[^<>\"]+)\"[^>]*>[^<]* MP4 \\((\\d+)p, ([^<>\"]+)\\)</a>").getMatches();
            if (dloadinfo != null && dloadinfo.length != 0) {
                for (final String[] dlinfo : dloadinfo) {
                    final String directurl = dlinfo[0];
                    // final String heightStr = dlinfo[1];
                    final String filesizeStr = dlinfo[2];
                    final long tempsize = SizeFormatter.getSize(filesizeStr);
                    if (StringUtils.containsIgnoreCase(directurl, "av1")) {
                        if (tempsize > filesizeBestAV1 || dllinkBestAV1 == null) {
                            filesizeBestAV1 = tempsize;
                            dllinkBestAV1 = directurl;
                        }
                        if (vq != null && directurl.contains(vq)) {
                            dllinkSelectedAV1 = directurl;
                            filesizeSelectedAV1 = tempsize;
                        }
                    } else {
                        if (tempsize > filesizeBestH264 || dllinkBestH264 == null) {
                            filesizeBestH264 = tempsize;
                            dllinkBestH264 = directurl;
                        }
                        if (vq != null && directurl.contains(vq)) {
                            dllinkSelectedH264 = directurl;
                            filesizeSelectedH264 = tempsize;
                        }
                    }
                }
            }
            // TODO: Implement setting for preferred video codec
            if (dllinkSelectedAV1 != null) {
                dllink = dllinkSelectedAV1;
                filesize = filesizeSelectedAV1;
            } else if (dllinkSelectedH264 != null) {
                dllink = dllinkSelectedH264;
                filesize = filesizeSelectedH264;
            } else if (dllinkBestAV1 != null) {
                dllink = dllinkBestAV1;
                filesize = filesizeBestAV1;
            } else if (dllinkBestH264 != null) {
                dllink = dllinkBestH264;
                filesize = filesizeBestH264;
            }
            if (dllink == null && isDownload) {
                /* Fallback to stream download */
                final String correctedBR = br.getRequest().getHtmlCode().replace("\\", "");
                final String continueLink = new Regex(correctedBR, "(\"|\\')(/config\\d+/\\w+/[0-9a-f]+(/)?)(\"|\\')").getMatch(1);
                if (continueLink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                br.getPage(Encoding.htmlDecode(continueLink) + (continueLink.endsWith("/") ? "1920" : "/1920"));
                dllink = br.getRegex("<hd\\.file>(https?://.*?)</hd\\.file>").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("<file>(https?://.*?)</file>").getMatch(0);
                    if (dllink == null) {
                        dllink = br.getRegex("file:[\r\n\r ]*?\"(https?://[^<>\"]*?)\"").getMatch(0);
                    }
                }
            }
        } else {
            /* Photo */
            dllink = br.getRegex("src=\"(https?://[^\"]+)\" autoplay ").getMatch(0); // gifs -> mp4
            if (dllink == null) {
                dllink = br.getRegex("class=\"mainphoto\" src=\"(https?://[^\"]+)").getMatch(0); // normal image -> jpg
            }
            final String[] jsons = br.getRegex("<script type=\"application/ld\\+json\">([^<]+)</script>").getColumn(0);
            String betterPhotoTitle = null;
            if (jsons != null && jsons.length > 0) {
                for (final String json : jsons) {
                    final Map<String, Object> entries = restoreFromString(json, TypeRef.MAP);
                    betterPhotoTitle = (String) entries.get("name");
                    if (betterPhotoTitle != null) {
                        break;
                    }
                }
            }
            if (betterPhotoTitle == null) {
                logger.warning("Failed to find betterPhotoTitle");
            } else {
                title = betterPhotoTitle;
            }
        }
        String ext = null;
        if (dllink != null) {
            ext = Plugin.getFileNameExtensionFromURL(dllink);
        }
        if (ext == null) {
            ext = extDefault;
        }
        if (title != null) {
            title = Encoding.htmlDecode(title).trim();
            title = title.replaceFirst("(?i)\\s*Porn Pic - EPORNER\\s*", "");
            title = title.replaceFirst("\\s*\\- EPORNER Free HD Porn Tube\\s*", "");
            title = title.replaceFirst("\\s*- EPORNER\\s*", "");
            link.setFinalFileName(title + ext);
        } else {
            link.setFinalFileName(fallbackFilename);
        }
        if (dllink != null && !isBadDirecturl(dllink)) {
            link.setProperty(PROPERTY_DIRECTURL, dllink);
        }
        /*
         * 2020-05-26: Checking their downloadlink counts towards their daily downloadlimit so only check them if the filesize has not been
         * found already!
         */
        if (filesize > 0) {
            link.setDownloadSize(filesize);
        } else if (link.getView().getBytesTotal() <= 0 && dllink != null && !isBadDirecturl(dllink)) {
            /* Only get filesize from url if we were not able to find it in html --> Saves us time! */
            final Browser br2 = br.cloneBrowser();
            // In case the link redirects to the finallink
            URLConnectionAdapter con = null;
            try {
                con = br2.openHeadConnection(dllink);
                connectionErrorhandling(con, link, account);
                if (con.getCompleteContentLength() > 0) {
                    if (con.isContentDecoded()) {
                        link.setDownloadSize(con.getCompleteContentLength());
                    } else {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        handleDownload(link, null);
    }

    public void handleDownload(final DownloadLink link, final Account account) throws Exception {
        final String storedDirecturl = link.getStringProperty(PROPERTY_DIRECTURL);
        String directurl = null;
        if (storedDirecturl != null) {
            directurl = storedDirecturl;
        } else {
            requestFileInformation(link, account, true);
            directurl = link.getStringProperty(PROPERTY_DIRECTURL);
            if (directurl == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else if (isBadDirecturl(directurl)) {
                errorBrokenVideo();
            }
        }
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, directurl, true, 0);
            connectionErrorhandling(dl.getConnection(), link, account);
        } catch (final Exception e) {
            if (storedDirecturl != null) {
                link.removeProperty(PROPERTY_DIRECTURL);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Stored directurl expired", e);
            } else {
                throw e;
            }
        }
        final String ext = Plugin.getExtensionFromMimeTypeStatic(dl.getConnection().getContentType());
        if (ext != null && link.getName() != null) {
            link.setFinalFileName(this.correctOrApplyFileNameExtension(link.getName(), "." + ext));
        }
        dl.startDownload();
    }

    private void connectionErrorhandling(final URLConnectionAdapter con, final DownloadLink link, final Account account) throws PluginException, IOException {
        /* E.g. https://static.eporner.com/na.mp4 */
        if (isBadDirecturl(con.getURL().toString())) {
            errorBrokenVideo();
        }
        /* Double-check for broken/bad/dummy-video */
        final String etag = con.getRequest().getResponseHeader("etag");
        if (StringUtils.equalsIgnoreCase(etag, "\"52ee70df-9cdc1\"")) {
            errorBrokenVideo();
        }
        if (!this.looksLikeDownloadableContent(con)) {
            br.followConnection(true);
            /* 2020-05-26: Limit = 100 videos per day for unregistered users, no limit for registered users */
            if (br.containsHTML("(?i)>\\s*You have downloaded more than|>\\s*Please try again tomorrow or register for free to unlock unlimited")) {
                if (account != null) {
                    /* 2020-05-26: This should never happen in account mode */
                    throw new AccountUnavailableException("Daily download limit reached or session error", 10 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Daily download limit reached", 60 * 60 * 1000l);
                }
            } else {
                errorBrokenVideo();
            }
        }
    }

    private boolean isBadDirecturl(final String url) {
        if (url != null && url.matches("(?i)https?://[^/]+/na\\.(flv|mp4).*")) {
            return true;
        } else {
            return false;
        }
    }

    private void errorBrokenVideo() throws PluginException {
        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Broken video?", 5 * 60 * 1000l);
    }

    private String getPreferredStreamQuality() {
        final EpornerComConfig cfg = PluginJsonConfig.get(this.getConfigInterface());
        final PreferredStreamQuality quality = cfg.getPreferredStreamQuality();
        switch (quality) {
        case Q2160P:
            return "2160p";
        case Q1440P:
            return "1440p";
        case Q1080P:
            return "1080p";
        case Q720P:
            return "720p";
        case Q480P:
            return "480p";
        case Q360P:
            return "360p";
        case Q240P:
            return "240p";
        case BEST:
        default:
            return null;
        }
    }

    private boolean login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    logger.info("Attempting cookie login");
                    this.br.setCookies(this.getHost(), cookies);
                    if (!force) {
                        /* Do not validate cookies. */
                        return false;
                    }
                    br.getPage("https://" + this.getHost() + "/");
                    if (this.isLoggedin()) {
                        logger.info("Cookie login successful");
                        /* Refresh cookie timestamp */
                        account.saveCookies(br.getCookies(br.getHost()), "");
                        return true;
                    } else {
                        logger.info("Cookie login failed");
                    }
                }
                logger.info("Performing full login");
                final Form loginform = new Form();
                loginform.setMethod(MethodType.POST);
                loginform.setAction("https://www." + this.getHost() + "/xhr/login/");
                loginform.put("xhr", "1");
                loginform.put("act", "login");
                loginform.put("login", Encoding.urlEncode(account.getUser()));
                loginform.put("haslo", Encoding.urlEncode(account.getPass()));
                loginform.put("ref", "/");
                br.submitForm(loginform);
                /* 2020-05-26: E.g. login failed: {"status":0,"msg_head":"Login failed.","msg_body":"Bad login\/password"} */
                br.getPage("/");
                if (!isLoggedin()) {
                    throw new AccountInvalidException();
                }
                account.saveCookies(this.br.getCookies(br.getHost()), "");
                return true;
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private boolean isLoggedin() {
        return br.containsHTML("/logout");
    }

    /** Free accounts do not have any downloadlimits. Anonymous users can download max. 100 files per day. */
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        account.setConcurrentUsePossible(false);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.handleDownload(link, account);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public Class<? extends EpornerComConfig> getConfigInterface() {
        return EpornerComConfig.class;
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
        /* 2020-05-26: No captchas at all */
        return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}
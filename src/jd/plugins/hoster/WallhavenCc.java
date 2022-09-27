//jDownloader - Downloadmanager
//Copyright (C) 2017  JD-Team support@jdownloader.org
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

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class WallhavenCc extends PluginForHost {
    public WallhavenCc(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://wallhaven.cc/join");
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_HOST };
    }

    @Override
    public void init() {
        for (final String[] domainList : getPluginDomains()) {
            for (final String domain : domainList) {
                Browser.setRequestIntervalLimitGlobal(domain, true, 300);
            }
        }
        // download domain
        Browser.setRequestIntervalLimitGlobal("w.wallhaven.cc", true, 500);
    }

    /* Connection stuff */
    private static final boolean free_resume              = false;
    private static final int     free_maxchunks           = 1;
    private static final int     free_maxdownloads        = 10;
    private String               dllink                   = null;
    private final String         TAGS_COMMA_SEPARATED     = "tags_comma_separated";
    private final String         PROPERTY_DIRECTURL       = "directurl";
    private final String         PATTERN_NORMAL           = "https?://[^/]+/w/([a-z0-9]+)";
    private final String         PATTERN_SHORT            = "https?://whvn\\.cc/([a-z0-9]+)$";
    private final String         PATTERN_DIRECT_FULL      = "https://w\\.[^/]+/full/([^/]+)/wallhaven-([a-z0-9]+)\\.jpg";
    private final String         PATTERN_DIRECT_THUMBNAIL = "https?://th\\.[^/]+/small/([^/]+)/([a-z0-9]+)\\.jpg";

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "wallhaven.cc", "whvn.cc" });
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
            final String hostsPatternPart = buildHostsPatternPart(domains);
            ret.add("https?://(?:www\\.)?" + hostsPatternPart + "/w/[a-z0-9]+|https?://whvn\\.cc/[a-z0-9]+$|https?://w\\." + hostsPatternPart + "/full/[^/]+/wallhaven-[a-z0-9]+\\.jpg|https?://th\\." + hostsPatternPart + "/small/[^/]+/[a-z0-9]+\\.jpg");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "https://wallhaven.cc/terms";
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
        if (link.getPluginPatternMatcher() == null) {
            return null;
        } else if (link.getPluginPatternMatcher().matches(PATTERN_NORMAL)) {
            return new Regex(link.getPluginPatternMatcher(), PATTERN_NORMAL).getMatch(0);
        } else if (link.getPluginPatternMatcher().matches(PATTERN_DIRECT_FULL)) {
            return new Regex(link.getPluginPatternMatcher(), PATTERN_DIRECT_FULL).getMatch(1);
        } else if (link.getPluginPatternMatcher().matches(PATTERN_DIRECT_THUMBNAIL)) {
            return new Regex(link.getPluginPatternMatcher(), PATTERN_DIRECT_THUMBNAIL).getMatch(1);
        } else if (link.getPluginPatternMatcher().matches(PATTERN_SHORT)) {
            return new Regex(link.getPluginPatternMatcher(), PATTERN_SHORT).getMatch(0);
        } else {
            /* Unsupported URL --> This should never happen! */
            return null;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return requestFileInformation(link, account, false);
    }

    private String getDirecturlFromContentURL(final DownloadLink link) {
        final Regex thumbnailRegex = new Regex(link.getPluginPatternMatcher(), PATTERN_DIRECT_THUMBNAIL);
        if (link.getPluginPatternMatcher().matches(PATTERN_DIRECT_FULL)) {
            return link.getPluginPatternMatcher();
        } else if (thumbnailRegex.matches()) {
            /* It is possible to alter thumbnail URLs to fullsize URLs. */
            return "https://w." + this.getHost() + "/full/" + thumbnailRegex.getMatch(0) + "/wallhaven-" + thumbnailRegex.getMatch(1) + ".jpg";
        } else {
            return null;
        }
    }

    private String getNormalContentURL(final DownloadLink link) {
        if (link.getPluginPatternMatcher().matches(PATTERN_NORMAL)) {
            return link.getPluginPatternMatcher();
        } else {
            return "https://" + this.getHost() + "/w/" + this.getFID(link);
        }
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        final String fid = this.getFID(link);
        if (!link.isNameSet()) {
            /* Set fallback filename */
            link.setName(fid + ".jpg");
        }
        if (account != null) {
            this.login(account, false);
        }
        br.setAllowedResponseCodes(429);
        this.dllink = getDirecturlFromContentURL(link);
        if (this.dllink != null) {
            if (checkDownloadableRequest(link, br, br.createHeadRequest(dllink), 0, true) != null) {
                logger.info("Successfully checked availablestatus via directurl from contentURL");
                link.setProperty(PROPERTY_DIRECTURL, this.dllink);
                return AvailableStatus.TRUE;
            } else {
                errorHandling(br, br.getHttpConnection());
                logger.info("Failed to check directurl via directurl from contentURL");
            }
        }
        this.dllink = checkDirecturlFromPropertyAndSetFilesize(link, PROPERTY_DIRECTURL);
        if (this.dllink != null) {
            logger.info("Successfully checked availablestatus via directurl");
            return AvailableStatus.TRUE;
        }
        br.setFollowRedirects(true);
        br.getPage(getNormalContentURL(link));
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("<title>\\s*404\\s*-\\s*Not Found Sorry!\\s*-\\s*wallhaven.cc\\s*<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else {
            errorHandling(br, br.getHttpConnection());
        }
        String tagsCommaSeparated = br.getRegex("name=\"title\" content=\"([^\"]+) \\| \\d+x\\d+ Wallpaper\"").getMatch(0);
        if (tagsCommaSeparated != null) {
            tagsCommaSeparated = Encoding.htmlDecode(tagsCommaSeparated).trim().replace(" ", "");
            link.setProperty(TAGS_COMMA_SEPARATED, tagsCommaSeparated);
        }
        dllink = br.getRegex("(https?://[^/]+/full/[^\"]+)").getMatch(0);
        if (!StringUtils.isEmpty(this.dllink)) {
            link.setProperty(PROPERTY_DIRECTURL, this.dllink);
            final boolean useTagsCommaSeparatedInFiletitle = false;
            final String ext = Plugin.getFileNameExtensionFromURL(this.dllink);
            if (ext != null) {
                if (tagsCommaSeparated != null && useTagsCommaSeparatedInFiletitle) {
                    link.setFinalFileName(tagsCommaSeparated + ext);
                } else {
                    /* Fallback */
                    link.setFinalFileName(fid + ext);
                }
            }
        }
        final boolean allowCheckFilesizeViaDirecturl = false;
        final String filesizeStr = br.getRegex("(?i)<dt>\\s*Size\\s*</dt><dd>(\\d+[^<]+)</dd>").getMatch(0);
        if (filesizeStr != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesizeStr));
        } else if (!StringUtils.isEmpty(dllink) && !isDownload && allowCheckFilesizeViaDirecturl) {
            if (checkDownloadableRequest(link, br, br.createHeadRequest(dllink), 0, true) == null) {
                errorHandling(br, br.getHttpConnection());
            }
        }
        return AvailableStatus.TRUE;
    }

    private String checkDirecturlFromPropertyAndSetFilesize(final DownloadLink link, final String propertyName) throws IOException, PluginException {
        final String url = link.getStringProperty(propertyName);
        if (StringUtils.isEmpty(url)) {
            return null;
        } else {
            if (checkDownloadableRequest(link, br, br.createHeadRequest(url), 0, true) != null) {
                return url;
            } else {
                errorHandling(br, br.getHttpConnection());
                link.removeProperty(propertyName);
                return null;
            }
        }
    }

    private void errorHandling(final Browser br, final URLConnectionAdapter con) throws PluginException {
        if (con != null) {
            if (con.getResponseCode() == 429) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Rate limited", 30 * 1000l);
            }
        }
    }

    /** Checks for some http responsecodes and throws an exception if given URLConnectionAdapter does not lead to a downloadable file. */
    private void errorHandling(final Browser br, final DownloadInterface dl) throws PluginException {
        final URLConnectionAdapter con = dl != null ? dl.getConnection() : null;
        if (con != null) {
            if (!this.looksLikeDownloadableContent(con)) {
                try {
                    br.followConnection(true);
                } catch (IOException e) {
                    logger.log(e);
                }
                errorHandling(br, con);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Image broken?");
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        handleDownload(link, null);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        handleDownload(link, account);
    }

    public void handleDownload(final DownloadLink link, final Account account) throws Exception {
        if (!attemptStoredDownloadurlDownload(link)) {
            requestFileInformation(link, account, true);
            if (br.getHttpConnection().getResponseCode() == 403) {
                /* Account required to view adult content. */
                if (account == null) {
                    throw new AccountRequiredException();
                } else {
                    logger.warning("Account available and still no permissions to view (NSFW) content -> This should never happen!");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            } else if (StringUtils.isEmpty(dllink)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, free_resume, free_maxchunks);
            errorHandling(br, dl);
        }
        dl.startDownload();
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link) throws Exception {
        final String url = link.getStringProperty(PROPERTY_DIRECTURL);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        boolean valid = false;
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, free_resume, free_maxchunks);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                valid = true;
                return true;
            } else {
                link.removeProperty(PROPERTY_DIRECTURL);
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final Throwable e) {
            logger.log(e);
            return false;
        } finally {
            if (!valid) {
                try {
                    dl.getConnection().disconnect();
                } catch (Throwable ignore) {
                }
                this.dl = null;
            }
        }
    }

    private boolean login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    logger.info("Attempting cookie login");
                    this.br.setCookies(this.getHost(), cookies);
                    if (!force) {
                        /* Don't validate cookies */
                        return false;
                    }
                    br.getPage("https://" + this.getHost() + "/");
                    if (this.isLoggedin(br)) {
                        logger.info("Cookie login successful");
                        /* Refresh cookie timestamp */
                        account.saveCookies(this.br.getCookies(this.getHost()), "");
                        return true;
                    } else {
                        logger.info("Cookie login failed");
                    }
                }
                logger.info("Performing full login");
                br.getPage("https://" + this.getHost() + "/login");
                final Form loginform = br.getFormbyActionRegex(".*auth/login.*");
                if (loginform == null) {
                    logger.warning("Failed to find loginform");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.put("username", Encoding.urlEncode(account.getUser()));
                loginform.put("password", Encoding.urlEncode(account.getPass()));
                br.submitForm(loginform);
                if (!isLoggedin(br)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
                return true;
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private boolean isLoggedin(final Browser br) {
        return br.containsHTML("auth/logout");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        return ai;
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final Account acc) {
        return false;
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
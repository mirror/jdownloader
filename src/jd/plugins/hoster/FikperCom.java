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
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.Time;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class FikperCom extends PluginForHost {
    public FikperCom(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium("https://fikper.com/register");
    }

    @Override
    public String getAGBLink() {
        return "https://fikper.com/terms-of-use";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "fikper.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/([A-Za-z0-9]+)/([^/]+)\\.html");
        }
        return ret.toArray(new String[0]);
    }

    /* Connection stuff */
    private final int FREE_MAXDOWNLOADS            = 20;
    private final int ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return false;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return true;
        } else {
            /* Free(anonymous) and unknown account type */
            return false;
        }
    }

    public int getMaxChunks(final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return 1;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return 0;
        } else {
            /* Free(anonymous) and unknown account type */
            return 1;
        }
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
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    private String getFiletitleFromURL(final DownloadLink link) {
        String title = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(1);
        final Regex looksLikeTitleWithExtension = new Regex(title, "(.+)([A-Za-z0-9]{3})$");
        if (looksLikeTitleWithExtension.matches()) {
            /* This will work for all file extensions with length == 3. */
            return looksLikeTitleWithExtension.getMatch(0) + "." + looksLikeTitleWithExtension.getMatch(1);
        } else {
            return title;
        }
    }

    private final String        WEBAPI_BASE = "https://sapi.fikper.com/";
    private Map<String, Object> entries     = null;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (!link.isNameSet()) {
            link.setName(getFiletitleFromURL(link));
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.postPageRaw(WEBAPI_BASE, "{\"fileHashName\":\"" + this.getFID(link) + "\"}");
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            /* {"code":404,"message":"The file might be deleted."} */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        link.setFinalFileName(entries.get("name").toString());
        final Object filesize = entries.get("size");
        if (filesize instanceof Number) {
            link.setVerifiedFileSize(((Number) filesize).longValue());
        } else {
            link.setVerifiedFileSize(Long.parseLong(filesize.toString()));
        }
        link.setPasswordProtected(((Boolean) entries.get("password")).booleanValue());
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, null);
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception, PluginException {
        if (attemptStoredDownloadurlDownload(link, account)) {
            logger.info("Re-using previously generated directurl");
        } else {
            if (account != null) {
                this.login(account, false);
            }
            requestFileInformation(link);
            if (link.isPasswordProtected()) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Password protected items are not yet supported");
            }
            final Object remainingDelay = entries.get("remainingDelay");
            if (remainingDelay != null) {
                /* Downloadlimit has been reached */
                final int limitWaitSeconds;
                if (remainingDelay instanceof Number) {
                    limitWaitSeconds = ((Number) remainingDelay).intValue();
                } else {
                    limitWaitSeconds = Integer.parseInt(remainingDelay.toString());
                }
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, limitWaitSeconds * 1000l);
            }
            final long timeBefore = Time.systemIndependentCurrentJVMTimeMillis();
            final int waitBeforeDownloadMillis = ((Number) entries.get("delayTime")).intValue();
            final boolean skipPreDownloadWaittime = true; // 2022-11-14: Waittime is skippable
            final String recaptchaV2Response = getRecaptchaHelper(br).getToken();
            final long passedTimeDuringCaptcha = Time.systemIndependentCurrentJVMTimeMillis() - timeBefore;
            final Map<String, Object> postdata = new HashMap<String, Object>();
            postdata.put("fileHashName", this.getFID(link));
            postdata.put("downloadToken", entries.get("downloadToken").toString());
            postdata.put("recaptcha", recaptchaV2Response);
            final long waitBeforeDownloadMillisLeft = waitBeforeDownloadMillis - passedTimeDuringCaptcha;
            if (waitBeforeDownloadMillisLeft > 0 && !skipPreDownloadWaittime) {
                this.sleep(waitBeforeDownloadMillisLeft, link);
            }
            br.postPageRaw(WEBAPI_BASE, JSonStorage.serializeToJson(postdata));
            final Map<String, Object> dlresponse = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final String dllink = dlresponse.get("directLink").toString();
            if (StringUtils.isEmpty(dllink)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, this.isResumeable(link, account), getMaxChunks(account));
            /* Save directurl even if it is "invalid" -> E.g. if error 429 happens we might be able to use the same URL later. */
            link.setProperty(getDirectlinkproperty(account), dl.getConnection().getURL().toString());
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                }
                checkError429(dl.getConnection());
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

    private void checkError429(final URLConnectionAdapter con) throws PluginException {
        if (con.getResponseCode() == 429) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Error 429 too many connections", 30 * 1000l);
        }
    }

    private CaptchaHelperHostPluginRecaptchaV2 getRecaptchaHelper(final Browser br) {
        final String host = br.getHost(false);
        return new CaptchaHelperHostPluginRecaptchaV2(this, br, "6Ley0XQeAAAAAK-H0p0T_zeun7NnUgMcLFQy0cU3") {
            @Override
            protected String getSiteUrl() {
                /* We are on subdomain.fikper.com but captcha needs to be solved on fikper.com. */
                return br._getURL().getProtocol() + "://" + host;
            }
        };
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link, final Account account) throws Exception {
        final String property = getDirectlinkproperty(account);
        final String url = link.getStringProperty(property);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        boolean valid = false;
        boolean throwException = false;
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, this.isResumeable(link, account), this.getMaxChunks(account));
            try {
                checkError429(dl.getConnection());
            } catch (final PluginException error429) {
                /* URL is valid but we can't use it right now. */
                valid = true;
                throwException = true;
                throw error429;
            }
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                valid = true;
                return true;
            } else {
                link.removeProperty(property);
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final Exception e) {
            if (throwException) {
                throw e;
            } else {
                logger.log(e);
                return false;
            }
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

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
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
                    if (this.isLoggedin()) {
                        logger.info("Cookie login successful");
                        /* Refresh cookie timestamp */
                        account.saveCookies(this.br.getCookies(this.getHost()), "");
                        return true;
                    } else {
                        logger.info("Cookie login failed");
                    }
                }
                logger.info("Performing full login");
                br.getPage("https://" + this.getHost() + "/login.php");
                final Form loginform = br.getFormbyProperty("name", "login");
                if (loginform == null) {
                    logger.warning("Failed to find loginform");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.put("username", Encoding.urlEncode(account.getUser()));
                loginform.put("password", Encoding.urlEncode(account.getPass()));
                br.postPage("", "email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                if (!isLoggedin()) {
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

    private boolean isLoggedin() {
        return br.containsHTML("/logout");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        String space = br.getRegex("").getMatch(0);
        if (space != null) {
            ai.setUsedSpace(space.trim());
        }
        ai.setUnlimitedTraffic();
        if (br.containsHTML("")) {
            account.setType(AccountType.FREE);
            /* free accounts can still have captcha */
            account.setMaxSimultanDownloads(FREE_MAXDOWNLOADS);
            account.setConcurrentUsePossible(false);
        } else {
            final String expire = br.getRegex("").getMatch(0);
            if (expire == null) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd MMMM yyyy", Locale.ENGLISH));
            }
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setConcurrentUsePossible(true);
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        handleDownload(link, account);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_PREMIUM_MAXDOWNLOADS;
    }

    public String getDirectlinkproperty(final Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return "free_directurl";
        } else {
            return "account_ " + acc.getType() + "_directurl";
        }
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        } else if (acc.getType() == AccountType.FREE) {
            /* Free accounts can have captchas */
            return true;
        } else {
            /* Premium accounts do not have captchas */
            return false;
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
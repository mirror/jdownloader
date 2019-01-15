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

import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "rocketshare.com" }, urls = { "https?://(?:www\\.)?rocketshare\\.com/file/[a-z0-9]{32}(?:/[^/]+)?" })
public class RocketshareCom extends PluginForHost {
    public RocketshareCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://rocketshare.com/premium");
    }

    @Override
    public String getAGBLink() {
        return "https://rocketshare.com/terms";
    }

    /* Connection stuff */
    private final boolean       FREE_RESUME                  = true;
    private final int           FREE_MAXCHUNKS               = 0;
    private final int           FREE_MAXDOWNLOADS            = 20;
    private final boolean       ACCOUNT_FREE_RESUME          = true;
    private final int           ACCOUNT_FREE_MAXCHUNKS       = 0;
    private final int           ACCOUNT_FREE_MAXDOWNLOADS    = 20;
    private final boolean       ACCOUNT_PREMIUM_RESUME       = true;
    private final int           ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private final int           ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    private static final String API_BASE                     = "https://rocketshare.com/api/jd2";

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = new Regex(link.getPluginPatternMatcher(), "/([a-z0-9]{32})").getMatch(0);
        if (linkid != null) {
            return linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private Browser prepBR_API(final Browser br) {
        br.setHeader("User-Agent", "JDownloader");
        br.setHeader("Content-Type", "application/json");
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) {
            return false;
        }
        try {
            final Browser br = new Browser();
            prepBR_API(br);
            br.setCookiesExclusive(true);
            final StringBuilder sb = new StringBuilder();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    /* we test 50 links at once */
                    if (index == urls.length || links.size() > 50) {
                        break;
                    }
                    links.add(urls[index]);
                    index++;
                }
                sb.delete(0, sb.capacity());
                sb.append(API_BASE + "/download/check/");
                for (final DownloadLink dl : links) {
                    sb.append(getLinkID(dl));
                    sb.append(",");
                }
                br.getPage(sb.toString());
                LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
                final ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("files");
                for (final DownloadLink dlink : links) {
                    boolean foundObject = false;
                    final String fid = getLinkID(dlink);
                    for (final Object fileo : ressourcelist) {
                        entries = (LinkedHashMap<String, Object>) fileo;
                        final String fid_tmp = (String) entries.get("uuid");
                        if (fid_tmp.equalsIgnoreCase(fid)) {
                            foundObject = true;
                            break;
                        }
                    }
                    if (!foundObject) {
                        /* This should never happen - so let's set such URLs offline. */
                        dlink.setAvailable(false);
                    } else {
                        final boolean available = ((Boolean) entries.get("available")).booleanValue();
                        final String filename = (String) entries.get("filename");
                        final long filesize = JavaScriptEngineFactory.toLong(entries.get("size"), 0);
                        if (filesize > 0) {
                            dlink.setDownloadSize(filesize);
                        }
                        if (filename != null) {
                            dlink.setFinalFileName(filename);
                        }
                        if (available) {
                            dlink.setAvailable(true);
                        } else {
                            dlink.setAvailable(false);
                        }
                    }
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        handleDownload(downloadLink, null, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void handleDownload(final DownloadLink downloadLink, final Account account, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        String dllink = checkDirectLink(downloadLink, directlinkproperty);
        dllink = null;
        if (dllink == null) {
            final String mode;
            String token = null;
            final boolean captchaRequired;
            if (account == null) {
                mode = "GUEST";
                captchaRequired = true;
            } else if (account.getType() == AccountType.FREE) {
                mode = "FREE";
                captchaRequired = true;
            } else {
                mode = "PREMIUM";
                captchaRequired = false;
            }
            if (captchaRequired) {
                /* GUEST an FREE(-account) mode */
                /* 2019-01-15: TODO: GUEST- and FREE mode are not yet working properly */
                final boolean useHardcodedReCaptchaSiteKey = false;
                final String reCaptchaSiteKey;
                if (useHardcodedReCaptchaSiteKey) {
                    /* 2019-01-14 */
                    reCaptchaSiteKey = "6LeXdH8UAAAAALDYC_CQg3iQxWeO5KiVvon-ZxFC";
                } else {
                    br.getPage("https://" + this.getHost() + "/static/assets/js/download.js?v=9&s=0");
                    reCaptchaSiteKey = br.getRegex("sitekey:\\'([^\\'\"]+)\\'").getMatch(0);
                    if (StringUtils.isEmpty(reCaptchaSiteKey)) {
                        logger.warning("Failed to find reCaptchaSiteKey");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
                token = "\"" + new CaptchaHelperHostPluginRecaptchaV2(this, br, reCaptchaSiteKey).getToken() + "\"";
            }
            PostRequest downloadReq = br.createJSonPostRequest(API_BASE + "/download/initialize/" + getLinkID(downloadLink), "{\"mode\": \"" + mode + "\",\"token\": " + token + "}");
            br.openRequestConnection(downloadReq);
            br.loadConnection(null);
            /* Premium users may get 1 second of waittime when they try to download an URL for the first time! */
            final String available_in_str = PluginJSonUtils.getJson(br, "available_in");
            if (available_in_str != null) {
                /* Guest/Free mode --> Waittime required --> Then do the same request again */
                final long available_in_long = Long.parseLong(available_in_str);
                if (available_in_long > 180) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, available_in_long * 1001l);
                }
                this.sleep(available_in_long * 1001l, downloadLink);
                br.openRequestConnection(downloadReq);
                br.loadConnection(null);
                /* TODO: This does not yet work for GUEST and FREE mode */
            }
            dllink = PluginJSonUtils.getJson(br, "url");
            if (StringUtils.isEmpty(dllink)) {
                handleErrors();
                logger.warning("dllink is null");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            handleErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        dl.startDownload();
    }

    private void handleErrors() throws PluginException {
        final String error_string = PluginJSonUtils.getJson(br, "error_code");
        if (error_string != null) {
            if (error_string.equalsIgnoreCase("ERROR_CAPTCHA_INVALID")) {
                /* This is a rare case */
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            } else if (error_string.equalsIgnoreCase("ERROR_FILE_UNAVAILABLE")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            } else if (error_string.equalsIgnoreCase("ERROR_TEMPORARY_SERVER_ISSUE")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Temporary server issue");
            } else if (error_string.equalsIgnoreCase("ERROR_PREMIUM_REQUIRED")) {
                throw new AccountRequiredException();
            } else if (error_string.equalsIgnoreCase("ERROR_INSUFFICIENT_DAILY_TRANSFER")) {
                logger.info("Not enough traffic left");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            } else if (error_string.equalsIgnoreCase("ERROR_FREE_NO_SIMULTANEOUS_DOWNLOADS")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "FREE account: No simultaneous downloads possible");
            } else if (error_string.equalsIgnoreCase("ERROR_GUEST_NO_SIMULTANEOUS_DOWNLOADS")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "GUEST: No simultaneous downloads possible");
            } else {
                /* E.g. "ERROR_INVALID_MODE", "ERROR_LOGGED_IN_GUEST", "ERROR_NOT_LOGGED_IN_FREE" or any other unhandled error-string */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            // if (error_string.equalsIgnoreCase("ERROR_WAIT_REQUIRED")) {
            // /* TODO: Waittime is handled before this gets executed but we should maybe still check for it here just in case */
            // }
        }
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openHeadConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return dllink;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    private static Object LOCK = new Object();

    /** Sets login-header without checking validity! */
    private void setLoginHeader(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            prepBR_API(br);
            br.getHeaders().put("Authorization", "Basic " + Encoding.Base64Encode(account.getUser() + ":" + account.getPass()));
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            setLoginHeader(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        br.getPage(API_BASE + "/user");
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final boolean isPremium = ((Boolean) entries.get("premium")).booleanValue();
        if (!isPremium) {
            account.setType(AccountType.FREE);
            /* free accounts can still have captcha */
            account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
            account.setConcurrentUsePossible(false);
            ai.setStatus("Registered (free) user");
        } else {
            final long daily_traffic_left = JavaScriptEngineFactory.toLong(entries.get("daily_traffic_left"), 0);
            entries = (LinkedHashMap<String, Object>) entries.get("valid_until");
            final long valid_until_timestamp = JavaScriptEngineFactory.toLong(entries.get("timestamp"), 0);
            ai.setValidUntil(valid_until_timestamp * 1000);
            ai.setTrafficLeft(daily_traffic_left);
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setConcurrentUsePossible(true);
            ai.setStatus("Premium account");
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        setLoginHeader(account, false);
        if (account.getType() == AccountType.FREE) {
            handleDownload(link, account, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
        } else {
            handleDownload(link, account, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS, "premium_directlink");
        }
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (acc.getType() == AccountType.FREE) {
            /* Free accounts have captchas */
            return true;
        }
        /* Premium accounts do not have captchas */
        return false;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_FREE_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        return null;
    }
}
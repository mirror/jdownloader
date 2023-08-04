//jDownloader - Downloadmanager
//Copyright (C) 2015  JD-Team support@jdownloader.org
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

import java.util.Locale;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.config.FlimmitComConfig;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
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

/**
 *
 * @author raztoki
 *
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "flimmit.com" }, urls = { "https?://flimmit\\.com/\\d+" })
public class FlimmitCom extends PluginForHost {
    public FlimmitCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.flimmit.com/customer/account/create/");
    }

    @Override
    public String getAGBLink() {
        return "http://www.flimmit.com/agb/";
    }

    public static String getInternalBaseURL() {
        return "https://flimmit.at/";
    }

    @Override
    public String[] siteSupportedNames() {
        return new String[] { "flimmit.at", "flimmit.com" };
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        // final Account aa = AccountController.getInstance().getValidAccount(this);
        // if (aa == null) {
        // link.getLinkStatus().setStatusText("Only downloadable for registered/premium users");
        // return AvailableStatus.UNCHECKABLE;
        // }
        // this.login(aa, false);
        // br.setFollowRedirects(true);
        // br.getPage(link.getDownloadURL());
        // if (!br.getURL().contains("/play/")) {
        // throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // }
        String filename = link.getStringProperty("filename");
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setFinalFileName(filename);
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        throw new AccountRequiredException();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private boolean isValidUserSubscriptionsResponse(Browser br) throws Exception {
        if (br.getHttpConnection().getResponseCode() == 200 && br.getCookie(getInternalBaseURL(), "PHPSESSID", Cookies.NOTDELETEDPATTERN) != null) {
            final Map<String, Object> response = restoreFromString(br.toString(), TypeRef.MAP);
            if ("success".equals(response.get("status")) && response.containsKey("data")) {
                return true;
            }
        }
        return false;
    }

    public void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                br.setFollowRedirects(true);
                br.setAllowedResponseCodes(400);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    logger.info("Attempting cookie login");
                    br.setCookies(cookies);
                    if (!force) {
                        logger.info("Trust cookies without login");
                        return;
                    } else {
                        br.getPage(getInternalBaseURL() + "dynamically/me/user-subscriptions/active");
                        if (isValidUserSubscriptionsResponse(br)) {
                            logger.info("Cookie login successful");
                            account.saveCookies(br.getCookies(br.getHost()), "");
                            return;
                        } else {
                            logger.info("Cookie login failed");
                            br.clearCookies(null);
                        }
                    }
                }
                logger.info("Performing full login");
                final String user = account.getUser();
                if (StringUtils.isEmpty(user) || !user.matches(".+@.+\\..+")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBitte gib deine E-Mail Adresse ins Benutzername Feld ein!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlease enter your e-mail address in the username field!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                br.getPage(getInternalBaseURL() + "de/login");
                final String responseString = br.postPageRaw("/de/dynamically/user/login", String.format("{\"email\":\"%s\",\"password\":\"%s\",\"_csrf_token\":null}", user, account.getPass()));
                final Map<String, Object> response = restoreFromString(responseString, TypeRef.MAP);
                if ("failure".equals(response.get("status"))) {
                    /* E.g. bad response: {"status":"failure","message":"Email oder Passwort ist nicht korrekt","extraData":[]} */
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, StringUtils.valueOfOrNull(response.get("message")), PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                br.getPage("/dynamically/me/user-subscriptions/active");
                if (!isValidUserSubscriptionsResponse(br)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(br.getCookies(br.getHost()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        synchronized (account) {
            try {
                login(account, true);
                /* All accounts are "premium" - users have to buy the movies to get the links they can add to JD. */
                if (br.getRequest() == null || !StringUtils.endsWithCaseInsensitive(br.getURL(), "user-subscriptions/active")) {
                    br.getPage(getInternalBaseURL() + "dynamically/me/user-subscriptions/active");
                    if (!isValidUserSubscriptionsResponse(br)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
        final Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
        final Map<String, Object> data = (Map<String, Object>) entries.get("data");
        // boolean autorenewal_active = false;
        if (data != null) {
            final String valid_until = (String) data.get("valid_until");
            ai.setValidUntil(0);
            if (ai.isExpired() && !StringUtils.isEmpty(valid_until)) {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(valid_until, "yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ENGLISH));
            }
            final String next_payment_date = (String) data.get("next_payment_date");
            // final Object autorenewal_activeO = entries.get("autorenewal_active");
            // if (Boolean.TRUE.equals(autorenewal_activeO)) {
            // autorenewal_active = true;
            // }
            if (ai.isExpired() && !StringUtils.isEmpty(next_payment_date)) {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(next_payment_date, "yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ENGLISH));
            }
            if (!ai.isExpired()) {
                account.setType(AccountType.PREMIUM);
            }
        } else {
            account.setType(AccountType.FREE);
            /* Free accounts cannot download/stream anything. */
            ai.setTrafficLeft(0);
            ai.setExpired(true);
        }
        account.setConcurrentUsePossible(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        // hls stuff
        final String dllink = link.getStringProperty("m3u8url");
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // dllink = dllink.replace("https://flimmitcdn-a.akamaihd.net/", "https://edgecast-stream.flimmit.com/");
        checkFFmpeg(link, "Download a HLS Stream");
        dl = new HLSDownloader(link, br, dllink);
        dl.startDownload();
    }

    @Override
    public Class<? extends FlimmitComConfig> getConfigInterface() {
        return FlimmitComConfig.class;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
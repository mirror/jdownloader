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

import org.appwork.storage.TypeRef;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class ManyvidsCom extends PluginForHost {
    public ManyvidsCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.manyvids.com/Create-Free-Account/");
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public String getAGBLink() {
        return "https://info.manyvids.com/home";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "manyvids.com" });
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
            ret.add("");
        }
        return ret.toArray(new String[0]);
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

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        // 2024-01-26: Not used
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        handleDownload(link, null);
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception, PluginException {
        // 2024-01-26: Not used
        throw new AccountRequiredException();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }

    public boolean login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            br.setCookiesExclusive(true);
            final Cookies cookies = account.loadCookies("");
            if (cookies != null) {
                logger.info("Attempting cookie login");
                br.setCookies(this.getHost(), cookies);
                if (!force) {
                    /* Don't validate cookies */
                    return false;
                }
                if (checkLogin(br)) {
                    logger.info("Cookie login successful");
                    /* Refresh cookie timestamp */
                    account.saveCookies(br.getCookies(br.getHost()), "");
                    return true;
                } else {
                    logger.info("Cookie login failed");
                    br.clearCookies(null);
                    account.clearCookies("");
                }
            }
            logger.info("Performing full login");
            br.getPage("https://www." + this.getHost() + "/Login/");
            final String rcKey = br.getRegex("enterprise\\.js\\?render=([^\"]+)").getMatch(0);
            if (rcKey == null) {
                logger.warning("Failed to find reCaptcha sitekey");
            }
            final String mvtoken = br.getRegex("data-mvtoken=\"([^\"]+)").getMatch(0); // 2024-04-18: Not needed anymore
            Form loginform = br.getFormbyProperty("id", "loginAccountSubmitForm");
            if (loginform == null) {
                for (final Form form : br.getForms()) {
                    if (form.hasInputFieldByName("password")) {
                        loginform = form;
                        break;
                    }
                }
            }
            if (loginform == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            loginform.setMethod(MethodType.POST);
            loginform.setAction("/includes/login-access-user.php");
            if (mvtoken != null) {
                loginform.put("mvtoken", Encoding.urlEncode(mvtoken));
            }
            loginform.remove("username");
            loginform.put("username", Encoding.urlEncode(account.getUser()));
            loginform.put("password", Encoding.urlEncode(account.getPass()));
            final CaptchaHelperHostPluginRecaptchaV2 rc2 = new CaptchaHelperHostPluginRecaptchaV2(this, br, rcKey) {
                @Override
                public TYPE getType() {
                    return TYPE.INVISIBLE;
                }

                @Override
                protected boolean isEnterprise() {
                    return true;
                }
            };
            final String rcToken = rc2.getToken();
            loginform.put("captcha", Encoding.urlEncode(rcToken));
            loginform.put("previousPage", "/");
            loginform.put("user_action", "login");
            br.submitForm(loginform);
            if (br.getRequest().getHtmlCode().startsWith("{")) {
                final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                final String serversideErrorMsg = (String) entries.get("error");
                if (serversideErrorMsg != null) {
                    /*
                     * E.g. {"internalStatus":7,
                     * "error":"We do not recognize the location in which you are logging in from. An email has been sent to you with further instructions to login."
                     * }
                     */
                    throw new AccountInvalidException(serversideErrorMsg);
                }
                final int internalStatus = ((Number) entries.get("internalStatus")).intValue();
                if (internalStatus != 3 && internalStatus != 6) {
                    /* Messages according to: https://www.manyvids.com/js/compiled.js */
                    final String customErrormessage;
                    if (internalStatus == 0) {
                        customErrormessage = "Check Username & Password are correct and re-try.";
                    } else if (internalStatus == 2) {
                        customErrormessage = "Invalid credentials";
                    } else if (internalStatus == 4) {
                        customErrormessage = "You need to verify your account before you are able to log in to manyvids. Please check your email inbox.";
                    } else {
                        customErrormessage = "Unknown error status " + internalStatus;
                    }
                    throw new AccountInvalidException(customErrormessage);
                }
            } else {
                logger.warning("Login response is not json -> Login possibly failed");
            }
            br.getPage("/");
            if (!checkLogin(br)) {
                /* This should never happen */
                throw new AccountInvalidException("Login failed for unknown reasons");
            }
            account.saveCookies(br.getCookies(br.getHost()), "");
            return true;
        }
    }

    private boolean checkLogin(final Browser br) throws IOException {
        br.getPage("https://www." + getHost() + "/bff/user/viewer");
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        if (getLoginCookie(br) == null) {
            return false;
        } else if ("MEMBER".equals(entries.get("type"))) {
            return true;
        } else {
            /* E.g. type "GUEST" */
            return false;
        }
    }

    private String getLoginCookie(final Browser br) {
        /* Returns login-cookie which is supposed to be the internal user-ID of the profile we are currently logged in. */
        return br.getCookie(br.getHost(), "KGID", Cookies.NOTDELETEDPATTERN);
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        final String internalUserID = getLoginCookie(br);
        final Browser brc = br.cloneBrowser();
        brc.getPage("https://api.journey-bff.kiwi.manyvids.com/api/v1/user/" + internalUserID);
        final Map<String, Object> entries = restoreFromString(brc.getRequest().getHtmlCode(), TypeRef.MAP);
        if (Boolean.TRUE.equals(entries.get("premiumMember"))) {
            account.setType(AccountType.PREMIUM);
        } else {
            /* Free accounts can still contain single bought video elements which the user will be allowed to watch- and download. */
            account.setType(AccountType.FREE);
        }
        ai.setUnlimitedTraffic();
        return ai;
    }
    // @Override
    // public void handlePremium(final DownloadLink link, final Account account) throws Exception {
    // requestFileInformation(link);
    // login(account, false);
    // br.getPage(link.getPluginPatternMatcher());
    // if (account.getType() == AccountType.FREE) {
    // doFree(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
    // } else {
    // String dllink = this.checkDirectLink(link, "premium_directlink");
    // if (dllink == null) {
    // dllink = br.getRegex("").getMatch(0);
    // if (StringUtils.isEmpty(dllink)) {
    // logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
    // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    // }
    // }
    // dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
    // if (!this.looksLikeDownloadableContent(dl.getConnection())) {
    // if (dl.getConnection().getResponseCode() == 403) {
    // throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
    // } else if (dl.getConnection().getResponseCode() == 404) {
    // throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
    // }
    // logger.warning("The final dllink seems not to be a file!");
    // try {
    // br.followConnection(true);
    // } catch (final IOException e) {
    // logger.log(e);
    // }
    // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    // }
    // link.setProperty("premium_directlink", dl.getConnection().getURL().toString());
    // dl.startDownload();
    // }
    // }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final Account acc) {
        return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
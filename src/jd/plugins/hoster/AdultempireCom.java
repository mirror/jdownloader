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
import java.util.Locale;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.formatter.TimeFormatter;

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
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.AdultempireComCrawler;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { AdultempireComCrawler.class })
public class AdultempireCom extends PluginForHost {
    public AdultempireCom(PluginWrapper wrapper) {
        super(wrapper);
        /** 2023-11-14: Added account support and then realized that this website is using a light DRM for their HLS streams. */
        this.enablePremium("https://www.adultempire.com/unlimited/account/join");
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public String getAGBLink() {
        return "https://www.adultempire.com/help/cs_termsofuse.html";
    }

    private static List<String[]> getPluginDomains() {
        return AdultempireComCrawler.getPluginDomains();
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
            /* No regex. URLs get assigned via crawler plugin. */
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
        // this.setBrowserExclusive();
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

    public boolean login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            br.setCookiesExclusive(true);
            final Cookies cookies = account.loadCookies("");
            final String urlPathAccountInfo = "/unlimited/account";
            if (cookies != null) {
                logger.info("Attempting cookie login");
                this.br.setCookies(this.getHost(), cookies);
                if (!force) {
                    /* Don't validate cookies */
                    return false;
                }
                br.getPage("https://www." + this.getHost() + urlPathAccountInfo);
                if (this.isLoggedin(br)) {
                    logger.info("Cookie login successful");
                    /* Refresh cookie timestamp */
                    account.saveCookies(this.br.getCookies(br.getHost()), "");
                    return true;
                } else {
                    logger.info("Cookie login failed");
                    br.clearCookies(null);
                }
            }
            logger.info("Performing full login");
            br.getPage("https://www." + this.getHost() + "/unlimited/account/signin");
            final Browser brc = br.cloneBrowser();
            final Form loginform = new Form();
            loginform.setAction("/Subscription/SubscriptionSignIn");
            loginform.setMethod(MethodType.POST);
            loginform.put("username", Encoding.urlEncode(account.getUser()));
            loginform.put("password", Encoding.urlEncode(account.getPass()));
            brc.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
            brc.submitForm(loginform);
            final Map<String, Object> entries = restoreFromString(brc.getRequest().getHtmlCode(), TypeRef.MAP);
            if (!Boolean.TRUE.equals(entries.get("success"))) {
                final Map<String, Object> messagemap = (Map<String, Object>) entries.get("message");
                throw new AccountInvalidException(messagemap.get("text").toString());
            }
            br.getPage("/unlimited/account/activate");
            /* Double-check */
            if (!isLoggedin(br)) {
                throw new AccountInvalidException();
            }
            br.getPage(urlPathAccountInfo);
            account.saveCookies(br.getCookies(br.getHost()), "");
            return true;
        }
    }

    private boolean isLoggedin(final Browser br) {
        return br.containsHTML("Sign Out\\s*</a>");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        ai.setUnlimitedTraffic();
        final String nextBillingDateStr = br.getRegex("Next Billing Date\\s*</h2>([^<]+)<").getMatch(0);
        if (nextBillingDateStr != null) {
            account.setType(AccountType.PREMIUM);
            ai.setValidUntil(TimeFormatter.getMilliSeconds(nextBillingDateStr.trim(), "MMM dd, yyyy", Locale.ENGLISH), br);
        } else {
            account.setType(AccountType.FREE);
        }
        account.setConcurrentUsePossible(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        throw new AccountRequiredException();
        // login(account, false);
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
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
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
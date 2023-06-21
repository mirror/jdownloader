//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import java.util.Arrays;
import java.util.Locale;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.MultiHosterManagement;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "linkgen.vip" }, urls = { "" })
public class LinkgenVip extends PluginForHost {
    /* Connection limits */
    private final boolean                ACCOUNT_PREMIUM_RESUME    = true;
    private final int                    ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    private static MultiHosterManagement mhm                       = new MultiHosterManagement("linkgen.vip");

    public LinkgenVip(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://linkgen.vip/premium.php");
    }

    @Override
    public String getAGBLink() {
        return "https://linkgen.vip/legal.php";
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = new Browser();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws PluginException {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (account == null) {
            return false;
        } else {
            mhm.runCheck(account, link);
            return true;
        }
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        /* handle premium should never be called */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.MULTIHOST };
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        mhm.runCheck(account, link);
        login(account, false);
        String dllink = null;
        if (StringUtils.isEmpty(dllink)) {
            mhm.handleErrorGeneric(account, link, "Failed to find final downloadlink", 20, 5 * 60 * 1000l);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            mhm.handleErrorGeneric(account, link, "Final downloadurl did not lead to file", 2, 5 * 60 * 1000l);
        }
        this.dl.startDownload();
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        br.getPage("https://" + this.getHost() + "/");
        ArrayList<String> supportedHosts = new ArrayList<String>();
        account.setType(AccountType.PREMIUM);
        final String expire = br.getRegex("TODO_fixme").getMatch(0);
        if (expire != null) {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "MM/dd/yyyy hh:mm a", Locale.US), br);
        }
        br.getPage("/todo_fixme.php");
        final String[] hostlist = br.getRegex("TODO").getColumn(0);
        if (hostlist != null) {
            supportedHosts = new ArrayList<String>(Arrays.asList(hostlist));
        }
        ai.setUnlimitedTraffic();
        ai.setMultiHostSupport(this, supportedHosts);
        return ai;
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    this.br.setCookies(this.getHost(), cookies);
                    if (!force) {
                        /* Do not check cookies */
                        return;
                    }
                    br.getPage("https://" + this.getHost() + "/");
                    if (isLoggedin(br)) {
                        logger.info("Cookie login successful");
                        account.saveCookies(this.br.getCookies(this.getHost()), "");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                        br.clearCookies(null);
                    }
                }
                br.getPage("https://" + this.getHost() + "/login.php");
                final Form loginform = br.getFormbyProperty("name", "UsernameLoginForm");
                loginform.put("username", Encoding.urlEncode(account.getUser()));
                loginform.put("password", Encoding.urlEncode(account.getPass()));
                final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                loginform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                br.submitForm(loginform);
                if (!isLoggedin(br)) {
                    final String loginErrormessage = br.getRegex("class=\"error\"[^>]*>\\s*<p><b>([^<]+)</b></p>").getMatch(0);
                    if (loginErrormessage != null) {
                        throw new AccountInvalidException(Encoding.htmlDecode(loginErrormessage).trim());
                    } else {
                        throw new AccountInvalidException();
                    }
                }
                account.saveCookies(this.br.getCookies(br.getHost()), "");
                if (true) {
                    throw new AccountUnavailableException("Unfinished plugin: Contact JDownloader support", 3 * 60 * 1000l);
                }
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    private boolean isLoggedin(final Browser br) {
        return br.containsHTML("/logout");
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
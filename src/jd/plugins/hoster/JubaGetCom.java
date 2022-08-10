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

import java.io.IOException;
import java.util.ArrayList;

import org.appwork.utils.DebugMode;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.MultiHosterManagement;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "juba-get.com" }, urls = { "" })
public class JubaGetCom extends PluginForHost {
    /* Connection limits */
    private static final boolean         ACCOUNT_PREMIUM_RESUME    = true;
    private static final int             ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    private static MultiHosterManagement mhm                       = new MultiHosterManagement("juba-get.com");

    public JubaGetCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://juba-get.com/plans");
    }

    @Override
    public String getAGBLink() {
        return "https://juba-get.com/terms";
    }

    private Browser prepBRWebsite(final Browser br) {
        br.setCookiesExclusive(true);
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws PluginException {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        if (account == null) {
            /* This plugin doesn't allow downloads without account */
            return false;
        } else {
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
        prepBRWebsite(this.br);
        mhm.runCheck(account, link);
        login(account, false);
        String dllink = null;
        if (StringUtils.isEmpty(dllink)) {
            mhm.handleErrorGeneric(account, link, "Failed to generate downloadlink", 50, 1 * 60 * 1000l);
        }
        link.setProperty(this.getHost() + "directlink", dllink);
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
                mhm.handleErrorGeneric(account, link, "Final downloadurl did not lead to downloadable content", 50, 1 * 60 * 1000l);
            }
            this.dl.startDownload();
        } catch (final Exception e) {
            link.setProperty(this.getHost() + "directlink", Property.NULL);
            throw e;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        /* TODO: Add detection of account type */
        br.getPage("/hosts");
        final String[] htmls = br.getRegex("<tr>(.*?)</tr>").getColumn(0);
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        for (final String html : htmls) {
            final String host = new Regex(html, "class=\"servidor_online\"[^>]*alt=\"([^\"]+)\"").getMatch(0);
            if (host == null) {
                /* Bad HTML or broken plugin. */
                continue;
            }
            supportedHosts.add(host);
        }
        ai.setMultiHostSupport(this, supportedHosts);
        return ai;
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unfinished plugin");
            }
            br.setCookiesExclusive(true);
            prepBRWebsite(this.br);
            try {
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    this.br.setCookies(this.getHost(), cookies);
                    if (!force) {
                        return;
                    }
                    br.getPage("https://" + this.getHost() + "/");
                    if (this.isLoggedIN(br)) {
                        account.saveCookies(this.br.getCookies(br.getHost()), "");
                        return;
                    }
                    br.clearCookies(br.getHost());
                }
                br.getPage("https://" + this.getHost() + "/login");
                final Form loginform = br.getFormbyActionRegex(".*/login.*");
                if (loginform == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.put("email", Encoding.urlEncode(account.getUser()));
                loginform.put("password", Encoding.urlEncode(account.getPass()));
                loginform.put("remember", "on"); // make sure that cookies last long
                loginform.put("g-recaptcha-response", Encoding.urlEncode(new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken()));
                this.postAPISafe("/login/", "auto_login=checked&u=" + Encoding.urlEncode(account.getUser()) + "&p=" + Encoding.urlEncode(account.getPass()));
                br.submitForm(loginform);
                final String errorMsg = br.getRegex("div class=\"alert alert-danger\">\\s*<ul>\\s*<li>([^<]+)</li>").getMatch(0);
                /*
                 * 2022-08-10: Sometimes even after successful login website will redirect us to /generator and display error 500 this we'll
                 * try this small workaround.
                 */
                if (!this.isLoggedIN(br)) {
                    br.getPage("/");
                }
                if (!this.isLoggedIN(br)) {
                    if (errorMsg != null) {
                        throw new AccountInvalidException(errorMsg);
                    } else {
                        throw new AccountInvalidException();
                    }
                }
                account.saveCookies(this.br.getCookies(br.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    private boolean isLoggedIN(final Browser br) {
        if (br.containsHTML("/logout")) {
            return true;
        } else {
            return false;
        }
    }

    private void postAPISafe(final String accesslink, final String postdata) throws IOException, PluginException {
        br.postPage(accesslink, postdata);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
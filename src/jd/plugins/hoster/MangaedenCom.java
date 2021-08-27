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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
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

/** 2021-08-27: This host plugin is only needed as a dummy for logging in as login is required to access some content via crawler. */
@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "mangaeden.com" }, urls = { "" })
public class MangaedenCom extends PluginForHost {
    public MangaedenCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.mangaeden.com/eng/");
    }

    @Override
    public String getAGBLink() {
        return "https://www.mangaeden.com/eng/";
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
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    public boolean login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    logger.info("Attempting cookie login");
                    this.br.setCookies(this.getHost(), cookies);
                    if (!force && System.currentTimeMillis() - account.getCookiesTimeStamp("") < 5 * 60 * 1000l) {
                        logger.info("Cookies are still fresh --> Trust cookies without login");
                        return false;
                    }
                    br.getPage("https://www." + this.getHost() + "/");
                    if (this.isLoggedin(this.br)) {
                        logger.info("Cookie login successful");
                        /* Refresh cookie timestamp */
                        account.saveCookies(this.br.getCookies(this.getHost()), "");
                        return true;
                    } else {
                        logger.info("Cookie login failed");
                        br.clearCookies(br.getHost());
                    }
                }
                logger.info("Performing full login");
                br.getPage("https://www." + this.getHost() + "/eng/login");
                Form loginform = null;
                for (final Form form : br.getForms()) {
                    if (form.hasInputFieldByName("username") && form.containsHTML("remember")) {
                        loginform = form;
                        break;
                    }
                }
                if (loginform == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.put("username", Encoding.urlEncode(account.getUser()));
                loginform.put("password", Encoding.urlEncode(account.getPass()));
                loginform.put("remember", "1");
                br.submitForm(loginform);
                if (!isLoggedin(this.br)) {
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
        return br.getCookie(this.getHost(), "remember_token", Cookies.NOTDELETEDPATTERN) != null;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        account.setType(AccountType.FREE);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 1;
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
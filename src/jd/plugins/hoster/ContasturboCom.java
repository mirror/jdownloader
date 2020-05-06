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

import java.util.Arrays;

import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

import jd.PluginWrapper;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
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
import jd.plugins.components.MultiHosterManagement;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "contasturbo.com" }, urls = { "" })
public class ContasturboCom extends PluginForHost {
    private static MultiHosterManagement mhm = new MultiHosterManagement("contasturbo.com");

    public ContasturboCom(PluginWrapper wrapper) {
        super(wrapper);
        setStartIntervall(1 * 1000l);
        this.enablePremium("https://www.contasturbo.com/planos/");
    }

    @Override
    public String getAGBLink() {
        return "https://www.contasturbo.com/";
    }

    private boolean login(final Account account, final boolean validateCookies) throws Exception {
        synchronized (account) {
            br.setCustomCharset("utf-8");
            br.setFollowRedirects(true);
            br.setCookiesExclusive(true);
            br.setFollowRedirects(true);
            final Cookies userCookies = Cookies.parseCookiesFromJsonString(account.getPass());
            if (userCookies != null) {
                /* Developer debug test */
                logger.info("Attempting user cookie login");
                br.setCookies(userCookies);
                if (!validateCookies && System.currentTimeMillis() - account.getCookiesTimeStamp("") <= 5 * 60 * 1000l) {
                    logger.info("Trust cookies as they're not that old");
                    return false;
                }
                br.getPage("https://www." + account.getHoster() + "/gerador/");
                if (this.isLoggedIN()) {
                    logger.info("User cookie login successful");
                    account.saveCookies(br.getCookies(this.getHost()), "");
                    return true;
                } else {
                    logger.info("User cookie login failed");
                    /* Throw Exception as we do not have username + password and cannot refresh the session! */
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            final Cookies cookies = account.loadCookies("");
            if (cookies != null) {
                logger.info("Attempting cookie login");
                br.setCookies(this.getHost(), cookies);
                if (!validateCookies && System.currentTimeMillis() - account.getCookiesTimeStamp("") <= 5 * 60 * 1000l) {
                    logger.info("Trust cookies as they're not that old");
                    return false;
                }
                br.getPage("https://www." + account.getHoster() + "/");
                if (this.isLoggedIN()) {
                    logger.info("Cookie login successful");
                    account.saveCookies(br.getCookies(this.getHost()), "");
                    return true;
                } else {
                    logger.info("Cookie login failed");
                }
            }
            logger.info("Performing full login");
            br.clearCookies(null);
            br.setFollowRedirects(true);
            br.getPage("https://www." + account.getHoster() + "/login/");
            final Form loginform = br.getFormbyActionRegex(".*?login.*?");
            if (loginform == null) {
                logger.warning("Failed to find loginform");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            loginform.put("email", Encoding.urlEncode(account.getUser()));
            loginform.put("password", Encoding.urlEncode(account.getPass()));
            /* Makes cookies last longer */
            loginform.put("remember", "1");
            br.submitForm(loginform);
            if (!isLoggedIN()) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            account.saveCookies(br.getCookies(this.getHost()), "");
            return true;
        }
    }

    private boolean isLoggedIN() {
        return br.getCookie(br.getHost(), "ct_auth", Cookies.NOTDELETEDPATTERN) != null && br.getCookie(br.getHost(), "ct_user", Cookies.NOTDELETEDPATTERN) != null;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        this.login(account, true);
        if (br.getURL() == null || !br.getURL().contains("/gerador")) {
            br.getPage("/gerador/");
        }
        final String expireDays = br.getRegex("Premium válida por (\\d+) dias").getMatch(0);
        final String expireExtraHours = br.getRegex("Premium válida por \\d+ dias e (\\d+) horas").getMatch(0);
        if (expireDays != null) {
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium Account");
            ai.setUnlimitedTraffic();
            long hours_total = Long.parseLong(expireDays) * 24;
            if (expireExtraHours != null) {
                hours_total += Long.parseLong(expireExtraHours);
            }
            ai.setValidUntil(System.currentTimeMillis() + hours_total * 60 * 60 * 1000, br);
        } else {
            account.setType(AccountType.FREE);
            ai.setTrafficLeft(0);
        }
        /* TODO: Move away from static list of supported hosts. */
        final String[] hostsList = { "1fichier.com", "2shared.com", "4shared.com", "alfafile.net", "aniteca.zlx.com.br", "axfiles.com", "bigfile.to", "brfiles.com", "dataFile.com", "file4go.net", "fileFactory.com", "fileNext.com", "mediafire.com", "mega.nz", "rapidgator.net", "sendspace.com", "turbobit.net", "uploaded.net", "uptobox.com", "userscloud.com" };
        ai.setMultiHostSupport(this, Arrays.asList(hostsList));
        return ai;
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        mhm.runCheck(account, link);
        login(account, false);
        final String url = Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this));
        br.postPage("https://www." + this.getHost() + "/api/ext/linkRequest/", "links=" + url);
        final String dllink = br.getRegex("\"(http?://cdn\\.contasturbo\\.com/dl/[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) {
            mhm.handleErrorGeneric(account, link, "dllinknull", 50);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            mhm.handleErrorGeneric(account, link, "unknown_dl_error", 50);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (account == null || account.getType() != AccountType.PREMIUM) {
            return false;
        }
        return true;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
    }
}
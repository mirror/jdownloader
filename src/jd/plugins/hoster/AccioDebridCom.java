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
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
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

import org.appwork.storage.JSonMapperException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Exceptions;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "accio-debrid.com" }, urls = { "" })
public class AccioDebridCom extends PluginForHost {
    private static final String          API_BASE               = "https://accio-debrid.com/apiv2/index.php";
    private static MultiHosterManagement mhm                    = new MultiHosterManagement("accio-debrid.com");
    private static final boolean         resume                 = true;
    private static final int             maxchunks              = -10;
    private static final String          PROPERTY_ACCOUNT_TOKEN = "LOGIN_TOKEN";

    @SuppressWarnings("deprecation")
    public AccioDebridCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://accio-debrid.com/en/premium-offers/");
    }

    @Override
    public String getAGBLink() {
        return "https://accio-debrid.com/en/terms-of-service/";
    }

    private Browser prepBR(final Browser br) {
        br.setCookiesExclusive(true);
        br.setFollowRedirects(true);
        br.getHeaders().put("User-Agent", "JDownloader");
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (account == null) {
            return false;
        } else {
            return super.canHandle(link, account);
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.MULTIHOST };
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        mhm.runCheck(account, link);
        this.login(account, false);
        if (!attemptStoredDownloadurlDownload(link)) {
            final UrlQuery query = new UrlQuery();
            query.add("link", Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this)));
            if (link.getDownloadPassword() != null) {
                query.add("password", Encoding.urlEncode(link.getDownloadPassword()));
            }
            br.postPage(API_BASE + "?action=getLink&token=" + Encoding.urlEncode(this.getLoginToken(account)), query);
            final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            final String dllink = (String) entries.get("debridLink");
            final String response_text = entries.get("response_text").toString(); // "ok" if no error is returned
            if (StringUtils.isEmpty(dllink)) {
                mhm.handleErrorGeneric(account, link, response_text, 50, 5 * 60 * 1000l);
            }
            link.setProperty(this.getHost() + "directlink", dllink);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxchunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                mhm.handleErrorGeneric(account, link, "unknown_dl_error", 10, 5 * 60 * 1000l);
            }
        }
        this.dl.startDownload();
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link) throws Exception {
        final String directurlproperty = this.getHost() + "directlink";
        final String url = link.getStringProperty(directurlproperty);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        boolean valid = false;
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, resume, maxchunks);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                valid = true;
                return true;
            } else {
                link.removeProperty(directurlproperty);
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
                dl = null;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        final Map<String, Object> root = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        /* 2022-04-11: Look like API is returning long values as string... */
        final long expireTimestamp = JavaScriptEngineFactory.toLong(root.get("vip_end"), -1);
        if (expireTimestamp != -1) {
            account.setType(AccountType.PREMIUM);
            ai.setValidUntil(expireTimestamp * 1000l);
        } else {
            account.setType(AccountType.FREE);
            ai.setExpired(true);
        }
        br.getPage(API_BASE + "?action=getHostersList");
        final Map<String, Object> hostsMap = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        for (final Object hostInfoO : hostsMap.values()) {
            /*
             * Check for this because server-response contains the data we want along with response-data like "success:true -> not nicely
             * made!
             */
            if (!(hostInfoO instanceof Map)) {
                continue;
            }
            final Map<String, Object> hostInfo = (Map<String, Object>) hostInfoO;
            final List<String> domains = (List<String>) hostInfo.get("domains");
            supportedHosts.addAll(domains);
        }
        ai.setMultiHostSupport(this, supportedHosts);
        account.setConcurrentUsePossible(true);
        return ai;
    }

    private void login(final Account account, final boolean validateLogins) throws IOException, PluginException, InterruptedException {
        synchronized (account) {
            try {
                prepBR(this.br);
                if (getLoginToken(account) != null && !validateLogins) {
                    logger.info("Trust existing login-token");
                    return;
                }
                logger.info("Performing full login");
                br.getPage(API_BASE + "?action=login&login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                final Map<String, Object> root = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                final String response_code = (String) root.get("response_code");
                if (StringUtils.equalsIgnoreCase(response_code, "INCORRECT_PASSWORD")) {
                    throw new AccountInvalidException();
                }
                final String token = (String) root.get("token");
                if (StringUtils.isEmpty(token)) {
                    throw new AccountUnavailableException("Unknown login error", 5 * 60 * 1000l);
                }
                account.setProperty(PROPERTY_ACCOUNT_TOKEN, token);
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.removeProperty(PROPERTY_ACCOUNT_TOKEN);
                }
                throw e;
            }
        }
    }

    private String getLoginToken(final Account account) {
        return account.getStringProperty(PROPERTY_ACCOUNT_TOKEN);
    }

    private void checkErrors(final Account account) throws PluginException, InterruptedException {
        /* TODO: Make use of this */
        try {
            final Object jsonO = JSonStorage.restoreFromString(br.toString(), TypeRef.OBJECT);
            if (jsonO == null || !(jsonO instanceof Map)) {
                return;
            }
            // handleErrorMap(account, (Map<String, Object>) jsonO);
        } catch (final JSonMapperException jme) {
            if (this.getDownloadLink() != null) {
                mhm.handleErrorGeneric(account, this.getDownloadLink(), "Bad API answer", 50, 5 * 60 * 1000l);
            } else {
                throw Exceptions.addSuppressed(new AccountUnavailableException("Bad API answer", 1 * 60 * 1000l), jme);
            }
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
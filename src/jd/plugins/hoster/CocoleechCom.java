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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;

import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.net.CountingOutputStream;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
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
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "cocoleech.com" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32424" })
public class CocoleechCom extends PluginForHost {
    private static final String          API_ENDPOINT        = "https://members.cocoleech.com/auth/api";
    /* Last updated: 2017-02-08 according to admin request. */
    private static final int             defaultMAXDOWNLOADS = 20;
    private static final int             defaultMAXCHUNKS    = -4;
    private static final boolean         defaultRESUME       = true;
    // private final String apikey = "cdb5efc9c72196c1bd8b7a594b46b44f";
    private static MultiHosterManagement mhm                 = new MultiHosterManagement("cocoleech.com");

    @SuppressWarnings("deprecation")
    public CocoleechCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://members.cocoleech.com/");
        this.setStartIntervall(3000l);
    }

    @Override
    public String getAGBLink() {
        return "https://members.cocoleech.com/terms";
    }

    private Browser prepBR(final Browser br) {
        br.setCookiesExclusive(true);
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws PluginException, IOException {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        if (account == null) {
            /* without account its not possible to download the link */
            return false;
        }
        return true;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        handleDL(account, link);
    }

    private void handleDL(final Account account, final DownloadLink link) throws Exception {
        String dllink = checkDirectLink(link, this.getHost() + "directlink");
        String maxchunksStr = null;
        if (dllink == null) {
            br.setFollowRedirects(true);
            /* request creation of downloadlink */
            /* Make sure that the file exists - unnecessary step in my opinion (psp) but admin wanted to have it implemented this way. */
            this.br.getPage(API_ENDPOINT + "?username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&link=" + Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this)));
            handleAPIErrors(this.br, account, link);
            maxchunksStr = PluginJSonUtils.getJsonValue(this.br, "chunks");
            dllink = PluginJSonUtils.getJsonValue(br, "download");
            if (StringUtils.isEmpty(dllink)) {
                logger.warning("Final downloadlink is null");
                mhm.handleErrorGeneric(account, link, "dllinknull", 50, 5 * 60 * 1000l);
            }
        }
        int maxChunks = defaultMAXCHUNKS;
        if (!StringUtils.isEmpty(maxchunksStr) && maxchunksStr.matches("\\d+")) {
            maxChunks = -Integer.parseInt(maxchunksStr);
        }
        link.setProperty(this.getHost() + "directlink", dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, defaultRESUME, maxChunks);
        if (dl.getConnection().getContentType().contains("html") || dl.getConnection().getContentType().contains("json")) {
            br.followConnection();
            handleAPIErrors(this.br, account, link);
            mhm.handleErrorGeneric(account, link, "unknowndlerror", 50, 5 * 60 * 1000l);
        }
        this.dl.startDownload();
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        prepBR(this.br);
        mhm.runCheck(account, link);
        handleDL(account, link);
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        final String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openGetConnection(dllink);
                if (!con.isOK() || con.getContentType().contains("html") || con.getResponseCode() == 404 || con.getLongContentLength() == -1) {
                    downloadLink.removeProperty(property);
                } else {
                    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    final CountingOutputStream cos = new CountingOutputStream(bos);
                    IO.readStreamToOutputStream(128 * 1024, con.getInputStream(), cos, false);
                    if (cos.transferedBytes() < 100) {
                        downloadLink.removeProperty(property);
                        logger.info(bos.toString("UTF-8"));
                    } else {
                        return dllink;
                    }
                }
            } catch (final Exception e) {
                logger.log(e);
                downloadLink.removeProperty(property);
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return null;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        prepBR(this.br);
        final AccountInfo ai = new AccountInfo();
        login(account);
        final String accounttype = PluginJSonUtils.getJsonValue(br, "type");
        final String trafficleft = PluginJSonUtils.getJsonValue(br, "traffic_left");
        final String validuntil = PluginJSonUtils.getJsonValue(br, "expire_date");
        long timestamp_validuntil = 0;
        if (validuntil != null) {
            timestamp_validuntil = TimeFormatter.getMilliSeconds(validuntil, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        }
        if ("premium".equalsIgnoreCase(accounttype) && timestamp_validuntil > 0) {
            ai.setValidUntil(timestamp_validuntil);
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium account");
            account.setConcurrentUsePossible(true);
            /*
             * 2017-02-08: Accounts do usually not have general traffic limits - however there are individual host traffic limits see
             * mainpage (when logged in) --> Right side "Daily Limit(s)"
             */
            if (trafficleft != null && !trafficleft.equalsIgnoreCase("unlimited")) {
                ai.setTrafficLeft(Long.parseLong(trafficleft));
            } else {
                ai.setUnlimitedTraffic();
            }
        } else {
            account.setType(AccountType.FREE);
            ai.setStatus("Registered (free) account");
            /*
             * 2016-05-05: According to admin, free accounts cannot download anything. We will allow download attempts anyways but this will
             * usually result in error "Premium membership expired." which will then temp. deactivate the account!
             */
            account.setConcurrentUsePossible(false);
            account.setMaxSimultanDownloads(1);
            ai.setUnlimitedTraffic();
        }
        this.br.getPage(API_ENDPOINT + "/hosts-status");
        ArrayList<String> supportedhostslist = new ArrayList();
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final ArrayList<Object> hosters = (ArrayList<Object>) entries.get("result");
        for (final Object hostero : hosters) {
            entries = (LinkedHashMap<String, Object>) hostero;
            String host = (String) entries.get("host");
            final String status = (String) entries.get("status");
            if (host != null && "online".equalsIgnoreCase(status)) {
                supportedhostslist.add(host);
            }
        }
        ai.setMultiHostSupport(this, supportedhostslist);
        return ai;
    }

    private void login(final Account account) throws IOException, PluginException, InterruptedException {
        synchronized (account) {
            this.br.getPage(API_ENDPOINT + "/info?username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
            handleAPIErrors(this.br, account, null);
        }
    }

    private void handleAPIErrors(final Browser br, final Account account, final DownloadLink link) throws PluginException, InterruptedException {
        final String statusmsg = PluginJSonUtils.getJsonValue(br, "message");
        if (statusmsg != null) {
            if (statusmsg.equalsIgnoreCase("Incorrect log-in or password.")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusmsg, PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else if (statusmsg.matches("Daily limit is reached\\. Hours left:\\s*?\\d+")) {
                mhm.handleErrorGeneric(account, link, "daily_limit_reached", 10, 5 * 60 * 1000l);
            } else if (statusmsg.equalsIgnoreCase("Failed to generate link.")) {
                mhm.handleErrorGeneric(account, link, "failedtogeneratelink", 50, 5 * 60 * 1000l);
            } else if (statusmsg.equalsIgnoreCase("Premium membership expired.")) {
                logger.info("Premium account has expired");
                account.getAccountInfo().setExpired(true);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusmsg, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            } else {
                /* Unknown error */
                mhm.handleErrorGeneric(account, link, "unknown_api_error", 50, 5 * 60 * 1000l);
            }
        }
    }

    @Override
    public int getMaxSimultanDownload(final DownloadLink link, final Account account) {
        if (account != null) {
            return defaultMAXDOWNLOADS;
        } else {
            return 0;
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
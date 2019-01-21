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
import java.util.HashMap;
import java.util.Random;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "conexaomega.com" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" })
public class ConexaomegaCom extends PluginForHost {
    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private static Object                                  LOCK               = new Object();
    private static final String                            COOKIE_HOST        = "https://conexaomega.com";

    /**
     * Important notes:<br />
     * 1. conexaomega.com and conexaomega.com.br are two DIFFERENT hosts! <br/>
     * 2. Download never worked for me, I always got server errors, limits are also untested
     */
    public ConexaomegaCom(PluginWrapper wrapper) {
        super(wrapper);
        setStartIntervall(1 * 1000l);
        this.enablePremium("https://www.conexaomega.com/planos");
    }

    @Override
    public String getAGBLink() {
        return "https://www.conexaomega.com/";
    }

    @Override
    public int getMaxSimultanDownload(final DownloadLink link, final Account account) {
        return -1;
    }

    @SuppressWarnings({ "deprecation" })
    private boolean login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /** Load cookies */
                br.setCustomCharset("utf-8");
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    br.setCookies(this.getHost(), cookies);
                    return true;
                }
                br.setFollowRedirects(true);
                br.getPage("https://www." + account.getHoster() + "/login");
                br.postPage("https://www." + account.getHoster() + "/login", "email=" + Encoding.urlEncode(account.getUser()) + "&senha=" + Encoding.urlEncode(account.getPass()) + "&remember=1&x=" + new Random().nextInt(100) + "&y=" + new Random().nextInt(100));
                if (br.getCookie(COOKIE_HOST, "cm_auth") == null) {
                    return false;
                }
                account.saveCookies(br.getCookies(this.getHost()), "");
                return true;
            } catch (final Exception e) {
                account.clearCookies("");
                return false;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        if (!login(account, true)) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        ai.setProperty("multiHostSupport", Property.NULL);
        br.getPage("https://www." + account.getHoster() + "/gerador");
        final String expireDays = br.getRegex(">Seu plano expira em (\\d+) dias\\.</strong>").getMatch(0);
        if (expireDays != null) {
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium Account");
            ai.setUnlimitedTraffic();
            ai.setValidUntil(System.currentTimeMillis() + Long.parseLong(expireDays) * 24 * 60 * 60 * 1000);
        } else {
            /* Accept free accounts but it's impossible to download with them! */
            account.setType(AccountType.FREE);
            ai.setStatus("Free Account");
            ai.setTrafficLeft(0);
        }
        br.getPage("https://www." + account.getHoster() + "/");
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        boolean extendedCheckSuccessful = false;
        final String supportedHostsTableHTML = br.getRegex("title=\"Status\" />\\s*?<br /><br />\\s*?<table cellspacing=\"5\" style=\"margin-left:10px\">(.*?)</table>").getMatch(0);
        if (supportedHostsTableHTML != null) {
            final String[] hostInfoHtmls = new Regex(supportedHostsTableHTML, "<td.*?</tr>").getColumn(-1);
            for (final String hostInfoHtml : hostInfoHtmls) {
                String crippledHost = new Regex(hostInfoHtml, "<td>\\s*?([A-Za-z0-9\\-\\.]+)").getMatch(0);
                final boolean isOnline = new Regex(hostInfoHtml, "Novo|Disponível").matches();
                if (!StringUtils.isEmpty(crippledHost) && isOnline) {
                    crippledHost = crippledHost.toLowerCase();
                    extendedCheckSuccessful = true;
                    supportedHosts.add(crippledHost);
                }
            }
        }
        if (!extendedCheckSuccessful) {
            /* Fallback - wider RegEx without check for host-status */
            final String[] crippledHosts = br.getRegex("<td>\\s*?([A-Za-z0-9\\-\\.]+)").getColumn(0);
            for (String crippledHost : crippledHosts) {
                crippledHost = crippledHost.toLowerCase();
                supportedHosts.add(crippledHost);
            }
        }
        ai.setMultiHostSupport(this, supportedHosts);
        return ai;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap != null) {
                Long lastUnavailable = unavailableMap.get(link.getHost());
                if (lastUnavailable != null && System.currentTimeMillis() < lastUnavailable) {
                    final long wait = lastUnavailable - System.currentTimeMillis();
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Host is temporarily unavailable via " + this.getHost(), wait);
                } else if (lastUnavailable != null) {
                    unavailableMap.remove(link.getHost());
                    if (unavailableMap.size() == 0) {
                        hostUnavailableMap.remove(account);
                    }
                }
            }
        }
        login(account, false);
        final String url = Encoding.urlEncode(link.getPluginPatternMatcher());
        br.getHeaders().put("Accept", "*/*");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        showMessage(link, "Generating downloadlink...");
        br.getPage("https://www." + account.getHoster() + "/_gerar?link=" + url + "&rnd=" + System.currentTimeMillis());
        final String dllink = br.getRegex("\"(https?://cdn\\.conexaomega\\.com/dl/[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("Erro \\d+")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error: " + br.toString().trim(), 30 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    private void tempUnavailableHoster(final Account account, final DownloadLink downloadLink, long timeout) throws PluginException {
        if (downloadLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
        }
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap == null) {
                unavailableMap = new HashMap<String, Long>();
                hostUnavailableMap.put(account, unavailableMap);
            }
            /* wait to retry this host */
            unavailableMap.put(downloadLink.getHost(), (System.currentTimeMillis() + timeout));
        }
        throw new PluginException(LinkStatus.ERROR_RETRY);
    }

    @Override
    public boolean canHandle(final DownloadLink downloadLink, final Account account) throws Exception {
        return true;
    }

    private void showMessage(final DownloadLink link, final String message) {
        link.getLinkStatus().setStatusText(message);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
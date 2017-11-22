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
import java.util.LinkedHashMap;

import jd.PluginWrapper;
import jd.config.Property;
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
import jd.plugins.components.PluginJSonUtils;

import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "mega-debrid.eu" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" })
public class MegaDebridEu extends PluginForHost {
    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private static final String                            NOCHUNKS           = "NOCHUNKS";
    private static Object                                  ACCLOCK            = new Object();
    private final String                                   mName              = "www.mega-debrid.eu";
    private final String                                   mProt              = "https://";

    public MegaDebridEu(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(mProt + mName + "/index.php");
    }

    @Override
    public String getAGBLink() {
        return mProt + mName + "/index.php";
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    private void prepBrowser(final Browser br) {
        br.setFollowRedirects(true);
        br.setCustomCharset("UTF-8");
        br.getHeaders().put("User-Agent", "JDownloader-" + Math.max(super.getVersion(), 0));
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ac = new AccountInfo();
        prepBrowser(br);
        login(account);
        final String daysLeft = PluginJSonUtils.getJson(br, "vip_end");
        if (daysLeft != null && !"0".equals(daysLeft)) {
            ac.setValidUntil(Long.parseLong(daysLeft) * 1000l);
        } else if ("0".equals(daysLeft)) {
            ac.setExpired(true);
            account.setType(AccountType.FREE);
            ac.setStatus("Free Account!");
            return ac;
        } else {
            throw new AccountInvalidException();
        }
        // now it's time to get all supported hosts
        br.getPage("/api.php?action=getHostersList");
        final LinkedHashMap<String, Object> results = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        if (!"ok".equalsIgnoreCase((String) results.get("response_code"))) {
            throw new AccountInvalidException();
        }
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        for (final Object resultz : (ArrayList<Object>) results.get("hosters")) {
            final LinkedHashMap<String, Object> r = (LinkedHashMap<String, Object>) resultz;
            if (!"up".equals(r.get("status"))) {
                continue;
            }
            for (final String domain : (ArrayList<String>) r.get("domains")) {
                supportedHosts.add(domain);
            }
        }
        ac.setMultiHostSupport(this, supportedHosts);
        account.setType(AccountType.PREMIUM);
        ac.setStatus("Premium Account");
        return ac;
    }

    private String login(Account account) throws Exception {
        synchronized (ACCLOCK) {
            br.getPage(mProt + mName + "/api.php?action=connectUser&login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
            if (br.getHttpConnection().getResponseCode() == 403 || br.getHttpConnection().getResponseCode() == 404) {
                // server issue
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Host provider has server issues!", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            final String token = PluginJSonUtils.getJson(br, "token");
            if (!"ok".equalsIgnoreCase(PluginJSonUtils.getJson(br, "response_code"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            if (token != null) {
                account.setProperty("token", token);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            return token;
        }
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
        String url = link.getDownloadURL();
        // link corrections
        if (link.getHost().matches("ddlstorage\\.com")) {
            // needs full url!
            url += "/" + link.getName();
        } else if (link.getHost().matches("filefactory\\.com")) {
            // http://www.filefactory.com/file/asd/n/ads.rar
            if (!url.endsWith("/")) {
                url += "/";
            }
            url += "/n/" + link.getName();
        }
        url = Encoding.urlEncode(url);
        showMessage(link, "Phase 1/2: Generate download link");
        prepBrowser(br);
        String token = account.getStringProperty("token", null);
        if (token == null) {
            // this shouldn't happen!
            token = login(account);
            if (token == null) {
                // big problem!
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\n BIG PROBLEM_1", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        for (int i = 0; i != 3; i++) {
            br.postPage(mProt + mName + "/api.php?action=getLink&token=" + token, "link=" + url);
            if ("TOKEN_ERROR".equalsIgnoreCase(PluginJSonUtils.getJson(br, "response_code")) && "Token error, please log-in".equalsIgnoreCase(PluginJSonUtils.getJson(br, "response_text"))) {
                if (i == 2) {
                    // big problem!
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\n BIG PROBLEM_2", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                token = login(account);
                continue;
            }
            break;
        }
        if (br.containsHTML("Erreur : Probl\\\\u00e8me D\\\\u00e9brideur")) {
            logger.warning("Unknown error, disabling current host for 10 minutes!");
            tempUnavailableHoster(account, link, 10 * 60 * 1000l);
        } else if (br.containsHTML("Erreur : Lien incorrect")) {
            tempUnavailableHoster(account, link, 10 * 60 * 1000l);
        } else if (br.containsHTML("Unable to load file")) {
            logger.info("'Unable to load file'");
            int timesFailed = link.getIntegerProperty("timesfailedmegadebrideu_unabletoload", 0);
            if (timesFailed <= 2) {
                timesFailed++;
                link.setProperty("timesfailedmegadebrideu_unabletoload", timesFailed);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Server error");
            } else {
                link.setProperty("timesfailedmegadebrideu_unabletoload", Property.NULL);
                tempUnavailableHoster(account, link, 5 * 60 * 1000l);
            }
        } else if (br.containsHTML("\"debridLink\":\"cantDebridLink\"")) {
            // no idea what this means
            int timesFailed = link.getIntegerProperty("timesfailedmegadebrideu_cantDebridLink", 0);
            if (timesFailed <= 2) {
                timesFailed++;
                link.setProperty("timesfailedmegadebrideu_cantDebridLink", timesFailed);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Server error");
            } else {
                link.setProperty("timesfailedmegadebrideu_cantDebridLink", Property.NULL);
                tempUnavailableHoster(account, link, 5 * 60 * 1000l);
            }
        }
        String dllink = br.getRegex("\"debridLink\":\"(.*?)\"\\}").getMatch(0);
        if (dllink == null) {
            if (br.containsHTML("Limite de lien dépassée pour cet hébergeur")) {
                tempUnavailableHoster(account, link, 60 * 60 * 1000l);
            } else if (br.containsHTML("Limite de trafic dépassée pour cet hébergeur")) {
                tempUnavailableHoster(account, link, 10 * 60 * 1000l);
            } else if (br.containsHTML("UNALLOWED_IP")) {
                throw new AccountUnavailableException("UNALLOWED_IP", 6 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dllink = dllink.replace("\\", "").replace("\"", "");
        showMessage(link, "Phase 2/2: Download");
        int maxChunks = 0;
        if (link.getBooleanProperty(NOCHUNKS, false)) {
            maxChunks = 1;
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, true, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML(">400 Bad Request<")) {
                logger.info("Temporarily removing hoster from hostlist because of server error 400");
                tempUnavailableHoster(account, link, 10 * 60 * 1000l);
            } else if (br.containsHTML("Erreur#")) {
                logger.info("Temporarily removing hoster from hostlist because of server error");
                tempUnavailableHoster(account, link, 10 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) {
                        return;
                    }
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(MegaDebridEu.NOCHUNKS, false) == false) {
                    link.setProperty(MegaDebridEu.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            // New V2 errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && link.getBooleanProperty(MegaDebridEu.NOCHUNKS, false) == false) {
                link.setProperty(MegaDebridEu.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw e;
        }
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    private void tempUnavailableHoster(Account account, DownloadLink downloadLink, long timeout) throws PluginException {
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
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        return true;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
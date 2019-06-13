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
import java.util.LinkedHashMap;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;
import org.jdownloader.scripting.JavaScriptEngineFactory;

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
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "mega-debrid.eu" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" })
public class MegaDebridEu extends PluginForHost {
    private final String                 mName = "www.mega-debrid.eu";
    private final String                 mProt = "https://";
    private static MultiHosterManagement mhm   = new MultiHosterManagement("mega-debrid.eu");

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
            if (!"up".equals(r.get("status")) || r.get("domains") == null) {
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
        synchronized (account) {
            br.getPage(mProt + mName + "/api.php?action=connectUser&login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
            if (br.getHttpConnection().getResponseCode() == 403 || br.getHttpConnection().getResponseCode() == 404) {
                // server issue
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Host provider has server issues!", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            final String token = PluginJSonUtils.getJson(br, "token");
            final String responsecode = PluginJSonUtils.getJson(br, "response_code");
            if (!"ok".equalsIgnoreCase(responsecode)) {
                final String response_text = PluginJSonUtils.getJson(br, "response_text");
                if (!StringUtils.isEmpty(response_text)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, response_text, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            if (StringUtils.isEmpty(token)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            account.setProperty("token", token);
            return token;
        }
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        mhm.runCheck(account, link);
        String url = link.getDefaultPlugin().buildExternalDownloadURL(link, this);
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
            mhm.handleErrorGeneric(account, link, "unknown_error", 50, 1 * 60 * 1000l);
        } else if (br.containsHTML("Erreur : Lien incorrect")) {
            mhm.handleErrorGeneric(account, link, "link_incorrect", 50, 1 * 60 * 1000l);
        } else if (br.containsHTML("Unable to load file")) {
            mhm.handleErrorGeneric(account, link, "unable_to_load_file", 50, 1 * 60 * 1000l);
        } else if (br.containsHTML("\"debridLink\":\"cantDebridLink\"")) {
            mhm.handleErrorGeneric(account, link, "cant_debrid_link", 50, 1 * 60 * 1000l);
        }
        String dllink = br.getRegex("\"debridLink\":\"(.*?)\"\\}").getMatch(0);
        if (dllink == null) {
            final String responseCode = PluginJSonUtils.getJson(br, "response_code");
            if ("UNALLOWED_IP".equals(responseCode)) {
                throw new AccountUnavailableException("UNALLOWED_IP", 6 * 60 * 1000l);
            } else if (br.containsHTML("VPN, proxy ou serveur détecté\\.")) {
                throw new AccountInvalidException("VPN/Proxy/Dedicated Server Prohibitied");
            }
            mhm.handleErrorGeneric(account, link, "unknown_error_2", 50, 1 * 60 * 1000l);
        }
        dllink = dllink.replace("\\", "").replace("\"", "");
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            mhm.handleErrorGeneric(account, link, "unknown_dl_error", 50, 1 * 60 * 1000l);
        }
        this.dl.startDownload();
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
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
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
import java.util.Arrays;
import java.util.HashMap;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "mega-debrid.eu" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" }, flags = { 2 })
public class MegaDebridEu extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private final String                                   NOCHUNKS           = "NOCHUNKS";
    private final String                                   mName              = "www.mega-debrid.eu";
    private final String                                   mProt              = "http://";

    public MegaDebridEu(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(mProt + mName + "/index.php");
    }

    @Override
    public String getAGBLink() {
        return mProt + mName + "/index.php";
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ac = new AccountInfo();
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        br.setFollowRedirects(true);

        // account is valid, let's fetch account details:
        br.getPage(mProt + mName + "/api.php?action=connectUser&login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
        if (!"ok".equalsIgnoreCase(getJson("response_code"))) {
            ac.setStatus("\r\nInvalid username/password!\r\nFalscher Benutzername/Passwort!");
            logger.severe("mega-debrid.eu: Error, can not parse left days. Account: " + account.getUser() + "\r\nAPI response:\r\n\r\n" + br.toString());
            account.setValid(false);
            return ac;
        }
        account.setProperty("token", getJson("token"));
        ac.setValidUntil(-1);
        final String daysLeft = getJson("vip_end");
        if (daysLeft != null && !"0".equals(daysLeft))
            ac.setValidUntil(Long.parseLong(daysLeft) * 1000l);
        else {
            ac.setStatus("Can not determine account expire time!");
            logger.severe("Error, can not parse left days. API response:\r\n\r\n" + br.toString());
            account.setValid(false);
            return ac;
        }

        // now it's time to get all supported hosts
        br.getPage("/api.php?action=getHosters");
        if (!"ok".equalsIgnoreCase(getJson("response_code"))) {
            ac.setStatus("can not get supported hosts");
            logger.severe("Error, can not parse supported hosts. API response:\r\n\r\n" + br.toString());
            account.setValid(false);
            return ac;
        }
        String[] hosts = br.getRegex("\\[\"([a-z0-9\\.]+)\"").getColumn(0);
        ArrayList<String> supportedHosts = new ArrayList<String>(Arrays.asList(hosts));
        // workaround for uploaded.to
        if (supportedHosts.contains("uploaded.net") || supportedHosts.contains("ul.to") || supportedHosts.contains("uploaded.to")) {
            if (!supportedHosts.contains("uploaded.net")) {
                supportedHosts.add("uploaded.net");
            }
            if (!supportedHosts.contains("ul.to")) {
                supportedHosts.add("ul.to");
            }
            if (!supportedHosts.contains("uploaded.to")) {
                supportedHosts.add("uploaded.to");
            }
        }
        account.setValid(true);
        if (supportedHosts.isEmpty()) {
            ac.setStatus("Account valid: 0 Hosts via available");
        } else {
            ac.setStatus("Account valid: " + supportedHosts.size() + " Hosts available");
            ac.setProperty("multiHostSupport", supportedHosts);
        }
        return ac;
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        String url = link.getDownloadURL();
        // link corrections
        if (link.getHost().matches("ddlstorage\\.com")) {
            // needs full url!
            url += "/" + link.getName();
        } else if (link.getHost().matches("filefactory\\.com")) {
            // http://www.filefactory.com/file/asd/n/ads.rar
            if (!url.endsWith("/")) url += "/";
            url += "/n/" + link.getName();
        }
        url = Encoding.urlEncode(url);

        showMessage(link, "Phase 1/2: Generate download link");
        br.setFollowRedirects(true);
        br.postPage(mProt + mName + "/api.php?action=getLink&token=" + account.getStringProperty("token", null), "link=" + url);
        if (br.containsHTML("Erreur : Probl\\\\u00e8me D\\\\u00e9brideur")) {
            logger.warning("Unknown error, disabling current host for 3 hours!");
            tempUnavailableHoster(account, link, 3 * 60 * 60 * 1000l);
        } else if (br.containsHTML("Erreur : Lien incorrect")) {
            // link is in the wrong format, needs to be corrected as above.
            logger.warning("Hi please inform JDownloader Development Team about this issue! Link correction needs to take place.");
            tempUnavailableHoster(account, link, 3 * 60 * 60 * 1000l);
        }
        String dllink = br.getRegex("\"debridLink\":\"(.*?)\"\\}").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = dllink.replace("\\", "").replace("\"", "");
        showMessage(link, "Phase 2/2: Download");

        int maxChunks = 0;
        if (link.getBooleanProperty(NOCHUNKS, false)) {
            maxChunks = 1;
        }

        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML(">400 Bad Request<")) {
                logger.info("Temporarily removing hoster from hostlist because of server error 400");
                tempUnavailableHoster(account, link, 3 * 60 * 60 * 1000l);
            } else if (br.containsHTML("Unable to load file")) {
                logger.info("Unable to load file, Temporarily removing hoster from supported host array");
                tempUnavailableHoster(account, link, 1 * 60 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!this.dl.startDownload()) {
            try {
                if (dl.externalDownloadStop()) return;
            } catch (final Throwable e) {
            }
            final String errormessage = link.getLinkStatus().getErrorMessage();
            if (errormessage != null && (errormessage.startsWith(JDL.L("download.error.message.rangeheaders", "Server does not support chunkload")) || errormessage.equals("Unerwarteter Mehrfachverbindungsfehlernull"))) {
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(NOCHUNKS, false) == false) {
                    link.setProperty(NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                } else {
                    /* unknown error, we disable multiple chunks */
                    if (link.getBooleanProperty(NOCHUNKS, false) == false) {
                        link.setProperty(NOCHUNKS, Boolean.valueOf(true));
                        throw new PluginException(LinkStatus.ERROR_RETRY);
                    }
                }
            }
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
        if (downloadLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
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
    public boolean canHandle(DownloadLink downloadLink, Account account) {
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap != null) {
                Long lastUnavailable = unavailableMap.get(downloadLink.getHost());
                if (lastUnavailable != null && System.currentTimeMillis() < lastUnavailable) {
                    return false;
                } else if (lastUnavailable != null) {
                    unavailableMap.remove(downloadLink.getHost());
                    if (unavailableMap.size() == 0) hostUnavailableMap.remove(account);
                }
            }
        }
        return true;
    }

    private String getJson(final String parameter) {
        String result = br.getRegex("\"" + parameter + "\":(\\d+)").getMatch(0);
        if (result == null) result = br.getRegex("\"" + parameter + "\":\"([^<>\"]*?)\"").getMatch(0);
        return result;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
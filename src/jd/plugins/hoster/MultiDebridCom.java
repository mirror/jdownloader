//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "multi-debrid.com" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" }, flags = { 2 })
public class MultiDebridCom extends PluginForHost {

    // DEV NOTES
    // password is APIKey from users profile.
    // myfastfile.com sister site

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private final String                                   mName              = "multi-debrid.com";
    private final String                                   mProt              = "http://";
    private int                                            count              = 0;
    private String                                         ec                 = null;
    // repeat is one more than desired
    private final int                                      sessionRepeat      = 4;
    private final int                                      globalRepeat       = 4;
    private final String                                   sessionRetry       = "sessionRetry";
    private final String                                   globalRetry        = "globalRetry";

    public MultiDebridCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(mProt + mName + "/premium");
    }

    @Override
    public String getAGBLink() {
        return mProt + mName + "/terms";
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ac = new AccountInfo();
        br.setFollowRedirects(true);
        // account is valid, let's fetch account details:
        br.getPage(mProt + mName + "/api.php?user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));
        if (inValidStatus()) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nAccount is invalid. Wrong login details. Note: Password has to be APIKey, see Account Profile on " + mName + "website.", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        try {
            String daysLeft = getJson("days_left");
            long validuntil = System.currentTimeMillis() + (long) (Float.parseFloat(daysLeft) * 1000 * 60 * 60 * 24);
            ac.setValidUntil(validuntil);
        } catch (Exception e) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nCan not parse days_left!", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }

        // now it's time to get all supported hosts
        br.getPage("/api.php?hosts");
        if (inValidStatus()) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nCan not parse supported hosts!", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }
        String hostsArray = getJsonArray("hosts");
        String[] hosts = new Regex(hostsArray, "\"(.*?)\"").getColumn(0);
        ArrayList<String> supportedHosts = new ArrayList<String>(Arrays.asList(hosts));
        ac.setMultiHostSupport(this, supportedHosts);
        account.setValid(true);
        ac.setStatus("Premium Account");
        return ac;
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(DownloadLink link, Account account) throws Exception {
        count = link.getIntegerProperty(sessionRetry, 0) + 1;
        ec = "(" + count + "/" + sessionRepeat + ")";

        // work around
        if (link.getBooleanProperty("hasFailed", false)) {
            final int hasFailedInt = link.getIntegerProperty("hasFailedWait", 60);
            // nullify old storeables
            link.setProperty("hasFailed", Property.NULL);
            link.setProperty("hasFailedWait", Property.NULL);
            sleep(hasFailedInt * 1001, link);
        }

        // generate downloadlink:
        showMessage(link, "Phase 1/2: Generate download link");
        br.setFollowRedirects(true);
        br.getPage(mProt + mName + "/api.php?user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()) + "&link=" + Encoding.urlEncode(link.getDownloadURL()));

        if (inValidStatus() && "null".equalsIgnoreCase(getJson("link"))) {
            link.setProperty(sessionRetry, count);
            logger.info("API error, API returned NULL, no downloadlink available...temporarily disabling current host...");
            tempUnavailableHoster(account, link, 60 * 60 * 1000l);
        }

        // parse json
        if (br.containsHTML("Max atteint\\s*!")) {
            link.setProperty(sessionRetry, count);
            // max for this host reached, try again in 1h
            tempUnavailableHoster(account, link, 60 * 60 * 1000l);
        }

        if (br.containsHTML("nvalidlink")) {
            link.setProperty(sessionRetry, count);
            throw new PluginException(LinkStatus.ERROR_FATAL, "Invalid link/unsupported format");
        }

        String dllink = getJson("link");
        if (inValidate(dllink) || !dllink.matches("https?://.+|/.+")) {
            link.setProperty(sessionRetry, count);
            if (dllink.contains("Cannot debrid link")) {
                logger.severe("Can not debrid link :: " + ec);
                tempUnavailableHoster(account, link, 20 * 60 * 1000l);
            }
            String msg = "Unknown issue: " + ec;
            logger.severe(msg);
            if (count >= sessionRepeat) {
                tempUnavailableHoster(account, link, 30 * 60 * 1000l);
            } else {
                // temp unavailable will ditch to next download candidate, and retry doesn't respect wait times... !
                link.setProperty(sessionRetry, count);
                link.setProperty("hasFailed", true);
                link.setProperty("hasFailedWait", 15);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
        }
        showMessage(link, "Phase 2/2: Download...");
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, -12);
        if (dl.getConnection().getResponseCode() == 404) {
            /* file offline */
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (count >= sessionRepeat) {
                /* disable hoster for 1h */
                link.setProperty(sessionRetry, count);
                tempUnavailableHoster(account, link, 60 * 60 * 1000);
            }
            showMessage(link, "Failed for unknown reason " + ec);
            // temp unavailable will ditch to next download candidate, and retry doesn't respect wait times... !
            link.setProperty(sessionRetry, count);
            link.setProperty("hasFailed", true);
            link.setProperty("hasFailedWait", 60);
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        dl.startDownload();

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

    private boolean inValidStatus() {
        return !"ok".equalsIgnoreCase(getJson("status"));
    }

    /**
     * Tries to return value of key from JSon response, from String source.
     * 
     * @author raztoki
     * */
    private String getJson(final String source, final String key) {
        String result = new Regex(source, "\"" + key + "\":(-?\\d+(\\.\\d+)?|true|false|null)").getMatch(0);
        if (result == null) {
            result = new Regex(source, "\"" + key + "\":\"([^\"]+)\"").getMatch(0);
        }
        if (result != null) {
            result = result.replaceAll("\\\\/", "/");
        }
        return result;
    }

    /**
     * Tries to return value of key from JSon response, from default 'br' Browser.
     * 
     * @author raztoki
     * */
    private String getJson(final String key) {
        return getJson(br.toString(), key);
    }

    /**
     * Tries to return value of key from JSon response, from provided Browser.
     * 
     * @author raztoki
     * */
    private String getJson(final Browser ibr, final String key) {
        return getJson(ibr.toString(), key);
    }

    /**
     * Tries to return value given JSon Array of Key from JSon response provided String source.
     * 
     * @author raztoki
     * */
    private String getJsonArray(final String source, final String key) {
        String result = new Regex(source, "\"" + key + "\":(\\[[^\\]]+\\])").getMatch(0);
        if (result != null) {
            result = result.replaceAll("\\\\/", "/");
        }
        return result;
    }

    /**
     * Tries to return value given JSon Array of Key from JSon response, from default 'br' Browser.
     * 
     * @author raztoki
     * */
    private String getJsonArray(final String key) {
        return getJsonArray(br.toString(), key);
    }

    /**
     * Validates string to series of conditions, null, whitespace, or "". This saves effort factor within if/for/while statements
     * 
     * @param s
     *            Imported String to match against.
     * @return <b>true</b> on valid rule match. <b>false</b> on invalid rule match.
     * @author raztoki
     * */
    protected boolean inValidate(final String s) {
        if (s == null || s.matches("[\r\n\t ]+") || s.equals("")) {
            return true;
        } else {
            return false;
        }
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
        // we reset session retry and int global here to allow for multiple retries, with tempUnailable inbetween
        downloadLink.setProperty(sessionRetry, Property.NULL);
        final int gr = downloadLink.getIntegerProperty(globalRetry, 0) + 1;
        downloadLink.setProperty(globalRetry, gr);
        if (gr >= globalRepeat) {
            // prevent more than globalRepeat retries.
            throw new PluginException(LinkStatus.ERROR_FATAL, "Exausted Global Retry!");
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
                    if (unavailableMap.size() == 0) {
                        hostUnavailableMap.remove(account);
                    }
                }
            }
        }
        return true;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        link.setProperty(sessionRetry, Property.NULL);
        link.setProperty(globalRetry, Property.NULL);
    }

}
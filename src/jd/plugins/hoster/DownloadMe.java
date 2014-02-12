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
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.Property;
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

import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "download.me" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" }, flags = { 2 })
public class DownloadMe extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private static AtomicInteger                           maxPrem            = new AtomicInteger(20);
    private static final String                            NOCHUNKS           = "NOCHUNKS";
    private static final String                            FAIL_STRING        = "downloadme";

    public DownloadMe(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.download.me/");
    }

    @Override
    public String getAGBLink() {
        return "http://www.download.me/terms";
    }

    @Override
    public int getMaxSimultanDownload(final DownloadLink link, final Account account) {
        return maxPrem.get();
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ac = new AccountInfo();
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        String username = Encoding.urlEncode(account.getUser());
        String pass = Encoding.urlEncode(account.getPass());
        String hosts[] = null;
        ac.setProperty("multiHostSupport", Property.NULL);
        // check if account is valid
        br.getPage("https://www.download.me/dlapi/user?mail=" + username + "&passwd=" + pass);
        final String userCookie = br.getCookie("http://download.me/", "user");
        final String lang = System.getProperty("user.language");
        // "Invalid login" / "Banned" / "Valid login"
        final String status = getJson("status");
        if ("0".equals(status)) {
            account.setValid(false);
            if ("de".equalsIgnoreCase(lang)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        } else if ("1".equals(status)) {
            account.setValid(true);
        } else if (userCookie == null) {
            // unknown error 1
            ac.setStatus("Cookie error, unknown account status");
            account.setValid(false);
            return ac;
        } else {
            // unknown error 2
            ac.setStatus("Unknown account status");
            account.setValid(false);
            return ac;
        }
        account.setProperty("usercookie", userCookie);
        final String expire = getJson("premuntil");
        ac.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MM-dd hh:mm:ss", Locale.ENGLISH));
        try {
            account.setMaxSimultanDownloads(maxPrem.get());
            account.setConcurrentUsePossible(true);
        } catch (final Throwable e) {
            // not available in old Stable 0.9.581
        }
        ac.setTrafficLeft(Long.parseLong(getJson("trafficleft")));
        ac.setStatus("Premium User");
        // now let's get a list of all supported hosts:
        br.getPage("https://www.download.me/dlapi/hosters");
        final String hostArrayText = br.getRegex("\"data\":\\[(.*?)\\]").getMatch(0);
        if (hostArrayText != null) hosts = new Regex(hostArrayText, "\"([^<>\"/]*?)\"").getColumn(0);
        ArrayList<String> supportedHosts = new ArrayList<String>();
        for (String host : hosts) {
            if (!host.isEmpty()) {
                supportedHosts.add(host.trim());
            }
        }

        if (supportedHosts.size() == 0) {
            ac.setStatus("Account valid: 0 Hosts via download.me available");
        } else {
            ac.setStatus("Account valid: " + supportedHosts.size() + " Hosts via download.me available");
            ac.setProperty("multiHostSupport", supportedHosts);
        }
        return ac;
    }

    private String getJson(final String parameter) {
        String result = br.getRegex("\"" + parameter + "\":(\\d+)").getMatch(0);
        if (result == null) result = br.getRegex("\"" + parameter + "\":\"([^<>\"]*?)\"").getMatch(0);
        return result;
    }

    private String getJson(final String parameter, final String source) {
        String result = new Regex(source, "\"" + parameter + "\":(\\d+)").getMatch(0);
        if (result == null) result = new Regex(source, "\"" + parameter + "\":\"([^<>\"]*?)\"").getMatch(0);
        return result;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(final DownloadLink link, final Account acc) throws Exception {
        br.setCookie("http://download.me/", "user", acc.getStringProperty("usercookie", null));
        final String url = Encoding.urlEncode(link.getDownloadURL());
        int maxChunks = 0;
        if (link.getBooleanProperty(DownloadMe.NOCHUNKS, false)) {
            maxChunks = 1;
        }
        showMessage(link, "Phase 1/3: Generating id");
        br.getPage("https://www.download.me/dlapi/file?url=" + url);
        final String id = getJson("id");
        // Should never happen
        if (id == null) {
            logger.info(this.getHost() + ": id is null");
            int timesFailed = link.getIntegerProperty("timesfailed" + FAIL_STRING + "_idnull", 1);
            link.getLinkStatus().setRetryCount(0);
            if (timesFailed <= 20) {
                timesFailed++;
                link.setProperty("timesfailed" + FAIL_STRING + "_idnull", timesFailed);
                throw new PluginException(LinkStatus.ERROR_RETRY, "id is null");
            } else {
                link.setProperty("timesfailed" + FAIL_STRING + "_idnull", Property.NULL);
                logger.info(this.getHost() + ": id is null");
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error: id is null", 5 * 60 * 1000l);
            }
        }

        String dllink = null;
        long lastProgress = 0;
        long currentProgress = 0;
        long filesize = 0;
        long lastProgressChange = System.currentTimeMillis();
        // Try to get downloadlink for up to 10 minutes
        int retryLimit = 5;
        for (int i = 0; i <= retryLimit; i++) {
            showMessage(link, "Phase 2/3: Generating downloadlink");
            logger.info("Trying to find link, try " + i + " / " + retryLimit);
            br.getPage("https://www.download.me/dlapi/file?id=" + id);
            final String data = br.getRegex("\"data\":\\{(.*?)\\}\\}").getMatch(0);
            // Account expired
            if ("1001".equals(getJson("errcode", data))) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            // No traffic left
            if ("1006".equals(getJson("errcode", data))) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            final String status = getJson("status", data);
            if ("0".equals(status)) {
                logger.info("Received status 0, link generation failed!");
                if (link.getLinkStatus().getRetryCount() == 2) throw new PluginException(LinkStatus.ERROR_FATAL, "Downloadlink generation failed");
                if (i <= retryLimit) throw new PluginException(LinkStatus.ERROR_RETRY, "Downloadlink generation failed");
                i++;
                sleep(10000, link, "Retry on Generating Link (" + i + " / " + retryLimit);
                continue;
            }

            // Try to detect if download is stuck serverside
            String prgr = getJson("progress");
            if (prgr.equals("")) prgr = "0";
            final String size = getJson("size");
            if (prgr != null && size != null) {
                currentProgress = Long.parseLong(prgr);
                filesize = Long.parseLong(getJson("size"));
                if (currentProgress == lastProgress && ((System.currentTimeMillis() - lastProgressChange) >= 60000)) {
                    logger.info("Download seems to be stuck on the download.me server, aborting...");
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error, download stuck on download.me servers", 10 * 60 * 1000l);
                } else if (currentProgress > lastProgress) {
                    lastProgress = currentProgress;
                    lastProgressChange = System.currentTimeMillis();
                }
                if (filesize > 0 && currentProgress == filesize) {
                    logger.info("File successfully transfered to the download.me servers, download should start soon...");
                    break;
                }
            }

            dllink = getJson("dlurl");
            if (dllink != null) {
                logger.info("File successfully transfered to the download.me servers, download should start soon...");
                break;
            }
            this.sleep(5000l, link);
        }

        if (dllink == null) {
            logger.warning("Unhandled download error on download.me:");
            logger.warning(br.toString());
            int timesFailed = link.getIntegerProperty("timesfailed" + FAIL_STRING + "_dlunknown", 1);
            link.getLinkStatus().setRetryCount(0);
            if (timesFailed <= 20) {
                timesFailed++;
                link.setProperty("timesfailed" + FAIL_STRING + "_dlunknown", timesFailed);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown download error");
            } else {
                link.setProperty("timesfailed" + FAIL_STRING + "_dlunknown", Property.NULL);
                logger.info(this.getHost() + ": Unknown download error -> Disabling current host");
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error: id is null", 5 * 60 * 1000l);
            }
        }
        dllink = dllink.replace("\\", "");
        showMessage(link, "Phase 3/3: Download begins!");

        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            logger.info("Unhandled download error on download.me: " + br.toString());
            tempUnavailableHoster(acc, link, 1 * 60 * 60 * 1000l);
        }
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) return;
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(DownloadMe.NOCHUNKS, false) == false) {
                    link.setProperty(DownloadMe.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            // New V2 errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && link.getBooleanProperty(DownloadMe.NOCHUNKS, false) == false) {
                link.setProperty(DownloadMe.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
        }
    }

    private void showMessage(final DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    private void tempUnavailableHoster(final Account account, final DownloadLink downloadLink, final long timeout) throws PluginException {
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
    public boolean canHandle(final DownloadLink downloadLink, final Account account) {
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

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}
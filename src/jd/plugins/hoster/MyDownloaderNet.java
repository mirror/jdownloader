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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "mydownloader.net" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" }, flags = { 2 })
public class MyDownloaderNet extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();

    public MyDownloaderNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://mydownloader.net/buy_credits.php");
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ac = new AccountInfo();
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);

        String page = null;
        String token = null;

        try {
            // login
            token = getLoginToken(account);
        } catch (final Exception e) {
            account.setValid(false);
            ac.setProperty("multiHostSupport", Property.NULL);
            ac.setStatus("\r\nCan't get login token. Wrong password?");
            return ac;
        }
        // get account info:
        try {
            page = br.getPage("http://api.mydownloader.net/api.php?task=user_info&auth=" + token);
        } catch (Exception e) {
            account.setValid(false);
            ac.setProperty("multiHostSupport", Property.NULL);
            ac.setStatus("Mydownloader.net server error.");
            return ac;
        }
        long traffic = (long) (Float.parseFloat(getXMLTag(page, "remaining_limit_mb").getMatch(0)) + 0.5f);
        ac.setTrafficLeft(traffic * 1024 * 1024l);
        String expire = getXMLTag(page, "experation_date").getMatch(0);
        if (!expire.equals("lifetime")) {
            if (expire.equalsIgnoreCase("trial")) {
                ac.setStatus("Trial");
                ac.setValidUntil(-1);
            } else if (expire.equalsIgnoreCase("expired")) {
                ac.setExpired(true);
                ac.setStatus("\r\nAccont expired!\r\nAccount abgelaufen!");
                /* Workaround for bug: http://svn.jdownloader.org/issues/11637 */
                final String lang = System.getProperty("user.language");
                if ("de".equalsIgnoreCase(lang)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nAccount abgelaufen!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nAccount expired!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            } else {
                ac.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MM-dd", Locale.ENGLISH));
            }
        }
        // get supported hoster
        page = br.getPage("http://api.mydownloader.net/api.php?task=supported_list&auth=" + token);
        String[] hosters = getXMLTag(page, "values").getColumn(0);
        if (hosters == null) {
            account.setValid(false);
            ac.setProperty("multiHostSupport", Property.NULL);
            return ac;
        }
        ArrayList<String> supportedHosts = new ArrayList<String>();
        for (String host : hosters) {
            supportedHosts.add(host.trim());
        }
        ac.setStatus("Account valid");
        ac.setProperty("multiHostSupport", supportedHosts);
        return ac;
    }

    @Override
    public String getAGBLink() {
        return "http://mydownloader.net/privacy.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(DownloadLink link, Account acc) throws Exception {
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        showMessage(link, "Phase 1/4: Login");
        String auth = getLoginToken(acc);
        if (auth.isEmpty()) {
            /*
             * after x retries we disable this host and retry with normal plugin
             */
            if (link.getLinkStatus().getRetryCount() >= 3) {
                try {
                    // disable hoster for 30min
                    tempUnavailableHoster(acc, link, 30 * 60 * 1000l);
                } catch (Exception e) {
                }
                /* reset retrycounter */
                link.getLinkStatus().setRetryCount(0);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            }
            String msg = "(" + link.getLinkStatus().getRetryCount() + 1 + "/" + 3 + ")";

            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Retry in few secs" + msg, 20 * 1000l);
        }

        String url = Encoding.urlEncode(link.getDownloadURL());

        showMessage(link, "Phase 2/4: Add Link");
        String page = br.getPage("http://api.mydownloader.net/api.php?task=add_url&auth=" + auth + "&url=" + url);
        if (getXMLTag(page, "url_error").getMatch(0) != null && !getXMLTag(page, "url_error").getMatch(0).isEmpty()) {
            // file already added
            // tempUnavailableHoster(acc, link, 10 * 60 * 1000l);
        } else if (!getXMLTag(page, "fid").getMatch(0).isEmpty()) {
            url = getXMLTag(page, "fid").getMatch(0).trim(); // we can use fileid instead url if given
        }
        sleep(2 * 1000l, link);

        int maxCount = 10;
        String genlink = "";
        String status = "";
        // wait until mydownloader has loaded file on their server
        while (genlink.isEmpty() && (maxCount > 0)) {
            page = br.getPage("http://api.mydownloader.net/api.php?task=file_info&auth=" + auth + "&file=" + url);
            status = getXMLTag(page, "status").getMatch(0);
            if (status.equalsIgnoreCase("new")) {
                // File was just added to our system
                // or
                sleep(10 * 1000l, link, "Phase 3/4: File was just added to mydownloader system");
            } else if (status.equalsIgnoreCase("download")) {
                // File uploading to our servers was started
                sleep(10 * 1000l, link, "Phase 3/4: File uploading to mydownloader servers was started");
            } else if (status.equalsIgnoreCase("turn")) {
                // File in queue of uploading
                sleep(15 * 1000l, link, "Phase 3/4: File in queue of uploading to mydownloader");
            } else if (status.equalsIgnoreCase("mturn")) {
                // File in user type of queue, it happens when user has exceeded daily limit, file stands in user type of queue and
                // automatically starts when new limit will be added
                // disable for 1h
                tempUnavailableHoster(acc, link, 60 * 60 * 1000l);
            } else if (status.equalsIgnoreCase("download_ok") || status.equalsIgnoreCase("ok")) {
                // File is uploading to our servers and you can start downloading to your computer.
                // or: File is fully uploaded to our servers
                genlink = getXMLTag(page, "download_link").getMatch(0).trim();
                if (genlink.isEmpty()) {
                    if (link.getLinkStatus().getRetryCount() >= 3) {
                        try {
                            // disable hoster for 30min
                            tempUnavailableHoster(acc, link, 30 * 60 * 1000l);
                        } catch (Exception e) {
                        }
                        /* reset retrycounter */
                        link.getLinkStatus().setRetryCount(0);
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                    }
                    String msg = "(" + link.getLinkStatus().getRetryCount() + 1 + "/" + 3 + ")";
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Retry in few secs" + msg, 20 * 1000l);
                }
                genlink = "http://" + genlink;
            } else if (status.equalsIgnoreCase("received")) {
                // File is downloading by user to his computer
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            } else if (status.equalsIgnoreCase("received_ok")) {
                // User file downloading completed
                // delete file and retry
                br.getPage("http://api.mydownloader.net/api.php?task=delete_file&auth=" + auth + "&file=" + url);
                throw new PluginException(LinkStatus.ERROR_RETRY, "File need to be deleted from mydownloader system first.", 1000l);

            } else if (status.equalsIgnoreCase("del")) {
                // File was deleted from our servers
                if (link.getLinkStatus().getRetryCount() >= 3) {
                    try {
                        // disable hoster for 30min
                        tempUnavailableHoster(acc, link, 30 * 60 * 1000l);
                    } catch (Exception e) {
                    }
                    /* reset retrycounter */
                    link.getLinkStatus().setRetryCount(0);
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                }
                String msg = "(" + link.getLinkStatus().getRetryCount() + 1 + "/" + 3 + ")";
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Retry in few secs" + msg, 20 * 1000l);
            } else if (status.equalsIgnoreCase("err")) {
                // Error while file downloading
                if (link.getLinkStatus().getRetryCount() >= 3) {
                    try {
                        // disable hoster for 30min
                        tempUnavailableHoster(acc, link, 30 * 60 * 1000l);
                    } catch (Exception e) {
                    }
                    /* reset retrycounter */
                    link.getLinkStatus().setRetryCount(0);
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                }
                String msg = "(" + link.getLinkStatus().getRetryCount() + 1 + "/" + 3 + ")";
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Retry in few secs" + msg, 20 * 1000l);
            } else if (status.equalsIgnoreCase("efo")) {
                showMessage(link, "Error: " + getXMLTag(page, "error").getMatch(0));
                tempUnavailableHoster(acc, link, 5 * 60 * 1000l);
            }
            maxCount--;
        }

        dl = jd.plugins.BrowserAdapter.openDownload(br, link, genlink, true, 0);
        if (dl.getConnection().getResponseCode() == 404) {
            /* file offline */
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (!dl.getConnection().isContentDisposition()) {
            /* unknown error */
            br.followConnection();
            logger.severe("MyDownloader(Error): " + br.toString());
            // disable hoster for 5min
            tempUnavailableHoster(acc, link, 5 * 60 * 1000l);
        }
        showMessage(link, "Phase 4/4: Begin download");
        dl.startDownload();
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

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    private String getLoginToken(Account ac) throws IOException {
        String user = Encoding.urlEncode(ac.getUser());
        String pw = Encoding.urlEncode(ac.getPass());
        String token = "";
        String page = br.getPage("http://api.mydownloader.net/api.php?task=auth&login=" + user + "&password=" + pw);
        token = getXMLTag(page, "auth").getMatch(0);
        if (token.length() == 0) {
            String errormsg = getXMLTag(page, "error").getMatch(0);
            if (errormsg.equals("LOGIN_ERROR")) {
                throw new RuntimeException("Error: Wrong mydownloader.net password.");
            } else {
                throw new RuntimeException("Error: Unknown mydownloader login error.");
            }
        }
        return token;
    }

    private Regex getXMLTag(String xml, String tag) {
        return new Regex(xml, "<" + tag + ">([^<]*)</" + tag + ">");
    }

}
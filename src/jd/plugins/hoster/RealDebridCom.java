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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
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

import org.appwork.utils.Hash;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "real-debrid.com" }, urls = { "http://\\w+\\.real\\-debrid\\.com/dl/\\w+/.+" }, flags = { 2 })
public class RealDebridCom extends PluginForHost {

    // DEV NOTES
    // supports last09 based on pre-generated links and jd2

    private static final String mName    = "real-debrid.com";
    private static final String mProt    = "http://";
    private static Object LOCK     = new Object();
    private static final String DIRECTRD = "directRD";

    public RealDebridCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(mProt + mName + "/");
    }

    @Override
    public String getAGBLink() {
        return mProt + mName + "/terms";
    }

    public void prepBrowser() {
        // define custom browser headers and language settings.
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9, de;q=0.8");
        br.setCookie(mProt + mName, "lang", "english");
        br.getHeaders().put("Agent", "JDOWNLOADER");
        br.setCustomCharset("utf-8");
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink dl) throws PluginException, IOException {
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(dl.getDownloadURL());
            if (con.isContentDisposition()) {
                dl.setProperty(DIRECTRD, true);
                if (dl.getFinalFileName() == null) dl.setFinalFileName(getFileNameFromHeader(con));
                dl.setVerifiedFileSize(con.getLongContentLength());
                dl.setAvailable(true);
                return AvailableStatus.TRUE;
            } else {
                dl.setProperty("directRD", false);
                dl.setAvailable(false);
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } finally {
            try {
                /* make sure we close connection */
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) {
        if (downloadLink.getBooleanProperty(DIRECTRD, false) == true) {
            /* direct link */
            return true;
        }
        if (account == null) {
            /* without account its not possible to download the link */
            return false;
        }
        return true;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        handleDL(downloadLink, downloadLink.getDownloadURL());
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        login(account, false);
        showMessage(link, "Task 1: Check URL validity!");
        requestFileInformation(link);
        showMessage(link, "Task 2: Download begins!");
        handleDL(link, link.getDownloadURL());
    }

    private void handleDL(DownloadLink link, String dllink) throws Exception {
        /* we want to follow redirects in final stage */
        if (dllink.startsWith("https")) {
            dllink = dllink.replace("https://", "http://");
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().isContentDisposition()) {
            /* contentdisposition, lets download it */
            dl.startDownload();
            return;
        } else {
            /*
             * download is not contentdisposition, so remove this host from premiumHosts list
             */
            br.followConnection();
        }

        /* temp disabled the host */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private void removeHostFromMultiHost(DownloadLink link, Account acc) throws PluginException {
        Object supportedHosts = acc.getAccountInfo().getProperty("multiHostSupport", null);
        if (supportedHosts != null && supportedHosts instanceof List) {
            ArrayList<String> newList = new ArrayList<String>((List<String>) supportedHosts);
            newList.remove(link.getHost());
            acc.getAccountInfo().setProperty("multiHostSupport", newList);
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(DownloadLink link, Account acc) throws Exception {
        prepBrowser();
        login(acc, false);
        showMessage(link, "Task 1: Generating Link");
        /* request Download */
        if (link.getStringProperty("pass", null) != null) {
            br.getPage(mProt + mName + "/ajax/unrestrict.php?link=" + Encoding.urlEncode(link.getDownloadURL()) + "&password=" + Encoding.urlEncode(link.getStringProperty("pass", null)));
        } else {
            br.getPage(mProt + mName + "/ajax/unrestrict.php?link=" + Encoding.urlEncode(link.getDownloadURL()));
        }
        // handleErrors();
        String generatedLinks = br.getRegex("\"generated_links\":\\[\\[(.*?)\\]\\]").getMatch(0);
        String dlLinks[] = new Regex(generatedLinks, "\"([^\"]*?)\"").getColumn(0);
        if (dlLinks == null || dlLinks.length == 0) {
            if (br.containsHTML("error\":9")) {
                logger.info("Host is currently not possible because no server is available!");
                removeHostFromMultiHost(link, acc);
            }
            if (br.containsHTML("error\":11")) {
                logger.info("Host seems buggy, remove it from list");
                removeHostFromMultiHost(link, acc);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        showMessage(link, "Task 2: Download begins!");
        int counter = 0;
        for (String dllink : dlLinks) {
            counter++;
            if (StringUtils.isEmpty(dllink) || !dllink.startsWith("http")) continue;
            dllink = dllink.replaceAll("\\\\/", "/");
            try {
                handleDL(link, dllink);
                return;
            } catch (PluginException e1) {
                try {
                    dl.getConnection().disconnect();
                } catch (final Throwable e) {
                }
                if (br.containsHTML("An error occured while generating a premium link, please contact an Administrator")) {
                    logger.info("Error while generating premium link, remove it from list");
                    removeHostFromMultiHost(link, acc);
                }
                if (br.containsHTML("An error occured while attempting to download the file. Multiple")) {
                    if (counter == dlLinks.length) { throw new PluginException(LinkStatus.ERROR_RETRY); }
                }
            }
        }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            ai.setProperty("multiHostSupport", Property.NULL);
            return ai;
        }
        account.setValid(true);
        account.setConcurrentUsePossible(true);
        account.setMaxSimultanDownloads(-1);
        br.getPage(mProt + mName + "/api/account.php");
        String expire = br.getRegex("(?i)<expiration\\-txt>([^<]+)").getMatch(0);
        if (expire != null) {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd/MM/yyyy hh:mm:ss", null));
        }
        String acctype = br.getRegex("(?i)<type>(\\w+)</type>").getMatch(0).toLowerCase();
        if (acctype.equals("premium")) {
            ai.setStatus("Premium User");
        } else {
            // unhandled account type here.
        }
        try {
            String hostsSup = br.cloneBrowser().getPage(mProt + mName + "/api/hosters.php");
            String[] hosts = new Regex(hostsSup, "\"([^\"]+)\",").getColumn(0);
            ArrayList<String> supportedHosts = new ArrayList<String>(Arrays.asList(hosts));
            /*
             * set ArrayList<String> with all supported multiHosts of this service
             */
            ai.setProperty("multiHostSupport", supportedHosts);
        } catch (Throwable e) {
            account.setProperty("multiHostSupport", Property.NULL);
            logger.info("Could not fetch ServerList from Multishare: " + e.toString());
        }

        return ai;
    }

    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /** Load cookies */
                br.setCookiesExclusive(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie(mProt + mName, key, value);
                        }
                        return;
                    }
                }
                prepBrowser();
                br.getPage(mProt + mName + "/ajax/login.php?user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Hash.getMD5(account.getPass()) + "&captcha_challenge=&captcha_answer=&time=" + System.currentTimeMillis());
                if (br.getCookie(mProt + mName, "auth") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                /** Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(mProt + mName);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
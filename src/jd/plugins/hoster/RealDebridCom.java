//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
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
import org.appwork.utils.logging2.LogSource;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "real-debrid.com" }, urls = { "https?://\\w+\\.real\\-debrid\\.com/dl/\\w+/.+" }, flags = { 2 })
public class RealDebridCom extends PluginForHost {

    // DEV NOTES
    // supports last09 based on pre-generated links and jd2 (but disabled with interfaceVersion 3)

    private final String         mName             = "real-debrid.com";
    private final String         mProt             = "https://";
    private static Object        LOCK              = new Object();
    private static AtomicInteger RUNNING_DOWNLOADS = new AtomicInteger(0);
    private static AtomicInteger MAX_DOWNLOADS     = new AtomicInteger(Integer.MAX_VALUE);

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
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        br.setCookie(mProt + mName, "lang", "english");
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setCustomCharset("utf-8");
        br.setConnectTimeout(2 * 60 * 1000);
        br.setReadTimeout(2 * 60 * 1000);
        br.setFollowRedirects(true);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink dl) throws PluginException, IOException {
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(dl.getDownloadURL());
            if (con.isContentDisposition()) {
                dl.setProperty("directRD", true);
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
        if (downloadLink.getBooleanProperty("directRD", false) == true) {
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
        return MAX_DOWNLOADS.get();
    }

    @Override
    public int getMaxSimultanDownload(DownloadLink link, final Account account) {
        return MAX_DOWNLOADS.get();
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
        // real debrid connections are flakey at times! Do this instead of repeating download steps.
        int repeat = 3;
        for (int i = 0; i <= repeat; i++) {
            Browser br2 = br.cloneBrowser();
            try {
                dl = jd.plugins.BrowserAdapter.openDownload(br2, link, dllink, true, 0);
                if (dl.getConnection().isContentDisposition()) {
                    /* content disposition, lets download it */
                    RUNNING_DOWNLOADS.incrementAndGet();
                    dl.startDownload();
                    if (link.getLinkStatus().isFinished()) {
                        // download is 100%
                        break;
                    }
                    if (link.getLinkStatus().getErrorMessage().contains("Unexpected rangeheader format:")) {
                        logger.warning("Bad Range Header! Resuming isn't possible without resetting");
                        new PluginException(LinkStatus.ERROR_FATAL);
                        break;
                        // logger.warning("BAD HEADER RANGES!, auto resetting");
                        // link.reset();
                    }
                } else if (dl.getConnection().getResponseCode() == 404) {
                    // unhandled error!
                    break;
                } else {
                    /* download is not content disposition. */
                    br2.followConnection();
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            } catch (Throwable e) {
                if (e instanceof PluginException) throw (PluginException) e;
                if (e instanceof InterruptedException) throw (InterruptedException) e;
                sleep(3000, link);
                LogSource.exception(logger, e);
                continue;
            } finally {
                try {
                    dl.getConnection().disconnect();
                } catch (final Throwable e) {
                }
                if (RUNNING_DOWNLOADS.decrementAndGet() == 0) {
                    MAX_DOWNLOADS.set(Integer.MAX_VALUE);
                }
            }
        }
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
        String dllink = link.getDownloadURL();
        for (int retry = 0; retry < 3; retry++) {
            try {
                if (link.getStringProperty("pass", null) != null) {
                    br.getPage(mProt + mName + "/ajax/unrestrict.php?link=" + Encoding.urlEncode(dllink) + "&password=" + Encoding.urlEncode(link.getStringProperty("pass", null)));
                } else {
                    br.getPage(mProt + mName + "/ajax/unrestrict.php?link=" + Encoding.urlEncode(dllink));
                }
                break;
            } catch (SocketException e) {
                if (retry == 2) throw e;
                sleep(3000l, link);
            }
        }
        if (br.containsHTML("\"error\":4,")) {
            if (dllink.contains("https://")) {
                dllink = dllink.replace("https://", "http://");
            } else {
                // not likely but lets try anyway.
                dllink = dllink.replace("http://", "https://");
            }
            for (int retry = 0; retry < 3; retry++) {
                try {
                    if (link.getStringProperty("pass", null) != null) {
                        br.getPage(mProt + mName + "/ajax/unrestrict.php?link=" + Encoding.urlEncode(dllink) + "&password=" + Encoding.urlEncode(link.getStringProperty("pass", null)));
                    } else {
                        br.getPage(mProt + mName + "/ajax/unrestrict.php?link=" + Encoding.urlEncode(dllink));
                    }
                    break;
                } catch (SocketException e) {
                    if (retry == 2) throw e;
                    sleep(3000l, link);
                }
            }
            if (br.containsHTML("\"error\":4,")) {
                logger.warning("Problemo in the old corral");
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Can not download from " + mName);
            }
        }
        String generatedLinks = br.getRegex("\"generated_links\":\\[\\[(.*?)\\]\\]").getMatch(0);
        String genLnks[] = new Regex(generatedLinks, "\"([^\"]*?)\"").getColumn(0);
        if (genLnks == null || genLnks.length == 0) {
            if (br.containsHTML("\"error\":1,")) {
                // from rd
                // 1: Happy hours activated BUT the concerned hoster is not included => Upgrade to Premium to use it
                logger.info("This Hoster isn't supported in Happy Hour!");
                removeHostFromMultiHost(link, acc);
            } else if (br.containsHTML("\"error\":2,")) {
                // from rd
                // 2: Free account, come back at happy hours
                logger.info("It's not happy hour, free account, you need premium!.");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            } else if (br.containsHTML("\"error\":3,")) {
                // {"error":3,"message":"Ein dedicated Server wurde erkannt, es ist dir nicht erlaubt Links zu generieren"}
                // dedicated server is detected, it does not allow you to generate links
                logger.info("Dedicated server detected, account disabled");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else if (br.containsHTML("\"error\":5,")) {
                /* no happy hour */
                logger.info("It's not happy hour, free account, you need premium!.");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            } else if (br.containsHTML("error\":6,")) {
                // {"error":6,"message":"Daily limit exceeded."}
                logger.info("You have run out of download quota for this hoster");
            } else if (br.containsHTML("error\":10,")) {
                logger.info("File's hoster is in maintenance. Try again later");
                removeHostFromMultiHost(link, acc);
            } else if (br.containsHTML("error\":11,")) {
                logger.info("Host seems buggy, remove it from list");
                removeHostFromMultiHost(link, acc);
            } else if (br.containsHTML("error\":12,")) {
                /* You have too many simultaneous downloads */
                MAX_DOWNLOADS.set(RUNNING_DOWNLOADS.get());
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Error 12: You have too many simultaneous downloads", 20 * 1000l);
            } else if (br.containsHTML("error\":(13|9),")) {
                String num = "";
                if (br.containsHTML("error\":13,")) {
                    num = "13";
                    logger.info("Unknown error " + num);
                }
                // doesn't warrant not retrying! it just means no available host at this point in time! ??
                if (br.containsHTML("error\":9,")) {
                    num = "9";
                    logger.info("Host is currently not possible because no server is available!");
                }

                /*
                 * after x retries we disable this host and retry with normal plugin
                 */
                if (link.getLinkStatus().getRetryCount() == 3) {
                    removeHostFromMultiHost(link, acc);
                    /* reset retrycounter */
                    link.getLinkStatus().setRetryCount(0);
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                String msg = (link.getLinkStatus().getRetryCount() + 1) + " / 3";
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Error " + num + " : Retry " + msg, 120 * 1000l);
            } else {
                // unknown error
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        showMessage(link, "Task 2: Download begins!");
        int counter = 0;
        for (String generatedLink : genLnks) {
            counter++;
            if (StringUtils.isEmpty(generatedLink) || !generatedLink.startsWith("http")) continue;
            generatedLink = generatedLink.replaceAll("\\\\/", "/");
            try {
                handleDL(link, generatedLink);
                return;
            } catch (PluginException e1) {
                try {
                    dl.getConnection().disconnect();
                } catch (final Throwable e) {
                }
                if (br.containsHTML("An error occured while generating a premium link, please contact an Administrator")) {
                    logger.info("Error while generating premium link, removing host from supported list");
                    removeHostFromMultiHost(link, acc);
                }
                if (br.containsHTML("An error occured while attempting to download the file.")) {
                    if (counter == genLnks.length) { throw new PluginException(LinkStatus.ERROR_RETRY); }
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
        for (int retry = 0; retry < 3; retry++) {
            try {
                br.getPage(mProt + mName + "/api/account.php");
                break;
            } catch (SocketException e) {
                if (retry == 2) throw e;
                Thread.sleep(1000);
            }
        }
        String expire = br.getRegex("<expiration\\-txt>([^<]+)").getMatch(0);
        if (expire != null) {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd/MM/yyyy hh:mm:ss", null));
        }
        String acctype = br.getRegex("<type>(\\w+)</type>").getMatch(0).toLowerCase();
        if (acctype.equals("premium")) {
            ai.setStatus("Premium User");
        } else {
            // non supported account type here.
            logger.warning("Sorry we do not support this account type at this stage.");
            account.setValid(false);
            ai.setProperty("multiHostSupport", Property.NULL);
            return ai;
        }
        if ("01/01/1970 01:00:00".equals(expire)) {
            ai.setValidUntil(-1);
            ai.setStatus("Free User");
        }
        try {
            String hostsSup = null;
            for (int retry = 0; retry < 3; retry++) {
                try {
                    hostsSup = br.cloneBrowser().getPage(mProt + mName + "/api/hosters.php");
                    break;
                } catch (SocketException e) {
                    if (retry == 2) throw e;
                    Thread.sleep(1000);
                }
            }
            String[] hosts = new Regex(hostsSup, "\"([^\"]+)\"").getColumn(0);
            ArrayList<String> supportedHosts = new ArrayList<String>(Arrays.asList(hosts));
            // remove youtube support from this multihoster. Our youtube plugin works from cdn/cached final links and this does not work
            // with multihosters as it has geolocation issues. To over come this we need to pass the watch link and not decrypted finallink
            // results...
            supportedHosts.remove("youtube.com");
            // and so does vimeo
            supportedHosts.remove("vimeo.com");
            // supportedHosts.remove("netload.in");
            if (supportedHosts.contains("freakshare.net")) {
                supportedHosts.add("freakshare.com");
            }
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
            ai.setProperty("multiHostSupport", supportedHosts);
        } catch (Throwable e) {
            account.setProperty("multiHostSupport", Property.NULL);
            logger.info("Could not fetch ServerList: " + e.toString());
        }
        return ai;
    }

    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /** Load cookies */
                br.setCookiesExclusive(true);
                prepBrowser();
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
                for (int retry = 0; retry < 3; retry++) {
                    try {
                        br.getPage(mProt + mName + "/ajax/login.php?user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Hash.getMD5(account.getPass()) + "&captcha_challenge=&captcha_answer=&time=" + System.currentTimeMillis());
                        break;
                    } catch (SocketException e) {
                        if (retry == 2) throw e;
                        Thread.sleep(1000);
                    }
                }
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

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return false;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return false;
        }
        return false;
    }

}
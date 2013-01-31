//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filebox.com" }, urls = { "https?://(www\\.)?filebox\\.com/(vidembed\\-)?[a-z0-9]{12}" }, flags = { 2 })
public class FileBoxCom extends PluginForHost {

    private String              correctedBR         = "";
    private static final String PASSWORDTEXT        = "(<br><b>Password:</b> <input|<br><b>Passwort:</b> <input)";
    private static final String COOKIE_HOST         = "http://filebox.com";
    private static final String MAINTENANCE         = ">This server is in maintenance mode|>Please refresh this page in some minutes";
    private static final String MAINTENANCEUSERTEXT = "This server is under Maintenance";
    private static final String ALLWAIT_SHORT       = "Waiting till new downloads can be started";
    private static Object       LOCK                = new Object();
    private String              LINKID              = null;

    // XfileSharingProBasic Version 2.5.2.0-modified beyond belief
    // chlog
    // 31-01-2013 - raztoki
    // - hacked up the script to support normal linkchecking first, and if that fails reverts to a revised api call.
    // - download method represents both linkchecking routines, and handlefree/handlepremium download

    @Override
    public String getAGBLink() {
        return COOKIE_HOST + "/tos.html";
    }

    public FileBoxCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(COOKIE_HOST + "/premium.html");
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return true;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("filebox.com/vidembed-", "filebox.com/"));
    }

    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        java.util.List<DownloadLink> failedLinkCheck = new ArrayList<DownloadLink>();
        for (DownloadLink url : urls) {
            try {
                requestFileInformation(url);
            } catch (Exception a) {
                failedLinkCheck.add(url);
            }
        }
        while (!failedLinkCheck.isEmpty()) {
            try {
                // not extensively tested.
                final Browser br = new Browser();
                br.setCookie(COOKIE_HOST, "lang", "english");
                br.setCookiesExclusive(true);
                final StringBuilder sb = new StringBuilder();
                final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
                while (!failedLinkCheck.isEmpty()) {
                    links.clear();
                    for (DownloadLink link : failedLinkCheck) {
                        links.add(link);
                        if (links.size() == 50) break;
                    }
                    sb.delete(0, sb.capacity());
                    sb.append("op=checkfiles&process=Check+URLs&list=");
                    for (final DownloadLink dl : links) {
                        sb.append(dl.getDownloadURL());
                        sb.append("%0A");
                    }
                    br.postPage(COOKIE_HOST + "/?op=checkfiles", sb.toString());
                    for (final DownloadLink dllink : links) {
                        final String fid = new Regex(dllink.getDownloadURL(), "([a-z0-9]+)$").getMatch(0);
                        if (br.containsHTML("filebox\\.com/" + fid + " found</font>")) {
                            dllink.setAvailable(true);
                            dllink.setName(fid);
                        } else if (br.containsHTML("filebox\\.com/" + fid + " not found\\!</font>")) {
                            dllink.setAvailable(false);
                        } else {
                            dllink.setAvailable(false);
                            dllink.getLinkStatus().setStatusText("Linkchecker is probably broken!");
                        }
                        failedLinkCheck.remove(dllink);
                    }
                }
            } catch (Exception b) {
            }
        }
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        LINKID = new Regex(link.getDownloadURL(), "([a-z0-9]{12})$").getMatch(0);
        String directlinkproperty = null;
        Account acc = AccountController.getInstance().getValidAccount(this);
        if (acc != null && acc.getBooleanProperty("nopremium")) {
            // free account
            directlinkproperty = "freelink2";
        } else if (acc != null && !acc.getBooleanProperty("nopremium")) {
            // prem account
            directlinkproperty = "premlink";
        } else {
            // non account
            directlinkproperty = "freelink";
        }
        String dllink = link.getStringProperty(directlinkproperty);
        if (dllink != null && link.getName() != null) {
            // lets try to dl without repeating
            return AvailableStatus.TRUE;
        } else {
            // lets reset values
            String passCode = link.getStringProperty("pass");
            this.setBrowserExclusive();
            br.getPage(link.getDownloadURL());
            doSomething();
            long timeBefore = System.currentTimeMillis();
            Form dlForm = br.getFormbyProperty("name", "F1");
            if (dlForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            boolean password = false;
            if (new Regex(correctedBR, PASSWORDTEXT).matches()) {
                password = true;
                logger.info("The downloadlink seems to be password protected.");
            }
            if (password) passCode = handlePassword(passCode, dlForm, link);
            waitTime(timeBefore, link);
            br.submitForm(dlForm);
            logger.info("Submitted DLForm");
            doSomething();
            checkErrors(link, true, passCode);
            dllink = getDllink();

            String filename = br.getRegex(">File Name : <span>(.*?)</span></div>").getMatch(0);
            if (filename == null) filename = br.getRegex(">(.*?)</a></textarea></div>").getMatch(0);
            if (filename != null) link.setFinalFileName(filename.trim());
            String filesize = br.getRegex(">Size : <span[^>]+>(\\d+ bytes)</span></div>").getMatch(0);
            if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize.trim()));
            if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            if (dllink != null) {
                // finallink url actually contains real filename instead of 'video.extension' from the vidembed- page.
                link.setProperty(directlinkproperty, dllink);
                /** Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(this.getHost());
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                link.setProperty("cookies", cookies);
            }
            return AvailableStatus.TRUE;
        }

    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, true, 1, "freelink");
    }

    public void doFree(DownloadLink downloadLink, boolean resumable, int maxchunks, String directlinkproperty) throws Exception, PluginException {
        String passCode = downloadLink.getStringProperty("pass");
        Browser br2 = br.cloneBrowser();
        if (downloadLink.getStringProperty(directlinkproperty) != null) {
            if (downloadLink.getProperty("cookies", null) != null) {
                br2 = new Browser();
                br2.setCookiesExclusive(true);
                final Object ret = downloadLink.getProperty("cookies", null);
                final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                    final String key = cookieEntry.getKey();
                    final String value = cookieEntry.getValue();
                    br2.setCookie(this.getHost(), key, value);
                }
            }
            String dllink = checkDirectLink(downloadLink, directlinkproperty, br2);
            if (dllink == null) {
                // revert back to vidembed!
                br2.getPage("http://www.filebox.com/vidembed-" + LINKID);
                doSomething();
                dllink = getDllink();
                if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            logger.info("Final downloadlink = " + dllink + " starting the download...");
            String ext = dllink.substring(dllink.lastIndexOf("."));
            if (ext == null || ext.length() > 5) ext = ".mp4";
            if (dllink.contains("video.mp4")) downloadLink.setFinalFileName(LINKID + ext);
            dl = jd.plugins.BrowserAdapter.openDownload(br2, downloadLink, dllink, resumable, maxchunks);
            if (dl.getConnection().getContentType().contains("html")) {
                if (dl.getConnection().getResponseCode() == 415) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l);
                logger.warning("The final dllink seems not to be a file!");
                br2.followConnection();
                doSomething();
                checkServerErrors();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (passCode != null) downloadLink.setProperty("pass", passCode);
            if (Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())).equals("video.flv")) downloadLink.setFinalFileName(new Regex(downloadLink.getDownloadURL(), "([a-z0-9]+)$").getMatch(0) + ".flv");
            dl.startDownload();
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 2;
    }

    /** This removes fake messages which can kill the plugin */
    public void doSomething() throws NumberFormatException, PluginException {
        correctedBR = br.toString();
        ArrayList<String> someStuff = new ArrayList<String>();
        ArrayList<String> regexStuff = new ArrayList<String>();
        regexStuff.add("<\\!(\\-\\-.*?\\-\\-)>");
        regexStuff.add("(display: none;\">.*?</div>)");
        regexStuff.add("(visibility:hidden>.*?<)");
        for (String aRegex : regexStuff) {
            String lolz[] = br.getRegex(aRegex).getColumn(0);
            if (lolz != null) {
                for (String dingdang : lolz) {
                    someStuff.add(dingdang);
                }
            }
        }
        for (String fun : someStuff) {
            correctedBR = correctedBR.replace(fun, "");
        }
    }

    public String getDllink() {
        String dllink = br.getRedirectLocation();
        if (dllink == null) {
            dllink = new Regex(correctedBR, "dotted #bbb;padding.*?<a href=\"(.*?)\"").getMatch(0);
            if (dllink == null) {
                dllink = new Regex(correctedBR, "This (direct link|download link) will be available for your IP.*?href=\"(http.*?)\"").getMatch(1);
                if (dllink == null) {
                    dllink = new Regex(correctedBR, "Download: <a href=\"(.*?)\"").getMatch(0);
                    if (dllink == null) {
                        if (dllink == null) {
                            dllink = new Regex(correctedBR, "value=\"Download\" onclick=\"document\\.location=\\'(http://[^<>\"]+)\\'\"").getMatch(0);
                            if (dllink == null) {
                                dllink = new Regex(correctedBR, "class=\"getpremium_heading4\"><a href=\"(http://[^<>\"\\']+)\"").getMatch(0);
                                if (dllink == null) {
                                    dllink = new Regex(br, "(http://([a-z0-9]+\\.)?(filebox\\.com|\\d+\\.\\d+\\.\\d+\\.\\d+):\\d+/d/[a-z0-9]+/[^<>\"]*?)\"").getMatch(0);
                                    if (dllink == null) {
                                        String cryptedScripts[] = br.getRegex("p\\}\\((.*?)\\.split\\('\\|'\\)").getColumn(0);
                                        if (cryptedScripts != null && cryptedScripts.length != 0) {
                                            for (String crypted : cryptedScripts) {
                                                dllink = decodeDownloadLink(crypted);
                                                if (dllink != null) break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return dllink;
    }

    public void checkErrors(DownloadLink theLink, boolean checkAll, String passCode) throws NumberFormatException, PluginException {
        if (checkAll) {
            if (new Regex(correctedBR, PASSWORDTEXT).matches() || correctedBR.contains("Wrong password")) {
                logger.warning("Wrong password, the entered password \"" + passCode + "\" is wrong, retrying...");
                throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
            }
            if (correctedBR.contains("Wrong captcha")) {
                logger.warning("Wrong captcha or wrong password!");
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            if (correctedBR.contains("\">Skipped countdown<")) throw new PluginException(LinkStatus.ERROR_FATAL, "Fatal countdown error (countdown skipped)");
        }
        /** Waittime reconnect handling */
        if (new Regex(correctedBR, "(You have reached the download\\-limit|You have to wait)").matches()) {
            String tmphrs = new Regex(correctedBR, "\\s+(\\d+)\\s+hours?").getMatch(0);
            if (tmphrs == null) tmphrs = new Regex(correctedBR, "You have to wait.*?\\s+(\\d+)\\s+hours?").getMatch(0);
            String tmpmin = new Regex(correctedBR, "\\s+(\\d+)\\s+minutes?").getMatch(0);
            if (tmpmin == null) tmpmin = new Regex(correctedBR, "You have to wait.*?\\s+(\\d+)\\s+minutes?").getMatch(0);
            String tmpsec = new Regex(correctedBR, "\\s+(\\d+)\\s+seconds?").getMatch(0);
            String tmpdays = new Regex(correctedBR, "\\s+(\\d+)\\s+days?").getMatch(0);
            if (tmphrs == null && tmpmin == null && tmpsec == null && tmpdays == null) {
                logger.info("Waittime regexes seem to be broken");
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 60 * 60 * 1000l);
            } else {
                int minutes = 0, seconds = 0, hours = 0, days = 0;
                if (tmphrs != null) hours = Integer.parseInt(tmphrs);
                if (tmpmin != null) minutes = Integer.parseInt(tmpmin);
                if (tmpsec != null) seconds = Integer.parseInt(tmpsec);
                if (tmpdays != null) days = Integer.parseInt(tmpdays);
                int waittime = ((days * 24 * 3600) + (3600 * hours) + (60 * minutes) + seconds + 1) * 1000;
                logger.info("Detected waittime #2, waiting " + waittime + "milliseconds");
                /** Not enough waittime to reconnect->Wait and try again */
                if (waittime < 180000) { throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.xfilesharingprobasic.allwait", ALLWAIT_SHORT), waittime); }
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, waittime);
            }
        }
        if (correctedBR.contains("You're using all download slots for IP")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 10 * 60 * 1001l); }
        if (correctedBR.contains("Error happened when generating Download Link")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error!", 10 * 60 * 1000l);
        /** Errorhandling for only-premium links */
        if (new Regex(correctedBR, "( can download files up to |Upgrade your account to download bigger files|>Upgrade your account to download larger files|>The file You requested  reached max downloads limit for Free Users|Please Buy Premium To download this file<|This file reached max downloads limit)").matches()) {
            String filesizelimit = new Regex(correctedBR, "You can download files up to(.*?)only").getMatch(0);
            if (filesizelimit != null) {
                filesizelimit = filesizelimit.trim();
                logger.warning("As free user you can download files up to " + filesizelimit + " only");
                throw new PluginException(LinkStatus.ERROR_FATAL, "Free users can only download files up to " + filesizelimit);
            } else {
                logger.warning("Only downloadable via premium");
                throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadable via premium or registered");
            }
        }
        if (new Regex(correctedBR, Pattern.compile(MAINTENANCE)).matches()) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.xfilesharingprobasic.undermaintenance", MAINTENANCEUSERTEXT), 2 * 60 * 60 * 1000l);
    }

    public void checkServerErrors() throws NumberFormatException, PluginException {
        if (new Regex(correctedBR, Pattern.compile("No file", Pattern.CASE_INSENSITIVE)).matches()) throw new PluginException(LinkStatus.ERROR_FATAL, "Server error");
        if (new Regex(correctedBR, "(File Not Found|<h1>404 Not Found</h1>)").matches()) {
            logger.warning("Server says link offline, please recheck that!");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    private String decodeDownloadLink(String s) {
        String decoded = null;

        try {
            Regex params = new Regex(s, "\\'(.*?[^\\\\])\\',(\\d+),(\\d+),\\'(.*?)\\'");

            String p = params.getMatch(0).replaceAll("\\\\", "");
            int a = Integer.parseInt(params.getMatch(1));
            int c = Integer.parseInt(params.getMatch(2));
            String[] k = params.getMatch(3).split("\\|");

            while (c != 0) {
                c--;
                if (k[c].length() != 0) p = p.replaceAll("\\b" + Integer.toString(c, a) + "\\b", k[c]);
            }

            decoded = p;
        } catch (Exception e) {
        }

        String finallink = null;
        if (decoded != null) {
            finallink = new Regex(decoded, "name=\"src\"value=\"(.*?)\"").getMatch(0);
            if (finallink == null) {
                finallink = new Regex(decoded, "type=\"video/divx\"src=\"(.*?)\"").getMatch(0);
                if (finallink == null) finallink = new Regex(decoded, "\\.addVariable\\(\\'file\\',\\'(http://.*?)\\'\\)").getMatch(0);
            }
        }
        return finallink;
    }

    public String handlePassword(String passCode, Form pwform, DownloadLink thelink) throws IOException, PluginException {
        passCode = thelink.getStringProperty("pass", null);
        if (passCode == null) passCode = Plugin.getUserInput("Password?", thelink);
        pwform.put("password", passCode);
        logger.info("Put password \"" + passCode + "\" entered by user in the DLForm.");
        return Encoding.urlEncode(passCode);
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        /* reset maxPrem workaround on every fetchaccount info */
        maxPrem.set(1);
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        String space = br.getRegex(Pattern.compile("<td>Used space:</td>.*?<td.*?b>([0-9\\.]+) of [0-9\\.]+ (Mb|GB)</b>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (space != null) ai.setUsedSpace(space.trim() + " Mb");
        account.setValid(true);
        String availabletraffic = new Regex(correctedBR, "Traffic available.*?:</TD><TD><b>([^<>\"\\']+)</b>").getMatch(0);
        if (availabletraffic != null && !availabletraffic.contains("nlimited") && !availabletraffic.equalsIgnoreCase(" Mb")) {
            ai.setTrafficLeft(SizeFormatter.getSize(availabletraffic));
        } else {
            ai.setUnlimitedTraffic();
        }
        if (account.getBooleanProperty("nopremium")) {
            ai.setStatus("Registered (free) User");
            try {
                maxPrem.set(2);
                // free accounts can still have captcha.
                account.setMaxSimultanDownloads(maxPrem.get());
                account.setConcurrentUsePossible(true);
            } catch (final Throwable e) {
                // not available in old Stable 0.9.581
            }
        } else {
            String expire = new Regex(correctedBR, Pattern.compile("<td>Premium(\\-| )Account expires?:</td>.*?<td>(<b>)?(\\d{1,2} [A-Za-z]+ \\d{4})(</b>)?</td>", Pattern.CASE_INSENSITIVE)).getMatch(2);
            if (expire == null) {
                ai.setExpired(true);
                account.setValid(false);
                return ai;
            } else {
                expire = expire.replaceAll("(<b>|</b>)", "");
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd MMMM yyyy", Locale.ENGLISH));
            }
            ai.setStatus("Premium User");
            try {
                maxPrem.set(1);
                // free accounts can still have captcha.
                account.setMaxSimultanDownloads(maxPrem.get());
                account.setConcurrentUsePossible(true);
            } catch (final Throwable e) {
                // not available in old Stable 0.9.581
            }
        }
        return ai;
    }

    private static AtomicInteger maxPrem = new AtomicInteger(1);

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        String directlinkproperty = null;
        int chunks;
        boolean resumes;
        if (account.getBooleanProperty("nopremium")) {
            directlinkproperty = "freelink2";
            chunks = 1;
            resumes = true;
        } else {
            directlinkproperty = "premlink";
            chunks = -10;
            resumes = true;
        }

        if (downloadLink.getStringProperty(directlinkproperty) != null) {
            if (downloadLink.getProperty("cookies", null) != null) {
                Browser br = new Browser();
                br.setCookiesExclusive(true);
                final Object ret = downloadLink.getProperty("cookies", null);
                final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                    final String key = cookieEntry.getKey();
                    final String value = cookieEntry.getValue();
                    br.setCookie(COOKIE_HOST, key, value);
                }
            }

            String dllink = checkDirectLink(downloadLink, directlinkproperty, br);
            if (dllink == null) {
                logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            logger.info("Final downloadlink = " + dllink + " starting the download...");
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumes, chunks);
            if (dl.getConnection().getContentType().contains("html")) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                doSomething();
                checkServerErrors();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();

        } else {
            String passCode = null;
            requestFileInformation(downloadLink);
            login(account, false);
            br.setFollowRedirects(false);
            String dllink = null;
            if (account.getBooleanProperty("nopremium")) {
                br.getPage(downloadLink.getDownloadURL());
                doSomething();
                doFree(downloadLink, resumes, chunks, directlinkproperty);
            } else {
                dllink = checkDirectLink(downloadLink, directlinkproperty, br);
                if (dllink == null) {
                    br.getPage(downloadLink.getDownloadURL());
                    doSomething();
                    dllink = getDllink();
                    if (dllink == null) {
                        checkErrors(downloadLink, true, passCode);
                        Form DLForm = br.getFormbyProperty("name", "F1");
                        if (DLForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        if (new Regex(correctedBR, PASSWORDTEXT).matches()) passCode = handlePassword(passCode, DLForm, downloadLink);
                        br.submitForm(DLForm);
                        doSomething();
                        dllink = getDllink();
                        checkErrors(downloadLink, true, passCode);
                    }
                }
                if (dllink == null) {
                    logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                logger.info("Final downloadlink = " + dllink + " starting the download...");
                dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumes, chunks);
                if (dl.getConnection().getContentType().contains("html")) {
                    logger.warning("The final dllink seems not to be a file!");
                    br.followConnection();
                    doSomething();
                    checkServerErrors();
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (passCode != null) downloadLink.setProperty("pass", passCode);
                dl.startDownload();
            }
        }
    }

    @SuppressWarnings("unchecked")
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
                            this.br.setCookie(COOKIE_HOST, key, value);
                        }
                        return;
                    }
                }
                br.setCookie(COOKIE_HOST, "lang", "english");
                br.getPage(COOKIE_HOST + "/login.html");
                Form loginform = br.getForm(0);
                if (loginform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                loginform.put("login", Encoding.urlEncode(account.getUser()));
                loginform.put("password", Encoding.urlEncode(account.getPass()));
                br.submitForm(loginform);
                if (br.getCookie(COOKIE_HOST, "login") == null || br.getCookie(COOKIE_HOST, "xfss") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                br.getPage(COOKIE_HOST + "/?op=my_account");
                doSomething();
                if (new Regex(correctedBR, "value=\"Extend Premium account\"").matches()) {
                    account.setProperty("nopremium", false);
                } else {
                    account.setProperty("nopremium", true);
                }
                /** Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(COOKIE_HOST);
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

    private String checkDirectLink(DownloadLink downloadLink, String property, Browser br) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    if (downloadLink.getProperty("cookies", null) != null) downloadLink.setProperty("cookies", Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                if (downloadLink.getProperty("cookies", null) != null) downloadLink.setProperty("cookies", Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /* workaround for free/premium issue on stable 09581 */
        return maxPrem.get();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    private void waitTime(long timeBefore, DownloadLink downloadLink) throws PluginException {
        int passedTime = (int) ((System.currentTimeMillis() - timeBefore) / 1000) - 1;
        /** Ticket Time */
        final String ttt = new Regex(correctedBR, "id=\"countdown_str\">[^<]+<span[^>]+>([\n ]+)?(\\d+)([\n ]+)?</span>").getMatch(1);
        if (ttt != null) {
            int tt = Integer.parseInt(ttt);
            tt -= passedTime;
            logger.info("Waittime detected, waiting " + ttt + " - " + passedTime + " seconds from now on...");
            if (tt > 0) sleep(tt * 1000l, downloadLink);
        }
    }

}
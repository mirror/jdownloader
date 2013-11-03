//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filesmonster.com" }, urls = { "https?://[\\w\\.\\d]*?filesmonsterdecrypted\\.com/(download.php\\?id=|dl/.*?/free/2/).+" }, flags = { 2 })
public class FilesMonsterCom extends PluginForHost {

    private static final String POSTTHATREGEX            = "\"(https?://filesmonster\\.com/dl/.*?/free/.*?)\"";
    private static final String POSTTHATREGEX2           = "(https?://(www\\.)?filesmonster\\.com/dl/.*?/free/.+)";
    private static final String TEMPORARYUNAVAILABLE     = "Download not available at the moment";
    private static final String REDIRECTFNF              = "DL_FileNotFound";
    private static final String PREMIUMONLYUSERTEXT      = "Only downloadable via premium";
    private static Object       LOCK                     = new Object();

    private static final String ADDLINKSACCOUNTDEPENDANT = "ADDLINKSACCOUNTDEPENDANT";

    public FilesMonsterCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setConfigElements();
        this.enablePremium("http://filesmonster.com/service.php");
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("filesmonsterdecrypted.com/", "filesmonster.com/"));
    }

    @Override
    public String getAGBLink() {
        return "http://filesmonster.com/rules.php";
    }

    // @Override to keep compatible to stable
    public boolean canHandle(DownloadLink downloadLink, Account account) {
        if (downloadLink.getBooleanProperty("PREMIUMONLY") && account == null) {
            /* premium only */
            return false;
        }
        if (downloadLink.getDownloadURL().contains("/free/2/") && account != null) {
            /* free only */
            return false;
        }
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        br.setReadTimeout(3 * 60 * 1000);
        br.setFollowRedirects(false);
        if (downloadLink.getDownloadURL().contains("/free/2/")) {
            br.getPage(downloadLink.getStringProperty("mainlink"));
            if (br.getRedirectLocation() != null) {
                if (br.getRedirectLocation().contains(REDIRECTFNF)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            downloadLink.setName(downloadLink.getName());
            downloadLink.setDownloadSize(downloadLink.getDownloadSize());
        } else {
            if (downloadLink.getBooleanProperty("offline", false)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            br.getPage(downloadLink.getDownloadURL());
            // Link offline
            if (br.containsHTML("(>File was deleted by owner or it was deleted for violation of copyrights<|>File not found<|>The link could not be decoded<)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            // Advertising link
            if (br.containsHTML(">the file can be accessed at the")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (br.getRedirectLocation() != null) {
                // Link offline 2
                if (br.getRedirectLocation().contains(REDIRECTFNF)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }

            synchronized (LOCK) {

                /*
                 * we only have to load this once, to make sure its loaded
                 */
                JDUtilities.getPluginForDecrypt("filesmonster.comDecrypt");

            }
            String filename = br.getRegex(jd.plugins.decrypter.FilesMonsterDecrypter.FILENAMEREGEX).getMatch(0);
            String filesize = br.getRegex(jd.plugins.decrypter.FilesMonsterDecrypter.FILESIZEREGEX).getMatch(0);
            if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            downloadLink.setFinalFileName(Encoding.htmlDecode(filename.trim()));
            if (filesize != null) {
                downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.trim()));
            }
        }
        if (br.containsHTML(TEMPORARYUNAVAILABLE)) downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.filesmonstercom.temporaryunavailable", "Download not available at the moment"));
        if (downloadLink.getBooleanProperty("PREMIUMONLY")) downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.filesmonstercom.only4premium", PREMIUMONLYUSERTEXT));
        return AvailableStatus.TRUE;

    }

    private String getNewTemporaryLink(String mainlink, String originalfilename) throws IOException, PluginException {
        // Find a new temporary link
        String mainlinkpart = new Regex(mainlink, "filesmonster\\.com/download\\.php\\?id=(.+)").getMatch(0);
        String temporaryLink = null;
        br.getPage(mainlink);
        final String[] allInfo = getTempLinks();
        if (allInfo != null && allInfo.length != 0) {
            for (String singleInfo : allInfo)
                if (singleInfo.contains("\"name\":\"" + originalfilename + "\"")) temporaryLink = new Regex(singleInfo, "\"dlcode\":\"(.*?)\"").getMatch(0);
        }
        if (temporaryLink != null) temporaryLink = "http://filesmonster.com/dl/" + mainlinkpart + "/free/2/" + temporaryLink + "/";
        return temporaryLink;
    }

    private void handleErrors() throws PluginException {
        logger.info("Handling errors...");
        if (br.containsHTML(TEMPORARYUNAVAILABLE)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.filesmonstercom.temporaryunavailable", "Download not available at the moment"), 120 * 60 * 1000l);
        String wait = br.getRegex("You can wait for the start of downloading (\\d+)").getMatch(0);
        if (wait == null) {
            wait = br.getRegex("is already in use (\\d+)").getMatch(0);
            if (wait == null) {
                wait = br.getRegex("You can start new download in (\\d+)").getMatch(0);
                if (wait == null) {
                    if (wait == null) {
                        wait = br.getRegex("will be available for free download in (\\d+) min\\.").getMatch(0);
                        if (wait == null) {
                            wait = br.getRegex("<br>Next free download will be available in (\\d+) min").getMatch(0);
                            if (wait == null) {
                                wait = br.getRegex("will be available for download in (\\d+) min").getMatch(0);
                            }
                        }
                    }
                }
            }

        }
        if (wait != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Long.parseLong(wait) * 60 * 1001l);
        if (br.containsHTML("Minimum interval between free downloads is 45 minutes")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 45 * 60 * 1001l);
        if (br.containsHTML("Respectfully yours Adminstration of Filesmonster\\.com")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
        if (br.containsHTML("You need Premium membership to download files")) throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.filesmonstercom.only4premium", PREMIUMONLYUSERTEXT));
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (downloadLink.getBooleanProperty("PREMIUMONLY")) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) throw (PluginException) e;
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, PREMIUMONLYUSERTEXT);
        }
        handleErrors();
        br.setFollowRedirects(true);
        String postThat = br.getRegex(POSTTHATREGEX).getMatch(0);
        if (postThat == null) postThat = new Regex(downloadLink.getDownloadURL(), POSTTHATREGEX2).getMatch(0);
        if (postThat == null) {
            logger.warning("The following string could not be found: postThat");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!downloadLink.getDownloadURL().contains("/free/2/")) {
            br.postPage(postThat, "");
            if (br.containsHTML("Free download links:")) {
                downloadLink.setProperty("PREMIUMONLY", true);
                try {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
                } catch (final Throwable e) {
                    if (e instanceof PluginException) throw (PluginException) e;
                }
                throw new PluginException(LinkStatus.ERROR_FATAL, PREMIUMONLYUSERTEXT);
            }
        } else {
            downloadLink.getLinkStatus().setStatusText("Waiting for ticket...");
            String newTemporaryLink = getNewTemporaryLink(downloadLink.getStringProperty("mainlink"), downloadLink.getStringProperty("origfilename"));
            if (newTemporaryLink == null) {
                logger.warning("Failed to find a new temporary link for this link...");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage(newTemporaryLink);
        }
        /* now we have the data page, check for wait time and data id */
        // Captcha handling
        PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        rc.parse();
        rc.load();
        File cf = rc.downloadCaptcha(getLocalCaptchaFile());
        String c = getCaptchaCode(cf, downloadLink);
        rc.setCode(c);
        handleErrors();
        if (br.containsHTML("(Captcha number error or expired|api\\.recaptcha\\.net)")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        if (!downloadLink.getDownloadURL().contains("/free/2/")) {
            String finalPage = br.getRegex("reserve_ticket\\(\\'(/dl/.*?)\\'\\)").getMatch(0);
            if (finalPage == null) {
                logger.warning("The following string could not be found: finalPage");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* request ticket for this file */
            br.getPage("http://filesmonster.com" + finalPage);
            String linkPart = br.getRegex("dlcode\":\"(.*?)\"").getMatch(0);
            String firstPart = new Regex(postThat, "(http://filesmonster\\.com/dl/.*?/free/)").getMatch(0);
            if (linkPart == null || firstPart == null) {
                logger.warning("The following string could not be found: linkPart or firstPart");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String nextLink = firstPart + "2/" + linkPart + "/";
            br.getPage(nextLink);
        }
        String overviewLink = br.getRegex("get_link\\('(/dl/.*?)'\\)").getMatch(0);
        if (overviewLink == null) {
            logger.warning("The following string could not be found: strangeLink");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        overviewLink = "http://filesmonster.com" + overviewLink;
        String regexedwaittime = br.getRegex("id=\\'sec\\'>(\\d+)</span>").getMatch(0);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        int shortWaittime = 45;
        if (regexedwaittime != null) {
            shortWaittime = Integer.parseInt(regexedwaittime);
        } else {
            logger.warning("Waittime regex doesn't work, using default waittime...");
        }
        sleep(shortWaittime * 1100l, downloadLink);
        try {
            br.getPage(overviewLink);
        } catch (Exception e) {
        }
        handleErrors();
        String dllink = br.getRegex("url\":\"(http:.*?)\"").getMatch(0);
        if (dllink == null) {
            logger.warning("The following string could not be found: dllink");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = dllink.replace("\\", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The downloadlink doesn't seem to refer to a file, following the connection...");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.setFilenameFix(true);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /** Load cookies */
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie(this.getHost(), key, value);
                        }
                        return;
                    }
                }
                br.setReadTimeout(3 * 60 * 1000);
                br.setFollowRedirects(true);
                // get login page first, that way we don't post twice in case
                // captcha is already invoked!
                br.getPage("http://filesmonster.com/login.php");
                Form login = br.getFormbyProperty("name", "login");
                if (login == null) {
                    String lang = System.getProperty("user.language");
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
                    DownloadLink dummyLink = new DownloadLink(this, "Account", "http://filesmonster.com", "http://filesmonster.com", true);
                    final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                    final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                    final String id = br.getRegex("\\?k=([A-Za-z0-9%_\\+\\- ]+)\"").getMatch(0);
                    if (id == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    rc.setId(id);
                    rc.load();
                    final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    final String c = getCaptchaCode(cf, dummyLink);
                    login.put("recaptcha_challenge_field", rc.getChallenge());
                    login.put("recaptcha_response_field", Encoding.urlEncode(c));
                }
                login.put("user", Encoding.urlEncode(account.getUser()));
                login.put("pass", Encoding.urlEncode(account.getPass()));
                br.submitForm(login);

                if (br.getRegex("Your membership type: <span class=\"[A-Za-z0-9 ]+\">(Premium)</span>").getMatch(0) == null || br.containsHTML("Username/Password can not be found in our database") || br.containsHTML("Try to recover your password by 'Password reminder'")) throw new PluginException(LinkStatus.ERROR_PREMIUM);
                /** Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(this.getHost());
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
                account.setProperty("lastlogin", System.currentTimeMillis());
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                account.setProperty("lastlogin", Property.NULL);
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            // CAPTCHA is shown after 30 successful logins since beginning of
            // the day or after 5 unsuccessful login attempts.
            // Make sure account service updates do not login more than once
            // every 4 hours? so we only use up to 6 logins a day?
            if (account.getStringProperty("lastlogin") != null && (System.currentTimeMillis() - 14400000 <= Long.parseLong(account.getStringProperty("lastlogin"))))
                login(account, false);
            else
                login(account, true);
        } catch (final PluginException e) {
            account.setValid(false);
            throw e;
        }
        // needed because of cached login and we need to have a browser
        // containing html to regex against!
        if (br.getURL() == null || !br.getURL().equalsIgnoreCase("http://filesmoster.com/")) br.getPage("http://filesmonster.com/");
        ai.setUnlimitedTraffic();
        String expires = br.getRegex("<span>Valid until: <span class=\\'green\\'>([^<>\"]*?)</span>").getMatch(0);
        long ms = 0;
        if (expires != null) {
            ms = TimeFormatter.getMilliSeconds(expires, "MM/dd/yy HH:mm", null);
            if (ms <= 0) {
                ms = TimeFormatter.getMilliSeconds(expires, "MM/dd/yy", Locale.ENGLISH);
            }
            ai.setValidUntil(ms);
            try {
                trafficUpdate(ai, account);
            } catch (IOException e) {
            }
            account.setValid(true);
            ai.setStatus("Premium User");
            return ai;
        } else {
            account.setValid(false);
            return ai;
        }
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        if (!downloadLink.getDownloadURL().contains("download.php?id=")) {
            logger.info(downloadLink.getDownloadURL());
            throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.filesmonstercom.only4freeusers", "This file is only available for freeusers"));
        }
        login(account, false);
        br.setDebug(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML(TEMPORARYUNAVAILABLE)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.filesmonstercom.temporaryunavailable", "Download not available at the moment"), 120 * 60 * 1000l);
        String premlink = br.getRegex("\"(http://filesmonster\\.com/get/.*?)\"").getMatch(0);
        if (premlink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage(premlink);
        if (br.containsHTML("<div id=\"error\">Today you have already downloaded")) {
            try {
                trafficUpdate(null, account);
            } catch (IOException e) {
            }
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        String ajaxurl = br.getRegex("get_link\\(\"(.*?)\"\\)").getMatch(0);
        Browser ajax = br.cloneBrowser();
        ajax.getPage(ajaxurl);

        String dllink = ajax.getRegex("url\":\"(http:.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = dllink.replaceAll("\\\\/", "/");
        /* max chunks to 1 , because each chunk gets calculated full size */
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (!(dl.getConnection().isContentDisposition())) {
            br.followConnection();
        }
        dl.startDownload();
    }

    private AccountInfo trafficUpdate(AccountInfo importedAi, Account account) throws IOException {
        AccountInfo ai = new AccountInfo();
        if (importedAi == null)
            ai = account.getAccountInfo();
        else
            ai = importedAi;
        // care of filesmonster
        br.getPage("/today_downloads/");
        String[] dailyQuota = br.getRegex("Today you have already downloaded <span[^>]+>(\\d+(\\.\\d+)? ?(KB|MB|GB)) </span>\\.[\r\n\t ]+Daily download limit <span[^>]+>(\\d+(\\.\\d+)? ?(KB|MB|GB))").getRow(0);
        if (dailyQuota != null) {
            long usedQuota = SizeFormatter.getSize(dailyQuota[0]);
            long maxQuota = SizeFormatter.getSize(dailyQuota[3]);
            long dataLeft = maxQuota - usedQuota;
            if (dataLeft <= 0) dataLeft = 0;
            ai.setTrafficLeft(dataLeft);
            ai.setTrafficMax(maxQuota);
        } else {
            // br.containsHTML("Today you have not downloaded anything")
            // resorted to setting static default
            ai.setTrafficLeft(SizeFormatter.getSize("12 GiB"));
            ai.setTrafficMax(SizeFormatter.getSize("12 GiB"));
        }
        // not sure if this is needed, but can't hurt either way.
        if (importedAi == null) account.setAccountInfo(ai);
        return ai;
    }

    private String[] getTempLinks() throws IOException {
        String[] decryptedStuff = null;
        final String postThat = br.getRegex("\"(/dl/.*?)\"").getMatch(0);
        if (postThat != null) {
            br.postPage("http://filesmonster.com" + postThat, "");
            final String findOtherLinks = br.getRegex("\\'(/dl/rft/.*?)\\'").getMatch(0);
            if (findOtherLinks != null) {
                br.getPage("http://filesmonster.com" + findOtherLinks);
                decryptedStuff = br.getRegex("\\{(.*?)\\}").getColumn(0);
            }
        }
        return decryptedStuff;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public String getDescription() {
        return "JDownloader's Filesmonster.com Plugin helps downloading files from Filesmonster.com.";
    }

    private void setConfigElements() {
        final StringBuilder sbinfo = new StringBuilder();
        sbinfo.append("Filesmonster provides a link which can only be downloaded by premium users\r\n");
        sbinfo.append("and multiple links which can only be downloaded by free users.\r\n");
        sbinfo.append("Whenever you add a filesmonster link, JDownloader will show both links in the linkgrabber via default.\r\n");
        sbinfo.append("The setting below will make this behaviour more intelligent.\r\n");
        sbinfo.append("\r\n");
        sbinfo.append("NOTE: If you enable this feature and add links before setting up your filesmonster premium\r\n");
        sbinfo.append("account in JD you will have to add these links again after adding the account to get the premium links!\r\n");
        sbinfo.append("Do NOT enable this setting if you're not familiar with JDownloader!\r\n");
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, sbinfo.toString()));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FilesMonsterCom.ADDLINKSACCOUNTDEPENDANT, JDL.L("plugins.hoster.filesmonstercom.AddLinksDependingOnAvailableAccounts", "Add only premium-only links whenever a premium account is available\r\n and add only free-only-links whenever no premium account is available?")).setDefaultValue(false));
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return false;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
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
            return true;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }
}
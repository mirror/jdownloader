//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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

import java.io.File;
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
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;

/**
 *
 * @author raztoki
 *
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "keep2share.cc" }, urls = { "https?://((www|new)\\.)?(keep2share|k2s|k2share|keep2s|keep2)\\.cc/file/(info/)?[a-z0-9]+" })
public class Keep2ShareCc extends K2SApi {
    public Keep2ShareCc(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(MAINPAGE + "/premium.html");
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return MAINPAGE + "/page/terms.html";
    }

    public final String MAINTLD  = "k2s.cc";
    public final String MAINPAGE = "https://" + MAINTLD; // new.keep2share.cc and keep2share.cc share same

    // private final String DOMAINS_HTTP = "(https?://((www|new)\\.)?" + DOMAINS_PLAIN + ")";
    @Override
    public String[] siteSupportedNames() {
        // keep2.cc no dns
        return new String[] { "keep2share.cc", "k2s.cc", "keep2s.cc", "k2share.cc", "keep2share.com" };
    }

    @Override
    public String rewriteHost(String host) {
        if (host == null) {
            return "keep2share.cc";
        }
        for (final String supportedName : siteSupportedNames()) {
            if (supportedName.equals(host)) {
                return "keep2share.cc";
            }
        }
        return super.rewriteHost(host);
    }

    @Override
    public String buildExternalDownloadURL(DownloadLink downloadLink, PluginForHost buildForThisPlugin) {
        if (StringUtils.equals("real-debrid.com", buildForThisPlugin.getHost())) {
            return "http://keep2share.cc/file/" + getFUID(downloadLink);// do not change
        } else {
            return downloadLink.getPluginPatternMatcher();
        }
    }

    /* abstract K2SApi class setters */
    /**
     * sets domain the API will use!
     */
    @Override
    protected String getDomain() {
        return MAINTLD;
    }

    @Override
    public long getVersion() {
        return (Math.max(super.getVersion(), 0) * 100000) + getAPIRevision();
    }

    @Override
    protected Browser prepBrowser(final Browser prepBr) {
        super.prepBrowser(prepBr);
        prepBr.setConnectTimeout(90 * 1000);
        return prepBr;
    }

    /**
     * easiest way to set variables, without the need for multiple declared references
     *
     * @param account
     */
    private void setConstants(final Account account) {
        if (account != null) {
            if (account.getType() == AccountType.FREE) {
                // free account
                chunks = 1;
                resumes = true;
                isFree = true;
                directlinkproperty = "freelink2";
            } else {
                // premium account
                chunks = 0;
                resumes = true;
                isFree = false;
                directlinkproperty = "premlink";
            }
            logger.finer("setConstants = " + account.getUser() + " @ Account Download :: isFree = " + isFree + ", upperChunks = " + chunks + ", Resumes = " + resumes);
        } else {
            // free non account
            chunks = 1;
            resumes = true;
            isFree = true;
            directlinkproperty = "freelink1";
            logger.finer("setConstants = Guest Download :: isFree = " + isFree + ", upperChunks = " + chunks + ", Resumes = " + resumes);
        }
    }

    /* end of abstract class setters */
    @Override
    public void correctDownloadLink(final DownloadLink link) {
        // link cleanup, but respect users protocol choosing.
        if (link.getSetLinkID() == null) {
            try {
                setFUID(link);
            } catch (PluginException e) {
            }
        }
        // DO NOT AUTOCORRECT DOMAINS
        // final String linkID = getFUID(link);
        link.setPluginPatternMatcher(link.getPluginPatternMatcher().replaceFirst("https?://", getProtocol()));
    }

    public void followRedirectNew(Browser br) throws Exception {
        final String forceNew = br.getRegex("<a href=\"(/file/[a-z0-9]+\\?force_new=1)").getMatch(0);
        if (forceNew != null) {
            getPage(forceNew);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        // for multihosters which call this method directly.
        if (useAPI()) {
            return super.requestFileInformation(link);
        }
        correctDownloadLink(link);
        br = newWebBrowser();
        br.setFollowRedirects(true);
        super.prepBrowserForWebsite(br);
        getPage(buildExternalDownloadURL(link, this));
        followRedirectNew(br);
        if (isNewLayout2017()) {
            return requestFileInformationNew2017(link);
        } else {
            return requestFileInformationOld(link);
        }
    }

    public AvailableStatus requestFileInformationOld(final DownloadLink link) throws Exception {
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("<title>Keep2Share\\.cc - Error</title>")) {
            link.getLinkStatus().setStatusText("Cannot check status - unknown error state");
            return AvailableStatus.UNCHECKABLE;
        }
        final String filename = getFileName();
        final String filesize = getFileSize();
        if (filename != null) {
            if (filename.contains("...")) {
                super.checkLinks(new DownloadLink[] { link });
            } else {
                link.setName(Encoding.htmlDecode(filename.trim()));
            }
        }
        if (filesize != null) {
            /* Remove spaces to support such inputs: 1 000.0 MB */
            link.setDownloadSize(SizeFormatter.getSize(filesize.trim().replace(" ", "")));
        }
        if (br.containsHTML("Downloading blocked due to")) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Downloading blocked: No JD bug, please contact the keep2share support", 10 * 60 * 1000l);
        }
        // you can set filename for offline links! handling should come here!
        if (br.containsHTML("Sorry, an error occurred while processing your request|File not found or deleted|>Sorry, this file is blocked or deleted\\.</h5>|class=\"empty\"|>Displaying 1")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (isPremiumOnly()) {
            link.getLinkStatus().setStatusText("Only downloadable for premium users");
        }
        return AvailableStatus.TRUE;
    }

    /** 2017-03-22: They switched to a new layout (accessible via new.keep2share.cc), old is still online at the moment. */
    public AvailableStatus requestFileInformationNew2017(final DownloadLink link) throws Exception {
        /*
         * TODO: Add error handling here - filename might not be available or located in a different place for abused content or when a
         * download limit is reached!
         */
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">This file is no longer available")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = getFileName();
        final String filesize = getFileSize();
        if (filename != null) {
            if (filename.contains("...")) {
                super.checkLinks(new DownloadLink[] { link });
            } else {
                link.setName(Encoding.htmlDecode(filename.trim()));
            }
        }
        if (filesize != null) {
            /* Remove spaces to support such inputs: 1 000.0 MB */
            link.setDownloadSize(SizeFormatter.getSize(filesize.trim().replace(" ", "")));
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (isPremiumOnly()) {
            link.getLinkStatus().setStatusText("Only downloadable for premium users");
        }
        return AvailableStatus.TRUE;
    }

    /**
     * E.g. user starts a download, stops it, directurl does not work anymore --> Retry --> Keep2share will save that information based on
     * his IP and possibly offer the free download without having to enter another captcha.
     */
    public boolean freeDownloadImmediatelyPossible() {
        return br.containsHTML(">To download this file with slow speed, use");
    }

    /** Determines via html strings whether we are on the new- or the old keep2share website. */
    public boolean isNewLayout2017() {
        return br.containsHTML("class=\"footer-nav\"|class=\"list-services\"");
    }

    public String getFileNameNew2017() {
        String fileName = br.getRegex("<span class=\"name-file\">\\s*(.*?)\\s*<em").getMatch(0);
        if (fileName == null) {
            fileName = br.getRegex("class=\"title-file\">\\s*([^<>\"]+)\\s*<").getMatch(0);
            if (fileName == null) {
                // 20170811 only available for premium users. note does not display filesize.
                fileName = br.getRegex("<strong>\\s*(.*?)\\s*</strong>\\s*available only for premium members").getMatch(0);
            }
        }
        return fileName;
    }

    public String getFileSizeNew2017() {
        final String filesize = br.getRegex("<span class=\"name-file\">.*?<em>(.*?)</em").getMatch(0);
        return filesize;
    }

    public String getFileName() {
        String fileName = getFileNameNew2017();
        // This might not be needed anymore but keeping it doesn't hurt either
        if (fileName == null && freeDownloadImmediatelyPossible()) {
            fileName = br.getRegex(">Downloading file:</span><br>[\t\n\r ]+<span class=\"c2\">.*?alt=\"\" style=\"\">([^<>\"]*?)</span>").getMatch(0);
        }
        if (fileName == null) {
            fileName = br.getRegex("File: <span>([^<>\"]*?)</span>").getMatch(0);
            if (fileName == null) {
                // offline/deleted
                fileName = br.getRegex("File name:</b>(.*?)<br>").getMatch(0);
            }
        }
        return fileName;
    }

    public String getFileSize() {
        String filesize = getFileSizeNew2017();
        if (filesize == null && freeDownloadImmediatelyPossible()) {
            filesize = br.getRegex("File size ([^<>\"]*?)</div>").getMatch(0);
        }
        if (filesize == null) {
            filesize = br.getRegex(">Size: ([^<>\"]*?)</div>").getMatch(0);
            if (filesize == null) {
                // offline/deleted
                filesize = br.getRegex("<b>File size:</b>(.*?)<br>").getMatch(0);
                if (filesize == null) {
                    // offline/deleted
                    filesize = br.getRegex("<span class=\"(?:name|title)-file\">\\s*.*?\\s*<em>\\s*([0-9\\. KMBT]+)").getMatch(0);
                }
            }
        }
        return filesize != null ? filesize.replaceAll("\\s", "") : null;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        setConstants(null);
        if (checkShowFreeDialog(getHost())) {
            showFreeDialog(getHost());
        }
        if (useAPI()) {
            super.handleDownload(downloadLink, null);
        } else {
            requestFileInformation(downloadLink);
            doFree(downloadLink, null);
        }
    }

    private void doFree(final DownloadLink downloadLink, final Account account) throws Exception {
        handleGeneralErrors(account);
        br.setFollowRedirects(false);
        if (isPremiumOnly()) {
            premiumDownloadRestriction("This file is only available to premium members");
        }
        String dllink = getDirectLinkAndReset(downloadLink, true);
        // because opening the link to test it, uses up the availability, then reopening it again = too many requests too quickly issue.
        if (!inValidate(dllink)) {
            final Browser obr = br.cloneBrowser();
            logger.info("Reusing cached final link!");
            dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, resumes, chunks);
            if (!isValidDownloadConnection(dl.getConnection())) {
                logger.info("Refresh final link");
                dllink = null;
                try {
                    dl.getConnection().setAllowedResponseCodes(new int[] { dl.getConnection().getResponseCode() });
                    br.followConnection();
                } catch (final Throwable e) {
                    logger.log(e);
                } finally {
                    br = obr;
                    dl.getConnection().disconnect();
                }
            }
        }
        /* if above has failed, dllink will be null */
        if (inValidate(dllink)) {
            if (freeDownloadImmediatelyPossible()) {
                dllink = getDllink();
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            } else {
                if (br.containsHTML("Traffic limit exceed!<br>|Download count files exceed!<br>")) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 30 * 60 * 1000l);
                }
                final String uniqueID = br.getRegex("name=\"slow_id\" value=\"([^<>\"]*?)\"").getMatch(0);
                if (uniqueID == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                postPage(br.getURL(), "slow_id=" + uniqueID);
                br.getHeaders().put("X-Requested-With", null);
                if (br.containsHTML("Free user can't download large files")) {
                    premiumDownloadRestriction("This file is only available to premium members");
                }
                // Browser br2 = br.cloneBrowser();
                // domain not transferable!
                // getPage(br2, getProtocol() + "static.k2s.cc/ext/evercookie/evercookie.swf");//404 no longer exists
                // can be here also, raztoki 20130521!
                dllink = getDllink();
                if (inValidate(dllink)) {
                    handleFreeErrors();
                    if (br.containsHTML("Free account does not allow to download more than one file at the same time")) {
                        throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1000l);
                    }
                    if (br.containsHTML("class=\"g-recaptcha\"")) {
                        final Form recap = br.getFormbyProperty("id", "captcha-form");
                        if (recap == null) {
                            // huston we have a problem
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br) {
                        }.getToken();
                        recap.put(Encoding.urlEncode("ReCaptchaForm[verifyCode]"), Encoding.urlEncode(recaptchaV2Response));
                        sendForm(recap);
                    } else if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
                        logger.info("Detected captcha method \"Re Captcha\" for this host");
                        final Recaptcha rc = new Recaptcha(br, this);
                        final String id = br.getRegex("\\?k=([A-Za-z0-9%_\\+\\- ]+)\"").getMatch(0);
                        if (id == null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        rc.setId(id);
                        rc.load();
                        final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                        final String c = getCaptchaCode("recaptcha", cf, downloadLink);
                        postPage(br.getURL(), "CaptchaForm%5Bcode%5D=&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c) + "&free=1&freeDownloadRequest=1&yt0=&uniqueId=" + uniqueID);
                        if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
                            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                        }
                    } else {
                        final String captchaLink = br.getRegex("\"(/file/captcha\\.html\\?[^\"]+)\"").getMatch(0);
                        if (captchaLink == null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        final String code = getCaptchaCode(captchaLink, downloadLink);
                        postPage(br.getURL(), "CaptchaForm%5Bcode%5D=" + code + "&free=1&freeDownloadRequest=1&uniqueId=" + uniqueID);
                        if (br.containsHTML(">The verification code is incorrect|/site/captcha.html")) {
                            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                        }
                    }
                    /** Skippable */
                    int wait = 30;
                    final String waittime = br.getRegex("<div id=\"download-wait-timer\">[\t\n\r ]+(\\d+)[\t\n\r ]+</div>").getMatch(0);
                    if (waittime != null) {
                        wait = Integer.parseInt(waittime);
                    }
                    sleep(wait * 1001l, downloadLink);
                    br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    postPage(br.getURL(), "free=1&uniqueId=" + uniqueID);
                    br.getHeaders().put("X-Requested-With", null);
                    dllink = getDllink();
                    if (inValidate(dllink)) {
                        handleFreeErrors();
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
            }
            logger.info("dllink = " + dllink);
            dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, resumes, chunks);
            if (!isValidDownloadConnection(dl.getConnection())) {
                dl.getConnection().setAllowedResponseCodes(new int[] { dl.getConnection().getResponseCode() });
                br.followConnection();
                dllink = br.getRegex("\"url\":\"(https?:[^<>\"]*?)\"").getMatch(0);
                if (dllink == null) {
                    handleGeneralServerErrors(account, downloadLink);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                dllink = dllink.replace("\\", "");
                dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, resumes, chunks);
                if (!isValidDownloadConnection(dl.getConnection())) {
                    dl.getConnection().setAllowedResponseCodes(new int[] { dl.getConnection().getResponseCode() });
                    logger.warning("The final dllink seems not to be a file!");
                    br.followConnection();
                    handleGeneralServerErrors(account, downloadLink);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        // add download slot
        controlSlot(+1, account);
        try {
            downloadLink.setProperty(directlinkproperty, dllink);
            dl.startDownload();
        } finally {
            // remove download slot
            controlSlot(-1, account);
        }
    }

    private void handleFreeErrors() throws PluginException {
        if (br.containsHTML("\">Downloading is not possible<")) {
            int hours = 0, minutes = 0, seconds = 0;
            final Regex waitregex = br.getRegex("Please wait (\\d{2}):(\\d{2}):(\\d{2}) to download this file");
            final String hrs = waitregex.getMatch(0);
            if (hrs != null) {
                hours = Integer.parseInt(hrs);
            }
            final String mins = waitregex.getMatch(1);
            if (mins != null) {
                minutes = Integer.parseInt(mins);
            }
            final String secs = waitregex.getMatch(2);
            if (secs != null) {
                seconds = Integer.parseInt(secs);
            }
            final long totalwait = (hours * 60 * 60 * 1000) + (minutes * 60 * 1000l) + (seconds * 1000l);
            if (totalwait > 0) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, totalwait + 10000l);
            }
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
        }
        if (br.containsHTML("\\s*At the moment all free slots are busy, try later\\.<br>")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "No free slots available", 10 * 60 * 1000l);
        }
    }

    private boolean isPremiumOnly() {
        return br.containsHTML("File size to large!<") || br.containsHTML("Only <b>Premium</b> access<br>") || br.containsHTML("only for premium members");
    }

    private String getDllink() throws PluginException {
        String dllink = br.getRegex("('|\")(/file/url\\.html\\?file=[a-z0-9]+)\\1").getMatch(1);
        if (dllink != null) {
            dllink = new Regex(br.getURL(), "(https?://[^/]+)/").getMatch(0) + dllink;
        }
        return dllink;
    }

    private final String cookiesProperty = "cookies2";

    @SuppressWarnings("unchecked")
    private HashMap<String, String> login(final Account account, final boolean force, final String MAINPAGE) throws Exception {
        synchronized (ACCLOCK) {
            try {
                // clear cookies/headers etc. this should nullify redirects to /file/
                br = newWebBrowser();
                br.setFollowRedirects(true);
                // reduce cpu cycles, do not enter and do evaluations when they are not needed.
                if (!force) {
                    final Object ret = account.getProperty(cookiesProperty, null);
                    if (ret != null && ret instanceof HashMap<?, ?>) {
                        final boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser()))) && Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                        if (acmatch) {
                            final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                            if (account.isValid()) {
                                // load to all bloody domains
                                for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                                    for (final String tld : siteSupportedNames()) {
                                        final String key = cookieEntry.getKey();
                                        final String value = cookieEntry.getValue();
                                        br.setCookie(tld, key, value);
                                    }
                                }
                                getPage(MAINPAGE + "/site/profile.html");
                                if (!br._getURL().getFile().equals("/login.html")) {
                                    return cookies;
                                }
                                // dump session
                                br = newWebBrowser();
                            }
                        }
                    }
                }
                getPage(this.MAINPAGE + "/login.html");
                String csrftoken = br.getRegex("value=\"([^<>\"\\']+)\" name=\"YII_CSRF_TOKEN\"").getMatch(0);
                if (StringUtils.isEmpty(csrftoken)) {
                    csrftoken = br.getCookie(account.getHoster(), "YII_CSRF_TOKEN");
                }
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                String postData = "LoginForm%5BrememberMe%5D=0&LoginForm%5BrememberMe%5D=1&LoginForm%5Busername%5D=" + Encoding.urlEncode(account.getUser()) + "&LoginForm%5Bpassword%5D=" + Encoding.urlEncode(account.getPass());
                if (csrftoken != null) {
                    postData += "&YII_CSRF_TOKEN=" + Encoding.urlEncode(csrftoken);
                }
                // Handle stupid login captcha
                final String captchaLink = br.getRegex("\"(/auth/captcha\\.html\\?v=[a-z0-9]+)\"").getMatch(0);
                if (captchaLink != null) {
                    final DownloadLink dummyLink = new DownloadLink(this, "Account", account.getHoster(), "http://" + account.getHoster(), true);
                    final String code = getCaptchaCode("https://" + br.getHost() + captchaLink, dummyLink);
                    postData += "&LoginForm%5BverifyCode%5D=" + Encoding.urlEncode(code);
                } else {
                    if (br.containsHTML("recaptcha/api/challenge") || br.containsHTML("Recaptcha.create")) {
                        final DownloadLink dummyLink = new DownloadLink(this, "Account", account.getHoster(), "http://" + account.getHoster(), true);
                        final Recaptcha rc = new Recaptcha(br, this);
                        String challenge = br.getRegex("recaptcha/api/challenge\\?k=(.*?)\"").getMatch(0);
                        if (challenge == null) {
                            challenge = br.getRegex("Recaptcha.create\\('(.*?)'").getMatch(0);
                        }
                        rc.setId(challenge);
                        rc.load();
                        File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                        String c = getCaptchaCode("recaptcha", cf, dummyLink);
                        postData = postData + "&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c);
                    }
                }
                postPage("/login.html", postData);
                if (br.containsHTML("Incorrect username or password")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                if (br.containsHTML("The verification code is incorrect.")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Login Captcha ungültig!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Invalid login captcha!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                if (br.containsHTML(">We have a suspicion that your account was stolen, this is why we")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Account temporär gesperrt!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Account temporarily blocked!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                if (br.containsHTML(">Please fill in the form with your login credentials")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                if (br.containsHTML(">Password cannot be blank.<")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Passwortfeld darf nicht leer sein!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Password field cannot be empty!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                br.getHeaders().put("X-Requested-With", null);
                String url = br.getRegex("url\":\"(.*?)\"").getMatch(0);
                if (url == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(account.getHoster());
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty(cookiesProperty, cookies);
                return cookies;
            } catch (final PluginException e) {
                account.setProperty(cookiesProperty, Property.NULL);
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        if (account.getUser() == null || !account.getUser().contains("@")) {
            account.setValid(false);
            ai.setStatus("Please use E-Mail as login/name!\r\nBitte E-Mail Adresse als Benutzername benutzen!");
            return ai;
        }
        if (useAPI()) {
            ai = super.fetchAccountInfo(account);
        } else {
            try {
                login(account, true, MAINPAGE);
            } catch (final PluginException e) {
                account.setValid(false);
                throw e;
            }
            getPage("/site/profile.html");
            account.setValid(true);
            final String accountType = br.getRegex("<span>Account type: </span>\\s*<strong>\\s*(.*?)\\s*<").getMatch(0);
            if (br.containsHTML("class=\"free\"[^>]*>Free</a>") || "Free".equalsIgnoreCase(accountType)) {
                account.setType(AccountType.FREE);
                ai.setStatus("Free Account");
            } else {
                account.setType(AccountType.PREMIUM);
                final String usedTraffic = br.getRegex("Used traffic(.*?\\(today\\))?.*?<a[^/>]*href=\"/user/statistic\\.html\">(.*?)</").getMatch(1);
                String availableTraffic = br.getRegex("Available traffic(.*?\\(today\\))?.*?<a[^/>]*href=\"/user/statistic\\.html\">(.*?)</").getMatch(1);
                if (StringUtils.isEmpty(availableTraffic)) {
                    availableTraffic = br.getRegex("Traffic left(.*?\\(today\\))?.*?<a[^/>]*href=\"/user/statistic\\.html\">(.*?)</").getMatch(1);
                }
                if (availableTraffic != null && usedTraffic != null) {
                    final long used = SizeFormatter.getSize(usedTraffic);
                    final long available = SizeFormatter.getSize(availableTraffic);
                    ai.setTrafficLeft(available);
                    ai.setTrafficMax(used + available);
                } else {
                    ai.setUnlimitedTraffic();
                }
                String expire = br.getRegex("class=\"premium\">Premium:[\t\n\r ]+(\\d{4}\\.\\d{2}\\.\\d{2})").getMatch(0);
                if (expire == null) {
                    expire = br.getRegex("Premium expires:\\s*?<b>(\\d{4}\\.\\d{2}\\.\\d{2})").getMatch(0);
                    if (expire == null) {
                        expire = br.getRegex("Premium expires:.*?<strong.*?>\\s*(\\d{4}\\.\\d{2}\\.\\d{2})").getMatch(0);
                    }
                }
                if (expire == null && (br.containsHTML(">Premium:[\t\n\r ]+LifeTime") || "LifeTime".equals(accountType))) {
                    ai.setStatus("Premium Lifetime Account");
                    ai.setValidUntil(-1);
                } else if (expire == null) {
                    ai.setStatus("Premium Account");
                    ai.setValidUntil(-1);
                } else {
                    ai.setStatus("Premium Account");
                    // Expired but actually we still got one day ('today')
                    if (br.containsHTML("1 day")) {
                        ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy.MM.dd", Locale.ENGLISH) + 24 * 60 * 60 * 1000l);
                    } else {
                        ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy.MM.dd", Locale.ENGLISH));
                    }
                }
            }
        }
        setAccountLimits(account);
        account.setValid(true);
        return ai;
    }

    @Override
    protected void setAccountLimits(Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            maxPrem.set(1);
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            maxPrem.set(20);
        }
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        setConstants(account);
        if (account.getType() == AccountType.FREE) {
            if (checkShowFreeDialog(getHost())) {
                showFreeDialog(getHost());
            }
        }
        if (useAPI()) {
            super.handleDownload(link, account);
            return;
        }
        requestFileInformation(link);
        // login based on current link url domain.
        login(account, false, "https://" + Browser.getHost(link.getPluginPatternMatcher()));
        if (account.getType() == AccountType.FREE) {
            setConstants(account);
            // post login referrer info has gone.
            super.prepBrowserForWebsite(br);
            getPage(buildExternalDownloadURL(link, this));
            followRedirectNew(br);
            doFree(link, account);
            return;
        }
        String dllink = getDirectLinkAndReset(link, true);
        if (!inValidate(dllink)) {
            final Browser obr = br.cloneBrowser();
            logger.info("Reusing cached final link!");
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, resumes, chunks);
            if (!isValidDownloadConnection(dl.getConnection())) {
                try {
                    dl.getConnection().setAllowedResponseCodes(new int[] { dl.getConnection().getResponseCode() });
                    br.followConnection();
                } catch (final Throwable e) {
                    logger.log(e);
                } finally {
                    try {
                        dl.getConnection().disconnect();
                    } catch (final Throwable e) {
                    }
                    dl = null;
                    br = obr;
                    logger.info("Refresh final link");
                }
            }
        }
        if (dl == null) {
            br.setFollowRedirects(false);
            final String url = buildExternalDownloadURL(link, this);
            getPage(url);
            followRedirectNew(br);
            handleGeneralErrors(account);
            dllink = br.getRedirectLocation();
            if (inValidate(dllink)) {
                dllink = getDllinkPremium();
                if (inValidate(dllink)) {
                    if (br.containsHTML("class=\"btn-download\".*?>\\s*Download blocked")) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    if (br.containsHTML("Traffic limit exceed!<")) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                    }
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            logger.info("dllink = " + dllink);
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, resumes, chunks);
            if (!isValidDownloadConnection(dl.getConnection())) {
                dl.getConnection().setAllowedResponseCodes(new int[] { dl.getConnection().getResponseCode() });
                br.followConnection();
                dllink = getDllinkPremium();
                if (dllink == null) {
                    if (br.containsHTML("Download of file will start in")) {
                        dllink = br.getRegex("document\\.location\\.href\\s*=\\s*'(https?://.*?)'").getMatch(0);
                    } else {
                        dllink = null;
                    }
                }
                if (dllink == null) {
                    logger.warning("The final dllink seems not to be a file!");
                    handleGeneralServerErrors(account, link);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, resumes, chunks);
                if (!isValidDownloadConnection(dl.getConnection())) {
                    dl.getConnection().setAllowedResponseCodes(new int[] { dl.getConnection().getResponseCode() });
                    logger.warning("The final dllink seems not to be a file!");
                    br.followConnection();
                    handleGeneralServerErrors(account, link);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        // add download slot
        controlSlot(+1, account);
        try {
            link.setProperty(directlinkproperty, dllink);
            dl.startDownload();
        } finally {
            // remove download slot
            controlSlot(-1, account);
        }
    }

    private void handleGeneralErrors(final Account account) throws PluginException {
        if (br.containsHTML("<title>Keep2Share\\.cc - Error</title>")) {
            if (br.containsHTML("<li>Sorry, our store is not available, please try later")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'Store is temporarily unavailable'", 5 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 30 * 60 * 1000l);
        }
    }

    private String getDllinkPremium() {
        final String dllink = br.getRegex("('|\")(/file/url\\.html\\?file=[a-z0-9]+)\\1").getMatch(1);
        return inValidate(dllink) ? dllink : Request.getLocation(dllink, br.getRequest());
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return maxPrem.get();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return maxFree.get();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        super.resetLink(link);
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (acc.getType() == AccountType.FREE) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }

    @Override
    protected String getUseAPIPropertyID() {
        return super.getUseAPIPropertyID() + "_2";
    }

    @Override
    protected boolean isUseAPIDefaultEnabled() {
        return false;
    }

    /**
     * because stable is lame!
     */
    public void setBrowser(final Browser ibr) {
        this.br = ibr;
    }

    private void setConfigElements() {
        final ConfigEntry cfgapi = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), getUseAPIPropertyID(), "Use API (recommended!)").setDefaultValue(isUseAPIDefaultEnabled());
        getConfig().addEntry(cfgapi);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), EXPERIMENTALHANDLING, "Enable reconnect workaround (only for API mode!)?").setDefaultValue(default_eh).setEnabledCondidtion(cfgapi, true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, this.getPluginConfig(), super.CUSTOM_REFERER, "Set custom Referer here (only non NON-API mode!)").setDefaultValue(null).setEnabledCondidtion(cfgapi, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), SSL_CONNECTION, "Use Secure Communication over SSL (HTTPS://)").setDefaultValue(default_SSL_CONNECTION));
    }
}
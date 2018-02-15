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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.components.SiteType.SiteTemplate;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uptobox.com" }, urls = { "https?://(?:www\\.)?uptobox\\.com/[a-z0-9]{12}" })
public class UpToBoxCom extends antiDDoSForHost {
    private final static String  SSL_CONNECTION               = "SSL_CONNECTION";
    private boolean              happyHour                    = false;
    private String               correctedBR                  = "";
    private static final String  PASSWORDTEXT                 = "Passwor(d|t):</b> <input|Password:</b>";
    private final String         COOKIE_HOST                  = "http://uptobox.com";
    private static final String  DOMAINS                      = "(uptobox\\.com|uptostream\\.com)";
    private static final String  regexIpBlock                 = "<center><p><b>Sorry, " + DOMAINS + " is not available in your country</b></p></center>";
    private static final String  MAINTENANCE                  = ">This server is in maintenance mode|Our website is currently undergoing maintenance and will be back online shortly\\s*!\\s*<";
    private static final String  MAINTENANCEUSERTEXT          = JDL.L("hoster.xfilesharingprobasic.errors.undermaintenance", "This server is under Maintenance");
    private static final String  ALLWAIT_SHORT                = JDL.L("hoster.xfilesharingprobasic.errors.waitingfordownloads", "Waiting till new downloads can be started");
    private static final String  PREMIUMONLY1                 = JDL.L("hoster.xfilesharingprobasic.errors.premiumonly1", "Max downloadable filesize for free users:");
    private static final String  PREMIUMONLY2                 = JDL.L("hoster.xfilesharingprobasic.errors.premiumonly2", "Only downloadable via premium or registered");
    /* Connection stuff */
    private static final boolean FREE_RESUME                  = true;
    private static final int     FREE_MAXCHUNKS               = -2;
    private static final int     FREE_MAXDOWNLOADS            = 5;
    private static final boolean ACCOUNT_FREE_RESUME          = true;
    private static final int     ACCOUNT_FREE_MAXCHUNKS       = -2;
    private static final int     ACCOUNT_FREE_MAXDOWNLOADS    = 5;
    private static final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 10;
    // note: can not be negative -x or 0 .:. [1-*]
    private static AtomicInteger totalMaxSimultanFreeDownload = new AtomicInteger(FREE_MAXDOWNLOADS);
    // don't touch
    private static AtomicInteger maxFree                      = new AtomicInteger(1);
    private static Object        LOCK                         = new Object();

    // DEV NOTES
    // XfileSharingProBasic Version 2.5.6.1-raz
    // mods: heavily modified, do NOT upgrade!
    // non account: 2 * 1
    // free account: 2 * 1
    // premium account: 20 * 3 = max 60 connections
    // protocol: no https
    // captchatype: solvemedia
    // other: no redirects
    // Tags: uptostream.com, uptobox.com
    @Override
    public String getAGBLink() {
        return COOKIE_HOST + "/tos.html";
    }

    public UpToBoxCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(COOKIE_HOST + "/premium.html");
        this.setConfigElements();
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return false;
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

    @Override
    public void init() {
        super.init();
        Browser.setRequestIntervalLimitGlobal(getHost(), 500);
    }

    @Override
    protected boolean useRUA() {
        return true;
    }

    private boolean isLogin = false;

    @Override
    protected Browser prepBrowser(final Browser prepBr, final String host) {
        if (!(browserPrepped.containsKey(prepBr) && browserPrepped.get(prepBr) == Boolean.TRUE)) {
            super.prepBrowser(prepBr, host);
            prepBr.setCookie(COOKIE_HOST, "lang", "english");
            if (isLogin) {
                prepBr.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:37.0) Gecko/20100101 Firefox/37.0");
            }
        }
        return prepBr;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        getPage(link.getDownloadURL());
        if (new Regex(correctedBR, regexIpBlock).matches()) {
            // apparently error fatal will prevent multihoster.
            return AvailableStatus.UNCHECKABLE;
        } else if (new Regex(correctedBR, Pattern.compile("(No such file|>File Not Found<|>The file was removed by|Reason (of|for) deletion:\n|>File not found <|>Unfortunately, the file you want is not available)", Pattern.CASE_INSENSITIVE)).matches()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (new Regex(correctedBR, MAINTENANCE).matches()) {
            link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.xfilesharingprobasic.undermaintenance", MAINTENANCEUSERTEXT));
            return AvailableStatus.UNCHECKABLE;
        } else if (correctedBR.contains("No htmlCode read")) {
            link.getLinkStatus().setStatusText("Server error -> Can't check status");
            return AvailableStatus.UNCHECKABLE;
        }
        String filename = new Regex(correctedBR, "You have requested.*?https?://(www\\.)?" + this.getHost() + "/[A-Za-z0-9]{12}/(.*?)</font>").getMatch(1);
        if (filename == null) {
            filename = new Regex(correctedBR, "fname\"( type=\"hidden\")? value=\"(.*?)\"").getMatch(1);
            if (filename == null) {
                filename = new Regex(correctedBR, "<h2>Download File(.*?)</h2>").getMatch(0);
                if (filename == null) {
                    filename = new Regex(correctedBR, "Download File:? ?(<[^>]+> ?)+?([^<>\"\\']+)").getMatch(1);
                    if (filename == null) {
                        filename = new Regex(correctedBR, ">([^\r\n]+) \\(\\d+(\\.\\d+)? [A-Z]{2,}\\)<").getMatch(0);
                    }
                }
            }
        }
        String filesize = new Regex(correctedBR, "\\(([0-9]+ bytes)\\)").getMatch(0);
        if (filesize == null) {
            filesize = new Regex(correctedBR, "</font>[ ]+\\(([^<>\"\\'/]+)\\)(.*?)</font>").getMatch(0);
            if (filesize == null) {
                filesize = new Regex(correctedBR, "<div class=\"info\\-bar\\-grey\">[\t\n\r ]+http://uptobox\\.com/[a-z0-9]{12} \\((.*?)\\)").getMatch(0);
            }
        }
        if (filesize == null) {
            /* 2017-02-04 */
            filesize = new Regex(correctedBR, "para_title\">.*?\\(([\\d\\.]+ ?(KB|MB|GB|B))\\)<").getMatch(0);
        }
        if (filesize == null) {
            filesize = new Regex(correctedBR, "\\(([\\d\\.]+ ?(KB|MB|GB))\\)").getMatch(0);
        }
        if (filesize == null) {
            filesize = new Regex(correctedBR, "([\\d\\.]+ ?(KB|MB|GB))").getMatch(0);
        }
        if (filename == null || filename.equals("")) {
            if (correctedBR.contains("You have reached the download\\-limit")) {
                logger.warning("Wait time detected, please reconnect to make the linkchecker work!");
                return AvailableStatus.UNCHECKABLE;
            }
            logger.warning("The filename equals null, throwing \"plugin defect\" now...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String md5hash = new Regex(correctedBR, "<b>MD5.*?</b>.*?nowrap>(.*?)<").getMatch(0);
        if (md5hash != null) {
            link.setMD5Hash(md5hash.trim());
        }
        filename = Encoding.htmlOnlyDecode(filename.replaceAll("(</b>|<b>|\\.html)", ""));
        link.setProperty("plainfilename", filename);
        link.setFinalFileName(filename.trim());
        if (filesize != null && !filesize.equals("")) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        ipBlock();
        doFree(downloadLink, null, FREE_RESUME, FREE_MAXCHUNKS, "freelink");
    }

    public void doFree(final DownloadLink downloadLink, final Account account, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        String passCode = null;
        // First, bring up saved final links
        String dllink = checkDirectLink(downloadLink, directlinkproperty);
        // Second, check for streaming links on the first page
        if (dllink == null) {
            dllink = getDllink();
        }
        // Third, continue like normal.
        if (dllink == null) {
            checkErrors(downloadLink, false, passCode);
            if (correctedBR.contains("\"download1\"")) {
                postPage(br.getURL(), "op=download1&usr_login=&id=" + new Regex(downloadLink.getDownloadURL(), "/([A-Za-z0-9]{12})$").getMatch(0) + "&fname=" + Encoding.urlEncode(downloadLink.getStringProperty("plainfilename")) + "&referer=&method_free=Free+Download");
                checkErrors(downloadLink, false, passCode);
            }
            dllink = getDllink();
        }
        if (dllink == null) {
            Form dlForm = br.getFormbyProperty("name", "F1");
            if (dlForm == null) {
                for (Form form : br.getForms()) {
                    if (form.containsHTML("waitingToken")) {
                        dlForm = form;
                        break;
                    }
                }
            }
            if (dlForm == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (int i = 0; i <= 3; i++) {
                dlForm.remove(null);
                final long timeBefore = System.currentTimeMillis();
                boolean skipWaittime = false; // Wait time is not always needed.
                if (new Regex(correctedBR, PASSWORDTEXT).matches()) {
                    logger.info("The downloadlink seems to be password protected.");
                    passCode = handlePassword(passCode, dlForm, downloadLink);
                }
                /* if happy hour */
                if (new Regex(correctedBR, ">Happy hour!!! Download as many files as you want, we offer you a preview of the Premium\\.</font></p>").matches()) {
                    // yay sending requests too fast can result in class="err">Skipped countdown< ?? see if this helps.
                    happyHour = true;
                    // random int
                    int x = 0;
                    while (x < 5 || x > 25) {
                        x = new Random().nextInt(25);
                    }
                    sleep(x * 1000l, downloadLink);
                } else {
                    /* Captcha START */
                    if (correctedBR.contains(";background:#ccc;text-align")) {
                        logger.info("Detected captcha method \"plaintext captchas\" for this host");
                        /** Captcha method by ManiacMansion */
                        String[][] letters = new Regex(Encoding.htmlDecode(br.toString()), "<span style=\\'position:absolute;padding\\-left:(\\d+)px;padding\\-top:\\d+px;\\'>(\\d)</span>").getMatches();
                        if (letters == null || letters.length == 0) {
                            logger.warning("plaintext captchahandling broken!");
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        SortedMap<Integer, String> capMap = new TreeMap<Integer, String>();
                        for (String[] letter : letters) {
                            capMap.put(Integer.parseInt(letter[0]), letter[1]);
                        }
                        StringBuilder code = new StringBuilder();
                        for (String value : capMap.values()) {
                            code.append(value);
                        }
                        dlForm.put("code", code.toString());
                        logger.info("Put captchacode " + code.toString() + " obtained by captcha metod \"plaintext captchas\" in the form.");
                    } else if (correctedBR.contains("/captchas/")) {
                        logger.info("Detected captcha method \"Standard captcha\" for this host");
                        String[] sitelinks = HTMLParser.getHttpLinks(br.toString(), null);
                        String captchaurl = null;
                        if (sitelinks == null || sitelinks.length == 0) {
                            logger.warning("Standard captcha captchahandling broken!");
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        for (String link : sitelinks) {
                            if (link.contains("/captchas/")) {
                                captchaurl = link;
                                break;
                            }
                        }
                        if (captchaurl == null) {
                            logger.warning("Standard captcha captchahandling broken!");
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        String code = getCaptchaCode("xfilesharingprobasic", captchaurl, downloadLink);
                        dlForm.put("code", code);
                        logger.info("Put captchacode " + code + " obtained by captcha metod \"Standard captcha\" in the form.");
                    } else if (new Regex(correctedBR, "(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)").matches()) {
                        logger.info("Detected captcha method \"Re Captcha\" for this host");
                        final Recaptcha rc = new Recaptcha(br, this);
                        rc.setForm(dlForm);
                        String id = new Regex(correctedBR, "\\?k=([A-Za-z0-9%_\\+\\- ]+)\"").getMatch(0);
                        rc.setId(id);
                        rc.load();
                        File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                        String c = getCaptchaCode("recaptcha", cf, downloadLink);
                        Form rcform = rc.getForm();
                        rcform.put("recaptcha_challenge_field", rc.getChallenge());
                        rcform.put("recaptcha_response_field", Encoding.urlEncode(c));
                        logger.info("Put captchacode " + c + " obtained by captcha metod \"Re Captcha\" in the form and submitted it.");
                        dlForm = rc.getForm();
                        /** wait time is often skippable for reCaptcha handling */
                        skipWaittime = true;
                    } else if (br.containsHTML("solvemedia\\.com/papi/")) {
                        logger.info("Detected captcha method \"solvemedia\" for this host");
                        final org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia sm = new org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia(br);
                        File cf = null;
                        try {
                            cf = sm.downloadCaptcha(getLocalCaptchaFile());
                        } catch (final Exception e) {
                            if (org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia.FAIL_CAUSE_CKEY_MISSING.equals(e.getMessage())) {
                                throw new PluginException(LinkStatus.ERROR_FATAL, "Host side solvemedia.com captcha error - please contact the " + this.getHost() + " support");
                            }
                            throw e;
                        }
                        final String code = getCaptchaCode("solvemedia", cf, downloadLink);
                        final String chid = sm.getChallenge(code);
                        dlForm.put("adcopy_challenge", chid);
                        dlForm.put("adcopy_response", "manual_challenge");
                    }
                    /* Captcha END */
                    if (!skipWaittime) {
                        waitTime(timeBefore, downloadLink, true);
                    }
                }
                submitForm(dlForm);
                logger.info("Submitted DLForm");
                checkErrors(downloadLink, true, passCode);
                dllink = getDllink();
                if (dllink == null && br.containsHTML("<Form name=\"F1\" method=\"POST\" action=\"\"")) {
                    dlForm = br.getFormbyProperty("name", "F1");
                    if (dlForm == null) {
                        for (Form form : br.getForms()) {
                            if (form.containsHTML("waitingToken")) {
                                dlForm = form;
                                break;
                            }
                        }
                    }
                    continue;
                } else if (dllink == null && !br.containsHTML("<Form name=\"F1\" method=\"POST\" action=\"\"")) {
                    logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    break;
                }
            }
        }
        if (passCode != null) {
            downloadLink.setDownloadPassword(passCode);
        }
        logger.info("Final downloadlink = " + dllink + " starting the download...");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            correctBR();
            checkServerErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty(directlinkproperty, dllink);
        try {
            if (account == null) {
                // add a download slot
                controlFree(+1);
            }
            // start the dl
            dl.startDownload();
        } finally {
            if (account == null) {
                // remove download slot
                controlFree(-1);
            }
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return maxFree.get();
    }

    /**
     * Prevents more than one free download from starting at a given time. One step prior to dl.startDownload(), it adds a slot to maxFree
     * which allows the next singleton download to start, or at least try.
     *
     * This is needed because xfileshare(website) only throws errors after a final dllink starts transferring or at a given step within pre
     * download sequence. But this template(XfileSharingProBasic) allows multiple slots(when available) to commence the download sequence,
     * this.setstartintival does not resolve this issue. Which results in x(20) captcha events all at once and only allows one download to
     * start. This prevents wasting peoples time and effort on captcha solving and|or wasting captcha trading credits. Users will experience
     * minimal harm to downloading as slots are freed up soon as current download begins.
     *
     * @param controlFree
     *            (+1|-1)
     */
    public synchronized void controlFree(int num) {
        logger.info("maxFree was = " + maxFree.get());
        maxFree.set(Math.min(Math.max(1, maxFree.addAndGet(num)), totalMaxSimultanFreeDownload.get()));
        logger.info("maxFree now = " + maxFree.get());
    }

    /** Remove HTML code which could break the plugin */
    public void correctBR() throws NumberFormatException, PluginException {
        correctedBR = br.toString();
        ArrayList<String> someStuff = new ArrayList<String>();
        ArrayList<String> regexStuff = new ArrayList<String>();
        regexStuff.add("<\\!(\\-\\-.*?\\-\\-)>");
        regexStuff.add("(display: ?none;\">.*?</div>)");
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

    public String getDllink() throws Exception {
        String dllink = br.getRedirectLocation();
        if (dllink != null && dllink.endsWith("/" + new Regex(this.getDownloadLink().getDownloadURL(), "uptobox.com/([A-Za-z0-9]{12})$").getMatch(0))) {
            getPage(dllink);
            return getDllink();
        }
        if (dllink == null) {
            dllink = new Regex(correctedBR, "(\"|\\')(?:https?://[\\w\\.]*adf\\.ly/\\d+/)?(https?://(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|([\\w\\-]+\\.)?" + DOMAINS + ")(:\\d{1,4})?/((files|d|cgi\\-bin/dl\\.cgi|dl)/(\\d+/)?[a-z0-9]+/|[a-zA-Z0-9_\\-]{100,}/)[^<>\"/]*?)\\1").getMatch(1);
            if (dllink == null) {
                dllink = new Regex(correctedBR, "product_download_url=(https?://[^<>\"]*?)\"").getMatch(0);
            }
            if (dllink == null) {
                dllink = new Regex(correctedBR, "<\\s*a\\s*href\\s*=\\s*\"(https?://[^\">]*?)\".*?>\\s*Click here to start").getMatch(0);
            }
            if (dllink == null) {
                final String cryptedScripts[] = new Regex(correctedBR, "p\\}\\((.*?)\\.split\\('\\|'\\)").getColumn(0);
                if (cryptedScripts != null && cryptedScripts.length != 0) {
                    for (String crypted : cryptedScripts) {
                        dllink = decodeDownloadLink(crypted);
                        if (dllink != null) {
                            break;
                        }
                    }
                }
            }
        }
        /*
         * Special: Usually it is a bad idea to change the protocol of final downloadlinks but in this case it is fine plus this helps to
         * avoid gouvernment/ISP blocks!
         */
        if (dllink != null) {
            dllink = fixLinkSSL(dllink);
        }
        return dllink;
    }

    @Override
    protected void getPage(String page) throws Exception {
        page = fixLinkSSL(page);
        for (int i = 1; i <= 3; i++) {
            super.getPage(page);
            if (br.containsHTML("No htmlCode read")) {
                continue;
            }
            break;
        }
        correctBR();
        if (br.containsHTML("No htmlCode read")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
        }
    }

    @Override
    protected void postPage(String page, String postdata) throws Exception {
        page = fixLinkSSL(page);
        super.postPage(page, postdata);
        correctBR();
        if (correctedBR.contains("No htmlCode read")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
        }
    }

    @Override
    protected void submitForm(Form form) throws Exception {
        super.submitForm(form);
        correctBR();
    }

    private void ipBlock() throws PluginException {
        if (new Regex(correctedBR, regexIpBlock).matches()) {
            logger.warning("Country/IP Block issued hoster!");
            throw new PluginException(LinkStatus.ERROR_FATAL, "Country/IP Block issued hoster!");
        }
    }

    public void checkErrors(DownloadLink theLink, boolean checkAll, String passCode) throws NumberFormatException, PluginException {
        if (checkAll) {
            if (new Regex(correctedBR, PASSWORDTEXT).matches() || correctedBR.contains("Wrong password")) {
                logger.warning("Wrong password, the entered password \"" + passCode + "\" is wrong, retrying...");
                if (theLink.getDownloadPassword() != null) {
                    theLink.setDownloadPassword(null);
                }
                throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
            }
            if (correctedBR.contains("Wrong captcha")) {
                logger.warning("Wrong captcha or wrong password!");
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            if (correctedBR.contains("\">Skipped countdown<")) {
                // to cover bullshit response..
                if (happyHour) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Apparently there are wait times in happy hour?", 2 * 60 * 1000l);
                }
                throw new PluginException(LinkStatus.ERROR_FATAL, "Fatal countdown error (countdown skipped)");
            }
        }
        /** Wait time reconnect handling */
        if (new Regex(correctedBR, "(You have reached the download\\-limit|You have to wait|you can wait|Vous devez attendre)").matches()) {
            // adjust this regex to catch the wait time string for COOKIE_HOST
            String WAIT = new Regex(correctedBR, "((You have reached the download\\-limit|You have to wait|you can wait|Vous devez attendre)[^<>]+)").getMatch(0);
            String tmphrs = new Regex(WAIT, "\\s+(\\d+)\\s+hours?").getMatch(0);
            if (tmphrs == null) {
                tmphrs = new Regex(correctedBR, "You have to wait.*?\\s+(\\d+)\\s+hours?").getMatch(0);
            }
            String tmpmin = new Regex(WAIT, "\\s+(\\d+)\\s+minutes?").getMatch(0);
            if (tmpmin == null) {
                tmpmin = new Regex(correctedBR, "You have to wait.*?\\s+(\\d+)\\s+minutes?").getMatch(0);
            }
            String tmpsec = new Regex(WAIT, "\\s+(\\d+)\\s+seconds?").getMatch(0);
            String tmpdays = new Regex(WAIT, "\\s+(\\d+)\\s+days?").getMatch(0);
            if (tmphrs == null && tmpmin == null && tmpsec == null && tmpdays == null) {
                logger.info("Waittime regexes seem to be broken");
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 60 * 60 * 1000l);
            } else {
                int minutes = 0, seconds = 0, hours = 0, days = 0;
                if (tmphrs != null) {
                    hours = Integer.parseInt(tmphrs);
                }
                if (tmpmin != null) {
                    minutes = Integer.parseInt(tmpmin);
                }
                if (tmpsec != null) {
                    seconds = Integer.parseInt(tmpsec);
                }
                if (tmpdays != null) {
                    days = Integer.parseInt(tmpdays);
                }
                int waittime = ((days * 24 * 3600) + (3600 * hours) + (60 * minutes) + seconds + 1) * 1000;
                logger.info("Detected waittime #2, waiting " + waittime + "milliseconds");
                /** Not enough wait time to reconnect->Wait and try again */
                if (waittime < 180000) {
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.xfilesharingprobasic.allwait", ALLWAIT_SHORT), waittime);
                }
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, waittime);
            }
        }
        if (correctedBR.contains("You're using all download slots for IP")) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 10 * 60 * 1001l);
        }
        if (correctedBR.contains("Error happened when generating Download Link")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error!", 10 * 60 * 1000l);
        }
        /** Error handling for only-premium links */
        if (new Regex(correctedBR, "( can download files up to |Upgrade your account to download bigger files|>Upgrade your account to download larger files|>The file You requested  reached max downloads limit for Free Users|Please Buy Premium To download this file<|This file reached max downloads limit|>Le statut premium vous permet de t\\&eacute;l\\&eacutecharger|>You need to be a)").matches()) {
            String filesizelimit = new Regex(correctedBR, "You can download files up to(.*?)only").getMatch(0);
            if (filesizelimit != null) {
                filesizelimit = filesizelimit.trim();
                logger.warning("As free user you can download files up to " + filesizelimit + " only");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PREMIUMONLY1 + " " + filesizelimit, PluginException.VALUE_ID_PREMIUM_ONLY);
            } else {
                logger.warning("Only downloadable via premium");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PREMIUMONLY2, PluginException.VALUE_ID_PREMIUM_ONLY);
            }
        }
        if (new Regex(correctedBR, MAINTENANCE).matches()) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, MAINTENANCEUSERTEXT, 2 * 60 * 60 * 1000l);
        }
    }

    public void checkServerErrors() throws NumberFormatException, PluginException {
        if (new Regex(correctedBR, Pattern.compile("No file", Pattern.CASE_INSENSITIVE)).matches()) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Server error");
        }
        if (new Regex(correctedBR, "(File Not Found|<h1>404 Not Found</h1>)").matches() || br.getURL().endsWith("/404.html")) {
            logger.warning("Server says link offline, please recheck that!");
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404#2", 30 * 60 * 1000l);
        }
        /* Error 500 without 500 response. */
        if (correctedBR.contains(">Service Unavailable, Service temporairement indisponible<")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 500");
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
                if (k[c].length() != 0) {
                    p = p.replaceAll("\\b" + Integer.toString(c, a) + "\\b", k[c]);
                }
            }
            decoded = p;
        } catch (Exception e) {
        }
        String finallink = null;
        if (decoded != null) {
            finallink = new Regex(decoded, "name=\"src\"value=\"(.*?)\"").getMatch(0);
            if (finallink == null) {
                finallink = new Regex(decoded, "type=\"video/divx\"src=\"(.*?)\"").getMatch(0);
                if (finallink == null) {
                    finallink = new Regex(decoded, "\\.addVariable\\(\\'file\\',\\'(http://.*?)\\'\\)").getMatch(0);
                }
            }
        }
        return finallink;
    }

    private String handlePassword(String passCode, Form pwform, DownloadLink thelink) throws IOException, PluginException {
        passCode = thelink.getDownloadPassword();
        if (passCode == null) {
            passCode = Plugin.getUserInput("Password?", thelink);
        }
        pwform.put("password", Encoding.urlEncode(passCode));
        logger.info("Put password \"" + passCode + "\" entered by user in the DLForm.");
        return passCode;
    }

    private String checkDirectLink(DownloadLink downloadLink, String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        login(account, false);
        String space[][] = new Regex(correctedBR, "<td>Used space:</td>.*?<td.*?b>([0-9\\.]+) of [0-9\\.]+ (KB|MB|GB|TB)</b>").getMatches();
        if ((space != null && space.length != 0) && (space[0][0] != null && space[0][1] != null)) {
            ai.setUsedSpace(space[0][0] + " " + space[0][1]);
        }
        account.setValid(true);
        String availabletraffic = new Regex(correctedBR, "Traffic available.*?:</TD><TD><b>([^<>\"\\']+)</b>").getMatch(0);
        if (availabletraffic != null && !availabletraffic.contains("nlimited") && !availabletraffic.equalsIgnoreCase(" Mb")) {
            availabletraffic.trim();
            // need to set 0 traffic left, as getSize returns positive result,
            // even when negative value supplied.
            if (!availabletraffic.startsWith("-")) {
                ai.setTrafficLeft(SizeFormatter.getSize(availabletraffic));
            } else {
                ai.setTrafficLeft(0);
            }
        } else {
            ai.setUnlimitedTraffic();
        }
        if (AccountType.FREE == account.getType()) {
            ai.setStatus("Free Account");
            // free accounts can still have captcha.
            account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
            account.setConcurrentUsePossible(true);
        } else {
            // alternative xfileshare expire time, usually shown on 'extend
            // premium account' page
            String format = null;
            boolean extraDay = false;
            String expire = new Regex(correctedBR, "(\\d{1,2} (January|February|March|April|May|June|July|August|September|October|November|December)\\s*\\d{4}\\s*\\d+:\\d+)").getMatch(0);
            if (expire != null) {
                format = "dd MMMM yyyy HH:mm";
            } else if (expire == null) {
                expire = new Regex(correctedBR, "(\\d{1,2} (January|February|March|April|May|June|July|August|September|October|November|December) \\d{4})").getMatch(0);
                if (expire != null) {
                    extraDay = true;
                    format = "dd MMMM yyyy";
                }
            }
            if (expire == null) {
                ai.setExpired(true);
                account.setValid(false);
                return ai;
            }
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, format, Locale.ENGLISH) + (extraDay ? (24 * 60 * 60 * 1000l) : 0));
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setConcurrentUsePossible(true);
            ai.setStatus("Premium Account");
        }
        return ai;
    }

    @SuppressWarnings("unchecked")
    private void login(Account account, boolean force) throws Exception {
        final boolean before = br.isFollowingRedirects();
        synchronized (LOCK) {
            try {
                isLogin = true;
                /** Load cookies */
                br.setCookiesExclusive(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) {
                    acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(COOKIE_HOST, key, value);
                        }
                        // check
                        getPage("https://uptobox.com/?op=my_account");
                        if (isCookieSessionValid()) {
                            return;
                        }
                        br = new Browser();
                    }
                }
                br.setFollowRedirects(true);
                getPage("https://uptobox.com/?op=login");
                ipBlock();
                final Form login = br.getFormbyKey("password");
                if (login == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                login.put("login", Encoding.urlEncode(account.getUser()));
                login.put("password", Encoding.urlEncode(account.getPass()));
                // there can be recaptchav2 here
                if (login.containsHTML("class=(?:'|\")g-recaptcha")) {
                    // recapthav2
                    final DownloadLink original = this.getDownloadLink();
                    if (original == null) {
                        this.setDownloadLink(new DownloadLink(this, "Account", "uptobox.com", "http://uptobox.com", true));
                    }
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                    if (original == null) {
                        this.setDownloadLink(null);
                    }
                    login.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                }
                submitForm(login);
                if (!isCookieSessionValid()) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername, Passwort oder login Captcha!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else if ("pl".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBłędny użytkownik/hasło lub kod Captcha wymagany do zalogowania!\r\nUpewnij się, że prawidłowo wprowadziłes hasło i nazwę użytkownika. Dodatkowo:\r\n1. Jeśli twoje hasło zawiera znaki specjalne, zmień je (usuń) i spróbuj ponownie!\r\n2. Wprowadź hasło i nazwę użytkownika ręcznie bez użycia opcji Kopiuj i Wklej.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password or login captcha!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                getPage("/?op=my_account");
                if (!new Regex(correctedBR, "class=\"premium_time\"").matches()) {
                    account.setType(AccountType.FREE);
                } else {
                    account.setType(AccountType.PREMIUM);
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", fetchCookies(COOKIE_HOST));
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            } finally {
                br.setFollowRedirects(before);
                isLogin = false;
            }
        }
    }

    private boolean isCookieSessionValid() {
        final boolean login = br.getCookie(COOKIE_HOST, "login") != null && !"deleted".equalsIgnoreCase(br.getCookie(COOKIE_HOST, "login"));
        if (login) {
            return login;
        }
        final boolean xfss = br.getCookie(COOKIE_HOST, "xfss") != null && !"deleted".equalsIgnoreCase(br.getCookie(COOKIE_HOST, "xfss"));
        if (xfss) {
            return xfss;
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        String passCode = null;
        requestFileInformation(link);
        ipBlock();
        br = new Browser();
        login(account, false);
        br.setFollowRedirects(false);
        String dllink = null;
        if (AccountType.FREE == account.getType()) {
            getPage(link.getDownloadURL());
            doFree(link, account, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "freelink2");
        } else {
            dllink = checkDirectLink(link, "premlink");
            if (dllink == null) {
                getPage(link.getDownloadURL());
                dllink = getDllink();
                if (dllink == null) {
                    // this is required here for maintenance mode, otherwise throw plugin defect will kick in if forms are not found!
                    checkErrors(link, false, passCode);
                    if (new Regex(correctedBR, PASSWORDTEXT).matches()) {
                        Form dlform = br.getFormbyProperty("name", "F1");
                        if (dlform == null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        passCode = handlePassword(passCode, dlform, link);
                        submitForm(dlform);
                        dllink = getDllink();
                    } else {
                        Form dlform = br.getFormbyProperty("name", "F1");
                        if (dlform == null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        submitForm(dlform);
                        dllink = getDllink();
                    }
                }
                if (dllink == null) {
                    checkErrors(link, true, passCode);
                }
            }
            if (dllink == null) {
                logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (passCode != null) {
                link.setDownloadPassword(passCode);
            }
            logger.info("Final downloadlink = " + dllink + " starting the download...");
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
            if (dl.getConnection().getContentType().contains("html")) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                correctBR();
                checkServerErrors();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setProperty("premlink", dllink);
            dl.startDownload();
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    private void waitTime(long timeBefore, DownloadLink downloadLink, final boolean forceWait) throws PluginException {
        int wait = 0;
        int passedTime = (int) ((System.currentTimeMillis() - timeBefore) / 1000) - 1;
        /* Ticket Time */
        String regexed_wait = new Regex(correctedBR, "<span id=\"[a-z0-9]+\">(\\d+)</span>\\s*seconds").getMatch(0);
        if (regexed_wait == null) {
            regexed_wait = new Regex(correctedBR, "time-remaining' data-remaining-time='(\\d+)'").getMatch(0);
        }
        if (regexed_wait == null && forceWait) {
            wait = 50;
        } else if (regexed_wait != null) {
            wait = Integer.parseInt(regexed_wait);
        }
        /* Do not use fake waittime */
        if (wait > 210 || wait < 3) {
            wait = 60;
        }
        /* Add some random seconds */
        wait += new Random().nextInt(10);
        /* Remove time, user needed to enter the captcha */
        wait -= passedTime;
        logger.info("Waittime detected, waiting " + regexed_wait + " - " + passedTime + " seconds from now on...");
        if (wait > 0) {
            sleep(wait * 1000l, downloadLink);
        }
    }

    private static String fixLinkSSL(String link) {
        if (link == null) {
            return null;
        }
        if (checkSsl()) {
            link = link.replace("http://", "https://");
        } else {
            link = link.replace("https://", "http://");
        }
        return link;
    }

    @SuppressWarnings("deprecation")
    private static boolean checkSsl() {
        return SubConfiguration.getConfig("uptobox.com").getBooleanProperty(SSL_CONNECTION, true);
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), SSL_CONNECTION, JDL.L("plugins.hoster.UpToBox.preferSSL", "Use Secure Communication over SSL (HTTPS://)")).setDefaultValue(true));
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.SibSoft_XFileShare;
    }
}
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
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.captcha.SkipException;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.CaptchaException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.components.SiteType.SiteTemplate;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "novafile.com" }, urls = { "https?://(www\\.)?novafile\\.com/[a-z0-9]{12}" })
public class NovaFileCom extends antiDDoSForHost {
    private String               correctedBR                  = "";
    private static final String  PASSWORDTEXT                 = "<br><b>Passwor(d|t):</b> <input";
    private static final String  COOKIE_HOST                  = "https://novafile.com";
    private static final String  MAINTENANCE                  = ">This server is in maintenance mode|No htmlCode read";
    private static final String  MAINTENANCEUSERTEXT          = JDL.L("hoster.xfilesharingprobasic.errors.undermaintenance", "This server is under Maintenance");
    private static final String  ALLWAIT_SHORT                = JDL.L("hoster.xfilesharingprobasic.errors.waitingfordownloads", "Waiting till new downloads can be started");
    private static final String  PREMIUMONLY1                 = JDL.L("hoster.xfilesharingprobasic.errors.premiumonly1", "Max downloadable filesize for free users:");
    private static final String  PREMIUMONLY2                 = JDL.L("hoster.xfilesharingprobasic.errors.premiumonly2", "Only downloadable via premium or registered");
    // note: can not be negative -x or 0 .:. [1-*]
    private static AtomicInteger totalMaxSimultanFreeDownload = new AtomicInteger(1);
    // don't touch
    private static AtomicInteger maxFree                      = new AtomicInteger(1);
    private static Object        LOCK                         = new Object();

    // DEV NOTES
    // XfileSharingProBasic Version 2.5.6.8-raz
    // mods: heavily modified, do NOT upgrade!
    // non account: 1 * 1, no resume
    // free account: untested, set same as FREE
    // premium account: 1 * 10
    // protocol: redirects to https
    // captchatype: reCaptchaV2
    // other: OLD standard-JD User-Agent is blocked!
    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("http://", "https://"));
    }

    @Override
    public String getAGBLink() {
        return COOKIE_HOST + "/tos.html";
    }

    public NovaFileCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(COOKIE_HOST + "/premium.html");
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
    protected boolean useRUA() {
        return true;
    }

    @Override
    protected Browser prepBrowser(final Browser prepBr, final String host) {
        if (!(this.browserPrepped.containsKey(prepBr) && this.browserPrepped.get(prepBr) == Boolean.TRUE)) {
            super.prepBrowser(prepBr, host);
            /* define custom browser headers and language settings */
            prepBr.setCookie(COOKIE_HOST, "lang", "english");
        }
        return prepBr;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        getPage(link.getDownloadURL());
        if (new Regex(correctedBR, "(No such file|>File Not Found<|>The file was removed by|Reason (of|for) deletion:\n)").matches()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (new Regex(correctedBR, MAINTENANCE).matches()) {
            link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.xfilesharingprobasic.undermaintenance", MAINTENANCEUSERTEXT));
            return AvailableStatus.TRUE;
        }
        String[] fileInfo = new String[3];
        // scan the first page
        scanInfo(fileInfo);
        // scan the second page. filesize[1] and md5hash[2] are not mission
        // critical
        if (fileInfo[0] == null) {
            Form download1 = br.getFormByInputFieldKeyValue("op", "download1");
            if (download1 != null) {
                download1.remove("method_premium");
                submitForm(download1);
                scanInfo(fileInfo);
            }
        }
        if (fileInfo[0] == null || fileInfo[0].equals("")) {
            if (correctedBR.contains("You have reached the download(\\-| )limit")) {
                logger.warning("Waittime detected, please reconnect to make the linkchecker work!");
                return AvailableStatus.UNCHECKABLE;
            }
            logger.warning("filename equals null, throwing \"plugin defect\"");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (fileInfo[2] != null && !fileInfo[2].equals("")) {
            link.setMD5Hash(fileInfo[2].trim());
        }
        fileInfo[0] = fileInfo[0].replaceAll("(</b>|<b>|\\.html)", "");
        link.setName(Encoding.htmlDecode(fileInfo[0].trim()));
        if (fileInfo[1] != null && !fileInfo[1].equals("")) {
            link.setDownloadSize(SizeFormatter.getSize(fileInfo[1]));
        }
        return AvailableStatus.TRUE;
    }

    private String[] scanInfo(String[] fileInfo) {
        // standard traits from base page
        if (fileInfo[0] == null) {
            fileInfo[0] = new Regex(correctedBR, "You have requested.*?https?://(www\\.)?" + this.getHost() + "/[A-Za-z0-9]{12}/(.*?)</font>").getMatch(1);
            if (fileInfo[0] == null) {
                fileInfo[0] = new Regex(correctedBR, "fname\"( type=\"hidden\")? value=\"(.*?)\"").getMatch(1);
                if (fileInfo[0] == null) {
                    fileInfo[0] = new Regex(correctedBR, "<div class=\"name\">(.*?)</div>").getMatch(0);
                    if (fileInfo[0] == null) {
                        fileInfo[0] = new Regex(correctedBR, "Download File:? ?(<[^>]+> ?)+?([^<>\"\\']+)").getMatch(1);
                        // traits from download1 page below.
                        if (fileInfo[0] == null) {
                            fileInfo[0] = new Regex(correctedBR, "Filename:? ?(<[^>]+> ?)+?([^<>\"\\']+)").getMatch(1);
                            // next two are details from sharing box
                            if (fileInfo[0] == null) {
                                fileInfo[0] = new Regex(correctedBR, "copy\\(this\\);.+>(.+) \\- [\\d\\.]+ (KB|MB|GB)</a></textarea>[\r\n\t ]+</div>").getMatch(0);
                                if (fileInfo[0] == null) {
                                    fileInfo[0] = new Regex(correctedBR, "copy\\(this\\);.+\\](.+) \\- [\\d\\.]+ (KB|MB|GB)\\[/URL\\]").getMatch(0);
                                }
                            }
                        }
                    }
                }
            }
        }
        if (fileInfo[1] == null) {
            fileInfo[1] = new Regex(correctedBR, "\\(([0-9]+ bytes)\\)").getMatch(0);
            if (fileInfo[1] == null) {
                fileInfo[1] = new Regex(correctedBR, "</font>[ ]+\\(([^<>\"\\'/]+)\\)(.*?)</font>").getMatch(0);
                if (fileInfo[1] == null) {
                    fileInfo[1] = new Regex(correctedBR, "([\\d\\.]+ ?(KB|MB|GB))").getMatch(0);
                }
            }
        }
        if (fileInfo[2] == null) {
            fileInfo[2] = new Regex(correctedBR, "<b>MD5.*?</b>.*?nowrap>(.*?)<").getMatch(0);
        }
        return fileInfo;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, null, false, 1, "freelink");
    }

    public void doFree(final DownloadLink downloadLink, final Account account, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        String passCode = null;
        // First, bring up saved final links
        String dllink = checkDirectLink(downloadLink, directlinkproperty);
        // Second, check for streaming links on the first page
        if (dllink == null) {
            dllink = getDllink();
        }
        if (dllink == null) {
            Form dlForm = br.getFormByInputFieldKeyValue("op", "download2");
            if (dlForm == null) {
                if (dlForm == null) {
                    this.checkErrors(downloadLink, account, false, passCode);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            // how many forms deep do you want to try.
            int repeat = 3;
            for (int i = 1; i < repeat; i++) {
                dlForm.remove(null);
                final long timeBefore = System.currentTimeMillis();
                boolean password = false;
                if (new Regex(correctedBR, PASSWORDTEXT).matches()) {
                    password = true;
                    logger.info("The downloadlink seems to be password protected.");
                }
                // md5 can be on the subquent pages
                if (downloadLink.getMD5Hash() == null) {
                    String md5hash = new Regex(correctedBR, "<b>MD5.*?</b>.*?nowrap>(.*?)<").getMatch(0);
                    if (md5hash != null) {
                        downloadLink.setMD5Hash(md5hash.trim());
                    }
                }
                waitTime(timeBefore, downloadLink);
                /* Captcha START */
                if (br.containsHTML("g-recaptcha")) {
                    logger.info("Detected captcha method \"RecaptchaV2\" for this host");
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                    dlForm.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                } else if (correctedBR.contains(";background:#ccc;text-align")) {
                    logger.info("Detected captcha method \"plaintext captchas\" for this host");
                    /** Captcha method by ManiacMansion */
                    String[][] letters = new Regex(br, "<span style=\\'position:absolute;padding\\-left:(\\d+)px;padding\\-top:\\d+px;\\'>(&#\\d+;)</span>").getMatches();
                    if (letters == null || letters.length == 0) {
                        logger.warning("plaintext captchahandling broken!");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    SortedMap<Integer, String> capMap = new TreeMap<Integer, String>();
                    for (String[] letter : letters) {
                        capMap.put(Integer.parseInt(letter[0]), Encoding.htmlDecode(letter[1]));
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
                } else if (new Regex(correctedBR, regexRecaptcha).matches()) {
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
                    dlForm = rc.getForm(); /* 2017-04-26: Not skippable anymore */
                }
                /* Captcha END */
                if (password) {
                    passCode = handlePassword(passCode, dlForm, downloadLink);
                }
                submitForm(dlForm);
                logger.info("Submitted DLForm");
                checkErrors(downloadLink, account, true, passCode);
                dllink = getDllink();
                if (dllink == null && (br.getFormByInputFieldKeyValue("op", "download2") == null || i == repeat)) {
                    logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else if (dllink == null && (dlForm = br.getFormByInputFieldKeyValue("op", "download2")) != null) {
                    invalidateLastChallengeResponse();
                    continue;
                } else {
                    validateLastChallengeResponse();
                    break;
                }
            }
        }
        logger.info("Final downloadlink = " + dllink + " starting the download...");
        dllink = Encoding.urlEncode_light(dllink);
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getResponseCode() == 416) {
            logger.info("Resume failed --> Retrying from zero");
            downloadLink.setChunksProgress(null);
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            correctBR();
            checkServerErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty(directlinkproperty, dllink);
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        try {
            // add a download slot
            controlFree(+1);
            // start the dl
            dl.startDownload();
        } finally {
            // remove download slot
            controlFree(-1);
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

    public String getDllink() {
        String dllink = br.getRedirectLocation();
        if (dllink == null) {
            dllink = new Regex(correctedBR, "dotted #bbb;padding.*?<a href=\"(.*?)\"").getMatch(0);
            if (dllink == null) {
                dllink = new Regex(correctedBR, "This (direct link|download link) will be available for your IP.*?href=\"(http.*?)\"").getMatch(1);
                if (dllink == null) {
                    dllink = new Regex(correctedBR, "Download: <a href=\"(.*?)\"").getMatch(0);
                    if (dllink == null) {
                        dllink = new Regex(correctedBR, "<a href=\"(https?://[^\"]+)\"[^>]+>(Click to Download|Download File)").getMatch(0);
                        if (dllink == null) {
                            String cryptedScripts[] = new Regex(correctedBR, "p\\}\\((.*?)\\.split\\('\\|'\\)").getColumn(0);
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
                }
            }
        }
        return dllink;
    }

    @Override
    protected void getPage(String page) throws Exception {
        super.getPage(page);
        correctBR();
    }

    @Override
    protected void postPage(String page, String postdata) throws Exception {
        super.postPage(page, postdata);
        correctBR();
    }

    @Override
    protected void submitForm(Form form) throws Exception {
        super.submitForm(form);
        correctBR();
    }

    @Override
    protected void runPostRequestTask(Browser ibr) throws Exception {
        if (!ibr.isFollowingRedirects()) {
            final String redirect = ibr.getRedirectLocation();
            if (redirect != null) {
                // this effectively prevents redirect of direct downloadable content.
                if (!new Regex(redirect, "https?://(?:[\\w\\-\\.]+)(?::\\d{1,4})?/(?:files|d|cgi-bin/dl\\.cgi)/(\\d+/)?[a-z0-9]+/[^<>\"/]+").matches()) {
                    getPage(ibr, redirect);
                    return;
                }
            }
        }
        // they can show silly recaptcha challenge I believe at anytime within download routine.
        if (ibr.getRequest().getURL().getFile().matches("(?i)/\\?op=captcha&id=[a-z0-9]{12}")) {
            try {
                // only one form on page
                final Form captcha = br.getForm(0);
                final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                captcha.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                submitForm(captcha);
                return;
            } catch (final Exception e) {
                boolean isCaptcha;
                if (e instanceof PluginException && ((PluginException) e).getLinkStatus() == LinkStatus.ERROR_CAPTCHA) {
                    isCaptcha = true;
                } else if (e instanceof SkipException || e instanceof CaptchaException) {
                    isCaptcha = true;
                } else {
                    isCaptcha = false;
                }
                if (isCaptcha && accountType != null) {
                    throw new AccountInvalidException(e, "'imNotARobot' captcha was not answered");
                } else {
                    throw e;
                }
            }
        }
    }

    public void checkErrors(final DownloadLink theLink, final Account account, boolean checkAll, final String passCode) throws NumberFormatException, PluginException {
        if (account != null) {
            synchronized (LOCK) {
                final String hours = new Regex(correctedBR, "class=\"error_page\">\\s*You've used \\d+ different IPs to download in last \\d+ hours\\. You're not allowed to download for (\\d+) hours\\.").getMatch(0);
                // recommend to users that they disable IP reconnection!
                if (hours != null) {
                    throw new AccountUnavailableException("Account Disabled due to logging in too many times from different IP address over short period", Long.parseLong(hours) * 60 * 60 * 1001l);
                }
                if (br.containsHTML("class=\"error_page\">\\s*You can't access this account from your IP\\.<br>")) {
                    throw new AccountInvalidException("You can't access this account from your current IP Address");
                }
            }
            if (theLink == null) {
                return;
            }
        }
        if (checkAll) {
            if (new Regex(correctedBR, PASSWORDTEXT).matches() || correctedBR.contains("Wrong password")) {
                logger.warning("Wrong password, the entered password \"" + passCode + "\" is wrong, retrying...");
                throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
            }
            if (correctedBR.contains("Wrong captcha")) {
                logger.warning("Wrong captcha or wrong password!");
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            if (correctedBR.contains("\">\\s*?Skipped countdown\\s*?<")) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Fatal countdown error (countdown skipped)");
            }
        }
        /** Wait time reconnect handling */
        if (new Regex(correctedBR, "(You have reached the download(\\-| )limit|You have to wait)").matches()) {
            // adjust this regex to catch the wait time string for COOKIE_HOST
            String WAIT = new Regex(correctedBR, "((You have reached the download(\\-| )limit|You have to wait)[^<>]+)").getMatch(0);
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
        if (new Regex(correctedBR, "(can only download| can download files up to |Upgrade your account to download bigger files|>Upgrade your account to download larger files|>The file you requested reached max downloads limit for Free Users|Please Buy Premium To download this file<|This file reached max downloads limit|This file can only be downloaded by Premium|<div id=\"premium-only\">)").matches()) {
            String filesizelimit = new Regex(correctedBR, "You can download files up to(.*?)only").getMatch(0);
            if (filesizelimit == null) {
                filesizelimit = new Regex(correctedBR, "Free Users can only download files sized up to(.*?)\\.<").getMatch(0);
            }
            if (filesizelimit != null) {
                filesizelimit = filesizelimit.trim();
                logger.warning("As free user you can download files up to " + filesizelimit + " only");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } else {
                logger.warning("Only downloadable via premium");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
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
                    finallink = new Regex(decoded, "\\.addVariable\\(\\'file\\',\\'(https?://.*?)\\'\\)").getMatch(0);
                }
            }
        }
        return finallink;
    }

    private String handlePassword(String passCode, Form pwform, DownloadLink thelink) throws IOException, PluginException {
        passCode = thelink.getStringProperty("pass", null);
        if (passCode == null) {
            passCode = Plugin.getUserInput("Password?", thelink);
        }
        pwform.put("password", passCode);
        logger.info("Put password \"" + passCode + "\" entered by user in the DLForm.");
        return Encoding.urlEncode(passCode);
    }

    private String checkDirectLink(DownloadLink downloadLink, String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                Browser br2 = br.cloneBrowser();
                con = br2.openGetConnection(dllink);
                /* 2017-05-06: Detection for bad 7b file has been added */
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1 || con.getLongContentLength() == 7) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return dllink;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        br = new Browser();
        // reset value
        account.setProperty("PROPERTY_TEMP_DISABLED_TIMEOUT", Property.NULL);
        AccountInfo ai = new AccountInfo();
        login(account, true);
        String space[][] = new Regex(correctedBR, "<td>Used space:</td>.*?<td.*?b>([0-9\\.]+) of [0-9\\.]+ (KB|MB|GB|TB)</b>").getMatches();
        if ((space != null && space.length != 0) && (space[0][0] != null && space[0][1] != null)) {
            ai.setUsedSpace(space[0][0] + " " + space[0][1]);
        }
        account.setValid(true);
        String availabletraffic = new Regex(correctedBR, ">Traffic Available:</td>[\r\n\t ]+<td>([\\-\\d\\.]+ (MB|GB))</td>").getMatch(0);
        if (availabletraffic != null && !availabletraffic.contains("nlimited") && !availabletraffic.equalsIgnoreCase(" Mb")) {
            availabletraffic.trim();
            // need to set 0 traffic left, as getSize returns a positive result,
            // even when negative value supplied.
            if (!availabletraffic.startsWith("-")) {
                ai.setTrafficLeft(SizeFormatter.getSize(availabletraffic));
            } else {
                ai.setTrafficLeft(0);
            }
            ai.setTrafficLeft(SizeFormatter.getSize(availabletraffic));
        } else {
            ai.setUnlimitedTraffic();
        }
        String expire = new Regex(correctedBR, ">Premium expires:</td>.+<div>(\\d{1,2} [A-Za-z]+ \\d{4})</div>").getMatch(0);
        if (expire == null) {
            expire = new Regex(correctedBR, "(\\d{1,2} [A-Za-z]+ \\d{4})").getMatch(0);
        }
        if (expire != null) {
            expire = expire.replaceAll("(<b>|</b>)", "");
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd MMMM yyyy", null) + (24 * 60 * 60 * 1000l));
            account.setMaxSimultanDownloads(10);
            account.setConcurrentUsePossible(false);
            account.setType(AccountType.PREMIUM);
        }
        if (ai.isExpired() || expire == null) {
            account.setMaxSimultanDownloads(1);
            account.setConcurrentUsePossible(false);
            account.setType(AccountType.FREE);
        }
        return ai;
    }

    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /** Load cookies */
                br.setCookiesExclusive(true);
                /* Load cookies */
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    br.setCookies(this.getHost(), cookies);
                    if (!force) {
                        return;
                    }
                    getPage(COOKIE_HOST + "/?op=my_account");
                    if (validateAccountCookies() && !br._getURL().getPath().equals("/login")) {
                        return;
                    }
                    br.setCookiesExclusive(true);
                }
                br.setFollowRedirects(true);
                getPage(COOKIE_HOST + "/login");
                String postData = "op=login&redirect=&rand=&login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass());
                final int repeat = 3;
                for (int i = 0; i <= repeat; i++) {
                    if (br.containsHTML(regexRecaptcha)) {
                        final Recaptcha rc = new Recaptcha(br, this);
                        rc.findID();
                        rc.load();
                        final DownloadLink dummyLink = new DownloadLink(this, "Account", "novafile.com", "http://novafile.com", true);
                        final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                        final String c = getCaptchaCode("recaptcha", cf, dummyLink);
                        postData += "&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c);
                        final String rand = br.getRegex("name=\"rand\" value=\"([^<>\"]*?)\"").getMatch(0);
                        if (rand != null) {
                            postData += "&rand=" + rand;
                        }
                    }
                    postPage(COOKIE_HOST + "/login", postData);
                    checkErrors(null, account, false, null);
                    // no point doing another post request when password is incorrect... only captcha!
                    if (new Regex(correctedBR, ">\\s*Incorrect Login or Password\\s*<").matches()) {
                        invalidAccountException();
                    }
                    if (!br.containsHTML(regexRecaptcha)) {
                        break;
                    } else if (br.containsHTML(regexRecaptcha) && i + 1 >= repeat) {
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    } else {
                        continue;
                    }
                }
                br.setFollowRedirects(false);
                if (!validateAccountCookies() && !br._getURL().getPath().equals("/login")) {
                    invalidAccountException();
                }
                if (!br.getRequest().getURL().getFile().equals("/?op=my_account")) {
                    getPage("/?op=my_account");
                }
                if (!new Regex(correctedBR, "(Premium[- ](Account )?expires|>Renew premium<)").matches()) {
                    account.setType(AccountType.FREE);
                } else {
                    account.setType(AccountType.PREMIUM);
                }
                account.saveCookies(br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    private void invalidAccountException() throws PluginException {
        final String lang = System.getProperty("user.language");
        if ("de".equalsIgnoreCase(lang)) {
            throw new AccountInvalidException("Ungültiger Benutzername, Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!");
        } else {
            throw new AccountInvalidException("Invalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!");
        }
    }

    private boolean validateAccountCookies() {
        final String xfss = br.getCookie(COOKIE_HOST, "xfss");
        if (xfss != null && !("".equals(xfss) || "deleted".equals(xfss))) {
            return true;
        }
        return false;
    }

    private final String regexRecaptcha = "api\\.recaptcha\\.net|google\\.com/recaptcha/api/";
    private AccountType  accountType    = null;

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        String passCode = null;
        requestFileInformation(link);
        login(account, false);
        accountType = account.getType();
        br.setFollowRedirects(false);
        String dllink = null;
        if (AccountType.FREE.equals(account.getType())) {
            getPage(link.getDownloadURL());
            doFree(link, account, false, 1, "freelink2");
        } else {
            dllink = checkDirectLink(link, "premlink");
            if (dllink == null) {
                getPage(link.getDownloadURL());
                dllink = getDllink();
                if (dllink == null) {
                    checkErrors(link, account, true, passCode);
                    Form dlform = br.getFormbyProperty("name", "F1");
                    if (dlform == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    if (new Regex(correctedBR, PASSWORDTEXT).matches()) {
                        passCode = handlePassword(passCode, dlform, link);
                    }
                    submitForm(dlform);
                    dllink = getDllink();
                    checkErrors(link, account, true, passCode);
                }
            }
            if (dllink == null) {
                logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            logger.info("Final downloadlink = " + dllink + " starting the download...");
            dllink = Encoding.urlEncode_light(dllink);
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, true, 1);
            if (dl.getConnection().getResponseCode() == 416) {
                logger.info("Resume failed --> Retrying from zero");
                link.setChunksProgress(null);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            if (dl.getConnection().getContentType().contains("html")) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                correctBR();
                checkServerErrors();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (passCode != null) {
                link.setProperty("pass", passCode);
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

    private void waitTime(long timeBefore, DownloadLink downloadLink) throws PluginException {
        int passedTime = (int) ((System.currentTimeMillis() - timeBefore) / 1000) - 1;
        /** Ticket Time */
        String ttt = new Regex(correctedBR, "id=\"countdown_str\">[^<>\"]+<span id=\"[^<>\"]+\"( class=\"[^<>\"]+\")?>([\n ]+)?(\\d+)([\n ]+)?</span>").getMatch(2);
        if (ttt == null) {
            ttt = new Regex(correctedBR, ">(\\d+)</span> seconds</p>").getMatch(0);
        }
        if (ttt == null) {
            /* 2017-04-26 */
            ttt = new Regex(correctedBR, "class=\"alert\\-success\\-invert\">(\\d+)<").getMatch(0);
        }
        if (ttt != null) {
            int tt = Integer.parseInt(ttt);
            tt -= passedTime;
            logger.info("Waittime detected, waiting " + ttt + " - " + passedTime + " seconds from now on...");
            if (tt > 0) {
                sleep(tt * 1000l, downloadLink);
            }
        }
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.SibSoft_XFileShare;
    }
}
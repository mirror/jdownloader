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

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.parser.html.InputField;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.SiteType.SiteTemplate;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.keycaptcha.KeyCaptcha;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "subyshare.com" }, urls = { "https?://(?:www\\.)?subyshare\\.com/(?:vidembed\\-)?[a-z0-9]{12}" })
public class SubyShareCom extends PluginForHost {
    private String                         correctedBR                  = "";
    private String                         passCode                     = null;
    private static final String            PASSWORDTEXT                 = "<br><b>Passwor(d|t):</b> <input";
    /* primary website url, take note of redirects */
    private static final String            COOKIE_HOST                  = "https://subyshare.com";
    private static final String            NICE_HOST                    = COOKIE_HOST.replaceAll("(https://|http://)", "");
    private static final String            NICE_HOSTproperty            = COOKIE_HOST.replaceAll("(https://|http://|\\.|-)", "");
    /* domain names used within download links */
    private static final String            DOMAINS                      = "(subyshare\\.com)";
    private static final String            MAINTENANCE                  = ">This server is in maintenance mode";
    private static final String            MAINTENANCEUSERTEXT          = JDL.L("hoster.xfilesharingprobasic.errors.undermaintenance", "This server is under maintenance");
    private static final String            ALLWAIT_SHORT                = JDL.L("hoster.xfilesharingprobasic.errors.waitingfordownloads", "Waiting till new downloads can be started");
    private static final String            PREMIUMONLY1                 = JDL.L("hoster.xfilesharingprobasic.errors.premiumonly1", "Max downloadable filesize for free users:");
    private static final String            PREMIUMONLY2                 = JDL.L("hoster.xfilesharingprobasic.errors.premiumonly2", "Only downloadable via premium or free account");
    // disabled for the moment because it causes high load on servers because they tried to fix instant load workaround bug
    private static final boolean           VIDEOHOSTER                  = false;
    private static final boolean           VIDEOHOSTER_2                = false;
    private static final boolean           SUPPORTSHTTPS                = true;
    private static final boolean           ENFORCESHTTPS                = true;
    private final boolean                  ENABLE_RANDOM_UA             = true;
    private static AtomicReference<String> agent                        = new AtomicReference<String>(null);
    /* Connection stuff */
    private static final boolean           FREE_RESUME                  = false;
    private static final int               FREE_MAXCHUNKS               = 1;
    private static final int               FREE_MAXDOWNLOADS            = 1;
    /* note: CAN NOT be negative or zero! (ie. -1 or 0) Otherwise math sections fail. .:. use [1-20] */
    private static AtomicInteger           totalMaxSimultanFreeDownload = new AtomicInteger(FREE_MAXDOWNLOADS);
    /* don't touch the following! */
    private static AtomicInteger           maxFree                      = new AtomicInteger(1);
    private static Object                  LOCK                         = new Object();
    private String                         fuid                         = null;

    /* DEV NOTES */
    // XfileSharingProBasic Version 2.6.6.1
    // mods: heavily modified, do NOT upgrade!
    // limit-info:
    // protocol: no https
    // captchatype: reCaptchaV2
    // other:
    @Override
    public void correctDownloadLink(final DownloadLink link) {
        /* link cleanup, but respect users protocol choosing */
        if (SUPPORTSHTTPS && ENFORCESHTTPS) {
            link.setUrlDownload(link.getDownloadURL().replaceFirst("http://", "https://"));
        } else if (!SUPPORTSHTTPS) {
            link.setUrlDownload(link.getDownloadURL().replaceFirst("https://", "http://"));
        }
        final String fid = new Regex(link.getDownloadURL(), "([a-z0-9]{12})$").getMatch(0);
        link.setUrlDownload(COOKIE_HOST + "/" + fid);
    }

    @Override
    public String getAGBLink() {
        return COOKIE_HOST + "/tos.html";
    }

    public SubyShareCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(COOKIE_HOST + "/premium.html");
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        br.setFollowRedirects(true);
        prepBrowser(br);
        setFUID(link);
        getPage(link.getDownloadURL());
        if (antiddosEnforced()) {
            if (!isDownload) {
                link.getLinkStatus().setStatusText("Captcha event present within linkchecking");
                return AvailableStatus.UNCHECKABLE;
            }
            // recaptchaV2 event!
            logger.info("Detected captcha method \"reCaptchaV2\"");
            final Form form = br.getFormbyProperty("id", "checkDDOS");
            final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
            form.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            sendForm(form);
            getPage(link.getDownloadURL());
            if (antiddosEnforced()) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
        }
        if (br.getRequest().getHttpConnection().getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "IP blocked by service");
        }
        if (new Regex(correctedBR, "(No such file|>File Not Found<|>The file was removed by|Reason for deletion:\n|<Title>Download </Title>)").matches()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (new Regex(correctedBR, MAINTENANCE).matches()) {
            link.getLinkStatus().setStatusText(MAINTENANCEUSERTEXT);
            return AvailableStatus.UNCHECKABLE;
        }
        if (br.getURL().contains("/?op=login&redirect=")) {
            link.getLinkStatus().setStatusText(PREMIUMONLY2);
            return AvailableStatus.UNCHECKABLE;
        }
        final String[] fileInfo = new String[3];
        scanInfo(fileInfo);
        if (fileInfo[0] == null || fileInfo[0].equals("")) {
            if (correctedBR.contains("You have reached the download(-| )limit")) {
                logger.warning("Waittime detected, please reconnect to make the linkchecker work!");
                return AvailableStatus.UNCHECKABLE;
            }
            if (br.containsHTML("You need upgrade to premium account before download") || br.getURL().contains("/predownload")) {
                if (!isDownload) {
                    return AvailableStatus.UNCHECKABLE;
                }
                if (account != null) {
                    return AvailableStatus.UNCHECKABLE;
                }
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (fileInfo[2] != null && !fileInfo[2].equals("")) {
            link.setMD5Hash(fileInfo[2].trim());
        }
        fileInfo[0] = fileInfo[0].replaceAll("(</b>|<b>|\\.html)", "");
        link.setName(fileInfo[0].trim());
        if (fileInfo[1] != null && !fileInfo[1].equals("")) {
            link.setDownloadSize(SizeFormatter.getSize(fileInfo[1]));
        }
        return AvailableStatus.TRUE;
    }

    private boolean antiddosEnforced() {
        final Form f = br.getFormbyProperty("id", "checkDDOS");
        final boolean captchaEvent = br.containsHTML("<center><h1>Your browser is computing access to [\\w.]+</h1></center>");
        return f != null && captchaEvent ? true : false;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, null, false);
    }

    private String[] scanInfo(final String[] fileInfo) {
        /* standard traits from base page */
        if (fileInfo[0] == null) {
            fileInfo[0] = new Regex(correctedBR, "You have requested.*?https?://(www\\.)?" + DOMAINS + "/" + fuid + "/(.*?)</font>").getMatch(2);
            if (fileInfo[0] == null) {
                fileInfo[0] = new Regex(correctedBR, "fname\"( type=\"hidden\")? value=\"(.*?)\"").getMatch(1);
                if (fileInfo[0] == null) {
                    fileInfo[0] = new Regex(correctedBR, "<h2>Download File(.*?)</h2>").getMatch(0);
                    /* traits from download1 page below */
                    if (fileInfo[0] == null) {
                        fileInfo[0] = new Regex(correctedBR, "Filename:? ?(<[^>]+> ?)+?([^<>\"']+)").getMatch(1);
                        // next two are details from sharing box
                        if (fileInfo[0] == null) {
                            fileInfo[0] = new Regex(correctedBR, "copy\\(this\\);.+>(.+) - [\\d\\.]+ (KB|MB|GB)</a></textarea>[\r\n\t ]+</div>").getMatch(0);
                            if (fileInfo[0] == null) {
                                fileInfo[0] = new Regex(correctedBR, "copy\\(this\\);.+\\](.+) - [\\d\\.]+ (KB|MB|GB)\\[/URL\\]").getMatch(0);
                                if (fileInfo[0] == null) {
                                    /* Link of the box without filesize */
                                    fileInfo[0] = new Regex(correctedBR, "onFocus=\"copy\\(this\\);\">http://(www\\.)?" + DOMAINS + "/" + fuid + "/([^<>\"]*?)</textarea").getMatch(2);
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
                fileInfo[1] = new Regex(correctedBR, "</font>[ ]+\\(([^<>\"'/]+)\\)(.*?)</font>").getMatch(0);
                if (fileInfo[1] == null) {
                    fileInfo[1] = new Regex(correctedBR, "(\\d+(\\.\\d+)? ?(KB|MB|GB))").getMatch(0);
                }
            }
        }
        if (fileInfo[2] == null) {
            fileInfo[2] = new Regex(correctedBR, "<b>MD5.*?</b>.*?nowrap>(.*?)<").getMatch(0);
        }
        return fileInfo;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink, null, true);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "freelink");
    }

    @SuppressWarnings("unused")
    public void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        if (checkShowFreeDialog(getHost())) {
            super.showFreeDialog(getHost());
        }
        checkForPremiumonlyNew();
        br.setFollowRedirects(false);
        passCode = downloadLink.getStringProperty("pass");
        /* First, bring up saved final links */
        String dllink = checkDirectLink(downloadLink, directlinkproperty);
        /* Second, check for streaming/direct links on the first page */
        if (dllink == null) {
            dllink = getDllink();
        }
        /* Third, do they provide video hosting? */
        if (dllink == null) {
            dllink = handleVideoembed1(dllink);
        }
        if (dllink == null && VIDEOHOSTER) {
            try {
                logger.info("Trying to get link via vidembed");
                final Browser brv = br.cloneBrowser();
                brv.getPage("/vidembed-" + fuid);
                dllink = brv.getRedirectLocation();
                if (dllink == null) {
                    logger.info("Failed to get link via vidembed");
                } else {
                    logger.info("Successfully found link via vidembed");
                }
            } catch (final Throwable e) {
                logger.info("Failed to get link via vidembed");
            }
        }
        if (dllink == null && VIDEOHOSTER_2) {
            try {
                logger.info("Trying to get link via embed");
                final String embed_access = "http://" + COOKIE_HOST.replace("http://", "") + "/embed-" + fuid + ".html";
                this.postPage(embed_access, "op=video_embed3&usr_login=&id2=" + fuid + "&fname=" + Encoding.urlEncode(downloadLink.getName()) + "&referer=&file_code=" + fuid + "&method_free=Click+here+to+watch+the+Video");
                // brv.getPage("http://grifthost.com/embed-" + fuid + ".html");
                dllink = getDllink();
                if (dllink == null) {
                    logger.info("Failed to get link via embed");
                } else {
                    logger.info("Successfully found link via embed");
                }
            } catch (final Throwable e) {
                logger.info("Failed to get link via embed");
            }
            if (dllink == null) {
                getPage(downloadLink.getDownloadURL());
            }
        }
        /* Fourth, continue like normal */
        if (dllink == null) {
            checkErrors(downloadLink, false);
            final Form download1 = getFormByKey("op", "download1");
            if (download1 != null) {
                download1.remove("method_premium");
                sendForm(download1);
                checkErrors(downloadLink, false);
                dllink = getDllink();
                if (dllink == null) {
                    checkErrors(downloadLink, false);
                }
            }
        }
        if (dllink == null) {
            Form dlForm = br.getFormbyProperty("name", "F1");
            if (dlForm == null) {
                handlePluginBroken(downloadLink, "dlform_f1_null", 3);
            }
            /* how many forms deep do you want to try? */
            int repeat = 2;
            for (int i = 0; i <= repeat; i++) {
                dlForm.remove(null);
                final long timeBefore = System.currentTimeMillis();
                boolean password = false;
                boolean skipWaittime = false;
                if (new Regex(correctedBR, PASSWORDTEXT).matches()) {
                    password = true;
                    logger.info("The downloadlink seems to be password protected.");
                }
                /* md5 can be on the subsequent pages - it is to be found very rare in current XFS versions */
                if (downloadLink.getMD5Hash() == null) {
                    String md5hash = new Regex(correctedBR, "<b>MD5.*?</b>.*?nowrap>(.*?)<").getMatch(0);
                    if (md5hash != null) {
                        downloadLink.setMD5Hash(md5hash.trim());
                    }
                }
                /* Captcha START */
                if (correctedBR.contains(";background:#ccc;text-align")) {
                    logger.info("Detected captcha method \"plaintext captchas\" for this host");
                    /* Captcha method by ManiacMansion */
                    final String[][] letters = new Regex(br, "<span style='position:absolute;padding-left:(\\d+)px;padding-top:\\d+px;'>(&#\\d+;)</span>").getMatches();
                    if (letters == null || letters.length == 0) {
                        logger.warning("plaintext captchahandling broken!");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final SortedMap<Integer, String> capMap = new TreeMap<Integer, String>();
                    for (String[] letter : letters) {
                        capMap.put(Integer.parseInt(letter[0]), Encoding.htmlDecode(letter[1]));
                    }
                    final StringBuilder code = new StringBuilder();
                    for (String value : capMap.values()) {
                        code.append(value);
                    }
                    dlForm.put("code", code.toString());
                    logger.info("Put captchacode " + code.toString() + " obtained by captcha metod \"plaintext captchas\" in the form.");
                } else if (correctedBR.contains("/captchas/")) {
                    logger.info("Detected captcha method \"Standard captcha\" for this host");
                    final String[] sitelinks = HTMLParser.getHttpLinks(br.toString(), null);
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
                    rc.findID();
                    rc.load();
                    final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    final String c = getCaptchaCode("recaptcha", cf, downloadLink);
                    dlForm.put("recaptcha_challenge_field", rc.getChallenge());
                    dlForm.put("recaptcha_response_field", Encoding.urlEncode(c));
                    logger.info("Put captchacode " + c + " obtained by captcha metod \"Re Captcha\" in the form and submitted it.");
                    /* wait time is usually skippable for reCaptcha handling */
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
                    dlForm.put("adcopy_challenge", Encoding.urlEncode(chid));
                    dlForm.put("adcopy_response", "manual_challenge");
                } else if (br.containsHTML("id=\"capcode\" name= \"capcode\"")) {
                    logger.info("Detected captcha method \"keycaptca\"");
                    String result = handleCaptchaChallenge(getDownloadLink(), new KeyCaptcha(this, br, getDownloadLink()).createChallenge(this));
                    if (result == null) {
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    }
                    if ("CANCEL".equals(result)) {
                        throw new PluginException(LinkStatus.ERROR_FATAL);
                    }
                    dlForm.put("capcode", result);
                    skipWaittime = false;
                } else if (br.containsHTML("class=\"g-recaptcha\"")) {
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                    dlForm.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                    skipWaittime = true;
                }
                /* Captcha END */
                if (password) {
                    passCode = handlePassword(dlForm, downloadLink);
                }
                if (!skipWaittime) {
                    waitTime(timeBefore, downloadLink);
                }
                sendForm(dlForm);
                logger.info("Submitted DLForm");
                checkErrors(downloadLink, true);
                dllink = getDllink();
                if (dllink == null && (!br.containsHTML("<Form name=\"F1\" method=\"POST\" action=\"\"") || i == repeat)) {
                    logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else if (dllink == null && br.containsHTML("<Form name=\"F1\" method=\"POST\" action=\"\"")) {
                    dlForm = br.getFormbyProperty("name", "F1");
                    continue;
                } else {
                    break;
                }
            }
        }
        logger.info("Final downloadlink = " + dllink + " starting the download...");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (!isValidDownloadConnection(dl.getConnection())) {
            if (dl.getConnection().getResponseCode() == 503) {
                dl.getConnection();
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Connection limit reached, please contact our support!", 5 * 60 * 1000l);
            }
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            correctBR();
            checkServerErrors();
            handlePluginBroken(downloadLink, "dllinknofile", 3);
        }
        downloadLink.setProperty(directlinkproperty, dllink);
        fixFilename(downloadLink);
        try {
            /* add a download slot */
            controlFree(+1);
            /* start the dl */
            dl.startDownload();
        } finally {
            /* remove download slot */
            controlFree(-1);
        }
    }

    private String handleVideoembed1(String dllink) {
        if (VIDEOHOSTER) {
            try {
                logger.info("Trying to get link via vidembed");
                final Browser brv = br.cloneBrowser();
                brv.getPage("/vidembed-" + fuid);
                dllink = brv.getRedirectLocation();
                if (dllink == null) {
                    logger.info("Failed to get link via vidembed");
                } else {
                    logger.info("Successfully found link via vidembed");
                }
            } catch (final Throwable e) {
                logger.info("Failed to get link via vidembed");
            }
        }
        return dllink;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return maxFree.get();
    }

    /* do not add @Override here to keep 0.* compatibility */
    public boolean hasAutoCaptcha() {
        return true;
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (AccountType.FREE.equals(acc.getType())) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }

    private void prepBrowser(final Browser br) {
        /* define custom browser headers and language settings */
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        br.setCookie(COOKIE_HOST, "lang", "english");
        br.setCookie(COOKIE_HOST, "is_checkddos", "1");
        if (ENABLE_RANDOM_UA) {
            if (agent.get() == null) {
                /* we first have to load the plugin, before we can reference it */
                JDUtilities.getPluginForHost("mediafire.com");
                agent.set(jd.plugins.hoster.MediafireCom.stringUserAgent());
            }
            br.getHeaders().put("User-Agent", agent.get());
        }
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
    public synchronized void controlFree(final int num) {
        logger.info("maxFree was = " + maxFree.get());
        maxFree.set(Math.min(Math.max(1, maxFree.addAndGet(num)), totalMaxSimultanFreeDownload.get()));
        logger.info("maxFree now = " + maxFree.get());
    }

    /* Removes HTML code which could break the plugin */
    public void correctBR() throws NumberFormatException, PluginException {
        correctedBR = br.toString();
        ArrayList<String> regexStuff = new ArrayList<String>();
        // remove custom rules first!!! As html can change because of generic cleanup rules.
        /* generic cleanup */
        regexStuff.add("<\\!(--.*?--)>");
        regexStuff.add("(display: ?none;\">.*?</div>)");
        regexStuff.add("(visibility:hidden>.*?<)");
        for (String aRegex : regexStuff) {
            String results[] = new Regex(correctedBR, aRegex).getColumn(0);
            if (results != null) {
                for (String result : results) {
                    correctedBR = correctedBR.replace(result, "");
                }
            }
        }
    }

    public String getDllink() {
        String dllink = br.getRedirectLocation();
        if (dllink == null) {
            dllink = new Regex(correctedBR, "(\"|')(https?://(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|([\\w\\-\\.]+\\.)?" + DOMAINS + ")(:\\d{1,4})?/(files|d|cgi-bin/dl\\.cgi)/(\\d+/)?[a-z0-9]+/[^<>\"/]*?)\\1").getMatch(1);
            if (dllink == null) {
                /* Special: Usually for premium mode */
                final String ret = new Regex(correctedBR, "Location: (https?://[^/]+/d/[A-Za-z0-9]+/[^/]+)").getMatch(1);
                if (ret != null && !getSupportedLinks().matcher(ret).matches()) {
                    dllink = ret;
                }
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
        return dllink;
    }

    private String decodeDownloadLink(final String s) {
        String decoded = null;
        try {
            Regex params = new Regex(s, "'(.*?[^\\\\])',(\\d+),(\\d+),'(.*?)'");
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
            /* Open regex is possible because in the unpacked JS there are usually only 1 links */
            finallink = new Regex(decoded, "(\"|')(https?://[^<>\"']*?\\.(?:avi|flv|mkv|mp4))\\1").getMatch(1);
        }
        return finallink;
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        final String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openGetConnection(dllink);
                if (isValidDownloadConnection(con)) {
                    return dllink;
                }
            } catch (final Exception e) {
                logger.log(e);
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
            downloadLink.setProperty(property, Property.NULL);
        }
        return null;
    }

    private void getPage(String page) throws Exception {
        if (SUPPORTSHTTPS && ENFORCESHTTPS) {
            page = page.replaceFirst("http://", "https://");
        } else if (!SUPPORTSHTTPS) {
            page = page.replaceFirst("https://", "http://");
        }
        br.getPage(page);
        correctBR();
    }

    private void postPage(String page, final String postdata) throws Exception {
        if (SUPPORTSHTTPS && ENFORCESHTTPS) {
            page = page.replaceFirst("http://", "https://");
        } else if (!SUPPORTSHTTPS) {
            page = page.replaceFirst("https://", "http://");
        }
        br.postPage(page, postdata);
        correctBR();
    }

    private void sendForm(final Form form) throws Exception {
        br.submitForm(form);
        correctBR();
    }

    private void waitTime(long timeBefore, final DownloadLink downloadLink) throws PluginException {
        int passedTime = (int) ((System.currentTimeMillis() - timeBefore) / 1000) - 1;
        /** Ticket Time */
        final String ttt = new Regex(correctedBR, "id=\"countdown\">[^<>\"]+<[^<>]+>(\\d+)<").getMatch(0);
        if (ttt == null) {
            logger.warning("Wait time regex failed.");
        } else {
            int wait = Integer.parseInt(ttt);
            wait -= passedTime;
            logger.info("[Seconds] Waittime on the page: " + ttt);
            logger.info("[Seconds] Passed time: " + passedTime);
            logger.info("[Seconds] Total time to wait: " + wait);
            if (wait > 0) {
                sleep(wait * 1000l, downloadLink);
            }
        }
    }

    // TODO: remove this when v2 becomes stable. use br.getFormbyKey(String key, String value)
    /**
     * Returns the first form that has a 'key' that equals 'value'.
     *
     * @param key
     * @param value
     * @return
     */
    private Form getFormByKey(final String key, final String value) {
        Form[] workaround = br.getForms();
        if (workaround != null) {
            for (Form f : workaround) {
                for (InputField field : f.getInputFields()) {
                    if (key != null && key.equals(field.getKey())) {
                        if (value == null && field.getValue() == null) {
                            return f;
                        }
                        if (value != null && value.equals(field.getValue())) {
                            return f;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Validates string to series of conditions, null, whitespace, or "". This saves effort factor within if/for/while statements
     *
     * @param s
     *            Imported String to match against.
     * @return <b>true</b> on valid rule match. <b>false</b> on invalid rule match.
     * @author raztoki
     */
    private boolean inValidate(final String s) {
        if (s == null || s != null && (s.matches("[\r\n\t ]+") || s.equals(""))) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * This fixes filenames from all xfs modules: file hoster, audio/video streaming (including transcoded video), or blocked link checking
     * which is based on fuid.
     *
     * @version 0.2
     * @author raztoki
     */
    private void fixFilename(final DownloadLink downloadLink) {
        String orgName = null;
        String orgExt = null;
        String servName = null;
        String servExt = null;
        String orgNameExt = downloadLink.getFinalFileName();
        if (orgNameExt == null) {
            orgNameExt = downloadLink.getName();
        }
        if (!inValidate(orgNameExt) && orgNameExt.contains(".")) {
            orgExt = orgNameExt.substring(orgNameExt.lastIndexOf("."));
        }
        if (!inValidate(orgExt)) {
            orgName = new Regex(orgNameExt, "(.+)" + orgExt).getMatch(0);
        } else {
            orgName = orgNameExt;
        }
        // if (orgName.endsWith("...")) orgName = orgName.replaceFirst("\\.\\.\\.$", "");
        String servNameExt = Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection()));
        if (!inValidate(servNameExt) && servNameExt.contains(".")) {
            servExt = servNameExt.substring(servNameExt.lastIndexOf("."));
            servName = new Regex(servNameExt, "(.+)" + servExt).getMatch(0);
        } else {
            servName = servNameExt;
        }
        String FFN = null;
        if (orgName.equalsIgnoreCase(fuid.toLowerCase())) {
            FFN = servNameExt;
        } else if (inValidate(orgExt) && !inValidate(servExt) && (servName.toLowerCase().contains(orgName.toLowerCase()) && !servName.equalsIgnoreCase(orgName))) {
            /*
             * when partial match of filename exists. eg cut off by quotation mark miss match, or orgNameExt has been abbreviated by hoster
             */
            FFN = servNameExt;
        } else if (!inValidate(orgExt) && !inValidate(servExt) && !orgExt.equalsIgnoreCase(servExt)) {
            FFN = orgName + servExt;
        } else {
            FFN = orgNameExt;
        }
        downloadLink.setFinalFileName(FFN);
    }

    private void setFUID(final DownloadLink dl) {
        fuid = new Regex(dl.getDownloadURL(), "([a-z0-9]{12})$").getMatch(0);
    }

    private String handlePassword(final Form pwform, final DownloadLink thelink) throws PluginException {
        if (passCode == null) {
            passCode = Plugin.getUserInput("Password?", thelink);
        }
        if (passCode == null || passCode.equals("")) {
            logger.info("User has entered blank password, exiting handlePassword");
            passCode = null;
            thelink.setProperty("pass", Property.NULL);
            return null;
        }
        if (pwform == null) {
            /* so we know handlePassword triggered without any form */
            logger.info("Password Form == null");
        } else {
            logger.info("Put password \"" + passCode + "\" entered by user in the DLForm.");
            pwform.put("password", Encoding.urlEncode(passCode));
        }
        thelink.setProperty("pass", passCode);
        return passCode;
    }

    public void checkErrors(final DownloadLink theLink, final boolean checkAll) throws NumberFormatException, PluginException {
        if (checkAll) {
            if (new Regex(correctedBR, PASSWORDTEXT).matches() && correctedBR.contains("Wrong password")) {
                /* handle password has failed in the past, additional try catching / resetting values */
                logger.warning("Wrong password, the entered password \"" + passCode + "\" is wrong, retrying...");
                passCode = null;
                theLink.setProperty("pass", Property.NULL);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
            }
            if (correctedBR.contains("Wrong captcha")) {
                logger.warning("Wrong captcha or wrong password!");
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            if (correctedBR.contains("\">Skipped countdown<")) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Fatal countdown error (countdown skipped)");
            }
        }
        /** Wait time reconnect handling */
        if (new Regex(correctedBR, "(You have reached the download(-| )limit|You have to wait)").matches()) {
            /* adjust this regex to catch the wait time string for COOKIE_HOST */
            String WAIT = new Regex(correctedBR, "((You have reached the download(-| )limit|You have to wait)[^<>]+)").getMatch(0);
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
                /* Not enough wait time to reconnect -> Wait short and retry */
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
        if (new Regex(correctedBR, "( can download files up to |Upgrade your account to download bigger files|>Please Upgrade to premium account to download this file.|>Upgrade your account to download larger files|>The file you requested reached max downloads limit for Free Users|Please Buy Premium To download this file<|This file reached max downloads limit|>Upgrade to premium account to download this file|>This file is available for Premium Users only\\.<)").matches()) {
            String filesizelimit = new Regex(correctedBR, "You can download files up to(.*?)only").getMatch(0);
            if (filesizelimit != null) {
                filesizelimit = filesizelimit.trim();
                logger.info("As free user you can download files up to " + filesizelimit + " only");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PREMIUMONLY1, PluginException.VALUE_ID_PREMIUM_ONLY);
            } else {
                logger.info("Only downloadable via premium");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PREMIUMONLY2, PluginException.VALUE_ID_PREMIUM_ONLY);
            }
        } else if (br.getURL().contains("/?op=login&redirect=")) {
            logger.info("Only downloadable via premium");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PREMIUMONLY2, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        if (new Regex(correctedBR, MAINTENANCE).matches()) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, MAINTENANCEUSERTEXT, 2 * 60 * 60 * 1000l);
        }
        checkResponseCodeErrors(this.br.getHttpConnection());
    }

    private void checkForPremiumonlyNew() throws PluginException {
        if (isPremiumonlyNew()) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
    }

    private boolean isPremiumonlyNew() {
        return this.br.getURL().contains("/predownload.php");
    }

    /** Handles all kinds of error-responsecodes! */
    private void checkResponseCodeErrors(final URLConnectionAdapter con) throws PluginException {
        if (con == null) {
            return;
        }
        final long responsecode = con.getResponseCode();
        if (responsecode == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 5 * 60 * 1000l);
        } else if (responsecode == 404) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404#1", 5 * 60 * 1000l);
        } else if (responsecode == 503) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error 503 connection limit reached, please contact our support!", 5 * 60 * 1000l);
        }
    }

    public void checkServerErrors() throws NumberFormatException, PluginException {
        if (new Regex(correctedBR, Pattern.compile("No file", Pattern.CASE_INSENSITIVE)).matches()) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error: 'no file'", 2 * 60 * 60 * 1000l);
        }
        if (new Regex(correctedBR, Pattern.compile("Wrong IP", Pattern.CASE_INSENSITIVE)).matches()) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error: 'Wrong IP'", 2 * 60 * 60 * 1000l);
        }
        if (new Regex(correctedBR, "(File Not Found|<h1>404 Not Found</h1>)").matches()) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error (404)", 30 * 60 * 1000l);
        }
    }

    /**
     * Is intended to handle out of date errors which might occur seldom by re-tring a couple of times before throwing the out of date
     * error.
     *
     * @param dl
     *            : The DownloadLink
     * @param error
     *            : The name of the error
     * @param maxRetries
     *            : Max retries before out of date error is thrown
     */
    private void handlePluginBroken(final DownloadLink dl, final String error, final int maxRetries) throws PluginException {
        int timesFailed = dl.getIntegerProperty(NICE_HOSTproperty + "failedtimes_" + error, 0);
        dl.getLinkStatus().setRetryCount(0);
        if (timesFailed <= maxRetries) {
            logger.info(NICE_HOST + ": " + error + " -> Retrying");
            timesFailed++;
            dl.setProperty(NICE_HOSTproperty + "failedtimes_" + error, timesFailed);
            throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown error occured: " + error);
        } else {
            dl.setProperty(NICE_HOSTproperty + "failedtimes_" + error, Property.NULL);
            logger.info(NICE_HOST + ": " + error + " -> Plugin is broken");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (final PluginException e) {
            account.setValid(false);
            throw e;
        }
        if (!br.getURL().contains("/?op=my_account")) {
            getPage("/?op=my_account");
        }
        final String space[] = new Regex(correctedBR, ">Storage:</strong>\\s*([0-9\\.]+)\\s*(KB|MB|GB|TB)?\\s*of").getRow(0);
        if ((space != null && space.length != 0) && (space[0] != null && space[1] != null)) {
            /* free users it's provided by default */
            ai.setUsedSpace(space[0] + " " + space[1]);
        } else if ((space != null && space.length != 0) && space[0] != null) {
            /* premium users the Mb value isn't provided for some reason... */
            ai.setUsedSpace(space[0] + "Mb");
        }
        account.setValid(true);
        String availabletraffic = new Regex(correctedBR, "Traffic available.*?:</TD><TD><b>([^<>\"']+)</b>").getMatch(0);
        if (availabletraffic == null) {
            availabletraffic = new Regex(correctedBR, ">Traffic Available</label>.*?>([^<>\"]+) Daily</p>").getMatch(0);
        }
        if (availabletraffic != null && !availabletraffic.contains("nlimited") && !availabletraffic.equalsIgnoreCase(" Mb")) {
            availabletraffic = availabletraffic.trim();
            /* need to set 0 traffic left, as getSize returns positive result, even when negative value supplied. */
            if (!availabletraffic.startsWith("-")) {
                ai.setTrafficLeft(SizeFormatter.getSize(availabletraffic));
            } else {
                ai.setTrafficLeft(0);
            }
        } else {
            ai.setUnlimitedTraffic();
        }
        /* If the premium account is expired we'll simply accept it as a free account. */
        final String expire = new Regex(correctedBR, "(\\d{1,2} (January|February|March|April|May|June|July|August|September|October|November|December) \\d{4})").getMatch(0);
        long expire_milliseconds = 0;
        if (expire != null) {
            expire_milliseconds = TimeFormatter.getMilliSeconds(expire, "dd MMMM yyyy", Locale.ENGLISH);
        }
        if (AccountType.FREE.equals(account.getType()) && (expire_milliseconds - System.currentTimeMillis()) <= 0) {
            account.setMaxSimultanDownloads(1);
            account.setConcurrentUsePossible(false);
            ai.setStatus("Free Account");
        } else {
            if (expire_milliseconds == 0) {
                account.setType(AccountType.FREE);
                account.setMaxSimultanDownloads(1);
                account.setConcurrentUsePossible(false);
                ai.setStatus("Free Account");
            } else {
                ai.setValidUntil(expire_milliseconds);
                account.setType(AccountType.PREMIUM);
                account.setMaxSimultanDownloads(5);
                account.setConcurrentUsePossible(true);
                ai.setStatus("Premium Account");
            }
        }
        return ai;
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            final boolean ifr = br.isFollowingRedirects();
            try {
                br.setCookiesExclusive(true);
                prepBrowser(br);
                br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    /*
                     * 2017-05-04: Seems like subyshare.com limits the amount of session to 1 which means we probably often play ping-pong
                     * with the users' browser --> ALWAYS validate the cookies as they can become invalid at any time ...
                     */
                    this.br.setCookies(this.getHost(), cookies);
                    getPage(COOKIE_HOST);
                    if (correctedBR.contains("/account/logout")) {
                        /* Cookies valid --> All good --> Save cookies (because of the new timestamp which might be useful in the future). */
                        if (br.getCookie(COOKIE_HOST, "login") == null || br.getCookie(COOKIE_HOST, "xfss") == null) {
                            br.clearCookies(getHost());
                        } else {
                            if (!br.getURL().contains("/?op=my_account")) {
                                getPage("/?op=my_account");
                            }
                            if (!new Regex(correctedBR, "(Premium(-| )Account expire|>Renew premium<|>\\s*PREMIUM User\\s*</span>)").matches() || new Regex(correctedBR, ">\\s*REGISTERED User\\s*</span").matches()) {
                                account.setType(AccountType.FREE);
                            } else {
                                account.setType(AccountType.PREMIUM);
                            }
                            account.saveCookies(br.getCookies(br.getURL()), "");
                            return;
                        }
                    } else {
                        br.clearCookies(getHost());
                    }
                    /* Perform full login */
                }
                // lets load home page FIRST
                getPage(COOKIE_HOST);
                getPage("/account/login");
                if (antiddosEnforced()) {
                    // recaptchaV2 event!
                    logger.info("Detected captcha method \"reCaptchaV2\"");
                    final Form form = br.getFormbyProperty("id", "checkDDOS");
                    if (this.getDownloadLink() == null) {
                        // login wont contain downloadlink
                        this.setDownloadLink(new DownloadLink(this, "antiDDoS captcha prompt!", this.getHost(), this.getHost(), true));
                    }
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                    form.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                    sendForm(form);
                    if (antiddosEnforced()) {
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    }
                    getPage("/account/login");
                }
                final String lang = System.getProperty("user.language");
                final Form loginform = br.getFormbyProperty("name", "FL");
                if (loginform == null) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                loginform.put("login", Encoding.urlEncode(account.getUser()));
                loginform.put("password", Encoding.urlEncode(account.getPass()));
                sendForm(loginform);
                if (br.getCookie(COOKIE_HOST, "login") == null || br.getCookie(COOKIE_HOST, "xfss") == null) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                if (!br.getURL().contains("/?op=my_account")) {
                    getPage("/?op=my_account");
                }
                if (!new Regex(correctedBR, "(Premium(-| )Account expire|>Renew premium<|>\\s*PREMIUM User\\s*</span>)").matches() || new Regex(correctedBR, ">\\s*REGISTERED User\\s*</span").matches()) {
                    account.setType(AccountType.FREE);
                } else {
                    account.setType(AccountType.PREMIUM);
                }
                account.saveCookies(br.getCookies(br.getURL()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            } finally {
                br.setFollowRedirects(ifr);
            }
        }
    }

    protected boolean isValidDownloadConnection(final URLConnectionAdapter con) {
        final String contentType = con.getContentType();
        if (StringUtils.contains(contentType, "text") || StringUtils.containsIgnoreCase(contentType, "html") || con.getCompleteContentLength() == -1 || con.getResponseCode() == 401 || con.getResponseCode() == 404 || con.getResponseCode() == 409 || con.getResponseCode() == 440) {
            return false;
        } else {
            return con.getResponseCode() == 200 || con.getResponseCode() == 206 || con.isContentDisposition();
        }
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        passCode = downloadLink.getStringProperty("pass");
        requestFileInformation(downloadLink, account, true);
        login(account, false);
        if (AccountType.FREE.equals(account.getType())) {
            requestFileInformation(downloadLink, account, true);
            doFree(downloadLink, false, 1, "freelink2");
        } else {
            String dllink = checkDirectLink(downloadLink, "premlink");
            if (dllink == null) {
                br.setFollowRedirects(false);
                getPage(downloadLink.getDownloadURL());
                if (br.getRedirectLocation() != null && br.getRedirectLocation().matches(getSupportedLinks().pattern())) {
                    // https <-> http redirect, should never happen
                    br.followRedirect();
                }
                if (isPremiumonlyNew()) {
                    if (downloadLink.getLinkStatus().getRetryCount() < 2) {
                        logger.info("Possible cookie failure");
                        this.login(account, true);
                        throw new PluginException(LinkStatus.ERROR_RETRY, "Possible cookie failure");
                    } else {
                        logger.info("Website failure or account is not premium anymore");
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
                    }
                }
                /* Special: vidembed will also work for non-videos and via premium mode! */
                dllink = handleVideoembed1(dllink);
                if (dllink == null) {
                    dllink = getDllink();
                }
                if (dllink == null) {
                    final Form dlform = br.getFormbyProperty("name", "F1");
                    if (dlform != null && new Regex(correctedBR, PASSWORDTEXT).matches()) {
                        passCode = handlePassword(dlform, downloadLink);
                    }
                    checkErrors(downloadLink, true);
                    if (dlform == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    sendForm(dlform);
                    checkErrors(downloadLink, true);
                    dllink = getDllink();
                }
            }
            if (dllink == null) {
                logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            logger.info("Final downloadlink = " + dllink + " starting the download...");
            br.setFollowRedirects(true);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -2);
            if (!isValidDownloadConnection(dl.getConnection())) {
                if (dl.getConnection().getResponseCode() == 503) {
                    dl.getConnection().disconnect();
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Connection limit reached, please contact our support!", 5 * 60 * 1000l);
                }
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                correctBR();
                checkServerErrors();
                handlePluginBroken(downloadLink, "dllinknofile", 3);
            }
            fixFilename(downloadLink);
            downloadLink.setProperty("premlink", dllink);
            dl.startDownload();
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.SibSoft_XFileShare;
    }
}
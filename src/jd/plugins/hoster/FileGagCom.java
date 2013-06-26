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
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.nutils.Formatter;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.parser.html.InputField;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filegag.com" }, urls = { "https?://(www\\.)?filegag(plus)?\\.com/(vidembed\\-)?[a-z0-9]{12}" }, flags = { 2 })
public class FileGagCom extends PluginForHost {

    // Site Setters
    // primary website url, take note of redirects
    private final String               COOKIE_HOST                  = "http://filegagplus.com";
    // domain names used within download links.
    private final String               DOMAINS                      = "(filegag(plus)?\\.com)";
    private final String               PASSWORDTEXT                 = "<br><b>Passwor(d|t):</b> <input";
    private final String               MAINTENANCE                  = ">This server is in maintenance mode";
    private final boolean              videoHoster                  = false;
    private final boolean              supportsHTTPS                = false;
    private final boolean              enforcesHTTPS                = false;
    private final boolean              useRUA                       = false;
    private final boolean              useAltExpire                 = true;
    private final boolean              useAltLinkCheck              = false;

    // Connection Management
    // note: CAN NOT be negative or zero! (ie. -1 or 0) Otherwise math sections fail. .:. use [1-20]
    private static final AtomicInteger totalMaxSimultanFreeDownload = new AtomicInteger(20);

    // DEV NOTES
    // XfileShare Version 3.0.5.0
    // last XfileSharingProBasic compare :: 2.6.2.1
    // mods:
    // protocol: no https
    // captchatype: null
    // other: no redirects

    private void setConstants(final Account account) {
        if (account != null && account.getBooleanProperty("nopremium")) {
            // free account
            chunks = 0;
            resumes = true;
            acctype = "Free Account";
            directlinkproperty = "freelink2";
        } else if (account != null && !account.getBooleanProperty("nopremium")) {
            // prem account
            chunks = 0;
            resumes = true;
            acctype = "Premium Account";
            directlinkproperty = "premlink";
        } else {
            // non account
            chunks = 0;
            resumes = true;
            acctype = "Non Account";
            directlinkproperty = "freelink";
        }
    }

    private boolean allowsConcurrent(final Account account) {
        if (account != null && account.getBooleanProperty("nopremium")) {
            // free account
            return false;
        } else if (account != null && !account.getBooleanProperty("nopremium")) {
            // prem account
            return true;
        } else {
            // non account
            return false;
        }
    }

    public boolean hasAutoCaptcha() {
        return true;
    }

    public boolean hasCaptcha(final DownloadLink downloadLink, final jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("nopremium"))) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }

    /**
     * @author raztoki
     * 
     * @category 'Experimental', Mods written July 2012 - 2013
     * */
    public FileGagCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(COOKIE_HOST + "/premium.html");
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        fuid = new Regex(downloadLink.getDownloadURL(), "([a-z0-9]{12})$").getMatch(0);
        br.setFollowRedirects(true);
        prepBrowser(br);

        String[] fileInfo = new String[3];

        if (useAltLinkCheck) {
            altAvailStat(fileInfo, downloadLink);
        }

        getPage(downloadLink.getDownloadURL());

        if (br.getURL().contains("/?op=login&redirect=")) {
            ArrayList<Account> accounts = AccountController.getInstance().getAllAccounts(this.getHost());
            Account account = null;
            if (accounts != null && accounts.size() != 0) {
                Iterator<Account> it = accounts.iterator();
                while (it.hasNext()) {
                    Account n = it.next();
                    if (n.isEnabled() && n.isValid()) {
                        account = n;
                        break;
                    }
                }
            }
            if (account != null) {
                login(account, false);
                getPage(downloadLink.getDownloadURL());
            } else {
                altAvailStat(fileInfo, downloadLink);
            }
        }

        if (cbr.containsHTML("(No such file|>File Not Found<|>The file was removed by|Reason for deletion:\n|<li>The file (expired|deleted by (its owner|administration)))")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (cbr.containsHTML(MAINTENANCE)) {
            downloadLink.getLinkStatus().setStatusText(MAINTENANCEUSERTEXT);
            return AvailableStatus.TRUE;
        }
        br.setFollowRedirects(false);

        // scan the first page
        scanInfo(fileInfo, downloadLink);
        // scan the second page. filesize[1] and md5hash[2] are not mission critical
        if (inValidate(fileInfo[0])) {
            Form download1 = getFormByKey(cbr, "op", "download1");
            if (download1 != null) {
                download1 = cleanForm(download1);
                download1.remove("method_premium");
                sendForm(download1);
                scanInfo(fileInfo, downloadLink);
            }
        }
        if (inValidate(fileInfo[0])) {
            if (cbr.containsHTML("You have reached the download(\\-| )limit")) {
                logger.warning("Waittime detected, please reconnect to make the linkchecker work!");
                return AvailableStatus.UNCHECKABLE;
            }
            logger.warning("filename equals null, throwing \"plugin defect\"");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!inValidate(fileInfo[2])) downloadLink.setMD5Hash(fileInfo[2].trim());
        fileInfo[0] = fileInfo[0].replaceAll("(</b>|<b>|\\.html)", "");
        downloadLink.setName(fileInfo[0].trim());
        if (!inValidate(fileInfo[1])) downloadLink.setDownloadSize(SizeFormatter.getSize(fileInfo[1]));
        return AvailableStatus.TRUE;
    }

    private String[] scanInfo(final String[] fileInfo, final DownloadLink downloadLink) {
        // standard traits from base page
        if (inValidate(fileInfo[0])) {
            fileInfo[0] = cbr.getRegex("You have requested.*?https?://(www\\.)?" + this.getHost() + "/" + fuid + "/(.*?)</font>").getMatch(1);
            if (inValidate(fileInfo[0])) {
                fileInfo[0] = cbr.getRegex("fname\"( type=\"hidden\")? value=\"(.*?)\"").getMatch(1);
                if (inValidate(fileInfo[0])) {
                    fileInfo[0] = cbr.getRegex("<h2>Download File(.*?)</h2>").getMatch(0);
                    if (inValidate(fileInfo[0])) {
                        // can cause new line finds, so check if it matches.
                        // fileInfo[0] = cbr.getRegex("Download File:? ?(<[^>]+> ?)+?([^<>\"\\']+)").getMatch(1);
                        // traits from download1 page below.
                        if (inValidate(fileInfo[0])) {
                            fileInfo[0] = cbr.getRegex("Filename:(<[^>]+>)+[\r\n\t ]+(<[^>]+>)+([^<>\"\\']+)").getMatch(2);
                            // next two are details from sharing box
                            if (inValidate(fileInfo[0])) {
                                fileInfo[0] = cbr.getRegex("copy\\(this\\);.+>(.+) \\- [\\d\\.]+ (KB|MB|GB)</a></textarea>[\r\n\t ]+</div>").getMatch(0);
                                if (inValidate(fileInfo[0])) {
                                    fileInfo[0] = cbr.getRegex("copy\\(this\\);.+\\](.+) \\- [\\d\\.]+ (KB|MB|GB)\\[/URL\\]").getMatch(0);
                                }
                            }
                        }
                    }
                }
            }
        }
        if (inValidate(fileInfo[1])) {
            fileInfo[1] = cbr.getRegex("\\(([0-9]+ bytes)\\)").getMatch(0);
            if (inValidate(fileInfo[1])) {
                fileInfo[1] = cbr.getRegex("</font>[ ]+\\(([^<>\"\\'/]+)\\)(.*?)</font>").getMatch(0);
                if (inValidate(fileInfo[1])) {
                    fileInfo[1] = cbr.getRegex("(\\d+(\\.\\d+)? ?(KB|MB|GB))").getMatch(0);
                    if (inValidate(fileInfo[1])) {
                        try {
                            altAvailStat(fileInfo, downloadLink);
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }
        if (inValidate(fileInfo[2])) fileInfo[2] = cbr.getRegex("<b>MD5.*?</b>.*?nowrap>(.*?)<").getMatch(0);
        return fileInfo;
    }

    /**
     * Provides alternative linkchecking method for a single link at a time. Can be used as generic failover, though kinda pointless as this
     * method doesn't give filename...
     * 
     * */
    private String[] altAvailStat(final String[] fileInfo, final DownloadLink downloadLink) throws Exception {
        Browser alt = new Browser();
        prepBrowser(alt);
        alt.postPage(COOKIE_HOST + "/?op=checkfiles", "op=checkfiles&process=Check+URLs&list=" + downloadLink.getDownloadURL());
        String[] linkInformation = alt.getRegex(">" + downloadLink.getDownloadURL() + "</td><td style=\"color:[^;]+;\">(\\w+)</td><td>([^<>]+)?</td>").getRow(0);
        if (linkInformation[0].equalsIgnoreCase("found")) {
            downloadLink.setAvailable(true);
            if (!inValidate(linkInformation[1]) && inValidate(fileInfo[1])) fileInfo[1] = linkInformation[1];
        } else {
            // not found! <td>link</td><td style="color:red;">Not found!</td><td></td>
            downloadLink.setAvailable(false);
        }
        if (!inValidate(fuid) && inValidate(fileInfo[0])) fileInfo[0] = fuid;
        return fileInfo;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        setConstants(null);
        requestFileInformation(downloadLink);
        doFree(downloadLink, null);
    }

    @SuppressWarnings("unused")
    private void doFree(final DownloadLink downloadLink, final Account account) throws Exception, PluginException {
        if (account != null) {
            logger.info(account.getUser() + " @ " + acctype + " -> Free Download");
        } else {
            logger.info("Guest @ " + acctype + " -> Free Download");
        }
        passCode = downloadLink.getStringProperty("pass");
        // First, bring up saved final links
        dllink = checkDirectLink(downloadLink);
        // Second, check for streaming links on the first page
        if (inValidate(dllink)) getDllink();
        // Third, do they provide video hosting?
        if (inValidate(dllink) && videoHoster) {
            final Browser brv = br.cloneBrowser();
            brv.getPage("/vidembed-" + fuid);
            dllink = brv.getRedirectLocation();
        }
        // Fourth, continue like normal.
        if (inValidate(dllink)) {
            checkErrors(downloadLink, false);
            Form download1 = getFormByKey(cbr, "op", "download1");
            if (download1 != null) {
                // stable is lame, issue finding input data fields correctly. eg. closes at ' quotation mark - remove when jd2 goes stable!
                download1 = cleanForm(download1);
                // end of backward compatibility
                download1.remove("method_premium");
                sendForm(download1);
                checkErrors(downloadLink, false);
                getDllink();
            }
        }
        if (inValidate(dllink)) {
            Form dlForm = getFormByKey(cbr, "op", "download2");
            if (dlForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            // how many forms deep do you want to try.
            int repeat = 2;
            for (int i = 0; i <= repeat; i++) {
                dlForm = cleanForm(dlForm);
                final long timeBefore = System.currentTimeMillis();
                boolean password = false;
                boolean skipWaittime = false;
                if (cbr.containsHTML(PASSWORDTEXT)) {
                    password = true;
                    logger.info("The downloadlink seems to be password protected.");
                }
                // md5 can be on the subsequent pages
                if (inValidate(downloadLink.getMD5Hash())) {
                    String md5hash = cbr.getRegex("<b>MD5.*?</b>.*?nowrap>(.*?)<").getMatch(0);
                    if (md5hash != null) downloadLink.setMD5Hash(md5hash.trim());
                }
                /* Captcha START */
                if (cbr.containsHTML(";background:#ccc;text-align")) {
                    logger.info("Detected captcha method \"plaintext captcha\"");
                    /** Captcha method by ManiacMansion */
                    final String[][] letters = cbr.getRegex("<span style=\\'position:absolute;padding\\-left:(\\d+)px;padding\\-top:\\d+px;\\'>(&#\\d+;)</span>").getMatches();
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
                } else if (cbr.containsHTML("/captchas/")) {
                    logger.info("Detected captcha method \"Standard captcha\"");
                    final String[] sitelinks = HTMLParser.getHttpLinks(cbr.toString(), null);
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
                    if (inValidate(captchaurl)) {
                        logger.warning("Standard captcha captchahandling broken!");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    String code = getCaptchaCode("xfilesharingprobasic", captchaurl, downloadLink);
                    dlForm.put("code", code);
                    logger.info("Put captchacode " + code + " obtained by captcha metod \"Standard captcha\" in the form.");
                } else if (cbr.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
                    logger.info("Detected captcha method \"Re Captcha\"");
                    final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                    final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                    final String id = cbr.getRegex("\\?k=([A-Za-z0-9%_\\+\\- ]+)\"").getMatch(0);
                    if (inValidate(id)) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    rc.setId(id);
                    rc.load();
                    final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    final String c = getCaptchaCode(cf, downloadLink);
                    dlForm.put("recaptcha_challenge_field", rc.getChallenge());
                    dlForm.put("recaptcha_response_field", Encoding.urlEncode(c));
                    logger.info("Put captchacode " + c + " obtained by captcha metod \"Re Captcha\" in the form and submitted it.");
                    /** wait time is often skippable for reCaptcha handling */
                    skipWaittime = true;
                } else if (cbr.containsHTML("solvemedia\\.com/papi/")) {
                    logger.info("Detected captcha method \"solvemedia\"");
                    final PluginForDecrypt solveplug = JDUtilities.getPluginForDecrypt("linkcrypt.ws");
                    final jd.plugins.decrypter.LnkCrptWs.SolveMedia sm = ((jd.plugins.decrypter.LnkCrptWs) solveplug).getSolveMedia(br);
                    final File cf = sm.downloadCaptcha(getLocalCaptchaFile());
                    final String code = getCaptchaCode(cf, downloadLink);
                    final String chid = sm.getChallenge(code);
                    dlForm.put("adcopy_challenge", chid);
                    dlForm.put("adcopy_response", "manual_challenge");
                } else if (cbr.containsHTML("id=\"capcode\" name= \"capcode\"")) {
                    logger.info("Detected captcha method \"keycaptca\"");
                    final PluginForDecrypt keycplug = JDUtilities.getPluginForDecrypt("linkcrypt.ws");
                    final jd.plugins.decrypter.LnkCrptWs.KeyCaptcha kc = ((jd.plugins.decrypter.LnkCrptWs) keycplug).getKeyCaptcha(br);
                    final String result = kc.showDialog(downloadLink.getDownloadURL());
                    if (result != null && "CANCEL".equals(result)) { throw new PluginException(LinkStatus.ERROR_FATAL); }
                    dlForm.put("capcode", result);
                }
                /* Captcha END */
                if (password) passCode = handlePassword(dlForm, downloadLink);
                if (!skipWaittime) waitTime(timeBefore, downloadLink);
                sendForm(dlForm);
                logger.info("Submitted DLForm");
                checkErrors(downloadLink, true);
                getDllink();
                if (inValidate(dllink) && (getFormByKey(cbr, "op", "download2") == null || i == repeat)) {
                    if (i == repeat)
                        logger.warning("Exausted repeat count, after dllink == null");
                    else
                        logger.warning("Couldn't find 'download2' and 'dllink == null'");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else if (inValidate(dllink) && getFormByKey(cbr, "op", "download2") != null) {
                    dlForm = getFormByKey(cbr, "op", "download2");
                    continue;
                } else {
                    break;
                }
            }
        }
        // Process usedHost within hostMap. We do it here so that we can probe if slots are already used before openDownload.
        controlHost(account, downloadLink, true);
        logger.info("Final downloadlink = " + dllink + " starting the download...");
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumes, chunks);
        } catch (UnknownHostException e) {
            // Try catch required otherwise plugin logic wont work as intended. Also prevents infinite loops when dns record is missing.

            // dump the saved host from directlinkproperty
            downloadLink.setProperty(directlinkproperty, Property.NULL);
            // remove usedHost slot from hostMap
            controlHost(account, downloadLink, false);
            logger.warning("DNS issue has occured!");
            e.printStackTrace();
            // int value of plugin property, as core error in current JD2 prevents proper retry handling.
            // TODO: remove when retry issues are resolved!
            int retry = downloadLink.getIntegerProperty("retry", 0);
            if (retry == 3) {
                downloadLink.setProperty("retry", Property.NULL);
                throw new PluginException(LinkStatus.ERROR_FATAL, "DNS issue cannot be resolved!");
            } else {
                retry++;
                downloadLink.setProperty("retry", retry);
                throw new PluginException(LinkStatus.ERROR_RETRY, 15000);
            }
        }
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 503 && dl.getConnection().getHeaderFields("server").contains("nginx")) {
                controlSimHost(account);
                controlHost(account, downloadLink, false);
            } else {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                correctBR();
                checkServerErrors();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } else {
            // we can not 'rename' filename once the download started, could be problematic!
            if (downloadLink.getDownloadCurrent() == 0) {
                fixFilename(downloadLink);
            }
            try {
                // add a download slot
                controlSlot(+1, account);
                // start the dl
                dl.startDownload();
            } finally {
                // remove usedHost slot from hostMap
                controlHost(account, downloadLink, false);
                // remove download slot
                controlSlot(-1, account);
            }
        }
    }

    /**
     * Removes patterns which could break the plugin due to fake/hidden HTML, or false positives caused by HTML comments.
     * 
     * @throws Exception
     * @author raztoki
     */
    public void correctBR() throws Exception {
        String toClean = br.toString();

        ArrayList<String> regexStuff = new ArrayList<String>();

        // remove custom rules first!!! As html can change because of generic cleanup rules.

        // generic cleanup
        // this checks for fake or empty forms from original source, and adds form html to regexStuff.
        for (final Form f : br.getForms()) {
            if (!f.containsHTML("<input[^>]+type=\"submit\"[^>]+>")) {
                regexStuff.add("(" + f.getHtmlCode() + ")");
            }
        }
        regexStuff.add("<\\!(\\-\\-.*?\\-\\-)>");
        regexStuff.add("(<div[^>]+display: ?none;[^>]+>.*?</div>)");
        regexStuff.add("(visibility:hidden>.*?<)");

        for (String aRegex : regexStuff) {
            String results[] = new Regex(toClean, aRegex).getColumn(0);
            if (results != null) {
                for (String result : results) {
                    toClean = toClean.replace(result, "");
                }
            }
        }
        cbr = br.cloneBrowser();
        cbr = cleanupBrowser(cbr, toClean);
    }

    private String regexDllink(final String source) {
        return new Regex(source, "(\"|')(https?://(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|([\\w\\-]+\\.)?" + DOMAINS + ")(:\\d{1,5})?/(files|d|cgi\\-bin/dl\\.cgi)/(\\d+/)?[a-z0-9]+/[^<>\"/]*?)(\"|')").getMatch(1);
    }

    private void getDllink() {
        dllink = br.getRedirectLocation();
        if (inValidate(dllink)) {
            dllink = regexDllink(cbr.toString());
            if (inValidate(dllink)) {
                final String cryptedScripts[] = cbr.getRegex("p\\}\\((.*?)\\.split\\('\\|'\\)").getColumn(0);
                if (cryptedScripts != null && cryptedScripts.length != 0) {
                    for (String crypted : cryptedScripts) {
                        decodeDownloadLink(crypted);
                        if (dllink != null) break;
                    }
                }
            }
        }
    }

    private void decodeDownloadLink(final String s) {
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

        if (decoded != null) {
            dllink = regexDllink(decoded);
        }
    }

    private void waitTime(final long timeBefore, final DownloadLink downloadLink) throws PluginException {
        int passedTime = (int) ((System.currentTimeMillis() - timeBefore) / 1000) - 1;
        /** Ticket Time */
        String ttt = cbr.getRegex("id=\"countdown_str\">[^<>\"]+<span id=\"[^<>\"]+\"( class=\"[^<>\"]+\")?>([\n ]+)?(\\d+)([\n ]+)?</span>").getMatch(2);
        if (inValidate(ttt)) ttt = cbr.getRegex("id=\"countdown_str\"[^>]+>Wait[^>]+>(\\d+)\\s?+</span>").getMatch(0);
        if (!inValidate(ttt)) {
            int tt = Integer.parseInt(ttt);
            tt -= passedTime;
            logger.info("Waittime detected, waiting " + ttt + " - " + passedTime + " seconds from now on...");
            if (tt > 0) sleep(tt * 1000l, downloadLink);
        }
    }

    private void checkErrors(final DownloadLink theLink, final boolean checkAll) throws NumberFormatException, PluginException {
        if (checkAll) {
            if (cbr.containsHTML("Wrong password|" + PASSWORDTEXT)) {
                // handle password has failed in the past, additional try catching / resetting values
                logger.warning("Wrong password, the entered password \"" + passCode + "\" is wrong, retrying...");
                passCode = null;
                theLink.setProperty("pass", Property.NULL);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
            }
            if (cbr.containsHTML("Wrong captcha")) {
                logger.warning("Wrong captcha or wrong password!");
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            if (cbr.containsHTML("\">Skipped countdown<")) throw new PluginException(LinkStatus.ERROR_FATAL, "Fatal countdown error (countdown skipped)");
        }
        // monitor this
        if (cbr.containsHTML("(class=\"err\">You have reached the download(\\-| )limit[^<]+for last[^<]+)")) {
            /*
             * Indication of when you've reached the max download limit for that given session! Usually shows how long the session was
             * recorded from x time (hours|days) which can trigger false positive below wait handling. As its only indication of what's
             * previous happened, as in past tense and not a wait time going forward... unknown wait time!
             */
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "You've reached the download session limit!", 60 * 60 * 1000l);
        }
        /** Wait time reconnect handling */
        if (cbr.containsHTML("You have to wait")) {
            // adjust this Regex to catch the wait time string for COOKIE_HOST
            String WAIT = cbr.getRegex("((You have to wait)[^<>]+)").getMatch(0);
            String tmphrs = new Regex(WAIT, "\\s+(\\d+)\\s+hours?").getMatch(0);
            if (inValidate(tmphrs)) tmphrs = cbr.getRegex("You have to wait.*?\\s+(\\d+)\\s+hours?").getMatch(0);
            String tmpmin = new Regex(WAIT, "\\s+(\\d+)\\s+minutes?").getMatch(0);
            if (inValidate(tmpmin)) tmpmin = cbr.getRegex("You have to wait.*?\\s+(\\d+)\\s+minutes?").getMatch(0);
            String tmpsec = new Regex(WAIT, "\\s+(\\d+)\\s+seconds?").getMatch(0);
            String tmpdays = new Regex(WAIT, "\\s+(\\d+)\\s+days?").getMatch(0);
            if (inValidate(tmphrs) && inValidate(tmpmin) && inValidate(tmpsec) && inValidate(tmpdays)) {
                logger.info("Waittime regexes seem to be broken");
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 60 * 60 * 1000l);
            } else {
                long days = 0, hours = 0, minutes = 0, seconds = 0;
                if (!inValidate(tmpdays)) days = Integer.parseInt(tmpdays);
                if (!inValidate(tmphrs)) hours = Integer.parseInt(tmphrs);
                if (!inValidate(tmpmin)) minutes = Integer.parseInt(tmpmin);
                if (!inValidate(tmpsec)) seconds = Integer.parseInt(tmpsec);
                long waittime = ((days * 86400000) + (hours * 3600000) + (minutes * 60000) + (seconds * 1000));
                logger.info("Detected waittime #2, waiting " + waittime + "milliseconds");
                /** Not enough wait time to reconnect->Wait and try again */
                if (waittime < 180000) {
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.xfilesharingprobasic.allwait", ALLWAIT_SHORT), waittime);
                } else {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, waittime);
                }
            }
        }
        if (cbr.containsHTML("You're using all download slots for IP")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 10 * 60 * 1001l); }
        if (cbr.containsHTML("Error happened when generating Download Link")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error!", 10 * 60 * 1000l);
        /** Error handling for only-premium links */
        if (cbr.containsHTML("( can download files up to |Upgrade your account to download bigger files|>Upgrade your account to download larger files|>The file you requested reached max downloads limit for Free Users|Please Buy Premium To download this file<|This file reached max downloads limit|>This file is available for Premium Users only\\.<)")) {
            String filesizelimit = cbr.getRegex("You can download files up to(.*?)only").getMatch(0);
            if (filesizelimit != null) {
                filesizelimit = filesizelimit.trim();
                logger.warning("As free user you can download files up to " + filesizelimit + " only");
                throw new PluginException(LinkStatus.ERROR_FATAL, PREMIUMONLY1 + " " + filesizelimit);
            } else {
                logger.warning("Only downloadable via premium");
                throw new PluginException(LinkStatus.ERROR_FATAL, PREMIUMONLY2);
            }
        }
        if (cbr.containsHTML(MAINTENANCE)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, MAINTENANCEUSERTEXT, 2 * 60 * 60 * 1000l);
    }

    private void checkServerErrors() throws NumberFormatException, PluginException {
        if (cbr.containsHTML("No file")) throw new PluginException(LinkStatus.ERROR_FATAL, "Server error");
        if (cbr.containsHTML("(File Not Found|<h1>404 Not Found</h1>)")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 30 * 60 * 1000l);
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        /* reset maxPrem workaround on every fetchAccount info */
        totalMaxSimultanPremDownload.set(1);
        try {
            login(account, true);
        } catch (final PluginException e) {
            account.setValid(false);
            throw e;
        }
        final String space[] = cbr.getRegex(">Used space:</td>.*?<td.*?b>([0-9\\.]+) of [0-9\\.]+ (KB|MB|GB|TB)?</b>").getRow(0);
        if ((space != null && space.length != 0) && (!inValidate(space[0]) && !inValidate(space[1]))) {
            // free users it's provided by default
            ai.setUsedSpace(space[0] + " " + space[1]);
        } else if ((space != null && space.length != 0) && !inValidate(space[0])) {
            // premium users the Mb value isn't provided for some reason...
            ai.setUsedSpace(space[0] + "Mb");
        }
        account.setValid(true);
        final String availabletraffic = cbr.getRegex("Traffic available.*?:</TD></tr>.+<TD><b>([^<>\"']+)</b>").getMatch(0);
        if (!inValidate(availabletraffic) && !availabletraffic.contains("nlimited") && !availabletraffic.equalsIgnoreCase(" Mb")) {
            availabletraffic.trim();
            // need to set 0 traffic left, as getSize returns positive result, even when negative value supplied.
            if (!availabletraffic.startsWith("-")) {
                ai.setTrafficLeft(SizeFormatter.getSize(availabletraffic));
            } else {
                ai.setTrafficLeft(0);
            }
        } else {
            ai.setUnlimitedTraffic();
        }
        if (account.getBooleanProperty("nopremium")) {
            ai.setStatus("Registered (free) User");
            totalMaxSimultanPremDownload.set(20);
        } else {
            long expire = 0, expireD = 0, expireS = 0;
            final String expireDay = cbr.getRegex("(\\d{1,2} (January|February|March|April|May|June|July|August|September|October|November|December) \\d{4})").getMatch(0);
            if (!inValidate(expireDay)) {
                expireD = TimeFormatter.getMilliSeconds(expireDay, "dd MMMM yyyy", Locale.ENGLISH);
            }
            if (inValidate(expireDay) || useAltExpire) {
                // A more accurate expire time, down to the second. Usually shown on 'extend premium account' page.
                getPage("/?op=payments");
                String expireSecond = cbr.getRegex("Premium(\\-| )Account expires?:([^\n\r]+)").getMatch(1);
                if (!inValidate(expireSecond)) {
                    String tmpdays = new Regex(expireSecond, "(\\d+)\\s+days?").getMatch(0);
                    String tmphrs = new Regex(expireSecond, "(\\d+)\\s+hours?").getMatch(0);
                    String tmpmin = new Regex(expireSecond, "(\\d+)\\s+minutes?").getMatch(0);
                    String tmpsec = new Regex(expireSecond, "(\\d+)\\s+seconds?").getMatch(0);
                    long days = 0, hours = 0, minutes = 0, seconds = 0;
                    if (!inValidate(tmpdays)) days = Integer.parseInt(tmpdays);
                    if (!inValidate(tmphrs)) hours = Integer.parseInt(tmphrs);
                    if (!inValidate(tmpmin)) minutes = Integer.parseInt(tmpmin);
                    if (!inValidate(tmpsec)) seconds = Integer.parseInt(tmpsec);
                    expireS = ((days * 86400000) + (hours * 3600000) + (minutes * 60000) + (seconds * 1000)) + System.currentTimeMillis();
                }
                if (expireD == 0 && expireS == 0) {
                    ai.setExpired(true);
                    account.setValid(false);
                    return ai;
                }
            }
            if (expireS != 0) {
                expire = expireS;
            } else {
                expire = expireD;
            }
            totalMaxSimultanPremDownload.set(20);
            ai.setValidUntil(expire);
            ai.setStatus("Premium User");
        }
        return ai;
    }

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /** Load cookies */
                prepBrowser(br);
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
                br.setFollowRedirects(true);
                getPage(COOKIE_HOST + "/login.html");
                Form loginform = br.getFormbyProperty("name", "FL");
                if (loginform == null) {
                    String lang = System.getProperty("user.language");
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                loginform = cleanForm(loginform);
                loginform.put("login", Encoding.urlEncode(account.getUser()));
                loginform.put("password", Encoding.urlEncode(account.getPass()));
                sendForm(loginform);
                if (br.getCookie(COOKIE_HOST, "login") == null || br.getCookie(COOKIE_HOST, "xfss") == null) {
                    String lang = System.getProperty("user.language");
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                if (!br.getURL().contains("/?op=my_account")) {
                    getPage("/?op=my_account");
                }
                if (!cbr.containsHTML("(Premium(\\-| )Account expire|>Renew premium<)")) {
                    account.setProperty("nopremium", true);
                } else {
                    account.setProperty("nopremium", false);
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

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        setConstants(account);
        passCode = downloadLink.getStringProperty("pass");
        requestFileInformation(downloadLink);
        login(account, false);
        br.setFollowRedirects(false);
        if (account.getBooleanProperty("nopremium")) {
            getPage(downloadLink.getDownloadURL());
            doFree(downloadLink, account);
        } else {
            logger.info(account.getUser() + " @ " + acctype + " -> Premium Download");
            dllink = checkDirectLink(downloadLink);
            if (inValidate(dllink)) {
                getPage(downloadLink.getDownloadURL());
                getDllink();
                if (inValidate(dllink)) {
                    checkErrors(downloadLink, true);
                    Form dlform = cbr.getFormbyProperty("name", "F1");
                    if (dlform != null && cbr.containsHTML(PASSWORDTEXT)) passCode = handlePassword(dlform, downloadLink);
                    checkErrors(downloadLink, true);
                    if (dlform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    sendForm(dlform);
                    checkErrors(downloadLink, true);
                    getDllink();
                }
            }
            if (inValidate(dllink)) {
                logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            // Process usedHost within hostMap. We do it here so that we can probe if slots are already used before openDownload.
            controlHost(account, downloadLink, true);
            logger.info("Final downloadlink = " + dllink + " starting the download...");
            try {
                dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumes, chunks);
            } catch (UnknownHostException e) {
                // Try catch required otherwise plugin logic wont work as intended. Also prevents infinite loops when dns record is missing.

                // dump the saved host from directlinkproperty
                downloadLink.setProperty(directlinkproperty, Property.NULL);
                // remove usedHost slot from hostMap
                controlHost(account, downloadLink, false);
                logger.warning("DNS issue has occured!");
                e.printStackTrace();
                // int value of plugin property, as core error in current JD2 prevents proper retry handling.
                // TODO: remove when retry issues are resolved!
                int retry = downloadLink.getIntegerProperty("retry", 0);
                if (retry == 3) {
                    downloadLink.setProperty("retry", Property.NULL);
                    throw new PluginException(LinkStatus.ERROR_FATAL, "DNS issue cannot be resolved!");
                } else {
                    retry++;
                    downloadLink.setProperty("retry", retry);
                    throw new PluginException(LinkStatus.ERROR_RETRY, 15000);
                }
            }
            if (dl.getConnection().getContentType().contains("html")) {
                if (dl.getConnection().getResponseCode() == 503 && dl.getConnection().getHeaderFields("server").contains("nginx")) {
                    controlSimHost(account);
                    controlHost(account, downloadLink, false);
                } else {
                    logger.warning("The final dllink seems not to be a file!");
                    br.followConnection();
                    correctBR();
                    checkServerErrors();
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            } else {
                // we can not 'rename' filename once the download started, could be problematic!
                if (downloadLink.getDownloadCurrent() == 0) {
                    fixFilename(downloadLink);
                }
                try {
                    // add a download slot
                    controlSlot(+1, account);
                    // start the dl
                    dl.startDownload();
                } finally {
                    // remove usedHost slot from hostMap
                    controlHost(account, downloadLink, false);
                    // remove download slot
                    controlSlot(-1, account);
                }
            }
        }
    }

    // ***************************************************************************************************** //
    // The components below doesn't require coder interaction, or configuration !

    private Browser                                           cbr                          = new Browser();

    private String                                            acctype                      = null;
    private String                                            directlinkproperty           = null;
    private String                                            dllink                       = null;
    private String                                            fuid                         = null;
    private String                                            passCode                     = null;
    private String                                            usedHost                     = null;

    private int                                               chunks                       = 1;

    private boolean                                           resumes                      = false;

    private final String                                      MAINTENANCEUSERTEXT          = JDL.L("hoster.xfilesharingprobasic.errors.undermaintenance", "This server is under Maintenance");
    private final String                                      ALLWAIT_SHORT                = JDL.L("hoster.xfilesharingprobasic.errors.waitingfordownloads", "Waiting till new downloads can be started");
    private final String                                      PREMIUMONLY1                 = JDL.L("hoster.xfilesharingprobasic.errors.premiumonly1", "Max downloadable filesize for free users:");
    private final String                                      PREMIUMONLY2                 = JDL.L("hoster.xfilesharingprobasic.errors.premiumonly2", "Only downloadable via premium or registered");

    private static AtomicInteger                              totalMaxSimultanPremDownload = new AtomicInteger(1);
    private static AtomicInteger                              maxFree                      = new AtomicInteger(1);
    private static AtomicInteger                              maxPrem                      = new AtomicInteger(1);
    // connections you can make to a given 'host' file server, this assumes each file server is setup identically.
    private static AtomicInteger                              maxNonAccSimDlPerHost        = new AtomicInteger(20);
    private static AtomicInteger                              maxFreeAccSimDlPerHost       = new AtomicInteger(20);
    private static AtomicInteger                              maxPremAccSimDlPerHost       = new AtomicInteger(20);

    private static HashMap<Account, HashMap<String, Integer>> hostMap                      = new HashMap<Account, HashMap<String, Integer>>();

    private static final Object                               LOCK                         = new Object();

    private static StringContainer                            agent                        = new StringContainer();

    public static class StringContainer {
        public String string = null;
    }

    @Override
    public void correctDownloadLink(final DownloadLink downloadLink) {
        if (enforcesHTTPS) {
            // does the site enforce the use of https?
            downloadLink.setUrlDownload(downloadLink.getDownloadURL().replaceFirst("http://", "https://"));
        } else if (!supportsHTTPS) {
            // link cleanup, but respect users protocol choosing.
            downloadLink.setUrlDownload(downloadLink.getDownloadURL().replaceFirst("https://", "http://"));
            // else we respect the users importation preference
        }
        // strip video hosting url's to reduce possible duped links.
        downloadLink.setUrlDownload(downloadLink.getDownloadURL().replace("/vidembed-", "/"));
        // output the hostmask as we wish based on COOKIE_HOST url!
        String desiredHost = new Regex(COOKIE_HOST, "https?://([^/]+)").getMatch(0);
        String importedHost = new Regex(downloadLink.getDownloadURL(), "https?://([^/]+)").getMatch(0);
        downloadLink.setUrlDownload(downloadLink.getDownloadURL().replaceAll(importedHost, desiredHost));
    }

    private Browser prepBrowser(final Browser prepBr) {
        // define custom browser headers and language settings.
        if (useRUA) {
            if (agent.string == null) {
                /* we first have to load the plugin, before we can reference it */
                JDUtilities.getPluginForHost("mediafire.com");
                agent.string = jd.plugins.hoster.MediafireCom.stringUserAgent();
            }
            prepBr.getHeaders().put("User-Agent", agent.string);
        }
        prepBr.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        prepBr.setCookie(COOKIE_HOST, "lang", "english");
        return prepBr;
    }

    public void showAccountDetailsDialog(final Account account) {
        setConstants(account);
        AccountInfo ai = account.getAccountInfo();
        String message = "";
        message += "Account type: " + acctype + "\r\n";
        if (ai.getUsedSpace() != -1) message += "  Used Space: " + Formatter.formatReadable(ai.getUsedSpace()) + "\r\n";
        if (ai.getPremiumPoints() != -1) message += "Premium Points: " + ai.getPremiumPoints() + "\r\n";

        jd.gui.UserIO.getInstance().requestMessageDialog(this.getHost() + " Account", message);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return maxFree.get();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /* workaround for free/premium issue on stable 09581 */
        return maxPrem.get();
    }

    @Override
    public String getAGBLink() {
        return COOKIE_HOST + "/tos.html";
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink downloadLink) {
        downloadLink.setProperty("retry", Property.NULL);
    }

    private void getPage(final String page) throws Exception {
        br.getPage(page);
        correctBR();
    }

    @SuppressWarnings("unused")
    private void postPage(final String page, final String postData) throws Exception {
        br.postPage(page, postData);
        correctBR();
    }

    private void sendForm(final Form form) throws Exception {
        br.submitForm(form);
        correctBR();
    }

    private void fixFilename(final DownloadLink downloadLink) {
        String oldName = downloadLink.getFinalFileName();
        if (oldName == null) oldName = downloadLink.getName();
        final String serverFilename = Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection()));
        String newExtension = null;
        // some streaming sites do not provide proper file.extension within headers (Content-Disposition or the fail over getURL()).
        if (serverFilename == null) {
            logger.info("Server filename is null, keeping filename: " + oldName);
        } else {
            if (serverFilename.contains(".")) {
                newExtension = serverFilename.substring(serverFilename.lastIndexOf("."));
            } else {
                logger.info("HTTP headers don't contain filename.extension information");
            }
        }
        if (newExtension != null && !oldName.endsWith(newExtension)) {
            String oldExtension = null;
            if (oldName.contains(".")) oldExtension = oldName.substring(oldName.lastIndexOf("."));
            if (oldExtension != null && oldExtension.length() <= 5) {
                downloadLink.setFinalFileName(oldName.replace(oldExtension, newExtension));
            } else {
                downloadLink.setFinalFileName(oldName + newExtension);
            }
        }
    }

    private String checkDirectLink(final DownloadLink downloadLink) {
        dllink = downloadLink.getStringProperty(directlinkproperty);
        if (dllink != null) {
            try {
                Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(directlinkproperty, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (Exception e) {
                downloadLink.setProperty(directlinkproperty, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    private String handlePassword(final Form pwform, final DownloadLink downloadLink) throws PluginException {
        if (inValidate(passCode)) passCode = Plugin.getUserInput("Password?", downloadLink);
        if (inValidate(passCode)) {
            logger.info("User has entered blank password, exiting handlePassword");
            passCode = null;
            downloadLink.setProperty("pass", Property.NULL);
            return null;
        }
        if (pwform == null) {
            // so we know handlePassword triggered without any form
            logger.info("Password Form == null");
        } else {
            logger.info("Put password \"" + passCode + "\" entered by user in the DLForm.");
            pwform.put("password", Encoding.urlEncode(passCode));
        }
        downloadLink.setProperty("pass", passCode);
        return passCode;
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
     * @param controlSlot
     *            (+1|-1)
     * */
    private synchronized void controlSlot(final int num, final Account account) {
        if (account == null) {
            int was = maxFree.get();
            maxFree.set(Math.min(Math.max(1, maxFree.addAndGet(num)), totalMaxSimultanFreeDownload.get()));
            logger.info("maxFree was = " + was + " && maxFree now = " + maxFree.get());
        } else {
            int was = maxPrem.get();
            maxPrem.set(Math.min(Math.max(1, maxPrem.addAndGet(num)), totalMaxSimultanPremDownload.get()));
            logger.info("maxPrem was = " + was + " && maxPrem now = " + maxPrem.get());
        }
    }

    /**
     * ControlSimHost, On error it will set the upper mark for 'max sim dl per host'. This will be the new 'static' setting used going
     * forward. Thus prevents new downloads starting when not possible and is self aware and requires no coder interaction.
     * 
     * @param account
     * 
     * @category 'Experimental', Mod written February 2013
     * */
    private synchronized void controlSimHost(final Account account) {
        if (usedHost == null) return;
        int was, current;
        if (account != null && account.getBooleanProperty("nopremium")) {
            // free account
            was = maxFreeAccSimDlPerHost.get();
            maxFreeAccSimDlPerHost.set(getHashedHashedValue(account) - 1);
            current = maxFreeAccSimDlPerHost.get();
        } else if (account != null && !account.getBooleanProperty("nopremium")) {
            // premium account
            was = maxPremAccSimDlPerHost.get();
            maxPremAccSimDlPerHost.set(getHashedHashedValue(account) - 1);
            current = maxPremAccSimDlPerHost.get();
        } else {
            // non account
            was = maxNonAccSimDlPerHost.get();
            maxNonAccSimDlPerHost.set(getHashedHashedValue(account) - 1);
            current = maxNonAccSimDlPerHost.get();
        }
        if (account == null) {
            logger.info("maxSimPerHost = Guest @ " + acctype + " -> was = " + was + " && new upper limit = " + current);
        } else {
            logger.info("maxSimPerHost = " + account.getUser() + " @ " + acctype + " -> was = " + was + " && new upper limit = " + current);
        }
    }

    /**
     * This matches dllink against an array of used 'host' servers. Use this when site have multiple download servers and they allow x
     * connections to ip/host server. Currently JD allows a global connection controller and doesn't allow for handling of different
     * hosts/IP setup. This will help with those situations by allowing more connection when possible.
     * 
     * @param Account
     *            Account that's been used, can be null
     * @param DownloadLink
     * @param action
     *            To add or remove slot, true == adds, false == removes
     * @throws Exception
     * */
    private synchronized void controlHost(final Account account, final DownloadLink downloadLink, final boolean action) throws Exception {

        // xfileshare valid links are either https://((sub.)?domain|IP)(:port)?/blah
        usedHost = new Regex(dllink, "https?://([^/\\:]+)").getMatch(0);
        if (inValidate(dllink) || usedHost == null) {
            if (inValidate(dllink))
                logger.warning("Invalid URL given to controlHost");
            else
                logger.warning("Regex on usedHost failed, Please report this to JDownloader Development Team");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        // save finallink and use it for later, this script can determine if it's usable at a later stage. (more for dev purposes)
        downloadLink.setProperty(directlinkproperty, dllink);

        // place account into a place holder, for later references;
        Account accHolder = account;

        // allows concurrent logic
        boolean thisAccount = allowsConcurrent(account);
        boolean continu = true;
        if (!hostMap.isEmpty()) {
            // compare stored values within hashmap, determine if they allow concurrent with current account download request!
            for (Entry<Account, HashMap<String, Integer>> holder : hostMap.entrySet()) {
                if (!allowsConcurrent(holder.getKey())) {
                    continu = false;
                }
            }
            if (thisAccount && continu) {
                // current account allows concurrent
                // hostmap entries c
            }

        }

        String user = null;
        Integer simHost;
        if (accHolder != null) {
            user = accHolder.getUser();
            if (accHolder.getBooleanProperty("nopremium")) {
                // free account
                simHost = maxFreeAccSimDlPerHost.get();
            } else {
                // normal account
                simHost = maxPremAccSimDlPerHost.get();
            }
        } else {
            user = "Guest";
            simHost = maxNonAccSimDlPerHost.get();
        }
        user = user + " @ " + acctype;

        if (!action) {
            // download finished (completed, failed, etc), check for value and remove a value
            Integer usedSlots = getHashedHashedValue(account);
            if (usedSlots == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            setHashedHashKeyValue(account, -1);
            if (usedSlots.equals(1)) {
                logger.info("controlHost = " + user + " -> " + usedHost + " :: No longer used!");
            } else {
                logger.info("controlHost = " + user + " -> " + usedHost + " :: " + getHashedHashedValue(account) + " simulatious connection(s)");
            }
        } else {
            // New download started, check finallink host against hostMap values && max(Free|Prem)SimDlHost!

            /*
             * max(Free|Prem)SimDlHost prevents more downloads from starting on a given host! At least until one of the previous downloads
             * finishes. This is best practice otherwise you have to use some crude system of waits, but you have no control over to reset
             * the count. Highly dependent on how fast or slow the users connections is.
             */
            if (isHashedHashedKey(account, usedHost)) {
                Integer usedSlots = getHashedHashedValue(account);
                if (usedSlots == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                if (!usedSlots.equals(simHost)) {
                    setHashedHashKeyValue(account, 1);
                    logger.info("controlHost = " + user + " -> " + usedHost + " :: " + getHashedHashedValue(account) + " simulatious connection(s)");
                } else {
                    logger.info("controlHost = " + user + " -> " + usedHost + " :: Too many concurrent connectons. We will try again when next possible.");
                    controlSlot(-1, accHolder);
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Too many concurrent connectons. We will try again when next possible.", 10 * 1000);
                }
            } else {
                // virgin download for given usedHost.
                setHashedHashKeyValue(account, 1);
                logger.info("controlHost = " + user + " -> " + usedHost + " :: " + getHashedHashedValue(account) + " simulatious connection(s)");
            }
        }
    }

    /**
     * Sets Key and Values to respective Account stored within hostMap
     * 
     * @param account
     *            Account that's been used, can be null
     * @param x
     *            Integer positive or negative. Positive adds slots. Negative integer removes slots.
     * */
    private void setHashedHashKeyValue(final Account account, final Integer x) {
        if (usedHost == null || x == null) return;
        HashMap<String, Integer> holder = new HashMap<String, Integer>();
        if (!hostMap.isEmpty()) {
            // load hostMap within holder if not empty
            holder = hostMap.get(account);
            // remove old hashMap reference, prevents creating duplicate entry of 'account' when returning result.
            if (holder.containsKey(account)) hostMap.remove(account);
        }
        String currentKey = getHashedHashedKey(account);
        Integer currentValue = getHashedHashedValue(account);
        if (currentKey == null) {
            // virgin entry
            holder.put(usedHost, 1);
        } else {
            if (currentValue.equals(1) && x.equals(-1)) {
                // remove table
                holder.remove(usedHost);
            } else {
                // add value, must first remove old to prevent duplication
                holder.remove(usedHost);
                holder.put(usedHost, currentValue + x);
            }
        }
        if (holder.isEmpty()) {
            // the last value(download) within holder->account. Remove entry to reduce memory allocation
            hostMap.remove(account);
        } else {
            // put updated holder back into hostMap
            hostMap.put(account, holder);
        }
    }

    /**
     * Returns String key from Account@usedHost from hostMap
     * 
     * @param account
     *            Account that's been used, can be null
     * */
    private String getHashedHashedKey(final Account account) {
        if (usedHost == null) return null;
        if (hostMap.containsKey(account)) {
            final HashMap<String, Integer> accKeyValue = hostMap.get(account);
            if (accKeyValue.containsKey(usedHost)) {
                for (final Entry<String, Integer> keyValue : accKeyValue.entrySet()) {
                    String key = keyValue.getKey();
                    return key;
                }
            }
        }
        return null;
    }

    /**
     * Returns integer value from Account@usedHost from hostMap
     * 
     * @param account
     *            Account that's been used, can be null
     * */
    private Integer getHashedHashedValue(final Account account) {
        if (usedHost == null) return null;
        if (hostMap.containsKey(account)) {
            final HashMap<String, Integer> accKeyValue = hostMap.get(account);
            if (accKeyValue.containsKey(usedHost)) {
                for (final Entry<String, Integer> keyValue : accKeyValue.entrySet()) {
                    Integer value = keyValue.getValue();
                    return value;
                }
            }
        }
        return null;
    }

    /**
     * Returns true if hostMap contains 'key'
     * 
     * @param account
     *            Account that's been used, can be null
     * @param key
     *            String of what ever you want to find
     * */
    private boolean isHashedHashedKey(final Account account, final String key) {
        if (key == null) return false;
        final HashMap<String, Integer> accKeyValue = hostMap.get(account);
        if (accKeyValue != null) {
            if (accKeyValue.containsKey(key)) {
                for (final Entry<String, Integer> keyValue : accKeyValue.entrySet()) {
                    if (keyValue.getKey().equals(key)) return true;
                }
            }
        }
        return false;
    }

    /**
     * Validates string to series of conditions, null, whitespace, or "". This saves effort factor within if/for/while statements
     * 
     * @param s
     *          Imported String to match against.
     * @returns true 
     *          on valid rule match
     * @returns false
     *          on invalid rule match.
     * @author raztoki
     * */
    private boolean inValidate(final String s) {
        if (s == null || s != null && (s.matches("[\r\n\t ]+") || s.equals("")))
            return true;
        else
            return false;
    }

    // TODO: remove this when v2 becomes stable. use br.getFormbyKey(String key, String value)
    /**
     * Returns the first form that has a 'key' that equals 'value'.
     * 
     * @param key
     *          name
     * @param value
     *          expected value
     * @param ibr
     *          import browser
     * @return 
     * */
    private Form getFormByKey(final Browser ibr, final String key, final String value) {
        Form[] workaround = ibr.getForms();
        if (workaround != null) {
            for (Form f : workaround) {
                for (InputField field : f.getInputFields()) {
                    if (key != null && key.equals(field.getKey())) {
                        if (value == null && field.getValue() == null) return f;
                        if (value != null && value.equals(field.getValue())) return f;
                    }
                }
            }
        }
        return null;
    }

    /**
     * If form contain both " and ' quotation marks within input fields it can return null values, thus you submit wrong/incorrect data re:
     * InputField parse(final String data). Affects revision 19688 and earlier!
     * 
     * TODO: remove after JD2 goes stable!
     * 
     * @author raztoki
     * */
    private Form cleanForm(Form form) {
        if (form == null) return null;
        String data = form.getHtmlCode();
        ArrayList<String> cleanupRegex = new ArrayList<String>();
        cleanupRegex.add("(\\w+\\s*=\\s*\"[^\"]+\")");
        cleanupRegex.add("(\\w+\\s*=\\s*'[^']+')");
        for (String reg : cleanupRegex) {
            String results[] = new Regex(data, reg).getColumn(0);
            if (results != null) {
                String quote = new Regex(reg, "(\"|')").getMatch(0);
                for (String result : results) {
                    String cleanedResult = result.replaceFirst(quote, "\\\"").replaceFirst(quote + "$", "\\\"");
                    data = data.replace(result, cleanedResult);
                }
            }
        }
        return new Form(data);
    }

    /**
     * This allows backward compatibility for design flaw in setHtmlCode(), It injects updated html into all browsers that share the same
     * request id. This is needed as request.cloneRequest() was never fully implemented like browser.cloneBrowser().
     * 
     * @param ibr
     *            Import Browser
     * @param t
     *            Provided replacement string output browser
     * @author raztoki
     * */
    private Browser cleanupBrowser(final Browser ibr, final String t) throws Exception {
        if (br.isDebug()) logger.info("\r\ndirtyMD5sum = " + JDHash.getMD5(ibr.toString()) + "\r\ncleanMD5sum = " + JDHash.getMD5(t) + "\r\n");
        Request req = ibr.createRequest(ibr.getURL());
        URLConnectionAdapter con = ibr.getRequest().getHttpConnection();
        req = new Request(con) {
            {
                requested = true;
            }

            @Override
            public long postRequest() throws IOException {
                return 0;
            }

            @Override
            public void preRequest() throws IOException {
            }
        };
        req.setHtmlCode(t);
        ibr.setRequest(req);
        return ibr;
    }

}
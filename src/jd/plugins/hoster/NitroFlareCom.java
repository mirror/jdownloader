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
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
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
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.os.CrossSystem;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "nitroflare.com" }, urls = { "https?://(www\\.)?nitroflare\\.com/view/[A-Z0-9]+" }, flags = { 2 })
public class NitroFlareCom extends PluginForHost {

    private final String         language = System.getProperty("user.language");
    private static final String  baseURL  = "https://nitroflare.com";
    private static final String  apiURL   = "https://www.nitroflare.com/api";
    private static AtomicBoolean useAPI   = new AtomicBoolean(true);

    public NitroFlareCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(baseURL + "/payment");
    }

    @Override
    public String getAGBLink() {
        return baseURL + "/tos";
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("http://", "https://"));
    }

    private void setConstants(final Account account) {
        if (account != null) {
            if (account.getBooleanProperty("free", false)) {
                // free account
                chunks = 1;
                resumes = false;
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
            resumes = false;
            isFree = true;
            directlinkproperty = "freelink";
            logger.finer("setConstants = Guest Download :: isFree = " + isFree + ", upperChunks = " + chunks + ", Resumes = " + resumes);
        }
    }

    public boolean canHandle(DownloadLink downloadLink, Account account) {
        return true;
    }

    public boolean checkLinks(final DownloadLink[] urls) {
        boolean okay = true;
        try {
            final Browser br = new Browser();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    if (links.size() > 100 || index == urls.length) {
                        break;
                    }
                    links.add(urls[index]);
                    index++;
                }
                final StringBuilder sb = new StringBuilder();
                sb.append("files=");
                boolean atLeastOneDL = false;
                for (final DownloadLink dl : links) {
                    if (atLeastOneDL) {
                        sb.append(",");
                    }
                    sb.append(getFUID(dl));
                    atLeastOneDL = true;
                }
                br.getPage(apiURL + "/getFileInfo?" + sb);
                for (final DownloadLink dl : links) {
                    final String filter = br.getRegex("(\"" + getFUID(dl) + "\":\\{.*?\\})").getMatch(0);
                    if (filter == null) {
                        dl.setProperty("apiInfo", Property.NULL);
                        okay = false;
                        continue;
                    }
                    final String status = getJson(filter, "status");
                    if ("online".equalsIgnoreCase(status)) {
                        dl.setAvailable(true);
                    } else {
                        dl.setAvailable(false);
                    }
                    final String name = getJson(filter, "name");
                    final String size = getJson(filter, "size");
                    final String md5 = getJson(filter, "md5");
                    final String prem = getJson(filter, "premiumOnly");
                    final String pass = getJson(filter, "password");
                    if (name != null) {
                        dl.setFinalFileName(name);
                    }
                    if (size != null) {
                        dl.setVerifiedFileSize(Long.parseLong(size));
                    }
                    if (md5 != null) {
                        dl.setMD5Hash(md5);
                    }
                    if (prem != null) {
                        dl.setProperty("premiumRequired", Boolean.parseBoolean(prem));
                    }
                    if (pass != null) {
                        dl.setProperty("passwordRequired", Boolean.parseBoolean(pass));
                    }
                    dl.setProperty("apiInfo", Boolean.TRUE);
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            return false;
        }
        return okay;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:30.0) Gecko/20100101 Firefox/30.0");
        br.setCookie("http://nitroflare.com/", "lang", "en");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">File doesn't exist<|This file has been removed due")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = br.getRegex("<b>File Name: </b><span title=\"([^<>\"]*?)\"").getMatch(0);
        final String filesize = br.getRegex("dir=\"ltr\" style=\"text-align: left;\">([^<>\"]*?)</span>").getMatch(0);
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (link.getBooleanProperty("apiInfo", Boolean.FALSE) == Boolean.FALSE) {
            /* no apiInfos available, set unverified name/size here */
            link.setName(Encoding.htmlDecode(filename.trim()));
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        if (useAPI.get()) {
            handleDownload_API(downloadLink, null);
        } else {
            requestFileInformation(downloadLink);
            dllink = checkDirectLink(downloadLink, directlinkproperty);
            if (dllink == null) {
                final Browser br2 = br.cloneBrowser();
                final String fid = new Regex(downloadLink.getDownloadURL(), "/view/([A-Z0-9]+)/").getMatch(0);
                br.postPage(br.getURL(), "goToFreePage=");

                final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                rc.findID();

                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.postPage("/ajax/freeDownload.php", "method=startTimer&fileId=" + fid);
                if (br.containsHTML("This file is available with premium key only")) {
                    throwPremiumRequiredException();
                } else if (br.containsHTML("﻿Downloading is not possible")) {
                    final String waitminutes = br.getRegex("You have to wait (\\d+) minutes to download your next file").getMatch(0);
                    if (waitminutes != null) {
                        throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Long.parseLong(waitminutes) * 60 * 1001l);
                    }
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
                }

                br2.getPage("/js/downloadFree.js?v=1.0.1");
                final String waittime = br2.getRegex("var time = (\\d+);").getMatch(0);
                int wait = 30;
                if (waittime != null) {
                    wait = Integer.parseInt(waittime);
                }
                this.sleep(wait * 1001l, downloadLink);

                for (int i = 1; i <= 5; i++) {
                    rc.load();
                    final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    final String c = getCaptchaCode("recaptcha", cf, downloadLink);
                    br.postPage("/ajax/freeDownload.php", "method=fetchDownload&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c));
                    if (br.containsHTML("The captcha wasn't entered correctly|You have to fill the captcha")) {
                        continue;
                    }
                    break;
                }
                if (br.containsHTML("The captcha wasn't entered correctly|You have to fill the captcha")) {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
                if (br.containsHTML("File doesn't exist")) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }

                dllink = br.getRegex("\"(https?://[a-z0-9\\-_]+\\.nitroflare\\.com/[^<>\"]*?)\"").getMatch(0);
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumes, chunks);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            downloadLink.setProperty(directlinkproperty, dllink);
            dl.startDownload();
        }
    }

    protected static Object LOCK = new Object();

    /**
     * Validates account and returns correct account info, when user has provided incorrect user pass fields to JD client. Or Throws
     * exception indicating users mistake, when it's a irreversible mistake.
     *
     * @param account
     * @return
     * @throws PluginException
     */
    private String validateAccount(final Account account) throws PluginException {
        synchronized (LOCK) {
            final String user = account.getUser();
            final String pass = account.getPass();
            if (inValidate(pass)) {
                // throw new PluginException(LinkStatus.ERROR_PREMIUM,
                // "\r\nYou haven't provided a valid password or premiumKey (this field can not be empty)!",
                // PluginException.VALUE_ID_PREMIUM_DISABLE);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nYou haven't provided a valid password (this field can not be empty)!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            if (pass.matches("(?-i)NF[a-zA-Z0-9]{10}")) {
                // no need to urlencode, this is always safe.
                // return "user=&premiumKey=" + pass;
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                String title = "Nitroflare no longer accepts PremiumKeys";
                                String message = "You are using PremiumKey! These are no longer accepted on Nitroflare.\r\n";
                                message += "You will need to bind your PremiumKey to account. Please Click -YES- for more information.";
                                if (CrossSystem.isOpenBrowserSupported()) {
                                    int result = JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
                                    if (JOptionPane.OK_OPTION == result) {
                                        CrossSystem.openURL(new URL(baseURL + "/upgradeTutorial"));
                                    }
                                }
                            } catch (Throwable e) {
                            }
                        }
                    });
                } catch (Throwable e) {
                }

                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPremiumKeys not accepted, you need to use Account (email and password).", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            if (inValidate(user) || !user.matches(".+@.+")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nYou haven't provided a valid username (must be email address)!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            // urlencode required!
            return "user=" + Encoding.urlEncode(user) + "&premiumKey=" + Encoding.urlEncode(pass);
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        synchronized (LOCK) {
            AccountInfo ai = new AccountInfo();
            br.getPage(apiURL + "/getKeyInfo?" + validateAccount(account));
            handleApiErrors(account, null);
            final String expire = getJson("expiryDate");
            final String status = getJson("status");
            final String storage = getJson("storageUsed");
            final String trafficLeft = getJson("trafficLeft");
            final String trafficMax = getJson("trafficMax");
            if (inValidate(status)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (!inValidate(expire) && !"0".equalsIgnoreCase(expire)) {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH));
            }
            if ("banned".equalsIgnoreCase(status)) {
                if ("de".equalsIgnoreCase(language)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nYour account has been banned! (transate me)", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nYour account has been banned!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            } else if ("expired".equalsIgnoreCase(status) || "inactive".equalsIgnoreCase(status) || ai.isExpired()) {
                // expired(free)? account
                account.setProperty("free", true);
                // dont support free account?
                ai.setStatus("Free Account");
                ai.setExpired(true);
            } else if ("active".equalsIgnoreCase(status)) {
                // premium account
                account.setProperty("free", false);
                ai.setStatus("Premium Account");
                account.setValid(true);
            }
            if (!inValidate(storage)) {
                ai.setUsedSpace(Long.parseLong(storage));
            }
            if (!inValidate(trafficLeft)) {
                ai.setTrafficLeft(Long.parseLong(trafficLeft));
            }
            if (!inValidate(trafficMax)) {
                ai.setTrafficMax(Long.parseLong(trafficMax));
            }
            return ai;
        }
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        handleDownload_API(downloadLink, account);
    }

    private void handleDownload_API(final DownloadLink downloadLink, final Account account) throws Exception {
        setConstants(account);
        try {
            br.setAllowedResponseCodes(500);
        } catch (final Throwable t) {
        }
        reqFileInformation(downloadLink);
        dllink = checkDirectLink(downloadLink, directlinkproperty);
        if (inValidate(dllink)) {
            // links that require premium...
            if (downloadLink.getBooleanProperty("premiumRequired", false) && account == null) {
                throwPremiumRequiredException();
            }
            final String req = apiURL + "/getDownloadLink?file=" + getFUID(downloadLink) + (account != null ? "&" + validateAccount(account) : "");
            // needed for when dropping to frame, the cookie session seems to carry over current position in download sequence and you get
            // recaptcha error codes at first step.
            br = new Browser();
            br.getPage(req);
            handleApiErrors(account, downloadLink);
            // error handling here.
            if ("free".equalsIgnoreCase(getJson("linkType"))) {
                // wait
                String delay = getJson("delay");
                long startTime = System.currentTimeMillis();
                String recap = getJson("recaptchaPublic");
                if (!inValidate(recap)) {
                    logger.info("Detected captcha method \"Re Captcha\"");
                    final Browser captcha = br.cloneBrowser();
                    final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                    final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(captcha);
                    int repeat = 5;
                    for (int i = 1; i != repeat; i++) {
                        rc.setId(recap);
                        rc.load();
                        final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                        final String c = getCaptchaCode("recaptcha", cf, downloadLink);
                        if (!inValidate(delay) && i == 1) {
                            sleep((Long.parseLong(delay) * 1000) - (System.currentTimeMillis() - startTime), downloadLink);
                        }
                        br.getPage(req + "&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c));
                        if ("error".equalsIgnoreCase(getJson("type")) && "6".equalsIgnoreCase(getJson("code")) && i + 1 != repeat) {
                            continue;
                        } else if ("error".equalsIgnoreCase(getJson("type")) && "6".equalsIgnoreCase(getJson("code")) && i + 1 == repeat) {
                            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                        } else {
                            break;
                        }
                    }
                }
            }
            // some times error 4 is found here
            handleApiErrors(account, downloadLink);
            dllink = getJson("url");
            if (inValidate(dllink)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumes, chunks);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            final String err1 = "ERROR: Wrong IP. If you are using proxy, please turn it off / Or buy premium key to remove the limitation";
            if (br.containsHTML(err1)) {
                // I don't see why this would happening logs contain no proxy!
                throw new PluginException(LinkStatus.ERROR_FATAL, err1);
            } else if (account != null && br.getHttpConnection() != null && br.toString().equals("Your premium has reached the maximum volume for today")) {
                synchronized (LOCK) {
                    final AccountInfo ai = account.getAccountInfo();
                    ai.setTrafficLeft(0);
                    account.setAccountInfo(ai);
                    account.setTempDisabled(true);
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            } else if (br.containsHTML("ERROR: link expired. Please unlock the file again")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'link expired'", 2 * 60 * 1000l);
            }

            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty(directlinkproperty, dllink);
        dl.startDownload();
    }

    private void handleApiErrors(final Account account, final DownloadLink downloadLink) throws Exception {
        // API Error handling codes.
        // 1 => 'Access denied', (banned for trying incorrect x times for y minutes
        // 2 => 'Invalid premium key',
        // 3 => 'Bad input',
        // 4 => 'File doesn't exist',
        // 5 => 'Free downloading is not possible. You have to wait 60 minutes between free downloads.',
        // 6 => 'Invalid captcha',
        // 7 => 'Free users can download only 1 file at the same time'
        // 8 => ﻿{"type":"error","message":"Wrong login","code":8}

        final String type = getJson("type");
        final String code = getJson("code");
        final String msg = getJson("message");
        final int cde = (!inValidate(code) && code.matches("\\d+") ? Integer.parseInt(code) : -1);
        if ("error".equalsIgnoreCase(type)) {
            try {
                if (cde == 1) {
                    if (account == null) {
                        throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, (!inValidate(msg) ? msg : null), 60 * 60 * 1000);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, (!inValidate(msg) ? msg : null), 60 * 60 * 1000);
                    }
                } else if (cde == 2) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, msg, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if (cde == 3 && downloadLink != null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, (!inValidate(msg) ? msg : null));
                } else if (cde == 4 && downloadLink != null) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, (!inValidate(msg) ? msg : null));
                } else if (cde == 5 && downloadLink != null) {
                    final String time = new Regex(msg, "You have to wait (\\d+) minutes").getMatch(0);
                    final long t = (!inValidate(time) ? Long.parseLong(time) * 60 * 1000 : 1 * 60 * 60 * 1000);
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, (!inValidate(msg) ? msg : null), t);
                } else if (cde == 7 && downloadLink != null) {
                    // shouldn't happen as its hard limited.
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, (!inValidate(msg) ? msg : "You can't download more than one file within a certain time period in free mode"), 60 * 60 * 1000l);
                } else if (cde == 8) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nIncorrect login attempt!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if (cde == 9) {
                    if (account != null) {
                        account.setAccountInfo(fetchAccountInfo(account));
                        throw new PluginException(LinkStatus.ERROR_RETRY, (!inValidate(msg) ? msg : null));
                    } else {
                        // this shouldn't happen
                    }
                }
            } catch (final PluginException p) {
                if (!inValidate(msg)) {
                    logger.warning("ERROR :: " + msg);
                }
                throw p;
            }
        }
    }

    private void throwPremiumRequiredException() throws PluginException {
        try {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } catch (final Throwable e) {
            if (e instanceof PluginException) {
                throw (PluginException) e;
            }
        }
        throw new PluginException(LinkStatus.ERROR_FATAL, "This file is only available to Premium Members");
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    private AvailableStatus reqFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (!checkLinks(new DownloadLink[] { link }) || !link.isAvailabilityStatusChecked()) {
            link.setAvailableStatus(AvailableStatus.UNCHECKABLE);
        } else if (!link.isAvailable()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return getAvailableStatus(link);
    }

    private AvailableStatus getAvailableStatus(DownloadLink link) {
        try {
            final Field field = link.getClass().getDeclaredField("availableStatus");
            field.setAccessible(true);
            Object ret = field.get(link);
            if (ret != null && ret instanceof AvailableStatus) {
                return (AvailableStatus) ret;
            }
        } catch (final Throwable e) {
        }
        return AvailableStatus.UNCHECKED;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        if (link != null) {
            link.setProperty("apiInfo", Property.NULL);
        }
    }

    private String  dllink             = null;
    private String  directlinkproperty = null;
    private int     chunks             = 0;
    private boolean resumes            = true;
    private boolean isFree             = true;

    private String getFUID(DownloadLink downloadLink) {
        final String fuid = new Regex(downloadLink.getDownloadURL(), "nitroflare\\.com/view/([A-Z0-9]+)").getMatch(0);
        return fuid;
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from String source.
     *
     * @author raztoki
     * */
    private String getJson(final String source, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(source, key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     * */
    private String getJson(final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(br.toString(), key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from provided Browser.
     *
     * @author raztoki
     * */
    private String getJson(final Browser ibr, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(ibr.toString(), key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value given JSon Array of Key from JSon response provided String source.
     *
     * @author raztoki
     * */
    private String getJsonArray(final String source, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJsonArray(source, key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value given JSon Array of Key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     * */
    private String getJsonArray(final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(br.toString(), key);
    }

    /**
     * Wrapper<br/>
     * Tries to return String[] value from provided JSon Array
     *
     * @author raztoki
     * @param source
     * @return
     */
    private String[] getJsonResultsFromArray(final String source) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJsonResultsFromArray(source);
    }

    /**
     * Validates string to series of conditions, null, whitespace, or "". This saves effort factor within if/for/while statements
     *
     * @param s
     *            Imported String to match against.
     * @return <b>true</b> on valid rule match. <b>false</b> on invalid rule match.
     * @author raztoki
     * */
    private boolean inValidate(final String s) {
        if (s == null || s != null && (s.matches("[\r\n\t ]+") || s.equals(""))) {
            return true;
        } else {
            return false;
        }
    }

    public boolean hasCaptcha(final DownloadLink downloadLink, final jd.plugins.Account acc) {
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

    public boolean hasAutoCaptcha() {
        return false;
    }
}
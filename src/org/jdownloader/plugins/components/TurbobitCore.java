package org.jdownloader.plugins.components;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
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
import jd.plugins.components.SiteType.SiteTemplate;
import jd.plugins.components.UserAgents;
import jd.utils.JDHexUtils;
import jd.utils.JDUtilities;

import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.keycaptcha.KeyCaptchaShowDialogTwo;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class TurbobitCore extends antiDDoSForHost {
    /**
     * TODO: Check if we already got errorhandling for this kind of error http://turbobit.net/error/download/dcount/xxxtesst --> "
     *
     * An amount of maximum downloads for this link has been exceeded "
     *
     * When adding new domains here also add them to the turbobit.net decrypter (TurboBitNetFolder)
     *
     */
    /* Settings */
    // private static final String SETTING_JAC = "SETTING_JAC";
    private static final String  SETTING_FREE_PARALLEL_DOWNLOADSTARTS         = "SETTING_FREE_PARALLEL_DOWNLOADSTARTS";
    private static final int     FREE_MAXDOWNLOADS_PLUGINSETTING              = 20;
    private static final boolean prefer_single_linkcheck_via_mass_linkchecker = true;
    private static final String  premRedirectLinks                            = ".*//?download/redirect/[A-Za-z0-9]+/[a-z0-9]+";

    public TurbobitCore(final PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
        enablePremium(getMainpage() + "/turbo/emoney/");
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) throws PluginException {
        // changing to temp hosts (subdomains causes issues with multihosters.
        final String protocol = new Regex(link.getDownloadURL(), "https?://").getMatch(-1);
        final String uid = getFUID(link);
        if (uid == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Not all added URLs have to be corrected! */
        if (!link.getDownloadURL().matches(premRedirectLinks)) {
            String newDownloadURL = protocol + this.getHost() + "/" + uid;
            if (downloadurls_need_html_ending()) {
                newDownloadURL += ".html";
            }
            link.setUrlDownload(newDownloadURL);
            link.setLinkID(uid);
        }
    }

    /**
     * 2019-05-11: There is also an API-version of this but it seems like it only returns online/offline - no filename/filesize:
     * https://hitfile.net/linkchecker/api
     */
    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) {
            return false;
        }
        final ArrayList<DownloadLink> deepChecks = new ArrayList<DownloadLink>();
        try {
            final Browser br = new Browser();
            prepBrowserWebsite(br, null);
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.setCookiesExclusive(true);
            final StringBuilder sb = new StringBuilder();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    /* we test 50 links at once */
                    if (index == urls.length || links.size() > 49) {
                        break;
                    }
                    links.add(urls[index]);
                    index++;
                }
                sb.delete(0, sb.capacity());
                sb.append("links_to_check=");
                for (final DownloadLink dl : links) {
                    correctDownloadLink(dl);
                    sb.append(Encoding.urlEncode(dl.getDownloadURL()));
                    sb.append("%0A");
                }
                /* remove last */
                sb.delete(sb.length() - 3, sb.length());
                /*
                 * '/linkchecker/csv' is the official "API" method but this will only return fileID and online/offline - not even the
                 * filename
                 */
                postPage(br, "https://" + this.getHost() + "/linkchecker/check", sb.toString());
                for (final DownloadLink dllink : links) {
                    final Regex fileInfo = br.getRegex("<td>" + getFUID(dllink) + "</td>[\t\n\r ]*<td>([^<]*)</td>[\t\n\r ]*<td style=\"text-align:center;\">(?:[\t\n\r ]*)?<img src=\"(?:[^\"]+)?/(done|error)\\.png\"");
                    if (fileInfo.getMatches() == null || fileInfo.getMatches().length == 0) {
                        dllink.setAvailable(false);
                        logger.warning("Linkchecker broken for " + getHost() + " Example link: " + dllink.getDownloadURL());
                    } else {
                        if (fileInfo.getMatch(1).equals("error")) {
                            dllink.setAvailable(false);
                        } else {
                            final String name = fileInfo.getMatch(0);
                            dllink.setAvailable(true);
                            dllink.setFinalFileName(Encoding.htmlDecode(name.trim()));
                            if (dllink.getKnownDownloadSize() < 0) {
                                deepChecks.add(dllink);
                            }
                        }
                    }
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            return false;
        } finally {
            for (final DownloadLink deepCheck : deepChecks) {
                try {
                    requestFileInformation_Web(deepCheck);
                } catch (final Throwable e) {
                }
            }
        }
        return true;
    }

    // Also check HitFileNet plugin if this one is broken
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        if (prefer_single_linkcheck_via_mass_linkchecker && supports_mass_linkcheck()) {
            requestFileInformation_Mass_Linkchecker(downloadLink);
        } else {
            requestFileInformation_Web(downloadLink);
        }
        return AvailableStatus.TRUE;
    }

    public AvailableStatus requestFileInformation_Mass_Linkchecker(final DownloadLink downloadLink) throws IOException, PluginException {
        checkLinks(new DownloadLink[] { downloadLink });
        if (!downloadLink.isAvailabilityStatusChecked()) {
            return AvailableStatus.UNCHECKED;
        }
        if (downloadLink.isAvailabilityStatusChecked() && !downloadLink.isAvailable()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    public AvailableStatus requestFileInformation_Web(final DownloadLink link) throws Exception {
        /* premium links should not be accessed here, we will just return true */
        if (link.getDownloadURL().matches(premRedirectLinks)) {
            return AvailableStatus.TRUE;
        }
        setBrowserExclusive();
        br.setFollowRedirects(true);
        prepBrowserWebsite(br, userAgent.get());
        getPage(link.getDownloadURL());
        if (isFileOfflineWebsite(this.br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filenameSize = "<title>\\s*(?:Download\\s+file|Datei\\s+downloaden)\\s*(.*?)\\s*\\(([\\d\\.,]+\\s*[BMGTP]{1,2})\\)\\s*\\|\\s*(?:TurboBit|Hitfile)\\.net";
        String filename = br.getRegex(filenameSize).getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<span class=(\"|')file\\-title\\1[^>]*>(.*?)</span>").getMatch(1);
        }
        String fileSize = br.getRegex(filenameSize).getMatch(1);
        if (fileSize == null) {
            /* E.g. for hitfile.net, filesize is in brakets '(")(")' */
            fileSize = br.getRegex("class=\"file-size\">(?:\\()?([^<>\"]*?)(?:\\))?<").getMatch(0);
        }
        if (filename != null) {
            link.setName(filename);
        }
        if (fileSize != null) {
            link.setDownloadSize(SizeFormatter.getSize(fileSize.trim().replace(",", ".").replace(" ", "")));
        }
        return AvailableStatus.TRUE;
    }

    public static boolean isFileOfflineWebsite(final Browser br) {
        return br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("(<div class=\"code-404\">404</div>|Файл не найден\\. Возможно он был удален\\.<br|File( was)? not found\\.|It could possibly be deleted\\.)");
    }

    /** 2019-05-09: Seems like API can only be used to check self uploaded content - it is useless for us! */
    public AvailableStatus requestFileInformation_API(final DownloadLink link) throws Exception {
        if (true) {
            return AvailableStatus.UNCHECKABLE;
        }
        /* premium links should not be accessed here, we will just return true */
        if (link.getDownloadURL().matches(premRedirectLinks)) {
            return AvailableStatus.TRUE;
        }
        br.setFollowRedirects(true);
        prepBrowserAPI(br, userAgent.get());
        getPage("https://turbobit.net/v001/files/" + this.getLinkID(link));
        return AvailableStatus.UNCHECKABLE;
    }

    // private String escape(final String s) {
    // /* CHECK: we should always use getBytes("UTF-8") or with wanted charset, never system charset! */
    // final byte[] org = s.getBytes();
    // final StringBuilder sb = new StringBuilder();
    // String code;
    // for (final byte element : org) {
    // sb.append('%');
    // code = Integer.toHexString(element);
    // code = code.length() % 2 > 0 ? "0" + code : code;
    // sb.append(code.substring(code.length() - 2));
    // }
    // return sb + "";
    // }
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        if (!account.getUser().matches(".+@.+\\..+")) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBitte gib deine E-Mail Adresse ins Benutzername Feld ein!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlease enter your e-mail adress in the username field!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        try {
            login(account, true);
        } catch (final PluginException e) {
            if (br.containsHTML("Our service is currently unavailable in your country\\.")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nWebsite is currently unavailable in your country!\r\nDiese webseite ist in deinem Land momentan nicht verfügbar!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            throw e;
        }
        ai.setUnlimitedTraffic();
        // >Turbo access till 27.09.2015</span>
        String expire = br.getRegex(">Turbo access till\\s*(.*?)\\s*</span>").getMatch(0);
        if (expire == null) {
            /* 2019-05-22: hitfile.net */
            expire = br.getRegex("'/premium'\\s*>\\s*(\\d+\\.\\d+\\.\\d+)\\s*<").getMatch(0);
        }
        if (expire != null) {
            if (br.containsHTML("<span class='glyphicon glyphicon-ok banturbo'>") || ((br.containsHTML("You have reached") && br.containsHTML("limit of premium downloads")))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "You have reached limit of premium downloads", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire.trim(), "dd.MM.yyyy", Locale.ENGLISH));
            ai.setStatus("Premium Account");
            account.setType(AccountType.PREMIUM);
        } else {
            ai.setStatus("Free Account");
            account.setType(AccountType.FREE);
        }
        return ai;
    }

    @Override
    public String getAGBLink() {
        return getMainpage() + "/rules";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        if (this.getPluginConfig().getBooleanProperty(SETTING_FREE_PARALLEL_DOWNLOADSTARTS, false)) {
            return FREE_MAXDOWNLOADS_PLUGINSETTING;
        } else {
            return 1;
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    protected String getAPIKey() {
        String apikey = this.getPluginConfig().getStringProperty("APIKEY", null);
        if (StringUtils.isEmpty(apikey) || apikey.equalsIgnoreCase("DEFAULT")) {
            /* No APIKey set or default? Return default key. */
            apikey = getAPIKeyDefault();
        }
        return apikey;
    }

    public String getAPIKeyDefault() {
        return null;
    }

    /**
     * @return true: Website supports https and plugin will prefer https. <br />
     *         false: Website does not support https - plugin will avoid https. <br />
     *         default: true
     */
    public boolean supports_https() {
        return true;
    }

    public boolean supports_mass_linkcheck() {
        return true;
    }

    /** E.g. '.html' needed at the end of downloadurls: turbobit.net - e.g. NOT needed: hitfile.net */
    public boolean downloadurls_need_html_ending() {
        return true;
    }

    /** If no waittime is found or it is less than this, a fallback waittime will get used. */
    public int minimum_pre_download_waittime_seconds() {
        return 60;
    }

    /** Waittime which is used if no waittime was found or the found waittime is less than minimum_pre_download_waittime_seconds */
    protected int get_fallback_waittime() {
        return 600;
    }

    private String id = null;

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        /* support for public premium links */
        if (link.getDownloadURL().matches(premRedirectLinks)) {
            handlePremiumLink(link);
            return;
        }
        requestFileInformation(link);
        if (checkShowFreeDialog(getHost())) {
            super.showFreeDialog(getHost());
        }
        br = new Browser();
        dupe.clear();
        prepBrowserWebsite(br, userAgent.get());
        String dllink = link.getDownloadURL();
        sleep(2500, link);
        br.setFollowRedirects(true);
        getPage(dllink);
        simulateBrowser();
        if (isFileOfflineWebsite(this.br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String fileSize = br.getRegex("class=\"file-size\">([^<>\"]*?)</span>").getMatch(0);
        if (fileSize != null) {
            fileSize = fileSize.replace("М", "M");
            fileSize = fileSize.replace("к", "k");
            fileSize = fileSize.replace("Г", "g");
            fileSize = fileSize.replace("б", "");
            if (!fileSize.endsWith("b")) {
                fileSize = fileSize + "b";
            }
            link.setDownloadSize(SizeFormatter.getSize(fileSize.trim().replace(",", ".").replace(" ", "")));
        }
        id = getFUID(link);
        /** 2019-05-11: Not required for e.g. hitfile.net but it does not destroy anything either so let's set it anyways. */
        br.setCookie(br.getHost(), "turbobit1", getCurrentTimeCookie(br));
        getPage("/download/free/" + id);
        simulateBrowser();
        if (isFileOfflineWebsite(this.br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("<div class=\"free-limit-note\">\\s*Limit reached for free download of this file\\.")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        partTwo(link, true);
    }

    protected String IMAGEREGEX(final String b) {
        final KeyCaptchaShowDialogTwo v = new KeyCaptchaShowDialogTwo();
        /*
         * CHECK: we should always use getBytes("UTF-8") or with wanted charset, never system charset!
         */
        final byte[] o = JDHash.getMD5(Encoding.Base64Decode("Yzg0MDdhMDhiM2M3MWVhNDE4ZWM5ZGM2NjJmMmE1NmU0MGNiZDZkNWExMTRhYTUwZmIxZTEwNzllMTdmMmI4Mw==") + JDHash.getMD5("V2UgZG8gbm90IGVuZG9yc2UgdGhlIHVzZSBvZiBKRG93bmxvYWRlci4=")).getBytes();
        /*
         * CHECK: we should always use new String (bytes,charset) to avoid issues with system charset and utf-8
         */
        if (b != null) {
            return new String(v.D(o, JDHexUtils.getByteArray(b)));
        } else {
            return new String(v.D(o, JDHexUtils.getByteArray("E3CEACB19040D08244C9E5C29D115AE220F83AB417")));
        }
    }

    private final void partTwo(final DownloadLink link, final boolean allowRetry) throws Exception {
        Form captchaform = null;
        final Form[] allForms = br.getForms();
        if (allForms != null && allForms.length != 0) {
            for (final Form aForm : allForms) {
                if (aForm.containsHTML("captcha")) {
                    captchaform = aForm;
                    break;
                }
            }
        }
        String downloadUrl = null, waittime = null;
        if (captchaform == null) {
            handleGeneralErrors();
            if (!br.getURL().contains("/download/free/")) {
                if (allowRetry && br.containsHTML("/download/free/" + Pattern.quote(id))) {
                    // from a log where the first call to this, just redirected to main page and set some cookies
                    getPage("/download/free/" + id);
                    partTwo(link, false);
                    return;
                }
                /* 2019-04-24: This should not happen anymore but still we should retry if it happens. */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Captcha form fail", 1 * 60 * 1000l);
            }
            /* Don't give up yet - check for waittime! */
            if (br.containsHTML(tb(0))) {
                waittime = br.getRegex(tb(1)).getMatch(0);
                final int wait = waittime != null ? Integer.parseInt(waittime) : -1;
                if (wait > 31) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, wait * 1001l);
                } else if (wait < 0) {
                } else {
                    sleep(wait * 1000l, link);
                }
            }
            waittime = br.getRegex(tb(1)).getMatch(0);
            if (waittime != null) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(waittime) * 1001l);
            }
            /* Give up */
            logger.warning("captchaform equals null!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Fix Form */
        if (StringUtils.equalsIgnoreCase(captchaform.getAction(), "#")) {
            captchaform.setAction(br.getURL());
        }
        if (!captchaform.hasInputFieldByName("captcha_type") && captchaform.containsHTML("recaptcha2")) {
            /* E.g. hitfile.net */
            captchaform.put("captcha_type", "recaptcha2");
        }
        if (!captchaform.hasInputFieldByName("captcha_subtype") && captchaform.containsHTML("captcha_subtype")) {
            /* E.g. hitfile.net */
            captchaform.put("captcha_subtype", "");
        }
        if (br.containsHTML("class=\"g-recaptcha\"")) {
            /* ReCaptchaV2 */
            String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
            captchaform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            submitForm(captchaform);
        } else {
            /* This should not happen - see old captcha handling in TurboBitNet class revision 40594 */
            logger.warning("Captcha-handling failed");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (br.getHttpConnection().getResponseCode() == 302) {
            /* Solving took too long? */
            invalidateLastChallengeResponse();
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        simulateBrowser();
        /** 2019-05-09: TODO: Remove this old overcomplicated handling */
        /* Ticket Time */
        String ttt = parseImageUrl(br.getRegex(IMAGEREGEX(null)).getMatch(0), true);
        int maxWait = 9999, realWait = 0;
        for (String s : br.getRegex(tb(11)).getColumn(0)) {
            realWait = Integer.parseInt(s);
            if (realWait == 0) {
                continue;
            }
            if (realWait < maxWait) {
                maxWait = realWait;
            }
        }
        boolean waited = false;
        int tt = getPreDownloadWaittime(br);
        if (ttt != null) {
            tt = Integer.parseInt(ttt);
            tt = tt < realWait ? tt : realWait;
            if (tt < 30 || tt > 600) {
                ttt = parseImageUrl(tb(2) + tt + "};" + br.getRegex(tb(3)).getMatch(0), false);
                if (ttt == null) {
                    /* 2019-05-09: Old code - according to old errortext, this state means that we got blocked by the host ... */
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Failed to find waittime", 10 * 60 * 60 * 1001l);
                }
                tt = Integer.parseInt(ttt);
            }
            logger.info(" Waittime detected, waiting " + String.valueOf(tt) + " seconds from now on...");
            if (tt > 250) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Limit reached or IP already loading", tt * 1001l);
            }
            waited = true;
        }
        String continueLink = br.getRegex("\\$\\('#timeoutBox'\\)\\.load\\(\"(/[^\"]+)\"\\);").getMatch(0);
        if (continueLink == null) {
            continueLink = "/download/getLinkTimeout/" + id;
        }
        if (!waited) {
            this.sleep(tt * 1001l, link);
        }
        final Browser br2 = br.cloneBrowser();
        br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        getPage(br2, continueLink);
        {
            /* 2019-07-11: New for turbobit.net */
            continueLink = br2.getRegex("(\"|')(/?/download/started/[^\"\\']+)\\1").getMatch(1);
            if (!StringUtils.isEmpty(continueLink)) {
                br2.getPage(continueLink);
            }
        }
        downloadUrl = br2.getRegex("(\"|')(/?/download/redirect/[^\"\\']+)\\1").getMatch(1);
        handleDownloadRedirectErrors(downloadUrl, link);
        /** 2019-05-11: Not required for e.g. hitfile.net but it does not destroy anything either so let's set it anyways. */
        br.setCookie(br.getHost(), "turbobit2", getCurrentTimeCookie(br2));
        br.setFollowRedirects(false);
        // Future redirects at this point! We want to catch them and not process in order to get the MD5sum! example url structure
        // http://s\\d{2}.turbobit.ru:\\d+/download.php?name=FILENAME.FILEEXTENTION&md5=793379e72eef01ed1fa3fec91eff5394&fid=b5w4jikojflm&uid=free&speed=59&till=1356198536&trycount=1&ip=YOURIP&sid=60193f81464cca228e7bb240a0c39130&browser=201c88fd294e46f9424f724b0d1a11ff&did=800927001&sign=7c2e5d7b344b4a205c71c18c923f96ab
        getPage(downloadUrl);
        downloadUrl = br.getRedirectLocation() != null ? br.getRedirectLocation() : br.getURL();
        final String md5sum = new Regex(downloadUrl, "md5=([a-f0-9]{32})").getMatch(0);
        if (md5sum != null) {
            logger.info("Found md5hash on downloadstart");
            link.setMD5Hash(md5sum);
        }
        initDownload(DownloadType.GUEST_FREE, link, downloadUrl, true);
        handleServerErrors();
        dl.startDownload();
    }

    public int getPreDownloadWaittime(final Browser br) {
        int wait = 0;
        /* This is NOT a corrent implementation - they use js for the waittime but usually this will do just fine! */
        final String wait_str = br.getRegex("minLimit\\s*?:\\s*?(\\d+)").getMatch(0);
        if (wait_str == null) {
            wait = get_fallback_waittime();
        } else {
            wait = Integer.parseInt(wait_str);
            /* Check for too short/too long waittime. */
            if (wait > 800 || wait < minimum_pre_download_waittime_seconds()) {
                /* We do not want to wait too long! */
                wait = get_fallback_waittime();
            }
        }
        return wait;
    }

    /** Handles errors */
    private void handleDownloadRedirectErrors(final String dllink, final DownloadLink link) throws PluginException {
        final String host = link.getHost();
        if (StringUtils.isEmpty(dllink)) {
            logger.info("'redirect' downloadurl is null");
            if (br.toString().matches("Error: \\d+")) {
                // unknown error...
                throw new PluginException(LinkStatus.ERROR_RETRY);
            } else if (br.toString().matches("^The file is not avaliable now because of technical problems\\. <br> Try to download it once again after 10-15 minutes\\..*?")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "File not avaiable due to technical problems.", 15 * 60 * 1001l);
            } else if (br.containsHTML("<a href=\\'/" + this.getLinkID(link) + "\\.html\\'>new</a>")) {
                /* Expired downloadlink - rare issue. If user has added such a direct-URL, we're not able to retry. */
                /**
                 * 2019-05-14: TODO: Even premium-directurls should contain the linkid so we should be able to use that to 'convert' such
                 * problematic URLs to 'normal' URLs. Keep in mind that this is a VERY VERY rare case!
                 */
                /*
                 * <div class="action-block"><p>Der Link ist abgelaufen. Fordern Sie bitte <a href='/FUID.html'>new</a> download
                 * link.</p></div></div> </div> Example: http://turbobit.net/download/redirect/TEST/TEST
                 */
                if (link.getPluginPatternMatcher().matches(premRedirectLinks)) {
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Generated Premium link has expired");
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Failed to generate final downloadlink");
                }
            }
            final String linkerror = br.getRegex("<div\\s*id\\s*=\\s*\"brin-link-error\"\\s*>\\s*([^>]+)\\s*</div>").getMatch(0);
            if (linkerror != null) {
                /* 2019-07-10: E.g. <div id="brin-link-error">Failed to generate link. Internal server error. Please try again.</div> */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, linkerror);
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown Error - failed to find redirect-url to final downloadurl");
        } else if (StringUtils.endsWithCaseInsensitive(dllink, "://" + host + "/")) {
            // expired/invalid?
            // @see Link; 0418034739341.log; 1111047; jdlog://0418034739341
            throw new PluginException(LinkStatus.ERROR_FATAL, "Premium link no longer valid");
        }
    }

    private String getCurrentTimeCookie(Browser ibr) throws PluginException {
        if (ibr == null) {
            ibr = br;
        }
        String output = ibr.getRequest().getResponseHeader("Date");
        if (output == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        output = Encoding.urlEncode_light(output);
        output = output.replace(":", "%3A");
        return output;
    }

    /**
     * fuid = case sensitive.
     *
     * @param downloadLink
     * @return
     * @throws PluginException
     */
    @SuppressWarnings("deprecation")
    private String getFUID(DownloadLink downloadLink) throws PluginException {
        /* standard links turbobit.net/uid.html && turbobit.net/uid/filename.html */
        String fuid = new Regex(downloadLink.getDownloadURL(), "https?://[^/]+/([A-Za-z0-9]+)(?:/[^/]+)?(?:\\.html)?$").getMatch(0);
        if (fuid == null) {
            // download/free/
            fuid = new Regex(downloadLink.getDownloadURL(), "download/free/([A-Za-z0-9]+)").getMatch(0);
            if (fuid == null) {
                // support for public premium links
                fuid = new Regex(downloadLink.getDownloadURL(), "download/redirect/[A-Za-z0-9]+/([a-zA-F0-9]+)").getMatch(0);
                if (fuid == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        return fuid;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        if (account.getType() == AccountType.FREE) {
            this.handleFree(link);
        } else {
            /* support for public premium links */
            if (link.getDownloadURL().matches(premRedirectLinks)) {
                handlePremiumLink(link);
                return;
            }
            requestFileInformation(link);
            login(account, false);
            sleep(2000, link);
            getPage(link.getDownloadURL());
            String dllink = null;
            final String[] mirrors = br.getRegex("('|\")(https?://([a-z0-9\\.]+)?[^/\\'\"]+//?download/redirect/.*?)\\1").getColumn(1);
            if (mirrors == null || mirrors.length == 0) {
                if (br.containsHTML("You have reached the.*? limit of premium downloads")) {
                    logger.info("You have reached the.*? limit of premium downloads");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                }
                if (br.containsHTML("'>Premium access is blocked<")) {
                    logger.info("No traffic available");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                }
                this.handleGeneralErrors();
                logger.warning("dllink equals null, plugin seems to be broken!");
                if (isLoggedIN()) {
                    synchronized (account) {
                        account.setProperty("UA", null);
                        account.clearCookies("");
                    }
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                logger.info("unknown_dl_error_premium");
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Failed to find final downloadlink");
            }
            br.setFollowRedirects(false);
            {
                Browser br2 = br.cloneBrowser();
                for (int i = 0; i < mirrors.length; i++) {
                    final String currentlink = mirrors[i];
                    logger.info("Checking mirror: " + currentlink);
                    getPage(currentlink);
                    if (br.getHttpConnection().getResponseCode() == 503) {
                        logger.info("Too many connections on current account via current IP");
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                    }
                    if (br.getRedirectLocation() != null) {
                        dllink = br.getRedirectLocation();
                        if (dllink != null) {
                            final boolean isdownloadable = initDownload(DownloadType.ACCOUNT_PREMIUM, link, dllink, mirrors.length - 1 == i);
                            if (isdownloadable) {
                                break;
                            }
                            br = br2.cloneBrowser();
                        }
                    }
                }
            }
            dl.startDownload();
        }
    }

    /**
     * 2019-05-11: Their final-downloadlinks usually contain the md5 checksum of the file and this is the only place we can get it from.
     * This function tries to find this md5 value and sets it if possible.
     */
    protected boolean getAndSetMd5Hash(final DownloadLink link, final String dllink) {
        final String md5sum = new Regex(dllink, "md5=([a-f0-9]{32})").getMatch(0);
        if (md5sum != null) {
            link.setMD5Hash(md5sum);
            return true;
        } else {
            return false;
        }
    }

    static enum DownloadType {
        ACCOUNT_PREMIUM,
        ACCOUNT_FREE,
        GUEST_FREE,
        GUEST_PREMIUMLINK;
    }

    /** 2019-05-11: Limits seem to be the same for all of their services. */
    private boolean initDownload(final DownloadType downloadType, final DownloadLink link, final String directlink, final boolean isLast) throws Exception {
        if (directlink == null) {
            logger.warning("dllink is null");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        getAndSetMd5Hash(link, directlink);
        boolean result = false;
        try {
            switch (downloadType) {
            case ACCOUNT_PREMIUM:
            case GUEST_PREMIUMLINK:
                dl = new jd.plugins.BrowserAdapter().openDownload(br, this.getDownloadLink(), directlink, true, 0);
                break;
            default:
                dl = new jd.plugins.BrowserAdapter().openDownload(br, this.getDownloadLink(), directlink, true, 1);
                break;
            }
            // we require error handling here
            if (dl.getConnection().getResponseCode() == 403 || dl.getConnection().getURL().getPath().startsWith("/error/download/ip")) {
                try {
                    dl.getConnection().setAllowedResponseCodes(new int[] { dl.getConnection().getResponseCode() });
                    br.followConnection();
                } catch (IOException e) {
                    logger.log(e);
                }
                // 403 by itself
                // response code 403 && <p>You have reached the limit of downloads from this IP address, please contact our
                if (downloadType == DownloadType.ACCOUNT_PREMIUM) {
                    logger.info("No traffic available");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                }
                // some reason we have different error handling for free.
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "You cannot download this file with your current IP", 60 * 60 * 1000l);
            }
            if (dl.getConnection().getResponseCode() == 404) {
                try {
                    dl.getConnection().setAllowedResponseCodes(new int[] { dl.getConnection().getResponseCode() });
                    br.followConnection();
                } catch (IOException e) {
                    logger.log(e);
                }
                logger.info("File is offline on download-attempt");
                if (dl.getConnection().getURL().getPath().startsWith("landpage")) {
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Seems to be blocked by ISP", 60 * 60 * 1000l);
                }
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (dl.getConnection().getContentType().contains("html") || dl.getConnection().getLongContentLength() == -1) {
                try {
                    dl.getConnection().setAllowedResponseCodes(new int[] { dl.getConnection().getResponseCode() });
                    br.followConnection();
                } catch (IOException e) {
                    logger.log(e);
                }
                handleGeneralErrors();
                if (isLast) {
                    // existing error handling is broken! there is no more mirrors!
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                return result;
            }
            result = true;
            return result;
        } catch (final Exception e) {
            // on last mirror we should throw proper exception.
            if (isLast) {
                throw e;
            }
            logger.log(e);
            return result;
        } finally {
            try {
                if (!result) {
                    dl.getConnection().disconnect();
                }
            } catch (final Throwable t) {
            }
            logger.info("Mirror is " + (result ? "okay: " : "down: ") + directlink);
        }
    }

    public void handlePremiumLink(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        br.setCookie(getMainpage(), "JD", "1");
        String dllink = link.getDownloadURL();
        br.setFollowRedirects(false);
        getPage(dllink);
        dllink = br.getRedirectLocation();
        handleDownloadRedirectErrors(dllink, link);
        initDownload(DownloadType.GUEST_PREMIUMLINK, link, dllink, true);
        handleServerErrors();
        dl.startDownload();
    }

    private void handleGeneralErrors() throws PluginException {
        if (br.containsHTML("Try to download it once again after")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'Try again later'", 20 * 60 * 1000l);
        }
        /* Either user waited too long for the captcha or maybe slow servers */
        if (br.containsHTML(">Ссылка просрочена\\. Пожалуйста получите")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'link expired'", 5 * 60 * 1000l);
        }
        if (br.containsHTML("Our service is currently unavailable in your country\\.")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Turbobit.net is currently unavailable in your country.");
        }
    }

    private void handleServerErrors() throws PluginException {
        if (dl.getConnection().getLongContentLength() == 0) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error, server sends empty file", 5 * 60 * 1000l);
        }
    }

    @Override
    public boolean hasAutoCaptcha() {
        return false;
    }

    private static AtomicReference<String> userAgent = new AtomicReference<String>(null);

    private Browser prepBrowserGeneral(final Browser prepBr, String UA) {
        if (UA == null) {
            userAgent.set(UserAgents.stringUserAgent());
            UA = userAgent.get();
        }
        prepBr.getHeaders().put("Pragma", null);
        prepBr.getHeaders().put("Cache-Control", null);
        prepBr.getHeaders().put("Accept-Charset", null);
        prepBr.getHeaders().put("Accept", "text/html, application/xhtml+xml, */*");
        prepBr.getHeaders().put("Accept-Language", "en-EN");
        prepBr.getHeaders().put("User-Agent", UA);
        prepBr.getHeaders().put("Referer", null);
        prepBr.setCustomCharset("UTF-8");
        prepBr.setCookie(getMainpage(), "JD", "1");
        prepBr.setCookie(getMainpage(), "set_user_lang_change", "en");
        return prepBr;
    }

    /** Only call this if a valid APIKey is available!! */
    private Browser prepBrowserAPI(final Browser prepBr, String UA) {
        prepBrowserGeneral(prepBr, UA);
        prepBr.getHeaders().put("X-API-KEY", this.getAPIKey());
        return prepBr;
    }

    private Browser prepBrowserWebsite(final Browser prepBr, String UA) {
        prepBrowserGeneral(prepBr, UA);
        return prepBr;
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                setBrowserExclusive();
                /*
                 * we have to reuse old UA, else the cookie will become invalid
                 */
                String ua = account.getStringProperty("UA", null);
                prepBrowserWebsite(br, ua);
                br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    logger.info("Attempting cookie login");
                    br.setCookies(this.getMainpage(), cookies);
                    getPage(this.getMainpage());
                    if (isLoggedIN()) {
                        logger.info("Cookie login successful");
                        /* Set new cookie timestamp */
                        br.setCookies(getMainpage(), cookies);
                        return;
                    }
                    logger.info("cookie login failed: Full login is required");
                    if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                        logger.warning("Cookie login failed MAKE SURE THAT YOU RE-USED THE SAME USER-AGENT AS USED FOR THE FIRST LOGIN ELSE COOKIE LOGIN WILL NOT WORK!!!");
                    }
                }
                /* lets set a new User-Agent */
                prepBrowserWebsite(br, null);
                getPage(getMainpage() + "login");
                Form loginform = findAndPrepareLoginForm(br, account);
                submitForm(loginform);
                if (findLoginForm(br, account) != null) {
                    /* Check for stupid login captcha */
                    final DownloadLink dummyLink = new DownloadLink(this, "Account", account.getHoster(), getMainpage(), true);
                    loginform = findAndPrepareLoginForm(br, account);
                    if (br.containsHTML("class=\"g-recaptcha\"")) {
                        this.setDownloadLink(dummyLink);
                        final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                        loginform.put("g-recaptcha-response", recaptchaV2Response);
                    } else if (loginform.containsHTML("class=\"reloadCaptcha\"")) {
                        /* Old captcha - e.g. wayupload.com */
                        final String captchaurl = br.getRegex("(https?://[^/]+/captcha/securimg[^\"<>]+)").getMatch(0);
                        if (captchaurl == null) {
                            logger.warning("Failed to find captchaURL");
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        final String code = this.getCaptchaCode(captchaurl, dummyLink);
                        loginform.put("user%5Bcaptcha_response%5D", Encoding.urlEncode(code));
                        loginform.put("user%5Bcaptcha_type%5D", "securimg");
                        loginform.put("user%5Bcaptcha_subtype%5D", "9");
                    }
                    submitForm(loginform);
                }
                universalLoginErrorhandling(br);
                if (!isLoggedIN()) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.\r\n3. Gehe auf folgende Seite und deaktiviere, den Login Captcha Schutz deines Accounts und versuche es erneut: turbobit.net/user/settings", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.\r\n3. Access the following site and disable the login captcha protection of your account and try again: turbobit.net/user/settings", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(br.getCookies(getMainpage()), "");
                account.setProperty("UA", userAgent.get());
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.setProperty("UA", Property.NULL);
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    protected boolean isLoggedIN() {
        return ("1".equals(br.getHostCookie("user_isloggedin", Cookies.NOTDELETEDPATTERN)));
    }

    private Form findAndPrepareLoginForm(Browser br, final Account account) throws PluginException {
        if (account == null) {
            return null;
        } else {
            final Form loginForm = findLoginForm(br, account);
            if (loginForm != null) {
                loginForm.put("user%5Blogin%5D", Encoding.urlEncode(account.getUser()));
                loginForm.put("user%5Bpass%5D", Encoding.urlEncode(account.getPass()));
                return loginForm;
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
    }

    private Form findLoginForm(Browser br, final Account account) {
        return br.getFormbyAction("/user/login");
    }

    public static void universalLoginErrorhandling(final Browser br) throws PluginException {
        if (br.containsHTML(">Limit of login attempts exceeded for your account")) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nMaximale Anzahl von Loginversuchen überschritten - dein Account wurde temporär gesperrt!\r\nBestätige deinen Account per E-Mail um ihn zu entsperren.\r\nFalls du keine E-Mail bekommen hast, gib deine E-Mail Adresse auf folgender Seite ein und lasse dir erneut eine zuschicken: " + br.getHost() + "/restoreaccess", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nLimit of login attempts exceeded for your account - your account is locked!\r\nConfirm your account via e-mail to unlock it.\r\nIf you haven't received an e-mail, enter your e-mail address on the following site so the service can send you a new confirmation mail: " + br.getHost() + "/restoreaccess", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
    }

    private String parseImageUrl(String fun, final boolean NULL) {
        if (fun == null) {
            return null;
        }
        if (!NULL) {
            final String[] next = fun.split(tb(9));
            if (next == null || next.length != 2) {
                fun = rhino(fun, 0);
                if (fun == null) {
                    return null;
                }
                fun = new Regex(fun, tb(4)).getMatch(0);
                return fun == null ? new Regex(fun, tb(5)).getMatch(0) : rhino(fun, 2);
            }
            return rhino(next[1], 1);
        }
        return new Regex(fun, tb(1)).getMatch(0);
    }

    public String getMainpage() {
        if (supports_https()) {
            return "https://" + this.getHost() + "/";
        } else {
            return "http://" + this.getHost() + "/";
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    private String rhino(final String s, final int b) {
        Object result = new Object();
        final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(this);
        final ScriptEngine engine = manager.getEngineByName("javascript");
        try {
            switch (b) {
            case 0:
                engine.eval(s + tb(6));
                result = engine.get(tb(7));
                break;
            case 1:
                result = ((Double) engine.eval(tb(8))).longValue();
                break;
            case 2:
                engine.eval("var out=\"" + s + "\";");
                result = engine.get("out");
                break;
            case 100:
                String[] code = s.split("@");
                engine.eval(code[0] + "var b = 3;var inn = \'" + code[1] + "\';" + code[2]);
                result = engine.get("out");
                break;
            case 666:
                code = s.split("@");
                engine.eval(code[0] + "var b = 1;var inn = \'" + code[1] + "\';" + code[2]);
                result = engine.get("out");
                break;
            case 999:
                code = s.split("@");
                engine.eval("var b = 2;var inn = \'" + code[0] + "\';" + code[1]);
                result = engine.get("out");
                break;
            }
        } catch (final Throwable e) {
            return null;
        }
        return result != null ? result.toString() : null;
    }

    private void setConfigElements() {
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SETTING_JAC, "Activate parallel
        // downloadstarts in free mode?\r\n<html><p style=\"color:#F62817\"><b>Warning: This setting can lead to a lot of non-accepted
        // captcha popups!</b></p></html>").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SETTING_FREE_PARALLEL_DOWNLOADSTARTS, "Activate parallel downloadstarts in free mode?\r\n<html><p style=\"color:#F62817\"><b>Warning: This setting can lead to a lot of non-accepted captcha popups!</b></p></html>").setDefaultValue(false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), "APIKEY", "Define custom APIKey (can be
        // found on website in account settings ['/user/settings'])").setDefaultValue(""));
    }

    private String tb(final int i) {
        final String[] s = new String[12];
        s[0] = "fe8cfbfafa57cde31bc2b798df5141ab2dc171ec0852d89a1a135e3f116c83d15d8bf93a";
        s[1] = "fddbfbfafa57cdef1a90b5cedf5647ae2cc572ec0958dd981e125c68156882d65d82f869";
        s[2] = "fdd9fbf2fb05cde71a97b69edf5742f1289470bb0a5bd9c81a1b5e39116c85805982fc6e880ce26a201651b8ea211874e4232d90c59b6462ac28d2b26f0537385fa6";
        s[3] = "f980f8f7fa0acdb21b91b6cbdf5043fc2ac775ea080fd8c71a4f5d68156586d05982fd3e8b5ae33f244555e8eb201d77e12128cbc1c7";
        s[4] = "f980ffa5fa07cdb01a93b6c8de0642ae299571bb0c0ddb9c1a1b5b6f143d84855ddfff6b8b5de66e254553eeea751d72e17e2d98c19a6760af75d6b46b05";
        s[5] = "f980ffa5f951ceb31ec7b3c8da5246fa2ac770bc0b0fdc9c1e13";
        s[6] = "fc8efbf2fb01c9e61bc2b798df5146f82cc075bf0b5fd8c71a4e5f3e153a8781588ff86f890de26a221050eaee701824e4742d9cc1c66238a973";
        s[7] = "fddefaf6fb07";
        s[8] = "fe8cfbfafa57cde31bc2b798df5146ad29c071b6080edbca1a135f6f156984d75982fc6e8800e338";
        s[9] = "ff88";
        s[10] = "f9def8a1fa02c9b21ac5b5c9da0746ae2ac671be0c0fd99f194e5b69113a85d65c8bf86e8d00e23d254751eded741d72e7262ecdc19c6267af72d2e26b5e326a59a5ce295d28f89e21ae29ea523acfb545fd8adb";
        s[11] = "f980fea5fa0ac9ef1bc7b694de0142f1289075bd0d0ddb9d1b195a6d103d82865cddff69890ae76a251b53efef711d74e07e299bc098";
        /*
         * we have to load the plugin first! we must not reference a plugin class without loading it before
         */
        JDUtilities.getPluginForDecrypt("linkcrypt.ws");
        return JDHexUtils.toString(IMAGEREGEX(s[i]));
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

    private LinkedHashSet<String> dupe = new LinkedHashSet<String>();

    private void simulateBrowser() throws InterruptedException {
        // dupe.clear();
        final AtomicInteger requestQ = new AtomicInteger(0);
        final AtomicInteger requestS = new AtomicInteger(0);
        final ArrayList<String> links = new ArrayList<String>();
        String[] l1 = new Regex(br, "\\s+(?:src)=(\"|')(.*?)\\1").getColumn(1);
        if (l1 != null) {
            links.addAll(Arrays.asList(l1));
        }
        l1 = new Regex(br, "\\s+(?:src)=(?!\"|')([^\\s]+)").getColumn(0);
        if (l1 != null) {
            links.addAll(Arrays.asList(l1));
        }
        for (final String link : links) {
            // lets only add links related to this hoster.
            final String correctedLink = Request.getLocation(link, br.getRequest());
            if (this.getHost().equals(Browser.getHost(correctedLink)) && !correctedLink.endsWith(this.getHost() + "/") && !correctedLink.contains(".html") && !correctedLink.equals(br.getURL()) && !correctedLink.contains("/captcha/") && !correctedLink.contains("'")) {
                if (dupe.add(correctedLink)) {
                    final Thread simulate = new Thread("SimulateBrowser") {
                        public void run() {
                            final Browser rb = br.cloneBrowser();
                            rb.getHeaders().put("Cache-Control", null);
                            // open get connection for images, need to confirm
                            if (correctedLink.matches(".+\\.png.*")) {
                                rb.getHeaders().put("Accept", "image/webp,*/*;q=0.8");
                            } else if (correctedLink.matches(".+\\.js.*")) {
                                rb.getHeaders().put("Accept", "*/*");
                            } else if (correctedLink.matches(".+\\.css.*")) {
                                rb.getHeaders().put("Accept", "text/css,*/*;q=0.1");
                            }
                            URLConnectionAdapter con = null;
                            try {
                                requestQ.getAndIncrement();
                                con = rb.openGetConnection(correctedLink);
                            } catch (final Exception e) {
                            } finally {
                                try {
                                    con.disconnect();
                                } catch (final Exception e) {
                                }
                                requestS.getAndIncrement();
                            }
                            return;
                        }
                    };
                    simulate.start();
                    Thread.sleep(100);
                }
            }
        }
        while (requestQ.get() != requestS.get()) {
            Thread.sleep(1000);
        }
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.Turbobit_Turbobit;
    }
}
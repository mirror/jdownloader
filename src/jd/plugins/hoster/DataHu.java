//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.simplejson.JSonUtils;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "data.hu" }, urls = { "https?://(?:www\\.)?data.hu/get/(\\d+)/([^<>\"/%]+)" })
public class DataHu extends antiDDoSForHost {
    private int statuscode = 0;

    public DataHu(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://data.hu/premium.php");
    }

    @Override
    public String getAGBLink() {
        return "https://data.hu/adatvedelem.php";
    }

    public void correctDownloadLink(DownloadLink link) {
        /* Prefer https */
        link.setPluginPatternMatcher(link.getPluginPatternMatcher().replace("http://", "https://").replace(".html", ""));
    }

    private static final String API_BASE             = "https://data.hu/api.php";
    private static final String PROPERTY_PREMIUMONLY = "premiumonly";

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 3;
    }

    /*
     * Max 4 connections per download server, if we try more this will end up in 503 responses. At the moment we allow 3 simultan DLs * 2
     * Chunks each.
     */
    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 3;
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 500;
    }

    private Browser prepBR(final Browser br) {
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFID(link);
        if (linkid != null) {
            return this.getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    private boolean isPremiumOnly(final DownloadLink link) {
        if (link.hasProperty(PROPERTY_PREMIUMONLY)) {
            return true;
        } else {
            return false;
        }
    }

    /** Using API: http://data.hu/api.php */
    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) {
            return false;
        }
        try {
            prepBR(this.br);
            br.setCookiesExclusive(true);
            final StringBuilder sb = new StringBuilder();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    /* 2020-05-28: Limit == 50 but their API won't allow 50 (wtf) so we only check 30 at the same time. */
                    if (index == urls.length || links.size() == 30) {
                        break;
                    } else {
                        links.add(urls[index]);
                        index++;
                    }
                }
                sb.delete(0, sb.capacity());
                for (final DownloadLink link : links) {
                    sb.append(link.getPluginPatternMatcher());
                    sb.append("%2C");
                }
                this.getAPISafe(API_BASE + "?act=check_download_links&links=" + sb.toString());
                final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                final Map<String, Object> link_info = (Map<String, Object>) entries.get("link_info");
                for (final DownloadLink link : links) {
                    final Map<String, Object> info = (Map<String, Object>) link_info.get(link.getPluginPatternMatcher());
                    if (info == null) {
                        /* This should never happen! */
                        link.setAvailable(false);
                        continue;
                    }
                    final String status = info.get("status").toString();
                    final String filename = (String) info.get("filename");
                    final String filesize = (String) info.get("filesize");
                    final String md5 = (String) info.get("md5");
                    final String sha1 = (String) info.get("sha1");
                    final String infoText = (String) info.get("info");
                    if (!StringUtils.isEmpty(filename)) {
                        link.setFinalFileName(filename);
                        /* Correct urls so when users copy them they can actually use them. */
                        if (!link.getPluginPatternMatcher().contains(filename)) {
                            link.setContentUrl("https://" + this.getHost() + "/get/" + this.getFID(link) + "/" + Encoding.urlEncode(filename));
                        }
                    }
                    if (!StringUtils.isEmpty(filesize)) {
                        link.setDownloadSize(SizeFormatter.getSize(filesize));
                    }
                    if (!StringUtils.isEmpty(sha1)) {
                        link.setSha1Hash(sha1);
                    }
                    if (!StringUtils.isEmpty(md5)) {
                        link.setMD5Hash(md5);
                    }
                    if (StringUtils.containsIgnoreCase(infoText, "only premium")) {
                        link.setProperty(PROPERTY_PREMIUMONLY, true);
                    } else {
                        link.removeProperty(PROPERTY_PREMIUMONLY);
                    }
                    if (status.equalsIgnoreCase("online")) {
                        link.setAvailable(true);
                    } else {
                        link.setAvailable(false);
                    }
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            logger.log(e);
            return false;
        }
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        checkLinks(new DownloadLink[] { link });
        if (!link.isAvailabilityStatusChecked()) {
            return AvailableStatus.UNCHECKED;
        }
        if (link.isAvailabilityStatusChecked() && !link.isAvailable()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        this.setBrowserExclusive();
        login(account);
        final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final String type = entries.get("type").toString();
        if (!"premium".equalsIgnoreCase(type)) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nNicht unterstützter Accounttyp (" + type + ")!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUnsupported account type (" + type + ")!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        final String expiredate = (String) entries.get("expiration_date");
        if (expiredate != null) {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expiredate, "yyyy-MM-dd mm:HH:ss", Locale.ENGLISH), br);
        }
        account.setType(AccountType.PREMIUM);
        return ai;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        final String directlinkproperty = "directlink";
        final boolean resume = true;
        final int maxchunks = 1;
        if (!this.attemptStoredDownloadurlDownload(link, directlinkproperty, resume, maxchunks)) {
            requestFileInformation(link);
            if (isPremiumOnly(link)) {
                throw new AccountRequiredException();
            }
            getPage(link.getPluginPatternMatcher());
            handleErrorsWebsite(br);
            if (br.containsHTML("class='slow_dl_error_text'")) {
                link.setProperty(PROPERTY_PREMIUMONLY, true);
                throw new AccountRequiredException();
            }
            link.removeProperty(PROPERTY_PREMIUMONLY);
            final Form captcha = br.getFormbyProperty("id", "captcha_form");
            final String dllink;
            if (captcha != null) {
                /* Captcha required */
                logger.info("Detected captcha method \"reCaptchaV2\" for this host");
                final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                captcha.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                final Browser ajax = br.cloneBrowser();
                ajax.getHeaders().put("Accept", "*/*");
                ajax.getHeaders().put("X-Requested-With", " XMLHttpRequest");
                submitForm(ajax, captcha);
                final Map<String, Object> entries = JSonStorage.restoreFromString(ajax.getRequest().getHtmlCode(), TypeRef.HASHMAP);
                dllink = (String) entries.get("redirect");
            } else {
                /* No captcha required */
                dllink = br.getRegex("(\"|')(https?://ddl\\d+\\." + org.appwork.utils.Regex.escape(this.getHost()) + "/get/\\d+/\\d+/.*?)\\1").getMatch(1);
            }
            if (StringUtils.isEmpty(dllink)) {
                final String message = PluginJSonUtils.getJsonValue(this.br, "message");
                if (!StringUtils.isEmpty(message)) {
                    /* 2017-02-02: They have reCaptchaV2 so this should never happen ... */
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, true, 1);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                logger.warning("The finallink doesn't seem to be a file...");
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
                handleServerErrors();
                handleErrorsWebsite(br);
                if (br.getURL().contains("/only_premium.php")) {
                    link.setProperty(PROPERTY_PREMIUMONLY, true);
                    throw new AccountRequiredException();
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setProperty(directlinkproperty, dllink);
        }
        dl.startDownload();
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        final String directlinkproperty = "dllink_premium";
        final boolean resume = true;
        final int maxchunks = -2;
        if (!attemptStoredDownloadurlDownload(link, directlinkproperty, resume, maxchunks)) {
            requestFileInformation(link);
            getAPISafe(API_BASE + "?act=get_direct_link&link=" + PluginJSonUtils.escape(link.getPluginPatternMatcher()) + "&username=" + JSonUtils.escape(account.getUser()) + "&password=" + JSonUtils.escape(account.getPass()));
            final String dllink = PluginJSonUtils.getJsonValue(br, "direct_link");
            if (StringUtils.isEmpty(dllink)) {
                /* Should never happen */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, true, -2);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                logger.warning("The finallink doesn't seem to be a file...");
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
                handleServerErrors();
                handleErrorsWebsite(br);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setProperty(directlinkproperty, dllink);
        }
        dl.startDownload();
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link, final String directlinkproperty, final boolean resumable, final int maxchunks) throws Exception {
        final String url = link.getStringProperty(directlinkproperty);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        boolean valid = false;
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, resumable, maxchunks);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                valid = true;
                return true;
            } else {
                link.removeProperty(directlinkproperty);
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final Throwable e) {
            logger.log(e);
            return false;
        } finally {
            if (!valid) {
                try {
                    dl.getConnection().disconnect();
                } catch (Throwable ignore) {
                }
                this.dl = null;
            }
        }
    }

    private void handleServerErrors() throws PluginException {
        /*
         * Wait a minute for response 503 because JD tried to start too many downloads in a short time
         */
        if (dl.getConnection().getResponseCode() == 503) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many simultan downloads, please wait some time!", 60 * 1000l);
        }
    }

    private void handleErrorsWebsite(final Browser br) throws PluginException {
        if (br.containsHTML("(?i)Az adott fájl nem létezik\\.")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)Az adott fájl már nem elérhető\\.")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    public void login(final Account account) throws Exception {
        this.setBrowserExclusive();
        prepBR(this.br);
        getAPISafe(API_BASE + "?act=check_login_data&username=" + JSonUtils.escape(account.getUser()) + "&password=" + JSonUtils.escape(account.getPass()));
    }

    private void getAPISafe(final String accesslink) throws Exception {
        getPage(accesslink);
        updatestatuscode();
        handleAPIErrors(this.br);
    }

    private void postAPISafe(final String accesslink, final String postdata) throws Exception {
        postPage(accesslink, postdata);
        updatestatuscode();
        handleAPIErrors(this.br);
    }

    /**
     * 0 = everything ok, 1-99 = "error"-errors, 100-199 = "not_available"-errors, 200-299 = Other (html) [download] errors, sometimes mixed
     * with the API errors.
     */
    private void updatestatuscode() {
        String error = PluginJSonUtils.getJsonValue(br, "error");
        final String msg = PluginJSonUtils.getJsonValue(br, "msg");
        if (error != null && msg != null) {
            if (msg.equals("wrong username or password")) {
                statuscode = 1;
            } else if (msg.equals("no premium")) {
                statuscode = 2;
            } else if (msg.equals("wrong link")) {
                statuscode = 3;
            } else if (msg.equals("max 50 link can check")) {
                statuscode = 4;
            } else if (msg.equals("no act param defined")) {
                statuscode = 5;
            } else {
                statuscode = 666;
            }
        } else {
            /* No way to tell that something unpredictable happened here --> status should be fine. */
            statuscode = 0;
        }
    }

    private void handleAPIErrors(final Browser br) throws PluginException {
        String statusMessage = null;
        try {
            switch (statuscode) {
            case 0:
                /* Everything ok */
                break;
            case 1:
                /* Wrong username or password -> permanently disable account */
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    statusMessage = "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.";
                } else {
                    statusMessage = "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.";
                }
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 2:
                /* Account = Free account tried a download in premium mode --> (Should never happen anyways) Permanently disable */
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    statusMessage = "\r\nNicht unterstützter Accounttyp!\r\nFalls du denkst diese Meldung sei falsch die Unterstützung dieses Account-Typs sich\r\ndeiner Meinung nach aus irgendeinem Grund lohnt,\r\nkontaktiere uns über das support Forum.";
                } else {
                    statusMessage = "\r\nUnsupported account type!\r\nIf you think this message is incorrect or it makes sense to add support for this account type\r\ncontact us via our support forum.";
                }
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 3:
                /* User tried to download an invalid link via account --> Should never happen */
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    statusMessage = "\r\nUngültiger Downloadlink";
                } else {
                    statusMessage = "\r\nInvalid downloadlink";
                }
                throw new PluginException(LinkStatus.ERROR_FATAL, statusMessage);
            case 4:
                /* We tried to check more than 50 links at the same time --> Should never happen */
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    statusMessage = "\r\nKann maximal 50 Links gleichzeitig überprüfen";
                } else {
                    statusMessage = "\r\nMax simultaneous checkable links number equals 50";
                }
                throw new PluginException(LinkStatus.ERROR_FATAL, statusMessage);
            case 5:
                /* 'no act param defined' --> Should never happen */
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    statusMessage = "\r\nFATALER API Fehler";
                } else {
                    statusMessage = "\r\nFATAL API error";
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, statusMessage);
            case 666:
                /* SHTF --> Should never happen */
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    statusMessage = "\r\nUnbekannter Fehler";
                } else {
                    statusMessage = "\r\nUnknown error";
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, statusMessage);
            default:
                /* Completely Unknown error */
                statusMessage = "Unknown error";
                logger.info("Unknown API error");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } catch (final PluginException e) {
            logger.info("Exception: statusCode: " + statuscode + " statusMessage: " + statusMessage);
            throw e;
        }
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (this.isPremiumOnly(link) && (account == null || account.getType() != AccountType.PREMIUM)) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}
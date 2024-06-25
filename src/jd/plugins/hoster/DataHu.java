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
import java.util.regex.Pattern;

import org.appwork.storage.JSonMapperException;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Exceptions;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.parser.UrlQuery;
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
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
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
        this.enablePremium("https://" + getHost() + "/premium.php");
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public String getAGBLink() {
        return "https://" + getHost() + "/adatvedelem.php";
    }

    private String getContentURL(final DownloadLink link) {
        return link.getPluginPatternMatcher().replaceFirst("(?i)http://", "https://").replace(".html", "");
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
                    sb.append(this.getContentURL(link));
                    sb.append("%2C");
                }
                final Map<String, Object> entries = (Map<String, Object>) this.getAPISafe(API_BASE + "?act=check_download_links&links=" + sb.toString(), links.get(0), null);
                final Map<String, Object> link_info = (Map<String, Object>) entries.get("link_info");
                for (final DownloadLink link : links) {
                    final String contenturl = this.getContentURL(link);
                    final Map<String, Object> info = (Map<String, Object>) link_info.get(contenturl);
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
                        if (!contenturl.contains(filename)) {
                            link.setContentUrl("https://" + getHost() + "/get/" + getFID(link) + "/" + URLEncode.encodeURIComponent(filename));
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
        this.setBrowserExclusive();
        final Map<String, Object> entries = (Map<String, Object>) login(account);
        final AccountInfo ai = new AccountInfo();
        final String type = entries.get("type").toString();
        if (!"premium".equalsIgnoreCase(type)) {
            throw new AccountInvalidException("\r\nUnsupported account type (" + type + ")!");
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
            getPage(this.getContentURL(link));
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
                final Map<String, Object> entries = restoreFromString(ajax.getRequest().getHtmlCode(), TypeRef.MAP);
                dllink = (String) entries.get("redirect");
            } else {
                /* No captcha required */
                dllink = br.getRegex("(\"|')(https?://ddl\\d+\\." + Pattern.quote(this.getHost()) + "/get/\\d+/\\d+/.*?)\\1").getMatch(1);
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
                br.followConnection(true);
                handleServerErrors();
                handleErrorsWebsite(br);
                if (br.getURL().contains("/only_premium.php")) {
                    link.setProperty(PROPERTY_PREMIUMONLY, true);
                    throw new AccountRequiredException();
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
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
            final UrlQuery query = new UrlQuery();
            query.append("act", "get_direct_link", true);
            query.append("link", this.getContentURL(link), true);
            query.append("username", account.getUser(), true);
            query.append("password", account.getPass(), true);
            final Map<String, Object> resp = (Map<String, Object>) getAPISafe(API_BASE + "?" + query.toString(), link, account);
            final String dllink = resp.get("direct_link").toString();
            if (StringUtils.isEmpty(dllink)) {
                /* Should never happen */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, true, -2);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                logger.warning("The finallink doesn't seem to be a file...");
                br.followConnection(true);
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

    public Object login(final Account account) throws Exception {
        this.setBrowserExclusive();
        final UrlQuery query = new UrlQuery();
        query.append("act", "check_login_data", true);
        query.append("username", account.getUser(), true);
        query.append("password", account.getPass(), true);
        return getAPISafe(API_BASE + "?" + query.toString(), null, account);
    }

    private Object getAPISafe(final String accesslink, final DownloadLink link, final Account account) throws Exception {
        getPage(accesslink);
        return this.checkErrors(account, link);
    }

    private Object checkErrors(final Account account, final DownloadLink link) throws PluginException, InterruptedException {
        try {
            final Object jsonO = restoreFromString(br.toString(), TypeRef.OBJECT);
            if (jsonO == null || !(jsonO instanceof Map)) {
                return jsonO;
            }
            final Map<String, Object> map = (Map<String, Object>) jsonO;
            final Object error = map.get("error");
            if (error == null || error.toString().equals("0")) {
                /* No error */
                return map;
            }
            final String msg = map.get("msg").toString();
            if (msg.equals("wrong username or password")) {
                throw new AccountInvalidException(msg);
            } else if (msg.equals("no premium")) {
                throw new AccountInvalidException("Invalid account type (no premium)");
            } else if (msg.equals("wrong link")) {
                throw new PluginException(LinkStatus.ERROR_FATAL, msg);
            } else {
                if (link != null) {
                    throw new AccountInvalidException(msg);
                } else {
                    /* Unknown login problem */
                    throw new AccountUnavailableException(msg, 5 * 60 * 1000l);
                }
            }
        } catch (final JSonMapperException jme) {
            final String errortext = "Bad API response";
            if (link != null) {
                throw new AccountInvalidException(errortext);
            } else {
                throw Exceptions.addSuppressed(new AccountUnavailableException(errortext, 1 * 60 * 1000l), jme);
            }
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
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.config.FreeDiscPlConfig;
import org.jdownloader.plugins.components.config.FreeDiscPlConfig.StreamDownloadMode;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
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
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class FreeDiscPl extends PluginForHost {
    public FreeDiscPl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://freedisc.pl/");
        this.setStartIntervall(1000);
        try {
            Browser.setBurstRequestIntervalLimitGlobal("freedisc.pl", 250, 20, 60000);
        } catch (final Throwable e) {
        }
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "freedisc.pl" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            String regex = "https?://(?:www\\.)?" + buildHostsPatternPart(domains);
            regex += "/(?:";
            regex += "(#(!|%21))?[A-Za-z0-9\\-_]+,f-\\d+(,[\\w\\-]+)?";
            regex += "|embed/(?:audio|video)/\\d+";
            regex += ")";
            ret.add(regex);
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "https://freedisc.pl/regulations";
    }

    public void correctDownloadLink(final DownloadLink link) {
        final String linkpart = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
        if (linkpart != null) {
            link.setPluginPatternMatcher("https://" + this.getHost() + "/" + linkpart);
        }
    }

    protected static Cookies    botSafeCookies                     = new Cookies();
    private static final String TYPE_FILE                          = "https?://[^/]+/(?:#(?:!|%21))?([A-Za-z0-9\\-_]+,f-(\\d+)(,([\\w\\-]+))?)";
    private static final String TYPE_EMBED                         = "https?://[^/]+/embed/(audio|video)/(\\d+)";
    private static final String PROPERTY_AUDIO_STREAM_IS_AVAILABLE = "stream_is_available_audio";
    private static final String PROPERTY_VIDEO_STREAM_IS_AVAILABLE = "stream_is_available_video";
    private static final String PROPERTY_DIRECTURL                 = "directurl";
    private static final String PROPERTY_DIRECTURL_ACCOUNT         = "directurl_account";
    private static final String HAS_ATTEMPTED_STREAM_DOWNLOAD      = "isvideo";

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        if (this.hasAttemptedStreamDownload(link)) {
            return true;
        } else {
            final AccountType type = account != null ? account.getType() : null;
            if (AccountType.FREE.equals(type)) {
                /* Free Account */
                return true;
            } else if (AccountType.PREMIUM.equals(type)) {
                /* Premium account */
                return true;
            } else {
                /* Free(anonymous) and unknown account type */
                return true;
            }
        }
    }

    public int getMaxChunks(final DownloadLink link, final Account account) {
        if (this.hasAttemptedStreamDownload(link)) {
            return 0;
        } else {
            final AccountType type = account != null ? account.getType() : null;
            if (AccountType.FREE.equals(type)) {
                /* Free Account */
                return 1;
            } else if (AccountType.PREMIUM.equals(type)) {
                /* Premium account */
                return 0;
            } else {
                /* Free(anonymous) and unknown account type */
                return 1;
            }
        }
    }

    private Browser prepBR(final Browser br) {
        prepBRStatic(br);
        synchronized (botSafeCookies) {
            if (!botSafeCookies.isEmpty()) {
                br.setCookies(this.getHost(), botSafeCookies);
            }
        }
        return br;
    }

    public static Browser prepBRStatic(final Browser br) {
        br.setAllowedResponseCodes(410);
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
        if (link == null || link.getPluginPatternMatcher() == null) {
            return null;
        } else if (link.getPluginPatternMatcher().matches(TYPE_FILE)) {
            return new Regex(link.getPluginPatternMatcher(), TYPE_FILE).getMatch(1);
        } else if (link.getPluginPatternMatcher().matches(TYPE_EMBED)) {
            return new Regex(link.getPluginPatternMatcher(), TYPE_EMBED).getMatch(1);
        } else {
            logger.warning("Developer mistake! URL with unknown pattern: " + link.getPluginPatternMatcher());
            return null;
        }
    }

    private String getWeakFilename(final DownloadLink link) {
        if (link.getPluginPatternMatcher().matches(TYPE_FILE)) {
            final String titleWithExtHint = new Regex(link.getPluginPatternMatcher(), TYPE_FILE).getMatch(0);
            final String extFromURL = getExtensionFromFileURL(link.getPluginPatternMatcher());
            if (extFromURL != null) {
                final String titleWithoutExtHint = titleWithExtHint.substring(0, titleWithExtHint.lastIndexOf("-"));
                return titleWithoutExtHint + extFromURL;
            } else {
                return titleWithExtHint;
            }
        } else if (link.getPluginPatternMatcher().matches(TYPE_EMBED)) {
            final Regex embed = new Regex(link.getPluginPatternMatcher(), TYPE_EMBED);
            final String type = embed.getMatch(0);
            final String fid = embed.getMatch(1);
            if (type.equalsIgnoreCase("audio")) {
                return fid + ".mp3";
            } else {
                return fid + ".mp4";
            }
        } else {
            return this.getFID(link);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        if (!link.isNameSet()) {
            link.setName(this.getWeakFilename(link));
        }
        prepBR(this.br);
        br.getPage(link.getPluginPatternMatcher());
        if (isBotBlocked(this.br)) {
            logger.info("Cannot do linkcheck due to antiBot captcha");
            return AvailableStatus.UNCHECKABLE;
        } else if (br.getRequest().getHttpConnection().getResponseCode() == 410) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)Ten plik został usunięty przez użytkownika lub administratora|Użytkownik nie posiada takiego pliku|<title>404 error")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!this.canHandle(br.getURL())) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* Handle non-public files as offline */
        if (br.containsHTML("(?i)Ten plik nie jest publicznie dostępny")) {
            logger.warning("Private files are not yet supported");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String fid = this.getFID(link);
        final String fileURL = regexFileURL(br, fid);
        /* Special handling if embed URL is given. */
        if (link.getPluginPatternMatcher().matches(TYPE_EMBED)) {
            /* Find "real" URL for embed URL */
            if (fileURL == null || !this.canHandle(fileURL)) {
                /* Assume that file is offline. */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            /* Access normal fileURL and resume linkcheck. */
            br.getPage(fileURL);
        }
        if (br.containsHTML("/embed/audio/" + fid)) {
            link.setProperty(PROPERTY_AUDIO_STREAM_IS_AVAILABLE, true);
        } else if (br.containsHTML("/embed/video/" + fid)) {
            link.setProperty(PROPERTY_VIDEO_STREAM_IS_AVAILABLE, true);
        } else {
            link.removeProperty(PROPERTY_AUDIO_STREAM_IS_AVAILABLE);
            link.removeProperty(PROPERTY_VIDEO_STREAM_IS_AVAILABLE);
        }
        /*
         * TODO: Check/fix strange bug: Sometimes we get redirected to an empty template e.g. resulting in filename with value "{{name}}".
         */
        String fileName = br.getRegex("itemprop=\"name\">\\s*[^\\{]([^<>\"]*?)</h").getMatch(0);
        // itemprop="name" style=" font-size: 17px; margin-top: 6px;">Alternatywne Metody Analizy technicznej .pdf</h1>
        if (fileName == null) {
            fileName = br.getRegex("itemprop=\"name\"( style=\"[^<>\"/]+\")?>([^<>\"]*?)</h1>").getMatch(1);
        }
        final String fpat = "\\s*([0-9]+(?:[\\.,][0-9]+)?\\s*[A-Z]{1,2})";
        String filesize = br.getRegex("class='frameFilesSize'>Rozmiar pliku</div>[\t\n\r ]+<div class='frameFilesCountNumber'>" + fpat).getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("(?i)Rozmiar pliku</div>[\t\n\r ]+<div class='frameFilesCountNumber'>" + fpat).getMatch(0);
            if (filesize == null) {
                filesize = br.getRegex("(?i)</i>\\s*Rozmiar pliku</div><div class='frameFilesCountNumber'>" + fpat).getMatch(0);
                if (filesize == null) {
                    filesize = br.getRegex("class='frameFilesCountNumber'>" + fpat).getMatch(0);
                    if (filesize == null) {
                        filesize = br.getRegex("<i class=\"icon-hdd\"></i>\\s*Rozmiar\\s*</div>\\s*<div class='value'>" + fpat).getMatch(0);
                    }
                }
            }
        }
        if (fileName != null) {
            fileName = Encoding.htmlDecode(fileName).trim();
            final String extensionFromURL = getExtensionFromFileURL(regexFileURL(br, fid));
            /* Apply or fix filename extension if needed. */
            if (extensionFromURL != null && !fileName.toLowerCase(Locale.ENGLISH).endsWith(extensionFromURL.toLowerCase(Locale.ENGLISH))) {
                fileName += extensionFromURL;
            }
            link.setName(fileName);
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    private static String regexFileURL(final Browser br, final String fid) {
        return br.getRegex("(https?://[^/\"]+/[^,\"]+,f-" + fid + ",[^/\"]+)").getMatch(0);
    }

    private static String getExtensionFromFileURL(final String fileURL) {
        final String unsafeExt = new Regex(fileURL, "-([A-Za-z0-9]+)$").getMatch(0);
        if (unsafeExt == null) {
            return null;
        } else if (("." + unsafeExt.toLowerCase(Locale.ENGLISH)).matches(DirectHTTP.ENDINGS)) {
            return "." + unsafeExt;
        } else {
            return null;
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, null);
    }

    private static String getDirecturlproperty(final Account account) {
        if (account != null) {
            return PROPERTY_DIRECTURL_ACCOUNT;
        } else {
            return PROPERTY_DIRECTURL;
        }
    }

    /** Returns true if the last download attempt of this item was stream-download. */
    private boolean hasAttemptedStreamDownload(final DownloadLink link) {
        if (link.hasProperty(HAS_ATTEMPTED_STREAM_DOWNLOAD)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isStreamAvailable(final DownloadLink link) {
        if (isAudioStreamAvailable(link)) {
            return true;
        } else if (isVideoStreamAvailable(link)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isAudioStreamAvailable(final DownloadLink link) {
        if (link.hasProperty(PROPERTY_AUDIO_STREAM_IS_AVAILABLE)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isVideoStreamAvailable(final DownloadLink link) {
        if (link.hasProperty(PROPERTY_VIDEO_STREAM_IS_AVAILABLE)) {
            return true;
        } else {
            return false;
        }
    }

    private String getStreamDownloadURL(final DownloadLink link) {
        final String fid = this.getFID(link);
        if (link.hasProperty(PROPERTY_AUDIO_STREAM_IS_AVAILABLE)) {
            return generateContentURLStreamAudio(fid);
        } else if (link.hasProperty(PROPERTY_VIDEO_STREAM_IS_AVAILABLE)) {
            return generateContentURLStreamVideo(fid);
        } else {
            /* No stream download available */
            return null;
        }
    }

    private String generateContentURLStreamAudio(final String fileID) {
        return "https://" + this.getHost() + "/embed/audio/" + fileID;
    }

    private String generateContentURLStreamVideo(final String fileID) {
        return "https://" + this.getHost() + "/embed/video/" + fileID;
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception, PluginException {
        /* Make sure to set antiBot cookies before we attempt to check directURLs! */
        prepBR(this.br);
        synchronized (botSafeCookies) {
            if (!attemptStoredDownloadurlDownload(link, account)) {
                requestFileInformation(link);
                if (isBotBlocked(this.br)) {
                    this.handleAntiBot(this.br);
                    /* Important! Check status if we were blocked before! */
                    requestFileInformation(link);
                }
                String dllink = null;
                final StreamDownloadMode mode = PluginJsonConfig.get(FreeDiscPlConfig.class).getStreamDownloadMode();
                final boolean streamIsAvailable = isStreamAvailable(link);
                if (mode == StreamDownloadMode.PREFER_STREAM && streamIsAvailable) {
                    logger.info("User prefers stream download over original file download");
                } else {
                    final String fid = this.getFID(link);
                    /* TODO: Improve errorhandling */
                    postPageRaw("/download/payment_info", "{\"item_id\":\"" + fid + "\",\"item_type\":1,\"code\":\"\",\"file_id\":" + fid + ",\"no_headers\":1,\"menu_visible\":0}");
                    try {
                        final Map<String, Object> root = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                        final Map<String, Object> response = (Map<String, Object>) root.get("response");
                        final String html = (String) response.get("html");
                        final Map<String, Object> download_data = (Map<String, Object>) response.get("download_data");
                        if (html != null && download_data == null) {
                            /* Download not possible --> Handle errors */
                            br.getRequest().setHtmlCode(html);
                            if (br.containsHTML("(?i)Pobranie plików większych jak [0-9\\.]+ (MB|GB|TB), wymaga opłacenia kosztów transferu")) {
                                /* 2022-03-03: E.g. "Freeusers (= users without account) can only download files up to 1MB".. */
                                logger.info("Account required to download this file");
                                throw new AccountRequiredException();
                            } else if (br.containsHTML("(?i)Wykorzystałeś limit \\d+ darmowych pobrań w tym tygodniu")) {
                                /*
                                 * Zypically limit reached for 5 downloads per week in free account mode (probably bad translation as more
                                 * downloads are possible the next day.)
                                 */
                                if (account != null) {
                                    throw new AccountUnavailableException("Daily downloadlimit reached", 10 * 60 * 1000l);
                                } else {
                                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Daily downloadlimit reached", 10 * 60 * 1000l);
                                }
                            } else {
                                logger.warning("Download impossible for unknown reasons");
                                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                            }
                        } else {
                            final String download_url = download_data.get("download_url").toString();
                            final String item_id = download_data.get("item_id").toString();
                            final String time = download_data.get("time").toString();
                            if (StringUtils.isAllNotEmpty(download_url, item_id, time)) {
                                dllink = download_url + item_id + "/" + time;
                            }
                            if (dllink == null) {
                                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                            }
                        }
                    } catch (final Exception e) {
                        if (mode == StreamDownloadMode.STREAM_AS_FALLBACK && streamIsAvailable) {
                            logger.info("Official download is not possible -> Fallback to stream download");
                        } else {
                            throw e;
                        }
                    }
                }
                if (dllink != null) {
                    /* Download original file */
                    link.removeProperty(HAS_ATTEMPTED_STREAM_DOWNLOAD);
                } else {
                    /* Download stream if possible */
                    if (!streamIsAvailable) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    logger.info("Seems like a stream is available --> Trying to find stream-downloadlink");
                    final String streamEmbedURL = getStreamDownloadURL(link);
                    getPage(streamEmbedURL);
                    dllink = br.getRegex("data-video-url=\"(https?://[^<>\"]*?)\"").getMatch(0);
                    if (dllink == null) {
                        dllink = br.getRegex("(https?://stream\\.[^/]+/(?:audio|video)/\\d+/[^/\"]+)").getMatch(0);
                    }
                    if (StringUtils.isEmpty(dllink)) {
                        logger.warning("Failed to find stream directurl");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    /* This will ensure that file-extension will be corrected later on downloadstart! */
                    link.setProperty(HAS_ATTEMPTED_STREAM_DOWNLOAD, true);
                }
                dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, this.isResumeable(link, account), this.getMaxChunks(link, account));
                if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                    try {
                        br.followConnection(true);
                    } catch (final IOException e) {
                        logger.log(e);
                    }
                    if (this.br.containsHTML("Ten plik jest chwilowo niedos")) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
                    }
                    if (br.getURL().contains("freedisc.pl/pierrw,f-")) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
                    }
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                link.setProperty(getDirecturlproperty(account), dllink);
            }
        }
        /* Correct filename if needed */
        final String filenameFromHeader = getFileNameFromDispositionHeader(dl.getConnection());
        if (filenameFromHeader != null) {
            link.setFinalFileName(Encoding.htmlDecode(filenameFromHeader).trim());
        } else {
            /* Correct filename extension if needed. E.g. required if user downloads stream instead of original file. */
            final String filename = link.getName();
            String realFileExtension = this.getExtensionFromMimeType(dl.getConnection().getContentType());
            /* Fallback if file-extension could not be determined by header. */
            if (realFileExtension == null && this.hasAttemptedStreamDownload(link)) {
                if (this.isAudioStreamAvailable(link)) {
                    realFileExtension = "mp3";
                } else {
                    realFileExtension = "mp4";
                }
            }
            if (filename != null && realFileExtension != null) {
                final String newFilename = this.correctOrApplyFileNameExtension(filename, "." + realFileExtension);
                if (!newFilename.equals(filename)) {
                    logger.info("Filename extension has changed. Old name: " + filename + " | New: " + newFilename);
                    link.setFinalFileName(newFilename);
                }
            }
        }
        dl.startDownload();
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link, final Account account) throws Exception {
        final String url = link.getStringProperty(getDirecturlproperty(account));
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        boolean valid = false;
        try {
            final Browser brc = br.cloneBrowser();
            /* Very important otherwise we'll get error-response 404! */
            brc.getHeaders().put("Referer", "https://" + this.getHost() + "/");
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, this.isResumeable(link, account), this.getMaxChunks(link, account));
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                valid = true;
                return true;
            } else {
                link.removeProperty(PROPERTY_DIRECTURL);
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

    public static boolean isBotBlocked(final Browser br) {
        if (br.containsHTML("(?i)Przez roboty internetowe nasze serwery się gotują|g-recaptcha")) {
            return true;
        } else {
            return false;
        }
    }

    private void getPage(final String url) throws Exception {
        br.getPage(url);
        handleAntiBot(this.br);
    }

    private void postPageRaw(final String url, final String parameters) throws Exception {
        this.br.postPageRaw(url, parameters);
        handleAntiBot(this.br);
    }

    private void handleAntiBot(final Browser br) throws Exception {
        int retry = 0;
        while (isBotBlocked(this.br)) {
            if (isAbort()) {
                throw new InterruptedException();
            }
            /* Process anti-bot captcha */
            logger.info("Login captcha / spam protection detected");
            final DownloadLink originalDownloadLink = this.getDownloadLink();
            final DownloadLink downloadlinkToUse;
            if (originalDownloadLink != null) {
                downloadlinkToUse = originalDownloadLink;
            } else {
                /* E.g. captcha during login process */
                downloadlinkToUse = new DownloadLink(this, "Account Login " + this.getHost(), this.getHost(), "https://" + this.getHost(), true);
            }
            final Request request = br.getRequest();
            /* Remove uncommented code from html */
            request.setHtmlCode(request.getHtmlCode().replaceAll("(?s)(<!--.*?-->)", ""));
            Form captchaForm = br.getFormByRegex("name\\s*=\\s*\"captcha\"");
            if (captchaForm == null) {
                captchaForm = br.getFormByRegex("value\\s*=\\s*\"Wchodzę\"");
                if (captchaForm == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            try {
                this.setDownloadLink(downloadlinkToUse);
                if (request.containsHTML("class\\s*=\\s*\"g-recaptcha\"")) {
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                    captchaForm.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                } else {
                    final String captcha = br.getRegex("\"([^\"]*captcha\\.png)").getMatch(0);
                    if (captcha == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final String code = getCaptchaCode(captcha, getDownloadLink());
                    captchaForm.put("captcha", Encoding.urlEncode(code));
                }
            } finally {
                if (originalDownloadLink != null) {
                    this.setDownloadLink(originalDownloadLink);
                }
            }
            br.submitForm(captchaForm);
            if (isBotBlocked(this.br)) {
                if (++retry == 5) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Anti-Bot block", 5 * 60 * 1000l);
                }
            } else {
                this.saveSession(br);
                break;
            }
        }
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                br2.getHeaders().put("Referer", "https://freedisc.pl/");
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    return dllink;
                } else {
                    throw new IOException();
                }
            } catch (final Exception e) {
                /* Do not check this directurl again! */
                link.removeProperty(property);
                logger.log(e);
                return null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return null;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            br.setCookiesExclusive(true);
            prepBR(br);
            final Cookies cookies = account.loadCookies("");
            if (cookies != null) {
                /* Always try to re-use cookies. */
                br.setCookies(this.getHost(), cookies);
                if (!force) {
                    /* Trust cookies without check */
                    // logger.info("Trust cookies without check");
                    return;
                }
                br.getPage("https://" + this.getHost() + "/");
                handleAntiBot(br);
                if (br.containsHTML("id=\"btnLogout\"")) {
                    logger.info("Cookie login successful");
                    account.saveCookies(br.getCookies(this.getHost()), "");
                    return;
                } else {
                    logger.info("Cookie login failed");
                    br.clearCookies(br.getHost());
                    account.clearCookies("");
                }
            }
            logger.info("Performing full login");
            br.getPage("https://" + this.getHost() + "/");
            handleAntiBot(br);
            // this is done via ajax!
            br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.getHeaders().put("Content-Type", "application/json");
            br.getHeaders().put("Cache-Control", null);
            br.postPageRaw("/account/signin_set", "{\"email_login\":\"" + account.getUser() + "\",\"password_login\":\"" + account.getPass() + "\",\"remember_login\":1,\"provider_login\":\"\"}");
            if (br.getCookie(br.getHost(), "login_remember", Cookies.NOTDELETEDPATTERN) == null && br.getCookie(br.getHost(), "cookie_login_remember", Cookies.NOTDELETEDPATTERN) == null) {
                throw new AccountInvalidException();
            }
            account.saveCookies(br.getCookies(this.getHost()), "");
        }
    }

    private void saveSession(final Browser br) {
        synchronized (botSafeCookies) {
            botSafeCookies.clear();
            botSafeCookies.add(br.getCookies(br.getHost()));
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        account.setMaxSimultanDownloads(-1);
        account.setConcurrentUsePossible(false);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        login(account, false);
        handleDownload(link, account);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        if (link != null) {
            link.removeProperty(PROPERTY_DIRECTURL);
            link.removeProperty(PROPERTY_DIRECTURL_ACCOUNT);
            link.removeProperty(HAS_ATTEMPTED_STREAM_DOWNLOAD);
        }
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return FreeDiscPlConfig.class;
    }
}
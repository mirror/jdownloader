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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.appwork.storage.JSonMapperException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.AbstractRecaptchaV2;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.config.FreeDiscPlConfig;
import org.jdownloader.plugins.components.config.FreeDiscPlConfig.StreamDownloadMode;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class FreeDiscPl extends PluginForHost {
    public FreeDiscPl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://freedisc.pl/");
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
            String regex = "https?://(?:(?:www|m)\\.)?" + buildHostsPatternPart(domains);
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

    private static final boolean       PREFER_AVAILABLECHECK_VIA_AJAX_REQUEST = true;
    protected static Cookies           botSafeCookies                         = new Cookies();
    private static final AtomicLong    timestampLastFreeDownloadStarted       = new AtomicLong(0);
    /* Don't touch the following! */
    private static final AtomicInteger freeRunning                            = new AtomicInteger(0);
    private static final String        TYPE_FILE                              = "https?://[^/]+/(?:#(?:!|%21))?([A-Za-z0-9\\-_]+,f-(\\d+)(,([\\w\\-]+))?)";
    private static final String        TYPE_EMBED_ALL                         = "https?://[^/]+/embed/(audio|video)/(\\d+)";
    private static final String        TYPE_EMBED_AUDIO                       = "https?://[^/]+/embed/audio/(\\d+)";
    private static final String        TYPE_EMBED_VIDEO                       = "https?://[^/]+/embed/video/(\\d+)";
    private static final String        PROPERTY_AUDIO_STREAM_IS_AVAILABLE     = "stream_is_available_audio";
    private static final String        PROPERTY_VIDEO_STREAM_IS_AVAILABLE     = "stream_is_available_video";
    private static final String        PROPERTY_DIRECTURL                     = "directurl";
    private static final String        PROPERTY_DIRECTURL_ACCOUNT             = "directurl_account";
    private static final String        PROPERTY_HAS_ATTEMPTED_STREAM_DOWNLOAD = "isvideo";
    private static final String        PROPERTY_STREAMING_FILENAME            = "streaming_filename";
    private static final String        PROPERTY_UPLOADER                      = "uploader";

    private boolean preferAvailablecheckViaAjaxRequest(final DownloadLink link) {
        final String username = getUploader(link);
        if (PREFER_AVAILABLECHECK_VIA_AJAX_REQUEST && username != null) {
            return true;
        } else {
            return false;
        }
    }

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

    private Browser prepBR(final Browser br, final Account account) {
        prepBRStatic(br);
        /* In account mode we're using account cookies thus we only need those when there is not account available. */
        if (account == null) {
            synchronized (botSafeCookies) {
                if (!botSafeCookies.isEmpty()) {
                    br.setCookies(this.getHost(), botSafeCookies);
                }
            }
        }
        return br;
    }

    public static Browser prepBRStatic(final Browser br) {
        br.setAllowedResponseCodes(410);
        br.setFollowRedirects(true);
        /* 2023-01-30: Looks like they've blocked our default User-Agent. */
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.0.0 Safari/537.36");
        return br;
    }

    public static Browser prepBRAjax(final Browser br) {
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Content-Type", "application/json");
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

    /** Returns name of the uploader. */
    private String getUploader(final DownloadLink link) {
        if (link == null || link.getPluginPatternMatcher() == null) {
            return null;
        } else if (link.hasProperty(PROPERTY_UPLOADER)) {
            return link.getStringProperty(PROPERTY_UPLOADER);
        } else if (link.getPluginPatternMatcher().matches(TYPE_FILE)) {
            return new Regex(link.getPluginPatternMatcher(), TYPE_FILE).getMatch(0);
        } else {
            return null;
        }
    }

    private String getFID(final DownloadLink link) {
        if (link == null || link.getPluginPatternMatcher() == null) {
            return null;
        } else if (link.getPluginPatternMatcher().matches(TYPE_FILE)) {
            return new Regex(link.getPluginPatternMatcher(), TYPE_FILE).getMatch(1);
        } else if (link.getPluginPatternMatcher().matches(TYPE_EMBED_ALL)) {
            return new Regex(link.getPluginPatternMatcher(), TYPE_EMBED_ALL).getMatch(1);
        } else {
            logger.warning("Developer mistake! URL with unknown pattern: " + link.getPluginPatternMatcher());
            return null;
        }
    }

    private String getWeakFilename(final DownloadLink link) {
        if (link.getPluginPatternMatcher().matches(TYPE_FILE)) {
            final String titleWithExtHint = new Regex(link.getPluginPatternMatcher(), TYPE_FILE).getMatch(2);
            if (titleWithExtHint != null) {
                final String extFromURL = getExtensionFromNameInFileURL(link.getPluginPatternMatcher());
                if (extFromURL != null) {
                    final String titleWithoutExtHint = titleWithExtHint.substring(0, titleWithExtHint.lastIndexOf("-"));
                    return titleWithoutExtHint + extFromURL;
                } else {
                    return titleWithExtHint;
                }
            } else {
                return this.getFID(link);
            }
        } else if (link.getPluginPatternMatcher().matches(TYPE_EMBED_ALL)) {
            final Regex embed = new Regex(link.getPluginPatternMatcher(), TYPE_EMBED_ALL);
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
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return requestFileInformation(link, account);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        if (preferAvailablecheckViaAjaxRequest(link)) {
            try {
                return requestFileInformationAJAX(link, account);
            } catch (final JSonMapperException ignore) {
                logger.log(ignore);
                logger.warning("requestFileInformationAJAX failed -> Fallback to check over html");
                return this.requestFileInformationHTML(link, account);
            }
        } else {
            return this.requestFileInformationHTML(link, account);
        }
    }

    private AvailableStatus requestFileInformationAJAX(final DownloadLink link, final Account account) throws Exception {
        if (!link.isNameSet()) {
            link.setName(this.getWeakFilename(link));
        }
        prepBR(this.br, account);
        if (account != null) {
            this.login(account, false);
        }
        prepBRAjax(this.br);
        final String username = this.getUploader(link);
        final String fid = this.getFID(link);
        if (fid == null || username == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage("https://" + this.getHost() + "/file/file_data/get/" + username + "/" + fid);
        if (isBotBlocked(this.br)) {
            logger.info("Cannot do linkcheck due to antiBot captcha");
            return AvailableStatus.UNCHECKABLE;
        }
        final Map<String, Object> root = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        /* TODO: Make use of field "current_file_description" */
        final Map<String, Object> data = (Map<String, Object>) JavaScriptEngineFactory.walkJson(root, "response/data");
        if ((Boolean) data.get("file_exists") == Boolean.FALSE) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if ((Boolean) data.get("file_abused") == Boolean.TRUE) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final boolean is_my_file = ((Boolean) data.get("is_my_file")).booleanValue();
        final Map<String, Object> file_data = (Map<String, Object>) data.get("file_data");
        final String filenameWithoutExt = file_data.get("fileName").toString();
        final String fileExtension = (String) file_data.get("fileExtension");
        final long filesize = Long.parseLong(file_data.get("fileSize").toString());
        String streamingFilename = null;
        Long streamingFilesize = null;
        final Map<String, Object> file_interface_data = (Map<String, Object>) data.get("file_interface_data");
        /* If no stream is available, this URL will simply lead to a image representing the file-type symbol. */
        final String embedURL = (String) file_interface_data.get("content_link");
        if (embedURL.matches(TYPE_EMBED_AUDIO)) {
            link.setProperty(PROPERTY_AUDIO_STREAM_IS_AVAILABLE, true);
            final Map<String, Object> streamingMetadata = (Map<String, Object>) file_interface_data.get("content_music");
            streamingFilename = streamingMetadata.get("fileName").toString();
            streamingFilesize = Long.parseLong(streamingMetadata.get("size").toString());
        } else if (embedURL.matches(TYPE_EMBED_VIDEO)) {
            link.setProperty(PROPERTY_VIDEO_STREAM_IS_AVAILABLE, true);
        } else {
            link.removeProperty(PROPERTY_AUDIO_STREAM_IS_AVAILABLE);
            link.removeProperty(PROPERTY_VIDEO_STREAM_IS_AVAILABLE);
        }
        if (!StringUtils.isEmpty(streamingFilename)) {
            link.setProperty(PROPERTY_STREAMING_FILENAME, streamingFilename);
        }
        final boolean userPrefersStreamDownloads = PluginJsonConfig.get(FreeDiscPlConfig.class).getStreamDownloadMode() == StreamDownloadMode.PREFER_STREAM;
        if (userPrefersStreamDownloads && !StringUtils.isEmpty(streamingFilename) && streamingFilesize != null) {
            link.setFinalFileName(streamingFilename);
            link.setVerifiedFileSize(streamingFilesize.longValue());
        } else if (userPrefersStreamDownloads && this.isStreamAvailable(link)) {
            /* Set custom made streaming filename */
            if (this.isAudioStreamAvailable(link)) {
                link.setFinalFileName(filenameWithoutExt + ".mp3");
            } else {
                link.setFinalFileName(filenameWithoutExt + ".mp4");
            }
            link.setDownloadSize(filesize);
        } else {
            /* Set original filename */
            if (!StringUtils.isEmpty(fileExtension)) {
                link.setFinalFileName(filenameWithoutExt + "." + fileExtension);
            } else {
                /* Filename has no extension */
                link.setFinalFileName(filenameWithoutExt);
            }
            link.setVerifiedFileSize(filesize);
        }
        /* File has been moved to trash by current owner. It is still downloadable but only for the owner. */
        if (file_data.get("inTrash").toString().equals("1") && !is_my_file) {
            throw new AccountRequiredException();
        }
        return AvailableStatus.TRUE;
    }

    private AvailableStatus requestFileInformationHTML(final DownloadLink link, final Account account) throws Exception {
        if (!link.isNameSet()) {
            link.setName(this.getWeakFilename(link));
        }
        prepBR(this.br, account);
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
        if (link.getPluginPatternMatcher().matches(TYPE_EMBED_ALL)) {
            /* Find "real" URL for embed URL */
            final Regex fileURLRegex = new Regex(fileURL, TYPE_FILE);
            if (!fileURLRegex.matches()) {
                /* Assume that file is offline. */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            /* Set name of uploader as property so next time ajax linkcheck can be used. */
            final String uploader = fileURLRegex.getMatch(0);
            link.setProperty(PROPERTY_UPLOADER, uploader);
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
        String fileName = br.getRegex("itemprop=\"name\">\\s*([^\\{][^<>\"]*?)</h").getMatch(0);
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
            final String extensionFromURL = getExtensionFromNameInFileURL(regexFileURL(br, fid));
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

    public static String getExtensionFromNameInFileURL(final String fileURL) {
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
        final boolean waitBetweenFreeDownloadStarts = false;
        if (waitBetweenFreeDownloadStarts) {
            /* Ensure that we wait at least X seconds between download-starts. */
            final long waitMillisBetweenDownloads = 30 * 1000;
            final long passedMillisSinceLastDownload = System.currentTimeMillis() - timestampLastFreeDownloadStarted.get();
            if (passedMillisSinceLastDownload < waitMillisBetweenDownloads) {
                final long wait = Math.min(waitMillisBetweenDownloads - passedMillisSinceLastDownload, waitMillisBetweenDownloads);
                this.sleep(wait, link);
            }
        }
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
        if (link.hasProperty(PROPERTY_HAS_ATTEMPTED_STREAM_DOWNLOAD)) {
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
        prepBR(this.br, account);
        synchronized (botSafeCookies) {
            if (!attemptStoredDownloadurlDownload(link, account)) {
                requestFileInformation(link, account);
                if (isBotBlocked(this.br)) {
                    this.handleAntiBot(this.br, account);
                    /* Important! Check status if we were blocked before! */
                    requestFileInformation(link, account);
                }
                String dllink = null;
                final StreamDownloadMode mode = PluginJsonConfig.get(FreeDiscPlConfig.class).getStreamDownloadMode();
                final boolean streamIsAvailable = isStreamAvailable(link);
                if (this.hasAttemptedStreamDownload(link)) {
                    /*
                     * Prevent users from trying to resume started stream downloads with original file download --> This wouldend up in
                     * corrupt files!
                     */
                    logger.info("User used streaming download last time -> Force stream download this time too");
                } else if (mode == StreamDownloadMode.PREFER_STREAM && streamIsAvailable) {
                    logger.info("User prefers stream download over original file download");
                } else {
                    final String fid = this.getFID(link);
                    /* TODO: Improve errorhandling */
                    postPageRaw("/download/payment_info", "{\"item_id\":\"" + fid + "\",\"item_type\":1,\"code\":\"\",\"file_id\":" + fid + ",\"no_headers\":1,\"menu_visible\":0}", account);
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
                                 * Typically limit reached for 5 downloads per week in free account mode. Probably bad translation as more
                                 * downloads are possible some minutes/hours later.
                                 */
                                if (account != null) {
                                    throw new AccountUnavailableException("Daily downloadlimit reached", 5 * 60 * 1000l);
                                } else {
                                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Daily downloadlimit reached", 5 * 60 * 1000l);
                                }
                            } else {
                                logger.warning("Download impossible for unknown reasons");
                                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                            }
                        } else {
                            final String download_url = download_data.get("download_url").toString();
                            final String item_id = download_data.get("item_id").toString();
                            final String time = download_data.get("time").toString();
                            if (StringUtils.isEmpty(download_url) || StringUtils.isEmpty(item_id) || StringUtils.isEmpty(time)) {
                                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                            }
                            dllink = download_url + item_id + "/" + time;
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
                    link.removeProperty(PROPERTY_HAS_ATTEMPTED_STREAM_DOWNLOAD);
                } else {
                    /* Download stream if possible */
                    if (!streamIsAvailable) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    logger.info("Seems like a stream is available --> Trying to find stream-downloadlink");
                    final String streamEmbedURL = getStreamDownloadURL(link);
                    getPage(streamEmbedURL, account);
                    dllink = br.getRegex("data-video-url=\"(https?://[^<>\"]*?)\"").getMatch(0);
                    if (dllink == null) {
                        dllink = br.getRegex("(https?://stream\\.[^/]+/(?:audio|video)/\\d+/[^/\"]+)").getMatch(0);
                    }
                    if (StringUtils.isEmpty(dllink)) {
                        logger.warning("Failed to find stream directurl");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    /* This will ensure that file-extension will be corrected later on downloadstart! */
                    link.setProperty(PROPERTY_HAS_ATTEMPTED_STREAM_DOWNLOAD, true);
                    /* Important! Stream-filesize can be different than previously set verifiedFilesize! */
                    link.setVerifiedFileSize(-1);
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
            String newFilename = null;
            final String filename = link.getName();
            if (link.hasProperty(PROPERTY_STREAMING_FILENAME)) {
                newFilename = link.getStringProperty(PROPERTY_STREAMING_FILENAME);
            } else {
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
                    newFilename = this.correctOrApplyFileNameExtension(filename, "." + realFileExtension);
                }
            }
            if (filename != null && newFilename != null && !newFilename.equals(filename)) {
                logger.info("Filename (extension) has changed. Old name: " + filename + " | New: " + newFilename);
                link.setFinalFileName(newFilename);
            }
        }
        try {
            /* Add a download slot */
            controlMaxFreeDownloads(account, link, +1);
            /* Start download */
            dl.startDownload();
        } finally {
            /* Remove download slot */
            controlMaxFreeDownloads(account, link, -1);
        }
    }

    protected void controlMaxFreeDownloads(final Account account, final DownloadLink link, final int num) {
        if (account == null) {
            synchronized (freeRunning) {
                final int before = freeRunning.get();
                final int after = before + num;
                if (after > before) {
                    synchronized (timestampLastFreeDownloadStarted) {
                        timestampLastFreeDownloadStarted.set(System.currentTimeMillis());
                    }
                }
                freeRunning.set(after);
                logger.info("freeRunning(" + link.getName() + ")|max:" + getMaxSimultanFreeDownloadNum() + "|before:" + before + "|after:" + after + "|num:" + num);
            }
        }
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

    private void getPage(final String url, final Account account) throws Exception {
        br.getPage(url);
        handleAntiBot(this.br, account);
    }

    private void postPageRaw(final String url, final String parameters, final Account account) throws Exception {
        this.br.postPageRaw(url, parameters);
        handleAntiBot(this.br, account);
    }

    private void handleAntiBot(final Browser br, final Account account) throws Exception {
        final DownloadLink originalDownloadLink = this.getDownloadLink();
        final DownloadLink downloadlinkToUse;
        if (originalDownloadLink != null) {
            downloadlinkToUse = originalDownloadLink;
        } else {
            /* E.g. captcha during login process */
            downloadlinkToUse = new DownloadLink(this, "Account Login " + this.getHost(), this.getHost(), "https://" + this.getHost(), true);
        }
        final Object lock = account != null ? account : downloadlinkToUse;
        synchronized (lock) {
            final Future<Boolean> abort = new Future<Boolean>() {
                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    return false;
                }

                @Override
                public boolean isCancelled() {
                    return false;
                }

                @Override
                public boolean isDone() {
                    return true;
                }

                @Override
                public Boolean get() throws InterruptedException, ExecutionException {
                    return FreeDiscPl.this.isAbort();
                }

                @Override
                public Boolean get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                    return FreeDiscPl.this.isAbort();
                }
            };
            this.setDownloadLink(downloadlinkToUse);
            try {
                handleAntiBot(this, br, abort, downloadlinkToUse, null);
                saveSession(botSafeCookies, br, account);
            } finally {
                this.setDownloadLink(originalDownloadLink);
            }
        }
    }

    public static void saveSession(final Cookies botSafeCookies, final Browser br, final Account account) {
        if (account != null) {
            synchronized (account) {
                account.saveCookies(br.getCookies(br.getHost()), "");
            }
        } else {
            synchronized (botSafeCookies) {
                botSafeCookies.clear();
                botSafeCookies.add(br.getCookies(br.getHost()));
            }
        }
    }

    public static void handleAntiBot(final Plugin plugin, final Browser br, final Future<Boolean> abort, final DownloadLink downloadLink, final CryptedLink cryptedLink) throws Exception {
        int retry = 0;
        while (isBotBlocked(br)) {
            if (abort.get()) {
                throw new InterruptedException();
            }
            /* Process anti-bot captcha */
            plugin.getLogger().info("Login captcha / spam protection detected");
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
            if (AbstractRecaptchaV2.containsRecaptchaV2Class(br)) {
                final String recaptchaV2Response;
                if (plugin instanceof PluginForHost) {
                    recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2((PluginForHost) plugin, br).getToken();
                } else if (plugin instanceof PluginForDecrypt) {
                    recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2((PluginForDecrypt) plugin, br).getToken();
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                captchaForm.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            } else {
                final String captcha = br.getRegex("\"([^\"]*captcha\\.png)").getMatch(0);
                if (captcha == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final String code;
                if (plugin instanceof PluginForHost) {
                    if (downloadLink == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    } else {
                        code = ((PluginForHost) plugin).getCaptchaCode(captcha, downloadLink);
                    }
                } else if (plugin instanceof PluginForDecrypt) {
                    if (cryptedLink == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    } else {
                        code = ((PluginForDecrypt) plugin).getCaptchaCode(captcha, cryptedLink);
                    }
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                captchaForm.put("captcha", Encoding.urlEncode(code));
            }
            br.submitForm(captchaForm);
            if (isBotBlocked(br)) {
                if (++retry == 5) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Anti-Bot block", 5 * 60 * 1000l);
                }
            } else {
                break;
            }
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        final int running = freeRunning.get();
        return running + 1;
    }

    public void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            br.setCookiesExclusive(true);
            prepBR(br, account);
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
                handleAntiBot(br, account);
                if (isLoggedinHTML(br)) {
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
            handleAntiBot(br, account);
            prepBRAjax(br);
            br.postPageRaw("/account/signin_set", "{\"email_login\":\"" + account.getUser() + "\",\"password_login\":\"" + account.getPass() + "\",\"remember_login\":1,\"provider_login\":\"\"}");
            final Map<String, Object> root = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            final Map<String, Object> response = (Map<String, Object>) root.get("response");
            if (((Boolean) root.get("success")) == Boolean.FALSE) {
                /*
                 * E.g.
                 * {"response":{"info":"Jeden u\u017cytkownik, jedno konto! Pozosta\u0142e konta zosta\u0142y czasowo zablokowane!"},"type":
                 * "set","success":false}
                 */
                final String errMsg = (String) response.get("info");
                if (errMsg == null) {
                    throw new AccountInvalidException();
                } else if (errMsg.equalsIgnoreCase("Jeden użytkownik, jedno konto! Pozostałe konta zostały czasowo zablokowane!")) {
                    /**
                     * 2022-03-22: Account is temp. banned under current IP. This can happen when trying to login with two accounts under
                     * the same IP. </br>
                     * Solution: Wait and retry later or delete cookies, change IP and try again.
                     */
                    throw new AccountUnavailableException(errMsg, 5 * 60 * 1000l);
                } else {
                    throw new AccountInvalidException(errMsg);
                }
            }
            if (!isLoggedinHTML(response.get("html").toString())) {
                throw new AccountInvalidException();
            }
            account.saveCookies(br.getCookies(this.getHost()), "");
        }
    }

    private boolean isLoggedinHTML(final Browser br) {
        return isLoggedinHTML(br.getRequest().getHtmlCode());
    }

    private boolean isLoggedinHTML(final String str) {
        if (str.contains("id=\"btnLogout\"")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        final Browser brc = br.cloneBrowser();
        prepBRAjax(brc);
        brc.getPage("/settings/get/");
        final Map<String, Object> root = JSonStorage.restoreFromString(brc.toString(), TypeRef.HASHMAP);
        final Map<String, Object> response = (Map<String, Object>) root.get("response");
        final Map<String, Object> profile = (Map<String, Object>) response.get("profile");
        final Map<String, Object> user_settings = (Map<String, Object>) response.get("user_settings");
        final long points = ((Number) profile.get("points")).longValue();
        final String register_date = profile.get("register_date").toString();
        ai.setCreateTime(TimeFormatter.getMilliSeconds(register_date, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH));
        ai.setPremiumPoints(points);
        ai.setUnlimitedTraffic();
        /* TODO: Find out differences between Free and Premium accounts */
        final String accStatus;
        if (((Boolean) profile.get("is_premium")) == Boolean.TRUE) {
            account.setType(AccountType.PREMIUM);
            accStatus = "Premium Account";
        } else {
            account.setType(AccountType.FREE);
            accStatus = "Free Account";
        }
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            final int free_downloads_count = Integer.parseInt(user_settings.get("free_downloads_count").toString());
            ai.setStatus(accStatus + " | Free DLs so far: " + free_downloads_count);
        }
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
            link.removeProperty(PROPERTY_HAS_ATTEMPTED_STREAM_DOWNLOAD);
        }
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return FreeDiscPlConfig.class;
    }
}
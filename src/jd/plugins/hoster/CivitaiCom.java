//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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
import java.util.Map;
import java.util.regex.Pattern;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class CivitaiCom extends PluginForHost {
    public CivitaiCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://" + getHost() + "/login?returnUrl=/login?returnUrl=/");
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.API_KEY_LOGIN };
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public String getAGBLink() {
        return "https://" + getHost() + "/content/tos";
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "civitai.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "(" + PATTERN_IMAGE.pattern() + "|" + PATTERN_DOWNLOAD_MODELS.pattern() + ")");
        }
        return ret.toArray(new String[0]);
    }

    /* Connection stuff */
    private static final Pattern PATTERN_IMAGE           = Pattern.compile("/images/(\\d+).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_DOWNLOAD_MODELS = Pattern.compile("/api/download/models/(\\d+).*", Pattern.CASE_INSENSITIVE);
    private final String         PROPERTY_DIRECTURL      = "directurl";

    @Override
    public String getLinkID(final DownloadLink link) {
        final Regex regex_image = new Regex(link.getPluginPatternMatcher(), PATTERN_IMAGE);
        final Regex regex_download_models;
        if (regex_image.patternFind()) {
            return "civitai://image/" + regex_image.getMatch(0);
        } else if ((regex_download_models = new Regex(link.getPluginPatternMatcher(), PATTERN_DOWNLOAD_MODELS)).patternFind()) {
            return "civitai://download_models/" + regex_download_models.getMatch(0);
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        final Regex regex_image = new Regex(link.getPluginPatternMatcher(), PATTERN_IMAGE);
        final Regex regex_download_models;
        if (regex_image.patternFind()) {
            return regex_image.getMatch(0);
        } else if ((regex_download_models = new Regex(link.getPluginPatternMatcher(), PATTERN_DOWNLOAD_MODELS)).patternFind()) {
            return regex_download_models.getMatch(0);
        } else {
            /* Unsupported link */
            return null;
        }
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    public int getMaxChunks(final DownloadLink link, final Account account) {
        return 0;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, null, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        final String contentID;
        final Regex regex_image = new Regex(link.getPluginPatternMatcher(), PATTERN_IMAGE);
        final Regex regex_download_models;
        String extDefault = null;
        if (regex_image.patternFind()) {
            contentID = regex_image.getMatch(0);
            extDefault = ".jpg";
        } else if ((regex_download_models = new Regex(link.getPluginPatternMatcher(), PATTERN_DOWNLOAD_MODELS)).patternFind()) {
            contentID = regex_download_models.getMatch(0);
        } else {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!link.isNameSet()) {
            /* Fallback */
            if (extDefault != null) {
                link.setName(contentID + extDefault);
            } else {
                link.setName(contentID);
            }
        }
        this.setBrowserExclusive();
        if (account != null) {
            this.login(account);
        }
        try {
            if (regex_image.patternFind()) {
                br.getPage(link.getPluginPatternMatcher());
                if (br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else if (br.getHttpConnection().getResponseCode() == 503) {
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Error 503 service unavailable");
                }
                final String json = br.getRegex("type\\s*=\\s*\"application/json\"[^>]*>(\\{\"props.*?)</script>").getMatch(0);
                final Map<String, Object> entries = restoreFromString(json, TypeRef.MAP);
                final Map<String, Object> imagemap = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "props/pageProps/trpcState/json/queries/{0}/state/data");
                if (imagemap == null) {
                    /* Invalid link e.g. https://civitai.com/images/1234567 */
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                final Map<String, Object> metadata = (Map<String, Object>) imagemap.get("metadata");
                String filename = (String) imagemap.get("name");
                final Number filesize = (Number) metadata.get("size");
                if (!StringUtils.isEmpty(filename)) {
                    filename = Encoding.htmlDecode(filename).trim();
                    link.setName(filename);
                } else {
                    /* Fallback */
                    filename = contentID;
                }
                final String mimeType = (String) imagemap.get("mimeType");
                final String ext = getExtensionFromMimeType(mimeType);
                if (ext != null) {
                    link.setFinalFileName(this.applyFilenameExtension(filename, "." + ext));
                } else {
                    link.setName(filename);
                }
                if (filesize != null) {
                    link.setDownloadSize(filesize.longValue());
                }
                /**
                 * 2024-03-11: Important: Do not open up the regex for original image too much or you run into risk of accidentally
                 * downloading the wrong image, see: </br>
                 * https://board.jdownloader.org/showthread.php?t=95419
                 */
                final String directurlOriginal = br.getRegex("class=\"mantine-it6rft\" src=\"(https?://image\\.civitai\\.com/[^\"]+/original=true/[^\"]+)").getMatch(0);
                if (directurlOriginal != null) {
                    /* Best case: We can download the original file. */
                    link.setProperty(PROPERTY_DIRECTURL, directurlOriginal);
                } else {
                    /* 2023-09-11: Base URL hardcoded from: https://civitai.com/_next/static/chunks/pages/_app-191d571abe9dc30e.js */
                    final String baseURL = "https://image.civitai.com/xG1nkqKTMzGDvpLrqFT7WA/";
                    final String directurl = baseURL + imagemap.get("url") + "/width=" + metadata.get("width") + "/" + Encoding.urlEncode(filename);
                    link.setProperty(PROPERTY_DIRECTURL, directurl);
                }
            } else {
                if (isDownload) {
                    /* Do nothing - download handling will take care. */
                    return AvailableStatus.UNCHECKABLE;
                }
                basicLinkCheck(br, br.createGetRequest(link.getPluginPatternMatcher()), link, null, extDefault, FILENAME_SOURCE.HEADER);
            }
        } catch (final AccountRequiredException e) {
            if (isDownload) {
                throw e;
            } else {
                /* File is online but account is required to download it. */
                return AvailableStatus.TRUE;
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, null);
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception, PluginException {
        requestFileInformation(link, account, true);
        String dllink;
        if (new Regex(link.getPluginPatternMatcher(), PATTERN_IMAGE).patternFind()) {
            dllink = link.getStringProperty(PROPERTY_DIRECTURL);
            if (StringUtils.isEmpty(dllink)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Failed to find final downloadurl");
            }
        } else {
            dllink = link.getPluginPatternMatcher();
            br.setFollowRedirects(false);
            br.getPage(link.getPluginPatternMatcher());
            dllink = br.getRedirectLocation();
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Failed to find final downloadurl");
            }
            /* Important! Clear authorization header else we will run into http error 400 on download attempt! */
            br.getHeaders().remove(HTTPConstants.HEADER_REQUEST_AUTHORIZATION);
            /* Allow subsequent redirects. */
            br.setFollowRedirects(true);
        }
        final String widthValue = new Regex(dllink, "(/width=\\d+[^/]*/)").getMatch(0);
        if (widthValue != null) {
            /*
             * Special: Replace the 'width' part with 'original=true does in some cases grant us access to download a better quality
             * (original image).
             */
            final String modifiedOriginalURL = dllink.replace(widthValue, "/original=true/");
            logger.info("Trying to download original image via modified URL: " + modifiedOriginalURL);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, modifiedOriginalURL, this.isResumeable(link, null), this.getMaxChunks(link, account));
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                logger.info("Failed to download original image with trick -> Download normal image");
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, this.isResumeable(link, null), this.getMaxChunks(link, account));
            }
        } else {
            /* Download the URL we have */
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, this.isResumeable(link, null), this.getMaxChunks(link, account));
        }
        handleConnectionErrors(br, dl.getConnection());
        dl.startDownload();
    }

    @Override
    protected void handleConnectionErrors(final Browser br, final URLConnectionAdapter con) throws PluginException, IOException {
        if (this.looksLikeDownloadableContent(con)) {
            /* No error */
            return;
        }
        br.followConnection(true);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (con.getURL().toExternalForm().matches("https?://[^/]+/login.*")) {
            throw new AccountRequiredException("Free account required to download this file");
        }
        throwConnectionExceptions(br, con);
        throwFinalConnectionException(br, con);
    }

    @Override
    protected boolean looksLikeDownloadableContent(URLConnectionAdapter urlConnection) {
        if (super.looksLikeDownloadableContent(urlConnection)) {
            return true;
        } else if (urlConnection.getResponseCode() == 200 || urlConnection.getResponseCode() == 206) {
            /*
             * 2024-03-25: They're sometimes returning wrong content-type information for images, see:
             * https://board.jdownloader.org/showthread.php?t=95419
             */
            final long completeContentLength = urlConnection.getCompleteContentLength();
            return "text/plain".equals(urlConnection.getContentType()) && (completeContentLength == -1 || completeContentLength > 512);
        } else {
            return false;
        }
    }

    private void login(final Account account) throws Exception {
        synchronized (account) {
            br.setCookiesExclusive(true);
            /* See: https://github.com/civitai/civitai/wiki/REST-API-Reference#authorization */
            br.getHeaders().put(HTTPConstants.HEADER_REQUEST_AUTHORIZATION, "Bearer " + account.getPass());
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account);
        /* We can't check the API key yet */
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.handleDownload(link, account);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return false;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        link.removeProperty(PROPERTY_DIRECTURL);
    }

    @Override
    protected String getAPILoginHelpURL() {
        return "https://" + getHost() + "/user/account";
    }

    @Override
    protected boolean looksLikeValidAPIKey(final String str) {
        if (str == null) {
            return false;
        } else if (str.matches("[a-f0-9]{32}")) {
            return true;
        } else {
            return false;
        }
    }
}
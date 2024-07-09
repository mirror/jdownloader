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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.Time;
import org.appwork.utils.net.HTTPHeader;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.http.requests.GetRequest;
import jd.http.requests.PostRequest;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.GoFileIoCrawler;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "gofile.io" }, urls = { "" })
public class GofileIo extends PluginForHost {
    public GofileIo(PluginWrapper wrapper) {
        super(wrapper);
        this.setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "https://gofile.io/";
    }

    private String getFolderIDFromURL(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), "(?:c=|/d/)([A-Za-z0-9]+)").getMatch(0);
    }

    private String getShortFileIDFromURL(final DownloadLink link) throws PluginException {
        return new Regex(link.getPluginPatternMatcher(), "#file=([a-f0-9]+)").getMatch(0);
    }

    @Override
    public void init() {
        Browser.setRequestIntervalLimitGlobal(getHost(), 350);
    }

    /* Connection stuff */
    private static final int           FREE_MAXCHUNKS                                               = -2;
    private static final String        PROPERTY_DANGEROUS_FILE                                      = "dangerous_file";
    private static final String        PROPERTY_DIRECTURL                                           = "directurl";
    private static final String        PROPERTY_INTERNAL_FILEID                                     = "internal_fileid";
    private static final String        PROPERTY_PARENT_FOLDER_ID                                    = "parent_folder_id";
    public static final String         PROPERTY_PARENT_FOLDER_SHORT_ID                              = "parent_folder_short_id";
    private static final String        SETTING_ALLOW_DOWNLOAD_OF_FILES_FLAGGED_AS_MALICIOUS         = "allow_download_of_files_flagged_as_malicious";
    private static final boolean       default_SETTING_ALLOW_DOWNLOAD_OF_FILES_FLAGGED_AS_MALICIOUS = false;
    /* Don't touch the following! */
    private static final AtomicInteger freeRunning                                                  = new AtomicInteger(0);

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    /**
     * TODO: Implement official API once available: https://gofile.io/?t=api . The "API" used here is only their website.
     *
     * @throws Exception
     */
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return this.requestFileInformation(link, false);
    }

    protected static AtomicReference<String> TOKEN                   = new AtomicReference<String>();
    protected static AtomicLong              TOKEN_TIMESTAMP         = new AtomicLong(-1);
    protected final static long              TOKEN_EXPIRE            = 30 * 60 * 1000l;
    protected static AtomicReference<String> WEBSITE_TOKEN           = new AtomicReference<String>();
    protected static AtomicLong              WEBSITE_TOKEN_TIMESTAMP = new AtomicLong(-1);

    public static String getWebsiteToken(final Plugin plugin, final Browser br) throws Exception {
        synchronized (WEBSITE_TOKEN) {
            String token = WEBSITE_TOKEN.get();
            if (!StringUtils.isEmpty(token) && Time.systemIndependentCurrentJVMTimeMillis() - WEBSITE_TOKEN_TIMESTAMP.get() < TOKEN_EXPIRE) {
                return token;
            } else {
                final Browser brc = br.cloneBrowser();
                final GetRequest req = brc.createGetRequest("https://" + plugin.getHost() + "/dist/js/alljs.js");
                GofileIo.getPage(plugin, brc, req);
                token = brc.getRegex("websiteToken\\s*(?::|=)\\s*\"(.*?)\"").getMatch(0);
                if (token == null) {
                    /* 2024-01-26 */
                    token = brc.getRegex("fetchData\\.wt\\s*(?::|=)\\s*\"(.*?)\"").getMatch(0);
                    if (token == null) {
                        /* 2024-03-11 */
                        token = brc.getRegex("wt\\s*:\\s*\"([^\"]+)").getMatch(0);
                    }
                }
                if (StringUtils.isEmpty(token)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                WEBSITE_TOKEN.set(token);
                WEBSITE_TOKEN_TIMESTAMP.set(Time.systemIndependentCurrentJVMTimeMillis());
                return token;
            }
        }
    }

    public static Request getPage(final Plugin plugin, final Browser br, final Request request) throws IOException, PluginException, DecrypterRetryException {
        final int retrySeconds = 5;
        final int maxtries = 5;
        for (int i = 0; i <= maxtries; i++) {
            final URLConnectionAdapter con = br.openRequestConnection(request);
            try {
                if (con.getResponseCode() == 429) {
                    br.followConnection(true);
                    if (plugin instanceof PluginForDecrypt) {
                        final PluginForDecrypt pluginForDecrypt = (PluginForDecrypt) plugin;
                        pluginForDecrypt.sleep(retrySeconds * 1000, pluginForDecrypt.getCurrentLink().getCryptedLink());
                    } else {
                        Thread.sleep(retrySeconds * 1000);
                    }
                    request.resetConnection();
                    continue;
                } else if (con.getResponseCode() == 200) {
                    br.followConnection();
                    return request;
                } else {
                    br.followConnection(true);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            } catch (final InterruptedException e) {
                if (plugin instanceof PluginForHost) {
                    throw new PluginException(LinkStatus.ERROR_RETRY, null, e);
                } else {
                    throw new DecrypterRetryException(RetryReason.HOST_RATE_LIMIT, null, null, e);
                }
            } finally {
                con.disconnect();
            }
        }
        if (plugin instanceof PluginForHost) {
            throw new PluginException(LinkStatus.ERROR_RETRY);
        } else {
            throw new DecrypterRetryException(RetryReason.HOST_RATE_LIMIT);
        }
    }

    public static String getAndSetToken(final Plugin plugin, final Browser br) throws Exception {
        synchronized (TOKEN) {
            final String existingToken = TOKEN.get();
            String token = null;
            if (!StringUtils.isEmpty(existingToken) && Time.systemIndependentCurrentJVMTimeMillis() - TOKEN_TIMESTAMP.get() < TOKEN_EXPIRE) {
                /* Re-use existing token */
                token = existingToken;
            } else {
                final Browser brc = br.cloneBrowser();
                final boolean usePOSTRequest = true;
                if (usePOSTRequest) {
                    /* 2024-03-11: New */
                    final PostRequest req = brc.createJSonPostRequest("https://api." + plugin.getHost() + "/accounts", new HashMap<String, Object>());
                    req.getHeaders().put(new HTTPHeader(HTTPConstants.HEADER_REQUEST_ORIGIN, "https://" + plugin.getHost()));
                    req.getHeaders().put(new HTTPHeader(HTTPConstants.HEADER_REQUEST_REFERER, "https://" + plugin.getHost()));
                    GofileIo.getPage(plugin, brc, req);
                } else {
                    final GetRequest req = brc.createGetRequest("https://api." + plugin.getHost() + "/createAccount");
                    req.getHeaders().put(new HTTPHeader(HTTPConstants.HEADER_REQUEST_ORIGIN, "https://" + plugin.getHost()));
                    req.getHeaders().put(new HTTPHeader(HTTPConstants.HEADER_REQUEST_REFERER, "https://" + plugin.getHost()));
                    GofileIo.getPage(plugin, brc, req);
                }
                final Map<String, Object> response = JSonStorage.restoreFromString(brc.getRequest().getHtmlCode(), TypeRef.MAP);
                if (!"ok".equalsIgnoreCase(response.get("status").toString())) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                token = JavaScriptEngineFactory.walkJson(response, "data/token").toString();
                if (StringUtils.isEmpty(token)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                TOKEN.set(token);
                TOKEN_TIMESTAMP.set(Time.systemIndependentCurrentJVMTimeMillis());
            }
            br.setCookie(plugin.getHost(), "accountToken", token);
            return token;
        }
    }

    @Override
    public String getPluginContentURL(final DownloadLink link) {
        final String parentFolderShortID = link.getStringProperty(PROPERTY_PARENT_FOLDER_SHORT_ID);
        final String parentFolderID = link.getStringProperty(PROPERTY_PARENT_FOLDER_ID);
        if (parentFolderShortID != null) {
            return "https://" + getHost() + "/d/" + parentFolderShortID;
        } else if (parentFolderID != null) {
            /* Link to next folder which contains this file */
            return "https://" + getHost() + "/d/" + parentFolderID;
        } else {
            return super.getPluginContentURL(link);
        }
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        /* 2021-11-30: Token cookie is even needed to check directURLs! */
        getAndSetToken(this, br);
        final boolean allowDirecturlLinkcheck = true;
        if (allowDirecturlLinkcheck && this.checkDirectLink(link, PROPERTY_DIRECTURL) != null) {
            logger.info("Availablecheck via directurl complete");
            return AvailableStatus.TRUE;
        }
        final String folderID = getFolderIDFromURL(link);
        if (folderID == null) {
            /* This should never happen! */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* Re-crawl folder item in order to obtain a fresh direct-downloadable URL. */
        final GoFileIoCrawler crawler = (GoFileIoCrawler) this.getNewPluginForDecryptInstance(this.getHost());
        final CryptedLink cl = new CryptedLink(this.getPluginContentURL(link));
        /* Make sure that user is not asked for password again if we already know it. */
        cl.setDecrypterPassword(link.getDownloadPassword());
        final ArrayList<DownloadLink> crawlerResults = crawler.decryptIt(cl, null);
        DownloadLink freshLink = null;
        for (final DownloadLink result : crawlerResults) {
            if (StringUtils.equals(this.getLinkID(link), this.getLinkID(result))) {
                freshLink = result;
                break;
            }
        }
        if (freshLink == null) {
            /* File must have been deleted from folder */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* Set property of fresh directurl. */
        link.setProperty(PROPERTY_DIRECTURL, freshLink.getProperty(PROPERTY_DIRECTURL));
        /* Set password we got from our crawler results just in case it has changed. */
        if (freshLink.getDownloadPassword() != null) {
            link.setDownloadPassword(freshLink.getDownloadPassword());
        }
        return AvailableStatus.TRUE;
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (!this.looksLikeDownloadableContent(con)) {
                    throw new IOException();
                }
                if (con.getCompleteContentLength() > 0) {
                    if (con.isContentDecoded()) {
                        link.setDownloadSize(con.getCompleteContentLength());
                    } else {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                }
                final String serverFilename = Plugin.getFileNameFromDispositionHeader(con);
                if (serverFilename != null) {
                    link.setFinalFileName(serverFilename);
                }
                return dllink;
            } catch (final Exception e) {
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

    public static void parseFileInfo(final DownloadLink link, final Map<String, Object> entry) {
        // link, download?
        String downloadURL = (String) entry.get("link");
        if (StringUtils.isEmpty(downloadURL)) {
            // directLink, streaming?
            downloadURL = (String) entry.get("directLink");
        }
        final long size = JavaScriptEngineFactory.toLong(entry.get("size"), -1);
        if (size > 0) {
            link.setVerifiedFileSize(size);
        }
        final String name = (String) entry.get("name");
        final String md5 = (String) entry.get("md5");
        final String description = (String) entry.get("description");
        if (!StringUtils.isEmpty(name)) {
            link.setFinalFileName(name);
        }
        if (!StringUtils.isEmpty(md5)) {
            link.setMD5Hash(md5);
        }
        if (StringUtils.isEmpty(link.getComment()) && !StringUtils.isEmpty(description)) {
            link.setComment(description);
        }
        /*
         * 2021-03-30: Check if the file contains malicious software according to their system. We could still download it but it's
         * impossible via website so let's not do it either.
         */
        final List<Object> dangers = (List<Object>) entry.get("v" + "i" + "ruses");
        if (dangers != null && !dangers.isEmpty()) {
            link.setProperty(PROPERTY_DANGEROUS_FILE, true);
        } else {
            link.removeProperty(PROPERTY_DANGEROUS_FILE);
        }
        if (!StringUtils.isEmpty(downloadURL)) {
            link.setProperty(PROPERTY_DIRECTURL, downloadURL);
        }
        link.setProperty(PROPERTY_INTERNAL_FILEID, entry.get("id"));
        link.setProperty(PROPERTY_PARENT_FOLDER_ID, entry.get("parentFolder"));
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link);
    }

    private void handleDownload(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link, true);
        final boolean isDangerousFile = link.getBooleanProperty(PROPERTY_DANGEROUS_FILE, false);
        final String directurl = link.getStringProperty(PROPERTY_DIRECTURL, null);
        if (isDangerousFile && !this.getPluginConfig().getBooleanProperty(SETTING_ALLOW_DOWNLOAD_OF_FILES_FLAGGED_AS_MALICIOUS, default_SETTING_ALLOW_DOWNLOAD_OF_FILES_FLAGGED_AS_MALICIOUS)) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "This file was flagged as to contain malicious software by " + this.getHost() + "!");
        } else if (StringUtils.isEmpty(directurl)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, directurl, this.isResumeable(link, null), FREE_MAXCHUNKS);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (br.containsHTML("Server is currently overloaded.\\s*Premium accounts have priority access.\\s*Please upgrade or retry later.")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server is currently overloaded. Premium accounts have priority access. Please upgrade or retry later.", 30 * 60 * 1000l);
            } else if (br.getURL().matches("(?i)https?://[^/]+/d/[a-f0-9\\-]+")) {
                /* Redirect to main/folder URL -> Most likely directurl expired */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error: Directurl expired?", 3 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        /* Add a download slot */
        controlMaxFreeDownloads(null, link, +1);
        try {
            /* Start download */
            dl.startDownload();
        } finally {
            /* Remove download slot */
            controlMaxFreeDownloads(null, link, -1);
        }
    }

    protected void controlMaxFreeDownloads(final Account account, final DownloadLink link, final int num) {
        if (account == null) {
            synchronized (freeRunning) {
                final int before = freeRunning.get();
                final int after = before + num;
                freeRunning.set(after);
                logger.info("freeRunning(" + link.getName() + ")|max:" + getMaxSimultanFreeDownloadNum() + "|before:" + before + "|after:" + after + "|num:" + num);
            }
        }
    }

    @Override
    public boolean looksLikeDownloadableContent(final URLConnectionAdapter urlConnection) {
        /* 2021-07-05: Override to allow 0-byte-files. */
        return urlConnection != null && (urlConnection.getResponseCode() == 200 || urlConnection.getResponseCode() == 206) && urlConnection.isContentDisposition();
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return false;
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SETTING_ALLOW_DOWNLOAD_OF_FILES_FLAGGED_AS_MALICIOUS, "Allow download of files flagged as 'malicious' by gofile.io?").setDefaultValue(default_SETTING_ALLOW_DOWNLOAD_OF_FILES_FLAGGED_AS_MALICIOUS));
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return freeRunning.get() + 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.Time;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.http.requests.GetRequest;
import jd.nutils.JDHash;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.UserAgents;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "gofile.io" }, urls = { "https?://(?:www\\.)?gofile\\.io/(?:\\?c=|d/)[A-Za-z0-9]+(?:#file=[a-f0-9]+)?" })
public class GofileIo extends PluginForHost {
    public GofileIo(PluginWrapper wrapper) {
        super(wrapper);
        this.setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "https://gofile.io/";
    }

    private String getFolderID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), "(?:c=|/d/)([A-Za-z0-9]+)").getMatch(0);
    }

    private String getFileID(final DownloadLink link) throws PluginException {
        return new Regex(link.getPluginPatternMatcher(), "#file=([a-f0-9]+)").getMatch(0);
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME                                                  = true;
    private static final int     FREE_MAXCHUNKS                                               = -2;
    private static final int     FREE_MAXDOWNLOADS                                            = -1;
    private static final String  PROPERTY_DANGEROUS_FILE                                      = "dangerous_file";
    private static final String  PROPERTY_DIRECTURL                                           = "directurl";
    private static final String  PROPERTY_INTERNAL_FILEID                                     = "internal_fileid";
    private static final String  SETTING_ALLOW_DOWNLOAD_OF_FILES_FLAGGED_AS_MALICIOUS         = "allow_download_of_files_flagged_as_malicious";
    private static final boolean default_SETTING_ALLOW_DOWNLOAD_OF_FILES_FLAGGED_AS_MALICIOUS = false;

    /** TODO: Implement official API once available: https://gofile.io/?t=api . The "API" used here is only their website. */
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        return this.requestFileInformation(link, false);
    }

    protected static AtomicReference<String> TOKEN           = new AtomicReference<String>();
    protected static AtomicLong              TOKEN_TIMESTAMP = new AtomicLong(-1);
    protected final static long              TOKEN_EXPIRE    = 30 * 60 * 1000l;

    public static String getToken(final Plugin plugin, final Browser br) throws IOException, PluginException {
        synchronized (TOKEN) {
            String token = TOKEN.get();
            if (!StringUtils.isEmpty(token) && Time.systemIndependentCurrentJVMTimeMillis() - TOKEN_TIMESTAMP.get() < TOKEN_EXPIRE) {
                return token;
            } else {
                final Browser brc = br.cloneBrowser();
                final GetRequest req = brc.createGetRequest("https://api." + plugin.getHost() + "/createAccount");
                req.getHeaders().put(new HTTPHeader(HTTPConstants.HEADER_REQUEST_ORIGIN, "https://gofile.io"));
                req.getHeaders().put(new HTTPHeader(HTTPConstants.HEADER_REQUEST_REFERER, "https://gofile.io"));
                brc.getPage(req);
                final HashMap<String, Object> response = JSonStorage.restoreFromString(brc.toString(), TypeRef.HASHMAP);
                if ("ok".equals(response.get("status"))) {
                    token = (String) JavaScriptEngineFactory.walkJson(response, "data/token");
                    if (StringUtils.isEmpty(token)) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    } else {
                        TOKEN.set(token);
                        TOKEN_TIMESTAMP.set(Time.systemIndependentCurrentJVMTimeMillis());
                        return token;
                    }
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        /* 2021-11-30: Token cookie is even needed to check directURLs! */
        final String token = getToken(this, this.br);
        br.setCookie(this.getHost(), "accountToken", token);
        final boolean allowDirecturlLinkcheck = true;
        if (allowDirecturlLinkcheck && this.checkDirectLink(link, PROPERTY_DIRECTURL) != null) {
            logger.info("Availablecheck via directurl complete");
            return AvailableStatus.TRUE;
        }
        final String folderID = getFolderID(link);
        if (folderID == null) {
            /* This should never happen! */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* 2020-08-20: Avoid blocks by user-agent - this is just a test based on a weak assumption, it is not necessarily! */
        br.getHeaders().put("User-Agent", UserAgents.stringUserAgent());
        /* 2020-08-20: Slow servers, timeouts will often occur --> Try a higher readtimeout */
        br.setReadTimeout(2 * 60 * 1000);
        final UrlQuery query = new UrlQuery();
        query.add("contentId", folderID);
        query.add("websiteToken", "websiteToken");
        query.add("cache", "true");
        String passCode = null;
        boolean passwordCorrect = true;
        boolean passwordRequired = false;
        int attempt = 0;
        final Browser brc = br.cloneBrowser();
        Map<String, Object> response = null;
        do {
            if (passwordRequired) {
                passCode = getUserInput("Password?", link);
                query.addAndReplace("password", JDHash.getSHA256(passCode));
            } else if (link.getDownloadPassword() != null) {
                /* E.g. first try and password is available from when user added folder via crawler. */
                query.addAndReplace("password", JDHash.getSHA256(link.getDownloadPassword()));
            }
            final GetRequest req = br.createGetRequest("https://api." + this.getHost() + "/getContent?" + query.toString() + "&token=" + token);
            req.getHeaders().put(new HTTPHeader(HTTPConstants.HEADER_REQUEST_ORIGIN, "https://gofile.io"));
            req.getHeaders().put(new HTTPHeader(HTTPConstants.HEADER_REQUEST_REFERER, "https://gofile.io"));
            brc.getPage(req);
            response = JSonStorage.restoreFromString(brc.toString(), TypeRef.HASHMAP);
            if ("error-passwordRequired".equals(response.get("status")) || "error-passwordWrong".equals(response.get("status"))) {
                if (!isDownload) {
                    /*
                     * Do not ask for passwords during linkcheck! Also we now know that the folder is online but we can't know if the file
                     * we want still exists!
                     */
                    return AvailableStatus.UNCHECKABLE;
                }
                passwordRequired = true;
                passwordCorrect = false;
                attempt += 1;
                if (attempt >= 3) {
                    break;
                } else {
                    continue;
                }
            } else {
                passwordCorrect = true;
                break;
            }
        } while (!this.isAbort());
        if (passwordRequired && !passwordCorrect) {
            throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
        }
        /* Save for the next time. */
        if (passCode != null) {
            link.setDownloadPassword(passCode);
        }
        if ("ok".equals(response.get("status"))) {
            /*
             * fileID is needed to find the correct files if multiple ones are in a 'folder'. If this is not available we most likely only
             * have a single file.
             */
            final String internalFileID = link.getStringProperty(PROPERTY_INTERNAL_FILEID);
            if (internalFileID == null) {
                /* This should never happen */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> data = (Map<String, Object>) response.get("data");
            final Map<String, Map<String, Object>> files = (Map<String, Map<String, Object>>) data.get("contents");
            for (Entry<String, Map<String, Object>> file : files.entrySet()) {
                final String id = file.getKey();
                if (internalFileID != null && id.toString().equals(internalFileID)) {
                    final Map<String, Object> entry = file.getValue();
                    parseFileInfo(link, entry);
                    return AvailableStatus.TRUE;
                }
            }
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    final String serverFilename = Plugin.getFileNameFromDispositionHeader(con);
                    if (serverFilename != null) {
                        link.setFinalFileName(serverFilename);
                    }
                    return dllink;
                } else {
                    throw new IOException();
                }
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
        link.setProperty(PROPERTY_INTERNAL_FILEID, entry.get("id").toString());
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link);
    }

    private void handleDownload(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link, true);
        final boolean isDangerousFile = link.getBooleanProperty(PROPERTY_DANGEROUS_FILE, false);
        final String downloadURL = link.getStringProperty(PROPERTY_DIRECTURL, null);
        if (isDangerousFile && !this.getPluginConfig().getBooleanProperty(SETTING_ALLOW_DOWNLOAD_OF_FILES_FLAGGED_AS_MALICIOUS, default_SETTING_ALLOW_DOWNLOAD_OF_FILES_FLAGGED_AS_MALICIOUS)) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "This file was flagged as to contain malicious software by " + this.getHost() + "!");
        } else if (StringUtils.isEmpty(downloadURL)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, downloadURL, FREE_RESUME, FREE_MAXCHUNKS);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            if (br.getURL().matches("(?i)https?://[^/]+/d/[a-f0-9\\-]+")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error: Directurl expired?", 3 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl.startDownload();
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
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
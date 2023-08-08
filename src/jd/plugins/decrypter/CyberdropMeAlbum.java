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
package jd.plugins.decrypter;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.controller.UpdateRequiredClassNotFoundException;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.http.requests.GetRequest;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class CyberdropMeAlbum extends PluginForDecrypt {
    public CyberdropMeAlbum(PluginWrapper wrapper) {
        super(wrapper);
    }

    public final static String MAIN_CYBERDROP_DOMAIN = "cyberdrop.me";

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { MAIN_CYBERDROP_DOMAIN, "cyberdrop.to", "cyberdrop.cc" });
        return ret;
    }

    public static List<String> getDeadDomains() {
        return null;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    private static final String EXTENSIONS = "(?i)(?:mp4|m4v|mp3|mov|jpe?g|zip|rar|png|gif|ts|[a-z0-9]{3})";

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            String regex = "https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/a/[A-Za-z0-9]+";
            regex += "|https?://fs-\\d+\\." + buildHostsPatternPart(domains) + "/.+\\." + EXTENSIONS;// TYPE_FS
            ret.add(regex);
        }
        return ret.toArray(new String[0]);
    }

    public static final String TYPE_ALBUM = "(?i)https?://[^/]+/a/([A-Za-z0-9]+)";                      // album
    public static final String TYPE_FS    = "(?i)https?://fs-(\\d+)\\.[^/]+/(.+\\." + EXTENSIONS + ")"; // cyberdrop
    private PluginForHost      plugin     = null;

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String singleFileURL = isSingleMediaURL(param.getCryptedUrl());
        if (singleFileURL != null) {
            /* Direct downloadable URL. */
            add(ret, null, param.getCryptedUrl(), null, null, null, false);
        } else {
            /* Most likely we have an album or similar: One URL which leads to more URLs. */
            String contentURL = param.getCryptedUrl();
            final String hostFromAddedURL = new URL(contentURL).getHost();
            final List<String> deadDomains = getDeadDomains();
            if (deadDomains != null && deadDomains.size() > 0) {
                for (final String deadHost : deadDomains) {
                    if (StringUtils.equalsIgnoreCase(hostFromAddedURL, deadHost) || StringUtils.equalsIgnoreCase(hostFromAddedURL, "www." + deadHost)) {
                        final String newHost = getHost();
                        contentURL = param.getCryptedUrl().replaceFirst(Pattern.quote(hostFromAddedURL) + "/", newHost + "/");
                        logger.info("Corrected domain in added URL: " + hostFromAddedURL + " --> " + newHost);
                        break;
                    }
                }
            }
            final HashSet<String> dups = new HashSet<String>();
            br.setFollowRedirects(true);
            /* Double-check if we got a direct-URL. */
            final GetRequest getRequest = br.createGetRequest(contentURL);
            final URLConnectionAdapter con = this.br.openRequestConnection(getRequest);
            try {
                if (this.looksLikeDownloadableContent(con)) {
                    add(ret, dups, contentURL, getFileNameFromHeader(con), con.getLongContentLength() > 0 ? String.valueOf(con.getLongContentLength()) : null, null, true);
                    return ret;
                } else {
                    br.followConnection();
                }
            } finally {
                con.disconnect();
            }
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            /* Double-check for offline / empty album. */
            final String albumID = new Regex(br.getURL(), TYPE_ALBUM).getMatch(0);
            final String buildID = br.getRegex("\"buildId\"\\s*:\\s*\"([^\"]+)").getMatch(0);
            if (albumID != null && buildID != null) {
                final Browser brc = br.cloneBrowser();
                brc.getPage("/_next/data/" + Encoding.urlEncode(buildID) + "/a/" + albumID + ".json");
                if (brc.getHttpConnection().getResponseCode() == 404) {
                    /* {"notFound":true} */
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            }
            final String albumjs = br.getRegex("const albumData\\s*=\\s*(\\{.*?\\})").getMatch(0);
            String albumTitle = new Regex(albumjs, "name\\s*:\\s*'([^\\']+)'").getMatch(0);
            if (albumTitle != null) {
                albumTitle = Encoding.htmlDecode(albumTitle).trim();
            } else {
                /* Fallback */
                albumTitle = albumID;
            }
            final String json = br.getRegex("dynamicEl\\s*:\\s*(\\[\\s*\\{.*?\\])").getMatch(0);
            if (json != null) {
                /* gallery mode only works for images */
                final List<Map<String, Object>> ressourcelist = (List<Map<String, Object>>) JavaScriptEngineFactory.jsonToJavaObject(json);
                for (final Map<String, Object> photo : ressourcelist) {
                    final String downloadUrl = (String) photo.get("downloadUrl");
                    final String subHtml = (String) photo.get("subHtml");
                    final String filesizeStr = new Regex(subHtml, "(\\d+(\\.\\d+)? [A-Za-z]{2,5})$").getMatch(0);
                    add(ret, dups, downloadUrl, null, null, filesizeStr, true);
                }
            }
            final String[] htmls = br.getRegex("<div class=\"image-container column\"[^>]*>(.*?)/p>\\s*</div>").getColumn(0);
            for (final String html : htmls) {
                final String filename = parseMediaFilename(br, html);
                final String directurl = parseMediaURL(br, html);
                if (directurl != null) {
                    final String filesizeBytes = new Regex(html, "class=\"(?:is-hidden)?\\s*file-size\"[^>]*>\\s*(\\d+) B").getMatch(0);
                    final String filesize = new Regex(html, "class=\"(?:is-hidden)?\\s*file-size\"[^>]*>\\s*([0-9\\.]+\\s+[MKG]B)").getMatch(0);
                    add(ret, dups, directurl, filename, filesizeBytes, filesize, true);
                }
            }
            if (ret.isEmpty()) {
                if (br.containsHTML("(?i)>\\s*0 files\\s*<") || br.containsHTML("(?i)0 files \\(0 Bytes\\)\\s*<") || br.containsHTML("(?i)There are no files in the album")) {
                    throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            if (albumTitle != null && albumID != null) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(albumTitle);
                fp.setAllowInheritance(true);
                fp.addLinks(ret);
            }
        }
        return ret;
    }

    private DownloadLink add(final List<DownloadLink> ret, Set<String> dups, String directurl, String filename, final String filesizeBytes, final String filesize, final boolean setOnlineStatus) throws Exception {
        if (dups == null || dups.add(directurl)) {
            // bunkr, html encoding in filename and directurl
            filename = Encoding.htmlOnlyDecode(filename);
            directurl = Encoding.htmlOnlyDecode(directurl);
            final String directURL = isSingleMediaURL(directurl);
            final DownloadLink dl;
            if (directURL != null) {
                dl = this.createDownloadlink(directURL);
            } else {
                dl = this.createDownloadlink(directurl);
            }
            dl.setProperty(DirectHTTP.PROPERTY_RATE_LIMIT, 500);
            dl.setProperty(DirectHTTP.PROPERTY_MAX_CONCURRENT, 1);
            if (plugin == null) {
                try {
                    final LazyHostPlugin lazyHostPlugin = HostPluginController.getInstance().get(getHost());
                    if (lazyHostPlugin == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    } else {
                        plugin = lazyHostPlugin.getPrototype(null, false);
                    }
                } catch (UpdateRequiredClassNotFoundException e) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, null, e);
                }
            }
            if (setOnlineStatus) {
                dl.setAvailable(true);
            }
            if (directURL != null) {
                // direct assign the dedicated hoster plugin because it does not have any URL regex
                dl.setDefaultPlugin(plugin);
            } else if (directurl.matches(TYPE_FS)) {
                /* reset AvailableStatus to allow reprocessing through decrypter to find final URL */
                /* see LinkCrawler.breakPluginForDecryptLoop */
                dl.setAvailableStatus(AvailableStatus.UNCHECKED);
            }
            if (filename != null) {
                dl.setFinalFileName(filename);
            }
            if (filesizeBytes != null) {
                dl.setVerifiedFileSize(Long.parseLong(filesizeBytes));
            } else if (filesize != null) {
                dl.setDownloadSize(SizeFormatter.getSize(filesize));
            }
            ret.add(dl);
            return dl;
        } else {
            return null;
        }
    }

    private String parseMediaFilename(Browser br, String html) {
        String filename = new Regex(html, "target\\s*=\\s*\"_blank\"\\s*title\\s*=\\s*\"([^<>\"]+)\"").getMatch(0);
        if (filename == null) {
            // bunkr.is
            filename = new Regex(html, "<p\\s*class\\s*=\\s*\"name\"\\s*>\\s*(.*?)\\s*<").getMatch(0);
            if (filename == null) {
                filename = new Regex(html, "<div\\s*class\\s*=\\s*\"[^\"]*details\"\\s*>\\s*<p\\s*class[^>]*>\\s*(.*?)\\s*<").getMatch(0);
            }
        }
        return filename;
    }

    private String parseMediaURL(final Browser br, final String html) throws IOException {
        String directurl = new Regex(html, "href\\s*=\\s*\"(https?://[^\"]+)\"").getMatch(0);
        if (directurl == null) {
            directurl = new Regex(html, "href\\s*=\\s*\"(/(?:d|v)/[^\"]+)\"").getMatch(0);
            if (directurl != null) {
                directurl = br.getURL(directurl).toExternalForm();
            }
        }
        return directurl;
    }

    /**
     * Returns URL if given URL looks like it is pointing to a single file. </br>
     * Returns null if given URL-structure is unknown or does not seem to point to a single file.
     */
    private String isSingleMediaURL(final String url) {
        if (url == null) {
            return null;
        } else if (url.matches(TYPE_FS)) {
            // cyberdrop
            return url;
        } else {
            /* Unknown URL */
            return null;
        }
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }
}

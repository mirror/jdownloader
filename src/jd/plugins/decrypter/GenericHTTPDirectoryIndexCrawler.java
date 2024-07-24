package jd.plugins.decrypter;

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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.abstractGenericHTTPDirectoryIndexCrawler;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.http.requests.GetRequest;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "HTTPDirectoryCrawler" }, urls = { "jd://directoryindex://.+" })
public class GenericHTTPDirectoryIndexCrawler extends abstractGenericHTTPDirectoryIndexCrawler {
    private enum DirectoryListingMode {
        NGINX,
        APACHE,
        CADDY,
        LIGHTTPD
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.GENERIC };
    }

    public GenericHTTPDirectoryIndexCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        return this.crawlHTTPDirectory(param);
    }

    protected ArrayList<DownloadLink> crawlHTTPDirectory(final CryptedLink param) throws IOException, PluginException, DecrypterRetryException {
        /* First check if maybe the user has added a directURL. */
        final String url = param.getCryptedUrl().replaceFirst("(?i)^jd://directoryindex://", "");
        final GetRequest getRequest = br.createGetRequest(url);
        final URLConnectionAdapter con = this.br.openRequestConnection(getRequest);
        try {
            if (this.looksLikeDownloadableContent(con)) {
                final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
                final DownloadLink direct = getCrawler().createDirectHTTPDownloadLink(getRequest, con);
                final String pathToFile = getCurrentDirectoryPath(url);
                /* Set relative path if one is available. */
                if (pathToFile.contains("/")) {
                    final String pathToFolder = pathToFile.substring(0, pathToFile.lastIndexOf("/"));
                    direct.setRelativeDownloadFolderPath(pathToFolder);
                }
                ret.add(direct);
                return ret;
            } else {
                br.followConnection();
                if (br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else if (!con.isOK()) {
                    /* E.g. response 403 */
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    return this.parseHTTPDirectory(param, br);
                }
            }
        } finally {
            con.disconnect();
        }
    }

    protected ArrayList<DownloadLink> parseLighttpd(final CryptedLink param, final Browser br) throws IOException, PluginException, DecrypterRetryException {
        final String path = this.getCurrentDirectoryPath(br);
        if (path == null) {
            // fast return
            return null;
        }
        final String[] fileEntries = br.getRegex("<tr(.*?)</tr>").getColumn(0);
        if (fileEntries != null && fileEntries.length > 0) {
            final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
            for (String fileEntry : fileEntries) {
                String name = new Regex(fileEntry, "<a[^>]*href\\s*=\\s*\"(.*?)\"").getMatch(0);
                String size = new Regex(fileEntry, "class\\s*=\\s*\"s\"[^>]*>\\s*([0-9\\.KMGTB]*?|-\\s*&nbsp;)\\s*<").getMatch(0);
                final String lastModified = new Regex(fileEntry, "class\\s*=\\s*\"m\"[^>]*>\\s*([a-zA-Z0-9 :-]+)\\s*<").getMatch(0);
                final String type = new Regex(fileEntry, "class\\s*=\\s*\"t\"[^>]*>\\s*(.*?)\\s*<").getMatch(0);
                if (!StringUtils.isAllNotEmpty(name, size, lastModified, type)) {
                    // no valid index entry
                    continue;
                } else if (name.startsWith("../")) {
                    continue;
                }
                if ("- &nbsp;".equals(size)) {
                    size = "-";
                }
                if ("Directory".equalsIgnoreCase(type)) {
                    size = "-";
                    if (name.endsWith("/")) {
                        name = name.substring(0, name.length() - 1);
                    }
                }
                final DownloadLink link = parseEntry(DirectoryListingMode.LIGHTTPD, br, new String[] { name, size });
                link.setRelativeDownloadFolderPath(path);
                ret.add(link);
            }
            if (!path.equals("/")) {
                final FilePackage fp = FilePackage.getInstance();
                fp.addLinks(ret);
                fp.setName(path);
            }
            if (ret.size() == 0) {
                return null;
            } else {
                return ret;
            }
        }
        return null;
    }

    protected ArrayList<DownloadLink> parseNginx(final CryptedLink param, final Browser br) throws IOException, PluginException, DecrypterRetryException {
        final String path = this.getCurrentDirectoryPath(br);
        if (path == null) {
            // fast return
            return null;
        }
        final String[][] filesAndFolders = br.getRegex("<a href=\"([^\"]+)\">([^>]+)</a>(?:\\s*[a-zA-Z0-9 :-]+)\\s*(\\d+|-)").getMatches();
        if (filesAndFolders != null && filesAndFolders.length > 0) {
            final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
            for (final String[] finfo : filesAndFolders) {
                if (finfo[0].endsWith("/")) {
                    finfo[2] = "-";
                }
                final DownloadLink link = parseEntry(DirectoryListingMode.NGINX, br, finfo);
                link.setRelativeDownloadFolderPath(path);
                ret.add(link);
            }
            if (!path.equals("/")) {
                final FilePackage fp = FilePackage.getInstance();
                fp.addLinks(ret);
                fp.setName(path);
            }
            if (ret.size() == 0) {
                return null;
            } else {
                return ret;
            }
        }
        return null;
    }

    protected ArrayList<DownloadLink> parseCaddy(final CryptedLink param, final Browser br) throws IOException, PluginException, DecrypterRetryException {
        final String path = br.getRegex(">\\s*Folder Path\\s*</div>\\s*<h\\d>\\s*<a[^>]*href\\s*=\\s*\"(.*?)\"[^>]*>\\s*(/.*?)\\s*<").getMatch(1);
        if (path == null) {
            // fast return
            return null;
        }
        final String[] fileEntries = br.getRegex("<tr\\s*class\\s*=\\s*\"file\"[^>]*>\\s*(.*?)\\s*</tr>").getColumn(0);
        if (fileEntries != null && fileEntries.length > 0) {
            final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
            for (String fileEntry : fileEntries) {
                String name = new Regex(fileEntry, "<span\\s*class\\s*=\\s*\"name\"[^>]*>\\s*(.*?)\\s*</span>").getMatch(0);
                String size = new Regex(fileEntry, "<td\\s*class\\s*=\\s*\"size\"[^>]*data-size\\s*=\\s*\"(\\d+)\"").getMatch(0);
                if (size == null) {
                    size = new Regex(fileEntry, "<td>\\s*(&mdash;)\\s*</td>").getMatch(0);
                }
                final String timestamp = new Regex(fileEntry, "<td\\s*class\\s*=\\s*\"timestamp[^\"]*\"[^>]*>\\s*<time\\s*(.*?)\\s*</time>\\s*</td").getMatch(0);
                if (!StringUtils.isAllNotEmpty(name, size, timestamp)) {
                    continue;
                }
                if ("&mdash;".equals(size)) {
                    size = "-";
                }
                if (name.endsWith("/")) {
                    size = "-";
                    name = name.substring(0, name.length() - 1);
                }
                final DownloadLink link = parseEntry(DirectoryListingMode.CADDY, br, new String[] { name, size });
                link.setRelativeDownloadFolderPath(path);
                ret.add(link);
            }
            if (!path.equals("/")) {
                final FilePackage fp = FilePackage.getInstance();
                fp.addLinks(ret);
                fp.setName(path);
            }
            if (ret.size() == 0) {
                return null;
            } else {
                return ret;
            }
        }
        return null;
    }

    protected ArrayList<DownloadLink> parseApache(final CryptedLink param, final Browser br) throws IOException, PluginException, DecrypterRetryException {
        final String path = this.getCurrentDirectoryPath(br);
        if (path == null) {
            // fast return
            return null;
        }
        /* nginx (default?): Entries sometimes contain the create-date, sometimes only the filesize (for folders, only "-"). */
        final String[][] filesAndFolders = br.getRegex("<a href=\"([^\"]+)\">[^<]*</a>\\s*</td><td align=\"right\">\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}\\s*</td><td align=\"right\">[ ]*([0-9\\.]*?[BMGKT]?|-)\\s*</td>").getMatches();
        if (filesAndFolders != null && filesAndFolders.length > 0) {
            final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
            for (final String[] finfo : filesAndFolders) {
                if (!StringUtils.isAllNotEmpty(finfo)) {
                    // no valid entry
                    continue;
                } else if (finfo[0].startsWith("../")) {
                    continue;
                }
                final DownloadLink link = parseEntry(DirectoryListingMode.APACHE, br, finfo);
                link.setRelativeDownloadFolderPath(path);
                ret.add(link);
            }
            if (!path.equals("/")) {
                final FilePackage fp = FilePackage.getInstance();
                fp.addLinks(ret);
                fp.setName(path);
            }
            if (ret.size() == 0) {
                return null;
            } else {
                return ret;
            }
        }
        return null;
    }

    /**
     * Does parsing only, without any HTTP requests!
     *
     * @throws DecrypterRetryException
     */
    public ArrayList<DownloadLink> parseHTTPDirectory(final CryptedLink param, final Browser br) throws IOException, PluginException, DecrypterRetryException {
        ArrayList<DownloadLink> ret = parseApache(param, br);
        if (ret == null) {
            ret = parseLighttpd(param, br);
        }
        if (ret == null) {
            ret = parseNginx(param, br);
        }
        if (ret == null) {
            ret = parseCaddy(param, br);
        }
        if (ret == null) {
            // this plugin should not fail
            ret = new ArrayList<DownloadLink>();
        }
        return ret;
    }

    protected DownloadLink parseEntry(final DirectoryListingMode directoryListing, final Browser br, final String finfo[]) throws PluginException, IOException {
        final String url = br.getURL(finfo[0]).toString();
        final String filesizeStr;
        switch (directoryListing) {
        case APACHE:
            filesizeStr = finfo[1];
            break;
        case NGINX:
            filesizeStr = finfo[2];
            break;
        case CADDY:
            filesizeStr = finfo[1];
            break;
        case LIGHTTPD:
            filesizeStr = finfo[1];
            break;
        default:
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unsupported:" + directoryListing);
        }
        /* Is it a file or a folder? */
        if (filesizeStr.equals("-")) {
            /* Folder -> Will go back into this crawler */
            final DownloadLink dlfolder = this.createDownloadlink("jd://directoryindex://" + url);
            return dlfolder;
        } else {
            /* File */
            final DownloadLink dlfile = new DownloadLink(null, null, "DirectHTTP", DirectHTTP.createURLForThisPlugin(url), true);
            /* Obtain filename from URL as displayed name may be truncated. */
            String name = url.substring(url.lastIndexOf("/") + 1);
            if (Encoding.isUrlCoded(name)) {
                name = Encoding.htmlDecode(name);
            }
            dlfile.setName(name);
            dlfile.setProperty(DirectHTTP.TRY_ALL, Boolean.TRUE);
            final long fileSize = SizeFormatter.getSize(filesizeStr + "b");
            switch (directoryListing) {
            case CADDY:
                if (filesizeStr.matches("^\\d+$")) {
                    dlfile.setVerifiedFileSize(fileSize);
                } else {
                    dlfile.setDownloadSize(fileSize);
                }
                break;
            case NGINX:
                if (filesizeStr.matches("^\\d+$")) {
                    dlfile.setVerifiedFileSize(fileSize);
                } else {
                    dlfile.setDownloadSize(fileSize);
                }
                break;
            default:
                dlfile.setDownloadSize(fileSize);
                break;
            }
            dlfile.setAvailable(true);
            return dlfile;
        }
    }

    /** Returns url-decoded directory path. */
    protected String getCurrentDirectoryPath(final Browser br) {
        String path = br.getRegex("(?i)<(?:title|h\\d)>\\s*Index of\\s*(/[^<]*)\\s*</(?:title|h\\d)>").getMatch(0);
        if (path == null) {
            return null;
        } else {
            if (Encoding.isUrlCoded(path)) {
                path = Encoding.htmlDecode(path);
            }
            return path;
        }
    }

    /**
     * Returns url-DECODED path based on given url.
     *
     * @throws UnsupportedEncodingException
     */
    protected String getCurrentDirectoryPath(final String url) throws UnsupportedEncodingException {
        final String path = new Regex(url, "(?i)^https?://[^/]+(.+)").getMatch(0);
        return URLDecoder.decode(path, "UTF-8");
    }
}

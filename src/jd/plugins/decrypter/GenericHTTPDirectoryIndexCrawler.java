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
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.hoster.DirectHTTP;

import org.appwork.utils.Regex;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.abstractGenericHTTPDirectoryIndexCrawler;
import org.jdownloader.plugins.controller.LazyPlugin;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "HTTPDirectoryCrawler" }, urls = { "jd://directoryindex://.+" })
public class GenericHTTPDirectoryIndexCrawler extends abstractGenericHTTPDirectoryIndexCrawler {
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

    /**
     * Does parsing only, without any HTTP requests!
     *
     * @throws DecrypterRetryException
     */
    public ArrayList<DownloadLink> parseHTTPDirectory(final CryptedLink param, final Browser br) throws IOException, PluginException, DecrypterRetryException {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String path = this.getCurrentDirectoryPath(br);
        /* Path should always be given! */
        if (path == null) {
            /* Either offline or not a http index */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        FilePackage fp = null;
        /* Set full path as packagename so users can easily see that for every package. */
        if (!path.equals("/")) {
            fp = FilePackage.getInstance();
            fp.setName(path);
        }
        /* nginx (default?): Entries sometimes contain the create-date, sometimes only the filesize (for folders, only "-"). */
        String[][] filesAndFolders = br.getRegex("<a href=\"([^\"]+)\">([^>]+)</a>(?: *\\d{1,2}-[A-Za-z]{3}-\\d{4} \\d{1,2}:\\d{1,2})?[ ]+(\\d+|-)").getMatches();
        if (filesAndFolders.length > 0) {
            /* Nginx */
            for (final String[] finfo : filesAndFolders) {
                final DownloadLink link = parseEntry(DirectoryListingMode.NGINX, br, finfo);
                link.setRelativeDownloadFolderPath(path);
                if (fp != null) {
                    link._setFilePackage(fp);
                }
                ret.add(link);
            }
        } else {
            /* Apache default http dir index */
            filesAndFolders = br.getRegex("<a href=\"([^\"]+)\">[^<]*</a>\\s*</td><td align=\"right\">\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}\\s*</td><td align=\"right\">[ ]*(\\d+(\\.\\d)?[A-Z]?|-)[ ]*</td>").getMatches();
            if (filesAndFolders == null || filesAndFolders.length == 0) {
                throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, "EMPTY_FOLDER_" + path);
            }
            for (final String[] finfo : filesAndFolders) {
                final DownloadLink link = parseEntry(DirectoryListingMode.APACHE, br, finfo);
                link.setRelativeDownloadFolderPath(path);
                if (fp != null) {
                    link._setFilePackage(fp);
                }
                ret.add(link);
            }
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
            final long fileSize = SizeFormatter.getSize(filesizeStr + "b");
            switch (directoryListing) {
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
        String path = br.getRegex("(?i)<(?:title|h1)>\\s*Index of\\s*(/[^<]*)\\s*</(?:title|h1)>").getMatch(0);
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

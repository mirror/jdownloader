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
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.SiteType.SiteTemplate;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "HTTPDirectoryCrawler" }, urls = { "" })
public class GenericHTTPDirectoryIndexCrawler extends PluginForDecrypt {
    public GenericHTTPDirectoryIndexCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public enum DirectoryListingMode {
        NGINX,
        APACHE
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        return this.crawlHTTPDirectory(param);
    }

    protected ArrayList<DownloadLink> crawlHTTPDirectory(final CryptedLink param) throws IOException, PluginException {
        br.setFollowRedirects(true);
        /* First check if maybe the user has added a directURL. */
        final URLConnectionAdapter con = this.br.openGetConnection(param.getCryptedUrl());
        try {
            if (this.looksLikeDownloadableContent(con)) {
                final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
                final DownloadLink direct = getCrawler().createDirectHTTPDownloadLink(getCurrentLink(), con);
                decryptedLinks.add(direct);
                return decryptedLinks;
            } else {
                br.followConnection();
                if (br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                return this.parseHTTPDirectory(param, br);
            }
        } finally {
            con.disconnect();
        }
    }

    /** Does parsing only, without any HTTP requests! */
    protected ArrayList<DownloadLink> parseHTTPDirectory(final CryptedLink param, final Browser br) throws IOException, PluginException {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String path = this.getCurrentDirectoryPath(br);
        /* Path should always be given! */
        if (path == null) {
            /* Either offline or not a http index */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final FilePackage fp = FilePackage.getInstance();
        /* Set full path as packagename so users can easily see that for every package. */
        fp.setName(path);
        /* nginx (default?): Entries sometimes contain the create-date, sometimes only the filesize (for folders, only "-"). */
        String[][] filesAndFolders = br.getRegex("<a href=\"([^\"]+)\">([^>]+)</a>(?: *\\d{1,2}-[A-Za-z]{3}-\\d{4} \\d{1,2}:\\d{1,2})?[ ]+(\\d+|-)").getMatches();
        if (filesAndFolders.length > 0) {
            /* nginx */
            for (final String[] finfo : filesAndFolders) {
                final DownloadLink downloadLink = parseEntry(DirectoryListingMode.NGINX, br, finfo);
                downloadLink.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, path);
                downloadLink._setFilePackage(fp);
                decryptedLinks.add(downloadLink);
            }
        } else {
            /* Apache default http dir index */
            filesAndFolders = br.getRegex("<a href=\"([^\"]+)\">[^<]*</a>\\s*</td><td align=\"right\">\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}\\s*</td><td align=\"right\">[ ]*(\\d+(\\.\\d)?[A-Z]?|-)[ ]*</td>").getMatches();
            if (filesAndFolders.length == 0) {
                decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl(), "EMPTY_FOLDER " + path, "EMPTY_FOLDER " + path));
                return decryptedLinks;
            }
            for (final String[] finfo : filesAndFolders) {
                final DownloadLink downloadLink = parseEntry(DirectoryListingMode.APACHE, br, finfo);
                downloadLink.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, path);
                downloadLink._setFilePackage(fp);
                decryptedLinks.add(downloadLink);
            }
        }
        return decryptedLinks;
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
            /* Folder */
            final DownloadLink dlfolder = this.createDownloadlink(url);
            return dlfolder;
        } else {
            /* File */
            final DownloadLink dlfile = new DownloadLink(null, null, "DirectHTTP", "directhttp://" + url, true);
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

    protected String getCurrentDirectoryPath(final Browser br) {
        String path = br.getRegex("(?i)<(?:title|h1)>Index of (/[^<]+)</(?:title|h1)>").getMatch(0);
        if (path == null) {
            return null;
        } else {
            if (Encoding.isUrlCoded(path)) {
                path = Encoding.htmlDecode(path);
            }
            return path;
        }
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.GenericHTTPDirectoryIndex;
    }
}

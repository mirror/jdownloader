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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import org.appwork.utils.Regex;
import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class KrakenfilesComFolder extends PluginForDecrypt {
    public KrakenfilesComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "krakenfiles.com" });
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
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/profiles/(\\w+).*");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String parameter = param.getCryptedUrl();
        final String profileName = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML(">\\s*This profile is private")) {
            /* 2021-03-31: Either only visible to owner or only specified users. */
            throw new AccountRequiredException();
        }
        final String[] pathSegments = br.getRegex("class=\"breadcrumb-item[^\"]*\"><a href=\"/profiles[^\"]+\"[^>]*>([^<]+)</a></li>").getColumn(0);
        final FilePackage fp = FilePackage.getInstance();
        String path = null;
        if (pathSegments != null && pathSegments.length > 0) {
            int index = 0;
            for (String pathSegment : pathSegments) {
                pathSegment = Encoding.htmlDecode(pathSegment).trim();
                if (index == 0) {
                    /*
                     * On their website the root directory is called "path" while we want that to be the name of the profile of the user who
                     * has uploaded those files.
                     */
                    pathSegment = pathSegment.replace("Home", profileName);
                }
                if (path == null) {
                    path = pathSegment;
                } else {
                    path += "/" + pathSegment;
                }
                index++;
            }
            fp.setName(path);
        } else {
            fp.setName(profileName);
        }
        fp.addLinks(ret);
        String next = null;
        int page = 0;
        final HashSet<String> dupes = new HashSet<String>();
        do {
            page += 1;
            final String[] fileIDs = br.getRegex("/view/([a-z0-9]+)/file\\.html").getColumn(0);
            final ArrayList<DownloadLink> newitems = new ArrayList<DownloadLink>();
            if (fileIDs != null && fileIDs.length > 0) {
                final String[] filesizes = br.getRegex(">Size</div>\\s*<div class=\"nk-file-details-col\"[^>]*>([^<]+)</div>").getColumn(0);
                int fileindex = 0;
                for (final String fileID : fileIDs) {
                    if (!dupes.add(fileID)) {
                        /* Skip dupes */
                        continue;
                    }
                    final DownloadLink dl = createDownloadlink("https://" + this.getHost() + "/view/" + fileID + "/file.html");
                    String filename = br.getRegex("/view/" + fileID + "/file\\.html\">\\s*<div class=\"sl-content\">\\s*<span[^>]*>([^>]+)<").getMatch(0);
                    if (filename == null) {
                        /* 2023-11-21 */
                        filename = br.getRegex("/view/" + fileID + "/file\\.html\" class=\"title\"[^>]*>([^<]+)</a>").getMatch(0);
                    }
                    if (filename != null) {
                        dl.setName(filename);
                    } else {
                        /* Fallback */
                        dl.setName(fileID);
                    }
                    if (filesizes != null && fileindex < filesizes.length) {
                        final String filesizeStr = filesizes[fileindex];
                        dl.setDownloadSize(SizeFormatter.getSize(filesizeStr));
                    }
                    dl.setAvailable(true);
                    dl._setFilePackage(fp);
                    if (path != null) {
                        dl.setRelativeDownloadFolderPath(path);
                    }
                    newitems.add(dl);
                    fileindex++;
                }
            }
            /* Crawl nested subfolders */
            final String folderids[] = br.getRegex("data-id=\"([^\"]+)\" data-dirname=").getColumn(0);
            if (folderids != null && folderids.length > 0) {
                for (final String folderid : folderids) {
                    if (!dupes.add(folderid)) {
                        /* Skip dupes */
                        continue;
                    }
                    final DownloadLink folder = this.createDownloadlink(br.getURL("/profiles/" + profileName + "/files/" + folderid).toExternalForm());
                    newitems.add(folder);
                }
            }
            next = br.getRegex("<a rel=\"next\" href=\"(/profiles/" + Pattern.quote(profileName) + "\\?page=" + (page + 1) + ")\"").getMatch(0);
            logger.info("Crawled page " + page + " | Found number of items so far: " + ret.size() + " | Nextpage: " + next);
            if (newitems.isEmpty()) {
                /* This should never happen. */
                if (ret.isEmpty()) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    /* This should never happen! */
                    logger.info("Stopping because: Current page doesn't contain any items");
                    break;
                }
            }
            /* Make items appear live in linkgrabber */
            for (final DownloadLink result : newitems) {
                distribute(result);
            }
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else if (newitems.isEmpty()) {
                logger.info("Stopping because: Failed to find any new items on current page");
                break;
            } else if (next == null) {
                logger.info("Stopping because: Reached end");
                break;
            } else {
                /* Continue to next page */
                br.getPage(next);
            }
        } while (!this.isAbort());
        return ret;
    }
}

//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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
import java.util.List;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.RapidGatorNet;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
@SuppressWarnings("deprecation")
@PluginDependencies(dependencies = { RapidGatorNet.class })
public class RapidGatorNetFolder extends PluginForDecrypt {
    public RapidGatorNetFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        return jd.plugins.hoster.RapidGatorNet.getPluginDomains();
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/folder/(\\d+)/.*");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String contenturl = param.getCryptedUrl().replaceFirst("(?i)http://", "https://");
        final String folderID = new Regex(contenturl, this.getSupportedLinks()).getMatch(0);
        br.setFollowRedirects(true);
        br.getPage(contenturl);
        String folderTitle = br.getRegex("(?i)Downloading\\s*:\\s*</strong>(.*?)</p>").getMatch(0);
        if (folderTitle == null) {
            folderTitle = br.getRegex("(?i)<title>Download file (.*?)</title>").getMatch(0);
        }
        final String titleForEmptyOrOfflineFolder;
        if (folderTitle != null) {
            folderTitle = Encoding.htmlDecode(folderTitle).trim();
            titleForEmptyOrOfflineFolder = folderID + "_" + folderTitle;
        } else {
            /* Fallback */
            folderTitle = folderID;
            titleForEmptyOrOfflineFolder = folderTitle;
        }
        if (br.containsHTML("E_FOLDERNOTFOUND") || br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("class=\"empty\"")) {
            throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, titleForEmptyOrOfflineFolder);
        }
        FilePackage fp;
        if (folderTitle != null) {
            fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(folderTitle).trim());
            fp.addLinks(ret);
        } else {
            fp = null;
        }
        String lastPageURL = null;
        int page = 1;
        String lastPageStr = "Unknown";
        do {
            ret.addAll(parsePage(fp));
            String nextPageURL = br.getRegex("(?i)<a href=\"(/folder/" + folderID + "/[^>]+\\?page=\\d+)\">\\s*Next").getMatch(0);
            if (lastPageURL == null) {
                lastPageURL = br.getRegex("(?i)<a href=\"(/folder/" + folderID + "/[^>]+\\?page=\\d+)\">\\s*Last").getMatch(0);
                if (lastPageURL != null) {
                    lastPageStr = UrlQuery.parse(lastPageURL).get("page");
                }
            }
            logger.info("Crawled page " + page + "/" + lastPageStr + " | Found items so far: " + ret.size());
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else if (nextPageURL == null) {
                logger.info("Stopping because: Failed to find nextPage");
                break;
            } else if (lastPageURL == null || br.getURL().contains(lastPageURL)) {
                logger.info("Stopping because: Reached last page");
                break;
            } else {
                /* Small delay between requests */
                sleep(500, param);
                br.getPage(nextPageURL);
                page++;
            }
        } while (true);
        return ret;
    }

    private List<DownloadLink> parsePage(final FilePackage fp) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final List<DownloadLink> pageRet = new ArrayList<DownloadLink>();
        final String[] subfolders = br.getRegex("<td><a href=\"(/folder/\\d+/[^<>\"/]+\\.html)\">").getColumn(0);
        final String[][] links = br.getRegex("\"(/file/([a-z0-9]{32}|\\d+)/([^\"]+))\".*?>([\\d\\.]+ (KB|MB|GB))").getMatches();
        if ((links == null || links.length == 0) && (subfolders == null || subfolders.length == 0)) {
            /* This should never happen as we check for offline/empty folder in beforehand. */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (links != null && links.length != 0) {
            for (String[] dl : links) {
                final DownloadLink link = createDownloadlink(Request.getLocation(dl[0], br.getRequest()));
                link.setName(dl[2].replaceFirst("\\.html$", ""));
                link.setDownloadSize(SizeFormatter.getSize(dl[3]));
                link.setAvailable(true);
                pageRet.add(link);
                if (fp != null) {
                    fp.add(link);
                }
                ret.add(link);
                distribute(link);
            }
        }
        if (subfolders != null && subfolders.length != 0) {
            for (final String folder : subfolders) {
                final DownloadLink link = createDownloadlink(Request.getLocation(folder, br.getRequest()));
                pageRet.add(link);
                if (fp != null) {
                    fp.add(link);
                }
                ret.add(link);
                distribute(link);
            }
        }
        return pageRet;
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    public boolean hasCaptcha(final CryptedLink link, final jd.plugins.Account acc) {
        return false;
    }
}
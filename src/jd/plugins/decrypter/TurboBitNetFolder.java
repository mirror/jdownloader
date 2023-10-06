//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.appwork.storage.TypeRef;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.components.SiteType.SiteTemplate;
import jd.plugins.hoster.TurboBitNet;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
@PluginDependencies(dependencies = { TurboBitNet.class })
public class TurboBitNetFolder extends antiDDoSForDecrypt {
    @Override
    public String[] siteSupportedNames() {
        return getAllSupportedNames();
    }

    public static String[] getAllSupportedNames() {
        /* Different Hostplugins which all extend one class - this crawler can handle all of their folder-links */
        final String[] supportedNamesTurbobit = jd.plugins.hoster.TurboBitNet.domains;
        final String[] supportedNamesHitfile = jd.plugins.hoster.HitFileNet.domains;
        final String[][] supportedNamesArrays = { jd.plugins.hoster.TurboBitNet.domains, jd.plugins.hoster.HitFileNet.domains };
        final String[] supportedNamesAll = new String[supportedNamesTurbobit.length + supportedNamesHitfile.length];
        int position = 0;
        for (final String[] supportedNamesOfOneHost : supportedNamesArrays) {
            for (final String supportedName : supportedNamesOfOneHost) {
                supportedNamesAll[position] = supportedName;
                position++;
            }
        }
        return supportedNamesAll;
    }

    public static String[] getAnnotationNames() {
        return new String[] { jd.plugins.hoster.TurboBitNet.domains[0] };
    }

    /**
     * returns the annotation pattern array
     *
     */
    public static String[] getAnnotationUrls() {
        // construct pattern
        final String host = getHostsPattern();
        return new String[] { host + "/download/folder/(\\d+)" };
    }

    private static String getHostsPattern() {
        final StringBuilder pattern = new StringBuilder();
        for (final String name : getAllSupportedNames()) {
            pattern.append((pattern.length() > 0 ? "|" : "") + Pattern.quote(name));
        }
        final String hosts = "https?://(?:www\\.)?" + "(?:" + pattern.toString() + ")";
        return hosts;
    }

    public TurboBitNetFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.setAllowedResponseCodes(new int[] { 400 });
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String folderID = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        if (folderID == null) {
            /* Developer mistake! */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // br.getPage("http://turbobit.net/downloadfolder/gridFile?id_folder=" + id + "&_search=false&nd=&rows=100000&page=1");
        final String host = Browser.getHost(param.getCryptedUrl(), true);
        br.setFollowRedirects(true);
        final int maxItemsPerPage = 100;
        int page = 1;
        do {
            final UrlQuery query = new UrlQuery();
            query.add("rootId", folderID);
            query.add("currentId", folderID);
            query.add("_search", "false");
            query.add("nd", Long.toString(System.currentTimeMillis()));
            query.add("rows", Integer.toString(maxItemsPerPage));
            query.add("page", Integer.toString(page));
            query.add("sidx", "name");
            /* Yes they misspelled "sort"! */
            query.add("sord", "asc");
            getPage("https://" + host + "/downloadfolder/gridFile?" + query.toString());
            if (br.getHttpConnection().getResponseCode() == 400 || br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
            final int maxPage = ((Number) entries.get("total")).intValue();
            final int numberofItemsOnCurrentPage = ((Number) entries.get("records")).intValue();
            final Map<String, Object> pathInfo = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "userdata/path/{0}");
            final String currentFolderName = pathInfo.get("name").toString();
            String path = this.getAdoptedCloudFolderStructure();
            if (path != null) {
                path += "/" + currentFolderName;
            } else {
                /* Current folder = our root folder */
                path = currentFolderName;
            }
            if (numberofItemsOnCurrentPage == 0) {
                if (ret.isEmpty()) {
                    /* Happened on first page */
                    final DownloadLink dummy = this.createOfflinelink(param.getCryptedUrl(), "EMPTY_FOLDER_" + path, "This folder is empty.");
                    ret.add(dummy);
                    return ret;
                } else {
                    /* This should never happen */
                    logger.info("Stopping because: Current page contains zero items");
                    break;
                }
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(path);
            final List<Map<String, Object>> resources = (List<Map<String, Object>>) entries.get("rows");
            for (final Map<String, Object> resource : resources) {
                final String id = resource.get("id").toString();
                final List<String> data = (List<String>) resource.get("cell");
                final String fileFolderInfo = data.get(2);
                if (id.matches("\\d+") && fileFolderInfo.contains("")) {
                    /* Folder */
                    final DownloadLink folder = this.createDownloadlink("https://" + br.getHost() + "/download/folder/" + id);
                    folder.setRelativeDownloadFolderPath(path);
                    ret.add(folder);
                    distribute(folder);
                } else {
                    /* File */
                    final DownloadLink file = this.createDownloadlink("https://" + br.getHost() + "/" + id + ".html");
                    final String filename = new Regex(fileFolderInfo, "target='_blank'[^>]*>([^<]*?)</a>").getMatch(0);
                    if (filename != null) {
                        file.setName(Encoding.htmlDecode(filename).trim());
                    } else {
                        logger.warning("Failed to find filename for item: " + id);
                    }
                    final String filesize = data.get(3);
                    file.setDownloadSize(SizeFormatter.getSize(filesize));
                    file.setAvailable(true);
                    file.setRelativeDownloadFolderPath(path);
                    file._setFilePackage(fp);
                    ret.add(file);
                    distribute(file);
                }
            }
            logger.info("Crawled page " + page + "/" + maxPage + " | Found items: " + ret.size() + "/" + numberofItemsOnCurrentPage);
            if (this.isAbort()) {
                break;
            } else if (page >= maxPage) {
                logger.info("Stopping because: Reached last page");
                break;
            } else if (resources.size() < maxItemsPerPage) {
                /* Additional fail-safe */
                logger.info("Stopping because: Reached end");
                break;
            }
            page++;
        } while (true);
        return ret;
    }

    @Override
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.Turbobit_Turbobit;
    }
}
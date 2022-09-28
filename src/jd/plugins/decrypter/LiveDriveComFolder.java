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

import org.appwork.utils.Regex;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class LiveDriveComFolder extends PluginForDecrypt {
    public LiveDriveComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "livedrive.com" });
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
            ret.add("https?://(?:[A-Za-z0-9\\-]+\\.)?" + buildHostsPatternPart(domains) + "/.+");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            /* Folder offline */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Regex urlinfo = new Regex(br.getURL(), "https?://[^/]+/portal/public-shares/([^/]+)(/\\*_([a-zA-Z0-9_/\\+\\=\\-%]+))?");
        final String user = new Regex(br.getURL(), "https?://[^/]+/portal/public-shares/([^/]+)").getMatch(0);
        final String directoryIDCrypted = urlinfo.getMatch(2);
        if (user == null) {
            /* Redirect to unsupported URL -> Must be offline */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String directoryID = null;
        if (directoryIDCrypted != null) {
            directoryID = Encoding.Base64Decode(directoryIDCrypted);
        }
        String currentSubfolderPath = this.getAdoptedCloudFolderStructure(user);
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        int page = 1;
        final int maxItemsPerPage = 50;
        FilePackage fp = FilePackage.getInstance();
        final String urlPart;
        if (directoryID != null) {
            urlPart = "directory/" + directoryID + "/files";
        } else {
            urlPart = "files";
        }
        final String titleForOfflineContent;
        if (directoryIDCrypted != null) {
            titleForOfflineContent = user + "_" + directoryIDCrypted;
        } else {
            titleForOfflineContent = user;
        }
        do {
            final UrlQuery query = new UrlQuery();
            query.add("count", Integer.toString(maxItemsPerPage));
            query.addAndReplace("page", Integer.toString(page));
            query.add("includePublicShares", "true");
            query.add("includePrivateShares", "false");
            br.getPage("https://public.livedrive.com/portal/account/sharing/withme/" + user + "/" + urlPart + "?" + query.toString());
            final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            if (page == 1) {
                fp.setName(currentSubfolderPath);
            }
            final List<Map<String, Object>> resourcelist = (List<Map<String, Object>>) entries.get("resourceList");
            if (resourcelist.isEmpty() && page == 1) {
                throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, titleForOfflineContent);
            }
            for (final Map<String, Object> resource : resourcelist) {
                final String resourceID = resource.get("fileId").toString();
                final int resourceType = ((Number) resource.get("type")).intValue();
                if (resourceType == 1) {
                    final DownloadLink file = this.createDownloadlink("https://" + br.getHost(true) + "/portal/public-shares/" + user + "/file/*_" + Encoding.Base64Encode(resourceID));
                    jd.plugins.hoster.LiveDriveCom.parseFileInfo(file, resource);
                    file.setRelativeDownloadFolderPath(currentSubfolderPath);
                    file._setFilePackage(fp);
                    ret.add(file);
                    distribute(file);
                } else if (resourceType == 2) {
                    /* Folder */
                    final DownloadLink folder = this.createDownloadlink("https://" + br.getHost(true) + "/portal/public-shares/" + user + "/*_" + Encoding.Base64Encode(resourceID));
                    folder.setRelativeDownloadFolderPath(currentSubfolderPath + "/" + resource.get("name"));
                    ret.add(folder);
                    distribute(folder);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            final int pageCount = ((Number) entries.get("pageCount")).intValue();
            logger.info("Crawled page " + page + "/" + pageCount + " | Found items so far: " + ret.size());
            if (this.isAbort()) {
                /* Aborted by user */
                break;
            } else if (page == pageCount) {
                /* We've just crawled the last page */
                logger.info("Stopping because: Reached end");
                break;
            } else if (resourcelist.size() < maxItemsPerPage) {
                /* Additional fail safe */
                logger.info("Stopping because: Current page contains less items than " + maxItemsPerPage);
                break;
            } else {
                page++;
            }
        } while (true);
        return ret;
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}
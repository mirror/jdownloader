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
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.AllDebridCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class AlldebridComFolder extends PluginForDecrypt {
    public AlldebridComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "alldebrid.com", "alldebrid.fr", "alldebrid.org", "alldebrid.it", "alldebrid.de", "alldebrid.es" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/getMagnet/(\\d+)");
        }
        return ret.toArray(new String[0]);
    }

    /** API docs: https://docs.alldebrid.com/#status */
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String addedURL = param.getCryptedUrl();
        /*
         * Important: Every account has its own files. A magnetID generated inside account A will not work in account B! Alldebrid support
         * knows about this issue and is thinking about adding a modifier into their magnetURL so that we know which account to use in case
         * the user owns multiple alldebrid.com accounts.
         */
        final String magnetID = new Regex(addedURL, this.getSupportedLinks()).getMatch(0);
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        if (account == null) {
            throw new AccountRequiredException();
        }
        final UrlQuery query = new UrlQuery();
        query.appendEncoded("agent", AllDebridCom.agent_raw);
        query.appendEncoded("apikey", AllDebridCom.getStoredApiKey(account));
        query.add("id", magnetID);
        br.getPage(AllDebridCom.api_base + "/magnet/status?" + query.toString());
        final Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.HASHMAP);
        if (entries.containsKey("error")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> magnet = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "data/magnets");
        final String torrentName = (String) magnet.get("filename");
        final String torrentNameEscaped = Pattern.quote(torrentName);
        final List<Map<String, Object>> linksO = (List<Map<String, Object>>) magnet.get("links");
        if (linksO == null || linksO.isEmpty()) {
            /* Probably unfinished torrent download */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String folderRoot = torrentName;
        final FilePackage fpRoot = FilePackage.getInstance();
        fpRoot.setName(folderRoot);
        for (final Map<String, Object> resource : linksO) {
            final String url = (String) resource.get("link");
            final String filename = (String) resource.get("filename");
            final long filesize = ((Number) resource.get("size")).longValue();
            final DownloadLink dl = this.createDownloadlink(url);
            dl.setName(filename);
            dl.setDownloadSize(filesize);
            final boolean isSpecialRar = filename.matches("^" + torrentNameEscaped + "(\\.rar|\\.part\\d+\\.rar)$");
            if (isSpecialRar) {
                dl.setRelativeDownloadFolderPath(folderRoot);
                dl._setFilePackage(fpRoot);
            } else {
                /* Check whether or not this file goes into a deeper subfolder level. */
                String filePath = getFilePath((List<Object>) resource.get("files"), "");
                /* Path is full path with filename at the end -> Remove that */
                filePath = filePath.replaceAll("/" + org.appwork.utils.Regex.escape(filename) + "$", "");
                if (!StringUtils.isEmpty(filePath)) {
                    /* File that goes into (nested) subfolder. */
                    dl.setRelativeDownloadFolderPath(folderRoot + filePath);
                    final FilePackage nestedFilePackage = FilePackage.getInstance();
                    nestedFilePackage.setName(folderRoot + filePath);
                    dl._setFilePackage(nestedFilePackage);
                } else {
                    /* File that is in the root of the torrent main folder (named after torrent name). */
                    dl.setRelativeDownloadFolderPath(folderRoot);
                    dl._setFilePackage(fpRoot);
                }
            }
            dl.setAvailable(true);
            ret.add(dl);
        }
        return ret;
    }

    /** Recursive function which returns the complete path to a file inside nested json arrays. */
    private String getFilePath(final List<Object> subfolderList, String path) {
        if (subfolderList.isEmpty()) {
            return null;
        }
        final Map<String, Object> entries = (Map<String, Object>) subfolderList.get(0);
        final Object subfolderNameO = entries.get("n");
        if (subfolderNameO == null) {
            return null;
        }
        final String subfolderName = (String) subfolderNameO;
        path += "/" + subfolderName;
        final Object nextSubFolderLevel = entries.get("e");
        if (nextSubFolderLevel == null) {
            /* We've reached the end */
            return path;
        } else {
            /* Go deeper */
            return this.getFilePath((List<Object>) nextSubFolderLevel, path);
        }
    }
}

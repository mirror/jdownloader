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

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class DownloadSaikoanimesNetFolder extends PluginForDecrypt {
    public DownloadSaikoanimesNetFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "download.saikoanimes.net" });
        ret.add(new String[] { "storage.saikoanimes.net" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/drive/s/([A-Za-z0-9]+)");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String folderID = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        final UrlQuery query = new UrlQuery();
        query.add("withEntries", "true");
        /* TODO: Add pagination support */
        query.add("page", "1");
        br.getPage("https://" + this.getHost() + "/secure/drive/shareable-links/" + folderID + "?" + query.toString());
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final Map<String, Object> folderChildren = (Map<String, Object>) entries.get("folderChildren");
        final Map<String, Object> linkInfo = (Map<String, Object>) entries.get("link");
        final Map<String, Object> fileFolderInfo = (Map<String, Object>) linkInfo.get("entry");
        final String folderName = (String) fileFolderInfo.get("name");
        final FilePackage fp = FilePackage.getInstance();
        if (!StringUtils.isEmpty(folderName)) {
            fp.setName(folderName);
        } else {
            /* Fallback */
            fp.setName(folderID);
        }
        /* TODO: Find out what happens when this == false */
        // final boolean allowDownload = ((Boolean)linkInfo.get("allow_download")).booleanValue();
        final int linkID = ((Number) linkInfo.get("id")).intValue();
        List<Map<String, Object>> ressourcelist = null;
        if (((Number) folderChildren.get("total")).intValue() > 0) {
            /* Add all items of a folder */
            ressourcelist = (List<Map<String, Object>>) folderChildren.get("data");
        } else {
            /* Assume we got a single file --> Add that */
            ressourcelist = new ArrayList<Map<String, Object>>();
            ressourcelist.add(fileFolderInfo);
        }
        for (final Map<String, Object> file : ressourcelist) {
            final String filename = (String) file.get("name");
            // final String url = (String) entries.get("url");
            final String hash = (String) file.get("hash");
            final long filesize = ((Number) file.get("file_size")).longValue();
            final DownloadLink dl = this.createDownloadlink("directhttp://https://" + this.getHost() + "/secure/uploads/download?hashes=" + hash + "&shareable_link=" + linkID);
            dl.setFinalFileName(filename);
            dl.setVerifiedFileSize(filesize);
            dl.setAvailable(true);
            dl._setFilePackage(fp);
            distribute(dl);
            decryptedLinks.add(dl);
        }
        return decryptedLinks;
    }
}

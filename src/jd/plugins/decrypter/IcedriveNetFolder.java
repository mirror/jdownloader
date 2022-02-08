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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class IcedriveNetFolder extends PluginForDecrypt {
    public IcedriveNetFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "icedrive.net" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/s/[A-Za-z0-9]+");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* Internal folderID (but this can also contain spaces lol) */
        final String folderID = br.getRegex("data-folder-id=\"([^\"]+)").getMatch(0);
        /* Name of current/root folder */
        final String folderName = br.getRegex("data-title=\"([^<>\"]+)\"").getMatch(0);
        if (folderID == null) {
            /* Assume that content is offline */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getPage("/dashboard/ajax/get?req=list-files&type=public-share&parentId=" + Encoding.urlEncode(folderID));
        final Map<String, Object> root = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        if ((Boolean) root.get("error") == Boolean.TRUE) {
            /* Assume that content is offline */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final int numberofFiles = ((Number) root.get("results")).intValue();
        if (numberofFiles <= 0) {
            /* Empty folder */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final List<Map<String, Object>> ressourcelist = (List<Map<String, Object>>) root.get("data");
        for (final Map<String, Object> resource : ressourcelist) {
            final Boolean isFolder = (Boolean) resource.get("isFolder");
            if (isFolder == Boolean.TRUE) {
                /* TODO: Subfolders are not yet supported (I was unable to find any working examples). */
                continue;
            }
            final String id = resource.get("id").toString();
            /* 2022-02-08: Title is sometimes url-encoded in json (space == "+"). */
            final String title = (String) resource.get("filename");
            final long filesize = ((Number) resource.get("filesize")).longValue();
            final DownloadLink dl = this.createDownloadlink("https://" + this.getHost() + "/file/" + id);
            dl.setFinalFileName(Encoding.htmlDecode(title));
            dl.setVerifiedFileSize(filesize);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }
        if (folderName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(folderName).trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}

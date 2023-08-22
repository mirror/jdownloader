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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
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
import jd.plugins.hoster.IcedriveNet;

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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:0/[A-Za-z0-9]+|s/[A-Za-z0-9]+)");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> crawlFolder(final CryptedLink param, Browser br, String name, final String folderID) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        if (isAbort()) {
            return ret;
        }
        if (!StringUtils.isEmpty(name)) {
            name = Encoding.htmlDecode(name).trim();
        }
        final Browser brc = br.cloneBrowser();
        /* Folder */
        brc.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        brc.getPage("https://" + this.getHost() + "/API/Internal/V1/?request=collection&type=public&folderId=" + URLEncode.encodeURIComponent(folderID) + "&sess=1");
        final Map<String, Object> root = restoreFromString(brc.toString(), TypeRef.MAP);
        if ((Boolean) root.get("error") == Boolean.TRUE) {
            /* Assume that content is offline */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final int numberofFiles = ((Number) root.get("results")).intValue();
        if (numberofFiles <= 0) {
            /* Empty folder */
            if (!StringUtils.isEmpty(name)) {
                throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, folderID + "_" + name);
            } else {
                throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, folderID);
            }
        }
        final FilePackage fp;
        if (!StringUtils.isEmpty(name)) {
            fp = FilePackage.getInstance();
            fp.setName(name);
        } else {
            fp = null;
        }
        final HashMap<String, String> subfolders = new HashMap<String, String>();
        final List<Map<String, Object>> ressourcelist = (List<Map<String, Object>>) root.get("data");
        for (final Map<String, Object> resource : ressourcelist) {
            final Object isFolder = resource.get("isFolder");
            final String id = resource.get("id").toString();
            /* 2022-02-08: Title is sometimes url-encoded in json (space == "+"). */
            final String filename = (String) resource.get("filename");
            if (Boolean.TRUE.equals(isFolder) || (isFolder instanceof Number && ((Number) isFolder).intValue() == 1)) {
                subfolders.put(id, filename);
            } else {
                final long filesize = ((Number) resource.get("filesize")).longValue();
                final DownloadLink dl = this.createDownloadlink(createContentURL(id));
                /* They do not have separate user-browsable URLs for single files inside folders. */
                dl.setContentUrl(param.getCryptedUrl());
                dl.setProperty(IcedriveNet.PROPERTY_INTERNAL_FOLDER_ID, folderID);
                dl.setFinalFileName(Encoding.htmlDecode(filename));
                dl.setVerifiedFileSize(filesize);
                dl.setAvailable(true);
                if (fp != null) {
                    fp.add(dl);
                }
                ret.add(dl);
                distribute(dl);
                if (isAbort()) {
                    break;
                }
            }
        }
        for (Entry<String, String> subfolder : subfolders.entrySet()) {
            if (!isAbort()) {
                try {
                    final ArrayList<DownloadLink> subfolderRet = crawlFolder(param, br, subfolder.getValue(), subfolder.getKey());
                    ret.addAll(subfolderRet);
                } catch (Exception e) {
                    logger.log(e);
                }
            }
        }
        return ret;
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* Internal folderID (but this can also contain spaces lol) */
        final String folderID = br.getRegex("downloadFolderZip\\((.*?)\\)").getMatch(0);
        final String fileID = br.getRegex("previewItem\\('(.*?)'").getMatch(0);
        /* Name of current/root folder */
        final String name = br.getRegex(">\\s*Filename\\s*</p>\\s*<p\\s*class\\s*=\\s*\"value\"\\s*>\\s*(.*?)\\s*<").getMatch(0);
        final String type = br.getRegex(">\\s*Type\\s*</p>\\s*<p\\s*class\\s*=\\s*\"value\"\\s*>\\s*(.*?)\\s*<").getMatch(0);
        final String size = br.getRegex(">\\s*Size\\s*</p>\\s*<p\\s*class\\s*=\\s*\"value\"\\s*>\\s*(.*?)\\s*<").getMatch(0);
        if (fileID == null && folderID == null) {
            /* Assume that content is offline */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if ("Folder".equals(type) || folderID != null) {
            if (folderID == null || !"Folder".equals(type)) {
                /* Unsupported type. This should never happen! */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            return crawlFolder(param, br, name, folderID);
        } else {
            if (fileID == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Single file */
            final DownloadLink dl = this.createDownloadlink(createContentURL(fileID));
            dl.setFinalFileName(name);
            if (size != null) {
                dl.setDownloadSize(SizeFormatter.getSize(size));
            }
            dl.setProperty(IcedriveNet.PROPERTY_INTERNAL_FILE_ID, fileID);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
    }

    private String createContentURL(final String fileID) {
        return "https://" + this.getHost() + "/file/" + fileID;
    }
}

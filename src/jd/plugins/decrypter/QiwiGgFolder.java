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
import java.util.regex.Pattern;

import org.appwork.storage.TypeRef;
import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.hoster.QiwiGg;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { QiwiGg.class })
public class QiwiGgFolder extends PluginForDecrypt {
    public QiwiGgFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        return QiwiGg.getPluginDomains();
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/folder/([a-f0-9]{24})");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String folderID = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        if (folderID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String thisFolderTitle = br.getRegex("<title>([^<]+)</title>").getMatch(0);
        if (thisFolderTitle != null) {
            thisFolderTitle = Encoding.htmlDecode(thisFolderTitle).trim();
            thisFolderTitle = thisFolderTitle.replaceFirst("(?i)\\s*• Download$", "");
        } else {
            /* Fallback */
            thisFolderTitle = folderID;
        }
        String path = this.getAdoptedCloudFolderStructure();
        if (path == null) {
            path = thisFolderTitle;
        } else {
            path += "/" + thisFolderTitle;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(path);
        final String filesJson = br.getRegex("files\\\\\":(\\[.*?\\}\\]),").getMatch(0);
        final String[] fileIDs = br.getRegex("/file/([^\"\\']+)").getColumn(0);
        final String[] subfolderIDs = br.getRegex("/folder/([a-f0-9]{24})\"").getColumn(0);
        if (filesJson != null) {
            /* Prefer json source as it is much easier to parse */
            final ArrayList<HashMap<String, Object>> ressourcelist = restoreFromString(PluginJSonUtils.unescape(filesJson), TypeRef.LIST_HASHMAP);
            for (final HashMap<String, Object> filemap : ressourcelist) {
                final DownloadLink file = this.createDownloadlink(br.getURL("/file/" + filemap.get("slug").toString()).toString());
                file.setName(filemap.get("fileName").toString());
                final String fileSizeBytesStr = filemap.get("fileSize").toString();
                if (fileSizeBytesStr != null && fileSizeBytesStr.matches("\\d+")) {
                    file.setDownloadSize(Long.parseLong(fileSizeBytesStr));
                }
                file.setRelativeDownloadFolderPath(path);
                file.setAvailable(true);
                file._setFilePackage(fp);
                ret.add(file);
            }
        } else if (fileIDs != null && fileIDs.length > 0) {
            logger.info("Fallback: Crawling via html code");
            for (final String fileID : fileIDs) {
                final String url = br.getURL("/file/" + fileID).toString();
                final String urlQuoted = Pattern.quote(url);
                final DownloadLink file = createDownloadlink(url);
                String filename = br.getRegex("class=\"DownloadButton_FileName_[^\"]*\"[^>]*><p>([^<]+)</p> <a target=\"_blank\" href=\"" + urlQuoted).getMatch(0);
                // if (filename == null) {
                // /* Other view: File-view without filesize */
                // filename = br.getRegex(urlQuoted + "\"aria-label=\"Download file\"[^>]*><div [^>]*></span></div><p
                // [^>]*>([^<]+)</p>").getMatch(0);
                // }
                if (filename != null) {
                    filename = Encoding.htmlDecode(filename).trim();
                    file.setName(filename);
                    file.setProperty(QiwiGg.PROPERTY_FILENAME, filename);
                }
                final String filesize = br.getRegex("FileSize_[^\"]*\"[^>]*>([^<]+)</div></div></div><div><a class=\"DownloadButton_DownloadButton_[^\"]+\"[^<]*href=\"" + urlQuoted).getMatch(0);
                if (filesize != null) {
                    file.setDownloadSize(SizeFormatter.getSize(filesize));
                }
                file.setRelativeDownloadFolderPath(path);
                file.setAvailable(true);
                file._setFilePackage(fp);
                ret.add(file);
            }
        }
        if (subfolderIDs != null && subfolderIDs.length > 0) {
            for (final String subfolderID : subfolderIDs) {
                final String url = br.getURL("/folder/" + subfolderID).toString();
                final DownloadLink subfolder = createDownloadlink(url);
                subfolder.setRelativeDownloadFolderPath(path);
                ret.add(subfolder);
            }
        }
        if (ret.isEmpty()) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return ret;
    }
}

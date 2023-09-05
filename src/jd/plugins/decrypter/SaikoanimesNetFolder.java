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

import org.appwork.utils.Regex;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.parser.UrlQuery;

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
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class SaikoanimesNetFolder extends PluginForDecrypt {
    public SaikoanimesNetFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "saikoanimes.net" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/baixarr/(?:folderr/)?\\?(?:id)?=([A-Za-z0-9:]+)");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        /* 2023-07-06: It is not possible anymore to use that old WebAPI -> We need to find all required information in html code. */
        final boolean useOldCrawlerWhichUsesAPI = false;
        final String folderID = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        if (useOldCrawlerWhichUsesAPI) {
            ret.add(createDownloadlink("https://download.saikoanimes.net/drive/s/" + folderID));
        } else {
            br.setFollowRedirects(true);
            br.getPage(param.getCryptedUrl());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String subfolderpath = this.getAdoptedCloudFolderStructure(folderID);
            FilePackage fp = null;
            if (subfolderpath != null) {
                fp = FilePackage.getInstance();
                fp.setName(subfolderpath);
            }
            final String[] htmls = br.getRegex("<nav class='container-down'.*?</nav>").getColumn(-1);
            for (final String html : htmls) {
                final String url = new Regex(html, "data-url='(http[^\\']+)").getMatch(0);
                final String filename = new Regex(html, "down-icon'></i>([^<]+)<").getMatch(0);
                final String filesizeStr = new Regex(html, "class='tumbup'[^>]*>(\\d+ [^<]+)").getMatch(0);
                if (url.contains("folderr")) {
                    final String foldername = new Regex(html, "down-folder'[^>]*></i>([^<]+)</div>").getMatch(0);
                    final DownloadLink folder = this.createDownloadlink(url);
                    if (foldername != null) {
                        if (subfolderpath != null) {
                            folder.setRelativeDownloadFolderPath(subfolderpath + "/" + foldername);
                        } else {
                            folder.setRelativeDownloadFolderPath(foldername);
                        }
                    }
                    ret.add(folder);
                } else {
                    final DownloadLink file = this.createDownloadlink(DirectHTTP.createURLForThisPlugin(url));
                    file.setAvailable(true);
                    if (filename != null) {
                        file.setName(Encoding.htmlDecode(filename).trim());
                    }
                    if (filesizeStr != null) {
                        file.setDownloadSize(SizeFormatter.getSize(filesizeStr));
                    }
                    if (subfolderpath != null) {
                        file.setRelativeDownloadFolderPath(subfolderpath);
                    }
                    if (fp != null) {
                        file._setFilePackage(fp);
                    }
                    /* Try to set fileID for dupe-detection */
                    String uniqueFileIdentifier = null;
                    final String urlb64 = UrlQuery.parse(url).get("url");
                    if (urlb64 != null) {
                        final String urlDecoded = Encoding.Base64Decode(urlb64);
                        uniqueFileIdentifier = new Regex(urlDecoded, "/download/([\\w]+)").getMatch(0);
                    }
                    if (uniqueFileIdentifier == null) {
                        uniqueFileIdentifier = filename;
                    }
                    if (uniqueFileIdentifier != null) {
                        file.setLinkID(CloudSaikoanimesNetFolder.DUPE_IDENTIFIER + folderID + "/" + uniqueFileIdentifier);
                    }
                    ret.add(file);
                }
            }
            if (ret.isEmpty()) {
                /**
                 * 2023-07-06: At this moment they do not display any error message to indicate that content is offline. </br>
                 * Example: https://saikoanimes.net/baixarr/?=blablablub
                 */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        return ret;
    }
}

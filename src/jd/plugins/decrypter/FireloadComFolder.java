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

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.SiteType.SiteTemplate;
import jd.plugins.hoster.FireloadCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class FireloadComFolder extends PluginForDecrypt {
    public FireloadComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        return FireloadCom.getPluginDomains();
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/folder/([a-f0-9]{32})/([^/]+)?");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        final Regex folderInfo = new Regex(param.getCryptedUrl(), this.getSupportedLinks());
        final String folderID = folderInfo.getMatch(0);
        String folderNameFromURL = folderInfo.getMatch(1);
        if (folderNameFromURL != null) {
            folderNameFromURL = Encoding.htmlDecode(folderNameFromURL).trim();
        }
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!br.getURL().contains(folderID)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String currentFolderName = br.getRegex("(?i)<h2>\\s*Folder \\'([^\\']+)'\\s*</h2>").getMatch(0);
        if (currentFolderName == null) {
            /* Fallback */
            currentFolderName = folderNameFromURL;
        }
        if (currentFolderName == null) {
            /* Final fallback */
            currentFolderName = folderID;
        }
        currentFolderName = Encoding.htmlDecode(currentFolderName).trim();
        String filePath = this.getAdoptedCloudFolderStructure("");
        if (filePath.length() > 0) {
            filePath += "/";
        }
        filePath += currentFolderName;
        final String tableHTML = br.getRegex("<tbody><tr><td class=\"reponsiveMobileHide\"(.*?)</tbody>").getMatch(0);
        if (tableHTML == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String[] htmls = tableHTML.split("<tr><td");
        if (htmls == null || htmls.length == 0) {
            throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, "EMPTY_FOLDER_" + filePath);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(filePath);
        for (final String html : htmls) {
            String url = new Regex(html, "href=\"([^\"]+)").getMatch(0);
            if (url == null) {
                logger.warning("Skipping invalid HTML snippet: " + html);
                continue;
            }
            /* Make full URL out of relative URL. */
            url = br.getURL(url).toString();
            final DownloadLink link = this.createDownloadlink(url);
            final boolean isFolder = this.canHandle(url);
            if (!isFolder) {
                /* File */
                final String filename = new Regex(html, "target=\"_blank\"[^>]*>([^<]+)</a>").getMatch(0);
                if (filename != null) {
                    link.setName(Encoding.htmlDecode(filename).trim());
                }
                final String filesize = new Regex(html, "\\&nbsp;\\&nbsp;\\(([^<\\)]+)\\)<br").getMatch(0);
                if (filesize != null) {
                    link.setDownloadSize(SizeFormatter.getSize(filesize));
                }
                /* We know that that file is online. */
                link.setAvailable(true);
                link._setFilePackage(fp);
            }
            ret.add(link);
        }
        return ret;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.MFScripts_YetiShare;
    }
}

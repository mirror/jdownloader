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
import java.util.regex.Pattern;

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class MeocloudPtFolder extends PluginForDecrypt {
    public MeocloudPtFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "meocloud.pt" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/link/([a-f0-9\\-]+)/([^\\?]{2,})");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (isOffline(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // final String folderID = urlinfo.getMatch(0);
        final String folderPath = getPathFromURL(param.getCryptedUrl());
        if (folderPath == null) {
            /* Developer mistake! */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (br.containsHTML("(?i)>\\s*Pasta Vazia")) {
            throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER);
        }
        final Form pwform = MeocloudPtFolder.getPasswordProtectedForm(br);
        if (pwform != null) {
            /* 2020-02-18: PW protected URLs are not yet supported. */
            throw new PluginException(LinkStatus.ERROR_FATAL, "Password protected links are not yet supported: Contact support and ask for implementation.", 8 * 60 * 1000l);
        }
        final PluginForHost hosterPlugin = this.getNewPluginForHostInstance(this.getHost());
        final String[] fileDownloadurls = br.getRegex("data-url-download=\"(http[^\"]+)").getColumn(0);
        if (fileDownloadurls != null && fileDownloadurls.length > 0) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(folderPath);
            final String[] filesizes = br.getRegex("class=\"file_size\">([^<]+)</span>").getColumn(0);
            int fileIndex = 0;
            for (final String fileDownloadurl : fileDownloadurls) {
                final DownloadLink file = new DownloadLink(hosterPlugin, null, this.getHost(), fileDownloadurl, true);
                file.setAvailable(true);
                if (filesizes != null && filesizes.length == fileDownloadurls.length) {
                    /* Set filesize if we found it. */
                    file.setDownloadSize(SizeFormatter.getSize(filesizes[fileIndex]));
                }
                file.setRelativeDownloadFolderPath(folderPath);
                file._setFilePackage(fp);
                ret.add(file);
                fileIndex++;
            }
        }
        logger.info("Number of files: " + ret.size());
        /* Find subfolder URLs. */
        int numberofSubfolders = 0;
        final String linkPath = br.getRegex("name=\"link_path\" value=\"([a-f0-9\\-]+)").getMatch(0);
        final String[] urls = HTMLParser.getHttpLinks(br.getRequest().getHtmlCode(), br.getURL());
        for (final String url : urls) {
            if (url.contains("?") || !this.canHandle(url)) {
                /* Skip stuff we can't handle. */
                continue;
            }
            final String subfolderPath = getPathFromURL(url);
            if (subfolderPath == null) {
                /* Skip unsupported URLs. */
                continue;
            } else if (subfolderPath.endsWith("/prev") || subfolderPath.endsWith("/next") || subfolderPath.endsWith("/0")) {
                /* Skip invalid URLs. */
                continue;
            } else if (linkPath != null && subfolderPath.endsWith(linkPath)) {
                /* Skip invalid URLs. */
                continue;
            } else if (!subfolderPath.matches(Pattern.quote(folderPath) + "/.+")) {
                /* Skip URLs with seemingly invalid path. */
                continue;
            }
            ret.add(this.createDownloadlink(url));
            numberofSubfolders++;
        }
        logger.info("Number of subfolders: " + numberofSubfolders);
        if (ret.isEmpty()) {
            /* Empty folder or broken plugin. */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return ret;
    }

    private String getPathFromURL(final String url) {
        final Regex urlinfo = new Regex(url, this.getSupportedLinks());
        // final String folderID = urlinfo.getMatch(0);
        String folderPath = urlinfo.getMatch(1);
        if (folderPath == null) {
            return null;
        }
        folderPath = Encoding.htmlDecode(folderPath);
        if (folderPath.endsWith("/")) {
            return folderPath.substring(0, folderPath.lastIndexOf("/"));
        } else {
            return folderPath;
        }
    }

    public static Form getPasswordProtectedForm(final Browser br) {
        return br.getFormbyKey("passwd");
    }

    public static boolean isOffline(final Browser br) {
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("class=\"error type404\"|class=\"no_link_available\"")) {
            return true;
        } else {
            return false;
        }
    }
}

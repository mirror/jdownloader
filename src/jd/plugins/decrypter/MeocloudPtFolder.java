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
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/link/([a-f0-9\\-]+)/(.+)");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        final Regex urlinfo = new Regex(param.getCryptedUrl(), this.getSupportedLinks());
        // final String folderID = urlinfo.getMatch(0);
        final String folderPath = Encoding.htmlDecode(urlinfo.getMatch(1));
        br.getPage(param.getCryptedUrl());
        if (isOffline(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Form pwform = MeocloudPtFolder.getPasswordProtectedForm(br);
        if (pwform != null) {
            /* 2020-02-18: PW protected URLs are not yet supported. */
            throw new PluginException(LinkStatus.ERROR_FATAL, "Password protected links are not yet supported: Contact support and ask for implementation", 8 * 60 * 1000l);
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
        // TODO: Add subfolder support
        // final String[] subfolderURLs = br.getRegex("").getColumn(0);
        // if (subfolderURLs == null || subfolderURLs.length == 0) {
        // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // }
        // for (final String singleLink : subfolderURLs) {
        // ret.add(createDownloadlink(singleLink));
        // }
        return ret;
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

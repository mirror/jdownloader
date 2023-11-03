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
import org.appwork.utils.net.URLHelper;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class StorjshareIoCrawler extends PluginForDecrypt {
    public StorjshareIoCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "storjshare.io" });
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
            ret.add("https?://link\\." + buildHostsPatternPart(domains) + "/s/([a-z0-9]{28})/(.+)");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        /** 2023-11-02: Remove all URL parameters since they can enforce a redirect to a direct file download e.g. "?download=1" */
        final String contenturl = URLHelper.getUrlWithoutParams(param.getCryptedUrl());
        final String path = Encoding.htmlDecode(new Regex(contenturl, this.getSupportedLinks()).getMatch(1));
        br.getPage(contenturl);
        if (br.getHttpConnection().getResponseCode() == 403) {
            throw new AccountRequiredException();
        } else if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML("download=1")) {
            /* Single file */
            final String filenameFromURL = new Regex(path, "/([^/]+)$").getMatch(0);
            final DownloadLink singlefile = this.createDownloadlink(DirectHTTP.createURLForThisPlugin(br.getURL("?download=1").toExternalForm()), false);
            singlefile.setName(filenameFromURL);
            final String filesizeStr = br.getRegex("<p class=\"mt-3\"[^>]*>([^<]+)</p>").getMatch(0);
            if (filesizeStr != null) {
                singlefile.setDownloadSize(SizeFormatter.getSize(filesizeStr));
            }
            singlefile.setAvailable(true);
            /* Remove filename from path */
            singlefile.setRelativeDownloadFolderPath(path.replace("/" + filenameFromURL, ""));
            ret.add(singlefile);
        } else {
            /* Folder containing multiple files */
            final String[] htmls = br.getRegex("<a class=\"directory-link\".*?</div>\\s*</a>").getColumn(-1);
            if (htmls == null || htmls.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(path);
            for (final String html : htmls) {
                String url = new Regex(html, "class=\"directory-link\" href=\"([^\"]+)\"").getMatch(0);
                // final String filenameFromURL = new Regex(url, "/([^/]+)$").getMatch(0);
                final String filename = new Regex(html, "class=\"directory-name\"[^>]*>([^<]+)</span>").getMatch(0);
                final String filesizeStr = new Regex(html, "class=\"directory-size\"[^>]*>([^<]+)<").getMatch(0);
                if (url == null || filename == null || filesizeStr == null) {
                    /* Skip invalid elements */
                    continue;
                }
                url = br.getURL(url).toExternalForm();
                url = URLHelper.getUrlWithoutParams(url);
                url += "?download=1";
                final DownloadLink link = createDownloadlink(DirectHTTP.createURLForThisPlugin(url), false);
                if (filename != null) {
                    link.setFinalFileName(Encoding.htmlDecode(filename).trim());
                }
                /*
                 * 2023-11-02: Website displays all files with filesize of "0 B" -> Do not set filesize in this case so it can be obtained
                 * from header during linkcheck.
                 */
                long filesize = 0;
                if (filesizeStr != null) {
                    filesize = SizeFormatter.getSize(filesizeStr);
                }
                if (filesize > 0) {
                    link.setAvailable(true);
                    link.setDownloadSize(filesize);
                } else {
                    logger.info("File will be checked manually to determine real filesize: " + url);
                }
                link.setRelativeDownloadFolderPath(path);
                link._setFilePackage(fp);
                ret.add(link);
            }
        }
        return ret;
    }
}

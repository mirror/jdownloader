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
import org.jdownloader.plugins.components.YetiShareCore;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.components.SiteType.SiteTemplate;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class GenericYetiShareFolder extends antiDDoSForDecrypt {
    public GenericYetiShareFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "letsupload.to", "letsupload.co" });
        ret.add(new String[] { "fireload.com" });
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
            // final String annotationName = domains[0];
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/folder/\\d+(?:/[^<>\"]+)?(?:\\?sharekey=[A-Za-z0-9\\-_]+)?");
        }
        return ret.toArray(new String[0]);
    }

    /**
     * Generic Crawler for YetiShare file-hosts. <br />
     * 2019-04-29: So far, letsupload.co is the only supported host(well they all have folders but it is the first YetiShare host of which
     * we know that has public folders).
     */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        // /*
        // * 2019-06-12: TODO: Their html contains json containing all translations. We might be able to use this for us for better
        // * errorhandling in the future ...
        // */
        // final String there_are_no_files_within_this_folderTEXT = PluginJSonUtils.getJson(br, "there_are_no_files_within_this_folder");
        if (br.getHttpConnection().getResponseCode() == 404 || !br.getURL().contains("/folder/")) {
            /* 2019-04-29: E.g. letsupload.co offline folder --> Redirect to /index.html */
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        } else if (br.containsHTML("<strong>- There are no files within this folder\\.</strong>")) {
            logger.info("Folder is empty");
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        Form folderPasswordForm = getFolderPasswordForm();
        String folderPassword = null;
        if (folderPasswordForm != null) {
            logger.info("Folder seems to be password protected");
            int counter = 0;
            do {
                folderPassword = getUserInput("Password?", param);
                folderPasswordForm.put("folderPassword", folderPassword);
                this.submitForm(folderPasswordForm);
                folderPasswordForm = getFolderPasswordForm();
                counter++;
            } while (counter <= 2 && folderPasswordForm != null);
            if (folderPasswordForm != null) {
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
        }
        String fpNameFallback = new Regex(parameter, "/folder/\\d+/([^/\\?]+)").getMatch(0);
        if (fpNameFallback != null) {
            /* Nicer fallback packagename */
            fpNameFallback = fpNameFallback.replace("_", " ");
        }
        String fpName = br.getRegex("<h2>Files Within Folder \\'([^<>\"\\']+)\\'</h2>").getMatch(0);
        if (fpName == null) {
            fpName = fpNameFallback;
        }
        final String tableHTML = br.getRegex("<table id=\"fileData\".*?</table>").getMatch(-1);
        final String[] urls;
        if (tableHTML != null) {
            urls = new Regex(tableHTML, "<tr>.*?</tr>").getColumn(-1);
        } else {
            urls = br.getRegex("href=\"(https?://[^<>/]+/[A-Za-z0-9]+(?:/[^<>/]+)?)\" target=\"_blank\"").getColumn(0);
        }
        if (urls == null || urls.length == 0) {
            logger.warning("Failed to find any content");
            return null;
        }
        for (final String urlInfo : urls) {
            String url = null, filename = null, filesize = null;
            if (urlInfo.startsWith("http")) {
                url = urlInfo;
            } else {
                final Regex finfo = new Regex(urlInfo, "target=\"_blank\">([^<>\"]+)</a>\\&nbsp;\\&nbsp;\\((\\d+(?:\\.\\d{1,2})? [A-Z]+)\\)<br/>");
                url = new Regex(urlInfo, "href=\"(https?://[^<>/]+/[A-Za-z0-9]+(?:/[^<>/\"]+)?)\"").getMatch(0);
                filename = finfo.getMatch(0);
                filesize = finfo.getMatch(1);
            }
            if (url == null) {
                continue;
            } else if (new Regex(url, this.getSupportedLinks()).matches()) {
                /* Skip URLs which would go into this crawler again */
                continue;
            }
            final DownloadLink dl = createDownloadlink(url);
            if (filename != null) {
                filename = Encoding.htmlDecode(filename);
                dl.setName(filename);
            } else {
                final String url_name = YetiShareCore.getFilenameFromURL(url);
                /* No filename information given? Use either fuid or name from inside URL. */
                if (url_name != null) {
                    dl.setName(url_name);
                }
            }
            if (filesize != null) {
                dl.setDownloadSize(SizeFormatter.getSize(filesize));
            }
            /* 2019-07-28: Given filename + filesize information does not imply that URLs are online. Example: firedrop.com */
            // if (filename != null && filesize != null) {
            // /* 2019-04-29: Assume all files in a folder with filename&filesize are ONline - TODO: Verify this assumption! */
            // dl.setAvailable(true);
            // }
            if (folderPassword != null) {
                /*
                 * 2019-06-12: URLs in password protected folders are not necessarily password protected (which is kinda stupid) as well but
                 * chances are there so let's set the folder password as single download password just in case.
                 */
                dl.setDownloadPassword(folderPassword);
            }
            decryptedLinks.add(dl);
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private Form getFolderPasswordForm() {
        return br.getFormbyKey("folderPassword");
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.MFScripts_YetiShare;
    }
}

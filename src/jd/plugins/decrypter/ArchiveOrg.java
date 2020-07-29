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
import java.util.regex.Pattern;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.config.ArchiveOrgConfig;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "archive.org" }, urls = { "https?://(?:www\\.)?archive\\.org/(?:details|download|stream)/(?!copyrightrecords)@?.+" })
public class ArchiveOrg extends PluginForDecrypt {
    public ArchiveOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return ArchiveOrgConfig.class;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("://www.", "://").replace("/stream/", "/download/");
        final String host_decrypted = "archivedecrypted.org";
        /*
         * 2017-01-25: We do not (yet) have to be logged in here. We can always see all items and their information but some may be limited
         * to premium users only
         */
        // final Account aa = AccountController.getInstance().getValidAccount(JDUtilities.getPluginForHost(this.getHost()));
        // if (aa != null) {
        // jd.plugins.hoster.ArchiveOrg.login(this.br, aa, false);
        // }
        URLConnectionAdapter con = null;
        try {
            /* Check if we have a direct URL --> Host plugin */
            con = br.openGetConnection(parameter);
            /*
             * 2020-03-04: E.g. directurls will redirect to subdomain e.g. ia800503.us.archive.org --> Sometimes the only way to differ
             * between a file or expected html.
             */
            final String host = Browser.getHost(con.getURL(), true);
            if (con.isContentDisposition() || con.getLongContentLength() > br.getLoadLimit() || !host.equals("archive.org")) {
                final DownloadLink fina = this.createDownloadlink(parameter.replace("archive.org", host_decrypted));
                if (con.getLongContentLength() > 0) {
                    fina.setDownloadSize(con.getLongContentLength());
                }
                fina.setFinalFileName(getFileNameFromHeader(con));
                fina.setAvailable(true);
                decryptedLinks.add(fina);
                return decryptedLinks;
            } else {
                br.followConnection();
            }
        } finally {
            con.disconnect();
        }
        br.getPage(parameter);
        if (StringUtils.containsIgnoreCase(parameter, "/details/")) {
            int page = 2;
            do {
                if (br.containsHTML("This item is only available to logged in Internet Archive users")) {
                    decryptedLinks.add(createDownloadlink(parameter.replace("/details/", "/download/")));
                    break;
                }
                final String showAll = br.getRegex("href=\"(/download/[^\"]*?)\">SHOW ALL").getMatch(0);
                if (showAll != null) {
                    decryptedLinks.add(createDownloadlink(br.getURL(showAll).toString()));
                    logger.info("Creating: " + br.getURL(showAll).toString());
                    break;
                }
                final String[] details = br.getRegex("<div class=\"item-ia\".*? <a href=\"(/details/[^\"]*?)\" title").getColumn(0);
                if (details == null || details.length == 0) {
                    break;
                }
                for (final String detail : details) {
                    final DownloadLink link = createDownloadlink(br.getURL(detail).toString());
                    decryptedLinks.add(link);
                    if (isAbort()) {
                        break;
                    } else {
                        distribute(link);
                    }
                }
                br.getPage("?page=" + (page++));
            } while (!this.isAbort());
        } else {
            if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">The item is not available")) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            if (!br.containsHTML("\"/download/")) {
                logger.info("Maybe invalid link or nothing there to download: " + parameter);
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            final boolean preferOriginal = PluginJsonConfig.get(ArchiveOrgConfig.class).isPreferOriginal();
            String subfolderPath = new Regex(parameter, "https?://[^/]+/download/(.*?)/?$").getMatch(0);
            subfolderPath = Encoding.urlDecode(subfolderPath, false);
            // final String fpName = br.getRegex("<h1>Index of [^<>\"]+/([^<>\"/]+)/?</h1>").getMatch(0);
            final String fpName = subfolderPath;
            String html = br.toString().replaceAll("(\\(\\s*<a.*?</a>\\s*\\))", "");
            final String[] htmls = new Regex(html, "<tr >(.*?)</tr>").getColumn(0);
            final String xmlurl = br.getRegex("<a href=\"([^<>\"]+_files\\.xml)\"").getMatch(0);
            String xmlSource = null;
            if (xmlurl != null) {
                final Browser brc = br.cloneBrowser();
                xmlSource = brc.getPage(brc.getURL() + "/" + xmlurl);
                final String[] items = new Regex(xmlSource, "<file(.*?)</file>").getColumn(0);
                /*
                 * 2020-03-04: Prefer crawling xml if possible as we then get all contents of that folder including contents of subfolders
                 * via only one request!
                 */
                for (final String item : items) {
                    /* <old_version>true</old_version> */
                    final boolean isOldVersion = item.contains("old_version");
                    final boolean isOriginal = item.contains("source=\"original\"");
                    final boolean isMetadata = item.contains("<format>Metadata</format>");
                    String name = new Regex(item, "name=\"([^\"]+)").getMatch(0);
                    final String filesizeStr = new Regex(item, "<size>(\\d+)</size>").getMatch(0);
                    final String sha1hash = new Regex(item, "<sha1>([a-f0-9]+)</sha1>").getMatch(0);
                    if (name == null) {
                        continue;
                    } else if (isOldVersion || isMetadata) {
                        /* Skip old elements and metadata! They are invisible to the user anyways */
                        continue;
                    } else if (preferOriginal && !isOriginal) {
                        /* Skip non-original content if user only wants original content. */
                        continue;
                    }
                    if (Encoding.isHtmlEntityCoded(name)) {
                        /* Will sometimes contain "&amp;" */
                        name = Encoding.htmlDecode(name);
                    }
                    final DownloadLink fina = createDownloadlink("https://" + host_decrypted + "/download/" + subfolderPath + "/" + URLEncode.encodeURIComponent(name));
                    fina.setDownloadSize(SizeFormatter.getSize(filesizeStr));
                    fina.setAvailable(true);
                    final String filename;
                    if (name.contains("/")) {
                        /* Remove foldername/path from item name --> Nice filename */
                        filename = name.substring(name.lastIndexOf("/") + 1);
                    } else {
                        filename = name;
                    }
                    fina.setFinalFileName(filename);
                    final String subfolderPathInName = new Regex(name, "(.+)/[^/]+$").getMatch(0);
                    final String thisPath;
                    if (subfolderPathInName != null) {
                        thisPath = subfolderPath + "/" + subfolderPathInName;
                        // fina.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, subfolderPath + "/" + subfolderPathInName);
                    } else {
                        thisPath = subfolderPath;
                        // fina.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, subfolderPath);
                    }
                    fina.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, thisPath);
                    final FilePackage fp = FilePackage.getInstance();
                    fp.setName(thisPath);
                    fina._setFilePackage(fp);
                    if (sha1hash != null) {
                        fina.setSha1Hash(sha1hash);
                    }
                    decryptedLinks.add(fina);
                }
                return decryptedLinks;
            }
            /* Old/harder way */
            for (final String htmlsnippet : htmls) {
                String name = new Regex(htmlsnippet, "<a href=\"([^<>\"]+)\"").getMatch(0);
                final String[] rows = new Regex(htmlsnippet, "<td>(.*?)</td>").getColumn(0);
                if (name == null || rows.length < 3) {
                    /* Skip invalid items */
                    continue;
                }
                String filesize = rows[rows.length - 1];
                if (StringUtils.endsWithCaseInsensitive(name, "_files.xml") || StringUtils.endsWithCaseInsensitive(name, "_meta.sqlite") || StringUtils.endsWithCaseInsensitive(name, "_meta.xml") || StringUtils.endsWithCaseInsensitive(name, "_reviews.xml")) {
                    /* Skip invalid content */
                    continue;
                } else if (xmlSource != null && preferOriginal) {
                    /* Skip non-original content if user only wants original content. */
                    if (!new Regex(xmlSource, "<file name=\"" + Pattern.quote(name) + "\" source=\"original\"").matches()) {
                        continue;
                    }
                }
                if (filesize.equals("-")) {
                    /* Folder --> Goes back into decrypter */
                    final DownloadLink fina = createDownloadlink("https://archive.org/download/" + subfolderPath + "/" + name);
                    decryptedLinks.add(fina);
                } else {
                    /* File */
                    filesize += "b";
                    final String filename = Encoding.urlDecode(name, false);
                    final DownloadLink fina = createDownloadlink("https://" + host_decrypted + "/download/" + subfolderPath + "/" + name);
                    fina.setDownloadSize(SizeFormatter.getSize(filesize));
                    fina.setAvailable(true);
                    fina.setFinalFileName(filename);
                    if (xmlSource != null) {
                        final String sha1 = new Regex(xmlSource, "<file name=\"" + Pattern.quote(filename) + "\".*?<sha1>([a-f0-9]{40})</sha1>").getMatch(0);
                        if (sha1 != null) {
                            fina.setSha1Hash(sha1);
                        }
                        final String size = new Regex(xmlSource, "<file name=\"" + Pattern.quote(filename) + "\".*?<size>(\\d+)</size>").getMatch(0);
                        if (size != null) {
                            fina.setVerifiedFileSize(Long.parseLong(size));
                        }
                    }
                    fina.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, subfolderPath);
                    decryptedLinks.add(fina);
                }
            }
            /* 2020-03-04: Setting packagenames makes no sense anymore as packages will get split by subfolderpath. */
            final FilePackage fp = FilePackage.getInstance();
            if (fpName != null) {
                fp.setName(fpName);
                fp.addLinks(decryptedLinks);
            }
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}
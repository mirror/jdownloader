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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.config.ArchiveOrgConfig;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "archive.org", "subdomain.archive.org" }, urls = { "https?://(?:www\\.)?archive\\.org/(?:details|download|stream|embed)/(?!copyrightrecords)@?.+", "https?://[^/]+\\.archive\\.org/view_archive\\.php\\?archive=[^\\&]+" })
public class ArchiveOrg extends PluginForDecrypt {
    public ArchiveOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return ArchiveOrgConfig.class;
    }

    private boolean isArchiveURL(final String url) {
        return url != null && url.contains("view_archive.php");
    }

    private final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
    final Set<String>                     dups           = new HashSet<String>();
    final String                          host_decrypted = "archivedecrypted.org";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        final String parameter = param.toString().replace("://www.", "://").replaceFirst("/(stream|embed)/", "/download/");
        /*
         * 2020-08-26: Login might sometimes be required for book downloads.
         */
        final Account aa = AccountController.getInstance().getValidAccount(JDUtilities.getPluginForHost("archive.org"));
        if (aa != null) {
            final PluginForHost plg = JDUtilities.getPluginForHost("archive.org");
            plg.setBrowser(this.br);
            ((jd.plugins.hoster.ArchiveOrg) plg).login(aa, false);
        }
        URLConnectionAdapter con = null;
        boolean isArchiveContent = isArchiveURL(parameter);
        if (isArchiveContent) {
            br.getPage(parameter);
        } else {
            try {
                /* Check if we have a direct URL --> Host plugin */
                con = br.openGetConnection(parameter);
                isArchiveContent = con.getURL().toString().contains("view_archive.php");
                /*
                 * 2020-03-04: E.g. directurls will redirect to subdomain e.g. ia800503.us.archive.org --> Sometimes the only way to differ
                 * between a file or expected html.
                 */
                final String host = Browser.getHost(con.getURL(), true);
                if ((con.isContentDisposition() || con.getLongContentLength() > br.getLoadLimit() || !host.equals("archive.org")) && !isArchiveContent) {
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
        }
        /**
         * 2021-02-01: Disabled book crawling because it's not working as intended and "/details/" URLs can also contain embedded pages of
         * books while at the same time, the original files are downloadable! E.g. https://board.jdownloader.org/showthread.php?t=86605
         */
        final boolean allowBookCrawling = false;
        if (allowBookCrawling && br.containsHTML("schema\\.org/Book")) {
            /* Crawl all pages of a book */
            final String bookAjaxURL = br.getRegex("\\'([^\\'\"]+BookReaderJSIA\\.php\\?[^\\'\"]+)\\'").getMatch(0);
            if (bookAjaxURL == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage(bookAjaxURL);
            if (br.getHttpConnection().getResponseCode() == 404) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            entries = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "data/brOptions");
            final String title = (String) entries.get("bookTitle");
            final ArrayList<Object> imagesO = (ArrayList<Object>) entries.get("data");
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(title);
            for (final Object imageO : imagesO) {
                /*
                 * Most of all objects will contain an array with 2 items --> Books always have two viewable pages. Exception = First page
                 * --> Cover
                 */
                final ArrayList<Object> pagesO = (ArrayList<Object>) imageO;
                for (final Object pageO : pagesO) {
                    /* Grab "Preview"(???) version --> Usually "pageType":"NORMAL", "pageSide":"L", "viewable":true */
                    entries = (Map<String, Object>) pageO;
                    final int pageNum = (int) JavaScriptEngineFactory.toLong(entries.get("leafNum"), -1);
                    final String url = (String) entries.get("uri");
                    if (StringUtils.isEmpty(url) || pageNum == -1) {
                        /* Skip invalid items */
                        continue;
                    }
                    final DownloadLink dl = this.createDownloadlink("directhttp://" + url);
                    dl.setName(pageNum + "_ " + title + ".jpg");
                    /* Assume all are online & downloadable */
                    dl.setAvailable(true);
                    dl._setFilePackage(fp);
                    decryptedLinks.add(dl);
                }
            }
        } else if (isArchiveContent) {
            /* 2020-09-07: Contents of a .zip file are also accessible and downloadable separately. */
            final String archiveName = new Regex(br.getURL(), ".*/([^/]+)$").getMatch(0);
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(archiveName));
            final String[] htmls = br.getRegex("<tr><td>(.*?)</tr>").getColumn(0);
            for (final String html : htmls) {
                String url = new Regex(html, "(/download/[^\"\\']+)").getMatch(0);
                final String filesizeStr = new Regex(html, "id=\"size\">(\\d+)").getMatch(0);
                if (StringUtils.isEmpty(url)) {
                    /* Skip invalid items */
                    continue;
                }
                url = "https://archive.org" + url;
                final DownloadLink dl = this.createDownloadlink(url);
                if (filesizeStr != null) {
                    dl.setDownloadSize(Long.parseLong(filesizeStr));
                }
                dl.setAvailable(true);
                dl._setFilePackage(fp);
                decryptedLinks.add(dl);
            }
        } else if (StringUtils.containsIgnoreCase(parameter, "/details/")) {
            if (br.containsHTML("id=\"gamepadtext\"")) {
                /* 2020-09-29: Rare case: Download browser emulated games */
                final String subfolderPath = new Regex(parameter, "/details/([^/]+)").getMatch(0);
                br.getPage("https://archive.org/download/" + subfolderPath + "/" + subfolderPath + "_files.xml");
                this.crawlXML(this.br, subfolderPath);
                return this.decryptedLinks;
            }
            /** TODO: 2020-09-29: Consider taking the shortcut here to always use that XML straight away (?!) */
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
            final String xmlURLs[] = br.getRegex("<a href\\s*=\\s*\"([^<>\"]+_files\\.xml)\"").getColumn(0);
            String xmlSource = null;
            if (xmlURLs != null && xmlURLs.length > 0) {
                for (String xmlURL : xmlURLs) {
                    final Browser brc = br.cloneBrowser();
                    brc.setFollowRedirects(true);
                    xmlSource = brc.getPage(brc.getURL() + "/" + xmlURL);
                    this.crawlXML(brc, subfolderPath);
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

    private void crawlXML(final Browser br, final String subfolderPath) {
        final boolean preferOriginal = PluginJsonConfig.get(ArchiveOrgConfig.class).isPreferOriginal();
        final String[] items = new Regex(br.toString(), "<file\\s*(.*?)\\s*</file>").getColumn(0);
        /*
         * 2020-03-04: Prefer crawling xml if possible as we then get all contents of that folder including contents of subfolders via only
         * one request!
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
            final String url = "https://" + host_decrypted + "/download/" + subfolderPath + "/" + URLEncode.encodeURIComponent(name);
            if (dups.add(url)) {
                final DownloadLink fina = createDownloadlink(url);
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
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}
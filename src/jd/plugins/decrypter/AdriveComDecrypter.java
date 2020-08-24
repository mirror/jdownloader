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

import java.net.URL;
import java.util.ArrayList;
import java.util.Random;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "adrive.com" }, urls = { "https?://(?:www(?:\\d+)?\\.)?adrive\\.com/public/([0-9a-zA-Z]+)(\\?path=[^\\&]+)?" })
public class AdriveComDecrypter extends PluginForDecrypt {
    public AdriveComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        final String parameter = param.toString();
        final String fid = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        if (fid == null) {
            /* This should never happen! */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final UrlQuery query = UrlQuery.parse(parameter);
        String subfolderPath = query.get("path");
        String iframe = null;
        final String subdomain = new Regex(parameter, "https?://(www\\d+[^/]+)/").getMatch(0);
        if (subdomain != null && !StringUtils.isEmpty(subfolderPath)) {
            /*
             * Workaround required: Subdomain/server given via users' URL may have changed in the meantime --> Access root folder --> Get
             * new/working server/subdomain --> Then access desired subfolder
             */
            br.getPage("https://www." + this.getHost() + "/public/" + fid);
            if (br.getHttpConnection().getResponseCode() == 404 || !br.containsHTML(fid)) {
                decryptedLinks.add(createOfflinelink(parameter, new Regex(parameter, "adrive\\.com/public/(.+)").getMatch(0)));
                return decryptedLinks;
            }
            iframe = br.getRegex("<iframe[^>]*src=\"(https?://\\w+\\.adrive.com/public/(?:view/)?[^\"]+\\.html)\"").getMatch(0);
            if (iframe == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Now access subfolder with new subdomain else it will fail ... */
            final String currentSubdomain = Browser.getHost(iframe, true);
            br.getPage("https://" + currentSubdomain + "/public/" + fid + "?path=" + subfolderPath);
        } else {
            br.getPage(parameter);
            iframe = br.getRegex("<iframe[^>]*src=\"(https?://\\w+\\.adrive.com/public/(?:view/)?[^\"]+\\.html)\"").getMatch(0);
            if (iframe != null) {
                if (br.containsHTML("The file you are trying to access is no longer available publicly\\.|The public file you are trying to download is associated with a non\\-valid ADrive") || br.getURL().equals("https://www.adrive.com/login") || iframe == null) {
                    decryptedLinks.add(createOfflinelink(parameter, new Regex(parameter, "adrive\\.com/public/(.+)").getMatch(0)));
                    return decryptedLinks;
                }
                // continue links can be direct download links
                URLConnectionAdapter con = null;
                try {
                    con = br.openGetConnection(iframe);
                    if (con.getContentType().contains("html")) {
                        br.followConnection();
                    } else {
                        String filename = getFileNameFromHeader(con);
                        long filesize = con.getContentLength();
                        if (filename != null && filesize > 0) {
                            final DownloadLink dl = createDownloadlink("http://adrivedecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(100000));
                            dl.setDownloadSize(filesize);
                            dl.setFinalFileName(filename);
                            dl.setProperty("LINKDUPEID", "adrivecom://" + fid);
                            dl.setProperty("mainlink", parameter);
                            dl.setProperty("directlink", br.getURL());
                            dl.setAvailable(true);
                            decryptedLinks.add(dl);
                            return decryptedLinks;
                        }
                    }
                } finally {
                    try {
                        con.disconnect();
                    } catch (Throwable e) {
                    }
                }
                if (br.getHttpConnection().getResponseCode() == 404 || br.toString().matches("<b>File doesn't exist\\. Please turn on javascript\\.</b>\\s*<script> window\\.top.location=\"http://www\\.adrive\\.com/public/noexist\"; </script>")) {
                    decryptedLinks.add(createOfflinelink(parameter, new Regex(parameter, "adrive\\.com/public/(.+)").getMatch(0)));
                    return decryptedLinks;
                }
            }
        }
        if (br.getHttpConnection().getResponseCode() == 404 || !br.containsHTML(fid)) {
            decryptedLinks.add(createOfflinelink(parameter, new Regex(parameter, "adrive\\.com/public/(.+)").getMatch(0)));
            return decryptedLinks;
        } else if (br.containsHTML("\"noFiles\"")) {
            logger.info("Empty folder");
            decryptedLinks.add(createOfflinelink(parameter, new Regex(parameter, "adrive\\.com/public/(.+)").getMatch(0)));
            return decryptedLinks;
        } else if (br.containsHTML("id=\"startdownload\"")) {
            /* Special case: Single file, direct download */
            final DownloadLink dl = createDownloadlink("http://adrivedecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(100000));
            dl.setProperty("LINKDUPEID", "adrivecom://" + fid);
            dl.setProperty("mainlink", parameter);
            dl.setProperty("directlink", br.getURL());
            dl.setProperty("directdl", true);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        /*
         * E.g. required when user adds the root folder --> Path is not given in URL and we need to find the name of the current (=
         * root-)folder!
         */
        final String currentPath = br.getRegex("class=\"top\">\\s*<a href=\"/public/[^\"]+\\?path=([^\\&\"]+).*\"").getMatch(0);
        if (StringUtils.isEmpty(subfolderPath) && !StringUtils.isEmpty(currentPath)) {
            subfolderPath = currentPath;
        }
        final String linktext = br.getRegex("<table>(.*?)</table>").getMatch(0);
        if (linktext == null) {
            // not always defect! -raztoki20160119
            if (br.toString().startsWith("<b>File overlimit.")) {
                // we can assume its a SINGLE file! we wont know its size
                final DownloadLink dl = createDownloadlink("http://adrivedecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(100000));
                final String filename = getFileNameFromURL(new URL(iframe));
                dl.setFinalFileName(filename);
                dl.setProperty("LINKDUPEID", "adrivecom://" + fid + "/" + filename);
                dl.setProperty("mainlink", parameter);
                dl.setProperty("directlink", iframe);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final String[] links = new Regex(linktext, "<tr>(.*?)</tr>").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (final String singleinfo : links) {
            if (singleinfo.contains("/folder.png")) {
                String folderURL = new Regex(singleinfo, "\"(/public/[A-Za-z0-9]+\\?path=[^\"]+)\"").getMatch(0);
                if (folderURL == null) {
                    continue;
                }
                folderURL = br.getURL(folderURL).toString();
                decryptedLinks.add(this.createDownloadlink(folderURL));
            } else {
                final Regex info = new Regex(singleinfo, "<td class=\"name\">[\t\n\r ]+<a href=\"(https?://download[^<>\"]*?)\">([^<>\"]*?)</a>");
                final String directlink = info.getMatch(0);
                String filename = info.getMatch(1);
                final String filesize = new Regex(singleinfo, "<td class=\"size\">([^<>\"]*?)</td>").getMatch(0);
                if (filename == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                filename = Encoding.htmlDecode(filename.trim());
                final DownloadLink dl = createDownloadlink("http://adrivedecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(100000));
                if (filesize == null || directlink == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                dl.setDownloadSize(SizeFormatter.getSize(filesize));
                dl.setFinalFileName(filename);
                dl.setProperty("LINKDUPEID", "adrivecom://" + fid + "/" + filename);
                dl.setProperty("mainlink", parameter);
                dl.setProperty("directlink", directlink);
                dl.setAvailable(true);
                if (!StringUtils.isEmpty(subfolderPath)) {
                    dl.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, Encoding.htmlDecode(subfolderPath));
                }
                decryptedLinks.add(dl);
            }
        }
        return decryptedLinks;
    }
}

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
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "adrive.com" }, urls = { "https?://(?:www(?:\\d+)?\\.)?adrive\\.com/public/([0-9a-zA-Z]+)(\\?path=[^\\&]+)?" })
public class AdriveComDecrypter extends PluginForDecrypt {
    public AdriveComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final PluginForHost hostPlg = this.getNewPluginForHostInstance(this.getHost());
        br.setFollowRedirects(true);
        final String fid = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        if (fid == null) {
            /* This should never happen! */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final UrlQuery query = UrlQuery.parse(param.getCryptedUrl());
        String subfolderPath = query.get("path");
        String iframe = null;
        final String subdomain = new Regex(param.getCryptedUrl(), "https?://(www\\d+[^/]+)/").getMatch(0);
        if (subdomain != null && !StringUtils.isEmpty(subfolderPath)) {
            /*
             * Workaround required: Subdomain/server given via users' URL may have changed in the meantime --> Access root folder --> Get
             * new/working server/subdomain --> Then access desired subfolder
             */
            br.getPage("https://www." + this.getHost() + "/public/" + fid);
            if (br.getHttpConnection().getResponseCode() == 404 || !br.containsHTML(fid)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            iframe = br.getRegex("<iframe[^>]*src=\"(https?://\\w+\\.adrive.com/public/(?:view/)?[^\"]+\\.html)\"").getMatch(0);
            if (iframe == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Now access subfolder with new subdomain else it will fail ... */
            final String currentSubdomain = Browser.getHost(iframe, true);
            br.getPage("https://" + currentSubdomain + "/public/" + fid + "?path=" + subfolderPath);
        } else {
            br.getPage(param.getCryptedUrl());
            iframe = br.getRegex("<iframe[^>]*src=\"(https?://\\w+\\.adrive.com/public/(?:view/)?[^\"]+\\.html)\"").getMatch(0);
            if (iframe != null) {
                if (br.containsHTML("(?i)The file you are trying to access is no longer available publicly\\.|The public file you are trying to download is associated with a non\\-valid ADrive") || br.getURL().equals("https://www.adrive.com/login") || iframe == null) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                // continue links can be direct download links
                URLConnectionAdapter con = null;
                try {
                    con = br.openGetConnection(iframe);
                    if (con.getContentType().contains("html")) {
                        br.followConnection();
                    } else {
                        String filename = getFileNameFromHeader(con);
                        long filesize = con.getCompleteContentLength();
                        if (filename != null && filesize > 0) {
                            final DownloadLink dl = new DownloadLink(hostPlg, this.getHost(), this.getHost(), br.getURL(), true);
                            dl.setDownloadSize(filesize);
                            dl.setFinalFileName(filename);
                            dl.setLinkID("adrivecom://" + fid);
                            dl.setProperty("mainlink", param.getCryptedUrl());
                            dl.setProperty("directlink", br.getURL());
                            dl.setAvailable(true);
                            ret.add(dl);
                            return ret;
                        }
                    }
                } finally {
                    try {
                        con.disconnect();
                    } catch (Throwable e) {
                    }
                }
                if (br.getHttpConnection().getResponseCode() == 404 || br.toString().matches("<b>File doesn't exist\\. Please turn on javascript\\.</b>\\s*<script> window\\.top.location=\"http://www\\.adrive\\.com/public/noexist\"; </script>")) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            }
        }
        if (br.getHttpConnection().getResponseCode() == 404 || !br.containsHTML(fid)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("\"noFiles\"")) {
            throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER);
        } else if (br.containsHTML("id=\"startdownload\"")) {
            /* Special case: Single file, direct download */
            final DownloadLink dl = new DownloadLink(hostPlg, this.getHost(), this.getHost(), br.getURL(), true);
            dl.setLinkID("adrivecom://" + fid);
            dl.setProperty("mainlink", param.getCryptedUrl());
            dl.setProperty("directlink", br.getURL());
            dl.setProperty("directdl", true);
            ret.add(dl);
            return ret;
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
            if (br.containsHTML("class=\"error-msg\"") || br.getURL().matches("^https?://[^/]+/?$")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String[] links = new Regex(linktext, "<tr>(.*?)</tr>").getColumn(0);
        if (links == null || links.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (final String singleinfo : links) {
            if (singleinfo.contains("/folder.png")) {
                String folderURL = new Regex(singleinfo, "\"(/public/[A-Za-z0-9]+\\?path=[^\"]+)\"").getMatch(0);
                if (folderURL == null) {
                    continue;
                }
                folderURL = br.getURL(folderURL).toString();
                ret.add(this.createDownloadlink(folderURL));
            } else {
                final Regex info = new Regex(singleinfo, "<td class=\"name\">[\t\n\r ]+<a href=\"(https?://download[^<>\"]*?)\">([^<>\"]*?)</a>");
                final String directlink = info.getMatch(0);
                String filename = info.getMatch(1);
                final String filesize = new Regex(singleinfo, "<td class=\"size\">([^<>\"]*?)</td>").getMatch(0);
                if (filename == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                filename = Encoding.htmlDecode(filename.trim());
                final DownloadLink dl = new DownloadLink(hostPlg, this.getHost(), this.getHost(), directlink, true);
                if (filesize == null || directlink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                dl.setDownloadSize(SizeFormatter.getSize(filesize));
                dl.setFinalFileName(filename);
                dl.setLinkID("adrivecom://" + fid + "/" + filename);
                dl.setProperty("mainlink", param.getCryptedUrl());
                dl.setProperty("directlink", directlink);
                dl.setAvailable(true);
                if (!StringUtils.isEmpty(subfolderPath)) {
                    dl.setRelativeDownloadFolderPath(Encoding.htmlDecode(subfolderPath));
                }
                ret.add(dl);
            }
        }
        return ret;
    }
}

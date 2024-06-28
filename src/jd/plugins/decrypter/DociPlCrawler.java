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
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.DociPl;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "doci.pl" }, urls = { "https?://(?:www\\.)?doci\\.pl/[^\\?\\&]+" })
public class DociPlCrawler extends PluginForDecrypt {
    public DociPlCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        DociPl.prepBR(this.br);
        final PluginForHost plg = this.getNewPluginForHostInstance(this.getHost());
        final String contenturl = param.getCryptedUrl().replaceFirst("(?i)http://", "https://");
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final boolean useJsonEndpoint = false;
        if (useJsonEndpoint && DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            final Regex urlinfo = new Regex(contenturl, "(?i)https?://[^/]+/([^/]+)/.*(d|f)([a-z0-9]+)$");
            final String username = urlinfo.getMatch(0);
            final String fileFolderCode = urlinfo.getMatch(2);
            if (username == null || fileFolderCode == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getHeaders().put("Referer", contenturl);
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
            br.getPage("https://" + getHost() + "/file/file_data/get/" + username + "/" + fileFolderCode);
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final Map<String, Object> response = (Map<String, Object>) entries.get("response");
            final Map<String, Object> data = (Map<String, Object>) response.get("data");
            final Map<String, Object> file_data = (Map<String, Object>) data.get("file_data");
            if (file_data != null) {
                /* The current url is a single file ... */
                final Object fileSize = file_data.get("fileSize");
                final DownloadLink dl = this.createDownloadlink(contenturl);
                dl.setDefaultPlugin(plg);
                dl.setHost(plg.getHost());
                dl.setProperty(DociPl.PROPERTY_FILE_ID, file_data.get("fileID"));
                dl.setFinalFileName(file_data.get("fileName") + "." + file_data.get("fileExtension"));
                if (fileSize != null) {
                    dl.setDownloadSize(Long.parseLong(fileSize.toString()));
                }
                dl.setAvailable(true);
                ret.add(dl);
                return ret;
            }
            /* TODO: Add folder handling */
        } else {
            br.getPage(contenturl);
            if (DociPl.isOffline(this.br)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String title = br.getRegex("<title>([^<>\"]+)(?:\\- Doci\\.pl)?</title>").getMatch(0);
            final String docid = DociPl.getDocumentID(this.br);
            if (docid != null) {
                /* The current url is a single file ... */
                final DownloadLink dl = this.createDownloadlink(contenturl);
                dl.setDefaultPlugin(plg);
                dl.setHost(plg.getHost());
                DociPl.setDownloadlinkInformation(this.br, dl);
                dl.setAvailable(true);
                ret.add(dl);
                return ret;
            }
            /* Crawl subfolders */
            final String[] folders = br.getRegex("<article\\s*class\\s*=\\s*\"elem\"\\s*>\\s*<header>\\s*<img[^<>]*?dir[^<>]*?>\\s*<p[^<>]*?>\\s*<a href=\"(/[^<>\"]+)\"").getColumn(0);
            for (final String singleLink : folders) {
                final String url = br.getURL(singleLink).toExternalForm();
                ret.add(createDownloadlink(url));
            }
            /* Crawl files */
            final String[][] files = br.getRegex("class=\"text\\-ellipsis elipsis\\-file\"[^>]*?><a href=\"(/[^<>\"]+)\"\\s*>\\s*(.*?)\\s*<.*?Rozmiar\\s*:\\s*([0-9\\.]+\\s*[GKM]*B)").getMatches();
            if (files == null || files.length == 0) {
                if (folders != null && folders.length > 0) {
                    return ret;
                }
                logger.info("Failed to find any downloadable content");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            for (final String singleLink[] : files) {
                String filename = singleLink[1];
                filename = this.applyFilenameExtension(filename, ".pdf");
                final String url = br.getURL(singleLink[0]).toExternalForm();
                final DownloadLink link = createDownloadlink(url);
                link.setDefaultPlugin(plg);
                link.setHost(plg.getHost());
                link.setAvailable(true);
                link.setName(filename);
                link.setDownloadSize(SizeFormatter.getSize(singleLink[2]));
                ret.add(link);
            }
            if (ret.isEmpty()) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final FilePackage fp = FilePackage.getInstance();
            if (title != null) {
                title = Encoding.htmlDecode(title).trim();
                title = title.replaceFirst("(?i) - doci\\.pl$", "");
                fp.setName(title);
            } else {
                /* Fallback */
                fp.setName(br._getURL().getPath());
            }
            fp.addLinks(ret);
        }
        return ret;
    }
}

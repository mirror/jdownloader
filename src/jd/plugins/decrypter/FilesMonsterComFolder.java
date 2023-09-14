//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.HashSet;

import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.FilesMonsterCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filesmonster.com" }, urls = { "https?://(?:www\\.)?filesmonster\\.com/folders\\.php\\?fid=([0-9a-zA-Z_-]{22,}|\\d+)" })
public class FilesMonsterComFolder extends PluginForDecrypt {
    public FilesMonsterComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final String parameter = param.getCryptedUrl();
        final String folderID = UrlQuery.parse(param.getCryptedUrl()).get("fid");
        if (StringUtils.isEmpty(folderID)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        FilesMonsterCom.prepBR(br);
        br.setFollowRedirects(true);
        br.setCookiesExclusive(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        FilePackage fp = null;
        String title = br.getRegex(">\\s*Folder title:\\s*</td>\\s*<td>([^<]+)</td>").getMatch(0);
        if (title != null) {
            title = Encoding.htmlDecode(title).trim();
            fp = FilePackage.getInstance();
            fp.setName(title);
        }
        final String baseurl = br.getURL();
        int page = 1;
        final int offsetIncreasePerPage = 50;
        int offsetMax = 0;
        final String[] pageValues = br.getRegex(folderID + "&s=(\\d+)").getColumn(0);
        for (final String pageValueTmp : pageValues) {
            final int pageValueTmpInt = Integer.parseInt(pageValueTmp);
            if (pageValueTmpInt > offsetMax) {
                offsetMax = pageValueTmpInt;
            }
        }
        int offset = 0;
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final HashSet<String> dupes = new HashSet<String>();
        do {
            String[] urls = br.getRegex("<a[^>]*href=\"(https?://[\\w\\.\\d]*?filesmonster\\.com/(download|folders)\\.php.*?)\">").getColumn(0);
            if (urls == null || urls.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            int numberofNewItems = 0;
            for (final String url : urls) {
                // prevent regex from finding itself, this is incase they change layout and creates infinite loop.
                if (!url.contains(folderID) && dupes.add(url)) {
                    final DownloadLink file = createDownloadlink(url);
                    if (fp != null) {
                        file._setFilePackage(fp);
                    }
                    ret.add(file);
                    distribute(file);
                    numberofNewItems++;
                }
            }
            logger.info("Crawled page " + page + " | Found items so far: " + ret.size());
            if (numberofNewItems == 0) {
                logger.info("Stopping because: Failed to find any new items on current page");
                break;
            } else if (offset >= offsetMax) {
                logger.info("Stopping because: Reached end-offset: " + offset);
                break;
            } else {
                /* Continue to next page */
                page++;
                offset += offsetIncreasePerPage;
                br.getPage(baseurl + "&s=" + offset);
                continue;
            }
        } while (!this.isAbort());
        return ret;
    }

    @Override
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}
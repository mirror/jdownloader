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

import java.io.IOException;
import java.util.ArrayList;

import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.FilesMonsterCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filesmonster.com" }, urls = { "https?://(?:www\\.)?filesmonster\\.com/folders\\.php\\?fid=([0-9a-zA-Z_-]{22,}|\\d+)" })
public class FilesMonsterComFolder extends PluginForDecrypt {
    // DEV NOTES:
    // packagename is useless, as Filesmonster decrypter creates its own..
    // most simple method to
    private String protocol = null;
    private String uid      = null;

    public FilesMonsterComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final String parameter = param.getCryptedUrl();
        protocol = new Regex(parameter, "(https?)://").getMatch(0);
        uid = UrlQuery.parse(param.getCryptedUrl()).get("fid");
        if (protocol == null || StringUtils.isEmpty(uid)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        FilesMonsterCom.prepBR(br);
        br.setFollowRedirects(false);
        br.setCookiesExclusive(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // base/first page, count always starts at zero!
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        parsePage(ret, parameter, 0);
        return ret;
    }

    /**
     * find all download and folder links, and returns ret;
     *
     * @throws PluginException
     */
    private void parsePage(ArrayList<DownloadLink> ret, String parameter, int s) throws IOException, PluginException {
        // the 's' increment per page is 50, find the first link with the same uid and s+50 each page!
        s = s + 50;
        String lastPage = br.getRegex("<a href='(/?folders\\.php\\?fid=" + uid + "[^']+)'>Last Page\\s*</a>").getMatch(0);
        if (lastPage == null) {
            // not really needed by hey why not, incase they change html
            lastPage = br.getRegex("<a href='(/?folders\\.php\\?fid=" + uid + "&s=" + s + ")").getMatch(0);
        }
        String[] links = br.getRegex("<a[^>]*href=\"(https?://[\\w\\.\\d]*?filesmonster\\.com/(download|folders)\\.php.*?)\">").getColumn(0);
        if (links == null || links.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (links != null && links.length != 0) {
            for (String dl : links) {
                // prevent regex from finding itself, this is incase they change layout and creates infinite loop.
                if (!dl.contains("fid=" + uid)) {
                    ret.add(createDownloadlink(dl.replaceFirst("https?", protocol)));
                }
            }
        }
        if (lastPage != null && !br.getURL().endsWith(lastPage)) {
            br.getPage(parameter + "&s=" + s);
            parsePage(ret, parameter, s);
        } else {
            // can't find last page, but non spanning pages don't have a last page! So something we shouldn't be concerned about.
            logger.info("Success in processing " + parameter);
        }
    }

    @Override
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}
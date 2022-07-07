//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "zaycev.net" }, urls = { "https?://(?:www\\.)?zaycev\\.net/artist/\\d+(\\?page=\\d+)?" })
public class ZyvNt extends PluginForDecrypt {
    public ZyvNt(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        final String artist = br.getRegex("schema.org/MusicGroup\"[^>]+><meta content=\"([^\"]+)\"").getMatch(0);
        final String[] urls = br.getRegex("href=\"(/pages/\\d+/\\d+\\.shtml)\"[^>]*track-link").getColumn(0);
        if (urls == null || urls.length == 0) {
            if (br.containsHTML(">Нет информации<|>Композиций не найдено<")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        for (String link : urls) {
            final DownloadLink dl = createDownloadlink("https://" + this.getHost() + link);
            decryptedLinks.add(dl);
        }
        if (artist != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(artist).trim());
            fp.addLinks(decryptedLinks);
            fp.setAllowMerge(true);
        }
        return decryptedLinks;
    }

    public boolean hasCaptcha(final CryptedLink link, final jd.plugins.Account acc) {
        return false;
    }
}
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
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "zaycev.net" }, urls = { "https?://(?:www\\.)?zaycev\\.net/artist/\\d+(\\?page=\\d+)?" })
public class ZaycevNetCrawler extends PluginForDecrypt {
    public ZaycevNetCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String artist = br.getRegex("schema.org/MusicGroup\"[^>]+><meta content=\"([^\"]+)\"").getMatch(0);
        final String[] urls = br.getRegex("href=\"(/pages/\\d+/\\d+\\.shtml)\"[^>]*track-link").getColumn(0);
        if (urls == null || urls.length == 0) {
            if (br.containsHTML(">\\s*0\\s*треков\\s*<")) {
                return ret;
            } else if (br.containsHTML(">\\s*Нет информации\\s*<|>\\s*Композиций не найдено\\s*<")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        for (String link : urls) {
            final DownloadLink dl = createDownloadlink(br.getURL(link).toExternalForm());
            ret.add(dl);
        }
        final FilePackage fp = FilePackage.getInstance();
        if (artist != null) {
            fp.setName(Encoding.htmlDecode(artist).trim());
        }
        fp.addLinks(ret);
        fp.setAllowMerge(true);
        return ret;
    }

    public boolean hasCaptcha(final CryptedLink link, final jd.plugins.Account acc) {
        return false;
    }
}
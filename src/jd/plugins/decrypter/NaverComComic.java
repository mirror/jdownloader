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

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision: 37833 $", interfaceVersion = 3, names = { "comic.naver.com" }, urls = { "https?://(?:comic\\.)?naver.com/(?:webtoon\\/detail\\.nhn)+\\?(?:titleId=)[\\d]+&(?:no=)[\\d]+.*" })
public class NaverComComic extends antiDDoSForDecrypt {
    public NaverComComic(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Tags: MangaPictureCrawler */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final Regex regex = new Regex(parameter, "https?://(?:comic\\.)?naver.com/(?:webtoon\\/detail\\.nhn)+\\?(?:titleId=)([\\d]+)&(?:no=)([\\d]+).*");
        final String webtoon_id = regex.getMatch(0);
        final String chapter = regex.getMatch(1);
        final String title = webtoon_id + "-" + chapter;
        final String wt_viewer = br.getRegex("(<div class=\"wt_viewer\"[^>]*>.*?<img.*?</div)").getMatch(0);
        if (wt_viewer != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(title);
            final String[] imgs = new Regex(wt_viewer, "<img src=\"(.*?)\"").getColumn(0);
            int page_current = 1;
            for (final String img : imgs) {
                final DownloadLink link = createDownloadlink(img);
                link.setAvailable(true);
                link.setFinalFileName(page_current + ".jpg");
                fp.add(link);
                decryptedLinks.add(link);
                page_current++;
            }
        }
        return decryptedLinks;
    }
}

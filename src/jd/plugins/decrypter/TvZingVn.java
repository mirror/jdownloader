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

/**
 * @author noone2407
 */
package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Request;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

/**
 *
 * @author noone2407
 * @author raztoki
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tv.zing.vn" }, urls = { "https?://tv\\.zing\\.vn/([\\w\\-]+$|series/[\\w\\-]+)" }) 
public class TvZingVn extends PluginForDecrypt {

    public TvZingVn(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String url = parameter.getCryptedUrl();
        br.setFollowRedirects(true);
        br.getPage(url);

        if (url.contains("tv.zing.vn/series/")) {
            // check if this site has see-all links
            final String[] seeAll = br.getRegex("(/series/[a-zA-Z0-9\\-]+)").getColumn(0);
            // if page has "see all" button(s) then we go the each page and crawl
            if (seeAll != null) {
                for (final String link : seeAll) {
                    decryptedLinks.add(createDownloadlink("http://tv.zing.vn" + link));
                }
            }
            // no package name
            return decryptedLinks;
        }
        // get title
        String title = br.getRegex("<title[^>]*>(.*?)</title>").getMatch(0);
        if (title != null) {
            title = title.split("\\|")[0];
        }
        // count total page
        final String[] pages = br.getRegex("<a title=\"Trang \\d+\" \\S+>\\d+</a>").getColumn(-1);
        int totalPage = pages != null && pages.length != 0 ? pages.length : 1;

        for (int i = 0; i <= totalPage; i++) {
            // get all video
            if (i > 0) {
                br.getPage("?p=" + i);
            }
            final String[] allvideos = br.getRegex("(/video/[a-zA-Z0-9\\-]+/[a-zA-Z0-9\\-]+.html)").getColumn(0);
            // send all video to package
            for (final String video : allvideos) {
                final String link = Request.getLocation(video, br.getRequest());
                decryptedLinks.add(createDownloadlink(link));
            }
        }
        if (title != null) {
            final FilePackage filePackage = FilePackage.getInstance();
            filePackage.setName(title);
            filePackage.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}

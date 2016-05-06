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
import java.util.List;
import java.util.regex.Matcher;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision:$", interfaceVersion = 3, names = { "tv.zing.vn" }, urls = { "https?://tv\\.zing\\.vn/series/[\\w\\-]+" }, flags = { 0 })
public class ZingTvSeries extends PluginForDecrypt {

    public ZingTvSeries(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String url = parameter.getCryptedUrl();
        br.setDebug(false);
        br.setFollowRedirects(true);
        br.getPage(url);
        // get title
        String title = br.getRegex("<title[^>]*>(.*?)</title>").getMatch(0);
        title = title.split("\\|")[0];
        FilePackage filePackage = FilePackage.getInstance();
        filePackage.setName(title);
        List<String> allvideos = new ArrayList<String>();
        // count total page
        int totalPage = 0;
        Matcher matcher = br.getRegex("<a title=\"Trang \\d+\" \\S+>\\d+</a>").getMatcher();
        while (matcher.find()) {
            totalPage++;
        }

        for (int i = 1; i <= totalPage; i++) {
            // get all video
            if (i > 1) {
                br.getPage(url + "?p=" + i);
            }
            Matcher videos = br.getRegex("(/video/[a-zA-Z0-9\\-]+/[a-zA-Z0-9\\-]+.html)").getMatcher();
            while (videos.find()) {
                if (!allvideos.contains(videos.group())) {
                    allvideos.add(videos.group());
                }
            }
        }

        String link;
        // send all video to package
        for (final String video : allvideos) {
            link = "http://tv.zing.vn" + video;
            DownloadLink dllink = createDownloadlink(link);
            dllink._setFilePackage(filePackage);
            decryptedLinks.add(dllink);
        }
        return decryptedLinks;
    }

}

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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

/*
 Domains:
 album.ee
 mallorca.as.album.ee
 static1.album.ee
 beta.album.ee
 http://wwww.album.ee
 ru.album.ee
 en.album.ee
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "album.ee" }, urls = { "http://[\\w\\.]*?(album.ee|mallorca.as.album.ee|static1.album.ee|beta.album.ee|ru.album.ee|en.album.ee)/(album|node)/[0-9]+/[0-9]+(\\?page=[0-9]+)?" }, flags = { 0 })
public class AlbumEE extends PluginForDecrypt {

    private Pattern fileNamePattern    = Pattern.compile("\">(photo|foto|Фото).*?<b>(.*?)</b></p>");
    private Pattern albumNamePattern   = Pattern.compile(">.*?(album|альбом).*?<a href=\"album[/0-9]+\".*?>(.*?)</a></p>");
    private Pattern nextPagePattern    = Pattern.compile("<a href=\"(album[/0-9]+\\?page=[0-9]+)\">(Next|Järgmine|Следующая)</a>");
    private Pattern singleLinksPattern = Pattern.compile("<div class=\"img\"><a href=\"(node/[0-9]+/[0-9]+)\"><img src");
    private Pattern pictureURLPattern  = Pattern.compile("<img src=\"(http://[\\w\\.]*?album.*?/files/.*?)\" alt");

    public AlbumEE(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        ArrayList<String> picLinks = new ArrayList<String>();
        br.setFollowRedirects(true);
        String link = parameter.toString();
        br.getPage(link);
        String nextPage = null;
        String[] links;
        String albumName = null;
        FilePackage fp = null;
        boolean onePageOnly = false;
        albumName = br.getRegex(albumNamePattern).getMatch(1);
        if (albumName != null) {
            fp = FilePackage.getInstance();
            fp.setName(albumName);
        }
        if (link.contains("/node")) {
            picLinks.add(parameter.toString());
        } else {
            if (link.contains("?page=")) {
                onePageOnly = true; // only load this single page
            }
            do {
                links = br.getRegex(singleLinksPattern).getColumn(0);
                for (String link2 : links) {
                    picLinks.add("http://www.album.ee/" + link2);
                }
                if (onePageOnly) break;
                nextPage = br.getRegex(nextPagePattern).getMatch(0);
                if (nextPage == null) break;
                br.getPage("http://www.album.ee/" + nextPage);
            } while (true);
        }
        String pictureURL = null;
        String filename = null;
        DownloadLink dlLink;
        for (String picLink : picLinks) {
            br.getPage(picLink);
            filename = br.getRegex(fileNamePattern).getMatch(1);
            pictureURL = br.getRegex(pictureURLPattern).getMatch(0);
            if (pictureURL == null && !br.containsHTML("<a name=\"browse\">")) {
                logger.info("Found an offline link: " + parameter);
                continue;
            }
            if (pictureURL == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            dlLink = createDownloadlink(pictureURL);
            if (filename != null) dlLink.setFinalFileName(filename);
            if (fp != null) fp.add(dlLink);
            decryptedLinks.add(dlLink);
        }
        return decryptedLinks;
    }

}

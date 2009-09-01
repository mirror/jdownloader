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
import jd.parser.Regex;
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
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {"album.ee"}, urls = {"http://[\\w\\.]*?(album.ee|mallorca.as.album.ee|static1.album.ee|beta.album.ee|ru.album.ee|en.album.ee)/(album|node)/[0-9]+/[0-9]+(\\?page=[0-9]+)?"}, flags = {0})
public class AlbumEE extends PluginForDecrypt {

    private Pattern fileNamePattern = Pattern.compile("<p class=\"f-left\">photo » <b>(.*?)</b></p>");
    private Pattern albumNamePattern = Pattern.compile("album » <a href=\"album[0-9/]+\" class=\"active\">(.*?)</a></p>");
    private Pattern singleLinksPattern = Pattern.compile("<div class=\"img\"><a href=\"(node/[0-9]+/[0-9]+)\"><img src");
    private Pattern pictureURLPattern = Pattern.compile("<img src=\"(http://[\\w\\.]*?album.*?/files/.*?)\" alt", Pattern.CASE_INSENSITIVE);

    public AlbumEE(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        ArrayList<String> picLinks = new ArrayList<String>();
        br.setFollowRedirects(true);
        br.getPage(parameter.toString());
        String nextPage;
        String[] links;
        String albumName;
        FilePackage fp = null;
        boolean onePageOnly = false;
        albumName = br.getRegex(albumNamePattern).getMatch(0);
        if (albumName != null) {
            fp = FilePackage.getInstance();
            fp.setName(albumName);
        }
        Regex linkType = br.getRegex(Pattern.compile("(.*?/node/[0-9]+/[0-9]+)|(.*?/album/[0-9]+/[0-9]+\\?page=[0-9]+)", Pattern.CASE_INSENSITIVE));
        if (linkType.getMatch(0) != null) {
            picLinks.add(parameter.toString());
        } else {
            if (linkType.getMatch(1) != null) {
                onePageOnly = true; //only load this single page
            }
            do {
                links = br.getRegex(singleLinksPattern).getColumn(0);
                for (int i = 0; i < links.length; i++) {
                    picLinks.add("http://www.album.ee/" + links[i]);
                }
                if (onePageOnly) break;
                nextPage = br.getRegex(Pattern.compile("<a href=\"(album/[0-9]+/[0-9]+\\?page=[0-9]+)\">Next</a>", Pattern.CASE_INSENSITIVE)).getMatch(0);
                if (nextPage == null) break;
                br.getPage("http://www.album.ee/" + nextPage);
            } while (true);
        }
        String pictureURL;
        String filename;
        DownloadLink dlLink;
        progress.setRange(picLinks.size());
        for (String link : picLinks) {
            br.getPage(link);
            filename = br.getRegex(fileNamePattern).getMatch(0);
            pictureURL = br.getRegex(pictureURLPattern).getMatch(0);
            if (pictureURL == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            dlLink = createDownloadlink(pictureURL);
            if (filename != null) dlLink.setFinalFileName(filename);
            if (fp != null) dlLink.setFilePackage(fp);
            decryptedLinks.add(dlLink);
            progress.increase(1);
        }
        return decryptedLinks;
    }
}

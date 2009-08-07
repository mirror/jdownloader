//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import jd.http.HTMLEntities;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mp3link.org" }, urls = { "http://[\\w\\.]*?mp3link\\.org/.*?/(song|album).+"}, flags = { 0 })


public class Mp3LinkOrg extends PluginForDecrypt {

    private String pattern_AlbumInfo = "http://mp3link\\.org/.*?/album/details\\.html";
    private String pattern_SongInfo = "http://mp3link\\.org/.*?/song/details\\.html";
    private String pattern_Album = "http://mp3link\\.org/download/album\\..*?";
    private String pattern_Song = "http://mp3link\\.org/download/song\\..*?";

    public Mp3LinkOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    //@Override
    //@SuppressWarnings("null")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setDebug(true);
        String link = parameter;
        String filename = null, seperator = "";
        progress.setRange(1);

        if (link.matches(pattern_AlbumInfo) || link.matches(pattern_SongInfo)) {
            br.getPage(link);
            link = "http://mp3link.org/download/" + br.getRegex("<a\\s+href=\"/download/(.*?)\"").getMatch(0);
            if (link.matches(pattern_Song)) {
                String artist = br.getRegex("Artist:<b>(.*?)</b>").getMatch(0);
                String title = br.getRegex("Title:<b>(.*?)</b>").getMatch(0);
                if ((artist.length() != 0) && (title.length() != 0))
                    seperator = " - ";
                filename = artist + seperator + title;
            } else 
                filename = br.getRegex("<h2><b>(.*?)</b></h2>").getMatch(0);
        }
        if (link.matches(pattern_Album) || link.matches(pattern_Song)) {
            br.getPage(link);
            if (link.matches(pattern_Album)) {
                link = br.getRegex("document\\.location\\s+=\\s+'(.*?)'").getMatch(0);
            } else if (link.matches(pattern_Song))
                link = br.getRedirectLocation();
            
            DownloadLink dlink = createDownloadlink(link);
            FilePackage fp = FilePackage.getInstance();
            fp.setName(HTMLEntities.unhtmlentities(filename));
            fp.add(dlink);
            decryptedLinks.add(dlink);
            progress.increase(1);

            return decryptedLinks;
        } else
            return null;
    }
    //@Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}

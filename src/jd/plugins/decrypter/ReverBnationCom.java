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

import java.text.DecimalFormat;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "reverbnation.com" }, urls = { "http://(www\\.)?reverbnation\\.com/(?!facebook_channel|main|mailto|user|promoter\\-promotion|fan\\-promotion|venue\\-promotion|label\\-promotion|javascript:|signup|appending|head|css|images|data:|band\\-promotion)(artist/artist_songs/\\d+|playlist/view_playlist/\\d+\\?page_object=artist_\\d+|open_graph/song/\\d+|play_now/song_\\d+|page_object/page_object_photos/artist_\\d+|artist/downloads/\\d+|[A-Za-z0-9\\-_]{5,})" }, flags = { 0 })
public class ReverBnationCom extends PluginForDecrypt {

    public ReverBnationCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        if (parameter.matches("http://(www\\.)?reverbnation\\.com/page_object/page_object_photos/artist_\\d+")) {
            br.getPage(parameter);
            String fpName = getFpname();
            int counter = 1;
            DecimalFormat df = new DecimalFormat("000");
            final String[] pictures = br.getRegex("id=\"photo_\\d+\">[\t\n\r ]+<img data\\-crop=\"\\d+x\\d+\" data\\-full\\-size=\"(//[^<>\"]*?)\"").getColumn(0);
            for (String picture : pictures) {
                DownloadLink fina = createDownloadlink("directhttp://http:" + picture);
                String ext = picture.substring(picture.lastIndexOf("."));
                if (ext != null && ext.length() < 5 && fpName != null) {
                    fina.setFinalFileName("Photo " + df.format(counter) + ext);
                    counter++;
                }
                decryptedLinks.add(fina);
            }
            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim()));
                fp.addLinks(decryptedLinks);
            }
        } else if (parameter.matches("http://(www\\.)?reverbnation\\.com/(play_now/song_\\d+|open_graph/song/\\d+)")) {
            final String fileID = new Regex(parameter, "(\\d+)$").getMatch(0);
            if (parameter.matches("http://(www\\.)?reverbnation\\.com/open_graph/song/\\d+")) parameter = parameter.replace("open_graph/song/", "play_now/song_");
            br.getPage(parameter);
            String artistID = br.getRegex("onclick=\"playSongNow\\(\\'all_artist_songs_(\\d+)\\'\\)").getMatch(0);
            if (artistID == null) {
                artistID = br.getRegex("\\(\\'all_artist_songs_(\\d+)\\')").getMatch(0);
                if (artistID == null) artistID = br.getRegex("artist/artist_songs/(\\d+)\\?").getMatch(0);
            }
            String filename = br.getRegex("data\\-song\\-id=\"" + fileID + "\" title=\"Play \\&quot;([^<>\"]*?)\\&quot;\"").getMatch(0);
            if (artistID == null || filename == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            filename = Encoding.htmlDecode(filename.trim());
            final DownloadLink dlLink = createDownloadlink("http://reverbnationcomid" + fileID + "reverbnationcomartist" + artistID);
            if (filename.contains(".mp3"))
                dlLink.setName(filename);
            else
                dlLink.setName(filename + ".mp3");
            dlLink.setProperty("orgName", dlLink.getName());
            decryptedLinks.add(dlLink);
        } else if (parameter.matches("http://(www\\.)?reverbnation\\.com/(artist/artist_songs/\\d+|playlist/view_playlist/\\d+\\?page_object=artist_\\d+|open_graph/song/\\d+|artist/downloads/\\d+|[^<>\"/]+)")) {
            String fpName = null;
            String[][] allInfo = null;
            if (parameter.matches("http://(www\\.)?reverbnation\\.com/(artist/artist_songs/\\d+|playlist/view_playlist/\\d+\\?page_object=artist_\\d+|open_graph/song/\\d+)")) {
                br.getPage(parameter);
                fpName = getFpname();
                allInfo = br.getRegex("data\\-url=\"/artist/artist_song/(\\d+)\\?song_id=(\\d+)\">[\t\n\r ]+<a href=\"#\" class=\" standard_play_button song\\-action play\" data\\-song\\-id=\"\\d+\" title=\"Play \\&quot;([^<>\"]*?)\\&quot;\"").getMatches();
            } else if (parameter.matches("http://(www\\.)?reverbnation\\.com/artist/downloads/\\d+")) {
                br.getPage(parameter);
                fpName = getFpname();
                allInfo = br.getRegex("production_public/Artist/(\\d+)/image/thumb/[a-z0-9_\\-]+\\.jpg\" /><a href=\"#\" class=\"size_48  standard_play_button song\\-action play\" data\\-song\\-id=\"(\\d+)\" title=\"Play &quot;([^<>\"]*?)&quot;\"").getMatches();
            } else {
                br.getPage(parameter);
                fpName = br.getRegex("<h1 class=\"profile_user_name\">([^<>\"]*?)</h1>").getMatch(0);
                allInfo = br.getRegex("reverbnation\\.com/artist/artist_songs/(\\d+)\\?song_id=(\\d+)\">[\t\n\r ]+<a href=\"#\" class=\"standard_play_button black_34 song\\-action play\" data\\-song\\-id=\"\\d+\" title=\"Play \\&quot;([^<>\"]*?)\\&quot;\"").getMatches();
            }
            if (allInfo == null || allInfo.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (String singleInfo[] : allInfo) {
                final DownloadLink dlLink = createDownloadlink("http://reverbnationcomid" + singleInfo[1] + "reverbnationcomartist" + singleInfo[0]);
                String name = Encoding.htmlDecode(singleInfo[2]);
                if (name.contains(".mp3"))
                    dlLink.setName(name);
                else
                    dlLink.setName(name + ".mp3");
                dlLink.setProperty("orgName", dlLink.getName());
                decryptedLinks.add(dlLink);
            }

            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim()));
                fp.addLinks(decryptedLinks);
            }

        }

        return decryptedLinks;
    }

    private String getFpname() {
        String fpName = br.getRegex("<title>([^<>\"]*?) \\- ReverbNation</title>").getMatch(0);
        return fpName;
    }
}
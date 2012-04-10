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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "reverbnation.com" }, urls = { "http://(www\\.)?reverbnation\\.com/artist/artist_songs/\\d+" }, flags = { 0 })
public class ReverBnationCom extends PluginForDecrypt {

    public ReverBnationCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        final String fpName = br.getRegex("<title>([^<>\"]*?) \\- ReverbNation</title>").getMatch(0);
        String[][] allInfo = br.getRegex("class=\"artist_backpage_songs_row clearfix \" data\\-url=\"/artist/artist_song/(\\d+)\\?song_id=(\\d+)\">[\t\n\r ]+<a href=\"#\" class=\" standard_play_button song\\-action play\" data\\-song\\-id=\"\\d+\" title=\"Play \\&quot;([^<>\"]*?)\\&quot;\"").getMatches();
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
        return decryptedLinks;
    }
}
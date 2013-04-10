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

import java.io.IOException;
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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mikseri.net" }, urls = { "http://(www\\.)?mikseri\\.net/(artists/[^/]+/[^/]+/\\d+/|artists/\\?id=\\d+|artists/[^\"\\']+\\.\\d+)" }, flags = { 0 })
public class MikSeriNet extends PluginForDecrypt {

    public MikSeriNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        String parameter = param.toString();
        String anID = new Regex(parameter, "mikseri\\.net/artists/[^\"\\']+\\.(\\d+)").getMatch(0);
        if (anID != null) parameter = "http://www.mikseri.net/artists/?id=" + anID;
        br.getPage(parameter);
        if (parameter.matches(".*?mikseri.net/artists/\\?id=.*?")) {
            String fpName = br.getRegex("<meta name=\"og:title\" content=\"(.*?)\"").getMatch(0);
            if (fpName == null) {
                fpName = br.getRegex("<meta name=\"title\" content=\"(.*?)\"").getMatch(0);
                if (fpName == null) fpName = br.getRegex("<title>(.*?)</title>").getMatch(0);
            }
            String[] fileIDs = br.getRegex("id=\"sharelinks_(\\d+)\"").getColumn(0);
            if (fileIDs == null || fileIDs.length == 0) {
                fileIDs = br.getRegex("type=\"hidden\" name=\"song_id\" value=\"(\\d+)\"").getColumn(0);
                if (fileIDs == null || fileIDs.length == 0) {
                    fileIDs = br.getRegex("id=\"player\\-(\\d+)\"").getColumn(0);
                    if (fileIDs == null || fileIDs.length == 0) {
                        fileIDs = br.getRegex("/music/play\\.php\\?id=(\\d+)").getColumn(0);
                        if (fileIDs == null || fileIDs.length == 0) {
                            fileIDs = br.getRegex("displaySomething\\(\\'sharelinks_(\\d+)\\'\\)").getColumn(0);
                        }
                    }
                }
            }
            if (fileIDs == null || fileIDs.length == 0) return null;
            progress.setRange(fileIDs.length);
            for (String id : fileIDs) {
                DownloadLink fina = getSingleLink(id);
                if (fina == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                decryptedLinks.add(fina);
                progress.increase(1);
            }
            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim()));
                fp.addLinks(decryptedLinks);
            }
        } else {
            DownloadLink fina = getSingleLink(new Regex(parameter, "/(\\d+)/$").getMatch(0));
            if (fina == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(fina);
        }

        return decryptedLinks;
    }

    private DownloadLink getSingleLink(String iD) throws IOException {
        br.getPage("http://www.mikseri.net/player/songlist.php?newsession=1&type=1&parameter=" + iD);
        String finallink = br.getRegex("<SongUrl>(http://[^<>\"]*?)</SongUrl>").getMatch(0);
        if (finallink == null) return null;
        return createDownloadlink("directhttp://" + finallink);

    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}
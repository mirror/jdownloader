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
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mp3lemon.net" }, urls = { "http://(www\\.)?mp3lemon\\.(net|org)/((song|album)/\\d+/|download\\.php\\?idfile=\\d+)" }, flags = { 0 })
public class MpLemonNet extends PluginForDecrypt {

    public MpLemonNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("mp3lemon.org", "mp3lemon.net");
        if (parameter.contains("download.php?idfile=")) parameter = parameter.replace("download.php?idfile=", "song/") + "/";
        br.setFollowRedirects(false);
        br.setCustomCharset("windows-1251");
        br.setReadTimeout(3 * 60 * 1000);
        if (parameter.contains("/song/")) {
            String finallink = decryptSingleLink(new Regex(parameter, "mp3lemon\\.net/song/(\\d+)/").getMatch(0));
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        } else {
            br.getPage(parameter);
            if (br.containsHTML(">Альбом не найден")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            String[][] fileInfo = br.getRegex("\"/song/(\\d+)/([^<>\"/]*?)\"></a></td>[\t\n\r ]+<td class=\"list_tracks\"></td>[\t\n\r ]+<td class=\"list_tracks\">\\d+:\\d+</td>[\t\n\r ]+<td class=\"list_tracks\">(\\d+(\\.\\d+)? *?)</td>").getMatches();
            if (fileInfo == null || fileInfo.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (String[] dl : fileInfo) {
                String finallink = dl[0];
                String filename = dl[1];
                if (finallink == null || filename == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                finallink = decryptSingleLink(dl[0]);
                if (finallink == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                DownloadLink fina = createDownloadlink(finallink);
                fina.setFinalFileName(Encoding.htmlDecode(dl[1].trim()) + ".mp3");
                fina.setDownloadSize(SizeFormatter.getSize(dl[2] + " MB"));
                fina.setAvailable(true);
                decryptedLinks.add(fina);
            }
        }
        return decryptedLinks;
    }

    private String decryptSingleLink(String fID) throws IOException {
        br.getPage("http://mp3lemon.net/download.php?idfile=" + fID);
        return br.getRedirectLocation();
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}
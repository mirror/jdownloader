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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "myzuka.ru" }, urls = { "https?://(?:www\\.)?myzuka\\.(ru|org|fm)/Album/\\d+" })
public class MyzukaRuDecrypter extends PluginForDecrypt {
    public MyzukaRuDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        /* Forced https */
        final String parameter = "https://myzuka.fm/Album/" + new Regex(param.toString(), "(\\d+)$").getMatch(0);
        br.setFollowRedirects(true);
        br.getPage(parameter);
        /* offline|abused */
        if (br.getHttpConnection().getResponseCode() == 403 || br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("Альбом удален по просьбе правообладателя")) {
            final DownloadLink offline = this.createOfflinelink(parameter);
            offline.setFinalFileName(new Regex(parameter, "https?://[^<>\"/]+/(.+)").getMatch(0));
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        final String[] info = br.getRegex("(<div id=\"playerDiv\\d+\".*?)lass=\"ico\\-rating\"").getColumn(0);
        if (info == null || info.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        String fpName = br.getRegex("<h1 class=\"green\">([^<>\"]*?)</h1>").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("<title>(.*?)</title>").getMatch(0);
        }
        if (fpName == null) {
            fpName = new Regex(br.getURL(), "myzuka\\.(?:ru|org|fm)/Album/\\d+/(.+)").getMatch(0);
        }
        for (final String singleLink : info) {
            final String url = new Regex(singleLink, "href=\"(/Song/\\d+/[^<>\"/]+)\"").getMatch(0);
            final String title = new Regex(singleLink, "href=\"/Song/\\d+/[^<>\"/]+\">([^<>\"]*?)</a>").getMatch(0);
            final String artist = new Regex(singleLink, "href=\"/Artist/\\d+/[^<>\"/]+\">([^<>\"]*?)</a>").getMatch(0);
            String filesize = new Regex(singleLink, "class=\"time\">([^<>\"]*?)<").getMatch(0);
            if (url == null || title == null || artist == null || filesize == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            filesize = new Regex(filesize, "(\\d+(?:,\\d+)?)").getMatch(0) + "MB";
            final DownloadLink fina = createDownloadlink("http://myzuka.ru" + Encoding.htmlDecode(url));
            fina.setName(Encoding.htmlDecode(artist) + " - " + Encoding.htmlDecode(title) + ".mp3");
            fina.setDownloadSize(SizeFormatter.getSize(filesize));
            fina.setAvailable(true);
            decryptedLinks.add(fina);
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}

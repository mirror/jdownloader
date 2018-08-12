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
import java.util.Random;

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "anime.thehylia.com" }, urls = { "https?://(www\\.)?anime\\.thehylia\\.com/(downloads/series/|download_file/|soundtracks/album/)[a-z0-9\\-_]+" })
public class AnimeTheHyliaCom extends PluginForDecrypt {
    public AnimeTheHyliaCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String type_series = "http://(www\\.)?anime\\.thehylia\\.com/downloads/series/[a-z0-9\\-_]+";
    private static final String type_music  = "http://(www\\.)?anime\\.thehylia\\.com/soundtracks/album/[a-z0-9\\-_]+";

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML(">No such series<|>No such album<")) {
            logger.info("Link offline: " + parameter);
            final DownloadLink offline = this.createOfflinelink(parameter);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        String fpName = null;
        String[][] links;
        if (parameter.contains("download_file")) {
            final DownloadLink dl = createDownloadlink("http://anime.thehyliadecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(10000));
            dl.setContentUrl(parameter);
            dl.setLinkID(new Regex(parameter, "download_file/(\\d+)").getMatch(0));
            dl.setProperty("referer", br.getRedirectLocation());
            dl.setProperty("directlink", parameter);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        if (parameter.matches(type_series)) {
            fpName = br.getRegex("<\\!\\-\\-Series name: <b>([^<>\"]*?)</b><br>\\-\\->").getMatch(0);
            if (fpName == null) {
                fpName = br.getRegex("<div><h2>([^<>\"]*?)</h2>").getMatch(0);
            }
            links = br.getRegex("\"(http://anime\\.thehylia\\.com/download_file/\\d+)\">([^<>\"]*?)</a></td>[\t\n\r ]+<td align=\"center\" width=\"55px\">([^<>\"]*?)</td>").getMatches();
            if (links == null || links.length == 0 || fpName == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            fpName = Encoding.htmlDecode(fpName);
            for (final String singleLinkInfo[] : links) {
                final String filename = fpName + Encoding.htmlDecode(singleLinkInfo[1]).replace(":", " - ") + ".avi";
                final String directlink = singleLinkInfo[0];
                final DownloadLink dl = createDownloadlink("http://anime.thehyliadecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(10000));
                dl.setLinkID(filename);
                dl.setContentUrl(parameter);
                dl.setFinalFileName(filename);
                dl.setDownloadSize(SizeFormatter.getSize(Encoding.htmlDecode(singleLinkInfo[2])));
                dl.setProperty("referer", br.getURL());
                dl.setProperty("decryptedfilename", filename);
                dl.setProperty("directlink", directlink);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
        } else {
            fpName = br.getRegex("Album name: <b>([^<>\"]*?)</b><").getMatch(0);
            links = br.getRegex("\"(http://anime\\.thehylia\\.com/soundtracks/album/[a-z0-9\\-]+/[^<>\"/]+)\">([^<>\"]*?)</a></td>[\t\n\r ]+<td>([^<>\"]*?)</td>").getMatches();
            if (links == null || links.length == 0 || fpName == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            fpName = Encoding.htmlDecode(fpName);
            for (final String singleLinkInfo[] : links) {
                final String filename = fpName + " - " + Encoding.htmlDecode(singleLinkInfo[1]) + ".mp3";
                final String directlink = singleLinkInfo[0];
                final DownloadLink dl = createDownloadlink("http://anime.thehyliadecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(10000));
                dl.setLinkID(filename);
                dl.setContentUrl(parameter);
                dl.setFinalFileName(fpName + " - " + Encoding.htmlDecode(singleLinkInfo[1]) + ".mp3");
                dl.setDownloadSize(SizeFormatter.getSize(Encoding.htmlDecode(singleLinkInfo[2])));
                dl.setProperty("referer", br.getURL());
                dl.setProperty("decryptedfilename", filename);
                dl.setProperty("directlink", directlink);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}
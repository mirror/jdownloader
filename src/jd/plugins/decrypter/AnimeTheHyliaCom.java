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

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "anime.thehylia.com" }, urls = { "http://(www\\.)?anime\\.thehylia\\.com/downloads/series/[a-z0-9\\-_]+" }, flags = { 0 })
public class AnimeTheHyliaCom extends PluginForDecrypt {

    public AnimeTheHyliaCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        String fpName = br.getRegex("<\\!\\-\\-Series name: <b>([^<>\"]*?)</b><br>\\-\\->").getMatch(0);
        if (fpName == null) fpName = br.getRegex("<div><h2>([^<>\"]*?)</h2>").getMatch(0);
        final String[][] links = br.getRegex("\"(http://anime\\.thehylia\\.com/download_file/\\d+)\">([^<>\"]*?)</a></td>[\t\n\r ]+<td align=\"center\" width=\"55px\">([^<>\"]*?)</td>").getMatches();
        if (links == null || links.length == 0 || fpName == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        fpName = Encoding.htmlDecode(fpName);

        for (final String singleLinkInfo[] : links) {
            final DownloadLink dl = createDownloadlink(singleLinkInfo[0]);
            dl.setFinalFileName(fpName + Encoding.htmlDecode(singleLinkInfo[1]).replace(":", " - ") + ".avi");
            dl.setDownloadSize(SizeFormatter.getSize(Encoding.htmlDecode(singleLinkInfo[2])));
            dl.setProperty("referer", br.getURL());
            dl.setAvailable(true);
            decryptedLinks.add(dl);
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
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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mangashare.com" }, urls = { "http://(www\\.)?read\\.mangashare\\.com/[A-Za-z0-9\\-_]+/chapter\\-\\d+/page\\d+\\.html" }, flags = { 0 })
public class MangaShareCom extends PluginForDecrypt {

    public MangaShareCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        String fpName = br.getRegex("<title>([^<>\"]*?) \\| Manga Share</title>").getMatch(0);
        final String[] pages = br.getRegex("\">Page (\\d+)</option>").getColumn(0);
        final String[][] directLink = br.getRegex("\"(http://dl\\d+\\.mangashare\\.com/manga/[A-Za-z0-9\\-_]+/\\d+/)\\d+(\\.[A-Za-z]{1,6})\"").getMatches();
        if (pages == null || pages.length == 0 || fpName == null || directLink.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final String part1 = directLink[0][0];
        final String part2 = directLink[0][1];

        fpName = Encoding.htmlDecode(fpName.trim());
        final DecimalFormat counterDF = new DecimalFormat("000");
        final DecimalFormat filenameDF = new DecimalFormat("0000");
        for (final String page : pages) {
            final int pageint = Integer.parseInt(page);
            final DownloadLink dl = createDownloadlink("directhttp://" + part1 + counterDF.format(pageint) + part2);
            dl.setFinalFileName(fpName + "_" + filenameDF.format(pageint) + part2);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}
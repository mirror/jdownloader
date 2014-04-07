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
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "microsoft.com" }, urls = { "http://(www\\.)?microsoft\\.com/(en\\-us|de\\-de)/download/(details|confirmation)\\.aspx\\?id=\\d+" }, flags = { 0 })
public class MicrosoftComDecrypter extends PluginForDecrypt {

    public MicrosoftComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String dlid = new Regex(param.toString(), "(\\d+)$").getMatch(0);
        final String parameter = "http://www.microsoft.com/en-us/download/details.aspx?id=" + dlid;
        br.getPage(parameter);
        if (br.containsHTML(">We are sorry, the page you requested cannot be found")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        br.getPage("http://www.microsoft.com/en-us/download/confirmation.aspx?id=" + dlid);
        String fpName = br.getRegex("<h2 class=\"title\">([^<>\"]*?)</h2>").getMatch(0);
        if (fpName == null) fpName = "Microsoft.com download " + dlid;
        final String dlTable = br.getRegex("<div class=\"chooseFile jsOff\">(.*?)</div>").getMatch(0);
        if (dlTable == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final String[] entries = new Regex(dlTable, "<tr>(.*?)</tr>").getColumn(0);
        for (final String dlentry : entries) {
            final String filename = new Regex(dlentry, "<td>([^<>\"]*?)(<span class=\"green-sniff-recommend\">\\(recommended\\)</span>)?</td>").getMatch(0);
            final String filesize = new Regex(dlentry, "<td>(\\d+(\\.\\d{1,2})? [A-Za-z]{1,5})</td>").getMatch(0);
            final String dllink = new Regex(dlentry, "href=\"(http://download\\.microsoft\\.com/download/[^<>\"]+)\"").getMatch(0);
            if (filename != null && filesize != null && dllink != null) {
                final DownloadLink dl = createDownloadlink(dllink);
                dl.setFinalFileName(Encoding.htmlDecode(filename.trim()));
                dl.setDownloadSize(SizeFormatter.getSize(filesize));
                dl.setAvailable(true);
                dl.setProperty("mainlink", parameter);
                decryptedLinks.add(dl);
            }
        }
        if (decryptedLinks.size() == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

}

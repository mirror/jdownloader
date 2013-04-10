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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

/**
 * French and modified version of protect-my-links.com, decrypterclass PrtcMyLnksCm
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "protege-mes-liens.com" }, urls = { "http://(www\\.)?protege\\-mes\\-liens\\.com/(mylink\\.php\\?linkid=|linkidwoc\\.php\\?linkid=)[a-z0-9]+" }, flags = { 0 })
public class ProtegeMesLiensCom extends PluginForDecrypt {

    public ProtegeMesLiensCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String PASSWORDTEXT = "(<h2>Password:</h2>|Password Incorrect \\!\\!\\! Please retype<|\\&message=wrong)";

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String linkID = new Regex(param.toString(), ".*?=(.+)$").getMatch(0);
        String parameter = "http://protege-mes-liens.com/linkidwoc.php?linkid=" + linkID;
        br.setFollowRedirects(true);
        br.getPage(parameter);
        boolean decrypterBroken = false;
        if (decrypterBroken) return null;
        /* File package handling */
        if (br.containsHTML(PASSWORDTEXT)) {
            for (int i = 0; i <= 3; i++) {
                String passCode = getUserInput(null, param);
                br.postPage("http://protege-mes-liens.com/linkid.php", "linkid=" + linkID + "&password=" + passCode + "&x=" + new Random().nextInt(100) + "&y=" + new Random().nextInt(100));
                if (br.containsHTML(PASSWORDTEXT)) {
                    br.getPage(parameter);
                    continue;
                }
                break;
            }
            if (br.containsHTML(PASSWORDTEXT)) throw new DecrypterException(DecrypterException.PASSWORD);
        } else {
            br.postPage("http://protege-mes-liens.com/linkid.php", "linkid=" + linkID + "&x=" + new Random().nextInt(100) + "&y=" + new Random().nextInt(100));
        }
        /* Error handling */
        if (br.containsHTML("<td style=\\'border:1px\\'><OL COMPACT><LI><a href=target=_blank></a>")) {
            logger.warning("Wrong link");
            logger.warning(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
            return new ArrayList<DownloadLink>();
        }
        final String fpName = br.getRegex("Titre:[\t\n\r ]+</td>[\t\n\r ]+<td style='border:1px'>([^<>\"\\']+)</td>").getMatch(0);
        String[] links = br.getRegex("<LI><a href=([^<>\"\\']+) target=_blank>").getColumn(0);
        if (links == null || links.length == 0) return null;
        for (String aLink : links) {
            if (!aLink.contains("protege-mes-liens.com/")) decryptedLinks.add(createDownloadlink(aLink));
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}
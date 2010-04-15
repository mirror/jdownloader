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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "protected.socadvnet.com" }, urls = { "http://[\\w\\.]*?protected\\.socadvnet\\.com/\\?[a-z0-9-]+" }, flags = { 0 })
public class PrtctdScdvntCm extends PluginForDecrypt {

    public PrtctdScdvntCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    // At the moment this decrypter only decrypts turbobit.net links as
    // "protected.socadvnet.com" only allows crypting links of this host!
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        String postvar = new Regex(parameter, "protected\\.socadvnet\\.com/\\?(.+)").getMatch(0);
        if (postvar == null) return null;
        String security = br.getRegex("<div id =\"cp\">.*?(.*?)= \\&nbsp;<input").getMatch(0);
        br.postPage("http://protected.socadvnet.com/allinks.php", "LinkName=" + postvar);
        String[] linksCount = br.getRegex("(ten\\.tibobrut/)").getColumn(0);
        if (linksCount == null || linksCount.length == 0) return null;
        if (security != null) {
            security = security.trim();
            Regex theNumbers = new Regex(security, "(\\d+) (-|\\+) (\\d+)");
            String num1 = theNumbers.getMatch(0);
            String num2 = theNumbers.getMatch(2);
            String plusMinus = theNumbers.getMatch(1);
            if (num1 == null || num2 == null || plusMinus == null) {
                logger.warning("Error in doing the maths for link: " + parameter);
                return null;
            }
            int equals = 0;
            if (plusMinus.equals("+")) {
                equals = Integer.parseInt(num1) + Integer.parseInt(num2);
            } else {
                equals = Integer.parseInt(num1) - Integer.parseInt(num2);
            }
            br.postPage("http://protected.socadvnet.com/cp_code.php", "res_code=" + equals);
            if (!br.toString().trim().equals("1")) {
                logger.warning("Error in doing the maths for link: " + parameter);
                return null;
            }
        }
        logger.info("Found " + linksCount.length + " links, decrypting now...");
        progress.setRange(linksCount.length);
        for (int i = 0; i <= linksCount.length - 1; i++) {
            String actualPage = "http://protected.socadvnet.com/allinks.php?out_name=" + postvar + "&&link_id=" + i;
            br.getPage(actualPage);
            String turboId = br.getRegex("\"http://turbobit\\.net/download/free/(.*?)\"").getMatch(0);
            if (turboId == null) {
                logger.warning("There is a problem with the link: " + actualPage);
                return null;
            }
            decryptedLinks.add(createDownloadlink("http://turbobit.net/" + turboId + ".html"));
            progress.increase(1);
        }
        return decryptedLinks;
    }
}

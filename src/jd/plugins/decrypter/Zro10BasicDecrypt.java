//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "zero10.info" }, urls = { "http://[\\w\\.]*?((zero10\\.info|save-link\\.info|share-link\\.info|h-link\\.us|zero10\\.us|(darkhorse|brg8)\\.fi5\\.us|arbforce\\.com/short|(pp9p|2utop)\\.com|arb4h\\.net|(get\\.(el3lam|al9daqa|sirtggp))\\.com|get\\.(i44i|city-way)\\.net|tanzel\\.eb2a\\.com/short)/[0-9]+|url-2\\.com/[A-Z]+/)" }, flags = { 0 })
public class Zro10BasicDecrypt extends PluginForDecrypt {

    public Zro10BasicDecrypt(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        // 3l3lam workaround, they got double redirect if i don't replace all
        // their domains with the main domain!
        parameter = parameter.replaceAll("(pp9p\\.com|2utop\\.com|get\\.i44i\\.net|arb4h\\.net|get\\.city-way\\.net|get\\.al9daqa\\.com|get\\.sirtggp\\.com)", "get.el3lam.com");
        br.setFollowRedirects(false);
        // finallink2 is used for unusual zero10 crypters like arbforce and
        // url-2
        String finallink2 = null;
        String finallink = null;
        if (parameter.contains("url-2.com/")) {
            br.getPage(parameter);
            finallink2 = br.getRegex("language='javascript'>.*?\\('(.*?)'\\)").getMatch(0);
            // Errorhandling
            if (finallink2 == null && br.containsHTML("Turn this long URL") && !br.containsHTML("Click here to go")) {
                logger.warning("The requested document was not found on this server.");
                logger.warning(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
                return new ArrayList<DownloadLink>();
            }
        } else if (parameter.contains("arbforce.com/short")) {
            String ID = new Regex(parameter, "arbforce\\.com/short/([0-9]+)").getMatch(0);
            String redirectlink = "http://www.arbforce.com/short/2.php?" + ID;
            br.getPage(redirectlink);
            finallink2 = br.getRedirectLocation();
            // Errorhandling
            if (br.getRedirectLocation() == null) {
                logger.warning("The requested document was not found on this server.");
                logger.warning(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
                return new ArrayList<DownloadLink>();
            }
        } else {
            String Domain = new Regex(parameter, "((zero10\\.us|save-link\\.info|share-link\\.info|h-link\\.us|zero10\\.info|(darkhorse|brg8)\\.fi5\\.us|get\\.el3lam\\.com)|tanzel\\.eb2a\\.com/short)/").getMatch(0);
            String ID = new Regex(parameter, "[0-9a-z.]+/([0-9]+)").getMatch(0);
            String m1link = "http://www." + Domain + "/m1.php?id=" + ID;
            br.getPage(m1link);
            // little errorhandling
            if (br.getRedirectLocation() != null) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
            finallink = br.getRegex("onclick=\"NewWindow\\('(.*?)','name'").getMatch(0);
        }
        if (finallink == null) {
            finallink = finallink2;
        }
        if (finallink == null) return null;
        decryptedLinks.add(createDownloadlink(finallink));

        return decryptedLinks;
    }
}

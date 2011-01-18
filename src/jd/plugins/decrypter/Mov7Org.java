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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

import org.appwork.utils.Regex;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "movie7.org" }, urls = { "http://[\\w\\.]*?movie7\\.(org|me)/.*?/[0-9]+-.*?\\.html" }, flags = { 0 })
public class Mov7Org extends PluginForDecrypt {

    public Mov7Org(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        parameter = parameter.replace("movie7.org", "movie7.me");
        br.setFollowRedirects(false);
        br.getPage(parameter);
        if (br.getRedirectLocation() != null && br.getRedirectLocation().equals("http://movie7.org")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String packageName = br.getRegex("<title>(.*?)\\|.*?</title>").getMatch(0);
        if (packageName == null) {
            packageName = br.getRegex("class=\"contentpagetitle\">(.*?)</a>").getMatch(0);
            if (packageName == null) {
                packageName = br.getRegex("rlsname=(.*?)\\&typ").getMatch(0);
                if (packageName == null) {
                    packageName = br.getRegex("\"releasename\">(.*?)<img").getMatch(0);
                }
            }
        }
        String allCharcodes[] = br.getRegex("write\\(String\\.fromCharCode\\(([0-9,]+)\\)").getColumn(0);
        if (allCharcodes == null || allCharcodes.length == 0) return null;
        for (String singlecharcode : allCharcodes) {
            String[] charcodes = singlecharcode.split(",");
            String decrypted = "";
            for (String charcode : charcodes) {
                decrypted += (char) Integer.valueOf(charcode).intValue();
            }
            String finallink = new Regex(decrypted, "href=\"(.*?)\"").getMatch(0);
            if (finallink != null) {
                if (!finallink.contains("firstload.de/affiliate")) {
                    DownloadLink dlink = createDownloadlink(finallink.trim());
                    dlink.addSourcePluginPassword("movie7.org");
                    decryptedLinks.add(dlink);
                }
            } else {
                logger.warning("An error occured! Decrypted StringfromCharCode = \"" + singlecharcode + " \"");
            }
        }
        if (packageName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(packageName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}

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

import java.io.File;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.nutils.JDHash;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

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
        br.setFollowRedirects(false);
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        String postvar = new Regex(parameter, "protected\\.socadvnet\\.com/\\?(.+)").getMatch(0);
        if (postvar == null) return null;
        br.getPage(parameter);
        if (br.getRedirectLocation() != null && br.getRedirectLocation().equals("http://protected.socadvnet.com/index.php")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
        String security = br.getRegex("(style=\"margin-top:200px;\">[\t\n\r ]+\\d+ <img  border=\"0\" style=\"margin-top: 20px;\" src=\".*?\"> (\\d+) =)").getMatch(0);
        br.postPage("http://protected.socadvnet.com/allinks.php", "LinkName=" + postvar);
        String[] linksCount = br.getRegex("(moc\\.tenvdacos\\.detcetorp//:eopp)").getColumn(0);
        if (linksCount == null || linksCount.length == 0) return null;
        int linkCounter = linksCount.length;
        if (security != null) {
            Regex numberRegex = new Regex(security, "style=\"margin-top:200px;\">[\t\n\r ]+(\\d+) <img  border=\"0\" style=\"margin-top: 20px;\" src=\"(.*?)\"> (\\d+) =");
            String num1 = numberRegex.getMatch(0);
            String num2 = numberRegex.getMatch(2);
            String aPage = numberRegex.getMatch(1);
            if (num1 == null || num2 == null || aPage == null) {
                logger.warning("Error in doing the maths for link: " + parameter);
                return null;
            }
            int equals = 0;
            File aFile = getLocalCaptchaFile();
            Browser.download(aFile, br.cloneBrowser().openGetConnection("http://protected.socadvnet.com/" + aPage));
            String hash = JDHash.getMD5(aFile);
            if (hash.equals("1022cbc696d52cb9f5d95e069c6e7c28"))
                equals = Integer.parseInt(num1) - Integer.parseInt(num2);
            else
                equals = Integer.parseInt(num1) + Integer.parseInt(num2);
            br.postPage("http://protected.socadvnet.com/cp_code.php", "res_code=" + equals);
            if (!br.toString().trim().equals("1")) {
                logger.warning("Error in doing the maths for link: " + parameter);
                return null;
            }
        }
        logger.info("Found " + linkCounter + " links, decrypting now...");
        progress.setRange(linkCounter);
        for (int i = 0; i <= linkCounter - 1; i++) {
            String actualPage = "http://protected.socadvnet.com/allinks.php?out_name=" + postvar + "&&link_id=" + i;
            br.getPage(actualPage);
            if (br.containsHTML("No htmlCode read")) {
                logger.info("Found one offline link for link " + parameter + " linkid:" + i);
                continue;
            }
            String finallink = br.getRegex("http-equiv=\"refresh\" content=\"0;url=(http.*?)\"").getMatch(0);
            if (finallink == null) {
                // Handlings for more hosters will come soon i think
                if (br.containsHTML("turbobit\\.net")) {
                    String singleProtectedLink = "http://protected.socadvnet.com/plugin/turbobit.net.free.php?out_name=" + postvar + "&link_id=" + i;
                    br.getPage(singleProtectedLink);
                    if (br.getRedirectLocation() == null) {
                        logger.warning("Redirect location for this link is null: " + parameter);
                        return null;
                    }
                    String turboId = new Regex(br.getRedirectLocation(), "http://turbobit\\.net/download/free/(.+)").getMatch(0);
                    if (turboId == null) {
                        logger.warning("There is a problem with the link: " + actualPage);
                        return null;
                    }
                    finallink = "http://turbobit.net/" + turboId + ".html";
                } else if (br.containsHTML("hotfile\\.com")) {
                    finallink = br.getRegex("style=\"margin:0;padding:0;\" action=\"(.*?)\"").getMatch(0);
                    if (finallink == null) {
                        finallink = br.getRegex("onmouseout=\"hoverFuncRemove\\(this\\)\" ><a href=\"(/dl.*?\\.html)\\?uploadid=").getMatch(0);
                        if (finallink != null) finallink = "http://hotfile.com" + finallink;
                    }
                }
            }
            if (finallink == null) {
                logger.warning("Finallink for the following link is null: " + parameter);
            }
            decryptedLinks.add(createDownloadlink(finallink));
            progress.increase(1);
        }
        return decryptedLinks;
    }
}

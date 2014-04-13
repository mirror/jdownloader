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

import java.awt.Point;
import java.io.File;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "goldesel.to" }, urls = { "http://(www\\.)?goldesel\\.to/[a-z0-9]+/\\d+.{2}" }, flags = { 0 })
public class GldSlTo extends PluginForDecrypt {

    public GldSlTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (!br.containsHTML("<h2>DDL\\-Links</h2>") && !br.containsHTML("<h2>Stream\\-Links</h2>")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }

        String fpName = br.getRegex("<title>([^<>\"]*?) \\&raquo; goldesel\\.to</title>").getMatch(0);
        if (fpName == null) fpName = new Regex(br.getURL(), "goldesel\\.to/(.+)").getMatch(0);
        fpName = Encoding.htmlDecode(fpName).trim();
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        String[] decryptIDs = br.getRegex("data=\"([^<>\"]*?)\"").getColumn(0);
        if (decryptIDs == null || decryptIDs.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        br.getHeaders().put("Accept", "*/*");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        final int maxc = decryptIDs.length;
        int counter = 1;
        for (final String decryptID : decryptIDs) {
            try {
                if (this.isAbort()) {
                    logger.info("Decryption aborted by user: " + parameter);
                    return decryptedLinks;
                }
            } catch (final Throwable e) {
                // Not available in old 0.9.581 Stable
            }
            br.postPage("http://goldesel.to/res/links", "data=" + Encoding.urlEncode(decryptID));
            boolean captchafailed = false;
            if (br.containsHTML("Klicke in den gestrichelten Kreis, der sich somit von den anderen unterscheidet")) {
                for (int i = 1; i <= 3; i++) {
                    try {
                        if (this.isAbort()) {
                            logger.info("Decryption aborted by user: " + parameter);
                            return decryptedLinks;
                        }
                    } catch (final Throwable e) {
                        // Not available in old 0.9.581 Stable
                    }
                    final String capLink = br.getRegex("\"(inc/cirlecaptcha\\.php[^<>\"]*?)\"").getMatch(0);
                    if (capLink == null) {
                        logger.warning("Decrypter broken for link: " + parameter);
                        return null;
                    }
                    final File file = this.getLocalCaptchaFile();
                    br.cloneBrowser().getDownload(file, "http://goldesel.to/" + capLink);
                    final Point p = UserIO.getInstance().requestClickPositionDialog(file, "Goldesel.to\r\nDecrypting: " + fpName + "\r\nClick-Captcha | Mirror " + counter + " / " + maxc + " : " + decryptID, "Klicke in den gestrichelten Kreis!");
                    if (p == null) {
                        logger.info("p = null --> Decrypt process or captchas aborted by user");
                        return decryptedLinks;
                    }
                    br.postPage("http://goldesel.to/res/links", "data=" + Encoding.urlEncode(decryptID) + "&xC=" + p.x + "&yC=" + p.y);
                    if (br.containsHTML("class=\"captchaWait\"")) {
                        logger.info("We have to wait because the user entered too many wrong captchas...");
                        int wait = 60;
                        String waittime = br.getRegex("<strong>(\\d+) Sekunden</strong> warten\\.").getMatch(0);
                        if (waittime != null) wait = Integer.parseInt(waittime);
                        this.sleep(wait * 1001, param);
                        continue;
                    }
                    if (br.containsHTML("Klicke in den gestrichelten Kreis, der sich somit von den anderen unterscheidet")) {
                        captchafailed = true;
                        continue;
                    }
                    captchafailed = false;
                    break;
                }
                if (captchafailed) {
                    logger.info("Captcha failed for decryptID: " + decryptID);
                    continue;
                }
            }
            final String[] finallinks = br.getRegex("url=\"(http[^<>\"]*?)\"").getColumn(0);
            for (final String finallink : finallinks) {
                final DownloadLink dl = createDownloadlink(Encoding.htmlDecode(finallink));
                dl._setFilePackage(fp);
                try {
                    distribute(dl);
                } catch (final Throwable e) {
                    // Not available in old 0.9.581 Stable
                }
                decryptedLinks.add(dl);
            }
            counter++;
        }
        if (decryptedLinks.size() == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    /* Prevent confusion */
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    // private String[] getAjaxPost(final Browser br) {
    // String[] postInfo = new String[2];
    // final Regex postInfoRegex = br.getRegex("function [A-Za-z0-9\\-_]+\\(([A-Z0-9]+)\\) \\{ \\$\\.post\\(\"(ajax[^<>\"]*?)\"");
    // if (postInfoRegex.getMatches().length != 0) {
    // postInfo[0] = "http://goldesel.to/" + postInfoRegex.getMatch(1);
    // postInfo[1] = postInfoRegex.getMatch(0);
    // } else {
    // postInfo[0] = "http://goldesel.to/ajax/jDL.php";
    // postInfo[1] = "LNK";
    // }
    // return postInfo;
    // }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}
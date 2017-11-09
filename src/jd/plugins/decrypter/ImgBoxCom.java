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
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "imgbox.com" }, urls = { "http://(www\\.)?imgbox\\.com/(g/)?[A-Za-z0-9]+" })
public class ImgBoxCom extends PluginForDecrypt {
    public ImgBoxCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String GALLERYLINK    = "http://(www\\.)?imgbox\\.com/g/[A-Za-z0-9]+";
    private static final String PICTUREOFFLINE = "The image in question does not exist|The image has been deleted due to a DMCA complaint";
    private static final String INVALIDLINKS   = "http://(www\\.)?imgbox\\.com/(help|login|privacy|register|tos|images|dmca|gallery|assets)";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        if (parameter.matches(INVALIDLINKS)) {
            logger.info("Link invalid: " + parameter);
            return decryptedLinks;
        }
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.containsHTML(">The page you (are|were) looking for") || br.getURL().contains("imgbox.com/login")) {
            final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        if (parameter.matches(GALLERYLINK)) {
            if (br.containsHTML("The specified gallery could not be found") || br.containsHTML("(?!\\d+)0 images</h1>")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            String fpName = br.getRegex("<h1 style=\"padding\\-left:15px;\">(.*?)</h1>").getMatch(0);
            if (fpName == null) {
                fpName = br.getRegex("<h1>([^<>\"]+)- \\d+ images(?:\\s+images)?</h1>").getMatch(0);
            }
            if (fpName == null) {
                fpName = "imgbox.com gallery " + new Regex(parameter, "imgbox\\.com/g/(.+)").getMatch(0);
            }
            final String[] uids = br.getRegex("(?i)<a href=(\"|')/([a-zA-Z0-9]+)\\1><img alt=(\"|')\\2[^'\"]*\\3").getColumn(1);
            if (uids == null || uids.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            FilePackage fp = null;
            if (fpName != null) {
                fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim()));
            }
            for (final String uid : uids) {
                try {
                    if (this.isAbort()) {
                        logger.info("Decryption aborted...");
                        return decryptedLinks;
                    }
                } catch (final Throwable e) {
                    // Not available in old 0.9.581 Stable
                }
                final DownloadLink dl = createDownloadlink("http://imgbox.com/" + uid);
                if (fp != null) {
                    fp.add(dl);
                }
                decryptedLinks.add(dl);
            }
        } else {
            if (br.containsHTML(PICTUREOFFLINE)) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            final DownloadLink dl = decryptSingle();
            if (dl == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(dl);
        }
        return decryptedLinks;
    }

    private DownloadLink decryptSingle() {
        final String finallink = br.getRegex("\"(https?://(i|[a-z0-9\\-]+)\\.imgbox\\.com/[^<>\"]*?)\"").getMatch(0);
        if (finallink == null) {
            return null;
        }
        return createDownloadlink("directhttp://" + Encoding.htmlDecode(finallink));
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}
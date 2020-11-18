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
import java.util.Date;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "boards.4chan.org" }, urls = { "https?://[\\w\\.]*?boards\\.(?:4chan|4channel)\\.org/[0-9a-z]{1,}/(thread/[0-9]+)?" })
public class Brds4Chnrg extends PluginForDecrypt {
    public Brds4Chnrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* TODO: Maybe implement API: https://github.com/4chan/4chan-API */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final FilePackage fp = FilePackage.getInstance();
        String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            logger.info("Link offline (404): " + parameter);
            return decryptedLinks;
        }
        if (parameter.matches("https?://[\\w\\.]*?boards\\.[^/]+/[0-9a-z]{1,}/[0-9]*")) {
            String[] threads = br.getRegex("\\[<a href=\"thread/(\\d+)").getColumn(0);
            for (String thread : threads) {
                decryptedLinks.add(createDownloadlink(parameter + "thread/" + thread));
            }
        } else {
            final String IMAGERDOMAINS = "(i\\.4cdn\\.org|is\\d*?\\.4chan\\.org|images\\.4chan\\.org)";
            String[] images = br.getRegex("(?i)File: <a (title=\"[^<>\"/]+\" )?href=\"(//" + IMAGERDOMAINS + "/[0-9a-z]{1,}/(src/)?\\d+\\.(gif|jpg|png|webm))\"").getColumn(1);
            if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("404 - Not Found")) {
                fp.setName("4chan - 404 - Not Found");
                br.getPage("//sys.4chan.org/error/404/rid.php");
                String image404 = br.getRegex("(https?://.+)").getMatch(0);
                DownloadLink dl = createDownloadlink(image404);
                dl.setAvailableStatus(AvailableStatus.TRUE);
                fp.add(dl);
                decryptedLinks.add(dl);
            } else if (images.length == 0) {
                logger.info("Empty 4chan link: " + parameter);
                return decryptedLinks;
            } else {
                String domain = "4chan.org";
                String cat = br.getRegex("<div class=\"boardTitle\">/(?:.{1,4}|trash)/\\s*-\\s*(.*?)\\s*</div>").getMatch(0);
                if (cat == null) {
                    cat = br.getRegex("<title>\\s*/b/\\s*-\\s*(.*?)\\s*</title>").getMatch(0);
                }
                if (cat != null) {
                    cat = Encoding.htmlOnlyDecode(cat);
                } else {
                    cat = "Unknown Cat";
                }
                // extract thread number from URL
                String suffix = new Regex(parameter, "/thread/([0-9]+)").getMatch(0);
                if (suffix == null) {
                    // Fall back to date if we can't resolve
                    suffix = new Date().toString();
                }
                fp.setName(domain + " - " + cat + " - " + suffix);
                /* First post = "postContainer opContainer", all others = "postContainer replyContainer" */
                final String[] posts = br.getRegex("<div class=\"postContainer [^\"]+\".*?</blockquote></div></div>").getColumn(-1);
                for (final String post : posts) {
                    String url = new Regex(post, "<a[^>]*href=\"((//|http)[^\"]+)\"").getMatch(0);
                    if (url == null) {
                        continue;
                    } else {
                        url = br.getURL(url).toString();
                    }
                    final DownloadLink dl = this.createDownloadlink(url);
                    dl.setAvailable(true);
                    String filename = new Regex(post, "<a title=\"([^\"]+)\" href=\"").getMatch(0);
                    if (filename == null) {
                        filename = new Regex(post, "target=\"_blank\">\\s*([^<>\"]+)\\s*</a>").getMatch(0);
                    }
                    if (filename != null) {
                        dl.setForcedFileName(Encoding.htmlDecode(filename).trim());
                    }
                    final String filesizeStr = new Regex(post, "\\((\\d+[^<>\"]+), \\d+x\\d+\\)").getMatch(0);
                    if (filesizeStr != null) {
                        dl.setDownloadSize(SizeFormatter.getSize(filesizeStr));
                    }
                    dl._setFilePackage(fp);
                    decryptedLinks.add(dl);
                }
                if (decryptedLinks.size() == 0) {
                    /* Fallback - old method which was used until rev 42702 */
                    for (String image : images) {
                        image = br.getURL(image).toString();
                        DownloadLink dl = createDownloadlink(image);
                        dl.setAvailableStatus(AvailableStatus.TRUE);
                        fp.add(dl);
                        decryptedLinks.add(dl);
                    }
                }
            }
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}
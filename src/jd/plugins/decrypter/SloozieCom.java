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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sloozie.com" }, urls = { "http://(www\\.)?sloozie\\.com/profile/[A-Za-z0-9\\-_]+" }, flags = { 0 })
public class SloozieCom extends PluginForDecrypt {

    public SloozieCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final int PICSPERSEGMENT = 16;

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if ("http://www.sloozie.com/".equals(br.getURL())) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        String fpName = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
        final String memberID = br.getRegex("\"mbID\":\"(\\d+)\"").getMatch(0);
        if (memberID == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        int tempCounter = 0;
        int offset = 0;
        int segmentCounter = 1;
        do {
            logger.info("Decrypting segment " + segmentCounter);
            if (offset > 0) {
                br.postPage("http://www.sloozie.com/load.php", "action=load&data=%7B%22view%22%3A%22ProfileContent%22%2C%22dest%22%3A%22divPRLoad1%22%2C%22args%22%3A%7B%22start%22%3A" + offset + "%2C%22mbID%22%3A%22" + memberID + "%22%7D%2C%22isTemplate%22%3Atrue%2C%22method%22%3A%22%22%7D");
                br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
            }
            final String[] links = br.getRegex("\"(http://(www\\.)?sloozie\\.com/(galleries|videos)/[a-z0-9]+/[^<>\"/]*?)\"").getColumn(0);
            if (links == null || links.length == 0) break;
            for (final String singleLink : links) {
                final DownloadLink dl = createDownloadlink(singleLink);
                dl.setAvailable(true);
                dl._setFilePackage(fp);
                try {
                    distribute(dl);
                } catch (final Throwable e) {
                    /* does not exist in 09581 */
                }
                decryptedLinks.add(createDownloadlink(singleLink));
            }
            tempCounter = links.length;
            offset += PICSPERSEGMENT;
            segmentCounter++;
        } while (tempCounter >= PICSPERSEGMENT && segmentCounter < 100);
        if (decryptedLinks.size() == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}
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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "e-hentai.org" }, urls = { "http://(www\\.)?g\\.e\\-hentai\\.org/g/\\d+/[a-z0-9]+" }, flags = { 0 })
public class EHentaiOrg extends PluginForDecrypt {

    public EHentaiOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        ArrayList<String> allPages = new ArrayList<String>();
        allPages.add("0");
        final String parameter = param.toString();
        br.setCookie("http://e-hentai.org", "nw", "1");
        br.getPage(parameter);
        if (br.containsHTML("Key missing, or incorrect key provided") || br.containsHTML("class=\"d\"")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        String fpName = br.getRegex("<title>([^<>\"]*?) \\- E\\-Hentai Galleries</title>").getMatch(0);
        fpName = Encoding.htmlDecode(fpName.trim());
        final String[] pages = br.getRegex("/?p=(\\d+)\" onclick=").getColumn(0);
        if (pages != null && pages.length != 0) {
            for (final String aPage : pages) {
                if (!allPages.contains(aPage)) allPages.add(aPage);
            }
        }
        final DecimalFormat df = new DecimalFormat("0000");
        int counter = 1;
        for (final String currentPage : allPages) {
            if (!currentPage.equals("0")) br.getPage(parameter + "/?p=" + currentPage);
            final String[] links = br.getRegex("\"(http://g\\.e\\-hentai\\.org/s/[a-z0-9]+/\\d+\\-\\d+)\"").getColumn(0);
            if (links == null || links.length == 0 || fpName == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String singleLink : links) {
                try {
                    if (this.isAbort()) {
                        logger.info("Decryption aborted by user: " + parameter);
                        return decryptedLinks;
                    }
                } catch (final Throwable e) {
                    // Not available in old 0.9.581 Stable
                }
                br.getPage(singleLink);
                String finallink = br.getRegex("\"(http://\\d+\\.\\d+\\.\\d+\\.\\d+(:\\d+)?/h/[^<>\"]*?)\"").getMatch(0);
                if (finallink == null) finallink = br.getRegex("src=\"(http://[^<>\"]*?image\\.php\\?[^<>\"]*?)\"").getMatch(0);
                if (finallink == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    logger.warning("Current link is: " + singleLink);
                    return null;
                }
                final DownloadLink dl = createDownloadlink("directhttp://" + finallink);
                dl.setFinalFileName(fpName + "_" + df.format(counter) + finallink.substring(finallink.lastIndexOf(".")));
                dl.setAvailable(true);
                decryptedLinks.add(dl);
                try {
                    distribute(dl);
                } catch (final Throwable e) {
                    // No available in old Stable
                }
                sleep(new Random().nextInt(5000), param);
                counter++;
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    /* NOTE: no override to keep compatible to old stable */
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}
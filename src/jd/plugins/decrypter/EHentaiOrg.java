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
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "e-hentai.org" }, urls = { "http://(?:www\\.)?g\\.e-hentai\\.org/g/(\\d+)/[a-z0-9]+" }, flags = { 0 })
public class EHentaiOrg extends PluginForDecrypt {

    public EHentaiOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String uid = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        if (uid == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "fuid can not be found");
        }
        br.setCookie("http://e-hentai.org", "nw", "1");
        br.getPage(parameter);
        if (br.containsHTML("Key missing, or incorrect key provided") || br.containsHTML("class=\"d\"")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        String fpName = br.getRegex("<title>([^<>\"]*?) - E-Hentai Galleries</title>").getMatch(0);
        if (fpName != null) {
            fpName = Encoding.htmlDecode(fpName.trim());
        }
        int pagemax = 1;
        final String[] pages = br.getRegex("/?p=(\\d+)\" onclick=").getColumn(0);
        if (pages != null && pages.length != 0) {
            for (final String aPage : pages) {
                final int pageint = Integer.parseInt(aPage);
                if (pageint > pagemax) {
                    pagemax = pageint;
                }
            }
        }
        final DecimalFormat df = new DecimalFormat("0000");
        for (int page = 0; page <= pagemax; page++) {
            try {
                if (this.isAbort()) {
                    logger.info("Decryption aborted by user: " + parameter);
                    return decryptedLinks;
                }
            } catch (final Throwable e) {
                // Not available in old 0.9.581 Stable
            }
            final Browser br2 = br.cloneBrowser();
            if (page > 0) {
                br2.getPage(parameter + "/?p=" + page);
            }
            final String[] links = br2.getRegex("\"(http://g\\.e-hentai\\.org/s/[a-z0-9]+/" + uid + "-\\d+)\"").getColumn(0);
            if (links == null || links.length == 0 || fpName == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            int counter = 1;
            for (final String singleLink : links) {
                final DownloadLink dl = createDownloadlink("http://ehentaidecrypted.org/" + System.currentTimeMillis() + new Random().nextInt(1000000000));
                final String namepart = fpName + "_" + df.format(page);
                dl.setProperty("mainlink", parameter);
                dl.setProperty("individual_link", singleLink);
                dl.setProperty("namepart", namepart);
                dl.setName(namepart + new Regex(singleLink, "([A-Za-z0-9]+)/?$").getMatch(0) + ".jpg");
                dl.setAvailable(true);
                try {
                    dl.setContentUrl(singleLink);
                } catch (final Throwable e) {
                    /* Not available in old 0.9.581 Stable */
                    dl.setBrowserUrl(singleLink);
                }
                decryptedLinks.add(dl);
                try {
                    distribute(dl);
                } catch (final Throwable e) {
                    // No available in old Stable
                }
                counter++;
            }
            sleep(new Random().nextInt(5000), param);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    /* NOTE: no override to keep compatible to old stable */
    public int getMaxConcurrentProcessingInstances() {
        /* Too many processes = server hates us */
        return 1;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}
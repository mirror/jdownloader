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
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "urlgalleries.net" }, urls = { "http://(www\\.)?[a-z0-9_]+\\.urlgalleries\\.net/blog_gallery\\.php\\?id=\\d+" }, flags = { 0 })
public class RlGalleriesNt extends PluginForDecrypt {

    private static String agent = null;

    public RlGalleriesNt(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final String parameter = param.toString();
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        br.setReadTimeout(3 * 60 * 1000);
        // br.setCookie(".urlgalleries.net", "popundr", "1");
        if (agent == null) {
            /* we first have to load the plugin, before we can reference it */
            JDUtilities.getPluginForHost("mediafire.com");
            agent = jd.plugins.hoster.MediafireCom.stringUserAgent();
        }
        br.getHeaders().put("User-Agent", agent);
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        br.getPage(parameter);
        if (br.containsHTML("<title> \\- urlgalleries\\.net</title>")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final String host = new Regex(parameter, "(https?://[^/]+\\.urlgalleries\\.net)").getMatch(0);
        final String fpName = br.getRegex("border=\\'0\\' /></a></div>(.*?)</td></tr><tr>").getMatch(0);
        final String[] links = br.getRegex("\\'(/image\\.php\\?cn=\\d+\\&uid=[A-Za-z0-9]+\\&where=.*?)\\'").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        FilePackage fp = FilePackage.getInstance();
        if (fpName != null) {
            fp.setName(fpName.trim());
        }
        int counter = 1;
        final Browser brc = br.cloneBrowser();
        for (final String aLink : links) {
            try {
                if (isAbort()) {
                    logger.info("Decryption process aborted by user, stopping...");
                    break;
                }
            } catch (final Throwable e) {
            }
            logger.info("Decrypting link " + counter + " of " + links.length);
            sleep(new Random().nextInt(3) + 1000, param);
            try {
                brc.getPage(host + aLink);
            } catch (final Exception e) {
                logger.info("Link timed out: " + aLink);
                counter++;
                continue;
            }
            final String finallink = brc.getRedirectLocation();
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            // new Random().nextInt(10)
            final DownloadLink lol = createDownloadlink(finallink);
            // Give temp name so we have no same filenames
            lol.setName(Integer.toString(new Random().nextInt(1000000000)));
            if (fp.getName() != null) fp.add(lol);
            decryptedLinks.add(lol);
            try {
                distribute(lol);
            } catch (final Throwable e) {
                // No available in old Stable
            }
            logger.info(finallink);
            counter++;
        }

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}
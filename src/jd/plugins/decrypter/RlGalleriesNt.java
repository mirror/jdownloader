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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "urlgalleries.net" }, urls = { "http://(www\\.)?[a-z0-9]+\\.urlgalleries\\.net/blog_gallery\\.php\\?id=\\d+" }, flags = { 0 })
public class RlGalleriesNt extends PluginForDecrypt {

    public RlGalleriesNt(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        final String fpName = br.getRegex("border=\\'0\\' /></a></div>(.*?)</td></tr><tr>").getMatch(0);
        final String[] links = br.getRegex("\\'(/image\\.php\\?cn=\\d+\\&uid=[A-Za-z0-9]+\\&where=.*?)\\'").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final String host = new Regex(parameter, "(http://(www\\.)?[a-z0-9]+\\.urlgalleries\\.net)").getMatch(0);
        for (String aLink : links) {
            try {
                if (isAbort()) break;
            } catch (final Throwable e) {
            }
            sleep(300, param);
            Browser brc = br.cloneBrowser();
            brc.getPage(host + aLink);
            String finallink = brc.getRedirectLocation();
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            // new Random().nextInt(10)
            final DownloadLink lol = createDownloadlink(finallink);
            // Give temp name so we have no same filenames
            lol.setName(Integer.toString(new Random().nextInt(1000000000)));
            decryptedLinks.add(lol);
            logger.info(finallink);
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}

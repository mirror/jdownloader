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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ex.ua" }, urls = { "http://(www\\.)?ex\\.ua/get/[0-9]+/[0-9]+" }, flags = { 0 })
public class Xa extends PluginForDecrypt {

    public Xa(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        if (br.getRedirectLocation() != null && br.getRedirectLocation().equals("http://www.ex.ua/")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        String fpName = br.getRegex("<title>(.*?)@ EX\\.UA</title>").getMatch(0);
        if (parameter.contains("/view/")) {
            String[] linksandmd5 = br.getRegex("'(/get/[0-9]+.*?md5:[0-9a-z]+)'").getColumn(0);
            if (linksandmd5 != null) {
                for (String pagepiece : linksandmd5) {
                    String md5hash = new Regex(pagepiece, "md5:([a-z0-9]+)").getMatch(0);
                    String cryptedlink = new Regex(pagepiece, "(/get/[0-9]+)").getMatch(0);
                    br.getPage("http://www.ex.ua" + cryptedlink);
                    String finallink = br.getRedirectLocation();
                    if (finallink == null) {
                        logger.warning("Decrypter broken for link: " + parameter);
                        return null;
                    }
                    DownloadLink decryptedlink = createDownloadlink("directhttp://" + finallink);
                    if (md5hash != null) {
                        decryptedlink.setMD5Hash(md5hash.trim());
                    }
                    // Errorhandling for offline links, adding them makes no
                    // sense!
                    if (!finallink.contains("http://www.ex.ua/online")) {
                        decryptedLinks.add(decryptedlink);
                    }
                }

            } else {
                String[] links = br.getRegex("'(/get/[0-9]+)'").getColumn(0);
                if (links.length == 0) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                for (String cryptedlink : links) {
                    br.getPage("http://www.ex.ua" + cryptedlink);
                    String finallink = br.getRedirectLocation();
                    if (finallink == null) {
                        logger.warning("Decrypter broken for link: " + parameter);
                        return null;
                    }
                    decryptedLinks.add(createDownloadlink("directhttp://" + finallink));
                }
            }
            if (fpName != null && !fpName.trim().equals("kein titel")) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(fpName.trim());
                fp.addLinks(decryptedLinks);
            }

        } else {
            String finallink = br.getRedirectLocation();
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink("directhttp://" + finallink));
        }
        return decryptedLinks;
    }
}

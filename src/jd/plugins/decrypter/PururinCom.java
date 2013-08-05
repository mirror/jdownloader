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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pururin.com" }, urls = { "http://(www\\.)?pururin\\.com/(gallery/\\d+/[a-z0-9\\-]+\\.html|view/[^<>\"]+\\.html)" }, flags = { 0 })
public class PururinCom extends PluginForDecrypt {

    public PururinCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String GALLERYLINK = "http://(www\\.)?pururin\\.com/gallery/\\d+/[a-z0-9\\-]+\\.html";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        if (parameter.matches(GALLERYLINK)) {
            parameter = parameter.replace("/gallery/", "/thumbs/");
            br.getPage(parameter);
            if (br.containsHTML(">Page not found")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            final String fpName = br.getRegex("<h1>([^<>\"]*?) Thumbnails</h1>").getMatch(0);
            final String[] links = br.getRegex("\"(/view/\\d+/\\d+/[a-z0-9\\-_]+\\.html)\"").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (String link : links) {
                try {
                    if (this.isAbort()) {
                        logger.info("Decryption aborted...");
                        return decryptedLinks;
                    }
                } catch (final Throwable e) {
                    // Not available in old 0.9.581 Stable
                }
                link = "http://pururin.com" + link;
                br.getPage(link);
                final DownloadLink dl = decryptSingle();
                if (dl == null) {
                    logger.warning("Decrypter broken at link: " + link);
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                try {
                    distribute(dl);
                } catch (final Throwable e) {
                    // Not available in old 0.9.581 Stable
                }
                decryptedLinks.add(dl);
            }
            if (fpName != null) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim()));
                fp.addLinks(decryptedLinks);
            }
        } else {
            br.getPage(parameter);
            if (br.containsHTML(">Page not found")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            final DownloadLink dl = decryptSingle();
            if (dl == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            try {
                distribute(dl);
            } catch (final Throwable e) {
                // Not available in old 0.9.581 Stable
            }
            decryptedLinks.add(dl);
        }

        return decryptedLinks;
    }

    private DownloadLink decryptSingle() {
        final String finallink = br.getRegex("\"(/f/[^<>\"]*?)\"").getMatch(0);
        if (finallink == null) return null;
        final DownloadLink dl = createDownloadlink("directhttp://http://pururin.com" + finallink);
        dl.setAvailable(true);
        return dl;
    }

}

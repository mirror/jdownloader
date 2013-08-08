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

import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "movie25.com" }, urls = { "http://(www\\.)?movie25\\.com/(movies/[a-z0-9\\-]+\\.html|watch[a-z0-9\\-]+\\.html)" }, flags = { 0 })
public class Mv25Com extends PluginForDecrypt {

    public Mv25Com(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String SINGLELINK = "http://(www\\.)?movie25\\.com/watch[a-z0-9\\-]+\\.html";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getURL().equals("http://www.movie25.com/404.shtml")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (parameter.matches(SINGLELINK)) {
            final DownloadLink dl = decryptSingleLink();
            if (dl == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(dl);
        } else {
            final String fpName = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
            final String[] links = br.getRegex("class=\"playing_button\"><span><a href=(http://(www\\.)?movie25\\.com/watch[a-z0-9\\-]+\\.html) target=\"_blank\"").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String singleLink : links) {
                try {
                    if (this.isAbort()) {
                        logger.info("Decryption aborted for link: " + parameter);
                        logger.info("On singleLink: " + singleLink);
                        return decryptedLinks;
                    }
                } catch (final Throwable e) {
                    // Not available in old 0.9.581 Stable
                }
                br.getPage(singleLink);
                final DownloadLink dl = decryptSingleLink();
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

            if (fpName != null) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim()));
                fp.addLinks(decryptedLinks);
            }
        }

        return decryptedLinks;
    }

    private DownloadLink decryptSingleLink() throws IOException {
        final String finallink = br.getRegex("location\\.href=\\\\\\'(http[^<>\"]*?)\\'\\\\\"").getMatch(0);
        if (finallink == null) return null;
        return createDownloadlink(finallink);
    }

}

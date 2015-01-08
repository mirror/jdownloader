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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "privatehomeclips.com" }, urls = { "http://(www\\.)?privatehomeclips\\.com/albums/[a-z0-9\\-_]+/" }, flags = { 0 })
public class PrivateHomeClipsComDecrypter extends PluginForDecrypt {

    public PrivateHomeClipsComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
            offline.setFinalFileName(new Regex(parameter, "https?://[^<>\"/]+/(.+)").getMatch(0));
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        String fpName = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
        if (fpName == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        String next = null;
        int counter = 1;
        final DecimalFormat df = new DecimalFormat("0000");
        do {
            try {
                if (this.isAbort()) {
                    logger.info("Decryption aborted by user: " + parameter);
                    return decryptedLinks;
                }
            } catch (final Throwable e) {
                // Not available in old 0.9.581 Stable
            }
            if (next != null) {
                br.getPage(next);
            }
            final String[] links = br.getRegex("data\\-lightbox=\"example\\-set\" href=\"(http://[^<>\"]*?\\.jpg)\"").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String singleLink : links) {
                final DownloadLink dl = createDownloadlink("directhttp://" + singleLink);
                dl.setFinalFileName(fpName + "_" + df.format(counter) + ".jpg");
                dl.setAvailable(true);
                decryptedLinks.add(dl);
                counter++;
            }
            next = br.getRegex("class=\"next\"><a data=\\'2\\' href=\"(/albums/[^<>\"/]+/\\d+/)\"").getMatch(0);
        } while (next != null);

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

}

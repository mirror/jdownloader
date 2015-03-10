//jDownloader - Downloadmanager
//Copyright (C) 2008  JD-Team support@jdownloader.org
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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hentai-foundry.com" }, urls = { "http://www\\.hentai\\-foundry\\.com/pictures/user/[A-Za-z0-9\\-_]+(/\\d+)?" }, flags = { 0 })
public class HentaiFoundryComGallery extends PluginForDecrypt {

    public HentaiFoundryComGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String type_single = "http://www\\.hentai\\-foundry\\.com/pictures/user/[A-Za-z0-9\\-_]+/\\d+";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setReadTimeout(3 * 60 * 1000);
        br.setFollowRedirects(true);
        String parameter = param.toString();
        if (parameter.matches(type_single)) {
            decryptedLinks.add(createDownloadlink(parameter.replace("hentai-foundry.com/", "hentai-foundrydecrypted.com/")));
            return decryptedLinks;
        }
        br.getPage(parameter + "?enterAgree=1&size=0");
        final String fpName = new Regex(parameter, "/user/(.+)").getMatch(0);
        int page = 1;
        String next = null;
        do {
            try {
                if (this.isAbort()) {
                    logger.info("Decryption aborted by user: " + parameter);
                    return decryptedLinks;
                }
            } catch (final Throwable e) {
                // Not available in old 0.9.581 Stable
            }
            logger.info("Decrypting page " + page);
            if (page > 1) {
                br.getPage(next);
            }
            String[] links = br.getRegex("<td class=\\'thumb_square\\'(.*?)<table><tr>").getColumn(0);
            if (links == null || links.length == 0) {
                return null;
            }
            for (String link : links) {
                String title = new Regex(link, "class=\"thumbTitle\">([^<>]*?)</span>").getMatch(0);
                final String url = new Regex(link, "\"(/pictures/user/[A-Za-z0-9\\-_]+/\\d+[^<>\"]*?)\"").getMatch(0);
                if (title == null || url == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                title = Encoding.htmlDecode(title).trim();
                title = encodeUnicode(title);
                final DownloadLink dl = createDownloadlink("http://www.hentai-foundrydecrypted.com" + url);
                dl.setName(title + ".png");
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
            next = br.getRegex("class=\"next\"><a href=\"(/pictures/user/[A-Za-z0-9\\-_]+/page/\\d+)\"").getMatch(0);
            page++;
        } while (next != null);
        FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    /* Avoid chars which are not allowed in filenames under certain OS' */
    private static String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "/");
        output = output.replace("\\", "");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }

}

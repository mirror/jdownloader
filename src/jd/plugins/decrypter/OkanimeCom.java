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
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "okanime.com" }, urls = { "http://(?:www\\.)?okanime\\.com/\\?post_type=episode\\&p=\\d+" }) 
public class OkanimeCom extends PluginForDecrypt {

    public OkanimeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String fpName = br.getRegex(">([^<>\"]*?)</a></div>[\t\n\r ]*?</div>[\t\n\r ]*?<div class=\"howto\"").getMatch(0);
        if (fpName == null) {
            fpName = new Regex(parameter, "(\\d+)$").getMatch(0);
        }
        String episodenumber = this.br.getRegex("class=\"numbeerr\" style=\"[^<>\"]+\">(\\d+)</div>").getMatch(0);
        if (episodenumber == null) {
            episodenumber = this.br.getRegex("<title>OKanime \\| (\\d+)</title>").getMatch(0);
        }
        if (episodenumber != null) {
            fpName += " - Episode " + episodenumber;
        }
        final String[] links = br.getRegex("onclick=\"FRAME\\d+\\.location\\.href=\\'(http[^<>\"]*?)\\'\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String singleLink : links) {
            if (this.isAbort()) {
                logger.info("Decryption aborted by user");
                return decryptedLinks;
            }
            this.br.getPage(singleLink);
            final String html = this.br.getRegex("<div id=\"apDiv[A-Za-z0-9]+\">(.*?)</div>").getMatch(0);
            if (html == null) {
                return null;
            }
            final String[] downloadlinks = HTMLParser.getHttpLinks(html, "");
            for (final String downloadlink : downloadlinks) {
                final DownloadLink dl = createDownloadlink(downloadlink);
                decryptedLinks.add(dl);
                distribute(dl);
            }
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

}

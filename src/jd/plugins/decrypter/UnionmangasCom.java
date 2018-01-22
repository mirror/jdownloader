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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "unionmangas.com" }, urls = { "https?://(?:www\\.)?unionmangas\\.(?:com|net)/(?:leitor/[^/]+/[a-z0-9\\.]+[^/\\s]*|manga/[a-z0-9\\-\\.]+)" })
public class UnionmangasCom extends antiDDoSForDecrypt {
    public UnionmangasCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        this.br.setFollowRedirects(true);
        getPage(parameter);
        final String url_fpname;
        final String url_name;
        if (parameter.matches(".+/leitor/.+")) {
            /* Decrypt single chapter */
            if (br.getHttpConnection().getResponseCode() == 404 || !this.br.getURL().contains("/leitor/")) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            final Regex urlinfo = new Regex(parameter, "unionmangas\\.(?:com|net)/leitor/([^/]+)/([a-z0-9\\-\\.]+)");
            final String chapter_str = urlinfo.getMatch(1);
            url_name = urlinfo.getMatch(0);
            url_fpname = Encoding.urlDecode(url_name + "_chapter_" + chapter_str, false);
            String[] links = this.br.getRegex("data\\-lazy=\"(http[^<>\"]+)\"").getColumn(0);
            if (links == null || links.length == 0) {
                links = this.br.getRegex("<img\\s*src=\"(.*?\\.(jpe?g|png|gif))\"").getColumn(0);
            }
            if (links == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (String url : links) {
                url = br.getURL(url).toString();
                final DownloadLink dl = this.createDownloadlink("directhttp://" + Encoding.urlEncode_light(url), false);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
        } else {
            url_name = new Regex(parameter, "/([^/]+)$").getMatch(0);
            url_fpname = url_name;
            if (br.getHttpConnection().getResponseCode() == 404 || !this.br.getURL().contains("/manga/")) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            final String[] chapterUrls = this.br.getRegex("\"(https?://unionmangas\\.net/leitor/[^\"\\']+)\"").getColumn(0);
            for (final String chapterUrl : chapterUrls) {
                decryptedLinks.add(this.createDownloadlink(chapterUrl));
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(url_fpname);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }
}

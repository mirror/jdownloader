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
import jd.http.Browser;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "allmovie.tv" }, urls = { "https?://(?:www\\.)?allmovie\\.(?:tv|pro)/video/[a-z0-9\\-]+\\-\\d+\\.html" })
public class AllmovieTv extends PluginForDecrypt {

    public AllmovieTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final Regex urlregex = new Regex(parameter, "([a-z0-9\\-]+)\\-(\\d+)\\.html$");
        final String url_filename = urlregex.getMatch(0);
        final String videoid = urlregex.getMatch(1);
        final String currenthost = Browser.getHost(parameter);

        this.br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        this.br.getHeaders().put("Referer", parameter);
        this.br.getPage("http://" + currenthost + "/video/show_player/" + videoid + "?autopay=1&skip_ads=1");

        // br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }

        final String iframe_src = this.br.getRegex("<iframe.*?</iframe>").getMatch(-1);
        if (iframe_src != null) {
            final String[] links = HTMLParser.getHttpLinks(iframe_src, "");
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String singleLink : links) {
                decryptedLinks.add(createDownloadlink(singleLink));
            }
        } else {
            final String finallink = this.br.getRegex("source src=\"(https?://[^<>\"]+\\.mp4)\"").getMatch(0);
            if (finallink == null) {
                return null;
            }
            final DownloadLink dl = createDownloadlink(finallink);
            dl.setFinalFileName(url_filename + ".mp4");
            decryptedLinks.add(dl);
        }

        return decryptedLinks;
    }
}

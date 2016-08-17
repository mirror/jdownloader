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
import jd.http.Browser.BrowserException;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "mycrazyvids.com" }, urls = { "http://(www\\.)?mycrazyvids\\.com/([a-z0-9\\-_]+\\-\\d+\\.html|\\?go=click\\&c=\\d+\\&n=\\d+\\&e=\\d+\\&g=\\d+\\&r=\\d+\\&u=http[^<>\"/]+)" }) 
public class MyCrazyVidsCom extends PornEmbedParser {

    public MyCrazyVidsCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        if (parameter.matches("http://(www\\.)?mycrazyvids\\.com/\\?go=click\\&c=\\d+\\&n=\\d+\\&e=\\d+\\&g=\\d+\\&r=\\d+\\&u=http[^<>\"/]+")) {
            String externLink = new Regex(parameter, "\\&u=(http[^<>\"/]+)").getMatch(0);
            externLink = Encoding.deepHtmlDecode(externLink);
            decryptedLinks.add(createDownloadlink(externLink));
        } else {
            try {
                br.getPage(parameter);
            } catch (final BrowserException e) {
                logger.info("Cannot decrypt link, either offline or server error: " + parameter);
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            if (br.containsHTML(">404 Not Found<") || this.br.getHttpConnection().getResponseCode() == 404) {
                logger.info("Cannot decrypt link, either offline or server error: " + parameter);
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            String filename = br.getRegex("<h1 class=\"name\">([^<>]*?)</h1>").getMatch(0);
            decryptedLinks.addAll(findEmbedUrls(filename));
            if (decryptedLinks.isEmpty()) {
                return null;
            }
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}
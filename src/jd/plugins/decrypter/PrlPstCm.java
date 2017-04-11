//jDownloader - Downloadmanager
//Copyright (C) 2014  JD-Team support@jdownloader.org
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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

/**
 * @author raztoki
 * */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "prialepaste.com", "anonymizer.link" }, urls = { "https?://[\\w\\.]*prialepaste\\.com/(f[a-z]{0,1}\\.php#[a-zA-Z0-9\\+-/=]+|p/[A-Z0-9]{8})", "https?://[\\w\\.]*anonymizer\\.link/(f[a-z]{0,1}\\.php#[a-zA-Z0-9\\+-/=]+|p/[A-Z0-9]{8})" }) 
public class PrlPstCm extends PluginForDecrypt {

    public PrlPstCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String host = Browser.getHost(parameter);
        String finallink = null;
        if (parameter.contains(host + "/f")) {
            br.getPage(parameter.substring(0, parameter.indexOf("#")));
            String hash = new Regex(parameter, "#(.+)$").getMatch(0);
            String redirect = br.getRegex("var url_redir\\s*=\\s*\"(https?://[^/]+/redirect[^\"]+)").getMatch(0);
            if (hash != null && redirect != null) {
                br.getPage(redirect + Encoding.urlEncode(hash));
                finallink = br.getRedirectLocation();
                decryptedLinks.add(createDownloadlink(finallink));
            }
        } else {
            // /p/uid support
            br.getPage(parameter);
            if (br.containsHTML("Entrada no existe!")) {
                logger.info("Not a valid URL, or plugin could be broken, please confirm in your personal Web Browser! " + parameter);
                return decryptedLinks;
            }
            String filter = br.getRegex("<table[^>]+>(.*?)</table>").getMatch(0);
            if (filter == null) {
                logger.warning("filter == null");
                return null;
            }
            String[] links = new Regex(filter, "href\\s*=\\s*(\"|')([a-z]+://.*?)\\1").getColumn(1);
            if (links != null) {
                for (String link : links) {
                    decryptedLinks.add(createDownloadlink(link));
                }
            } else {
                // not finding links isn't a error!
            }
        }

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}
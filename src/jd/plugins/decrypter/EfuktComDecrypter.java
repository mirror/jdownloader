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

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "efukt.com" }, urls = { "https?://(?:www\\.)?efukt\\.com/(\\d+[A-Za-z0-9_\\-]+\\.html|out\\.php\\?id=\\d+|view\\.gif\\.php\\?id=\\d+)" })
public class EfuktComDecrypter extends antiDDoSForDecrypt {
    public EfuktComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        if (parameter.matches(".+view\\.gif\\.php.+")) {
            /* Pass this to host plugin */
            decryptedLinks.add(this.createDownloadlink(parameter));
            return decryptedLinks;
        }
        setBrowserExclusive();
        br.setFollowRedirects(false);
        getPage(parameter);
        String redirect = br.getRedirectLocation();
        if (redirect == null) {
            redirect = this.br.getRegex("window\\.location[\t\n\r ]*?=[\t\n\r ]*?\\'(https?[^<>\"]*?)\\';").getMatch(0);
        }
        if (redirect != null && !redirect.contains("efukt.com/")) {
            decryptedLinks.add(createDownloadlink(redirect));
            return decryptedLinks;
        } else if (redirect != null) {
            getPage(redirect);
            redirect = br.getRedirectLocation();
            if (redirect == null) {
                redirect = this.br.getRegex("window\\.location[\t\n\r ]*?=[\t\n\r ]*?\\'(https?[^<>\"]*?)\\';").getMatch(0);
            }
            if (redirect != null && !redirect.contains("efukt.com/")) {
                decryptedLinks.add(createDownloadlink(redirect));
                return decryptedLinks;
            }
            br.followRedirect(true);
        }
        final DownloadLink main = createDownloadlink(parameter);
        if (br.getURL().equals("http://efukt.com/") || br.getURL().equals("https://efukt.com/")) {
            main.setFinalFileName(new Regex(parameter, "https?://efukt\\.com/(.+)").getMatch(0));
            main.setAvailable(false);
            main.setProperty("offline", true);
            decryptedLinks.add(main);
            return decryptedLinks;
        }
        if (br.containsHTML("flashplayer") || br.containsHTML("videoplayer_contents") || br.getURL().contains("view.gif")) {
            decryptedLinks.add(main);
        } else {
            /* We should have a picture gallery */
            String title = new Regex(parameter, "efukt\\.com/(\\d+[A-Za-z0-9_\\-]+)\\.html").getMatch(0);
            title = Encoding.htmlDecode(title);
            title = title.trim();
            String[] pics = br.getRegex("<a target=\"_blank\" href=\"(/content/[^<>\"]*?)\"").getColumn(0);
            if (pics == null || pics.length == 0) {
                pics = br.getRegex("img\\s*src\\s*=\\s*\"(https?://media\\.efukt\\.com/[^\"<>]*)\"\\s*onerror=").getColumn(0);
            }
            if (pics == null || pics.length == 0) {
                pics = br.getRegex("<img\\s*?src\\s*?=\\s*?\"(https?://media\\.efukt\\.com/[^\"<>]*\\.(gif|webm|jpg))\"\\s*?alt=\".*?\"\\s*?class=\"image_content\"").getColumn(0);
            }
            if (pics == null || pics.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String pic : pics) {
                final DownloadLink dl = createDownloadlink("directhttp://" + br.getURL(pic));
                decryptedLinks.add(dl);
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(title);
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}

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
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "multiurl.com" }, urls = { "http://(www\\.)?multiurl\\.com/(s|l)/[A-Za-z0-9]+" }, flags = { 0 })
public class MultiUrlCom extends PluginForDecrypt {

    public MultiUrlCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_SINGLE = "http://(www\\.)?multiurl\\.com/s/[A-Za-z0-9]+";
    private static final String TYPE_LIST   = "http://(www\\.)?multiurl\\.com/l/[A-Za-z0-9]+";

    @SuppressWarnings("unchecked")
    private Browser prepBrowser(final Browser prepBr) {
        // loading previous cookie session results in less captchas
        final Object ret = this.getPluginConfig().getProperty("cookies", null);
        if (ret != null) {
            final HashMap<String, String> cookies = (HashMap<String, String>) ret;
            for (Map.Entry<String, String> entry : cookies.entrySet()) {
                prepBr.setCookie(this.getHost(), entry.getKey(), entry.getValue());
            }
        }
        prepBr.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        prepBr.getHeaders().put("Pragma", null);
        prepBr.getHeaders().put("Accept-Charset", null);
        prepBr.setFollowRedirects(true);
        return prepBr;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        prepBrowser(this.br);
        br.getPage(parameter);
        if (br.containsHTML("class=\"msgerr\"")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (parameter.matches(TYPE_LIST)) {
            final String[] links = br.getRegex("\"(http://(www\\.)?multiurl\\.com/s/[A-Za-z0-9]+)\" class=\"out\"").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String link : links) {
                decryptedLinks.add(createDownloadlink(link));
            }
        } else {
            if (br.containsHTML("id=\"link_password\"")) {
                for (int i = 1; i <= 3; i++) {
                    final String passCode = getUserInput("Password?", param);
                    br.postPage(br.getURL(), "dosecure=1&password=" + Encoding.urlEncode(passCode) + "&submit=Submit");
                    if (br.containsHTML("id=\"link_password\"")) {
                        continue;
                    }
                    break;
                }
                if (br.containsHTML("id=\"link_password\"")) throw new DecrypterException(DecrypterException.PASSWORD);
            }

            // saving session info can result in you not having to enter a password for each new link viewed!
            final HashMap<String, String> cookies = new HashMap<String, String>();
            final Cookies add = br.getCookies(this.getHost());
            for (final Cookie c : add.getCookies()) {
                cookies.put(c.getKey(), c.getValue());
            }
            this.getPluginConfig().setProperty("cookies", cookies);
            this.getPluginConfig().save();

            final String link = br.getRegex("id =\"url_label\" value=\"(http[^<>\"]*?)\"").getMatch(0);
            if (link == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(link));
        }

        return decryptedLinks;
    }

    public int getMaxConcurrentProcessingInstances() {
        // Set to 1 to prevent a lot of simultan passwqord prompts
        return 1;
    }

}

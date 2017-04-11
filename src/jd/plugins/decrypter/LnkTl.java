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
import java.util.LinkedHashMap;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.components.PluginJSonUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "link.tl" }, urls = { "http://(www\\.)?link\\.tl/(?!advertising)[A-Za-z0-9\\-]+" })
public class LnkTl extends antiDDoSForDecrypt {

    public LnkTl(PluginWrapper wrapper) {
        super(wrapper);
    }

    private Browser ajax = null;

    private void ajaxPostPage(final String url, final LinkedHashMap<String, String> param) throws Exception {
        ajax = br.cloneBrowser();
        ajax.getHeaders().put("Accept", "*/*");
        ajax.getHeaders().put("Connection-Type", "application/x-www-form-urlencoded");
        ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        postPage(ajax, url, param);
    }

    /**
     * Important note: Via browser the videos are streamed via RTMP (maybe even in one part) but with this method we get HTTP links which is
     * fine.
     */
    // NOTE: Similar plugins: BcVc, AdliPw, AdcrunCh,
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        br = new Browser();
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(false);
        getPage(parameter);

        /* Check for direct redirect */
        String redirect = br.getRedirectLocation();
        if (redirect == null) {
            redirect = br.getRegex("top\\.location\\.href = \"(http[^<>\"]*?)\"").getMatch(0);
        }
        if (redirect != null && !redirect.contains("link.tl/")) {
            decryptedLinks.add(createDownloadlink(redirect));
            return decryptedLinks;
        }

        if (br.getURL().equals("http://link.tl/") || br.containsHTML("top\\.location\\.href = \"http://link\\.tl/\"") || br.containsHTML(">404 Not Found<") || br.containsHTML(">Sorry the page you are looking for does not exist|>Üzgünüz, ulaşmaya çalışmış olduğunuz kısaltma sistemde yer almamaktadır\\.<")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (br.containsHTML("Unable to connect to database")) {
            logger.info("Link can't be decrypted because of server problems: " + parameter);
            return decryptedLinks;
        }
        String packed = null;
        final String cryptedScripts[] = br.getRegex("p\\}\\((.*?)\\.split\\('\\|'\\)").getColumn(0);
        if (cryptedScripts != null && cryptedScripts.length != 0) {
            for (String crypted : cryptedScripts) {
                packed = decodeDownloadLink(crypted);
                if (packed != null) {
                    break;
                }
            }
            if (packed == null) {
                packed = "";
            }
        }
        final String[] matches = new Regex(packed, "aid\\:(.*?)\\,lid\\:(.*?)\\,oid\\:(.*?)\\}").getRow(0);
        final String[] post = new Regex(packed, "\\$\\.post\\('(https?://link\\.tl/fly/.*?\\.php)',\\{opt").getColumn(0);
        if (matches == null || matches.length == 0 || post == null || post.length == 0) {
            logger.warning("Possible Decrypter broken for link: " + parameter);
            return decryptedLinks;
        }
        LinkedHashMap<String, String> data = new LinkedHashMap<String, String>();
        // first repeated twice
        data.put("opt", "check_log");
        data.put(Encoding.urlEncode("args[lid]"), matches[1]);
        data.put(Encoding.urlEncode("args[oid]"), matches[2]);
        ajaxPostPage(post[0], data);
        ajaxPostPage(post[0], data);

        // waittime is 5 seconds. but somehow this often results in an error.
        // we use 5.5 seconds to avoid them
        sleep(5500, param);

        // second
        data.put("opt", "make_log");
        data.put(Encoding.urlEncode("args[aid]"), matches[0]);
        ajaxPostPage(post[post.length - 1], data);

        String url = PluginJSonUtils.getJsonValue(ajax, "url");
        if (url == null) {
            // maybe we have to wait even longer?
            sleep(2000, param);
            ajaxPostPage(post[post.length - 1], data);
            url = PluginJSonUtils.getJsonValue(ajax, "url");
        }
        if (url.contains("link.tl/fly/go.php?")) {
            getPage(url);
            url = br.getRegex("href=\"([^\"]+)\"\\s*>Skip!<").getMatch(0);
            if (url != null && url.contains("/fly/site.php")) {
                getPage(url);
                // skip
                url = br.getRegex("<div class=\"skip\">\\s*<a href=\"(.*?)\"").getMatch(0);
                if (url == null) {
                    // they also load iframe
                    url = br.getRegex("<iframe class=\"site_frame\" [^>]*src=\"(.*?)\"").getMatch(0);
                }
            }
        }

        decryptedLinks.add(createDownloadlink(url));
        return decryptedLinks;
    }

    /**
     * @param source
     *            String for decoder to process
     * @return String result
     */
    protected String decodeDownloadLink(final String s) {
        String decoded = null;

        try {
            Regex params = new Regex(s, "'(.*?[^\\\\])',(\\d+),(\\d+),'(.*?)'");

            String p = params.getMatch(0).replaceAll("\\\\", "");
            int a = Integer.parseInt(params.getMatch(1));
            int c = Integer.parseInt(params.getMatch(2));
            String[] k = params.getMatch(3).split("\\|");

            while (c != 0) {
                c--;
                if (k[c].length() != 0) {
                    p = p.replaceAll("\\b" + Integer.toString(c, a) + "\\b", k[c]);
                }
            }

            decoded = p;
        } catch (Exception e) {
        }
        return decoded;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}
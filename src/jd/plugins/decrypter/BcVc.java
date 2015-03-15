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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

/**
 * Note: using cloudflare, has simlar link structure/behaviour to adfly
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bc.vc" }, urls = { "https?://(?:www\\.)?bc\\.vc/([A-Za-z0-9]{5,6}$|\\d+/(?:ftp|http).+)" }, flags = { 0 })
public class BcVc extends antiDDoSForDecrypt {

    public BcVc(PluginWrapper wrapper) {
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
    // NOTE: Similar plugins: BcVc, AdliPw, AdcrunCh
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        {
            final String linkInsideLink = new Regex(parameter, "/\\d+/((?:http|ftp).+)").getMatch(0);
            if (linkInsideLink != null) {
                if (!linkInsideLink.matches(this.getHost() + "/.+")) {
                    decryptedLinks.add(createDownloadlink(linkInsideLink));
                    return decryptedLinks;
                } else {
                    parameter = linkInsideLink;
                }
            }
        }
        // we have to rename them here because we can damage urls within urls.
        // - parameters containing www. will always be offline.
        // - https never returns results, doesn't work in browser either.
        parameter = parameter.replaceFirst("://www.", "://").replaceFirst("https://", "http://");

        br.setFollowRedirects(false);
        getPage(parameter);

        /* Check for direct redirect */
        String redirect = br.getRedirectLocation();
        if (redirect == null) {
            redirect = br.getRegex("top\\.location\\.href = \"((?:http|ftp)[^<>\"]*?)\"").getMatch(0);
        }
        if (redirect != null && !redirect.contains("bc.vc/")) {
            decryptedLinks.add(createDownloadlink(redirect));
            return decryptedLinks;
        }

        if (br.getURL().matches("https?://(?:www\\.)?bc.vc/") || br.containsHTML("top\\.location\\.href = \"https?://(?:www\\.)?bc\\.vc/\"") || br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">404 Not Found<") || br.containsHTML(">Sorry the page you are looking for does not exist")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (br.containsHTML("Unable to connect to database")) {
            logger.info("Link can't be decrypted because of server problems: " + parameter);
            return decryptedLinks;
        }
        final String[] matches = br.getRegex("aid\\:(.*?)\\,lid\\:(.*?)\\,oid\\:(.*?)\\,ref\\: ?\\'(.*?)\\'\\}").getRow(0);
        if (matches == null || matches.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        LinkedHashMap<String, String> data = new LinkedHashMap<String, String>();
        // first
        data.put("opt", "checks_log");
        ajaxPostPage("/fly/ajax.fly.php", data);

        // second repeated twice
        data.put("opt", "check_log");
        data.put(Encoding.urlEncode("args[aid]"), matches[0]);
        data.put(Encoding.urlEncode("args[lid]"), matches[1]);
        data.put(Encoding.urlEncode("args[oid]"), matches[2]);
        data.put(Encoding.urlEncode("args[ref]"), "");
        ajaxPostPage("/fly/ajax.fly.php", data);
        ajaxPostPage("/fly/ajax.fly.php", data);

        // waittime is 5 seconds. but somehow this often results in an error.
        // we use 5.5 seconds to avoid them
        Thread.sleep(5500);

        // third
        data.put("opt", "make_log");
        data.put(Encoding.urlEncode("args[nok]"), "no");
        ajaxPostPage("/fly/ajax.fly.php", data);

        String url = jd.plugins.hoster.K2SApi.JSonUtils.getJson(ajax, "url");
        if (url == null) {
            // maybe we have to wait even longer?
            Thread.sleep(2000);
            ajaxPostPage("/fly/ajax.fly.php", data);
            url = jd.plugins.hoster.K2SApi.JSonUtils.getJson(ajax, "url");
        }
        if (url != null) {
            decryptedLinks.add(createDownloadlink(url));
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}
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
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.components.PluginJSonUtils;

/**
 * Note: using cloudflare, has simlar link structure/behaviour to adfly
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bc.vc" }, urls = { "https?://(?:www\\.)?bc\\.vc/(\\d+/.+|[A-Za-z0-9]{5,7})" })
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
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        {
            final String linkInsideLink = new Regex(parameter, "bc\\.vc/\\d+/(.+)").getMatch(0);
            if (linkInsideLink != null) {
                final String finalLinkInsideLink;
                if (StringUtils.startsWithCaseInsensitive(linkInsideLink, "http") || StringUtils.startsWithCaseInsensitive(linkInsideLink, "ftp")) {
                    finalLinkInsideLink = linkInsideLink;
                } else {
                    finalLinkInsideLink = "http://" + linkInsideLink;
                }
                if (!StringUtils.containsIgnoreCase(finalLinkInsideLink, getHost() + "/")) {
                    final DownloadLink link = createDownloadlink(finalLinkInsideLink);
                    link.setProperty("Referer", param.toString());
                    decryptedLinks.add(link);
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
        if (StringUtils.endsWithCaseInsensitive(redirect, "//bc.vc/7") || br.getURL().matches("https?://(?:www\\.)?bc.vc/") || br.containsHTML("top\\.location\\.href = \"https?://(?:www\\.)?bc\\.vc/\"") || br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">404 Not Found<") || br.containsHTML(">Sorry the page you are looking for does not exist")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (br.containsHTML("Unable to connect to database")) {
            logger.info("Link can't be decrypted because of server problems: " + parameter);
            return decryptedLinks;
        }
        {
            // old method
            final String[] matches = br.getRegex("aid\\s*:\\s*(.*?)\\s*,\\s*lid\\s*:\\s*(.*?)\\s*,\\s*oid:\\s*(.*?)\\s*,\\s*ref\\s*:\\s*\\'(.*?)\\'\\s*\\}").getRow(0);
            if (matches != null) {
                final LinkedHashMap<String, String> data = new LinkedHashMap<String, String>();
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
                sleep(5500, param);

                // third
                data.put("opt", "make_log");
                data.put(Encoding.urlEncode("args[nok]"), "no");
                ajaxPostPage("/fly/ajax.fly.php", data);
                String url = PluginJSonUtils.getJsonValue(ajax, "url");
                if (url == null) {
                    // maybe we have to wait even longer?
                    sleep(2000, param);
                    ajaxPostPage("/fly/ajax.fly.php", data);
                    url = PluginJSonUtils.getJsonValue(ajax, "url");
                }
                if (url != null) {
                    decryptedLinks.add(createDownloadlink(url));
                }
            } else {
                // new method, way less requests.. really should use inhouse js
                String javascript = br.getRegex("(\\$\\.post\\('https?://bc\\.vc/fly/ajax\\.php\\?.*?\\}),\\s*function").getMatch(0);
                if (javascript == null) {
                    throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
                }
                // from here we have construct some ajax request
                javascript = javascript.replaceFirst(":\\s*tZ(,?)", ": '480'$1");
                javascript = javascript.replaceFirst(":\\s*cW(,?)", ": '1920'$1");
                javascript = javascript.replaceFirst(":\\s*cH(,?)", ": '984'$1");
                javascript = javascript.replaceFirst(":\\s*sW(,?)", ": '1920'$1");
                javascript = javascript.replaceFirst(":\\s*sH(,?)", ": '1080'$1");
                // last figure time from clicking button and some x and y math
                String url = new Regex(javascript, "'(http://bc\\.vc/fly/.*?)'").getMatch(0);
                url += "1845,30:71.01:20:1457";
                final LinkedHashMap<String, String> data = new LinkedHashMap<String, String>();
                // enter all parameters within map
                final Regex regex = new Regex(javascript, "(\\w+):\\s*\\{(.*?)\\},?");
                final String[] parm = regex.getRow(0);
                if (parm != null) {
                    final String[][] keyValue = new Regex(parm[1], "\\s*(\\w+):\\s*'(.*?)',?\\s*").getMatches();
                    if (keyValue == null) {
                        throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
                    }
                    for (final String[] kV : keyValue) {
                        data.put(parm[0] + Encoding.urlEncode("[" + kV[0] + "]"), Encoding.urlEncode(kV[1]));
                    }
                }
                javascript = javascript.replaceAll(Pattern.quote(regex.getMatch(-1)), "");
                // others
                final String[][] other = new Regex(javascript, "\\s*(\\w+):\\s*'(.*?)',?\\s*").getMatches();
                for (final String[] o : other) {
                    data.put(Encoding.urlEncode(o[0]), Encoding.urlEncode(o[1]));
                }
                // sleep(6000, param);
                ajaxPostPage(url, data);
                final String link = PluginJSonUtils.getJsonValue(ajax, "url");
                if (link != null) {
                    decryptedLinks.add(createDownloadlink(link));
                }
            }

        }

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}
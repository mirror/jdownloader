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

import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.requests.FormData;
import jd.http.requests.PostFormDataRequest;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.components.PluginJSonUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "link.tl" }, urls = { "https?://(www\\.)?link\\.tl/(?!advertising|\\w+/.+)?[A-Za-z0-9\\-]{4,}" })
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
        if (redirect != null) {
            if (!redirect.contains("link.tl/")) {
                decryptedLinks.add(createDownloadlink(redirect));
                return decryptedLinks;
            } else {
                br.followRedirect();
            }
        }
        if (br.getURL().matches("https?://link.tl/") || br.containsHTML("top\\.location\\.href = \"https?://link\\.tl/\"") || br.containsHTML(">404 Not Found<") || br.containsHTML(">Sorry the page you are looking for does not exist|>Üzgünüz, ulaşmaya çalışmış olduğunuz kısaltma sistemde yer almamaktadır\\.<")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (br.containsHTML("Unable to connect to database")) {
            logger.info("Link can't be decrypted because of server problems: " + parameter);
            return decryptedLinks;
        }
        String packed = null;
        {
            final String cryptedScripts[] = br.getRegex("eval\\((function\\(p,a,c,k,e,d\\)[^\r\n]+\\))\\)").getColumn(0);
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
        }
        // 20180221
        final String[] vars = new Regex(packed, "var \\w+\\s*=\\s*function\\(\\)\\s*\\{.*?\\};").getColumn(-1);
        // should be two and they are in order.
        // time delay
        this.sleep(1800, param);
        if (vars != null) {
            for (final String var : vars) {
                final String url = new Regex(var, "url\\s*:\\s*'([^']+)'").getMatch(0);
                final String unique = new Regex(var, "data\\.append\\('unique'\\s*,\\s*'([a-f0-9]{13})'\\)").getMatch(0);
                final String xcsrf = new Regex(var, "'X-CSRF-TOKEN'\\s*:\\s*'([A-Za-z0-9]+)'").getMatch(0);
                if (url == null || unique == null || xcsrf == null) {
                    continue;
                }
                final PostFormDataRequest r = br.createPostFormDataRequest(url);
                r.addFormData(new FormData("width", "1920"));
                r.addFormData(new FormData("height", "1050"));
                r.addFormData(new FormData("browser_width", "1693"));
                r.addFormData(new FormData("browser_height", "949"));
                r.addFormData(new FormData("width", "1920"));
                r.addFormData(new FormData("incognito_browser", "0"));
                r.addFormData(new FormData("adblock", "0"));
                r.addFormData(new FormData("unique", unique));
                r.getHeaders().put("X-CSRF-TOKEN", xcsrf);
                r.getHeaders().put("Accept", "*/*");
                ajax = br.cloneBrowser();
                sendRequest(ajax, r);
                // waittime is 5 seconds. but somehow this often results in an error.
                // we use 5.5 seconds to avoid them
                sleep(5500, param);
            }
        }
        String url = PluginJSonUtils.getJsonValue(ajax, "url");
        if (url != null && url.contains("link.tl/d/")) {
            getPage(url);
            br.followRedirect();
            {
                final String cryptedScripts[] = br.getRegex("eval\\((function\\(p,a,c,k,e,d\\)[^\r\n]+\\))\\)").getColumn(0);
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
            }
            url = new Regex(packed, "window\\.location\\.href='([^\"]+)'").getMatch(0);
            // should be /i/uid
            if (url != null && url.contains("link.tl/i/")) {
                getPage(url);
                // skip
                url = br.getRegex("<div class=\"skip\">\\s*<a href=\"(.*?)\"").getMatch(0);
                if (url == null) {
                    // they also load iframe
                    url = br.getRegex("<iframe class=\"site_frame\" [^>]*src=\"(.*?)\"").getMatch(0);
                }
            }
        }
        if (url != null) {
            decryptedLinks.add(createDownloadlink(url));
        }
        return decryptedLinks;
    }

    /**
     * @param source
     *            String for decoder to process
     * @return String result
     */
    protected String decodeDownloadLink(final String s) {
        final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(null);
        final ScriptEngine engine = manager.getEngineByName("javascript");
        String result = null;
        try {
            engine.eval("var res = " + s + ";");
            result = (String) engine.get("res");
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}
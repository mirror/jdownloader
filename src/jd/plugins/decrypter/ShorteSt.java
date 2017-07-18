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
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Request;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.components.SiteType.SiteTemplate;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class ShorteSt extends antiDDoSForDecrypt {

    // add new domains here.
    private static final String[] domains = { "sh.st", "viid.me", "wiid.me", "skiip.me", "clkme.me", "clkmein.com", "clkme.in", "destyy.com", "festyy.com", "corneey.com", "gestyy.com", "ceesty.com" };

    public ShorteSt(PluginWrapper wrapper) {
        super(wrapper);
    }

    private boolean containsLoginRedirect(final String input) {
        if (input == null) {
            return false;
        }
        final String redirect = Request.getLocation(input, br.getRequest());
        final boolean result = redirect.matches("(?i)" + HOSTS.get() + "/login");
        return result;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("//clkme.in/", "//cllkme.com/");
        if (parameter.contains("%29")) {
            parameter = parameter.replace("%29", ")");
            parameter = parameter.replace("%28", "(");
            parameter = parameter.replace("_", "i");
            parameter = parameter.replace("*", "u");
            parameter = parameter.replace("!", "a");
        }
        getPage(parameter);
        final String redirect = br.getRegex("<meta http-equiv=\"refresh\" content=\"\\d+\\;url=(.*?)\" \\/>").getMatch(0);
        if (containsLoginRedirect(redirect)) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        } else if (redirect != null) {
            parameter = redirect;
            getPage(parameter);
        }
        if (br.containsHTML(">page not found<")) {
            if (!parameter.contains("!/")) {
                logger.info("Link offline: " + parameter);
                decryptedLinks.add(createOfflinelink(parameter));
            }
            return decryptedLinks;
        }
        final String timer = PluginJSonUtils.getJsonValue(br, "seconds");
        final String cb = PluginJSonUtils.getJsonValue(br, "callbackUrl");
        final String sid = PluginJSonUtils.getJsonValue(br, "sessionId");
        if (cb == null || sid == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        int t = 5;
        if (timer != null) {
            t = Integer.parseInt(timer);
        }
        sleep(t * 1001, param);
        final Browser br2 = br.cloneBrowser();
        br2.getHeaders().put("Accept", "application/json, text/javascript");
        br2.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
        br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        postPage(br2, cb, "adSessionId=" + sid + "&callback=reqwest_" + new Regex(String.valueOf(new Random().nextLong()), "(\\d{10})$").getMatch(0));
        final String finallink = PluginJSonUtils.getJsonValue(br2, "destinationUrl");
        if (finallink == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        decryptedLinks.add(createDownloadlink(finallink.replaceAll(" ", "%20")));
        return decryptedLinks;
    }

    private static AtomicReference<String> HOSTS           = new AtomicReference<String>(null);
    private static AtomicLong              HOSTS_REFERENCE = new AtomicLong(-1);

    @Override
    public void init() {
        // first run -1 && revision change == sync.
        if (this.getVersion() > HOSTS_REFERENCE.get()) {
            HOSTS.set(getHostsPattern());
            HOSTS_REFERENCE.set(this.getVersion());
        }
    }

    @Override
    public String[] siteSupportedNames() {
        return domains;
    }

    /**
     * Returns the annotations names array
     *
     * @return
     */
    public static String[] getAnnotationNames() {
        return new String[] { "shorte.st" };
    }

    /**
     * returns the annotation pattern array
     *
     */
    public static String[] getAnnotationUrls() {
        // construct pattern
        final String host = getHostsPattern();
        return new String[] { host + "/[^<>\r\n\t]+" };
    }

    private static String getHostsPattern() {
        final StringBuilder pattern = new StringBuilder();
        for (final String name : domains) {
            pattern.append((pattern.length() > 0 ? "|" : "") + Pattern.quote(name));
        }
        final String hosts = "https?://(www\\.)?" + "(?:" + pattern.toString() + ")";
        return hosts;
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.ShorteSt_ShorteSt;
    }

}

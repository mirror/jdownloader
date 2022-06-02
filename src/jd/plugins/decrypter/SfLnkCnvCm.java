//jDownloader - Downloadmanager
//Copyright (C) 2015  JD-Team support@jdownloader.org
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
import java.util.HashSet;
import java.util.List;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

/**
 * c & user values are required in order to not have faked output in other sections of the site.
 *
 * @author raztoki
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class SfLnkCnvCm extends antiDDoSForDecrypt {
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        ret.add(new String[] { "safelinkconverter.com" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/.+");
        }
        return ret.toArray(new String[0]);
    }

    public SfLnkCnvCm(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        br = new Browser();
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        final String b64 = Encoding.htmlDecode(new Regex(param.getCryptedUrl(), "\\?id=([a-zA-Z0-9_/\\+\\=\\-%]+)").getMatch(0));
        if (b64 == null) {
            return null;
        }
        // c value is nearly always 1, user value is important.
        // if not present lets just try to decrypt id, as sometimes its not obstructed, other times it is...
        if (!new Regex(param.getCryptedUrl(), "(\\?|\\&)user=\\d+").matches()) {
            final HashSet<String> results = jd.plugins.decrypter.GenericBase64Decrypter.handleBase64Decode(b64);
            for (final String result : results) {
                decryptedLinks.add(createDownloadlink(result));
            }
            return decryptedLinks;
        }
        br.setFollowRedirects(true);
        getPage(param.getCryptedUrl().replace("http://", "https://"));
        String link = null;
        if (StringUtils.containsIgnoreCase(br.getURL(), "safelinkconverter.com/decrypt-\\d+/")) {
            // stuff that ends up going to /decrypted-2/ with solvemedia can be bypassed.
            getPage(br.getURL().replace("/decrypt-2/", "/decrypt/"));
        } else if (br.containsHTML("decrypt.safelinkconverter")) {
            link = br.getRegex("onclick=\"window\\.open\\('(.*?)'").getMatch(0);
            if (link != null) {
                getPage(link);
            }
        }
        link = br.getRedirectLocation();
        if (link == null) {
            // decrypted.safelinkconverter.com or safelinkconverter.com/decrypt/
            link = br.getRegex("<div class=\"redirect_url\">\\s*<a href=\"(.*?)\"").getMatch(0);
            if (link == null) {
                link = br.getRegex("<div class=\"redirect_url\">\\s*<div onclick=\"window\\.open\\('(.*?)'").getMatch(0);
            }
        }
        if (link != null) {
            decryptedLinks.add(createDownloadlink(link));
        }
        return decryptedLinks;
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}
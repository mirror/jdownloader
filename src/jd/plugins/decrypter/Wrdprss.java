//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Pattern;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Request;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3,

        names = { "hd-area.org", "movie-blog.org", "doku.cc", "hoerbuch.in", "hd-area.org", "hi10anime.com", "watchseries-online.pl", "scene-rls.com", "urbanmusicdaily.me", "ddmkv.me", "links.ddmkv.me" },

        urls = { "https?://(www\\.)?hd-area\\.org/\\d{4}/\\d{2}/\\d{2}/.+", "https?://(www\\.)?movie-blog\\.org/\\d{4}/\\d{2}/\\d{2}/.+", "https?://(www\\.)?doku\\.cc/\\d{4}/\\d{2}/\\d{2}/.+", "https?://(www\\.)?hoerbuch\\.in/blog\\.php\\?id=[\\d]+", "https?://(www\\.)?hd-area\\.org/index\\.php\\?id=\\d+", "https?://(www\\.)?hi10anime\\.com/([\\w\\-]+/){2}", "https?://(\\w+\\.)?watchseries-online\\.(?:ch|pl)/episode/.+", "https?://(www\\.)?scene-rls\\.com/[\\w-]+/?$", "https?://(www\\.)?urbanmusicdaily\\.me/videos/[\\w\\-]+/", "https?://(www\\.)?ddmkv\\.me/\\d{4}/\\d{2}/[\\w\\-]+\\.html", "https?://(www\\.)?links\\.ddmkv\\.me/\\?p=\\d+" }

)
public class Wrdprss extends antiDDoSForDecrypt {

    private HashMap<String, String[]> defaultPasswords = new HashMap<String, String[]>();

    public Wrdprss(PluginWrapper wrapper) {
        super(wrapper);

        /* Die defaultpasswörter der einzelnen seiten */
        defaultPasswords.put("doku.cc", new String[] { "doku.cc", "doku.dl.am" });
        defaultPasswords.put("hd-area.org", new String[] { "hd-area.org" });
        defaultPasswords.put("movie-blog.org", new String[] { "movie-blog.org", "movie-blog.dl.am" });
    }

    @Override
    protected boolean useRUA() {
        return true;
    }

    private String parameter = null;

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        parameter = param.toString().replace("watchseries-online.ch/", "watchseries-online.pl/");

        getPage(parameter);

        /* Defaultpasswörter der Seite setzen */
        final ArrayList<String> link_passwds = new ArrayList<String>();
        final String[] passwords = defaultPasswords.get(this.getHost());
        if (passwords != null) {
            for (final String password : passwords) {
                link_passwds.add(password);
            }
        }
        final ArrayList<String[]> customHeaders = new ArrayList<String[]>();
        if (parameter.matches(".+hi10anime\\.com.+")) {
            customHeaders.add(new String[] { "Referer", br.getURL() });
        }
        /* Passwort suchen */
        final String password = br.getRegex(Pattern.compile("<.*?>Passwor(?:t|d)[<|:].*?[>|:]\\s*(.*?)[\\||<]", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (password != null) {
            link_passwds.add(password.trim());
        }
        /* Alle Parts suchen */
        final String[] links = br.getRegex(Pattern.compile("href=.*?((?:(?:https?|ftp):)?//[^\"']{2,}|(&#x[a-f0-9]{2};)+)", Pattern.CASE_INSENSITIVE)).getColumn(0);
        final HashSet<String> dupe = new HashSet<String>();
        for (String link : links) {
            if (link.matches("(&#x[a-f0-9]{2};)+")) {
                // decode
                link = HTMLEntities.unhtmlentities(link);
            }
            link = Request.getLocation(link, br.getRequest());
            if (!dupe.add(link)) {
                continue;
            }
            if (kanHandle(link)) {
                final DownloadLink dLink = createDownloadlink(link);
                if (link_passwds != null && link_passwds.size() > 0) {
                    dLink.setSourcePluginPasswordList(link_passwds);
                }
                if (!customHeaders.isEmpty()) {
                    dLink.setProperty("customHeader", customHeaders);
                }
                decryptedLinks.add(dLink);
            }
        }
        if (parameter.contains("urbanmusicdaily.me/")) {
            // lets look for embeded types
            String[] embed = br.getRegex("file\\s*:\\s*(\"|')(https?://.*?)\\1").getColumn(1);
            if (embed != null) {
                for (final String link : embed) {
                    if (kanHandle(link)) {
                        decryptedLinks.add(createDownloadlink(link));
                    }
                }
            }
            embed = br.getRegex("<iframe[^>]*>.*?</iframe>").getColumn(-1);
            if (embed != null) {
                for (final String link : embed) {
                    String l = new Regex(link, "src=(\"|')(.*?)\\1").getMatch(1);
                    if (inValidate(l)) {
                        l = new Regex(link, "src=([^\\s]*)").getMatch(0);
                    }
                    if (l != null && kanHandle(link)) {
                        decryptedLinks.add(createDownloadlink(l));
                    }
                }
            }
        }

        return decryptedLinks;
    }

    public boolean kanHandle(final String link) {
        final boolean ch = !canHandle(link);
        if (!ch) {
            return ch;
        }
        if ("hd-area.org".equalsIgnoreCase(this.getHost())) {
            return !link.matches(".+\\.(css|xml)(.*)?|.+://img\\.hd-area\\.org/.+");
        }
        if ("urbanmusicdaily.me".equalsIgnoreCase(this.getHost())) {
            return !link.contains("urbanmusicdaily.me") && !link.matches(".+(\\.|/)(css|xml|jpe?g|png|gif|ico).*");
        }
        return !link.matches(".+\\.(css|xml)(.*)?");
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}
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

import jd.PluginWrapper;
import jd.controlling.DistributeData;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {}, flags = {})
public class Wrdprss extends antiDDoSForDecrypt {
    /**
     * Returns the annotations names array
     *
     * @return
     */
    public static String[] getAnnotationNames() {
        return new String[] { "Wrdprss" };
    }

    /**
     * returns the annotation pattern array
     *
     * @return
     */
    public static String[] getAnnotationUrls() {

        StringBuilder completePattern = new StringBuilder();
        completePattern.append("https?://(\\w+\\.)?(");
        completePattern.append("(cinetopia\\.ws/.*\\.html)");
        String[] listType1 = { "hd-area.org", "movie-blog.org", "doku.cc" };
        for (String pattern : listType1) {
            completePattern.append("|(" + pattern.replaceAll("\\.", "\\\\.") + "/\\d{4}/\\d{2}/\\d{2}/.+)");
        }
        String[] listType2 = { "hoerbuch.in", "serien-blog.com" };
        for (String pattern : listType2) {
            completePattern.append("|(" + pattern.replaceAll("\\.", "\\\\.") + "/blog\\.php\\?id=[\\d]+)");
        }
        completePattern.append("|hd-area\\.org/index\\.php\\?id=\\d+");
        completePattern.append("|hi10anime\\.com/([\\w\\-]+/){2}");
        completePattern.append("|watchseries-online\\.ch/episode/.+");
        completePattern.append(")");
        // System.out.println(("Wrdprss: " + (10 + listType1.length + listType2.length) + " Pattern added!"));
        return new String[] { completePattern.toString() };
    }

    /**
     * Returns the annotations flags array
     *
     * @return
     */
    public static int[] getAnnotationFlags() {

        return new int[] { 0 };
    }

    private HashMap<String, String[]> defaultPasswords = new HashMap<String, String[]>();

    public Wrdprss(PluginWrapper wrapper) {
        super(wrapper);

        /* Die defaultpasswörter der einzelnen seiten */
        defaultPasswords.put("doku.cc", new String[] { "doku.cc", "doku.dl.am" });
        defaultPasswords.put("hd-area.org", new String[] { "hd-area.org" });
        defaultPasswords.put("movie-blog.org", new String[] { "movie-blog.org", "movie-blog.dl.am" });
        defaultPasswords.put("cinetopia.ws", new String[] { "cinetopia.ws" });
    }

    // @Override
    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        getPage(parameter);

        /* Defaultpasswörter der Seite setzen */
        ArrayList<String> link_passwds = new ArrayList<String>();
        for (String host : defaultPasswords.keySet()) {
            if (br.getHost().toLowerCase().contains(host)) {
                for (String password : defaultPasswords.get(host)) {
                    link_passwds.add(password);
                }
                break;
            }
        }
        ArrayList<String[]> customHeaders = new ArrayList<String[]>();
        if (parameter.matches(".+hi10anime\\.com.+")) {
            customHeaders.add(new String[] { "Referer", br.getURL() });
        }
        /* Passwort suchen */
        String password = br.getRegex(Pattern.compile("<.*?>Passwor(?:t|d)[<|:].*?[>|:]\\s*(.*?)[\\||<]", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (password != null) {
            link_passwds.add(password.trim());
        }
        /* Alle Parts suchen */
        String[] links = br.getRegex(Pattern.compile("href=.*?((?:(?:https?|ftp):)?//[^\"']{2,}|(&#x[a-f0-9]{2};)+)", Pattern.CASE_INSENSITIVE)).getColumn(0);
        progress.setRange(links.length);
        final String protocol = new Regex(br.getURL(), "^https?:").getMatch(-1);
        final HashSet<String> dupe = new HashSet<String>();
        for (String link : links) {
            if (link.matches("(&#x[a-f0-9]{2};)+")) {
                // decode
                link = HTMLEntities.unhtmlentities(link);
            }
            if (!dupe.add(link)) {
                progress.increase(1);
                continue;
            }
            // respect current protocol under RFC
            if (link.matches("^//.+")) {
                link = protocol + link;
            }
            // this will construct basic relative path
            else if (link.matches("^/.+")) {
                link = protocol + "//" + Browser.getHost(br.getURL(), true) + link;
            }
            if (!new Regex(link, this.getSupportedLinks()).matches() && DistributeData.hasPluginFor(link, true) && !link.matches(".+\\.(css|xml)(.*)?|.+://img\\.hd-area\\.org/.+")) {
                DownloadLink dLink = createDownloadlink(link);
                if (link_passwds != null && link_passwds.size() > 0) {
                    dLink.setSourcePluginPasswordList(link_passwds);
                }
                if (!customHeaders.isEmpty()) {
                    dLink.setProperty("customHeader", customHeaders);
                }
                decryptedLinks.add(dLink);
            }
            progress.increase(1);
        }

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}
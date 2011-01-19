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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.DistributeData;
import jd.controlling.JDLogger;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {}, flags = {})
public class Wrdprss extends PluginForDecrypt {
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
        completePattern.append("http://[\\w\\.]*?(");
        completePattern.append("(game-blog\\.us/game-.+\\.html)");
        completePattern.append("|(cinetopia\\.ws/.*\\.html)");
        completePattern.append("|(load-it\\.biz/[^/]*/?)");
        completePattern.append("|(guru-world\\.net/wordpress/\\d+.*)");
        completePattern.append("|(klee\\.tv/blog/\\d+.*)");
        completePattern.append("|(blogload\\.org/\\d+.*)");
        completePattern.append("|(pressefreiheit\\.ws/[\\d]+/.+\\.html)");
        completePattern.append("|(zeitungsjunge\\.info/.*?/.*?/.*?/)");
        completePattern.append("|(serien-blog\\.com/download/[\\d]+/.+\\.html)");
        String[] listType1 = { "hd-area.org", "movie-blog.org", "doku.cc", "sound-blog.org" };
        for (String pattern : listType1) {
            completePattern.append("|(" + pattern.replaceAll("\\.", "\\\\.") + "/\\d{4}/\\d{2}/\\d{2}/.+)");
        }
        String[] listType2 = { "hoerbuch.in", "serien-blog.com" };
        for (String pattern : listType2) {
            completePattern.append("|(" + pattern.replaceAll("\\.", "\\\\.") + "/blog\\.php\\?id=[\\d]+)");
        }
        String[] listType3 = { "sky-porn.info/blog", "best-movies.us/enter", "ladekabel.us/enter", "xxx-blog.org/blog", "ladekabel.us" };
        for (String pattern : listType3) {
            completePattern.append("|(" + pattern.replaceAll("\\.", "\\\\.") + "/\\?p=[\\d]+)");
        }
        completePattern.append(")");
        JDLogger.getLogger().finest("Wrdprss: " + (10 + listType1.length + listType2.length + listType3.length) + " Pattern added!");
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
        defaultPasswords.put("xxx-blog.org", new String[] { "xxx-blog.org", "xxx-blog.dl.am" });
        defaultPasswords.put("zeitungsjunge.info", new String[] { "www.zeitungsjunge.info" });
        defaultPasswords.put("sound-blog.org", new String[] { "sound-blog.org" });
        defaultPasswords.put("cinetopia.ws", new String[] { "cinetopia.ws" });
        defaultPasswords.put("ladekabel.us", new String[] { "Ladekabel.us" });
        defaultPasswords.put("load-it.biz", new String[] { "load-it.biz" });
        defaultPasswords.put("blogload.org", new String[] { "blogload.org" });
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        // System.out.println(param);
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.getPage(parameter);

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

        /* Passwort suchen */
        String password = br.getRegex(Pattern.compile("<.*?>Passwort[<|:].*?[>|:]\\s*(.*?)[\\||<]", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (password != null) link_passwds.add(password.trim());
        /* Alle Parts suchen */
        String[] links = br.getRegex(Pattern.compile("href=.*?(http://[^\"']+)", Pattern.CASE_INSENSITIVE)).getColumn(0);
        progress.setRange(links.length);
        for (String link : links) {
            if (!new Regex(link, this.getSupportedLinks()).matches() && DistributeData.hasPluginFor(link, true)) {
                DownloadLink dLink = createDownloadlink(link);
                dLink.setSourcePluginPasswordList(link_passwds);
                decryptedLinks.add(dLink);
                progress.increase(1);
            }
        }

        return decryptedLinks;
    }

    // @Override

}

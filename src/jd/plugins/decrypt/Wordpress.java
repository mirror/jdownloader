//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.plugins.decrypt;

import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class Wordpress extends PluginForDecrypt {

    private ArrayList<String[]> defaultPasswords = new ArrayList<String[]>();
    private Vector<String> passwordPattern = new Vector<String>();
    @SuppressWarnings("unused")
    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?(hd-area\\.org/\\d{4}/\\d{2}/\\d{2}/.+|movie-blog\\.org/\\d{4}/\\d{2}/\\d{2}/.+|hoerbuch\\.in/blog\\.php\\?id=[\\d]+|doku\\.cc/\\d{4}/\\d{2}/\\d{2}/.+|xxx-blog\\.org/blog\\.php\\?id=[\\d]+|sky-porn\\.info/blog/\\?p=[\\d]+|best-movies\\.us/\\?p=[\\d]+|game-blog\\.us/game-.+\\.html|pressefreiheit\\.ws/[\\d]+/.+\\.html).*", Pattern.CASE_INSENSITIVE);

    public Wordpress(PluginWrapper wrapper) {
        super(wrapper);
        add_defaultpasswords();
        add_passwordpatterns();
    }

    private void add_defaultpasswords() {
        /* Die defaultpasswörter der einzelnen seiten */
        /* Host, defaultpw1, defaultpw2, usw */
        defaultPasswords.add(new String[] { "doku.cc", "doku.cc", "doku.dl.am" });
        defaultPasswords.add(new String[] { "hd-area.org", "hd-area.org" });
        defaultPasswords.add(new String[] { "movie-blog.org", "movie-blog.org", "movie-blog.dl.am" });
        defaultPasswords.add(new String[] { "xxx-blog.org", "xxx-blog.org", "xxx-blog.dl.am" });
    }

    private void add_passwordpatterns() {
        /* diese Pattern dienen zum auffinden des Passworts */
        /* ACHTUNG: passwort muss an erster stelle im pattern sein */
        passwordPattern.add("<b>Passwort\\:<\\/b> (.*?) \\|");
        passwordPattern.add("<b>Passwort\\:<\\/b> (.*?)<br><\\/p>");
        passwordPattern.add("<b>Passwort\\:<\\/b> (.*?)<\\/p>");
        passwordPattern.add("<strong>Passwort\\:<\\/strong> (.*?) \\|");
        passwordPattern.add("<strong>Passwort\\: <\\/strong>(.*?)<strong>");
        passwordPattern.add("<strong>Passwort<\\/strong>\\: (.*?) <strong>");
        passwordPattern.add("<strong>Passwort\\: <\\/strong>(.*?)<\\/p>");
        passwordPattern.add("<strong>Passwort\\:<\\/strong> (.*?)<\\/p>");
        passwordPattern.add("<strong>Passwort\\:<\\/strong> (.*?) <\\/p>");
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.getPage(parameter);

        /* Defaultpasswörter der Seite setzen */
        Vector<String> link_passwds = new Vector<String>();
        for (String[] passwords : defaultPasswords) {
            if (br.getHost().toLowerCase().contains(passwords[0])) {
                for (String password : passwords) {
                    link_passwds.add(password);
                }
                break;
            }
        }

        /* Passwort suchen */
        String[] password = null;
        for (String pattern : passwordPattern) {
            password = br.getRegex(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE)).getColumn(0);
            if (password.length != 0) {
                for (String element : password) {
                    link_passwds.add(Encoding.htmlDecode(element));
                }
                break;
            }
        }

        /* Alle Parts suchen */
        String[] links = br.getRegex(Pattern.compile("<a(.*?)href=\"(http://.*?)\"", Pattern.CASE_INSENSITIVE)).getColumn(1);
        for (String link : links) {
            if (!new Regex(link, this.getSupportedLinks()).matches()) {
                DownloadLink dLink = createDownloadlink(link);
                dLink.setSourcePluginPasswords(link_passwds);
                decryptedLinks.add(dLink);
            }
        }

        return decryptedLinks;
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }
}
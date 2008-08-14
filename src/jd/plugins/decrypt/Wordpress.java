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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.controlling.DistributeData;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;

public class Wordpress extends PluginForDecrypt {
    static private final String host = "Wordpress Parser";
    private ArrayList<String[]> defaultpasswords = new ArrayList<String[]>();
    private Vector<String> passwordpattern = new Vector<String>();
    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?(hd-area\\.org/\\d{4}/\\d{2}/\\d{2}/.+|movie-blog\\.org/\\d{4}/\\d{2}/\\d{2}/.+|hoerbuch\\.in/blog\\.php\\?id=[\\d]+|doku\\.cc/\\d{4}/\\d{2}/\\d{2}/.+|xxx-blog\\.org/blog\\.php\\?id=[\\d]+|sky-porn\\.info/blog/\\?p=[\\d]+|best-movies\\.us/\\?p=[\\d]+|game-blog\\.us/game-.+\\.html|pressefreiheit\\.ws/[\\d]+/.+\\.html).*", Pattern.CASE_INSENSITIVE);

    public Wordpress() {
        super();
        add_defaultpasswords();
        add_passwordpatterns();
    }

    private void add_defaultpasswords() {
        /* Die defaultpasswörter der einzelnen seiten */
        /* Host, defaultpw1, defaultpw2, usw */
        defaultpasswords.add(new String[] { "doku.cc", "doku.cc", "doku.dl.am" });
        defaultpasswords.add(new String[] { "hd-area.org", "hd-area.org" });
        defaultpasswords.add(new String[] { "movie-blog.org", "movie-blog.org", "movie-blog.dl.am" });
        defaultpasswords.add(new String[] { "xxx-blog.org", "xxx-blog.org", "xxx-blog.dl.am" });
    }

    private void add_passwordpatterns() {
        /* diese Pattern dienen zum auffinden des Passworts */
        /* ACHTUNG: passwort muss an erster stelle im pattern sein */
        passwordpattern.add("<b>Passwort\\:<\\/b> (.*?) \\|");
        passwordpattern.add("<b>Passwort\\:<\\/b> (.*?)<br><\\/p>");
        passwordpattern.add("<b>Passwort\\:<\\/b> (.*?)<\\/p>");
        passwordpattern.add("<strong>Passwort\\:<\\/strong> (.*?) \\|");
        passwordpattern.add("<strong>Passwort\\: <\\/strong>(.*?)<strong>");
        passwordpattern.add("<strong>Passwort<\\/strong>\\: (.*?) <strong>");
        passwordpattern.add("<strong>Passwort\\: <\\/strong>(.*?)<\\/p>");
        passwordpattern.add("<strong>Passwort\\:<\\/strong> (.*?)<\\/p>");
        passwordpattern.add("<strong>Passwort\\:<\\/strong> (.*?) <\\/p>");
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            URL url = new URL(parameter);
            RequestInfo reqinfo = HTTP.getRequest(url);

            /* Defaultpasswörter der Seite setzen */
            Vector<String> link_passwds = new Vector<String>();
            for (int j = 0; j < defaultpasswords.size(); j++) {
                if (url.getHost().toLowerCase().contains(defaultpasswords.get(j)[0])) {
                    for (int jj = 1; jj < defaultpasswords.get(j).length; jj++) {
                        link_passwds.add(defaultpasswords.get(j)[jj]);
                    }
                    break;
                }
            }

            /* Passwort suchen */
            String[] password = null;
            for (int i = 0; i < passwordpattern.size(); i++) {
                password = new Regex(reqinfo, Pattern.compile(passwordpattern.get(i), Pattern.CASE_INSENSITIVE)).getMatches(1);
                if (password.length != 0) {
                    for (String element : password) {
                        link_passwds.add(Encoding.htmlDecode(element));
                    }
                    break;
                }
            }

            /* Alle Parts suchen */
            String[] links = new Regex(reqinfo, Pattern.compile("(<a(.*?)</a>)", Pattern.CASE_INSENSITIVE)).getMatches(2);
            for (int i = 0; i < links.length; i++) {
                if (!new Regex(links[i], patternSupported).matches()) {
                    Vector<DownloadLink> LinkList = new DistributeData(links[i]).findLinks();
                    for (int ii = 0; ii < LinkList.size(); ii++) {
                        DownloadLink link = createDownloadlink(Encoding.htmlDecode(LinkList.get(ii).getDownloadURL()));
                        link.setSourcePluginPasswords(link_passwds);
                        decryptedLinks.add(link);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return decryptedLinks;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginName() {
        return host;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getFirstMatch();
        return ret == null ? "0.0" : ret;
    }
}
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
import jd.controlling.DistributeData;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class Wordpress extends PluginForDecrypt {

    private ArrayList<String[]> defaultPasswords = new ArrayList<String[]>();

    public Wordpress(PluginWrapper wrapper) {
        super(wrapper);

        /* Die defaultpasswörter der einzelnen seiten */
        /* Host, defaultpw1, defaultpw2, usw */
        defaultPasswords.add(new String[] { "doku.cc", "doku.cc", "doku.dl.am" });
        defaultPasswords.add(new String[] { "hd-area.org", "hd-area.org" });
        defaultPasswords.add(new String[] { "movie-blog.org", "movie-blog.org", "movie-blog.dl.am" });
        defaultPasswords.add(new String[] { "xxx-blog.org", "xxx-blog.org", "xxx-blog.dl.am" });
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
        String password = br.getRegex(Pattern.compile("<.*?>Passwort[<|:].*?[>|:](.*?)[\\||<]", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (password != null) link_passwds.add(password.trim());

        /* Alle Parts suchen */
        String[] links = br.getRegex(Pattern.compile("href=.*?(http://[^\"']+)", Pattern.CASE_INSENSITIVE)).getColumn(0);
        progress.setRange(links.length);
        for (String link : links) {
            if (!new Regex(link, this.getSupportedLinks()).matches() && DistributeData.hasPluginFor(link)) {
                DownloadLink dLink = createDownloadlink(link);
                dLink.setSourcePluginPasswords(link_passwds);
                decryptedLinks.add(dLink);
                progress.increase(1);
            }
        }

        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}
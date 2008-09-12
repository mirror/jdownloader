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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class CryptGetMoviesOrg extends PluginForDecrypt {

    private static Vector<String> passwords = new Vector<String>();

    public CryptGetMoviesOrg(PluginWrapper wrapper) {
        super(wrapper);
        passwords.add("www.get-movies.6x.to");
        passwords.add("get-movies.6x.to");
        passwords.add("get-movies.org");
        passwords.add("www.get-movies.org");
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        String link = new Regex(br.getPage(parameter), Pattern.compile("frameborder=\"no\" width=\"100%\" src=\"(.*?)\"></iframe>", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (link == null) return null;
        DownloadLink dl_link = createDownloadlink(link.trim());
        dl_link.addSourcePluginPasswords(passwords);
        decryptedLinks.add(dl_link);

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
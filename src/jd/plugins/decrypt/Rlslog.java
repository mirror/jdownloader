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

import jd.PluginWrapper;
import jd.parser.HTMLParser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class Rlslog extends PluginForDecrypt {

    public Rlslog(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        String followcomments = "";
        if (parameter.contains("/comment-page")) {
            followcomments = parameter.substring(0, parameter.indexOf("/comment-page"));
        }
        if (!parameter.contains("#comments")) {
            parameter += "#comments";
        }
        followcomments = parameter.substring(0, parameter.indexOf("/#comments"));

        String page = br.getPage(parameter);
        String[] links = HTMLParser.getHttpLinks(page, null);
        Vector<String> pass = HTMLParser.findPasswords(page);
        String[] links2;
        Vector<String> pass2;
        for (String element : links) {
            if (element.contains(followcomments)) {
                /* weitere comment pages abrufen */
                page = br.getPage(element);
                links2 = HTMLParser.getHttpLinks(page, null);
                pass2 = HTMLParser.findPasswords(page);
                for (String element2 : links2) {
                    DownloadLink dLink = createDownloadlink(element2);
                    dLink.addSourcePluginPasswords(pass2);
                    decryptedLinks.add(dLink);
                }
            } else {
                DownloadLink dLink = createDownloadlink(element);
                dLink.addSourcePluginPasswords(pass);
                decryptedLinks.add(dLink);
            }
        }

        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }
}
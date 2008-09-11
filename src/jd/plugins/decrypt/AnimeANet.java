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
import java.util.regex.Pattern;

import jd.PluginPattern;
import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class AnimeANet extends PluginForDecrypt {
    final static String host = "animea.net";

    public AnimeANet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        parameter = parameter.replaceAll(" ", "+");

        if (PluginPattern.decrypterPattern_AnimeANet_Series.matcher(parameter).matches()) {
            String[] links = new Regex(br.getPage(parameter), Pattern.compile("<a href=\"/download/(.*?)\\.html\"", Pattern.CASE_INSENSITIVE)).getColumn(0);
            progress.setRange(links.length);
            for (String element : links) {
                decryptedLinks.add(createDownloadlink("http://www.animea.net/download/" + element + ".html"));
                progress.increase(1);
            }
        } else {
            String[] links = new Regex(br.getPage(parameter), Pattern.compile("/><a href=\"(.*?)\" rel=\"nofollow\"", Pattern.CASE_INSENSITIVE)).getColumn(0);
            progress.setRange(links.length);
            for (String element : links) {
                decryptedLinks.add(createDownloadlink(element));
                progress.increase(1);
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
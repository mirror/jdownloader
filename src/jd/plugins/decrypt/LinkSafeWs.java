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

import jd.http.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class LinkSafeWs extends PluginForDecrypt {

    private static final String host = "linksafe.ws";
    private static final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?linksafe\\.ws/files/[a-zA-Z0-9]{4}-[\\d]{5}-[\\d]", Pattern.CASE_INSENSITIVE);

    public LinkSafeWs() {
        super();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

        String[][] files = new Regex(br.getPage(parameter), Pattern.compile("<input type='hidden' name='id' value='(.*?)' />(.*?)<input type='hidden' name='f' value='(.*?)' />", Pattern.DOTALL)).getMatches();
        progress.setRange(files.length);
        for (String[] elements : files) {
            decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(new Regex(br.postPage("http://www.linksafe.ws/go/", "id=" + elements[0] + "&f=" + elements[2] + "&Download.x=5&Download.y=10&Download=Download"), Pattern.compile("src=\"(.*?)\">", Pattern.CASE_INSENSITIVE)).getFirstMatch())));
            progress.increase(1);
        }

        return decryptedLinks;
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

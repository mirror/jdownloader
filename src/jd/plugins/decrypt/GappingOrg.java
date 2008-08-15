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

import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class GappingOrg extends PluginForDecrypt {
    final static String host = "gapping.org";
    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?gapping\\.org/(index\\.php\\?folderid=\\d+|file\\.php\\?id=.+|f/\\d+\\.html)", Pattern.CASE_INSENSITIVE);

    public GappingOrg() {
        super();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

        if (parameter.indexOf("index.php") != -1) {
            String links[] = new Regex(br.getPage(parameter), Pattern.compile("href=\"http://gapping\\.org/file\\.php\\?id=(.*?)\" >", Pattern.CASE_INSENSITIVE)).getMatches(0);
            progress.setRange(links.length);
            for (String element : links) {
                decryptedLinks.add(createDownloadlink(new Regex(br.getPage("http://gapping.org/decry.php?fileid=" + element), Pattern.compile("src=\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getFirstMatch().trim()));
                progress.increase(1);
            }
        } else if (parameter.indexOf("file.php") != -1) {
            parameter = parameter.replace("file.php?id=", "decry.php?fileid=");
            decryptedLinks.add(createDownloadlink(new Regex(br.getPage(parameter), Pattern.compile("src=\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getFirstMatch().trim()));
        } else {
            String[] links = new Regex(br.getPage(parameter), Pattern.compile("http://gapping\\.org/d/(.*?)\\.html", Pattern.CASE_INSENSITIVE)).getMatches(0);
            progress.setRange(links.length);
            for (String element : links) {
                decryptedLinks.add(createDownloadlink(new Regex(br.getPage(element), Pattern.compile("src=\"(.*?)\"", Pattern.DOTALL)).getFirstMatch().trim()));
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
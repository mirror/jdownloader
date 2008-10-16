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

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class GappingOrg extends PluginForDecrypt {

    private Pattern patternSupported_folder1 = Pattern.compile("http://[\\w\\.]*?gapping\\.org/index\\.php\\?folderid=\\d+", Pattern.CASE_INSENSITIVE);
    private Pattern patternSupported_file = Pattern.compile("http://[\\w\\.]*?gapping\\.org/file\\.php\\?id=.+", Pattern.CASE_INSENSITIVE);
    private Pattern patternSupported_file2 = Pattern.compile("http://[\\w\\.]*?gapping\\.org/d/.*\\.html", Pattern.CASE_INSENSITIVE);
    private Pattern patternSupported_folder2 = Pattern.compile("http://[\\w\\.]*?gapping\\.org/f/\\d+\\.html", Pattern.CASE_INSENSITIVE);
    private Pattern patternSupported_container = Pattern.compile("http://[\\w\\.]*?gapping\\.org/g.*?\\.html", Pattern.CASE_INSENSITIVE);

    public GappingOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        if (new Regex(parameter, patternSupported_folder1).matches()) {
            String links[] = new Regex(br.getPage(parameter), Pattern.compile("href=\"http://gapping\\.org/file\\.php\\?id=(.*?)\" >", Pattern.CASE_INSENSITIVE)).getColumn(0);
            progress.setRange(links.length);
            for (String element : links) {
                decryptedLinks.add(createDownloadlink(new Regex(br.getPage("http://gapping.org/decry.php?fileid=" + element), Pattern.compile("src=\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0).trim()));
                progress.increase(1);
            }
        } else if (new Regex(parameter, patternSupported_file).matches()) {
            parameter = parameter.replace("file.php?id=", "decry.php?fileid=");
            decryptedLinks.add(createDownloadlink(new Regex(br.getPage(parameter), Pattern.compile("src=\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0).trim()));
        } else if (new Regex(parameter, patternSupported_container).matches()) {
            logger.info("bla");
        } else if (new Regex(parameter, patternSupported_folder2).matches()) {
            String[] links = new Regex(br.getPage(parameter), Pattern.compile("<a target=\"_blank\" onclick=\"image\\d+\\.src.*? href=\"(.*?)\".*?>.*?</a>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getColumn(0);
            progress.setRange(links.length);

            Pattern patternFollow = Pattern.compile("url=(.*)");
            for (String element : links) {
                if (new Regex(element, patternFollow).matches()) {
                    String[] newLink = new Regex(element, patternFollow).getColumn(-1);
                    decryptedLinks.add(createDownloadlink(newLink[0].trim()));
                    progress.increase(1);
                } else {
                    decryptedLinks.add(createDownloadlink(element.trim()));
                    progress.increase(1);
                }
            }
        } else if (new Regex(parameter, patternSupported_file2).matches()) {
            Pattern patternIframe = Pattern.compile("<iframe.*src=\"(.*?)\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            if (new Regex(br.getPage(parameter), patternIframe).matches()) {
                String newLink = new Regex(br, patternIframe).getMatch(0);
                decryptedLinks.add(createDownloadlink(newLink.trim()));
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
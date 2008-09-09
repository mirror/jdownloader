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
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class GappingOrg extends PluginForDecrypt {
    final static String host = "gapping.org";
    private Pattern patternSupported_folder1 = Pattern.compile("http://[\\w\\.]*?gapping\\.org/index\\.php\\?folderid=\\d+", Pattern.CASE_INSENSITIVE);
    private Pattern patternSupported_file = Pattern.compile("http://[\\w\\.]*?gapping\\.org/file\\.php\\?id=.+", Pattern.CASE_INSENSITIVE);
    private Pattern patternSupported_folder2 = Pattern.compile("http://[\\w\\.]*?gapping\\.org/f/\\d+\\.html", Pattern.CASE_INSENSITIVE);
    private Pattern patternSupported_container = Pattern.compile("http://[\\w\\.]*?gapping\\.org/g.*?\\.html", Pattern.CASE_INSENSITIVE);
    private Pattern patternSupported = Pattern.compile(patternSupported_folder1.pattern() + "|" + patternSupported_folder2.pattern() + "|" + patternSupported_file.pattern() + "|" + patternSupported_container.pattern(), Pattern.CASE_INSENSITIVE);

    public GappingOrg(String cfgName){
        super(cfgName);
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
            String[] links = new Regex(br.getPage(parameter), Pattern.compile("http://gapping\\.org/d/(.*?)\\.html", Pattern.CASE_INSENSITIVE)).getColumn(-1);
            progress.setRange(links.length);
            for (String element : links) {
                String dl_link = new Regex(br.getPage(element), Pattern.compile("src=\"(.*?)\"", Pattern.DOTALL)).getMatch(0);
                decryptedLinks.add(createDownloadlink(dl_link.trim()));
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
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

}
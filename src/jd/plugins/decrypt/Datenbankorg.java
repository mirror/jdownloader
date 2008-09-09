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

public class Datenbankorg extends PluginForDecrypt {
    final static String host = "daten-bank.org";

    private Pattern patternSupported_Go = Pattern.compile("http://daten-bank\\.org/go/\\d+/", Pattern.CASE_INSENSITIVE);
    private Pattern patternSupported_Info = Pattern.compile("http://daten-bank\\.org/view/.*?/", Pattern.CASE_INSENSITIVE);
    private Pattern patternSupported = Pattern.compile(patternSupported_Go.pattern() + "|" + patternSupported_Info.pattern(), Pattern.CASE_INSENSITIVE);

    public Datenbankorg(String cfgName){
        super(cfgName);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        if (patternSupported_Info.matcher(parameter).matches()) {
            String page = br.getPage(parameter);
            String password = new Regex(page, Pattern.compile("class='Value Password'>(.*?)<", Pattern.CASE_INSENSITIVE)).getMatch(0);
            String links_page = new Regex(page, Pattern.compile("Head'>OneClick-Hoster<(.+)(class='Head'>Sample)?", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
            String links[] = new Regex(links_page, Pattern.compile("href='/go/(\\d+)/'", Pattern.CASE_INSENSITIVE)).getColumn(0);
            if (links == null) return null;
            for (String link : links) {
                DownloadLink dl_link = createDownloadlink("http://daten-bank.org/go/" + link + "/");
                dl_link.addSourcePluginPassword(password);
                decryptedLinks.add(dl_link);
            }
        } else {
            br.getPage(parameter);
            decryptedLinks.add(createDownloadlink(br.getRedirectLocation()));
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

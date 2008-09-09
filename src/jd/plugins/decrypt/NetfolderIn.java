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
import jd.utils.JDUtilities;

public class NetfolderIn extends PluginForDecrypt {
    static private String host = "netfolder.in";
    static private final Pattern patternSupported_1 = Pattern.compile("http://[\\w\\.]*?netfolder\\.in/folder\\.php\\?folder_id\\=[a-zA-Z0-9]{7}", Pattern.CASE_INSENSITIVE);
    static private final Pattern patternSupported_2 = Pattern.compile("http://[\\w\\.]*?netfolder\\.in/[a-zA-Z0-9]{7}/.*?", Pattern.CASE_INSENSITIVE);

    static private final Pattern patternSupported = Pattern.compile(patternSupported_1.pattern() + "|" + patternSupported_2.pattern(), Pattern.CASE_INSENSITIVE);

    public NetfolderIn(String cfgName){
        super(cfgName);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        String page = br.getPage(parameter);
        if (parameter.matches(patternSupported_2.pattern())) {
            decryptedLinks.add(createDownloadlink(br.getRedirectLocation()));
        } else if (parameter.matches(patternSupported_1.pattern())) {
            String password = "";
            for (int retrycounter = 1; retrycounter <= 5; ++retrycounter) {
                int check = new Regex(page, Pattern.compile("input type=\"password\" name=\"password\"", Pattern.CASE_INSENSITIVE)).count();
                if (check > 0) {
                    password = JDUtilities.getController().getUiInterface().showUserInputDialog("Die Links sind mit einem Passwort gesch\u00fctzt. Bitte geben Sie das Passwort ein:");
                    if (password == null) return null;
                    page = br.postPage(parameter, "password=" + password + "&save=Absenden");
                } else {
                    break;
                }
            }

            String[][] links = new Regex(page, Pattern.compile("href=\"http://netload\\.in/(.*?)\"", Pattern.CASE_INSENSITIVE)).getMatches();
            progress.setRange(links.length);
            for (int i = 0; i < links.length; ++i) {
                decryptedLinks.add(createDownloadlink("http://netload.in/" + links[i][0]));
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
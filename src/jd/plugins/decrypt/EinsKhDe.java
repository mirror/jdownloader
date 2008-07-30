//jDownloader - Downloadmanager
//Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://gnu.org/licenses/>.

package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class EinsKhDe extends PluginForDecrypt {

    static private String host = "1kh.de";

    private String version = "1.0.0.0";
    final static private Pattern patternSupported_File = Pattern.compile("http://[\\w\\.]*?1kh\\.de/[0-9]+", Pattern.CASE_INSENSITIVE);
    final static private Pattern patternSupported_Folder = Pattern.compile("http://[\\w\\.]*?1kh\\.de/f/[0-9/]+", Pattern.CASE_INSENSITIVE);
    final static private Pattern patternSupported = Pattern.compile(patternSupported_Folder.pattern() + "|" + patternSupported_File.pattern(), Pattern.CASE_INSENSITIVE);

    public EinsKhDe() {
        super();
    }

    
    public String getCoder() {
        return "JD-Team";
    }

    
    public String getHost() {
        return host;
    }

 

    
    public String getPluginName() {
        return host;
    }

    
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    
    public String getVersion() {
       String ret=new Regex("$Revision$","\\$Revision: ([\\d]*?) \\$").getFirstMatch();return ret==null?"0.0":ret;
    }

    public ArrayList<DownloadLink> decryptIt(String parameter) {
        String cryptedLink = (String) parameter;
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            progress.setRange(1);
            URL url = new URL(cryptedLink);
            RequestInfo reqinfo = HTTP.getRequest(url);
            if (cryptedLink.matches(patternSupported_File.pattern())) {
                /* eine einzelne Datei */
                String link = JDUtilities.htmlDecode(new Regex(reqinfo.getHtmlCode(), "<iframe name=\"pagetext\" height=\".*?\" frameborder=\"no\" width=\"100%\" src=\"(.*?)\"></iframe>").getFirstMatch().toString());
                if (link != null) {
                    decryptedLinks.add(this.createDownloadlink(link));
                } else
                    return null;
            } else if (cryptedLink.matches(patternSupported_Folder.pattern())) {
                /* ein Folder */
                if (reqinfo.containsHTML("Der Ordner ist Passwortgesch&uuml;tzt.")) {
                    for (int retrycounter = 1; retrycounter <= 5; retrycounter++) {
                        logger.finest("1kh.de - Ordnerpasswort benötigt");
                        String password = JDUtilities.getGUI().showUserInputDialog("1kh.de - Ordnerpasswort?");
                        if (password == null) {
                            /* auf abbruch geklickt */
                            return null;
                        }
                        reqinfo = HTTP.postRequest(url, "Password=" + password + "&submit=weiter");
                        if (!reqinfo.containsHTML("Das eingegebene Passwort ist falsch")) break;
                    }
                }
                String[] links = new Regex(reqinfo.getHtmlCode(), Pattern.compile("<div class=\"Block3\" ><a id=\"DownloadLink_(\\d+)\"", Pattern.CASE_INSENSITIVE)).getMatches(1);
                progress.setRange(links.length);
                for (int i = 0; i < links.length; i++) {
                    decryptedLinks.add(this.createDownloadlink("http://1kh.de/" + links[i]));
                    progress.increase(1);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return decryptedLinks;
    }

    
    public boolean doBotCheck(File file) {
        return false;
    }
}
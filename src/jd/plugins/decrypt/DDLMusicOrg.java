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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;

public class DDLMusicOrg extends PluginForDecrypt {
    final static String host = "ddl-music.org";
    private String version = "0.1.0";
    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?ddl-music\\.org/(music_crypth\\.php\\?.+|index\\.php\\?site=view_download.+)", Pattern.CASE_INSENSITIVE);

    public DDLMusicOrg() {
        super();
    }

    
    public String getCoder() {
        return "jD-Team";
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
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {

            if (parameter.indexOf("music_crypth.php") != -1) {
                parameter = parameter.replace("music_crypth.php", "frame_crypth.php");
                RequestInfo reqinfo = HTTP.getRequest(new URL(parameter));
                decryptedLinks.add(this.createDownloadlink("http://" + SimpleMatches.getBetween(reqinfo.getHtmlCode(), "src=http://", " target=\"_self\">")));
            } else if (parameter.indexOf("site=view_download") != -1) {
                RequestInfo reqinfo = HTTP.getRequest(new URL(parameter));
                // passwort auslesen
                if (reqinfo.getHtmlCode().indexOf("<td class=\"normalbold\"><div align=\"center\">Passwort</div></td>") != -1) {
                    String password = SimpleMatches.getBetween(reqinfo.getHtmlCode(), "<td class=\"normalbold\"><div align=\"center\">Passwort</div></td>\n" + "                      </tr>\n" + "                      <tr>\n" + "                      <td class=\"normal\"><div align=\"center\">", "</div></td>");
                    default_password.add(password);
                }

                ArrayList<ArrayList<String>> ids = SimpleMatches.getAllSimpleMatches(reqinfo.getHtmlCode(), "href=\"/music_crypth.php°\" target=\"_blank\"");
                progress.setRange(ids.size());

                int j = 0;

                for (int i = 0; i < ids.size(); i++) {

                    reqinfo = HTTP.getRequest(new URL("http://ddl-music.org/frame_crypth.php" + ids.get(i).get(0)));
                    logger.info("http://ddl-music.org/frame_crypth.php" + ids.get(i).get(0));
                    decryptedLinks.add(this.createDownloadlink("http://" + SimpleMatches.getBetween(reqinfo.getHtmlCode(), "src=http://", " target=\"_self\">")));
                    progress.increase(1);

                    // nach 3 anfragen schnell hintereinander streikt der
                    // server -> http 403
                    // => ab 3. aufruf jeweils 0.5 sekunden warten
                    if (j >= 2) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    j++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return decryptedLinks;
    }

    
    public boolean doBotCheck(File file) {
        return false;
    }
}
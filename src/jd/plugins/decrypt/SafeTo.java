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
import jd.utils.JDUtilities;

public class SafeTo extends PluginForDecrypt {

    private static final String host = "safe.to";



    private static final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?safe\\.to/get\\.php\\?i=[a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE);

    /*
     * Suchmasken
     */
    private static final String FRAME_URL = "<frame src=\"°\" scrolling=\"auto\" name=\"FrameRedirect\" noresize>";
    private static final String FILE_ID = "<input type=\"submit\" name=\"dl\" value=\"Download\" onClick=\"popup_dl(°)\">";
    private static final String PASSWORD = "<input type=\"password\" name=\"pw\" class=\"txt\">";

    public SafeTo() {
        super();
    }

    
    public String getCoder() {
        return "JD-Team";
    }

    
    public String getPluginName() {
        return host;
    }

    
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    
    public String getHost() {
        return host;
    }

    
    public String getVersion() {
       return new Regex("$Revision$","\\$Revision: ([\\d]*?)\\$").getFirstMatch();
    }

    
    
        
    

    
    public boolean doBotCheck(File file) {
        return false;
    }

    
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        ArrayList<ArrayList<String>> fileIDs = null;
        try {
            String strURL = parameter;
            URL url = new URL(strURL);
            RequestInfo reqinfo = HTTP.getRequest(url); // Seite aufrufen

            String frameURL = SimpleMatches.getSimpleMatch(reqinfo.getHtmlCode(), FRAME_URL, 0);

            if (frameURL == null) {
                logger.severe("Cannot find frame-URL!");
                return null;
            }

            // frame aufrufen
            reqinfo = HTTP.getRequest(new URL(frameURL));

            boolean password = false;
            if (reqinfo.getHtmlCode().contains(PASSWORD)) password = true;

            // Solange, bis Passwort richtig eingegeben wurde (also Links
            // gefunden)
            while (true) {
                // Im HTML-Code nach "file-ids"/"Form-Inputs" suchen
                fileIDs = SimpleMatches.getAllSimpleMatches(reqinfo.getHtmlCode(), FILE_ID);

                // Passwort-Abfrage?
                if (fileIDs.isEmpty() && password) {
                    // Passwort abfragen
                    String pwd = JDUtilities.getController().getUiInterface().showUserInputDialog("Die Links sind mit einem Passwort gesch\u00fctzt. Bitte geben Sie das Passwort ein:");
                    // Passwort senden
                    reqinfo = HTTP.postRequest(new URL(frameURL), "pw=" + pwd + "&chk=Check");
                } else {
                    break;
                }
            }
            progress.setRange(fileIDs.size());
            for (int i = 0; i < fileIDs.size(); i++) {
                reqinfo = HTTP.getRequest(new URL("http://85.17.45.96/~safe/futsch.php?i=" + fileIDs.get(i).get(0)));
                String newLink = reqinfo.getLocation();
                decryptedLinks.add(this.createDownloadlink(newLink));
                progress.increase(1);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return decryptedLinks;
    }

}

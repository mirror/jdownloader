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

import jd.http.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class CodedTo extends PluginForDecrypt {
    /*
     * Suchmasken
     */
    private static final String FILES = "<a target=\"_blank\" href=\"down.php?id=(.*?)\">";

    private static final String host = "coded.to";

    private static final String LINK = "<iframe src=\"(.*?)\" height=\"100%\" width=\"100%\"";
    private static final String PASSWORD = "Bitte gib das erforderliche Passwort ein";
    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?coded\\.to/.*?\\?jump\\=[a-zA-Z0-9]{32}", Pattern.CASE_INSENSITIVE);

    public CodedTo() {
        super();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            String strURL = parameter;
            URL url = new URL(strURL);
            RequestInfo reqinfo = HTTP.getRequest(url);
            boolean do_continue = false;
            String files[][] = null;
            // solange bis Passwort eingegeben und Links gefunden wurden
            for (int retry = 1; retry < 5; retry++) {
                // Im HTML-Code nach "files"/"Forms" suchen
                files = new Regex(reqinfo.getHtmlCode(), Pattern.compile(FILES, Pattern.CASE_INSENSITIVE)).getMatches();

                // Passwort-Abfrage vorhanden und keine Links gefunden
                if (reqinfo.getHtmlCode().contains(PASSWORD) && files.length == 0) {
                    String pwd = JDUtilities.getController().getUiInterface().showUserInputDialog("Die Links sind mit einem Passwort gesch\u00fctzt. Bitte geben Sie das Passwort ein:");
                    reqinfo = HTTP.postRequest(url, null, strURL, null, "pwd=" + pwd + "&pwsub=OK", true);
                } else {
                    do_continue = true;
                    break; // keine Passwort-Abfrage
                }
            }
            if (do_continue == true) {
                // Anzahl Links
                progress.setRange(files.length);
                for (String[] element : files) {
                    reqinfo = HTTP.getRequest(new URL("http://www.coded.to/down.php?id=" + element[0]));
                    String html = Encoding.htmlDecode(reqinfo.getHtmlCode());
                    String newLink = new Regex(html, Pattern.compile(LINK, Pattern.CASE_INSENSITIVE)).getFirstMatch();
                    decryptedLinks.add(createDownloadlink(newLink));
                    progress.increase(1);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return decryptedLinks;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
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

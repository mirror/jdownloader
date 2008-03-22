//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://wnu.org/licenses/>.


package jd.plugins.decrypt;  import jd.plugins.DownloadLink;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class CodedTo extends PluginForDecrypt {
    private static final String  CODER          = "Bo0nZ";

    private static final String  HOST           = "coded.to";

    private static final String  PLUGIN_NAME    = HOST;

    private static final String  PLUGIN_VERSION = "1.0.0.0";

    private static final String  PLUGIN_ID      = PLUGIN_NAME + "-" + PLUGIN_VERSION;
    //http://www.coded.to/?jump=6f4920ea25403ec77bee9efce43ea25e
    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://.*?coded\\.to/.*?\\?jump\\=[a-zA-Z0-9]{32}", Pattern.CASE_INSENSITIVE);

    /*
     * Suchmasken
     */
    private static final String  FILES          = "<a target=\"_blank\" href=\"down.php?id=°\">";

    private static final String  LINK           = "<iframe src=\"°\" height=\"100%\" width=\"100%\"";

    private static final String  PASSWORD       = "Bitte gib das erforderliche Passwort ein";

    public CodedTo() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
    }

    /*
     * Funktionen
     */
    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public String getPluginID() {
        return PLUGIN_ID;
    }

    @Override
    public String getPluginName() {
        return HOST;
    }

    @Override
    public Pattern getSupportedLinks() {
        return PAT_SUPPORTED;
    }

    @Override
    public String getVersion() {
        return PLUGIN_VERSION;
    }

    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
        if (step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
            Vector<Vector<String>> files;
            try {
                String strURL = parameter;
                URL url = new URL(strURL);
                RequestInfo reqinfo = getRequest(url); // Seite aufrufen

                // solange bis Passwort eingegeben und Links gefunden wurden
                while (true) {
                    // Im HTML-Code nach "files"/"Forms" suchen
                    files = getAllSimpleMatches(reqinfo.getHtmlCode(), FILES);

                    // Passwort-Abfrage vorhanden und keine Links gefunden
                    if (reqinfo.getHtmlCode().contains(PASSWORD) && files.isEmpty()) {
                        String pwd = JDUtilities.getController().getUiInterface().showUserInputDialog("Die Links sind mit einem Passwort gesch\u00fctzt. Bitte geben Sie das Passwort ein:");
                        reqinfo = postRequest(url, null, strURL, null, "pwd=" + pwd + "&pwsub=OK", true);
                    }
                    else {
                        break; // keine Passwort-Abfrage
                    }
                }

                // Anzahl Links
                progress.setRange(files.size());

                for (int i = 0; i < files.size(); i++) {
                    reqinfo = getRequest(new URL("http://www.coded.to/down.php?id=" + files.get(i).get(0)));

                    String html = JDUtilities.htmlDecode(reqinfo.getHtmlCode());
                    String newLink = getSimpleMatch(html, LINK, 0);

                    decryptedLinks.add(this.createDownloadlink(newLink));
                    progress.increase(1);
                }

                // Decrypt abschliessen

                step.setParameter(decryptedLinks);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

}

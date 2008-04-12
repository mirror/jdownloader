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
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.


package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;
import jd.plugins.DownloadLink;

public class SafeTo extends PluginForDecrypt {
    private static final String  CODER          = "Bo0nZ";
    private static final String  HOST           = "safe.to";
    private static final String  PLUGIN_NAME    = HOST;
    private static final String  PLUGIN_VERSION = "1.0.0.0";
    private static final String  PLUGIN_ID      = PLUGIN_NAME + "-" + PLUGIN_VERSION;
    private static final Pattern PAT_SUPPORTED  = getSupportPattern("http://[*]safe\\.to/get\\.php\\?i=[a-zA-Z0-9]+");

    /*
     * Suchmasken
     */
    private static final String  FRAME_URL      = "<frame src=\"°\" scrolling=\"auto\" name=\"FrameRedirect\" noresize>";
    private static final String  FILE_ID        = "<input type=\"submit\" name=\"dl\" value=\"Download\" onClick=\"popup_dl(°)\">";
    private static final String  PASSWORD       = "<input type=\"password\" name=\"pw\" class=\"txt\">";

    public SafeTo() {
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
    public String getVersion() {
        return PLUGIN_VERSION;
    }

    @Override
    public Pattern getSupportedLinks() {
        return PAT_SUPPORTED;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
        if (step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
            ArrayList<ArrayList<String>> fileIDs = null;
            try {
                String strURL = parameter;
                URL url = new URL(strURL);
                RequestInfo reqinfo = getRequest(url); // Seite aufrufen

                String frameURL = getSimpleMatch(reqinfo.getHtmlCode(), FRAME_URL, 0);

                if (frameURL == null) {
                    logger.severe("Cannot find frame-URL!");
                    return null;
                }

                // frame aufrufen
                reqinfo = getRequest(new URL(frameURL));

                boolean password = false;
                if (reqinfo.getHtmlCode().contains(PASSWORD)) password = true;

                // Solange, bis Passwort richtig eingegeben wurde (also Links
                // gefunden)
                while (true) {
                    // Im HTML-Code nach "file-ids"/"Form-Inputs" suchen
                    fileIDs = getAllSimpleMatches(reqinfo.getHtmlCode(), FILE_ID);

                    // Passwort-Abfrage?
                    if (fileIDs.isEmpty() && password) {
                        // Passwort abfragen
                        String pwd = JDUtilities.getController().getUiInterface().showUserInputDialog("Die Links sind mit einem Passwort gesch\u00fctzt. Bitte geben Sie das Passwort ein:");
                        // Passwort senden
                        reqinfo = postRequest(new URL(frameURL), "pw=" + pwd + "&chk=Check");
                    }
                    else {
                        break;
                    }
                }
               progress.setRange(fileIDs.size());

                for (int i = 0; i < fileIDs.size(); i++) {
                    reqinfo = getRequest(new URL("http://85.17.45.96/~safe/futsch.php?i=" + fileIDs.get(i).get(0)));

                    String newLink = reqinfo.getLocation();

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

}

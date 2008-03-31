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


package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class LinkSafeWs extends PluginForDecrypt {
    private static final String  CODER          = "Bo0nZ";
    private static final String  HOST           = "linksafe.ws";
    private static final String  PLUGIN_NAME    = HOST;
    private static final String  PLUGIN_VERSION = "1.0.0.0";
    private static final String  PLUGIN_ID      = PLUGIN_NAME + "-" + PLUGIN_VERSION;
    private static final Pattern PAT_SUPPORTED  = Pattern.compile("http://.*?linksafe\\.ws/files/[a-zA-Z0-9]{4}-[\\d]{5}-[\\d]", Pattern.CASE_INSENSITIVE);

    /*
     * Suchmasken
     */
    private static final String  FILES          = "<input type='hidden' name='id' value='°' />°<input type='hidden' name='f' value='°' />";
    private static final String  LINK           = "<iframe frameborder=\"0\" height=\"100%\" width=\"100%\" src=\"°\">";

    public LinkSafeWs() {
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
            try {
                String strURL = parameter;
                URL url = new URL(strURL);
                RequestInfo reqinfo = getRequest(url); // Seite aufrufen

                // Im HTML-Code nach "files"/"Forms" suchen
                ArrayList<ArrayList<String>> files = getAllSimpleMatches(reqinfo.getHtmlCode(), FILES);
                progress.setRange(files.size());

                for (int i = 0; i < files.size(); i++) {
                    reqinfo = postRequest(new URL("http://www.linksafe.ws/go/"), reqinfo.getCookie(), strURL, null, "id=" + files.get(i).get(0) + "&f=" + files.get(i).get(2) + "&Download.x=5&Download.y=10&Download=Download", true);

                    String newLink = getSimpleMatch(reqinfo.getHtmlCode(), LINK, 0);
                    
                    String pattern = "\\&\\#[0-9]{1,3}";
                    for (Matcher r = Pattern.compile(pattern, Pattern.DOTALL).matcher(newLink); r.find();) {                     
                            char c = (char) JDUtilities.filterInt(r.group(0));
                            newLink = newLink.replaceFirst("\\&\\#[0-9]{1,3}", c + "");                   
                    }

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

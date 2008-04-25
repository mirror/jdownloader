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


package jd.plugins.decrypt;  import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class Rapidsafenet extends PluginForDecrypt {

    static private final String host             = "rapidsafe.net";

    private String              version          = "1.0.0.0";
    //http://www.rapidsafe.net/rsAzNhVDZxYTM/ShapeInstall.zip.html
    private Pattern             patternSupported = getSupportPattern("http://[*]rapidsafe\\.net/r.-?[a-zA-Z0-9]{11}/.*");

    public Rapidsafenet() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));

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
    public String getPluginID() {
        return "Rapidsafe.net-1.0.0.";
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
        return version;
    }

    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
        if (step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
            progress.setRange(1);
            try {
                URL url = new URL(parameter);
                RequestInfo reqinfo = getRequest(url);

                // Links auslesen und konvertieren
                progress.increase(1);
                decryptedLinks.add(this.createDownloadlink((JDUtilities.htmlDecode(getBetween(reqinfo.getHtmlCode(), "&nbsp;<FORM ACTION=\"", "\" METHOD=\"post\" ID=\"postit\"")))));
            }
            catch (IOException e) {
                e.printStackTrace();
            }

            // Decrypt abschliessen

            step.setParameter(decryptedLinks);
        }

        return null;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
}
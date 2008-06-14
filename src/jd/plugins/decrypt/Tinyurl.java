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

import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;

public class Tinyurl extends PluginForDecrypt {

    static private String host = "tinyurl.com";

    private String version = "2.0.0.0";
    // tinyurl.com/preview.php?num=37nt3d
    private Pattern patternSupported = getSupportPattern("http://[*]tinyurl\\.com/(preview\\.php\\?num\\=[a-zA-Z0-9]{6}|[a-zA-Z0-9]{6})");

    private Pattern patternLink = Pattern.compile("http://tinyurl\\.com/.*");

    public Tinyurl() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
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
        return "Tinyurl-1.0.0.";
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
            try {
                progress.setRange(1);
                if (!parameter.matches("http://.*?tinyurl\\.com/preview\\.php\\?num\\=[a-zA-Z0-9]{6}")) {
                    parameter=parameter.replaceFirst("tinyurl\\.com/", "tinyurl.com/preview.php?num=");
                }

                URL url = new URL(parameter);
                RequestInfo reqinfo = HTTP.getRequest(url);

                // Besonderen Link herausfinden
                if (SimpleMatches.countOccurences(parameter, patternLink) > 0) {
                    String[] result = parameter.split("/");
                    reqinfo = HTTP.getRequest(new URL("http://tinyurl.com/" + result[result.length - 1]));
                }

                // Link der Liste hinzufügen
                progress.increase(1);
                decryptedLinks.add(this.createDownloadlink(SimpleMatches.getBetween(reqinfo.getHtmlCode(), "id=\"redirecturl\" href=\"", "\">Proceed to")));

                // Decrypt abschliessen

                step.setParameter(decryptedLinks);
            } catch (IOException e) {
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
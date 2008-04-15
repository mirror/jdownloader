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
import java.util.Vector;
import java.util.regex.Pattern;

import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;

public class DLCXliceNet extends PluginForDecrypt {

    final static String host = "DLC.Xlice.net";

    private String version = "0.0.1";

    private Pattern patternSupported = getSupportPattern("http://dlc.xlice.net/[+]/[+]/[+]/[+]/[+]");

    public DLCXliceNet() {
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
        return host+" " + version;
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
            parameter=parameter.replaceFirst("http://.*?adref.in/\\?", "");
            if(!parameter.matches("^http://"))
                parameter="http://"+parameter;
            decryptedLinks.add(this.createDownloadlink(parameter));
            progress.increase(1);

            step.setParameter(decryptedLinks);
        }
        return null;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
}
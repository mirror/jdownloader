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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.config.Configuration;
import jd.controlling.JDController;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

// http://crypt-it.com/s/BXYMBR
// http://crypt-it.com/s/B44Z4A

public class DLCXliceNet extends PluginForDecrypt {

    static private final String HOST = "Xlice.net.redirect";

    private String VERSION = "0.0.1";

    private String CODER = "jD-Team";

    // http://xlice.net/getdlc/a50d323947054cc204362a47ddd5bc49/
    static private final Pattern patternSupported = getSupportPattern("http://xlice.net/getdlc/[+]");

    public DLCXliceNet() {

        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();

    }

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
        return HOST + "-" + VERSION;
    }

    @Override
    public String getPluginName() {
        return HOST;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
        // surpress jd warning
        Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
        step.setParameter(decryptedLinks);
        if (step.getStep() == PluginStep.STEP_DECRYPT) {

            File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".dlc");
            if (JDUtilities.download(container, parameter)) {
                
                JDUtilities.getController().loadContainerFile(container);

            }
        }

        return step;

    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

}

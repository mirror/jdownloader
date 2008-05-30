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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.gui.skins.simple.Link.JLinkButton;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class DLCXliceNetRedirect extends PluginForDecrypt {

    final static String host = "DLC.Xlice.net";

    private String version = "0.0.1";

    private Pattern patternSupported = getSupportPattern("http://dlc.xlice.net/[+]/[+]/[+]/[+]/[+]");

    public static ArrayList<String> openedLinks = new ArrayList<String>();;
    
    public DLCXliceNetRedirect() {
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
        return host + " " + version;
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
           // http://xlice.net/folder/f786e5cdc2ca2172e2648c1c72104793/
            progress.setRange(1);
            try {
               
                RequestInfo ri = getRequestWithoutHtmlCode(new URL(parameter), null, null, false);
                //logger.info(ri.getHeaders() + "");
                if (ri.getLocation() != null&&!openedLinks.contains(ri.getLocation())) {
                    // ri.getLocation()

                    if (ri.getLocation().contains("folder")) {
                        if (getProperties().getBooleanProperty("REFRESH_DLC_BROWSER", true) ) {

                            if (JDUtilities.getGUI().showConfirmDialog(JDLocale.L("plugins.decrypt.dlcxlice.net", "DLC expired. Open Xlicefolder?"))) {
                                
                                JLinkButton.openURL(ri.getLocation());
                                String id = ri.getLocation().substring(ri.getLocation().indexOf("folder/") + 7, ri.getLocation().length() - 1);
                                JDUtilities.download(JDUtilities.getResourceFile("container/" + JDUtilities.getMD5(ri.getLocation()) + ".dlc"), "http://xlice.net/getdlc/" + id + "/");
                                JDUtilities.getController().loadContainerFile(JDUtilities.getResourceFile("container/" + JDUtilities.getMD5(ri.getLocation()) + ".dlc"));
                            }
                        }
                        openedLinks.add(ri.getLocation());
                    } else {
                        decryptedLinks.add(this.createDownloadlink(ri.getLocation()));
                        
                       
                    }
                } else {

                }

            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            progress.increase(1);
       
            step.setParameter(decryptedLinks);
        }
        return null;
    }

    private void setConfigElements() {
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "REFRESH_DLC_BROWSER", "Veraltete DLCs im Browser öffnen"));
        cfg.setDefaultValue(true);

    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
}
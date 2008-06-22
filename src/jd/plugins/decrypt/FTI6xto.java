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
import java.util.Vector;
import java.util.regex.Pattern;

import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

// http://crypt-it.com/s/BXYMBR
// http://crypt-it.com/s/B44Z4A

public class FTI6xto extends PluginForDecrypt {

    static private final String HOST = "FTI.6x.to";

    private String VERSION = "0.0.1";

    private String CODER = "jD-Team";
    //http://fucktheindustry.ru/file.php?id=23423765
    // http://fucktheindustry.ru/store/file/dlc/forcedl.php?file=One.Piece.S01E001.Hier.Kommt.Ruffy.Der.Kuenftige.Koenig.Der.Piraten.German.DVDRiP.UNCUT.Xvid-iND.dlc
    static private final Pattern patternSupported = getSupportPattern("http://fucktheindustry.ru/store/file/dlc/forcedl.php\\?file\\=[+]|http://fucktheindustry.ru/file.php\\?id\\=[+]");

    public FTI6xto() {

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
            
            if(parameter.trim().toLowerCase().contains("file.php")){
                try {
                    RequestInfo ri = HTTP.getRequest(new URL(parameter));
                    
                   parameter= "http://fucktheindustry.ru/store/file/dlc/forcedl.php?file="+SimpleMatches.getSimpleMatch(ri.getHtmlCode(), "<a href=\"http://fucktheindustry.ru/store/file/dlc/forcedl.php?file=°.dlc",0)+".dlc";
                } catch (MalformedURLException e) {
                    return step;
                } catch (IOException e) {
                    return step;
                }
                
            }
            if(parameter.trim().toLowerCase().endsWith("ccf")){
                File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".ccf");
                if (JDUtilities.download(container, parameter)) {

                    JDUtilities.getController().loadContainerFile(container);

                } 
            }else{
            File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".dlc");
            if (JDUtilities.download(container, parameter)) {

                JDUtilities.getController().loadContainerFile(container);

            }
            }

          
            return step;

        }

        return null;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

}

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
import jd.plugins.DownloadLink;
import jd.plugins.HTTPConnection;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.utils.JDUtilities;

/*Die Api von Linksavein verursacht *falsche* DLC's*/
/*Am ende einer DLC wird "No database selected" angehängt*/

public class LinksaveIn extends PluginForDecrypt {

    static private final String HOST = "Linksave.in";

    private String VERSION = "1.0.0";

    private String CODER = "JD-Team";

    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?linksave\\.in/[a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE);

    public LinksaveIn() {
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
        String cryptedLink = (String) parameter;
        if (step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
            URL url;
            try {
                if (cryptedLink.matches(patternSupported.pattern())) {
                    cryptedLink = cryptedLink + ".dlc";
                    url = new URL(cryptedLink);
                    File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".dlc");
                    HTTPConnection dlc_con = new HTTPConnection(url.openConnection());
                    dlc_con.setRequestProperty("Referer", "jDownloader.ath.cx");
                    JDUtilities.download(container, dlc_con);
                    JDUtilities.getController().loadContainerFile(container);
                }
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            step.setParameter(decryptedLinks);
        }
        return null;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

}

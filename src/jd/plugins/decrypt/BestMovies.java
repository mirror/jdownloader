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
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

// http://crypt-it.com/s/BXYMBR
// http://crypt-it.com/s/B44Z4A

public class BestMovies extends PluginForDecrypt {

    static private final String HOST = "best-movies.us";

    private String VERSION = "0.0.1";

    private String CODER = "jD-Team";
    static private final Pattern patternSupported = getSupportPattern("http://[*]best-movies.us/\\?p\\=[+]|http://crypt.best-movies.us/go.php\\?id\\=[+]");

    public BestMovies() {

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
            try {
                if (parameter.contains("go.php")) {
                    Vector<DownloadLink> links = loadCryptLinks(parameter, null, null);
                    if (links != null) decryptedLinks.addAll(links);
                } else {
                    RequestInfo ri = getRequest(new URL(parameter));
                    String packageName = JDUtilities.htmlDecode(getSimpleMatch(ri, "<h1>°</h1>", 0));
                    String password = JDUtilities.htmlDecode(getSimpleMatch(ri, " <p><strong>Passwort:</strong>°</p>", 0));

                    ArrayList<String> matches = getAllSimpleMatches(ri, "http://crypt.best-movies.us/go.php?id=°\"", 1);
                    for (String match : matches) {
                        Vector<DownloadLink> links = loadCryptLinks("http://crypt.best-movies.us/go.php?id="+match, packageName, password);
                        if (links != null) decryptedLinks.addAll(links);
                    }

                }

                return step;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }

        }

        return null;
    }

    private Vector<DownloadLink> loadCryptLinks(String parameter, String name, String pass) throws Exception {
        Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
        
        FilePackage fp= new FilePackage();
        fp.setName(name);
        fp.setPassword(pass);

        @SuppressWarnings("unused")
        RequestInfo ri = getRequest(new URL(parameter));
        String url = getSimpleMatch(ri, "document.write('°' frameborder=0", 0);
        if (url == null) return null;
        url = getSimpleMatch(url.replaceAll("'\\+'", ""), "<iframe src=\\'°\\", 0);
        // String url2=getSimpleMatch(ri,"<div style=\"display: none;\"><iframe
        // name=\"pagetext\" height=\"100%\" frameborder=\"no\" width=\"100%\"
        // src=\"°\"></iframe>",0);
        if (url.endsWith(".ccf")) {
            File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".ccf");
            JDUtilities.download(container, url);
            JDUtilities.getController().loadContainerFile(container);
        } else if (url.endsWith(".rsdf")) {
            File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".rsdf");
            JDUtilities.download(container, url);
            JDUtilities.getController().loadContainerFile(container);
        } else if (url.endsWith(".dlc")) {
            File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".dlc");
            JDUtilities.download(container, url);
            JDUtilities.getController().loadContainerFile(container);
        } else {
            decryptedLinks.add(this.createDownloadlink(url));
            if(name!=null && pass!=null)decryptedLinks.lastElement().setFilePackage(fp);
                
        }
        return decryptedLinks;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

}

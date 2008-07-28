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
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.parser.XPath;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;

public class AnimeANet extends PluginForDecrypt {

    final static String host = "animea.net";

    private String version = "1.0.0.0";

    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?animea\\.net/download/[\\d]+/.*", Pattern.CASE_INSENSITIVE);

    public AnimeANet() {
        super();
        //steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        //currentStep = steps.firstElement();
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
        return "AnimeA.net-1.0.0.";
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
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        //if (step.getStep() == PluginStep.STEP_DECRYPT) {
            ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
            parameter = parameter.replaceAll(" ", "+");

            try {
                URL url = new URL(parameter);
                RequestInfo reqinfo = HTTP.getRequest(url);

                String[] links = reqinfo.getRegexp("onclick=\"reqLink\\(\'(.*?)\'\\)").getMatches(1);
                progress.setRange(links.length);

                for (int i = 0; i < links.length; i++) {
                    reqinfo = HTTP.getRequest(new URL("http://www.animea.net/download_link.php?e_id=" + links[i]));
                    ArrayList<String> erg = new XPath(reqinfo.getHtmlCode(), "//td[1]/a").getAttributeMatches("href");
                    for(int j=0; j<erg.size(); j++) {
                        decryptedLinks.add(this.createDownloadlink(erg.get(j)));
                    }
                    progress.increase(1);
                }

                //step.setParameter(decryptedLinks);
            } catch (IOException e) {
                e.printStackTrace();
            }
        
        return decryptedLinks;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
}
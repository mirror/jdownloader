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
import java.util.Vector;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;

public class ImagefapCom extends PluginForDecrypt {
    static private final String host = "imagefap.com folder";
    private String version = "1.0.0.0";

    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?imagefap\\.com/(gallery\\.php\\?gid=.+|gallery/.+)", Pattern.CASE_INSENSITIVE);

    private ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
    private URL url;

    public ImagefapCom() {
        super();
        //steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));

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
        return host + " - " + version;
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
            try {
                parameter = parameter.replaceAll("view\\=[0-9]+", "view=2");
                if (!parameter.contains("view=2")) parameter += "&view=2";
                url = new URL(parameter);
                RequestInfo reqinfo = HTTP.getRequest(url);
                String links[][] = new Regex(reqinfo.getHtmlCode(), Pattern.compile("image\\.php\\?id=(.*?)\">", Pattern.CASE_INSENSITIVE)).getMatches();
                progress.setRange(links.length);
                for (int i = 0; i < links.length; i++) {
                    DownloadLink link = this.createDownloadlink("http://imagefap.com/image.php?id=" + links[i][0]);
                    decryptedLinks.add(link);
                    progress.increase(1);
                }
                // Decrypt abschliessen
                //step.setParameter(decryptedLinks);
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
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
import java.net.URLDecoder;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;

public class MirrorItDe extends PluginForDecrypt {

    final static String host = "mirrorit.de";
    private String version = "1.1.2";
    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?mirrorit\\.de/\\?id=[a-zA-Z0-9]{16}", Pattern.CASE_INSENSITIVE);

    public MirrorItDe() {
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
        return host + "-" + version;
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
                RequestInfo reqInfo = HTTP.getRequest(new URL(parameter));
                String[][] links = new Regex(reqInfo.getHtmlCode(), Pattern.compile("launchDownloadURL\\(\'(.*?)\', \'(.*?)\'\\)", Pattern.CASE_INSENSITIVE)).getMatches();
                progress.setRange(links.length);
                logger.info(links.length + "");

                for (int i = 0; i < links.length; i++) {
                    logger.info(URLDecoder.decode(links[i][0], "UTF-8") + " == " + links[i][1]);
                    reqInfo = HTTP.getRequest(new URL("http://www.mirrorit.de/Out?id=" + URLDecoder.decode(links[i][0], "UTF-8") + "&num=" + links[i][1]));
                    reqInfo = HTTP.getRequest(new URL(reqInfo.getLocation()));

                    decryptedLinks.add(this.createDownloadlink(reqInfo.getLocation()));
                    progress.increase(1);
                }
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
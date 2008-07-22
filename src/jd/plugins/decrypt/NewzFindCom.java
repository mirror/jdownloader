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

import jd.parser.HTMLParser;
import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;

public class NewzFindCom extends PluginForDecrypt {

    final static String host = "newzfind.com";
    private String version = "0.1.0";
    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?newzfind\\.com/(video|music|games|software|mac|graphics|unix|magazines|e-books|xxx|other)/.+", Pattern.CASE_INSENSITIVE);

    public NewzFindCom() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
    }

    @Override
    public String getCoder() {
        return "jD-Team";
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

                String url = "http://newzfind.com/ajax/links.html?a=" + parameter.substring(parameter.lastIndexOf("/") + 1);
                RequestInfo reqinfo = HTTP.getRequest(new URL(url));
                ArrayList<ArrayList<String>> links = SimpleMatches.getAllSimpleMatches(reqinfo.getHtmlCode(), "<link title=\"°\"");

                reqinfo = HTTP.getRequest(new URL(parameter));
                Vector<String> pws = HTMLParser.findPasswords(reqinfo.getHtmlCode());
                default_password.addAll(pws);

                progress.setRange(links.size());

                for (int i = 0; i < links.size(); i++) {

                    decryptedLinks.add(this.createDownloadlink(links.get(i).get(0)));
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
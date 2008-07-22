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

import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;

public class LeecherWs extends PluginForDecrypt {

    final static String host = "leecher.ws";

    private String version = "0.1.0";

    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?leecher\\.ws/(folder/.+|out/.+/[0-9]+)", Pattern.CASE_INSENSITIVE);

    public LeecherWs() {
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
            RequestInfo reqinfo;
            ArrayList<ArrayList<String>> outLinks = new ArrayList<ArrayList<String>>();

            try {

                if (parameter.indexOf("out") != -1) {
                    ArrayList<String> tempVector = new ArrayList<String>();
                    tempVector.add(parameter.substring(parameter.lastIndexOf("leecher.ws/out/") + 15));
                    outLinks.add(tempVector);

                } else {

                    reqinfo = HTTP.getRequest(new URL(parameter));
                    outLinks = SimpleMatches.getAllSimpleMatches(reqinfo.getHtmlCode(), "href=\"http://www.leecher.ws/out/°\"");

                }

                progress.setRange(outLinks.size());

                for (int i = 0; i < outLinks.size(); i++) {

                    reqinfo = HTTP.getRequest(new URL("http://leecher.ws/out/" + outLinks.get(i).get(0)));
                    String cryptedLink = SimpleMatches.getBetween(reqinfo.getHtmlCode(), "<iframe src=\"", "\"");
                    decryptedLinks.add(this.createDownloadlink(decryptAsciiEntities(cryptedLink)));
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

    // Zeichencode-Entities (&#124 etc.) in normale Zeichen umwandeln
    private String decryptAsciiEntities(String str) {
        ArrayList<ArrayList<String>> codes = SimpleMatches.getAllSimpleMatches(str, "&#°;");
        String decodedString = "";

        for (int i = 0; i < codes.size(); i++) {
            int code = Integer.parseInt(codes.get(i).get(0));
            char[] asciiChar = { (char) code };
            decodedString += String.copyValueOf(asciiChar);
        }
        return decodedString;
    }

}
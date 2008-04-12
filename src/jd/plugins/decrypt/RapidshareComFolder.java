//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.


package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;
import jd.plugins.DownloadLink;

public class RapidshareComFolder extends PluginForDecrypt {
    static private final String host             = "rapidshare.com folder";
    private String              version          = "1.0.0.0";
    //rapidshare.com/users/AM0F5G
    static private final Pattern patternSupported = Pattern.compile("http://.*?rapidshare\\.com/users/[a-zA-Z0-9]{6}", Pattern.CASE_INSENSITIVE);
    private String              password         = "";
    private Vector<DownloadLink>      decryptedLinks   = new Vector<DownloadLink>();
    private URL                 url;

    public RapidshareComFolder() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));

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
        return "Rapidshare.com Folder-1.0.0.";
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
            try {
                url = new URL(parameter);
                RequestInfo reqinfo = getRequest(url);

                while (true) {
                    int check = countOccurences(reqinfo.getHtmlCode(), Pattern.compile("method=\"post\">Passwort:"));

                    if (check > 0) {
                        password = JDUtilities.getController().getUiInterface().showUserInputDialog("Die Links sind mit einem Passwort gesch\u00fctzt. Bitte geben Sie das Passwort ein:");

                        if (password == null) {
                            step.setParameter(decryptedLinks);
                            return null;
                        }

                        reqinfo = postRequest(url, "password=" + password);
                    }
                    else {
                        break;
                    }
                }

                getLinks(reqinfo.getHtmlCode());
                progress.setRange(decryptedLinks.size());

                for (int i = 0; i < decryptedLinks.size(); i++) {
                    progress.increase(1);
                }

                // Decrypt abschliessen

                step.setParameter(decryptedLinks);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    // Rekursion zum auslesen der
    private void getLinks(String source) {
        RequestInfo reqhelp;
        ArrayList<ArrayList<String>> ordner = getAllSimpleMatches(source, "fileicon.gif\"> <input type=\"submit\" name=\"browse\" value=\"°\"");

        for (int i = 0; i < ordner.size(); i++) {
            try {
                String hpost = ordner.get(i).get(0);
                hpost = hpost.replaceAll(" ", "+");
                hpost = hpost.replaceAll("=", "%3D");
                reqhelp = postRequest(url, "password" + password + "=&subpassword=&browse=" + hpost);
                getLinks(reqhelp.getHtmlCode());
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        ArrayList<ArrayList<String>> links = getAllSimpleMatches(source, "<center> <a href=\"°\" target=\"_blank\"");
        for (int i = 0; i < links.size(); i++) {
            decryptedLinks.add(this.createDownloadlink(links.get(i).get(0)));
        }
    }
}
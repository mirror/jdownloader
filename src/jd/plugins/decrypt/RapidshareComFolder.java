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

import jd.parser.Regex;
import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class RapidshareComFolder extends PluginForDecrypt {
    static private final String host = "rapidshare.com folder";
    private String version = "1.0.0.0";
    // http://rapidshare.com/users/32P7CI
    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?rapidshare.com/users/.+", Pattern.CASE_INSENSITIVE);
    private String password = "";
    private ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
    private String para = "";
    private String cookie = "";

    public RapidshareComFolder() {
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
        return "Rapidshare.com Folder-1.0.1.";
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
                URL url = new URL(parameter);
                para = parameter;
                RequestInfo reqinfo = HTTP.getRequest(url);

                while (true) {
                    if (reqinfo.getHtmlCode().contains("input type=\"password\" name=\"password\"")) {
                        password = JDUtilities.getController().getUiInterface().showUserInputDialog("Die Links sind mit einem Passwort gesch\u00fctzt. Bitte geben Sie das Passwort ein:");

                        if (password == null) {
                            //step.setParameter(decryptedLinks);
                            return null;
                        }

                        reqinfo = HTTP.postRequest(url, "password=" + password);
                    } else {
                        break;
                    }
                }
                cookie = reqinfo.getCookie();
                getLinks(reqinfo.getHtmlCode());
                progress.setRange(decryptedLinks.size());

                for (int i = 0; i < decryptedLinks.size(); i++) {
                    progress.increase(1);
                }
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

    private void getLinks(String source) {
        RequestInfo reqhelp;
        ArrayList<ArrayList<String>> links = SimpleMatches.getAllSimpleMatches(source, "<div style=\"text-align:right;\">°</div>");
        for (int i = 0; i < links.size(); i++) {
            if (new Regex(links.get(i).get(0), "javascript:folderoeffnen").count() > 0) {
                try {
                    reqhelp = HTTP.postRequest(new URL(para), cookie, para, null, "password=" + password + "&subpassword=&browse=ID%3D" + SimpleMatches.getBetween(links.get(i).get(0), "', '", "'"), false);
                    getLinks(reqhelp.getHtmlCode());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                decryptedLinks.add(this.createDownloadlink(SimpleMatches.getBetween(links.get(i).get(0), "href=\"", "\" ")));
            }
        }
    }
}
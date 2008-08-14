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
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class RapidshareComFolder extends PluginForDecrypt {
    static private final String host = "rapidshare.com";
    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?rapidshare.com/users/.+", Pattern.CASE_INSENSITIVE);
    private String cookie = "";
    private ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
    private String para = "";
    private String password = "";

    public RapidshareComFolder() {
        super();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        try {
            URL url = new URL(parameter);
            para = parameter;
            RequestInfo reqinfo = HTTP.getRequest(url);

            while (true) {
                if (reqinfo.getHtmlCode().contains("input type=\"password\" name=\"password\"")) {
                    password = JDUtilities.getController().getUiInterface().showUserInputDialog("Die Links sind mit einem Passwort gesch\u00fctzt. Bitte geben Sie das Passwort ein:");

                    if (password == null) { return null; }
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
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return decryptedLinks;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getHost() {
        return host;
    }

    private void getLinks(String source) {
        RequestInfo reqhelp;
        String folders[] = new Regex(source, "font\\-size:12pt\\;\" href=\"javascript:folderoeffnen\\('(\\d+?)'\\);").getMatches(1);
        String[] links=new Regex(source, "<a style=\"font-size:12pt;\" target=\"_blank\" href=\"http://rapidshare.com/files/(.*?)\">").getMatches(1);
        for (String element : folders) {
       
                try {
                    reqhelp = HTTP.postRequest(new URL(para), cookie, para, null, "password=" + password + "&subpassword=&browse=ID%3D" + element, false);
                    getLinks(reqhelp.getHtmlCode());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            
        }
        
        for (String element : links) {
           
                decryptedLinks.add(createDownloadlink("http://rapidshare.com/files/"+element));
            
        }
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
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getFirstMatch();
        return ret == null ? "0.0" : ret;
    }
}
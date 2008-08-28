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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class YourFilesBizFolder extends PluginForDecrypt {

    final static String host = "yourfiles.biz";
    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?yourfiles\\.biz/.*/folders/[0-9]+/.+\\.html", Pattern.CASE_INSENSITIVE);

    public YourFilesBizFolder() {
        super();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        
        try {
            RequestInfo reqinfo = HTTP.getRequest(new URL(parameter));
            if (reqinfo.getHtmlCode().contains("Ordner Passwort")) {
                String url = parameter.substring(0, parameter.lastIndexOf("/") + 1) + new Regex(reqinfo.getHtmlCode(), "action\\=(folders\\.php\\?fid\\=.*)method\\=post>").getMatch(0).trim();
                String cookie = reqinfo.getCookie();
                String password = JDUtilities.getController().getUiInterface().showUserInputDialog(JDLocale.L("plugins.decrypt.passwordProtected", "Die Links sind mit einem Passwort gesch\u00fctzt. Bitte geben Sie das Passwort ein:"));
                String post = "act=login&password=" + password + "&login=Einloggen";
                HashMap<String, String> reqinfoHeaders = new HashMap<String, String>();
                reqinfoHeaders.put("Content-Type", "application/x-www-form-urlencoded");

                reqinfo = HTTP.postRequest(new URL(url), cookie, parameter, reqinfoHeaders, post, false);

                url = reqinfo.getConnection().getHeaderField("Location");
                reqinfo = HTTP.getRequest(new URL(url), reqinfo.getCookie(), parameter, false);
            }

            String ids[][] = new Regex(reqinfo.getHtmlCode(), "href='http://yourfiles\\.biz/\\?d=(.*?)'", Pattern.CASE_INSENSITIVE).getMatches();
            progress.setRange(ids.length);
            for (String[] id : ids) {
                decryptedLinks.add(createDownloadlink("http://yourfiles.biz/?d=" + id[0]));
                progress.increase(1);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return decryptedLinks;
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
    public String getPluginName() {
        return host;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

}
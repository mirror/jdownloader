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

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class LinkBankeu extends PluginForDecrypt {
    static private final String host = "LinkBank.eu";
    private String version = "1.0.0.0";

    private static final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?linkbank\\.eu/show\\.php\\?show=\\d+", Pattern.CASE_INSENSITIVE);
    private static final String CHECK_MIRRORS = "CHECK_MIRRORS";

    public LinkBankeu() {
        super();
        this.setConfigEelements();
    }

    
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        String cryptedLink = (String) parameter;
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            URL url = new URL(cryptedLink);
            RequestInfo requestInfo = HTTP.getRequest(url);
            String[][] links = new Regex(requestInfo.getHtmlCode(), Pattern.compile("onclick='posli\\(\"([\\d]+)\",\"([\\d]+)\"\\);'", Pattern.CASE_INSENSITIVE)).getMatches();
            String[] mirrors = new Regex(requestInfo.getHtmlCode(), Pattern.compile("onclick='mirror\\(\"(.*?)\"\\);'", Pattern.CASE_INSENSITIVE)).getMatches(1);
            for (int i = 0; i < links.length; i++) {
                url = new URL("http://www.linkbank.eu/posli.php?match=" + links[i][0] + "&id=" + links[i][1]);
                requestInfo = HTTP.getRequestWithoutHtmlCode(url, null, cryptedLink, false);
                decryptedLinks.add(this.createDownloadlink(requestInfo.getLocation()));
            }
            if (getProperties().getBooleanProperty(CHECK_MIRRORS, false) == true) {
                for (int i = 0; i < mirrors.length; i++) {
                    decryptedLinks.add(this.createDownloadlink(JDUtilities.htmlDecode(mirrors[i])));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return decryptedLinks;
    }

    
    public String getCoder() {
        return "JD-Team";
    }

    
    public String getHost() {
        return host;
    }

  

    
    public String getPluginName() {
        return host;
    }

    
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    
    public String getVersion() {
       String ret=new Regex("$Revision$","\\$Revision: ([\\d]*?) \\$").getFirstMatch();return ret==null?"0.0":ret;
    }

    private void setConfigEelements() {
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), CHECK_MIRRORS, JDLocale.L("plugins.decrypt.linkbankeu", "Check Mirror Links")).setDefaultValue(false));
    }

    
    public boolean doBotCheck(File file) {
        return false;
    }
}

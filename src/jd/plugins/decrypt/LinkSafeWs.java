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
import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class LinkSafeWs extends PluginForDecrypt {
    private static final String host = "linksafe.ws";
   
    private static final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?linksafe\\.ws/files/[a-zA-Z0-9]{4}-[\\d]{5}-[\\d]", Pattern.CASE_INSENSITIVE);

    /*
     * Suchmasken
     */
    private static final String FILES = "<input type='hidden' name='id' value='°' />°<input type='hidden' name='f' value='°' />";
    private static final String LINK = "<iframe frameborder=\"0\" height=\"100%\" width=\"100%\" src=\"°\">";

    public LinkSafeWs() {
        super();
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

    
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            String strURL = parameter;
            URL url = new URL(strURL);
            RequestInfo reqinfo = HTTP.getRequest(url);
            ArrayList<ArrayList<String>> files = SimpleMatches.getAllSimpleMatches(reqinfo.getHtmlCode(), FILES);
            progress.setRange(files.size());
            for (int i = 0; i < files.size(); i++) {
                reqinfo = HTTP.postRequest(new URL("http://www.linksafe.ws/go/"), reqinfo.getCookie(), strURL, null, "id=" + files.get(i).get(0) + "&f=" + files.get(i).get(2) + "&Download.x=5&Download.y=10&Download=Download", true);
                String newLink = SimpleMatches.getSimpleMatch(reqinfo.getHtmlCode(), LINK, 0);
                decryptedLinks.add(this.createDownloadlink(JDUtilities.htmlDecode(newLink)));
                progress.increase(1);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return decryptedLinks;
    }

    
    public boolean doBotCheck(File file) {
        return false;
    }
}

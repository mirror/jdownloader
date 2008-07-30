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
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

public class ShareProtect extends PluginForDecrypt {
    final static String host = "shareprotect.t-w.at";
    private static String VERSION = "1.0.0.0";

    private Pattern patternSupported = Pattern.compile("http://shareprotect\\.t-w\\.at/\\?id\\=[a-zA-Z0-9\\-]{3,10}", Pattern.CASE_INSENSITIVE);

    public ShareProtect() {
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

            request.getRequest(parameter);
            String[] matches = request.getRegexp("unescape\\(\\'(.*?)'\\)").getMatches(1);
            StringBuffer htmlc = new StringBuffer();
            for (int i = 0; i < matches.length; i++) {
                htmlc.append(JDUtilities.htmlDecode(matches[i]) + "\n");
            }
            requestInfo = request.getRequestInfo();
            requestInfo.setHtmlCode(htmlc.toString());

            String[] links = request.getRegexp("<input type=\"button\" value=\"Free\" onClick=.*? window\\.open\\(\\'\\./(.*?)\\'").getMatches(1);
            progress.setRange(links.length);
            htmlc = new StringBuffer();
            for (int i = 0; i < links.length; i++) {
                request.getRequest("http://" + request.getHost() + "/" + links[i]);
                htmlc.append(JDUtilities.htmlDecode(request.getRegexp("unescape\\(\\'(.*?)'\\)").getFirstMatch()) + "\n");
                progress.increase(1);
            }
            requestInfo.setHtmlCode(htmlc.toString());
            Form[] forms = requestInfo.getForms();
            for (int i = 0; i < forms.length; i++) {
                decryptedLinks.add(createDownloadlink(forms[i].action));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return decryptedLinks;
    }

    
    public boolean doBotCheck(File file) {
        return false;
    }
}
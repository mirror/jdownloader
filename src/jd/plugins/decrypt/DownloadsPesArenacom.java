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

public class DownloadsPesArenacom extends PluginForDecrypt {

    static private String host = "downloads.pes-arena.com";

    private String version = "1.0.0.0";
    final static private Pattern patternSupported = Pattern.compile("http://downloads\\.pes-arena\\.com/\\?id=(\\d+)", Pattern.CASE_INSENSITIVE);

    public DownloadsPesArenacom() {
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
        String cryptedLink = (String) parameter;
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            String id = new Regex(cryptedLink, patternSupported).getFirstMatch();
            if (id != null) {
                URL url = new URL("http://downloads.pes-arena.com/content.php?id=" + id);
                RequestInfo reqinfo = HTTP.getRequest(url);
                String link = JDUtilities.htmlDecode(new Regex(reqinfo.getHtmlCode(), "<iframe src=\"(.*?)\"").getFirstMatch());
                if (link != null) {
                    decryptedLinks.add(this.createDownloadlink(link));
                } else
                    return null;
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

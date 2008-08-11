//jDownloader - Downloadmanager
//Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://gnu.org/licenses/>.

package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;

public class FuckTheIndustryRu extends PluginForDecrypt {

    static private String host = "fucktheindustry.ru";

    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?92\\.241\\.164\\.63/file\\.php\\?id=[\\d]+", Pattern.CASE_INSENSITIVE);
    private Pattern patternDLC       = Pattern.compile("href=\"(http://92\\.241\\.164\\.63/store/_dlc//forcedl\\.php\\?file=(.*?)\\.dlc)\"", Pattern.CASE_INSENSITIVE);
    private Pattern patternPW        = Pattern.compile("\\<input.*?id=\"pw_2_copy\".*?value=\"(.*?)\".*\\>", Pattern.CASE_INSENSITIVE);
    
    
    //<input readonly id="pw_2_copy" class="boxes-inactive" onclick="className='boxes-active'" onblur="className='boxes-inactive'" type="text" value="passcomeshere">
    //http://92.241.164.63/file.php?id=123456
    public FuckTheIndustryRu() {
        super();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) {
    	
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            URL url = new URL(parameter);
            RequestInfo reqinfo = HTTP.getRequest(url);
            //logger.info(reqinfo.getHtmlCode());
            String name =  new Regex(reqinfo.getHtmlCode(), patternDLC).getMatch(1);
            String link =  new Regex(reqinfo.getHtmlCode(), patternDLC).getMatch(0);
            String pass =  new Regex(reqinfo.getHtmlCode(), patternPW).getMatch(0);
            logger.info(name + " - " + link + " - " + pass);
            if ((link != null)&&(name != null)) {
                FilePackage fp = new FilePackage();
                fp.setName(name);
                fp.setPassword(pass);
                fp.setComment("from " + parameter);
                DownloadLink thislink = createDownloadlink(link);
                thislink.setSourcePluginComment("from " + parameter);
                logger.info("DLC: " + link + " Name: " + name + " Pass: " + pass);
                decryptedLinks.add(thislink);
                
            } else {
                return null;
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
        String ret = new Regex("$Revision: 2140 $", "\\$Revision: ([\\d]*?) \\$").getFirstMatch();
        return ret == null ? "0.0" : ret;
    }
}
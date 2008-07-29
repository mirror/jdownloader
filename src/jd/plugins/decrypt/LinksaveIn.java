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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;
import jd.plugins.DownloadLink;
import jd.plugins.HTTPConnection;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;


public class LinksaveIn extends PluginForDecrypt {

    static private final String HOST = "Linksave.in";

    private String VERSION = "1.0.0";

    private String CODER = "JD-Team";

    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?linksave\\.in/[a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE);

    public LinksaveIn() {
        super();        
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public String getPluginID() {
        return HOST + "-" + VERSION;
    }

    @Override
    public String getPluginName() {
        return HOST;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        String cryptedLink = (String) parameter;        
            ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
            URL url;
            try {
                if (cryptedLink.matches(patternSupported.pattern())) {
                    cryptedLink = cryptedLink + ".dlc";
                    url = new URL(cryptedLink);
                    File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".dlc");
                    HTTPConnection dlc_con = new HTTPConnection(url.openConnection());
                    dlc_con.setRequestProperty("Referer", "jDownloader.ath.cx");
                    JDUtilities.download(container, dlc_con);
                    JDUtilities.getController().loadContainerFile(container);
                }
            } catch (MalformedURLException e) {                
                e.printStackTrace();
                return null;
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
}

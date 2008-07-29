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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;

public class LoadingCc extends PluginForDecrypt {

    static private String host = "loading.cc";

    private String version = "1.0.0.0";
    private Pattern patternSupported = Pattern.compile("http://.*?loading\\.cc/detail\\.php\\?id=[0-9]+", Pattern.CASE_INSENSITIVE);

    public LoadingCc() {
        super();
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
        return host + "-" + version;
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
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            URL url = new URL(parameter);
            RequestInfo reqinfo = HTTP.getRequest(url);
            String content = new Regex(reqinfo.getHtmlCode(), "\\<!-- Hauptfenster --\\>(.*?)\\<!-- Rechte Navigation --\\>").getFirstMatch();
            String[] links = new Regex(content, "<a href=\"([^\"]*?)\" target=\"\\_blank\"><img src='images[^>]*?' border=\"0\" />").getMatches(1);
            progress.setRange(links.length);
            for (int i = 0; i < links.length; i++) {
                if (!links[i].matches("(?is)http://.*?loading\\.cc.*")) {
                    decryptedLinks.add(this.createDownloadlink(links[i]));
                }
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
}
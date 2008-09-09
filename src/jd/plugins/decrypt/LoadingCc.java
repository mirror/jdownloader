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

import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class LoadingCc extends PluginForDecrypt {

    static private String host = "loading.cc";

    private Pattern patternSupported = Pattern.compile("http://.*?loading\\.cc/detail\\.php\\?id=[0-9]+", Pattern.CASE_INSENSITIVE);

    public LoadingCc(String cfgName){
        super(cfgName);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        String content = new Regex(br.getPage(parameter), "\\<!-- Hauptfenster --\\>(.*?)\\<!-- Rechte Navigation --\\>").getMatch(0);
        String[] links = new Regex(content, "<a href=\"([^\"]*?)\" target=\"\\_blank\"><img src='images[^>]*?' border=\"0\" />").getColumn(0);
        progress.setRange(links.length);
        for (int i = 0; i < links.length; i++) {
            if (!links[i].matches("(?is)http://.*?loading\\.cc.*")) {
                decryptedLinks.add(createDownloadlink(links[i]));
            }
            progress.increase(1);
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
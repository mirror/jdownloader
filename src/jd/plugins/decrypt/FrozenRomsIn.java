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

public class FrozenRomsIn extends PluginForDecrypt {
    final static String host = "frozen-roms.in";
    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?frozen-roms\\.in/(details_[0-9]+|get_[0-9]+_[0-9]+)\\.html", Pattern.CASE_INSENSITIVE);

    

    public FrozenRomsIn() {
        super();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        RequestInfo reqinfo;
        String getLinks[][];
        try {
            if (parameter.indexOf("get") != -1) {
                getLinks = new Regex(parameter, Pattern.compile("http://[\\w\\.]*?frozen-roms\\.in/get_(.*?)\\.html", Pattern.CASE_INSENSITIVE)).getMatches();
            } else {
                reqinfo = HTTP.getRequest(new URL(parameter));
                getLinks = new Regex(reqinfo.getHtmlCode(), Pattern.compile("href=\"http://[\\w\\.]*?frozen-roms\\.in/get_(.*?)\\.html\"")).getMatches();
            }
            progress.setRange(getLinks.length);
            for (String[] element : getLinks) {
                reqinfo = HTTP.getRequest(new URL("http://frozen-roms.in/get_" + element[0] + ".html"));
                decryptedLinks.add(createDownloadlink(reqinfo.getConnection().getHeaderField("Location")));
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
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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;

public class YouPornCom extends PluginForDecrypt {
    private static final String CODER = "JD-Team";
    private static final String HOST = "youporn.com";
    private static final Pattern patternSupported_File = Pattern.compile("http://[\\w\\.]*?youporn\\.com/watch/\\d+/?.*/?", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternSupported_Other = Pattern.compile("http://[\\w\\.]*?youporn\\.com/(.*?page=\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternSupported = Pattern.compile(patternSupported_File.pattern() + "|" + patternSupported_Other.pattern(), Pattern.CASE_INSENSITIVE);
    private static final Pattern DOWNLOADFILE = Pattern.compile("download\\.youporn\\.com/download/\\d+/flv/[^\\?]*", Pattern.CASE_INSENSITIVE);

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        RequestInfo loader;
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            if (new Regex(parameter, patternSupported_Other).matches()) {
                loader = HTTP.getRequest(new URL(parameter), "age_check=1", "", true);
                String[] matches = new Regex(loader.getHtmlCode(), Pattern.compile("<a href=\"(/watch[^\"]+)\">.*?</.*?></", Pattern.CASE_INSENSITIVE)).getMatches(1);
                for (String link : matches) {
                    DownloadLink dlink = createDownloadlink("http://youporn.com" + link);
                    decryptedLinks.add(dlink);
                }
            } else {
                loader = HTTP.getRequest(new URL(parameter), "age_check=1", "", true);
                String matches = new Regex(loader.getHtmlCode(), DOWNLOADFILE).getFirstMatch();
                if (matches == null) {
                    return null;
                }
                DownloadLink dlink = createDownloadlink("http://" + matches);
                dlink.setBrowserUrl(parameter);
                decryptedLinks.add(dlink);
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
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getCoder() {
        // TODO Auto-generated method stub
        return CODER;
    }

    @Override
    public String getHost() {
        // TODO Auto-generated method stub
        return HOST;
    }

    @Override
    public String getPluginName() {
        // TODO Auto-generated method stub
        return HOST;
    }

    @Override
    public Pattern getSupportedLinks() {
        // TODO Auto-generated method stub
        return patternSupported;
    }

    @Override
    public String getVersion() {
        // TODO Auto-generated method stub
        String ret = new Regex("$Revision: 2107 $", "\\$Revision: ([\\d]*?) \\$").getFirstMatch();
        return ret == null ? "0.0" : ret;
    }

}

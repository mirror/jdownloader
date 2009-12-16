//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "lastfm.de" }, urls = { "http://([\\w\\.]|cn)*?(lastfm|last)\\.(fm|de|pl|es|fr|it|jp|com\\.br|ru|se|com\\.tr)/(user/[a-zA-Z0-9]+|.+)" }, flags = { 0 })
public class LstFmDe extends PluginForDecrypt {

    private static final String lang = "?setlang=en";

    public LstFmDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        long pages = 0;
        br.setFollowRedirects(true);
        br.setDebug(true);
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; de; rv:1.9.1.5) Gecko/20091102 Firefox/3.5.5 (.NET CLR 3.5.30729)");
        
        String parameter = param.toString();
        br.getPage(parameter + lang);
        if(parameter.contains("/_/") || parameter.contains("?autostart")) {
            progress.setRange(1);
            decryptedLinks.add(createDownloadlink(parameter.replace(".last", "ik3mg23seryi4521")));
            progress.increase(1);
            return decryptedLinks;
        } else if (parameter.contains("/user/")) {
            br.getPage(parameter + lang);
            if(br.containsHTML("User not found")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
            if(br.containsHTML("/tracks\" title=\"\">See more</a>")) {
                br.getPage(parameter + "/tracks" + lang);
                if(br.containsHTML("class=\"lastpage\">")) {
                    String pagesString = br.getRegex("class=\"lastpage\">([0-9]+)</a>").getMatch(0);
                    pages = Long.parseLong(pagesString);
                    progress.setRange(pages);
                }
                for(long i = 1; i <= pages; i++) {
                    br.getPage(parameter + "/tracks" + "?page=" + i + lang);
                    getLinks(decryptedLinks);
                    progress.increase(1);
                }
            }
            
        } else {
            br.getPage(parameter + "/+charts" + lang);
            if(br.containsHTML("Sorry, that artist could not be found.")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
            getLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private void getLinks(ArrayList<DownloadLink> links) {
        String host = "http://www.last.fm";
        String[] songs = br.getRegex("<a class=\"playbutton\" href=\"(.*?)\\?autostart\"").getColumn(0);
        if (songs != null) {
            for (String song : songs) {
                String finallink = host + song + lang;
                links.add(createDownloadlink(finallink.replace(".last", "ik3mg23seryi4521")));
            }
        }
    }
}

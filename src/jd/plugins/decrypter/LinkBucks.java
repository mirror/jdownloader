//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "realfiles.net", "youfap.com", "linkgalleries.net", "thesefiles.com", "urlpulse.net", "placepictures.com", "viraldatabase.com", "seriousfiles.com", "ubucks.net", "thesegalleries.com", "seriousurls.com", "blahetc.com", "baberepublic.com", "qvvo.com", "linkbucks.com" }, urls = { "http://[\\w\\.]*?realfiles\\.net(/link/[0-9a-fA-F]+(/\\d+)?)?", "http://[\\w\\.]*?youfap\\.com(/link/[0-9a-fA-F]+(/\\d+)?)?", "http://[\\w\\.]*?linkgalleries\\.net(/link/[0-9a-fA-F]+(/\\d+)?)?", "http://[\\w\\.]*?thesefiles\\.com(/link/[0-9a-fA-F]+(/\\d+)?)?", "http://[\\w\\.]*?urlpulse\\.net(/link/[0-9a-fA-F]+(/\\d+)?)?", "http://[\\w\\.]*?placepictures\\.com(/link/[0-9a-fA-F]+(/\\d+)?)?", "http://[\\w\\.]*?viraldatabase\\.com(/link/[0-9a-fA-F]+(/\\d+)?)?", "http://[\\w\\.]*?seriousfiles\\.com(/link/[0-9a-fA-F]+(/\\d+)?)?",
        "http://[\\w\\.]*?ubucks\\.net(/link/[0-9a-fA-F]+(/\\d+)?)?", "http://[\\w\\.]*?thesegalleries\\.com(/link/[0-9a-fA-F]+(/\\d+)?)?", "http://[\\w\\.]*?seriousurls\\.com(/link/[0-9a-fA-F]+(/\\d+)?)?", "http://[\\w\\.]*?blahetc\\.com(/link/[0-9a-zA-Z]+(/\\d+)?)?", "http://[\\w\\.]*?baberepublic\\.com(/link/[0-9a-zA-Z]+(/\\d+)?)?", "http://[\\w\\.]*?qvvo\\.com(/link/[0-9a-zA-Z]+(/\\d+)?)?", "http://[\\w\\.]*?linkbucks\\.com(/link/[0-9a-zA-Z]+(/\\d+)?)?"

}, flags = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 })
public class LinkBucks extends PluginForDecrypt {

    public LinkBucks(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        String link = br.getRegex(Pattern.compile("</a>\\s+<a\\shref=\"(.*?)\"\\sclass=\"lb_link\\sright", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        if (link == null) {
            link = br.getRegex(Pattern.compile("AdBriteInit(\"(.*?)\")", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        }
        if (link == null) {
            link = br.getRegex(Pattern.compile("linkDestUrl\\s+=\\s+'(.*?)';", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        }
        if (link == null) {
            link = br.getRegex(Pattern.compile("var\\s+LinkURL\\s+=\\s+'(.*?)';", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        }
        if (link == null) return null;
        decryptedLinks.add(createDownloadlink(link));

        return decryptedLinks;
    }

    // @Override
    
}

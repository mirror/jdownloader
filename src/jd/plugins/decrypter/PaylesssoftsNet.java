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
import java.util.LinkedHashMap;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class PaylesssoftsNet extends PluginForDecrypt {

    public PaylesssoftsNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    //@Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String param = parameter.toString();
        br.setFollowRedirects(true);
        br.getPage(param);
        param = br.getURL();
        br.setFollowRedirects(false);

        String[] worker = param.split("=");
        String code = worker[1].substring(0, 3);
        String id = worker[1].substring(3);
        LinkedHashMap<String, String> post = new LinkedHashMap<String, String>();
        post.put("code", code);
        post.put("id", id);
        String rsOrMega = new Regex(param, "\\.net\\/(.*?)\\/").getMatch(0);
        br.postPage("http://www.paylesssofts.net/" + rsOrMega + "/fdngenerate.php", post);

        // If Uploader posted not existent URL, Decrypter reports a warning
        // which we catch here to get the link
        String failedUrl = br.getRegex("<b>Warning<\\/b>\\:  file\\((.*?)\\)\\:").getMatch(0);
        if (failedUrl != null) {
            decryptedLinks.add(createDownloadlink(failedUrl));
            return decryptedLinks;
        }

        br.getPage("http://www.paylesssofts.net/" + rsOrMega + "/fdngetfile.php");
        String finalurl = br.getRegex("<INPUT type=hidden value=(.*?) name=link>").getMatch(0);
        decryptedLinks.add(createDownloadlink(finalurl));
        return decryptedLinks;
    }

    //@Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

}

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

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class SharebankWs extends PluginForDecrypt {

    public SharebankWs(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        String[] links = new Regex(br.getPage(parameter), Pattern.compile("go=(.*?)'")).getColumn(0);

        progress.setRange(links.length);
        for (String element : links) {
            /* Get the security id and save them */
            String securityId = new Regex(br.getPage("http://sharebank.ws/?go=" + element), Pattern.compile("(go=.*?&q1=.*?&q2=.*?)>")).getMatch(0);

            /* Follow the link with the securityId and filter out the finalLink */
            String finalLink = new Regex(br.getPage("http://sharebank.ws/?" + securityId), Pattern.compile("<iframe src='(.*)' marginheight=")).getMatch(0);

            decryptedLinks.add(createDownloadlink(finalLink));
            progress.increase(1);
        }

        return decryptedLinks;
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }
}

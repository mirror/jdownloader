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
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class SharebankWs extends PluginForDecrypt {
    private static final String REGEX_FOLDER = "http://[\\w\\.]*?sharebank\\.ws/\\?v=[a-zA-Z0-9]+";
    private static final String REGEX_DLLINK = "http://[\\w\\.]*?sharebank\\.ws/\\?go=([a-zA-Z0-9]+)";

    public SharebankWs(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String url = param.toString();
        Regex urlGoRegex = new Regex(url, Pattern.compile(REGEX_DLLINK));
        br.setDebug(true);
        String[] links = null;
        if (url.matches(REGEX_FOLDER)) {
            br.getPage(url);
            links = br.getRegex(Pattern.compile("go=(.*?)'")).getColumn(0);

        } else if (urlGoRegex.matches()) {
            links = new String[] { urlGoRegex.getMatch(0) };
        } else {
            logger.severe("Ungültiges Pattern in der JDinit für Sharebank.Ws");
        }

        progress.setRange(links.length);
        for (String element : links) {
            /* Get the security id and save them */
            Browser brc = br.cloneBrowser();
            brc.getPage("http://sharebank.ws/?go=" + element);
            String finalLink = null;
            for (int retry = 0; retry < 5; retry++) {
                String securityId = brc.getRegex(Pattern.compile("(go=.*?&q1=.*?&q2=.*?)>")).getMatch(0);
                brc.getPage("http://sharebank.ws/?" + securityId);
                finalLink = brc.getRegex(Pattern.compile(">document.location='(.*?)';<")).getMatch(0);
                if (finalLink != null && !finalLink.startsWith("?go")) {
                    break;
                }
            }
            if (finalLink == null) return null;
            /* find base64 coded url */
            String finalLink2 = brc.getRegex(Pattern.compile("base64_decode\\('(.*?)'\\)")).getMatch(0);
            if (finalLink2 != null) {
                decryptedLinks.add(createDownloadlink(Encoding.Base64Decode(finalLink2)));
            } else {
                decryptedLinks.add(createDownloadlink(finalLink));
            }
            progress.increase(1);
        }

        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}

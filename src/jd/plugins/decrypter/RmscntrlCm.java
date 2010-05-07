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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class RmscntrlCm extends PluginForDecrypt {

    static private String lastRootCatUrl = "http://www.romscentral.com/<kategorie>/<htmlseite>";
    static private final Pattern patternSupportedWay1 = Pattern.compile("http://[\\w.]*?romscentral\\.com/(.+)/(.+\\.htm)", Pattern.CASE_INSENSITIVE);
    static private final Pattern patternSupportedWay2 = Pattern.compile("onclick=\"return popitup\\('(.+\\.htm)'\\)", Pattern.CASE_INSENSITIVE);

    public RmscntrlCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        Matcher matcher = patternSupportedWay1.matcher(parameter);
        Matcher matcher2 = patternSupportedWay2.matcher(parameter);
        if (matcher.find()) {
            br.getPage(param.toString());

            String file = new Regex(br, patternSupportedWay1.pattern()).getMatch(0);
            decryptedLinks.add(createDownloadlink(file));
        } else if (matcher2.find()) {
            String rootUrl = getUserInput("Bitte gebe die URL an, von woher du den Quelltext eingefügt hast!", lastRootCatUrl, param);

            matcher = patternSupportedWay1.matcher(rootUrl);
            if (matcher.find() && !rootUrl.equals("http://www.romscentral.com/<kategorie>/<htmlseite>")) {
                lastRootCatUrl = rootUrl;
                String rootCat = new Regex(rootUrl, patternSupportedWay1).getMatch(0);
                String matches[] = new Regex(parameter, patternSupportedWay2.pattern()).getRow(0);
                for (String match : matches) {
                    decryptedLinks.add(createDownloadlink("http://www.romscentral.com/" + rootCat + "/" + match));
                }
            }
        }
        return decryptedLinks;
    }

}

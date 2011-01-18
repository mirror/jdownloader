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

import org.appwork.utils.Regex;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rapidfolder.com" }, urls = { "http://[\\w\\.]*?rapidfolder\\.com/\\?\\w+" }, flags = { 0 })
public class RpdFldrCm extends PluginForDecrypt {
    private static final Pattern PATTERN_SUPPORTED = Pattern.compile("http://[\\w\\.]*?rapidfolder\\.com/\\?(\\w+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_REDIRECT_URL = Pattern.compile("action=\"(http://.+?)\">");
    // private static final Pattern PATTERN_DOWNLOADLINK = Pattern.compile(
    // "<input type=\"button\" value=\"Download\" onClick=\"window.open\\('./download.php?id=\\w+&subid=\\d+',"
    // );
    private static final String DOWNLOAD_PHP = "http://rapidfolder.com/download.php?id={id}&subid={subid}";

    public RpdFldrCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink cryptedLink, ProgressController progress) throws Exception {
        br.setFollowRedirects(false);
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String id = new Regex(cryptedLink.getCryptedUrl(), PATTERN_SUPPORTED).getMatch(0);
        // Um kein neues String Objekt zu erzeugen wird hier einfach ein schon
        // vorhandenes referenziert.
        // Es könnte genauso gut = ""; im Source stehen!
        String redirectUrl = DOWNLOAD_PHP;
        String downloadUrl;
        int count = 0;
        boolean running = true;
        while (running) {
            downloadUrl = DOWNLOAD_PHP.replaceAll("\\{id\\}", id).replaceAll("\\{subid\\}", String.valueOf(count));
            // Bei manchen Downloads ist page == ""; Dann gibt es eine
            // redirectUrl, bei anderen Downloads steht die RedirectUrl im html
            // source.
            if (br.getPage(downloadUrl).equals("")) {
                redirectUrl = br.getRedirectLocation();
            } else {
                redirectUrl = br.getRegex(PATTERN_REDIRECT_URL).getMatch(0);
            }
            running = !(redirectUrl == null || redirectUrl.equals("http://"));
            if (running) {
                decryptedLinks.add(createDownloadlink(redirectUrl));
                count++;
            }

        }

        return decryptedLinks;
    }

    // @Override

}

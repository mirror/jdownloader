//jDownloader - Downloadmanager
//Copyright (C) 2020  JD-Team support@jdownloader.org
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
//along with this program.  If not, see <https?://www.gnu.org/licenses/>.
package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.*;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "animestelecine.top" }, urls = { "https?://(?:www\\.)?animestelecine\\.top/link/[^/]+/?" })
public class AnimesTelecine extends PluginForDecrypt {
    static final int API_URL_REQUEST_SLEEP_SECONDS = 7;

    public AnimesTelecine(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        String html = br.toString();

        String linkId = parseLinkId();

        if (linkId.isEmpty()) {
            return decryptedLinks;
        }

        String episodeName = parseEpisodeName(html);
        String finalLink = requestDownloadLink(linkId);
        decryptedLinks.add(createDownloadlink(Encoding.htmlOnlyDecode(finalLink)));

        if (!episodeName.isEmpty()) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(episodeName));
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

    private String parseLinkId() {
        String [][] matches = br.getRegex("<meta id=\"link-id\" value=\"(.*?)\">").getMatches();
        if (matches.length == 0){
            logger.warning("API lnk ID not found");
            return "";
        }
        return matches[0][0];
    }

    private String parseEpisodeName(String html) {
        String [][] matches = br.getRegex("<h2 class=\"media-heading\">(.*?)</h2>").getMatches();
        if (matches.length == 0){
            logger.warning("API lnk ID not found");
            return "";
        }
        return matches[0][0];
    }

    private String requestDownloadLink(String linkId) throws Exception {
        String apiUrl = br.getBaseURL().replace("link/", "api/link/" + linkId);
        TimeUnit.SECONDS.sleep(API_URL_REQUEST_SLEEP_SECONDS);
        br.getPage(apiUrl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            logger.warning("API URL link not working: " + apiUrl);
            return "";
        }

        String [][] matches = br.getRegex("\"link\":\"(.*?)\"}").getMatches();
        if (matches.length == 0) {
            logger.warning("Unable to parse API response");
            return "";
        }
        return matches[0][0];
    }
}

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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.*;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "animestelecine.top" }, urls = { "https?://(?:www\\.)?animestelecine\\.top/link/[^/]+/?" })
public class AnimesTelecine extends PluginForDecrypt {
    final int API_URL_REQUEST_SLEEP_MILLISECONDS = 7000;

    public AnimesTelecine(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        String linkId = parseLinkId();
        String episodeName = parseEpisodeName();
        String finalLink = requestDownloadLink(linkId, param);
        decryptedLinks.add(createDownloadlink(Encoding.htmlOnlyDecode(finalLink)));
        if (!episodeName.isEmpty()) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(episodeName));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private String parseLinkId() throws PluginException {
        final String[][] matches = br.getRegex("<meta id=\"link-id\" value=\"(.*?)\">").getMatches();
        if (matches.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "API lnk ID not found");
        }
        return matches[0][0];
    }

    private String parseEpisodeName() {
        final String[][] matches = br.getRegex("<h2 class=\"media-heading\">(.*?)</h2>").getMatches();
        if (matches.length == 0) {
            logger.warning("Episode name not found");
            return "";
        }
        return matches[0][0];
    }

    private String requestDownloadLink(String linkId, CryptedLink param) throws Exception {
        sleep(API_URL_REQUEST_SLEEP_MILLISECONDS, param);
        br.getPage("/api/link/" + linkId);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String[][] matches = br.getRegex("\"link\":\"(.*?)\"}").getMatches();
        if (matches.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to parse API response");
        }
        return matches[0][0];
    }
}

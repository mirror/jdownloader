//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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
//along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.decrypter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.appwork.utils.Regex;

import com.fasterxml.jackson.databind.ObjectMapper;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 39999 $", interfaceVersion = 2, names = { "onmovies.se" }, urls = { "https?://(www\\.)?onmovies\\.se/(tv|film)/.+" })
public class OnMovies extends PluginForDecrypt {
    public OnMovies(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        String page = br.getPage(parameter);
        String fpName = br.getRegex("<meta (?:name|property)=\"og:title\" content=[\"'](?:Watch ?)([^<>\\\"]*?)(?: ?\\(HD Online\\))[\"']/>").getMatch(0);
        String itemType = new Regex(parameter, "https?://(?:www\\.)?onmovies\\.se/(tv|film)/([0-9a-zA-Z]+)/.+").getMatch(0);
        String itemID = new Regex(parameter, "https?://(?:www\\.)?onmovies\\.se/(tv|film)/([0-9a-zA-Z]+)/.+").getMatch(1);
        ArrayList<DownloadLink> links = getLinks(br, itemType, itemID);
        decryptedLinks.addAll(links);
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> getLinks(Browser br, String itemType, String itemID) throws IOException, Exception {
        ArrayList<DownloadLink> results = new ArrayList<DownloadLink>();
        String detailURL = "/ajax/mep.php?id=" + itemID;
        final Browser brDetail = br.cloneBrowser();
        String details = brDetail.getPage(detailURL);
        String detailHTML = new Regex(details, "\"html\":\"(.+)\"}$").getMatch(0);
        String[][] detailItems = new Regex(detailHTML, "data-id=\\\\\"([0-9a-zA-Z]*)\\\\\"[\\r\\t\\n ]+data-epNr=\\\\\"([0-9a-zA-Z]*)\\\\\"[\\r\\t\\n ]+data-so=\\\\\"([0-9a-zA-Z]*)\\\\\"[\\r\\t\\n ]+data-server=\\\\\"([0-9a-zA-Z]*)\\\\\"[\\r\\t\\n ]+data-tip=\\\\\"([0-9a-zA-Z]*)\\\\\"[\\r\\t\\n ]+data-index=\\\\\"([0-9a-zA-Z]*)\\\\\"[\\r\\t\\n ]+data-srvr=\\\\\"([0-9a-zA-Z]*)\\\\\"[^>]+>([^<]+)<").getMatches();
        if (detailItems == null) {
            // Escape procedure if we don't get any show/movie details
            return results;
        }
        for (String[] detailItem : detailItems) {
            results.addAll(getVideoLinks(brDetail, itemType, itemID, detailItem));
        }
        return results;
    }

    private ArrayList<DownloadLink> getVideoLinks(Browser br, String itemType, String itemID, String[] detailItem) throws IOException, Exception {
        ArrayList<DownloadLink> results = new ArrayList<DownloadLink>();
        String detailURL = "/ajax/movie_embed.php?eid=" + detailItem[0] + "&lid=undefined&ts=&up=0&mid=" + itemID + "&epNr=" + detailItem[1] + "&type=" + itemType + "&server=" + detailItem[3] + "&epIndex=" + detailItem[5] + "&so=" + detailItem[2] + "&srvr=";
        String fileNamePrefix = null;
        final Browser brDetail = br.cloneBrowser();
        String details = brDetail.getPage(detailURL);
        HashMap<String, Object> jsonObject = new ObjectMapper().readValue(details, HashMap.class);
        String source = (String) jsonObject.get("src");
        //
        DownloadLink dlVideo = createDownloadlink(Encoding.htmlOnlyDecode(source));
        fileNamePrefix = detailItem[7].trim();
        dlVideo.setForcedFileName(fileNamePrefix + ".mp4");
        results.add(dlVideo);
        //
        String subSource = new Regex(source, "c[0-9]+_file=([^&$]+)").getMatch(0);
        if (subSource != null && subSource.length() > 0) {
            DownloadLink dlSub = createDownloadlink(Encoding.htmlOnlyDecode(subSource));
            if (fileNamePrefix != null) {
                dlSub.setForcedFileName(fileNamePrefix + ".srt");
            }
            results.add(dlSub);
        }
        return results;
    }
}
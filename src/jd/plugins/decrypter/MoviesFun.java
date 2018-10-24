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

@DecrypterPlugin(revision = "$Revision: 40000 $", interfaceVersion = 2, names = { "123moviesfun.org" }, urls = { "https?://([w0-9]+\\.)?123moviesfun\\.org/film/.+" })
public class MoviesFun extends PluginForDecrypt {
    public MoviesFun(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        String page = br.getPage(parameter);
        String fpName = br.getRegex("<meta (?:name|property)=\"og:title\" content=[\"'](?:Watch ?)([^<>\"]*?) online free in HD - 123movies.org[\"']/>").getMatch(0);
        //
        String mediaID = new Regex(parameter, "/film/[^/]+-([0-9]+)/,*").getMatch(0);
        decryptedLinks.addAll(getLinks(br, mediaID));
        //
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> getLinks(Browser br, String mediaID) throws IOException {
        ArrayList<DownloadLink> results = new ArrayList<DownloadLink>();
        final Browser brHTML = br.cloneBrowser();
        String json = brHTML.getPage("/ajax/movie_episodes/" + mediaID);
        HashMap<String, Object> jsonObject = new ObjectMapper().readValue(json, HashMap.class);
        String list = (String) jsonObject.get("html");
        String[][] listItems = new Regex(list, "<li class=\"ep-item\" data-index=\"([^\"]+)\" data-server=\"([^\"]+)\" data-id=\"([^\"]+)\" id=\"([^\"]+)\"><div class=\"sli-name\"><a[^>]+ title=\"\">([^<]+)<").getMatches();
        for (String[] listItem : listItems) {
            results.addAll(getVideoLinks(brHTML, mediaID, listItem));
        }
        return results;
    }

    private ArrayList<DownloadLink> getVideoLinks(Browser br, String mediaID, String[] listItem) throws IOException {
        ArrayList<DownloadLink> results = new ArrayList<DownloadLink>();
        String detailURL = "/ajax/movie_embed/" + mediaID + "/" + listItem[2] + "/" + listItem[1];
        String fileNamePrefix = listItem[4].trim();
        final Browser brJSON = br.cloneBrowser();
        String json = brJSON.getPage(detailURL);
        HashMap<String, Object> jsonObject = new ObjectMapper().readValue(json, HashMap.class);
        String source = (String) jsonObject.get("src");
        DownloadLink dlVideo = createDownloadlink(Encoding.htmlOnlyDecode(source.replaceAll("^[/]+", "https://")));
        dlVideo.setForcedFileName(fileNamePrefix + ".mp4");
        results.add(dlVideo);
        return results;
    }
}
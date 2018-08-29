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

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.Regex;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "animo-pace-stream.io" }, urls = { "https?://animo-pace-stream\\.io/[^\\.]+\\.php.*" })
public class AnimoPaceStream extends PluginForDecrypt {
    public AnimoPaceStream(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("http:", "https:");
        br.setFollowRedirects(true);
        Request request = br.createGetRequest(parameter);
        request.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        final String page = br.getPage(request);
        // Sometimes the target URL is in a Base64-encoded string
        if (page.contains("document.write(Base64.decode")) {
            String[][] regExMatches = new Regex(page, "document\\.write\\(Base64\\.decode\\(\"(.+?)\\\"").getMatches();
            String decodedData = Encoding.Base64Decode(regExMatches[0][0]);
            String decodedDataSrc = new Regex(decodedData, "src=\\\"(.+?)[ \\\"\"]+").getMatch(0);
            if (decodedDataSrc != null && !decodedDataSrc.contains("window.location.href")) {
                decryptedLinks.add(createDownloadlink(decodedDataSrc));
            }
        }
        // In some cases we can just translate the target URL from Base64-encoded URL parameters
        String[][] directDownloadMatches = br.getRegex("link=(.+?)[\"/&]").getMatches();
        for (String[] directDownloadMatch : directDownloadMatches) {
            String decodedData = Encoding.Base64Decode(directDownloadMatch[0]);
            if (decodedData != null) {
                decryptedLinks.add(createDownloadlink(decodedData));
            }
        }
        // Sometimes the URL is just a plain reference
        String videoURL = br.getRegex("window.location = '(.+?)'").getMatch(0);
        if (videoURL != null) {
            decryptedLinks.add(createDownloadlink(videoURL));
        }
        // The HTML5 player version has the file URLs embedded in a struct
        String[][] videoURLMatches = br.getRegex("file:\"(.+?)\"").getMatches();
        for (String[] videoURLMatch : videoURLMatches) {
            videoURL = Encoding.htmlDecode(videoURLMatch[0]);
            decryptedLinks.add(createDownloadlink(videoURL));
        }
        // In rare cases we have an embedded player with a dropdown list to switch between different hosters.
        String[][] selectOptionMatches = br.getRegex("<option value=\"https?://animo-pace-stream\\.io/[a-zA-Z0-9]+/[a-zA-Z0-9]+\\.php\\?[^>]+").getMatches();
        for (String[] selectOptionMatch : selectOptionMatches) {
            String selectOptionURL = Encoding.htmlDecode(selectOptionMatch[0].replace("[^\"]+\"", "").trim());
            String optionTarget = new Regex(selectOptionURL, "value=\"(.+?)\"").getMatch(0);
            if (optionTarget != null) {
                optionTarget = Encoding.htmlDecode(optionTarget);
                decryptedLinks.add(createDownloadlink(optionTarget));
            }
            optionTarget = new Regex(selectOptionURL, "data=(.+?)&").getMatch(0);
            if (optionTarget != null) {
                optionTarget = Encoding.htmlDecode(optionTarget);
                decryptedLinks.add(createDownloadlink(optionTarget));
            }
        }
        return decryptedLinks;
    }
}
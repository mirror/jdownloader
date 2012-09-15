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
import java.util.HashMap;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rapidshare.com" }, urls = { "https?://(www\\.)?rapidshare\\.com/(users/\\w+(/\\d+)?|\\#\\!users\\|\\d+-[0-9a-f]+(\\|\\d+)?)" }, flags = { 0 })
public class RapidshareFolder extends PluginForDecrypt {
    public final int MAX_FOLDERS = 10;

    public RapidshareFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter.replace("https://", "http://"));

        String[] folderInfo = new Regex(br.getURL(), "https?://(?:www\\.)?rapidshare\\.com/\\#\\!users\\|(\\d+-[0-9a-f]+)(?:\\|(\\d+))?").getRow(0);
        if (folderInfo == null || folderInfo[0] == null) return decryptedLinks;

        String contact = folderInfo[0];
        String folder = folderInfo[1];
        if (folderInfo[1] == null) {
            folder = new Regex(parameter, "https?://(?:www\\.)?rapidshare\\.com/users/\\w+/(\\d+)").getMatch(0);
        }

        HashMap<String, String[]> links = new HashMap<String, String[]>();
        br.getPage("https://api.rapidshare.com/cgi-bin/rsapi.cgi?sub=listfiles&realfolder=" + folder + "&contact=" + contact + "&fields=serverid,filename,uploadtime");
        if (br.toString().equals("NONE") || br.toString().startsWith("ERROR: ")) return decryptedLinks;

        // Adds links to a hashmap by download name, to filter old files
        for (String line : br.toString().split("\r?\n")) {
            String[] fileParams = line.split(",");
            String[] tmp = links.get(fileParams[2]);
            if (tmp != null) {
                int time1 = Integer.parseInt(fileParams[3]);
                int time2 = Integer.parseInt(tmp[3]);
                if (time1 > time2) links.put(fileParams[2], fileParams);
            } else {
                links.put(fileParams[2], fileParams);
            }
        }

        for (String[] link : links.values()) {
            decryptedLinks.add(createDownloadlink("https://rapidshare.com/#!download|" + link[1] + "|" + link[0] + "|" + link[2] + "|"));
        }

        return decryptedLinks;
    }
}

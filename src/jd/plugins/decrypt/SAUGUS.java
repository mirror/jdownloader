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

package jd.plugins.decrypt;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class SAUGUS extends PluginForDecrypt {

    private String patternSupported_go = "http://[\\w\\.]*?saug\\.us/go.+\\.php";
    private String patternSupported_folder = "http://[\\w\\.]*?saug\\.us/folder.?-[a-zA-Z0-9\\-]{30,50}\\.html";

    public SAUGUS(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        String server_folder_id = "";
        String server_id = "";
        if (parameter.matches(patternSupported_folder)) {
            if (parameter.contains("folder2")) {
                server_folder_id = "2";
            }
            if (parameter.contains("s2.saug.us")) {
                server_id = "s2.";
            }
            br.getPage(parameter);
            String folder_id = br.getRegex("onload=\"loadFolder\\('(.*?)'\\);\">").getMatch(0);
            if (folder_id == null) return null;
            br.postPage("http://" + server_id + "saug.us/folder" + server_folder_id + ".php", "id=" + folder_id);
            String ids[] = br.getRegex("javascript:page\\('.*?\\?url=(.*?)'\\)").getColumn(0);
            for (String id : ids) {
                br.getPage("http://" + server_id + "saug.us/go" + server_folder_id + ".php?url=" + id);
                String link = Encoding.htmlDecode(br.getRegex("</iframe>--><iframe src=\"(.*?)\";").getMatch(0));
                if (link != null) {
                    if (link.startsWith("http")) {
                        decryptedLinks.add(this.createDownloadlink(link));
                    } else if (link.startsWith("go_x")) {
                        br.getPage("http://" + server_id + "saug.us/" + link);
                        link = br.getRegex("<p class=\"downloadlink\">(.*?)<fon").getMatch(0);
                        if (link != null) decryptedLinks.add(this.createDownloadlink(link));
                    }
                }
            }
        } else if (parameter.matches(patternSupported_go)) {
            if (parameter.contains("folder2")) {
                server_folder_id = "2";
            }
            if (parameter.contains("s2.saug.us")) {
                server_id = "s2.";
            }
            br.getPage(parameter);
            String link = Encoding.htmlDecode(br.getRegex("</iframe>--><iframe src=\"(.*?)[\r\n\t]*?\";").getMatch(0));
            if (link != null) {
                if (link.startsWith("http")) {
                    decryptedLinks.add(this.createDownloadlink(link));
                } else if (link.startsWith("go_x")) {
                    br.getPage("http://" + server_id + "saug.us/" + link);
                    link = br.getRegex("<p class=\"downloadlink\">(.*?)<fon").getMatch(0);
                    if (link != null) decryptedLinks.add(this.createDownloadlink(link));
                }
            }
        }
        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}
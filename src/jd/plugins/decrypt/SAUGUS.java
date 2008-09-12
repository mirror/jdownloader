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
import jd.http.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class SAUGUS extends PluginForDecrypt {

    private Pattern patternSupported_go = Pattern.compile("http://[\\w\\.]*?saug\\.us/go.+\\.php", Pattern.CASE_INSENSITIVE);
    private Pattern patternSupported_folder = Pattern.compile("http://[\\w\\.]*?saug\\.us/folder.?-[a-zA-Z0-9\\-]{30,50}\\.html", Pattern.CASE_INSENSITIVE);

    public SAUGUS(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        String server_folder_id = "";
        String server_id = "";
        if (new Regex(parameter, patternSupported_folder).matches()) {
            if (parameter.contains("folder2")) {
                server_folder_id = "2";
            }
            if (parameter.contains("s2.saug.us")) {
                server_id = "s2.";
            }
            br.getPage(parameter);
            String folder_id = br.getRegex(Pattern.compile("onload=\"loadFolder\\('(.*?)'\\);\">", Pattern.CASE_INSENSITIVE)).getMatch(0);
            if (folder_id == null) return null;
            br.postPage("http://" + server_id + "saug.us/folder" + server_folder_id + ".php", "id=" + folder_id);
            String ids[] = br.getRegex(Pattern.compile("javascript:page\\('.*?\\?url=(.*?)'\\)", Pattern.CASE_INSENSITIVE)).getColumn(0);
            for (String id : ids) {
                br.getPage("http://" + server_id + "saug.us/go" + server_folder_id + ".php?url=" + id);
                String link = Encoding.htmlDecode(br.getRegex(Pattern.compile("</iframe>--><iframe src=\"(.*?)\";", Pattern.CASE_INSENSITIVE)).getMatch(0));
                if (link != null) {
                    if (link.startsWith("http")) {
                        decryptedLinks.add(this.createDownloadlink(link));
                    } else if (link.startsWith("go_x")) {
                        br.getPage("http://" + server_id + "saug.us/" + link);
                        link = br.getRegex(Pattern.compile("<p class=\"downloadlink\">(.*?)<fon", Pattern.CASE_INSENSITIVE)).getMatch(0);
                        if (link != null) decryptedLinks.add(this.createDownloadlink(link));
                    }
                }
            }
        } else if (new Regex(parameter, patternSupported_go).matches()) {
            if (parameter.contains("folder2")) {
                server_folder_id = "2";
            }
            if (parameter.contains("s2.saug.us")) {
                server_id = "s2.";
            }
            br.getPage(parameter);
            String link = Encoding.htmlDecode(br.getRegex(Pattern.compile("</iframe>--><iframe src=\"(.*?)[\r\n\t]*?\";", Pattern.CASE_INSENSITIVE)).getMatch(0));
            if (link != null) {
                if (link.startsWith("http")) {
                    decryptedLinks.add(this.createDownloadlink(link));
                } else if (link.startsWith("go_x")) {
                    br.getPage("http://" + server_id + "saug.us/" + link);
                    link = br.getRegex(Pattern.compile("<p class=\"downloadlink\">(.*?)<fon", Pattern.CASE_INSENSITIVE)).getMatch(0);
                    if (link != null) decryptedLinks.add(this.createDownloadlink(link));
                }
            }
        }
        return decryptedLinks;
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }
}
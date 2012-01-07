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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mediafire.com" }, urls = { "http://[\\w\\.]*?(?!download)[\\w\\.]*?(mediafire\\.com|mfi\\.re)/(imageview.+|i/\\?.+|\\\\?sharekey=.+|(?!download|file|\\?JDOWNLOADER).+)" }, flags = { 0 })
public class MdfrFldr extends PluginForDecrypt {

    private static boolean pluginloaded = false;

    public MdfrFldr(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("mfi.re/", "mediafire.com/");
        parameter = parameter.replaceAll("(&.+)", "").replaceAll("(#.+)", "");
        this.setBrowserExclusive();
        if (parameter.matches("http://download\\d+\\.mediafire.+")) {
            /* direct download */
            String ID = new Regex(parameter, "\\.com/\\?(.+)").getMatch(0);
            if (ID == null) ID = new Regex(parameter, "\\.com/.*?/(.*?)/").getMatch(0);
            if (ID != null) {
                DownloadLink link = createDownloadlink("http://www.mediafire.com/download.php?" + ID);
                decryptedLinks.add(link);
                return decryptedLinks;
            }
        }
        if (parameter.contains("imageview.php")) {
            String ID = new Regex(parameter, "\\.com/.*?quickkey=(.+)").getMatch(0);
            if (ID != null) {
                DownloadLink link = createDownloadlink("http://www.mediafire.com/download.php?" + ID);
                decryptedLinks.add(link);
                return decryptedLinks;
            }
            return null;
        }
        if (parameter.contains("/i/?")) {
            String ID = new Regex(parameter, "\\.com/i/\\?(.+)").getMatch(0);
            if (ID != null) {
                DownloadLink link = createDownloadlink("http://www.mediafire.com/download.php?" + ID);
                decryptedLinks.add(link);
                return decryptedLinks;
            }
            return null;
        }
        br.setFollowRedirects(false);
        br.getPage(parameter);
        if (br.getRedirectLocation() != null) {
            /* check for direct download stuff */
            String red = br.getRedirectLocation();
            if (red.matches("http://download\\d+\\.mediafire.+")) {
                /* direct download */
                String ID = new Regex(parameter, "\\.com/\\?(.+)").getMatch(0);
                if (ID == null) ID = new Regex(parameter, "\\.com/.*?/(.*?)/").getMatch(0);
                if (ID != null) {
                    DownloadLink link = createDownloadlink("http://www.mediafire.com/download.php?" + ID);
                    decryptedLinks.add(link);
                    return decryptedLinks;
                }
            } else {
                URLConnectionAdapter con = null;
                try {
                    con = br.openGetConnection(red);
                    if (con.isContentDisposition()) {
                        String ID = new Regex(red, "//.*?/.*?/(.*?)/").getMatch(0);
                        if (ID != null) {
                            DownloadLink link = createDownloadlink("http://www.mediafire.com/download.php?" + ID);
                            decryptedLinks.add(link);
                            return decryptedLinks;
                        }
                    }
                    br.followConnection();
                } finally {
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }
        }
        if (br.containsHTML("The page cannot be found")) return decryptedLinks;
        br.setFollowRedirects(true);
        Thread.sleep(500);
        String reqlink = br.getRegex("(This is a shared Folder)").getMatch(0);
        if (reqlink == null) {
            String ID = new Regex(parameter, "\\.com/\\?(.+)").getMatch(0);
            if (ID == null) ID = new Regex(parameter, "\\.com/.*?/(.*?)/").getMatch(0);
            if (ID != null) {
                DownloadLink link = createDownloadlink("http://www.mediafire.com/download.php?" + ID);
                decryptedLinks.add(link);
                return decryptedLinks;
            }
            return null;
        }
        String ID = new Regex(parameter, "\\.com/\\?(.+)").getMatch(0);
        if (ID == null) ID = new Regex(parameter, "\\.com/.*?/(.*?)/").getMatch(0);
        if (ID != null) {
            br.getPage("http://www.mediafire.com/api/folder/get_info.php?r=nuul&recursive=yes&folder_key=" + ID + "&response_format=json&version=1");
            String links[][] = br.getRegex("quickkey\":\"(.*?)\",\"filename\":\"(.*?)\".*?\"size\":\"(\\d+)").getMatches();
            progress.setRange(links.length);

            for (String[] element : links) {
                if (!element[2].equalsIgnoreCase("0")) {
                    DownloadLink link = createDownloadlink("http://www.mediafire.com/download.php?" + element[0]);
                    link.setName(unescape(element[1]));
                    link.setDownloadSize(Long.parseLong(element[2]));
                    link.setProperty("origin", "decrypter");
                    link.setAvailable(true);
                    decryptedLinks.add(link);
                }
                progress.increase(1);
            }
        }
        return decryptedLinks;
    }

    private static synchronized String unescape(final String s) {
        /* we have to make sure the youtube plugin is loaded */
        if (pluginloaded == false) {
            final PluginForHost plugin = JDUtilities.getPluginForHost("youtube.com");
            if (plugin == null) throw new IllegalStateException("youtube plugin not found!");
            pluginloaded = true;
        }
        return jd.plugins.hoster.Youtube.unescape(s);
    }
}

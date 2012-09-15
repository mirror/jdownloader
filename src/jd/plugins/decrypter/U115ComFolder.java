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
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "u.115.com" }, urls = { "http://(www\\.)?(u\\.)?115\\.com/folder/[a-z0-9]{1,11}" }, flags = { 0 })
public class U115ComFolder extends PluginForDecrypt {

    // DEV NOTES
    // other: haven't tested for over 1000 links / folder as I haven't been able
    // to find one to play with!

    public U115ComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String PASSCODETEXT = ">文件发布者设置了访问权限，您需要输入访问密码才可进入下载页";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        parameter = parameter.replace("u.115.com/", "115.com/");
        br.setReadTimeout(2 * 60 * 1000);
        br.setCookie("http://115.com/", "lang", "en");
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(false);
        br.setCookiesExclusive(true);
        br.getPage(parameter);
        if (br.containsHTML("(>文件夹提取码不存在<|>文件拥有者未分享该文件夹。<)")) {
            logger.warning("Invalid URL: " + parameter);
            return decryptedLinks;
        }
        if (br.containsHTML(PASSCODETEXT)) {
            for (int i = 0; i <= 3; i++) {
                final String passCode = getUserInput("Enter password for: " + parameter, param);
                br.postPage(parameter, "pass=" + Encoding.urlEncode(passCode));
                if (br.containsHTML(PASSCODETEXT)) continue;
                break;
            }
            if (br.containsHTML(PASSCODETEXT)) throw new Exception(DecrypterException.PASSWORD);
        }
        // Set package name and prevent null field from creating plugin errors
        String fpName = br.getRegex("<i class=\"file\\-type tp\\-folder\"></i><span class=\"file\\-name\">(.*?)</span>").getMatch(0);
        if (fpName == null) fpName = br.getRegex("desc:\\'分享好资源\\|   (.*?) http://").getMatch(0);
        if (fpName == null) fpName = "Untitled";

        // API STUFF
        String API = br.getRegex("(var UAPI = \\{[^\\}]+)").getMatch(0);
        if (API == null) {
            logger.warning("Can't find UAPI: " + parameter);
            return null;
        }
        String aid = new Regex(API, "aid: Number\\(\\'(\\d+)").getMatch(0);
        String cid = new Regex(API, "cid: Number\\(\\'(\\d+)").getMatch(0);
        String uid = new Regex(API, "user_id: Number\\(\\'(\\d+)").getMatch(0);
        if (aid == null || cid == null || uid == null) {
            logger.warning("Can't find values within 'API' " + parameter);
            return null;
        }
        br.getHeaders().put("Referer", "http://web.api.115.com/bridge.html?namespace=UAPI&api=DataAPI");
        br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
        br.getPage("http://web.api.115.com/folder/file?aid=" + aid + "&cid=" + cid + "&user_id=" + uid + "&offset=0&limit=1000&_t=" + Integer.toString(new Random().nextInt(100000000)));

        // find and process each entry.
        String[] results = br.getRegex("(\\{\"file_name\":[^\\}]+)").getColumn(0);

        if (results != null && results.length != 0) {
            for (String result : results) {
                String[][] formatThis = new Regex(result, "\"file_name\":\"([^\"]+).+?\"file_size\":\"([^\"]+)\",\"file_status\":\"([^\"]+)\".+?\"pick_code\":\"([^\"]+)\"").getMatches();
                if (formatThis == null || formatThis.length == 0) {
                    logger.warning("Problem with 'formatThis' " + parameter);
                    return null;
                }
                DownloadLink dl = createDownloadlink(new Regex(param, "(https?)://").getMatch(0) + "://115.com/file/" + formatThis[0][3]);
                dl.setName(unescape(formatThis[0][0].trim()));
                dl.setDownloadSize(SizeFormatter.getSize(formatThis[0][1]));
                if (formatThis[0][2].equals("1"))
                    dl.setAvailable(true);
                else
                    dl.setAvailable(false);
                decryptedLinks.add(dl);
            }
        } else {
            logger.warning("Problem with 'results' " + parameter);
            return null;
        }

        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName);
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private static synchronized String unescape(final String s) {
        /* we have to make sure the youtube plugin is loaded */

        final PluginForHost plugin = JDUtilities.getPluginForHost("youtube.com");
        if (plugin == null) throw new IllegalStateException("youtube plugin not found!");

        return jd.plugins.hoster.Youtube.unescape(s);
    }
}
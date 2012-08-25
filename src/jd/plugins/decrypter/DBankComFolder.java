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
import java.util.Map.Entry;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vmall.com" }, urls = { "http://(www\\.)?dl\\.(dbank|vmall)\\.com/[a-z0-9]+" }, flags = { 0 })
public class DBankComFolder extends PluginForDecrypt {

    public DBankComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.setCustomCharset("utf-8");
        String parameter = param.toString().replace("dbank.com/", "vmall.com/");
        br.getPage(parameter);
        if (br.getURL().contains("vmall.com/linknotexist.html") || br.containsHTML("(>抱歉，此外链不存在。|1、你输入的地址错误；<br/>|2、外链中含非法内容；<br />|3、创建外链的文件还没有上传到服务器，请稍后再试。<br /><br />)")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        /* Password protected links */
        String passCode = null;
        if (br.getURL().contains("/m_accessPassword.html")) {
            String id = new Regex(br.getURL(), "id=(\\w+)$").getMatch(0);
            id = id == null ? parameter.substring(parameter.lastIndexOf("/") + 1) : id;

            for (int i = 0; i < 3; i++) {
                passCode = Plugin.getUserInput(null, param);
                br.postPage("http://dl.vmall.com/app/encry_resource.php", "id=" + id + "&context=%7B%22pwd%22%3A%22" + passCode + "%22%7D&action=verify");
                if (br.getRegex("\"retcode\":\"0000\"").matches()) {
                    break;
                }
            }
            if (!br.getRegex("\"retcode\":\"0000\"").matches()) { throw new DecrypterException(DecrypterException.PASSWORD); }
            br.getPage(parameter);
        }

        String fpName = br.getRegex("<h1  id=\"link_title\">([^<>\"]*?)</h1>").getMatch(0);
        if (fpName == null) fpName = br.getRegex("<h2[ ]*id=('|\")link_title('|\") title=('|\")(.*?)('|\")").getMatch(3);
        if (fpName == null) fpName = parameter;

        String globalLinkData = br.getRegex("var globallinkdata = \\{.*?\"resource\":\\{(.*?)\\}\\;").getMatch(0);
        if (globalLinkData == null) br.getRegex("var globallinkdata = \\{(.*?)\\}\\;").getMatch(0);
        String links = new Regex(globalLinkData, "\"files\":\\[(.*?)\\]\\}").getMatch(0);

        if (links == null) return null;

        HashMap<String, String> linkParameter = new HashMap<String, String>();

        for (String[] all : new Regex(links, "\\{(.*?)\\}").getMatches()) {
            if (passCode != null) linkParameter.put("password", passCode);
            for (String[] single : new Regex(all[0].replaceAll("\\\\/", "/"), "\"([^\",]+)\":\"?([^\"?,]+)").getMatches()) {
                linkParameter.put(single[0], single[1]);
            }
            DownloadLink dl = createDownloadlink("http://vmalldecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(10000));
            for (final Entry<String, String> next : linkParameter.entrySet()) {
                dl.setProperty(next.getKey(), next.getValue());
            }
            dl.setProperty("mainlink", parameter);
            try {
                dl.setDownloadSize(SizeFormatter.getSize(dl.getStringProperty("size") + "b"));
            } catch (Throwable e) {
            }
            dl.setName(Encoding.htmlDecode(decodeUnicode(dl.getStringProperty("name", "UnknownTitle" + System.currentTimeMillis()))));
            dl.setAvailable(true);
            try {
                distribute(dl);
            } catch (final Throwable e) {
                /* does not exist in 09581 */
            }
            decryptedLinks.add(dl);
            linkParameter.clear();
        }

        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private String decodeUnicode(final String s) {
        final Pattern p = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
        String res = s;
        final Matcher m = p.matcher(res);
        while (m.find()) {
            res = res.replaceAll("\\" + m.group(0), Character.toString((char) Integer.parseInt(m.group(1), 16)));
        }
        return res;
    }

}
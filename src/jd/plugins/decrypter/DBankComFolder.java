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
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dbank.com" }, urls = { "http://(www\\.)?dl\\.dbank\\.com/[a-z0-9]+" }, flags = { 0 })
public class DBankComFolder extends PluginForDecrypt {

    public DBankComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.setCustomCharset("utf-8");
        String parameter = param.toString();
        br.getPage(parameter);
        if (br.getURL().contains("dbank.com/linknotexist.html") || br.containsHTML("(>抱歉，此外链不存在。|1、你输入的地址错误；<br/>|2、外链中含非法内容；<br />|3、创建外链的文件还没有上传到服务器，请稍后再试。<br /><br />)")) return decryptedLinks;
        String fpName = br.getRegex("<title>(.*?)\\–华为网盘\\|资源共享-文件备份\\-免费网络硬盘</title>").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("name=\"title\" id=\"titleV\" value=\"(.*?)\"").getMatch(0);
            if (fpName == null) {
                fpName = br.getRegex("<h2 id=\"title\" class=\"tit\">(.*?)</h2>").getMatch(0);
            }
        }
        boolean details = true;
        String[][] links = br.getRegex("\"(http://(www\\.)?dbank\\.com/download/([^/<>]+)\\?f=[a-z0-9]+\\&i=\\d+\\&h=\\d+\\&v=[a-z0-9]+)(\\&ip=[0-9\\.]+)?\" class=\"gbtn btn\\-xz\">下载</a>[\t\n\r ]+<em title=\"[^<>\"/]+\" name=\"delete_Icon\" onclick=\"deleteLinkFile\\(this\\);\" class=\"gbtn btn\\-sc\">删除</em>[\t\n\r ]+</span>[\t\n\r ]+<strong>([^<>\"]*?)</strong>").getMatches();
        if (links == null || links.length == 0) {
            details = false;
            links = br.getRegex("\"(http://(www\\.)?dbank\\.com/download/[^/<>]+\\?f=[a-z0-9]+\\&i=\\d+\\&h=\\d+\\&v=[a-z0-9]+)(\\&ip=[0-9\\.]+)?\"").getMatches();
        }
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        if (details) {
            for (String singleLink[] : links) {
                DownloadLink dl = createDownloadlink(singleLink[0]);
                dl.setDownloadSize(SizeFormatter.getSize(singleLink[4] + "b"));
                dl.setFinalFileName(Encoding.htmlDecode(singleLink[2].trim()));
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
        } else {
            for (String singleLink[] : links) {
                decryptedLinks.add(createDownloadlink(singleLink[0]));
            }
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}

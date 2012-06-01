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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision: 16817 $", interfaceVersion = 2, names = { "dbank.com" }, urls = { "http://(www\\.)?dl\\.dbank\\.com/[a-z0-9]+" }, flags = { 0 })
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
        if (br.getURL().contains("dbank.com/linknotexist.html") || br.containsHTML("(>抱歉，此外链不存在。|1、你输入的地址错误；<br/>|2、外链中含非法内容；<br />|3、创建外链的文件还没有上传到服务器，请稍后再试。<br /><br />)")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        String fpName = br.getRegex("\"title\":\"([^<>\"]*?)\"").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("<h1  id=\"link_title\">([^<>\"]*?)</h1>").getMatch(0);
        }
        final String[][] linkInformation = br.getRegex("\"name\":\"([^<>\"]*?)\".*?\"size\":(\\d+).*?\"downloadurl\":\"([^<>\"]*?)\"").getMatches();
        if (linkInformation != null && linkInformation.length != 0) {
            for (final String[] linkInfo : linkInformation) {
                final String finalfilename = Encoding.htmlDecode(linkInfo[0].trim());
                DownloadLink dl = createDownloadlink("http://dbankdecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(10000));
                dl.setDownloadSize(SizeFormatter.getSize(linkInfo[1] + "b"));
                dl.setFinalFileName(finalfilename);
                dl.setAvailable(true);
                dl.setProperty("premiumonly", true);
                dl.setProperty("plainfilename", linkInfo[0]);
                dl.setProperty("mainlink", parameter);
                decryptedLinks.add(dl);
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

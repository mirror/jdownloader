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
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision: 16817 $", interfaceVersion = 2, names = { "pan.baidu.com" }, urls = { "http://(www\\.)?pan\\.baidu\\.com/netdisk/(extractpublic\\?username=\\d+|singlepublic\\?fid=\\d+_\\d+)" }, flags = { 0 })
public class PanBaiduCom extends PluginForDecrypt {

    public PanBaiduCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        final String correctedBR = br.toString().replace("\\", "");
        final String[][] linkInfo = new Regex(correctedBR, "\"server_filename\":\"([^<>\"]*?)\",\"s3_handle\":\"(http://[^<>\"]*?)\",\"size\":(\\d+)").getMatches();
        if (linkInfo == null || linkInfo.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (String[] sglnkInfo : linkInfo) {
            final String finalfilename = Encoding.htmlDecode(sglnkInfo[0]);
            final DownloadLink dl = createDownloadlink("http://pan.baidudecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(10000));
            dl.setFinalFileName(finalfilename);
            dl.setProperty("plainfilename", sglnkInfo[0]);
            dl.setProperty("mainlink", parameter);
            dl.setDownloadSize(SizeFormatter.getSize(sglnkInfo[2] + "b"));
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }
        FilePackage fp = FilePackage.getInstance();
        fp.setName("Links: " + new Regex(parameter, "([0-9_]+)$").getMatch(0));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }
}

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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "xunlei.com" }, urls = { "http://(www\\.)?kuai\\.xunlei\\.com/d/[A-Z]{12}" }, flags = { 0 })
public class XunleiComDecrypter extends PluginForDecrypt {

    public XunleiComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        ArrayList<String> removeDouble = new ArrayList<String>();
        String parameter = param.toString();
        br.setCustomCharset("utf-8");
        br.getPage(parameter);
        final String fpName = br.getRegex("<span style=\"color:gray\">下载 (.*?) 分享的文件</span>").getMatch(0);
        final String[] links = br.getRegex("\"(http://dl\\d+\\.[a-z]\\d+\\.sendfile\\.vip\\.xunlei\\.com:\\d+/[^/<>\"]+\\?key=[a-z0-9]+\\&file_url=[^/<>\"]+\\&file_type=\\d+\\&authkey=[A-Z0-9]+\\&exp_time=\\d+&from_uid=\\d+\\&task_id=\\d+\\&get_uid=\\d+)").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (String aLink : links)
            if (!removeDouble.contains(aLink)) removeDouble.add(aLink);
        for (String singleLink : removeDouble)
            decryptedLinks.add(createDownloadlink(singleLink));
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}

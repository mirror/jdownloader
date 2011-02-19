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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "multi.file.am" }, urls = { "http://(www\\.)?(url|multi)\\.file\\.am/(file/[A-Z0-9]+|\\?[A-Za-z0-9]+|\\d+)" }, flags = { 0 })
public class MultiFileAm extends PluginForDecrypt {

    public MultiFileAm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        if (parameter.contains("multi.file.am")) {
            logger.info("Decrypting multi.file.am links...");
            br.getPage(parameter);
            if (br.containsHTML(">This file does not exist<")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
            String[] redirectLinks = br.getRegex("width=\"140\"><a target=\"_blank\" href=\"(/.*?)\"").getColumn(0);
            if (redirectLinks == null || redirectLinks.length == 0) redirectLinks = br.getRegex("width=\"70\" align=\"right\"><a target=\"_blank\" href=\"(/.*?))\"").getColumn(0);
            if (redirectLinks == null || redirectLinks.length == 0) return null;
            progress.setRange(redirectLinks.length);
            for (String singleRedirectLink : redirectLinks) {
                br.getPage("http://multi.file.am" + singleRedirectLink);
                String finallink = br.getRedirectLocation();
                if (finallink == null) return null;
                decryptedLinks.add(createDownloadlink(finallink));
                progress.increase(1);
            }
        } else if (parameter.contains("url.file.am")) {
            logger.info("Decrypting a url.file.am link...");
            br.getPage(parameter);
            String finallink = br.getRegex("width=\"240\" height=\"3\"><br><a href=\\'(http.*?)\\'").getMatch(0);
            if (finallink == null) {
                finallink = br.getRegex("HTTP-EQUIV=\"refresh\" CONTENT=\"\\d+;url=\\'(http.*?)\\'\">").getMatch(0);
                if (finallink == null) {
                    finallink = br.getRegex("\\'(http://\\d+\\.file\\.am/\\d+)\\'").getMatch(0);
                }
            }
            if (finallink == null) return null;
            if (finallink.contains("?")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
            decryptedLinks.add(createDownloadlink(finallink));
        }

        return decryptedLinks;
    }

}

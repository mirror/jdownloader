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
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "embedupload.com" }, urls = { "http://[\\w\\.]*?embedupload\\.com/\\?([A-Z0-9]{2}|d)=[A-Z0-9]+" }, flags = { 0 })
public class EmbedUploadCom extends PluginForDecrypt {

    public EmbedUploadCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        if (parameter.contains("embedupload.com/?d=")) {
            String[] redirectLinks = br.getRegex("style=\"padding-left:43px;padding-right:20px;padding-bottom:20px;font-size:17px;font-style:italic\" >[\t\r\n ]+<a href=\"(http://.*?)\"").getColumn(0);
            if (redirectLinks == null || redirectLinks.length == 0) redirectLinks = br.getRegex("\"(http://(www\\.)?embedupload\\.com/\\?[A-Z0-9]{2}=[A-Z0-9]+)\"").getColumn(0);
            if (redirectLinks == null || redirectLinks.length == 0) return null;
            progress.setRange(redirectLinks.length);
            for (String singleLink : redirectLinks) {
                if (!singleLink.contains("&urlkey=")) {
                    br.getPage(singleLink);
                    if (br.getRedirectLocation() == null) return null;
                    decryptedLinks.add(createDownloadlink(br.getRedirectLocation()));
                }
                progress.increase(1);
            }
        } else {
            if (br.getRedirectLocation() == null) return null;
            decryptedLinks.add(createDownloadlink(br.getRedirectLocation()));
        }

        return decryptedLinks;
    }

}

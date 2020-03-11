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

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "javnew.net" }, urls = { "https?://(?:www\\d*\\.)?(javnew|av-th)\\.net/[^/\\s]+/?" })
public class JavNew extends antiDDoSForDecrypt {
    public JavNew(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        String filename = br.getRegex("<title>(?:Watch\\s+)?([^<]+)\\s+\\|[\\s\\w]+</title>").getMatch(0);
        logger.info("filename: " + filename);
        if (filename != null) {
            filename = Encoding.htmlDecode(filename.trim());
        }
        String link = br.getRegex("<iframe[^<>]*?src=\"([^\"]+)\"[^<>]+?allowfullscreen").getMatch(0);
        if (link != null) {
            DownloadLink dl = createDownloadlink(Encoding.htmlDecode(link));
            dl.setFinalFileName(filename); // .setName(filename);
            logger.info("getFinalFileName(): " + dl.getFinalFileName());
            logger.info("dl.getName(): " + dl.getName());
            dl.setProperty("filename", filename);
            decryptedLinks.add(dl);
            logger.info("Decrypter output: " + dl);
            final String file_name = dl.getStringProperty("filename", null);
            logger.info("file_name: " + file_name);
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return decryptedLinks;
    }
}
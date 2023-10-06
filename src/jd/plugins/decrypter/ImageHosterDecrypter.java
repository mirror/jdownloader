//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "imagecurl.com", "girlswithmuscle.com" }, urls = { "http://(?:www\\.)?imagecurl\\.com/viewer\\.php\\?file=[\\w-]+\\.[a-z]{2,4}", "https?://(www.)?girlswithmuscle\\.com/\\d+/?" })
public class ImageHosterDecrypter extends antiDDoSForDecrypt {
    public ImageHosterDecrypter(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        String parameter = param.getCryptedUrl();
        br.setFollowRedirects(false);
        String finallink = null;
        String finalfilename = null;
        if (parameter.contains("imagecurl.com/")) {
            br.setFollowRedirects(true);
            br.getPage(parameter);
            finallink = br.getRegex("\\('<br/><a href=\"(https?://cdn\\.imagecurl\\.com/images/\\w+\\.[a-z]{2,4})\">").getMatch(0);
            if (finallink == null) {
                finallink = br.getRegex("To view its <a href=\"(https?://cdn\\.imagecurl\\.com/images/\\w+\\.[a-z]{2,4})\">true size<").getMatch(0) + finallink;
            }
        } else if (parameter.contains("girlswithmuscle.com/")) {
            String fuid = new Regex(parameter, "([\\d+]+)/?$").getMatch(0);
            if (!parameter.endsWith("/")) {
                parameter = parameter + "/";
            }
            br.setFollowRedirects(true);
            br.getPage(parameter);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            finallink = br.getRegex("<a href=\"([^\"]+)\">Link to full-size").getMatch(0);
            String ext = new Regex(finallink, "(\\.[a-z0-9]+$)").getMatch(0);
            finalfilename = fuid + " " + br.getRegex("<title>([^<>]+)</title>").getMatch(0) + ext;
        } else {
            /* Unsupported URL -> This should never happen. */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (finallink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        finallink = Encoding.htmlDecode(DirectHTTP.createURLForThisPlugin(finallink));
        final DownloadLink dl = createDownloadlink(finallink);
        if (finalfilename != null) {
            dl.setFinalFileName(Encoding.htmlDecode(finalfilename));
        }
        ret.add(dl);
        return ret;
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}
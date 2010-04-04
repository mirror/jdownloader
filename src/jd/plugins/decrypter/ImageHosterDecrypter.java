//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "imagebam.com", "photobucket.com", "freeimagehosting.net", "pixhost.org" }, urls = { "http://[\\w\\.]*?imagebam\\.com/image/[a-z0-9]+", "http://[\\w\\.]*?media\\.photobucket.com/image/.+\\..{3,4}\\?o=[0-9]+", "http://[\\w\\.]*?freeimagehosting\\.net/image\\.php\\?.*?\\..{3,4}", "http://[\\w\\.]*?pixhost\\.org/show/[0-9]+/.*?\\.{3,4}" }, flags = { 0, 0, 0, 0 })
public class ImageHosterDecrypter extends PluginForDecrypt {

    public ImageHosterDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        if (parameter.contains("download.su")) br.setCustomCharset("windows-1251");
        br.setFollowRedirects(false);
        br.getPage(parameter);
        String finallink = null;
        String filename = null;
        if (parameter.contains("imagebam.com")) {
            /* Error handling */
            if (br.containsHTML("Image not found")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
            finallink = br.getRegex("'(http://[0-9]+\\.imagebam\\.com/dl\\.php\\?ID=.*?)'").getMatch(0);
        } else if (parameter.contains("media.photobucket.com")) {
            finallink = br.getRegex("mediaUrl':'(http.*?)'").getMatch(0);
        } else if (parameter.contains("freeimagehosting.net")) {
            /* Error handling */
            if (!br.containsHTML("uploads/")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
            finallink = parameter.replace("image.php?", "uploads/");
        } else if (parameter.contains("pixhost.org")) {
            /* Error handling */
            if (!br.containsHTML("images/")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
            finallink = br.getRegex("show_image\" src=\"(http.*?)\"").getMatch(0);
            if (finallink == null) finallink = br.getRegex("\"(http://img[0-9]+\\.pixhost\\.org/images/[0-9]+/.*?)\"").getMatch(0);
        }
        if (finallink == null) return null;
        finallink = "directhttp://" + finallink;
        DownloadLink dl = createDownloadlink(finallink);
        if (filename != null) dl.setFinalFileName(filename);
        decryptedLinks.add(dl);
        return decryptedLinks;
    }

}

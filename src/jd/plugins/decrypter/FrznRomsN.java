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

import java.awt.Point;
import java.io.File;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "frozen-roms.in" }, urls = { "http://[\\w\\.]*?frozen-roms\\.in/download/\\d+/(wii|nds|gba|gbc|nes|n64|snes)/.{1}" }, flags = { 0 })
public class FrznRomsN extends PluginForDecrypt {

    public FrznRomsN(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String CAPTCHATEXT = "go\\.php";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        String fpName = br.getRegex("<br /> <b>(.*?)</b> <br /><br />").getMatch(0);
        if (fpName == null) fpName = br.getRegex("\"http://frozen-roms\\.in/captcha/\\d+/\\d+/(.*?)/").getMatch(0);
        String continueLink = br.getRegex("\"(http://frozen-roms\\.in/captcha/\\d+/\\d+/.*?)\"").getMatch(0);
        if (continueLink == null) continueLink = br.getRegex("</p><p align=\"center\"><b><a href=\"(http://.*?)\">Download<").getMatch(0);
        br.getPage(continueLink);
        if (!br.containsHTML(CAPTCHATEXT)) return null;
        boolean failed = true;
        String downPage = null;
        for (int i = 0; i <= 3; i++) {
            File file = this.getLocalCaptchaFile();
            br.cloneBrowser().getDownload(file, "http://frozen-roms.in/captcha/go.php");
            Point p = UserIO.getInstance().requestClickPositionDialog(file, this.getHost(), JDL.L("plugins.decrypter.frznromsn.captchadescription", "Please click on the open circle!"));
            String postData = "button.x=" + p.x + "&button.y=" + p.y + "&button=Send";
            br.postPage(continueLink, postData);
            downPage = br.getRegex("<p align=\"center\"><b><a href=\"(http://.*?)\"").getMatch(0);
            if (downPage == null) downPage = br.getRegex("\"(http://frozen-roms\\.in/down-.*?\\.html)\"").getMatch(0);
            if (downPage == null) {
                logger.info("downPage is null...");
                if (br.containsHTML(CAPTCHATEXT)) continue;
                return null;
            }
            failed = false;
            break;
        }
        if (failed) throw new DecrypterException(DecrypterException.CAPTCHA);
        br.getPage(downPage);
        if (br.containsHTML("http-equiv=\"refresh\" content=\"0 url=http://frozen-roms\\.in/\"")) throw new DecrypterException("Server error...");
        String goFrame = br.getRegex("\"(http://frozen-roms\\.in/go-.*?\\.html)\"").getMatch(0);
        if (goFrame == null) {
            logger.warning("goLink is null...");
            return null;
        }
        br.getPage(goFrame);
        String goFrameTwo = br.getRegex("\"(http://frozen-roms\\.in/frame-.*?\\.html)\"").getMatch(0);
        if (goFrameTwo == null) {
            logger.warning("goFrameTwo is null...");
            return null;
        }
        br.getPage(goFrameTwo);
        String finallink = br.getRegex("<iframe src=\"(.*?)\"").getMatch(0);
        if (finallink == null) {
            logger.warning("finallink is null...");
            return null;
        }
        decryptedLinks.add(createDownloadlink(finallink));
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}

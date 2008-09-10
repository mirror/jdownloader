//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.plugins.decrypt;

import java.awt.Point;
import java.io.File;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.ClickPositionDialog;
import jd.http.Browser;
import jd.http.Encoding;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class Stealth extends PluginForDecrypt {
    static private final String host = "Stealth.to";

    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?stealth\\.to/(\\?id\\=[a-zA-Z0-9]+|index\\.php\\?id\\=[a-zA-Z0-9]+|\\?go\\=captcha&id=[a-zA-Z0-9]+)", Pattern.CASE_INSENSITIVE);

    public Stealth(String cfgName){
        super(cfgName);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception, DecrypterException {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        String id = new Regex(parameter, Pattern.compile("\\?id\\=([a-zA-Z0-9]+)")).getMatch(0);
        if (id != null) {
            File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".dlc");
           Browser.download(container, br.openGetConnection("http://stealth.to/?go=dlc&id=" + id));
                Vector<DownloadLink> dl_links = (JDUtilities.getController().getContainerLinks(container));
                for (DownloadLink dl_link : dl_links) {
                    decryptedLinks.add(dl_link);
                }
                container.delete();
            
        }

        if (decryptedLinks.size() == 0) {
            br.getPage(parameter);
            boolean valid = true;
            for (int i = 0; i < 5; ++i) {
                if (br.containsHTML("captcha_img.php")) {
                    valid = false;
                    /* alte Captcha Seite */
                    String sessid = new Regex(br.getRequest().getCookieString(), "PHPSESSID=([a-zA-Z0-9]*)").getMatch(0);
                    if (sessid == null) {
                        logger.severe("Error sessionid: " + br.getRequest().getCookieString());
                        return null;
                    }
                    logger.finest("Captcha Protected");
                    String captchaAdress = "http://stealth.to/captcha_img.php?PHPSESSID=" + sessid;
                    File file = this.getLocalCaptchaFile(this);
                    Form form = br.getForm(0);
                    Browser.download(file, br.cloneBrowser().openGetConnection(captchaAdress));
                    form.put("txtCode", getCaptchaCode(file, this));
                    br.submitForm(form);
                } else if (br.containsHTML("libs/captcha.php")) {
                    /* Neue Captcha Seite */
                    valid = false;
                    logger.finest("Captcha Protected");
                    File file = this.getLocalCaptchaFile(this);
                    Form form = br.getForm(0);
                    Browser.download(file, br.cloneBrowser().openGetConnection("http://stealth.to/libs/captcha.php"));
                    form.put("code", getCaptchaCode(file, this));
                    br.submitForm(form);
                } else if (br.containsHTML("libs/crosshair.php")) {
                    /* Neue Crosshair Seite */
                    valid = false;
                    logger.finest("Captcha Protected");
                    File file = this.getLocalCaptchaFile(this);
                    Form form = br.getForm(0);
                    Browser.download(file, br.cloneBrowser().openGetConnection("http://stealth.to/libs/crosshair.php"));
                    ClickPositionDialog d = new ClickPositionDialog(SimpleGUI.CURRENTGUI.getFrame(), file, "Captcha", JDLocale.L("plugins.decrypt.stealthto.captcha", "Please click on the Circle with a gap"), 20, null);
                    Point p = d.result;
                    form.put("button.x", p.x + "");
                    form.put("button.y", p.y + "");
                    br.submitForm(form);

                } else {
                    valid = true;
                    break;
                }
            }
            if (valid == false) throw new DecrypterException("Wrong Captcha Code");
            /* Alte Links Seite */
            String[] links = br.getRegex("popup.php\\?id=(\\d+?)\"\\,'dl'").getColumn(0);
            if (links.length > 0) {
                progress.setRange(links.length);
                for (String element : links) {
                    decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(new Regex(br.cloneBrowser().getPage("http://stealth.to/popup.php?id=" + element), Pattern.compile("frame src=\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0))));
                    progress.increase(1);
                }
            }
            /* Neue Links Seite */
            String[][] links2 = br.getRegex("download\\('(\\d+)', '(\\d+)'\\);\"></td>").getMatches();
            if (links2.length > 0) {
                for (String[] element : links2) {
                    br.getPage("http://stealth.to/index.php?go=download&id=" + element[0]);
                    String link = br.getRegex("(.*?)\\|\\|\\|").getMatch(0);
                    decryptedLinks.add(createDownloadlink("http://" + link));
                }
            }
        }
        return decryptedLinks;
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

   
    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }
}

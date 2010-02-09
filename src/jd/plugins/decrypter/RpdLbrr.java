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
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.parser.html.InputField;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rapidlibrary.com" }, urls = { "http://rapidlibrary\\.com/download_file_i\\.php\\?.+" }, flags = { 0 })
public class RpdLbrr extends PluginForDecrypt {

    private static boolean decryptRunning = false;

    public RpdLbrr(PluginWrapper wrapper) {
        super(wrapper);
        br.setCookiesExclusive(false);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

        /**
         * only the first link shows a captcha. so we wait and queue paralell
         * requests
         */
        if (RpdLbrr.decryptRunning) progress.setStatusText("Queued");
        waitQueue();

        br.getPage(parameter.getCryptedUrl());
        String fpName = br.getRegex("<<title>File download:(.*?)from .*?</title>").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("<font class=\"texta\">(.*?)</font>").getMatch(0);
            if (fpName == null) {
                fpName = br.getRegex("<span style=\"font-size: 16px; color:#0374f1;\">.*?<b>(.*?)</b>").getMatch(0);
            }
        }
        String pagepiece = br.getRegex("<span style=\"font-size:12px;color:#000000;\">(.*?)<hr width=100% noshade size=\"0\" color=").getMatch(0);
        if (pagepiece == null) pagepiece = br.getRegex("class=\"parts_div_one\"(.*?)</div><br>").getMatch(0);
        if (pagepiece == null) {
            progress.setRange(2);
            progress.setStatus(1);
            for (int i = 0; i <= 7; i++) {
                Form captchaForm = br.getForms()[1];
                if (captchaForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                captchaForm.setAction(br.getURL());
                String captchaCode = getCaptchaCode("http://rapidlibrary.com/code2.php", parameter);
                InputField nv = new InputField("c_code", captchaCode);
                captchaForm.addInputField(nv);
                br.submitForm(captchaForm);
                if (br.containsHTML("code2.php")) continue;
                break;
            }
            if (br.containsHTML("code2.php")) throw new DecrypterException(DecrypterException.CAPTCHA);
            pagepiece = br.getRegex("<span style=\"font-size:12px;color:#000000;\">(.*?)<hr width=100% noshade size=\"0\" color=").getMatch(0);
            if (pagepiece == null) pagepiece = br.getRegex("class=\"parts_div_one\"(.*?)</div><br>").getMatch(0);
            if (pagepiece == null) return null;
            String[] links = HTMLParser.getHttpLinks(pagepiece, "");
            if (links == null || links.length == 0) return null;
            for (String finallink : links) {
                decryptedLinks.add(createDownloadlink(finallink));
            }
        }
        if (fpName != null && decryptedLinks.size() > 1) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        RpdLbrr.decryptRunning = false;
        return decryptedLinks;
    }

    private void waitQueue() throws InterruptedException {
        while (RpdLbrr.decryptRunning)
            Thread.sleep(1000);
        RpdLbrr.decryptRunning = true;
    }
}

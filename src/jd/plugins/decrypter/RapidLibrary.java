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
import jd.parser.html.InputField;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

public class RapidLibrary extends PluginForDecrypt {

    private static boolean decryptRunning = false;

    public RapidLibrary(PluginWrapper wrapper) {
        super(wrapper);
        br.setCookiesExclusive(false);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

        /**
         * only the first link shows a captcha. so we wait and queue paralell
         * requests
         */
        if (RapidLibrary.decryptRunning) progress.setStatusText("Queued");
        waitQueue();

        br.getPage(parameter.getCryptedUrl());
        String directLink = br.getRegex("\"(http://rapidshare.com.*?)\"").getMatch(0);
        if (directLink == null) {
            progress.setRange(2);
            progress.setStatus(1);
            Form captchaForm = br.getForms()[1];
            if (captchaForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            captchaForm.setAction(br.getURL());
            String captchaCode = getCaptchaCode("http://rapidlibrary.com/code2.php", parameter);
            InputField nv = new InputField("c_code", captchaCode);
            captchaForm.addInputField(nv);
            br.submitForm(captchaForm);
            directLink = br.getRegex("\"(http://rapidshare\\.com.*?)\"").getMatch(0);
            if (directLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        }
        decryptedLinks.add(createDownloadlink(directLink));
        RapidLibrary.decryptRunning = false;
        return decryptedLinks;
    }

    // @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    private void waitQueue() throws InterruptedException {
        while (RapidLibrary.decryptRunning)
            Thread.sleep(1000);
        RapidLibrary.decryptRunning = true;
    }
}

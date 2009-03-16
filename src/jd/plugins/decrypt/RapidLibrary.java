/* http://rapidlibrary.com/download_file_i.php?*
 * 
 * Testcases: 
 * http://rapidlibrary.com/download_file_i.php?qq=scsi&file=173062&desc=SCSI-9+-+Vega+EP-+DNR008+-WEB-2007-HQEM+.rar
 * http://rapidlibrary.com/download_file_i.php?qq=scsi&file=96439&desc=Scsi-9+-+Railway+Sessions++K2-018+-WEB-2006-TR+.zip
 * 
 * JDInit.java
 * new DecryptPluginWrapper("rapidlibrary.com", "RapidLibrary", "http://rapidlibrary\\.com/download_file_i\\.php\\?.+");
 */

package jd.plugins.decrypt;

import java.io.File;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
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

    @Override
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
            Browser capRead = br.cloneBrowser();
            File captchaFile = capRead.getDownloadTemp("http://rapidlibrary.com/code2.php");
            String captchaCode = getCaptchaCode(captchaFile, this, parameter);
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

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    private void waitQueue() throws InterruptedException {
        while (RapidLibrary.decryptRunning)
            Thread.sleep(1000);
        RapidLibrary.decryptRunning = true;
    }
}

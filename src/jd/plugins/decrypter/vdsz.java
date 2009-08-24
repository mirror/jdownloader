//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import java.io.File;
import java.util.ArrayList;
import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;
import jd.captcha.easy.load.LoadImage;

@DecrypterPlugin(revision = "$Revision: 7794 $", interfaceVersion = 2, names = { "videosz.in" }, urls = { "http://[\\w\\.]*?videosz\\.in/screen\\.php\\?load=.*" }, flags = { 0 })
public class vdsz extends PluginForDecrypt {

    public vdsz(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        for (int retry = 0; retry < 5; retry++) {

            LoadImage li = LoadImage.loadFile(getHost());
            li.baseUrl = param.toString();
            li.load(getHost());
            String captchaCode = getCaptchaCode("crypt-me.com.Calc", li.file, param);

            /* Calculation process */
            captchaCode = captchaCode.replaceAll("_", "-").replaceAll("=", "").replaceAll("!", "");
            if (captchaCode.contains("-")) {
                String[] values = captchaCode.split("-");
                captchaCode = Integer.toString(Integer.parseInt(values[0]) - Integer.parseInt(values[1]));
            } else if (captchaCode.contains("+")) {
                String[] values = captchaCode.split("\\+");
                captchaCode = Integer.toString(Integer.parseInt(values[0]) + Integer.parseInt(values[1]));
            }
            br = li.br;
            Form form = br.getForm(0);
            form.getInputFieldByType("text").setValue(captchaCode);
            br.submitForm(form);
            if (br.containsHTML("BROKEN PLEASE REPORT")) {
                break;
            }
        }
        if (!br.containsHTML("BROKEN PLEASE REPORT")) throw new DecrypterException(DecrypterException.CAPTCHA);
        String[] links =  br.getRegex("<br /><a href=\"(http[^\"]+)\" target=\"_blank\">[^<]+</a>").getColumn(0);
        for (String link : links) {
            decryptedLinks.add(createDownloadlink(link));
        }
        String container = "http://" + getHost() + "/" + br.getRegex("<a href=\"(dlc_php[^\"]*)\"").getMatch(0);
        if(container!=null && decryptedLinks.size() == 0)
        {
        File containerFile = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + container);
        Browser.download(containerFile, container);
        decryptedLinks.addAll(JDUtilities.getController().getContainerLinks(containerFile));
        containerFile.delete();
        }

        return decryptedLinks;
    }
}

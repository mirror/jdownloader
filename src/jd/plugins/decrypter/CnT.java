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

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
@DecrypterPlugin(revision = "$Revision: 7139 $", interfaceVersion = 2, names = { "cine.to" }, urls = { "http://[\\w\\.]*?cine\\.to/index\\.php\\?do=show_download&id=[\\w]+|http://[\\w\\.]*?cine\\.to/index\\.php\\?do=protect&id=[\\w]+|http://[\\w\\.]*?cine\\.to/pre/index\\.php\\?do=show_download&id=[\\w]+|http://[\\w\\.]*?cine\\.to/pre/index\\.php\\?do=protect&id=[\\w]+"}, flags = { 0 })


public class CnT extends PluginForDecrypt {

    private static final String patternLink_Protected = "http://[\\w\\.]*?cine\\.to/index\\.php\\?do=protect\\&id=[a-zA-Z0-9]+|http://[\\w\\.]*?cine\\.to/index\\.php\\?do=protect\\&id=[a-zA-Z0-9]+|http://[\\w\\.]*?cine\\.to/pre/index\\.php\\?do=protect\\&id=[a-zA-Z0-9]+";
    static private Integer lock = 0;

    public CnT(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        String parameter = param.toString();
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        synchronized (lock) {
            br.getPage(parameter);
            if (parameter.matches(patternLink_Protected)) {
                String code = getCaptchaCode("http://cine.to/securimage_show.php", param);
                br.postPage(param.toString(), "captcha=" + code + "&submit=Senden");
                if (br.containsHTML("Code ist falsch")) throw new DecrypterException(DecrypterException.CAPTCHA);
                String[] links = br.getRegex("window\\.open\\('(.*?)'").getColumn(0);
                progress.setRange(links.length);
                for (String element : links) {
                    DownloadLink link = createDownloadlink(element);
                    link.addSourcePluginPassword("cine.to");
                    decryptedLinks.add(link);
                    progress.increase(1);
                }
            } else {
                String[] mirrors = br.getRegex("href=\"(index\\.php\\?do=protect\\&id=[\\w]+)\"").getColumn(0);
                for (String element : mirrors) {
                    decryptedLinks.add(createDownloadlink("http://cine.to/" + element));
                }
            }
            return decryptedLinks;
        }
    }

    // @Override
    

}

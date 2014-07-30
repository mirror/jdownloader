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
import java.util.concurrent.atomic.AtomicBoolean;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "narod.ru" }, urls = { "http://(www\\.)?narod(\\.yandex)?\\.ru/disk/.+" }, flags = { 0 })
public class NarodRuDecrypter extends PluginForDecrypt {

    public NarodRuDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static Object        ctrlLock     = new Object();
    private static AtomicBoolean pluginLoaded = new AtomicBoolean(false);

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        /*
         * Correct added links because some guys are spreading narod direct links, which only causes problems! So correcting the link is the
         * best solution here.
         */
        parameter = parameter.replace("narod.yandex.ru/", "narod.ru/");
        if (parameter.contains("/start/")) {
            final String linkid = new Regex(parameter, "/start/[0-9]+\\.[0-9a-z]+\\-narod(\\.yandex)?\\.ru/([0-9]{6,15})/[0-9a-z]+/[^<>\\'\"/&=]+").getMatch(1);
            final String filename = new Regex(parameter, "/start/[0-9]+\\.[0-9a-z]+\\-narod(\\.yandex)?\\.ru/[0-9]{6,15}/[0-9a-z]+/([^<>\\'\"/&=]+)").getMatch(1);
            final String finallink = "http://narod.ru/disk/" + linkid + "/" + filename;
            parameter = finallink;
        }
        br.setFollowRedirects(false);
        String redirect = parameter;
        do {
            /* Redirects from narod.ru back to narod.ru */
            if (redirect.contains("narod.ru/") || redirect.contains("narod.yandex.ru/")) {
                br.getPage(redirect);
            } else {
                /* Redirects to external site */
                final DownloadLink dl = createDownloadlink(redirect);
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
            redirect = br.getRedirectLocation();
        } while (redirect != null);
        synchronized (ctrlLock) {
            if (!pluginLoaded.get()) {
                // load plugin!
                JDUtilities.getPluginForHost("narod.ru");
                pluginLoaded.set(true);
            }
        }
        final DownloadLink dl = createDownloadlink(parameter.replace("narod.ru/", "naroddecrypted.ru/"));
        dl.setAvailableStatus(jd.plugins.hoster.NarodRu.reqStatus(this.br, dl));
        decryptedLinks.add(dl);

        return decryptedLinks;
    }

    private void prepBR() {
        /*
         * no captcha because of http://userscripts.org/scripts/review/64343
         */
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.0; chrome://global/locale/intl.properties; rv:1.8.1.12) Gecko/2008102920  Firefox/3.0.0 YB/4.2.0");
    }

}

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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "megatv.com" }, urls = { "https?://(?:www\\.)?megatv\\.com/[^<>\"]+\\.asp\\?catid=\\d+\\&subid=\\d+\\&pubid=\\d+|https?://(?:www\\.)?megatv\\.com\\.cy/cgibin/hweb\\?\\-A=\\d+\\&\\-V=[A-Za-z0-9]+" })
public class MegatvComDecrypter extends PluginForDecrypt {

    public MegatvComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        if (parameter.matches(".+\\.asp\\?catid=\\d+\\&subid=\\d+\\&pubid=\\d+")) {
            /* Old */
            final String catid = new Regex(parameter, "catid=(\\d+)").getMatch(0);
            final String subid = new Regex(parameter, "subid=(\\d+)").getMatch(0);
            final String pubid = new Regex(parameter, "pubid=(\\d+)").getMatch(0);
            try {
                br.getPage(parameter);
                // addPrototypeElement('31371','VIDEOSTRIPES','incl/1314megatvseirescategoryajaxclassicsss_31371.asp','pageid=849&catid=24072&subid=2&pubid=31416470&catidlocal=24072&subidlocal=2'
                final Regex contentregex = br.getRegex("addPrototypeElement\\(\\'(\\d+)\\',\\'VIDEOSTRIPES\\',\\'(incl/[^<>\"]*?\\.asp)\\',\\'(pageid=\\d+[^<>\"]*?)\\'");
                final String ajax_id = contentregex.getMatch(0);
                String ajax_url = contentregex.getMatch(1);
                final String ajax_get_data_part = contentregex.getMatch(2);
                if (catid == null || subid == null || pubid == null || ajax_id == null || ajax_url == null || ajax_get_data_part == null) {
                    throw new DecrypterException("Decrypter broken");
                }
                ajax_url = "http://www.megatv.com/" + ajax_url + "?" + ajax_get_data_part + "&ajaxid=" + ajax_id + "&ajaxgroup=VIDEOSTRIPES&page1=";
                int page_num = 1;
                final int entries_per_page = 30;
                int added_links_num = 0;
                do {
                    if (this.isAbort()) {
                        logger.info("User aborted decryption");
                        break;
                    }
                    br.getPage(ajax_url + page_num);
                    final String[] video_ids = br.getRegex("reloadPrototypeElementGroups\\(\\'VIDEOPLAYER\\',\\'catid=" + catid + "\\&subid=" + subid + "\\&pubid=(\\d+)\\'").getColumn(0);
                    for (final String videoID : video_ids) {
                        final String url_real = "http://www.megatv.com/classics.asp?catid=" + catid + "&subid=" + subid + "&pubid=" + videoID;
                        final DownloadLink dl = createDownloadlink("http://www.megatvdecrypted.com/classics.asp?catid=" + catid + "&subid=" + subid + "&pubid=" + videoID);
                        dl.setLinkID(videoID);
                        dl.setContentUrl(url_real);
                        decryptedLinks.add(dl);
                    }
                    added_links_num = video_ids.length;
                    page_num++;
                } while (added_links_num >= entries_per_page && added_links_num < 10);

            } catch (final Throwable e) {
            }
            if (decryptedLinks.size() == 0) {
                /* Decrypter failed or there is only 1 video --> Add to host plugin */
                final String url_real = "http://www.megatv.com/classics.asp?catid=" + catid + "&subid=" + subid + "&pubid=" + pubid;
                final DownloadLink dl = createDownloadlink("http://www.megatvdecrypted.com/classics.asp?catid=" + catid + "&subid=" + subid + "&pubid=" + pubid);
                dl.setLinkID(pubid);
                dl.setContentUrl(url_real);
                decryptedLinks.add(dl);
            }
        } else {
            /* New 2017-01-30 */
            this.br.getPage(parameter);
            if (this.br.getHttpConnection().getResponseCode() == 404) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            String finallink = this.br.getRegex(Pattern.compile("\"(https?://media\\.livenews\\.com\\.cy/DesktopModules/IST[^<>\"]*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
            if (finallink == null) {
                return null;
            }
            /* Remove linebreaks ... */
            finallink = finallink.replace("\n", "");
            decryptedLinks.add(this.createDownloadlink(finallink));
        }

        return decryptedLinks;
    }
}

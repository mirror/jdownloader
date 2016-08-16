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

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.TimeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hamburg1.de" }, urls = { "http://(www\\.)?hamburg1\\.de/[^<>\"]+/\\d+/[^<>\"]+\\.html" }, flags = { 0 })
public class Hamburg1De extends PluginForDecrypt {

    public Hamburg1De(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            try {
                decryptedLinks.add(this.createOfflinelink(parameter));
            } catch (final Throwable e) {
                /* Not available in old 0.9.581 Stable */
            }
            return decryptedLinks;
        }
        int counter = 1;
        final DecimalFormat df = new DecimalFormat("00");
        String date = br.getRegex("class=\"small-7 columns\">[\t\n\r ]*?<p class=\"text-right\">([^<>\"]*?)</p>").getMatch(0);
        String date_formatted = "-";
        String title = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (title == null || date == null) {
            return null;
        }
        date = Encoding.htmlDecode(date).trim();
        if (!date.equals("")) {
            date_formatted = formatDate(Encoding.htmlDecode(date).trim());
        }
        title = date_formatted + "_hamburg1_" + encodeUnicode(title);
        final String[] regexes = { "video\\.show\\(\"player\"[^\\)]+\"([^<>\"]*?)\"\\)", "(https?://embed\\.telvi\\.de/\\d+/clip/\\d+)" };
        for (final String regex : regexes) {
            final String[] matches = br.getRegex(regex).getColumn(0);
            if (matches != null && matches.length > 0) {
                for (final String match : matches) {
                    final String hostplugin_url;
                    if (match.startsWith("http")) {
                        hostplugin_url = match;
                    } else {
                        hostplugin_url = "decrypted://telvi.de/" + match;
                    }
                    final String filetitle = title + "_" + df.format(counter);
                    final DownloadLink dl = this.createDownloadlink(hostplugin_url);
                    dl.setContentUrl(parameter);
                    dl.setProperty("decryptedfilename", filetitle);
                    decryptedLinks.add(dl);
                    counter++;
                }
            }
        }

        if (decryptedLinks.size() == 0) {
            /* Chances are high that we just don't have a video. */
            try {
                decryptedLinks.add(this.createOfflinelink(parameter));
            } catch (final Throwable e) {
                /* Not available in old 0.9.581 Stable */
            }
            return decryptedLinks;
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

    private String formatDate(final String input) {
        final long date = TimeFormatter.getMilliSeconds(input, "HH:mm 'Uhr' | dd.MM.yyyy", Locale.GERMAN);
        String formattedDate = null;
        final String targetFormat = "yyyy-MM-dd";
        Date theDate = new Date(date);
        try {
            final SimpleDateFormat formatter = new SimpleDateFormat(targetFormat);
            formattedDate = formatter.format(theDate);
        } catch (Exception e) {
            /* prevent input error killing plugin */
            formattedDate = input;
        }
        return formattedDate;
    }

}

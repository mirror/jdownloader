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

package jd.plugins.decrypt;

import java.util.ArrayList;
import java.util.Arrays;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class CineTo extends PluginForDecrypt {

    private static final String patternLink_Protected = "http://[\\w\\.]*?cine\\.to/index\\.php\\?do=protect\\&id=[a-zA-Z0-9]+|http://[\\w\\.]*?cine\\.to/index\\.php\\?do=protect\\&id=[a-zA-Z0-9]+|http://[\\w\\.]*?cine\\.to/pre/index\\.php\\?do=protect\\&id=[a-zA-Z0-9]+";

    public CineTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.getPage(parameter);
        if (parameter.matches(patternLink_Protected)) {
            String[] captcha = br.getRegex("span class=\"([ws]?)\"").getColumn(0);
            String capText = "";
            if (captcha.length == 80) {
                for (int j = 1; j < 5; j++) {
                    capText = capText + extractCaptcha(captcha, j);
                }
            }
            br.postPage(parameter, "captcha=" + capText + "&submit=Senden");
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

    private String extractCaptcha(String[] source, int captchanumber) {
        String[] erg = new String[15];

        erg[0] = source[captchanumber * 4 - 4];
        erg[1] = source[captchanumber * 4 - 3];
        erg[2] = source[captchanumber * 4 - 2];

        erg[3] = source[captchanumber * 4 + 12];
        erg[4] = source[captchanumber * 4 + 13];
        erg[5] = source[captchanumber * 4 + 14];

        erg[6] = source[captchanumber * 4 + 28];
        erg[7] = source[captchanumber * 4 + 29];
        erg[8] = source[captchanumber * 4 + 30];

        erg[9] = source[captchanumber * 4 + 44];
        erg[10] = source[captchanumber * 4 + 45];
        erg[11] = source[captchanumber * 4 + 46];

        erg[12] = source[captchanumber * 4 + 60];
        erg[13] = source[captchanumber * 4 + 61];
        erg[14] = source[captchanumber * 4 + 62];

        String[] wert0 = { "s", "s", "s", "s", "w", "s", "s", "w", "s", "s", "s", "w", "s", "s", "s" };
        if (Arrays.equals(erg, wert0)) { return "0"; }

        String[] wert1 = { "w", "w", "s", "w", "s", "s", "w", "w", "s", "w", "w", "s", "w", "w", "s" };
        if (Arrays.equals(erg, wert1)) { return "1"; }

        String[] wert2 = { "s", "s", "s", "w", "w", "s", "w", "s", "s", "s", "w", "w", "s", "s", "s" };
        if (Arrays.equals(erg, wert2)) { return "2"; }

        String[] wert3 = { "s", "s", "s", "w", "w", "s", "w", "s", "s", "w", "w", "s", "s", "s", "s" };
        if (Arrays.equals(erg, wert3)) { return "3"; }

        String[] wert4 = { "s", "w", "w", "s", "w", "s", "s", "s", "s", "w", "w", "s", "w", "w", "s" };
        if (Arrays.equals(erg, wert4)) { return "4"; }

        String[] wert5 = { "s", "s", "s", "s", "w", "w", "s", "s", "s", "w", "w", "s", "s", "s", "s" };
        if (Arrays.equals(erg, wert5)) { return "5"; }

        String[] wert6 = { "s", "s", "s", "s", "w", "w", "s", "s", "s", "s", "w", "s", "s", "s", "s" };
        if (Arrays.equals(erg, wert6)) { return "6"; }

        String[] wert7 = { "s", "s", "s", "w", "w", "s", "w", "s", "s", "w", "w", "s", "w", "w", "s" };
        if (Arrays.equals(erg, wert7)) { return "7"; }

        String[] wert8 = { "s", "s", "s", "s", "w", "s", "s", "s", "s", "s", "w", "s", "s", "s", "s" };
        if (Arrays.equals(erg, wert8)) { return "8"; }

        String[] wert9 = { "s", "s", "s", "s", "w", "s", "s", "s", "s", "w", "w", "s", "s", "s", "s" };
        if (Arrays.equals(erg, wert9)) { return "9"; }

        return "0";
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

}
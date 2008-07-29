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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;

public class CineTo extends PluginForDecrypt {
    final static String host = "cine.to";
    private String version = "1.2.0";
    private static final Pattern patternLink_Show = Pattern.compile("http://[\\w\\.]*?cine.to/index.php\\?do=show_download\\&id=[a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternLink_Protected = Pattern.compile("http://[\\w\\.]*?cine.to/index.php\\?do=protect\\&id=[a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE);
    private Pattern patternSupported = Pattern.compile(patternLink_Show.pattern() + "|" + patternLink_Protected.pattern(), Pattern.CASE_INSENSITIVE);

    public CineTo() {
        super();        
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginID() {
        return host + "-" + version;
    }

    @Override
    public String getPluginName() {
        return host;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        String cryptedLink = (String) parameter;        
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        RequestInfo reqinfo;
        try {
            if (cryptedLink.matches(patternLink_Show.pattern())) {
                reqinfo = HTTP.getRequest(new URL(cryptedLink));
                String[] mirrors = reqinfo.getRegexp("href=\"index.php\\?do=protect\\&id=([a-zA-Z0-9]+)\"").getMatches(1);
                for (int i = 0; i < mirrors.length; i++) {
                    decryptedLinks.add(this.createDownloadlink("http://cine.to/index.php?do=protect&id=" + mirrors[i]));
                }
            } else if (cryptedLink.matches(patternLink_Protected.pattern())) {
                reqinfo = HTTP.getRequest(new URL(cryptedLink));
                logger.info(reqinfo.getLocation());
                String[][] captcha = reqinfo.getRegexp("span class=\"(.*?)\"").getMatches();
                String capText = "";
                if (captcha.length == 80) {
                    for (int j = 1; j < 5; j++) {
                        capText = capText + extractCaptcha(captcha, j);
                    }
                }
                reqinfo = HTTP.postRequest(new URL(cryptedLink), reqinfo.getCookie(), parameter, null, "captcha=" + capText + "&submit=Senden", true);
                String[][] links = reqinfo.getRegexp("window.open\\(\'(.*?)\'").getMatches();
                progress.setRange(links.length);
                for (int j = 0; j < links.length; j++) {
                    DownloadLink link = this.createDownloadlink(links[j][0]);
                    link.addSourcePluginPassword("cine.to");
                    decryptedLinks.add(link);
                    progress.increase(1);
                }
            }            
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return decryptedLinks;
    }

    private String extractCaptcha(String[][] source, int captchanumber) {
        String[] erg = new String[15];

        erg[0] = source[(captchanumber * 4) - 4][0];
        erg[1] = source[(captchanumber * 4) - 3][0];
        erg[2] = source[(captchanumber * 4) - 2][0];

        erg[3] = source[(captchanumber * 4) + 12][0];
        erg[4] = source[(captchanumber * 4) + 13][0];
        erg[5] = source[(captchanumber * 4) + 14][0];

        erg[6] = source[(captchanumber * 4) + 28][0];
        erg[7] = source[(captchanumber * 4) + 29][0];
        erg[8] = source[(captchanumber * 4) + 30][0];

        erg[9] = source[(captchanumber * 4) + 44][0];
        erg[10] = source[(captchanumber * 4) + 45][0];
        erg[11] = source[(captchanumber * 4) + 46][0];

        erg[12] = source[(captchanumber * 4) + 60][0];
        erg[13] = source[(captchanumber * 4) + 61][0];
        erg[14] = source[(captchanumber * 4) + 62][0];

        String[] wert0 = { "s", "s", "s", "s", "w", "s", "s", "w", "s", "s", "s", "w", "s", "s", "s" };
        if (Arrays.equals(erg, wert0)) return "0";

        String[] wert1 = { "w", "w", "s", "w", "s", "s", "w", "w", "s", "w", "w", "s", "w", "w", "s" };
        if (Arrays.equals(erg, wert1)) return "1";

        String[] wert2 = { "s", "s", "s", "w", "w", "s", "w", "s", "s", "s", "w", "w", "s", "s", "s" };
        if (Arrays.equals(erg, wert2)) return "2";

        String[] wert3 = { "s", "s", "s", "w", "w", "s", "w", "s", "s", "w", "w", "s", "s", "s", "s" };
        if (Arrays.equals(erg, wert3)) return "3";

        String[] wert4 = { "s", "w", "w", "s", "w", "s", "s", "s", "s", "w", "w", "s", "w", "w", "s" };
        if (Arrays.equals(erg, wert4)) return "4";

        String[] wert5 = { "s", "s", "s", "s", "w", "w", "s", "s", "s", "w", "w", "s", "s", "s", "s" };
        if (Arrays.equals(erg, wert5)) return "5";

        String[] wert6 = { "s", "s", "s", "s", "w", "w", "s", "s", "s", "s", "w", "s", "s", "s", "s" };
        if (Arrays.equals(erg, wert6)) return "6";

        String[] wert7 = { "s", "s", "s", "w", "w", "s", "w", "s", "s", "w", "w", "s", "w", "w", "s" };
        if (Arrays.equals(erg, wert7)) return "7";

        String[] wert8 = { "s", "s", "s", "s", "w", "s", "s", "s", "s", "s", "w", "s", "s", "s", "s" };
        if (Arrays.equals(erg, wert8)) return "8";

        String[] wert9 = { "s", "s", "s", "s", "w", "s", "s", "s", "s", "w", "w", "s", "s", "s", "s" };
        if (Arrays.equals(erg, wert9)) return "9";

        return "0";
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

}
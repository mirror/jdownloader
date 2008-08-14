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
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.HTMLEntities;

public class SAUGUS extends PluginForDecrypt {
    final static String host = "saug.us";
    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?saug\\.us/folder-[a-zA-Z0-9\\-]{30,50}\\.html", Pattern.CASE_INSENSITIVE);

    public SAUGUS() {
        super();
    }

    public String deca1(String input) {
        final String keyStr = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
        String output = "";
        char type1, type2, type3;
        char get1, get2 = 0, get3 = 0, get4 = 0;
        int i = 0;

        // remove all characters that are not A-Z, a-z, 0-9, +, /, or =
        input = input.replaceAll("[^A-Za-z0-9\\+\\/\\=]", "");

        do {
            get1 = (char) keyStr.indexOf(input.charAt(i++));

            get2 = (char) keyStr.indexOf(input.charAt(i++));

            get3 = (char) keyStr.indexOf(input.charAt(i++));

            get4 = (char) keyStr.indexOf(input.charAt(i++));

            type1 = (char) (get1 << 2 | get2 >> 4);
            type2 = (char) ((get2 & 15) << 4 | get3 >> 2);
            type3 = (char) ((get3 & 3) << 6 | get4);

            output = output + type1;

            if (get3 != 64) {
                output = output + type2;
            }
            if (get4 != 64) {
                output = output + type3;
            }
        } while (i < input.length());

        return output;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            br.getPage(parameter);
            
            if ( br.containsHTML("<span style=\"font-size:9pt;\">Dateien offline!")) { return null; }
            String hst = "http://" + br.getRequest().getUrl().getHost() + "/";
            String[] crypt = br.getRegex("document.write\\(decb.*?\\(\'(.*?)\'\\)\\)\\;").getMatches(1);
            progress.setRange(crypt.length);

            for (String string : crypt) {
                string = deca1(string);

                string = new Regex(string, "\\(deca.*?\\(\'(.*?)\'").getFirstMatch();
                string = deca1(string);

                string = new Regex(string, "\\(dec.*?\\(\'(.*?)\'").getFirstMatch();
                string = deca1(string);

                string = hst + HTMLEntities.unhtmlentities(new Regex(string, "javascript\\:page\\(\'(.*?)\'\\)\\;").getFirstMatch());
                br.getPage(string);
                string = HTMLEntities.unhtmlentities(new Regex(br.toString().replaceAll("<!--.*?-->", ""), "<iframe src=\"(.*?)\"").getFirstMatch()).trim().replaceAll("^[\\s]*", "");
                if (!string.toLowerCase().matches("http\\:\\/\\/.*")) {
                    br.getPage(hst + string);
                    decryptedLinks.add(createDownloadlink(br.getForm(0).getAction("http://saug.us")));
                } else {
                    decryptedLinks.add(createDownloadlink(string));
                }
                progress.increase(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return decryptedLinks;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
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
    public String getPluginName() {
        return host;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getFirstMatch();
        return ret == null ? "0.0" : ret;
    }
}
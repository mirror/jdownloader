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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class RapidsafeDe extends PluginForDecrypt {
    final static String host = "rapidsafe.de";
    private String version = "0.1";
    private Pattern patternSupported = Pattern.compile("http://.+rapidsafe\\.de", Pattern.CASE_INSENSITIVE);

    public RapidsafeDe() {
        super();
    }

    
    public String getCoder() {
        return "JD-Team";
    }

    
    public String getHost() {
        return host;
    }

  

    
    public String getPluginName() {
        return host;
    }

    
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    
    public String getVersion() {
        return new Regex("$Revision$","\\$Revision: ([\\d]*?)\\$").getFirstMatch();
    }

    
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (!parameter.endsWith("/")) parameter += "/";
        try {
            HTTP.setReadTimeout(120000);
            HTTP.setConnectTimeout(120000);
            JDUtilities.getSubConfig("DOWNLOAD").save();

            RequestInfo ri = HTTP.getRequest(new URL(parameter));

            String cookie = ri.getCookie();

            String[] dat = SimpleMatches.getSimpleMatches(ri.getHtmlCode(), "RapidSafePSC('°=°&t=°','°');");
            ri = HTTP.postRequest(new URL(parameter), cookie, parameter, null, dat[0] + "=" + dat[1] + "&t=" + dat[2], false);

            dat = SimpleMatches.getSimpleMatches(ri.getHtmlCode(), "RapidSafePSC('°&adminlogin='");
            ri = HTTP.postRequest(new URL(parameter), cookie, parameter, null, dat[0] + "&f=1", false);

            ArrayList<ArrayList<String>> flash = SimpleMatches.getAllSimpleMatches(ri.getHtmlCode(), "<param name=\"movie\" value=\"/°\" />");

            ArrayList<ArrayList<String>> helpsites = SimpleMatches.getAllSimpleMatches(ri.getHtmlCode(), "onclick=\"RapidSafePSC('°&start=°','");
            for (int i = 0; i < helpsites.size(); i++) {
                ri = HTTP.postRequest(new URL(parameter), cookie, parameter, null, dat[0] + "&f=1&start=" + helpsites.get(i).get(1), false);
                ArrayList<ArrayList<String>> helpflash = SimpleMatches.getAllSimpleMatches(ri.getHtmlCode(), "<param name=\"movie\" value=\"/°\" />");

                for (int j = 0; j < helpflash.size(); j++)
                    flash.add(helpflash.get(j));
            }

            progress.setRange(flash.size());

            for (int flashcounter = 0; flashcounter < flash.size(); flashcounter++) {
                boolean repeat = true;
                long[] zaehler = new long[7];
                ArrayList<ArrayList<String>> search2 = new ArrayList<ArrayList<String>>();
                while (repeat) {
                    try {
                        HTTPConnection con = new HTTPConnection(new URL(parameter + flash.get(flashcounter).get(0)).openConnection());
                        con.setRequestProperty("Cookie", cookie);
                        con.setRequestProperty("Referer", parameter);

                        BufferedInputStream input = new BufferedInputStream(con.getInputStream());
                        StringBuffer sb = new StringBuffer();

                        zaehler = new long[7];
                        byte[] b = new byte[1];
                        // liest alles ein und legt alle daten hexcodiert
                        // ab. Das ermöglicht ascii regex suche
                        while (input.read(b) != -1) {
                            String s = Integer.toHexString((byte) b[0] & 0x000000ff);
                            sb.append((s.length() == 1 ? "0" : "") + s + "");
                        }
                        input.close();
                        String c = sb.toString();
                        // die zwei positionen von getURL
                        int index1 = c.indexOf("67657455524c");
                        int index2 = c.indexOf("67657455524c", index1 + 8);
                        c = c.substring(c.indexOf("67657455524c"), index2);
                        // Suchen der zahlen
                        ArrayList<ArrayList<String>> search1 = SimpleMatches.getAllSimpleMatches(c, "96070008°07°3c");
                        search2 = SimpleMatches.getAllSimpleMatches(c, "070007°08021c960200");
                        // Umwandlen der Hexwerte
                        for (int i = 0; i < 7; i++) {
                            zaehler[i] = (int) Long.parseLong(spin(search1.get(i).get(1).toUpperCase()), 16);
                        }
                        repeat = false;
                    } catch (NumberFormatException e) {
                        logger.info("Error while parsing. Loading flash again!");
                    }

                }

                long ax5 = zaehler[0];
                long ccax4 = zaehler[1];
                long ax3 = zaehler[2];
                long ax7 = zaehler[3];
                long ax6 = zaehler[4];
                long ax1 = zaehler[5];
                long ax2 = zaehler[6];

                // Umwandlen der Hexwerte
                long[] modifier = new long[search2.size()];
                int count = 0;
                for (Iterator<ArrayList<String>> it = search2.iterator(); it.hasNext();) {
                    modifier[count++] = Long.parseLong(spin(it.next().get(0)), 16);
                }

                String postdata = "";
                for (long zahl : modifier) {
                    postdata += String.valueOf((char) (zahl ^ (ax3 ^ ax2 + 2 + 9 ^ ccax4 + 12) ^ 2 ^ 41 - 12 ^ 112 ^ ax1 ^ ax5 ^ 41 ^ ax6 ^ ax7));
                }

                postdata = SimpleMatches.getSimpleMatch(postdata, "RapidSafePSC('°'", 0);

                ri = HTTP.postRequest(new URL(parameter), cookie, parameter, null, postdata, false);
                Map<String, List<String>> headers = ri.getHeaders();
                String content = "";

                int counter = 0;
                String help1 = "";
                while (true) {
                    try {
                        help1 = headers.get("X-RapidSafe-E" + counter).get(0);
                        content += help1;
                        counter++;
                    } catch (NullPointerException e) {
                        break;
                    }
                }

                String content1 = "";
                for (int i = 0; i < content.length(); i += 2) {
                    content1 += String.valueOf((char) Integer.parseInt(content.substring(i, i + 2), 16));
                }

                String[][] help = new Regex(content1, "\\(([\\d]+).([\\d]+).([\\d]+)\\)").getMatches();

                content = "";
                for (int i = 0; i < help.length; i++) {
                    content += String.valueOf((char) (Integer.parseInt(help[i][0]) ^ Integer.parseInt(help[i][1]) ^ Integer.parseInt(help[i][2])));
                }
                progress.increase(1);
                decryptedLinks.add(this.createDownloadlink(SimpleMatches.getSimpleMatch(content, "action=\"°\" id", 0)));
            }

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return decryptedLinks;
    }

    private String spin(String string) {
        String ret = "";
        for (int i = string.length(); i >= 2; i -= 2)
            ret += string.substring(i - 2, i);
        return ret;
    }

    
    public boolean doBotCheck(File file) {
        return false;
    }
}
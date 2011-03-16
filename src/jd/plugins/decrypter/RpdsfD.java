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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rapidsafe.de" }, urls = { "http://.+rapidsafe\\.de" }, flags = { 0 })
public class RpdsfD extends PluginForDecrypt {

    public RpdsfD(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        if (!parameter.endsWith("/")) {
            parameter += "/";
        }
        try {
            br.setFollowRedirects(false);
            br.getPage(parameter);

            String dat = br.getRegex("RapidSafePSC\\('(.*?=.*?&t=.*?)','.*?'\\);").getMatch(0);
            br.postPage(parameter, dat);

            String pass = getPluginConfig().getStringProperty("pass");
            for (int i = 0; i < 5; i++) {

                Regex pw = br.getRegex("RapidSafePSC\\('(.*?)'\\+escape\\(document\\.getElementById\\('linkpassword'\\)\\.value\\)\\+'(.*?)','(.*?)'\\)");
                if (pw.matches()) {
                    if (i > 0) pass = null;
                    String[] pwDat = pw.getRow(0);
                    String post = pwDat[0] + pwDat[1];

                    if (pass == null) pass = getUserInput(null, param);

                    br.postPage(parameter, post + pass.trim());

                } else {
                    break;
                }
            }
            if (pass != null) {
                this.getPluginConfig().setProperty("pass", pass);
            }
            dat = br.getRegex("RapidSafePSC\\('(.*?)&adminlogin='").getMatch(0);
            br.postPage(parameter, dat + "&f=1");

            ArrayList<String> flash = new ArrayList<String>();
            String[] flashsites = br.getRegex("<param name=\"movie\" value=\"/(.*?)\" />").getColumn(0);
            for (String flashsite : flashsites) {
                flash.add(flashsite);
            }

            String[][] helpsites = br.getRegex("onclick=\"RapidSafePSC\\('(.*?)&start=(.*?)','").getMatches();
            for (String[] helpsite : helpsites) {
                br.postPage(parameter, dat + "&f=1&start=" + helpsite[1]);
                String[] helpflash = br.getRegex("<param name=\"movie\" value=\"/(.*?)\" />").getColumn(0);

                for (String element : helpflash) {
                    flash.add(element);
                }
            }

            progress.setRange(flash.size());

            main: for (int flashcounter = 0; flashcounter < flash.size(); flashcounter++) {
                boolean repeat = true;
                long[] zaehler = new long[7];
                String[] search2 = new String[0];
                int retries = 0;
                while (repeat) {
                    retries++;
                    if (retries > 10) {
                        logger.severe("Decrypt error: HOst server error. please try again later");
                        continue main;
                    }
                    URLConnectionAdapter con = null;
                    try {
                        con = br.openGetConnection(parameter + flash.get(flashcounter));

                        InputStream input = con.getInputStream();
                        StringBuilder sb = new StringBuilder();

                        zaehler = new long[7];
                        byte[] b = new byte[1];
                        // liest alles ein und legt alle daten hexcodiert
                        // ab. Das ermöglicht ascii regex suche
                        while (input.read(b) != -1) {
                            String s = Integer.toHexString(b[0] & 0x000000ff);
                            sb.append((s.length() == 1 ? "0" : "") + s + "");
                        }
                        input.close();
                        String c = sb.toString();
                        // die zwei positionen von getURL
                        int index1 = c.indexOf("67657455524c");
                        int index2 = c.indexOf("67657455524c", index1 + 8);
                        c = c.substring(c.indexOf("67657455524c"), index2);
                        // Suchen der zahlen
                        String[][] search1 = new Regex(c, "96070008(.*?)07(.*?)3c").getMatches();
                        search2 = new Regex(c, "070007(.*?)08021c960200").getColumn(0);
                        // Umwandlen der Hexwerte
                        for (int i = 0; i < 7; i++) {
                            zaehler[i] = (int) Long.parseLong(spin(search1[i][1].toUpperCase()), 16);
                        }
                        repeat = false;
                    } catch (Exception e) {
                        logger.info("Error while parsing. Loading flash again!");

                    } finally {
                        try {
                            con.disconnect();
                        } catch (final Throwable e) {
                        }
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
                long[] modifier = new long[search2.length];
                int count = 0;
                for (String arrayList : search2) {
                    modifier[count++] = Long.parseLong(spin(arrayList), 16);
                }

                String postdata = "";
                for (long zahl : modifier) {
                    postdata += String.valueOf((char) (zahl ^ ax3 ^ ax2 + 2 + 9 ^ ccax4 + 12 ^ 2 ^ 41 - 12 ^ 112 ^ ax1 ^ ax5 ^ 41 ^ ax6 ^ ax7));
                }

                postdata = new Regex(postdata, "RapidSafePSC\\('(.*?)'", 0).getMatch(0);

                br.postPage(parameter, postdata);
                Map<String, List<String>> headers = br.getHttpConnection().getHeaderFields();
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
                for (String[] element : help) {
                    content += String.valueOf((char) (Integer.parseInt(element[0]) ^ Integer.parseInt(element[1]) ^ Integer.parseInt(element[2])));
                }
                progress.increase(1);
                decryptedLinks.add(createDownloadlink(new Regex(content, "action=\"(.*?)\" id").getMatch(0)));
            }
        } catch (DecrypterException e2) {
            throw e2;
        } catch (IOException e) {
            logger.log(java.util.logging.Level.SEVERE, "Exception occurred", e);
            return null;
        }
        return decryptedLinks;
    }

    private String spin(String string) {
        StringBuilder ret = new StringBuilder();
        for (int i = string.length(); i >= 2; i -= 2) {
            ret.append(string.substring(i - 2, i));
        }
        return ret.toString();
    }

}

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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.plugins.DownloadLink;
import jd.plugins.HTTPConnection;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.plugins.Regexp;
import jd.utils.JDUtilities;

public class RapidsafeDe extends PluginForDecrypt {
    final static String host = "rapidsafe.de";
    private String version = "0.1";
    private Pattern patternSupported = getSupportPattern("http://[+]rapidsafe.de");

    public RapidsafeDe() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
    }

    @Override
    public String getCoder() {
        return "jD-Team";
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
    public PluginStep doStep(PluginStep step, String parameter) {

        if (step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
            if (!parameter.endsWith("/")) parameter += "/";
            try {
                progress.setRange(10);
                setReadTimeout(120000);
                setConnectTimeout(120000);
                JDUtilities.getSubConfig("DOWNLOAD").save();

                RequestInfo ri = getRequest(new URL(parameter));
                @SuppressWarnings("unused")
                String cookie = ri.getCookie();
                @SuppressWarnings("unused")
                String[] dat = getSimpleMatches(ri.getHtmlCode(), "RapidSafePSC('°=°&t=°','°');");
                progress.increase(1);
                ri = postRequest(new URL(parameter), cookie, parameter, null, dat[0] + "=" + dat[1] + "&t=" + dat[2], false);
                progress.increase(1);
                dat = getSimpleMatches(ri.getHtmlCode(), "RapidSafePSC('°&adminlogin='");
                ri = postRequest(new URL(parameter), cookie, parameter, null, dat[0] + "&f=1", false);
                progress.increase(1);
                ArrayList<String> flashs = getAllSimpleMatches(ri.getHtmlCode(), "<param name=\"movie\" value=\"/°\" />", 1);
                progress.setRange(flashs.size() * 2);
                progress.setStatus(0);
                for (String flash : flashs) {

                    HTTPConnection con = new HTTPConnection(new URL(parameter + flash).openConnection());
                    con.setRequestProperty("Cookie", cookie);
                    con.setRequestProperty("Referer", parameter);
                    // Debug inputstream
                    // InputStream in = new FileInputStream(new
                    // File("c:/swf.swf"));
                    BufferedInputStream input = new BufferedInputStream(con.getInputStream());
                    StringBuffer sb = new StringBuffer();

                    long[] zaehler = new long[7];
                    byte[] b = new byte[1];
                    // liest alles ein und legt alle daten hexcodiert ab. Das
                    // ermöglicht ascii regex suche
                    while (input.read(b) != -1) {
                        String s = Integer.toHexString((byte) b[0] & 0x000000ff);
                        sb.append((s.length() == 1 ? "0" : "") + s + "");

                    }
                    progress.increase(1);
                    String c = sb.toString();
                    // die zwei positionen von getURL
                    int index1 = c.indexOf("67657455524c");
                    int index2 = c.indexOf("67657455524c", index1 + 8);
                    c = c.substring(c.indexOf("67657455524c"), index2);
                    // Suchen der zahlen
                    ArrayList<ArrayList<String>> search1 = getAllSimpleMatches(c, Pattern.compile("96070008[^\\s]{2}07([^\\s]{8})3c"));
                    ArrayList<ArrayList<String>> search2 = getAllSimpleMatches(c, Pattern.compile("4796070007([^\\s]{8})08021c96"));
                    
                    // Umwandlen der Hexwerte
                    for (int i = 0; i < 7; i++){
                   try{
                        zaehler[i] = (int) Long.parseLong(spin(search1.get(i).get(0).toUpperCase()), 16);
                   } catch(Exception e){
                            e.printStackTrace();
                        }
                    }
                    long ax5 = zaehler[0];
                    long ccax4 = zaehler[1];
                    long ax3 = zaehler[2];
                    long ax7 = zaehler[3];
                    long ax6 = zaehler[4];
                    long ax1 = zaehler[5];
                    long ax2 = zaehler[6];
                    input.close();
                    // Umwandlen der Hexwerte
                    long[] modifier = new long[search2.size()];
                    int count = 0;
                    for (Iterator<ArrayList<String>> it = search2.iterator(); it.hasNext();) {
                        modifier[count++] = (Long.parseLong(spin(it.next().get(0)), 16));
                    }

                    String postdata = "";
                    for (long zahl : modifier) {
                        postdata += String.valueOf((char) (zahl ^ (ax3 ^ ax2 + 2 + 9 ^ ccax4 + 12) ^ 2 ^ 41 - 12 ^ 112 ^ ax1 ^ ax5 ^ 41 ^ ax6 ^ ax7));
                    }

                    //System.out.println(postdata);
                    postdata = getSimpleMatch(postdata, "RapidSafePSC('°'", 0);
                   // System.out.println(postdata);

                    ri = postRequest(new URL(parameter), cookie, parameter, null, postdata, false);
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
                    // System.out.println(content.length());
                    String content1 = "";

                    // System.out.println(content);
                    for (int i = 0; i < content.length(); i += 2) {
                        content1 += String.valueOf((char) Integer.parseInt(content.substring(i, i + 2), 16));
                    }
                    // System.out.println(content1);
                    String[][] help = new Regexp(content1, "\\(([\\d]+).([\\d]+).([\\d]+)\\)").getMatches();

                    content = "";
                    for (int i = 0; i < help.length; i++) {
                        content += String.valueOf((char) (Integer.parseInt(help[i][0]) ^ Integer.parseInt(help[i][1]) ^ Integer.parseInt(help[i][2])));
                    }
                    progress.increase(1);
                    String link = getSimpleMatch(content, "action=\"°\" id", 0);
                    decryptedLinks.add(this.createDownloadlink(link));

                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            step.setParameter(decryptedLinks);
            return step;
        }
        return null;
    }

    private String spin(String string) {
        String ret = "";
        for (int i = string.length(); i >= 2; i -= 2)
            ret += string.substring(i - 2, i);
        return ret;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
}
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
import java.io.BufferedInputStream;
import java.net.*;
import java.util.ArrayList;
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
    final static String host             = "rapidsafe.de";
    private String      version          = "0.1";
    private Pattern     patternSupported = getSupportPattern("http://[+]rapidsafe.de");

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
        return host+"-"+version;
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
        
        if(step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
            if(!parameter.endsWith("/"))
                parameter += "/";
            try {
                setReadTimeout(120000);
                setConnectTimeout(120000);
                JDUtilities.getSubConfig("DOWNLOAD").save();

                RequestInfo ri = getRequest(new URL(parameter));
                @SuppressWarnings("unused")
                String cookie = ri.getCookie();
                @SuppressWarnings("unused")
                String[] dat = getSimpleMatches(ri.getHtmlCode(), "RapidSafePSC('°=°&t=°','°');");
                ri = postRequest(new URL(parameter), cookie, parameter, null, dat[0] + "=" + dat[1] + "&t=" + dat[2], false);
                dat = getSimpleMatches(ri.getHtmlCode(), "RapidSafePSC('°&adminlogin='");
                ri = postRequest(new URL(parameter), cookie, parameter, null, dat[0] + "&f=1", false);
                String flash = getSimpleMatch(ri.getHtmlCode(), "<param name=\"movie\" value=\"/°\" />", 0);
                HTTPConnection con = new HTTPConnection(new URL(parameter + flash).openConnection());
                con.setRequestProperty("Cookie", cookie);
                con.setRequestProperty("Referer", parameter);

                BufferedInputStream input = new BufferedInputStream(con.getInputStream());
                StringBuffer sb = new StringBuffer();
                ArrayList<Byte> bb = new ArrayList<Byte>();
                int xx = 0;
                int[] zaehler = new int[7];
                byte[] b = new byte[1];
                while (input.read(b) != -1) {
                    if(b[0]<0||b[0]>34||b[0]==9||b[0]==10||b[0]==13){
                        sb.append(new String(b));
                    }else{
                        sb.append(".");
                    }
                    if(sb.toString().endsWith(".....") && xx < 7 && sb.toString().indexOf("getURL") > 1){
                        input.read(b);
                        if(b[0]<0||b[0]>34||b[0]==9||b[0]==10||b[0]==13){
                            sb.append(new String(b));
                        }
                        else {
                            sb.append(".");
                        }
                        zaehler[xx] = (0x000000FF & ((int)b[0]));
                        xx++;
                    }
                    if(new Regexp(sb.toString(), "getURL").count() == 1)
                        bb.add(b[0]);
                    
                    if(new Regexp(sb.toString(), "getURL").count() > 1)
                        break;
                }
                input.close();
                //logger.info(""+sb.toString());
                for(int u=0; u< zaehler.length; u++)
                    System.out.println(zaehler[u]);
                    
                byte[] bh1 = new byte[8];
                ArrayList<Integer> zahlen = new ArrayList<Integer>();

                for(int i=0; i<bb.size()-15; i++) {
                    if(bb.get(i)==0x07){
                        bh1 = new byte[8];
                        bh1[0] = bb.get(i+1);
                        bh1[1] = bb.get(i+2);
                        bh1[2] = bb.get(i+7);
                        bh1[3] = bb.get(i+8);
                        bh1[4] = bb.get(i+9);
                        bh1[5] = bb.get(i+10);
                        bh1[6] = bb.get(i+11);
                        bh1[7] = bb.get(i+12);
                        if(bh1[0]==0x00 && bh1[1]==0x07 && bh1[2]==0x08 && bh1[3]==0x02 && bh1[4]==0x01C && bh1[5]==-106 && bh1[6]==0x02 && bh1[7]==0x00) {
                            //System.out.println(bh1[2] + " " + bh1[3] + " " + bh1[4] + " " + bh1[5] + " " + bh1[6] + " " + bh1[7]);
                            zahlen.add((0x000000FF & ((int)bb.get(i+3))));
                        }
                    }
                }
                
                String postdata = "";
                int xor1 = zaehler[2] ^ zaehler[6] + 2 + 9 ^ zaehler[1] + 12;
                int xor2 = 12 ^ 112 ^ zaehler[5] ^ zaehler[0] ^ 41 ^ zaehler[4] ^ zaehler[3];
                for(int i=0;i<zahlen.size();i++) {
                    postdata += String.valueOf((char) (zahlen.get(i) ^ xor1 ^ 2 ^ 41 - xor2));
                }
                System.out.println(postdata);
                postdata = getSimpleMatch(postdata,"RapidSafePSC('°'",0);
                System.out.println(postdata);
                
                ri = postRequest(new URL(parameter), cookie, parameter, null, postdata, false);
                Map<String, List<String>> headers = ri.getHeaders();
                String content = "";
                
                int counter = 0;
                String help1 = "";
                while(true) {
                    try {
                        help1=headers.get("X-RapidSafe-E"+counter).get(0);
                        content += help1;
                        counter++;
                    }
                    catch(NullPointerException e) {
                        break;
                    }
                }
                //System.out.println(content.length());
                String content1 = "";                
                
                //System.out.println(content);
                for(int i=0; i<content.length(); i+=2) {
                    content1 += String.valueOf((char)Integer.parseInt(content.substring(i, i+2), 16));
                }
                //System.out.println(content1);
                String[][] help = new Regexp(content1, "\\(([\\d]+).([\\d]+).([\\d]+)\\)").getMatches();
                
                content = "";
                for(int i=0; i<help.length; i++) {
                    content += String.valueOf((char)(Integer.parseInt(help[i][0]) ^ Integer.parseInt(help[i][1]) ^ Integer.parseInt(help[i][2])));
                }
                System.out.println(getBetween(content, "method=\"POST\" action=\"", "\""));
                step.setParameter(decryptedLinks);
            } catch(IOException e) {
                 e.printStackTrace();
            }
            
        }
        return null;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
}
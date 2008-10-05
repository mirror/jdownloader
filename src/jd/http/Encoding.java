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

package jd.http;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.utils.HTMLEntities;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

public class Encoding {

    
    public static byte[] base16Decode(String code){
        while(code.length()%2>0)code+="0";
        byte[] res= new byte[code.length()/2];
       int i=0;
        while(i<code.length()){
          res[i/2]=(byte)Integer.parseInt(code.substring(i, i+2), 16);
          i+=2;
            
        }
        return res;
    }
    /**
     * "http://rapidshare.com&#x2F;&#x66;&#x69;&#x6C;&#x65;&#x73;&#x2F;&#x35;&#x34;&#x35;&#x34;&#x31;&#x34;&#x38;&#x35;&#x2F;&#x63;&#x63;&#x66;&#x32;&#x72;&#x73;&#x64;&#x66;&#x2E;&#x72;&#x61;&#x72;" ;
     * Wandelt alle hexkodierten zeichen in diesem Format in normalen text um
     * 
     * @param str
     * @return decoded string
     */
    public static String htmlDecode(String str) {
        // http://rs218.rapidshare.com/files/&#0052;&#x0037;&#0052;&#x0034;&#0049
        // ;&#x0032;&#0057;&#x0031;/STE_S04E04.Borderland.German.dTV.XviD-2
        // Br0th3rs.part1.rar
        if (str == null) { return null; }
        StringBuffer sb = new StringBuffer();
        String pattern = "(\\&\\#x[a-f0-9A-F]+\\;?)";
        Matcher r = Pattern.compile(pattern, Pattern.DOTALL).matcher(str);
        while (r.find()) {
            if (r.group(1).length() > 0) {
                char c = (char) Integer.parseInt(r.group(1).replaceAll("\\&\\#x", "").replaceAll("\\;", ""), 16);
                if (c == '$' || c == '\\') {
                    r.appendReplacement(sb, "\\" + c);
                } else {
                    r.appendReplacement(sb, "" + c);
                }
            }
        }
        r.appendTail(sb);
        str = sb.toString();

        sb = new StringBuffer();
        pattern = "(\\&\\#\\d+\\;?)";
        r = Pattern.compile(pattern, Pattern.DOTALL).matcher(str);
        while (r.find()) {

            if (r.group(1).length() > 0) {
                char c = (char) Integer.parseInt(r.group(1).replaceAll("\\&\\#", "").replaceAll("\\;", ""), 10);
                if (c == '$' || c == '\\') {
                    r.appendReplacement(sb, "\\" + c);
                } else {
                    r.appendReplacement(sb, "" + c);
                }
            }
        }
        r.appendTail(sb);
        str = sb.toString();

        sb = new StringBuffer();
        pattern = "(\\%[a-f0-9A-F]{2})";
        r = Pattern.compile(pattern, Pattern.DOTALL).matcher(str);
        while (r.find()) {
            if (r.group(1).length() > 0) {
                char c = (char) Integer.parseInt(r.group(1).replaceFirst("\\%", ""), 16);
                if (c == '$' || c == '\\') {
                    r.appendReplacement(sb, "\\" + c);
                } else {
                    r.appendReplacement(sb, "" + c);
                }
            }
        }
        r.appendTail(sb);
        str = sb.toString();

        try {
            str = URLDecoder.decode(str, "UTF-8");
        } catch (Exception e) {
        }
        return HTMLEntities.unhtmlentities(str);
    }

    /**
     * @author JD-Team Macht ein urlRawEncode und spart dabei die angegebenen
     *         Zeichen aus
     * @param str
     * @return str URLCodiert
     */
    @SuppressWarnings("deprecation")
    public static String urlEncode(String str) {
        try {
            return URLEncoder.encode(str);
        } catch (Exception e) {
            // TODO: handle exception
        }
        return null;
        /*
         * 
         * 
         * if (str == null) return str; String allowed =
         * "1234567890QWERTZUIOPASDFGHJKLYXCVBNMqwertzuiopasdfghjklyxcvbnm-_.?/\\:&=;" ;
         * String ret = ""; String l; int i; for (i = 0; i < str.length(); i++) {
         * char letter = str.charAt(i); if (allowed.indexOf(letter) >= 0) { ret +=
         * letter; } else { l = Integer.toString(letter, 16); ret += "%" +
         * (l.length() == 1 ? "0" + l : l); } }
         */

    }

    /**
     * @author JD-Team
     * @param str
     * @return str als UTF8Decodiert
     */
    public static String UTF8Decode(String str) {
        if (str == null) { return null; }
        try {
            return new String(str.getBytes(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @author JD-Team
     * @param str
     * @return str als UTF8 Kodiert
     */
    public static String UTF8Encode(String str) {
        try {
            return new String(str.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String Base64Decode(String base64) {
        if (base64 == null) { return null; }
        try {
            byte[] plain = new BASE64Decoder().decodeBuffer(base64);
            if (Encoding.filterString(new String(plain)).length() < plain.length / 1.5) { return base64; }
            return new String(plain);
        } catch (IOException e) {
            return base64;
        }
    }

    public static String Base64Encode(String plain) {

        if (plain == null) { return null; }
        String base64 = new BASE64Encoder().encode(plain.getBytes());
        base64 = Encoding.filterString(base64, "qwertzuiopasdfghjklyxcvbnmMNBVCXYASDFGHJKLPOIUZTREWQ1234567890=/");

        return base64;
    }

    /**
     * Filtert alle nicht lesbaren zeichen aus str
     * 
     * @param str
     * @return
     */
    public static String filterString(String str) {
        String allowed = "QWERTZUIOPÜASDFGHJKLÖÄYXCVBNMqwertzuiopasdfghjklyxcvbnm;:,._-&$%(){}#~+ 1234567890<>='\"/";
        return Encoding.filterString(str, allowed);
    }

    /**
     * Filtert alle zeichen aus str die in filter nicht auftauchen
     * 
     * @param str
     * @param filter
     * @return
     */
    public static String filterString(String str, String filter) {
        if (str == null || filter == null) { return ""; }

        byte[] org = str.getBytes();
        byte[] mask = filter.getBytes();
        byte[] ret = new byte[org.length];
        int count = 0;
        int i;
        for (i = 0; i < org.length; i++) {
            byte letter = org[i];
            for (byte element : mask) {
                if (letter == element) {
                    ret[count] = letter;
                    count++;
                    break;
                }
            }
        }
        return new String(ret).trim();
    }
    
    /**
     * Wenden htmlDecode an, bis es keine Änderungen mehr gibt. Aber max 50 mal!
     * @param string
     * @return
     */
    public static String deepHtmlDecode(String string) {
        String decoded,tmp;
        tmp=Encoding.htmlDecode(string);
        int i=50;
        while(!tmp.equals(decoded=Encoding.htmlDecode(tmp))){                
            tmp=decoded;
            if(i--<=0){
                System.err.println("Max Decodeingloop 50 reached!!!");
                return tmp;
            }
        }
        return tmp;
    }

}

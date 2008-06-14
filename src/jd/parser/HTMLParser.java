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


package jd.parser;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.MultiPartFormOutputStream;
import jd.plugins.Plugin;
import jd.plugins.RequestInfo;
import jd.unrar.JUnrar;
import jd.utils.JDUtilities;

public class HTMLParser {

    /**
     * Gibt alle Hidden fields als hasMap zurück
     * 
     * @param data
     * @return hasmap mit allen hidden fields variablen
     */
    public static HashMap<String, String> getInputHiddenFields(String data) {
        Pattern intput1 = Pattern.compile("(?s)<[ ]?input([^>]*?type=['\"]?hidden['\"]?[^>]*?)[/]?>", Pattern.CASE_INSENSITIVE);
        Pattern intput2 = Pattern.compile("name=['\"]([^'\"]*?)['\"]", Pattern.CASE_INSENSITIVE);
        Pattern intput3 = Pattern.compile("value=['\"]([^'\"]*?)['\"]", Pattern.CASE_INSENSITIVE);
        Pattern intput4 = Pattern.compile("name=([^\\s]*)", Pattern.CASE_INSENSITIVE);
        Pattern intput5 = Pattern.compile("value=([^\\s]*)", Pattern.CASE_INSENSITIVE);
        Matcher matcher1 = intput1.matcher(data);
        Matcher matcher2;
        Matcher matcher3;
        Matcher matcher4;
        Matcher matcher5;
        HashMap<String, String> ret = new HashMap<String, String>();
        boolean iscompl;
        while (matcher1.find()) {
            matcher2 = intput2.matcher(matcher1.group(1) + " ");
            matcher3 = intput3.matcher(matcher1.group(1) + " ");
            matcher4 = intput4.matcher(matcher1.group(1) + " ");
            matcher5 = intput5.matcher(matcher1.group(1) + " ");
            iscompl = false;
            String key, value;
            key = value = null;
            if (matcher2.find()) {
                iscompl = true;
                key = matcher2.group(1);
            } else if (matcher4.find()) {
                iscompl = true;
                key = matcher4.group(1);
            }
            if (matcher3.find() && iscompl)
                value = matcher3.group(1);
            else if (matcher5.find() && iscompl)
                value = matcher5.group(1);
            else
                iscompl = false;
            ret.put(key, value);
        }
        return ret;
    }

    /**
     * Diese Methode sucht die vordefinierten input type="hidden" und formatiert
     * sie zu einem poststring z.b. würde bei:
     * 
     * <input type="hidden" name="f" value="f50b0f" /> <input type="hidden"
     * name="h" value="390b4be0182b85b0" /> <input type="hidden" name="b"
     * value="9" />
     * 
     * f=f50b0f&h=390b4be0182b85b0&b=9 ausgegeben werden
     * 
     * @param data
     *            Der zu durchsuchende Text
     * 
     * @return ein String, der als POST Parameter genutzt werden kann und alle
     *         Parameter des Formulars enthält
     */
    public static String getFormInputHidden(String data) {
        return Plugin.joinMap(getInputHiddenFields(data), "=", "&");
    }
    /**
     * Ermittelt alle hidden input felder in einem HTML Text und gibt die hidden
     * variables als hashmap zurück es wird dabei nur der text zwischen start
     * dun endpattern ausgewertet
     * 
     * @param data
     * @param startPattern
     * @param lastPattern
     * @return hashmap mit hidden input variablen zwischen startPattern und
     *         endPattern
     */
    public static HashMap<String, String> getInputHiddenFields(String data, String startPattern, String lastPattern) {
        return HTMLParser.getInputHiddenFields(SimpleMatches.getBetween(data, startPattern, lastPattern));
    }
    /**
     * Diese Methode sucht die vordefinierten input type="hidden" zwischen
     * startpattern und lastpattern und formatiert sie zu einem poststring z.b.
     * würde bei:
     * 
     * <input type="hidden" name="f" value="f50b0f" /> <input type="hidden"
     * name="h" value="390b4be0182b85b0" /> <input type="hidden" name="b"
     * value="9" />
     * 
     * f=f50b0f&h=390b4be0182b85b0&b=9 rauskommen
     * 
     * @param data
     *            Der zu durchsuchende Text
     * @param startPattern
     *            der Pattern, bei dem die Suche beginnt
     * @param lastPattern
     *            der Pattern, bei dem die Suche endet
     * @return ein String, der als POST Parameter genutzt werden kann und alle
     *         Parameter des Formulars enthält
     */
    public static String getFormInputHidden(String data, String startPattern, String lastPattern) {
        return HTMLParser.getFormInputHidden(SimpleMatches.getBetween(data, startPattern, lastPattern));
    }

    /**
     * Diese Methode sucht nach passwörtern in einem Datensatz
     * 
     * @param data
     * @return
     */
    public static Vector<String> findPasswords(String data) {
        if (data == null) return new Vector<String>();
        Iterator<String> iter = JUnrar.getPasswordList().iterator();
        Vector<String> ret = new Vector<String>();
        while (iter.hasNext()) {
            String pass = (String) iter.next();
            if (data.contains(pass)) ret.add(pass);
        }
        data = data.replaceAll("(?s)<!-- .*? -->", "").replaceAll("(?s)<script .*?>.*?</script>", "").replaceAll("(?s)<.*?>", "").replaceAll("Spoiler:", "").replaceAll("(no.{0,2}|kein.{0,8}|ohne.{0,8}|nicht.{0,8})(pw|passwort|password|pass)", "").replaceAll("(pw|passwort|password|pass).{0,12}(nicht|falsch|wrong)", "");
    
        Pattern pattern = Pattern.compile("(pw|passwort|password|pass)[\\s][\\s]*?[\"']([[^\\:\"'\\s]][^\"'\\s]*)[\"']?", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(data);
        while (matcher.find()) {
            String pass = matcher.group(2);
            if (pass.length() > 2 && !pass.matches(".*(rar|zip|jpg|gif|png|html|php|avi|mpg)$") && !ret.contains(pass)) ret.add(pass);
        }
        pattern = Pattern.compile("(pw|passwort|password|pass)[\\s][\\s]*?([[^\\:\"'\\s]][^\"'\\s]*)[\\s]?", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(data);
        while (matcher.find()) {
            String pass = matcher.group(2);
            if (pass.length() > 4 && !pass.matches(".*(rar|zip|jpg|gif|png|html|php|avi|mpg)$") && !ret.contains(pass)) ret.add(pass);
        }
        pattern = Pattern.compile("(pw|passwort|password|pass)[\\s]?\\:[\\s]*?[\"']([^\"']+)[\"']?", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(data);
        while (matcher.find()) {
            String pass = matcher.group(2);
            if (pass.length() > 2 && !pass.matches(".*(rar|zip|jpg|gif|png|html|php|avi|mpg)$") && !ret.contains(pass)) ret.add(pass);
        }
        pattern = Pattern.compile("(pw|passwort|password|pass)[\\s]?\\:[\\s]*?([^\"'\\s]+)[\\s]?", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(data);
        while (matcher.find()) {
            String pass = matcher.group(2);
            if (pass.length() > 2 && !pass.matches(".*(rar|zip|jpg|gif|png|html|php|avi|mpg)$") && !ret.contains(pass)) ret.add(pass);
        }
    
        return ret;
    }

    /**
     * Gibt alle links die in data gefunden wurden als Stringliste zurück
     * 
     * @param data
     * @return STringliste
     */
    public static String getHttpLinkList(String data) {
        String[] links = getHttpLinks(data, "%HOST%");
        String ret = "";
        for (int i = 0; i < links.length; i++) {
            ret += "\"" + links[i] + "\"\r\n";
        }
        return ret;
    }

    /**
     * Sucht alle Links heraus
     * 
     * @param data
     *            ist der Quelltext einer Html-Datei
     * @param url
     *            der Link von dem der Quelltext stammt (um die base automatisch
     *            zu setzen)
     * @return Linkliste aus data extrahiert
     */
    /*
     * 
     * public static void testGetHttpLinks() throws IOException { String input =
     * ""; String thisLine; BufferedReader br = new BufferedReader(new
     * FileReader("index.html")); while ((thisLine = br.readLine()) != null)
     * input += thisLine + "\n"; String[] dd = getHttpLinks(input,
     * "http://www.google.de/"); for (int i = 0; i < dd.length; i++)
     * System.out.println(dd[i]); }
     */
    public static String[] getHttpLinks(String data, String url) {
        String[] protocols = new String[] { "h.{2,3}", "https", "ccf", "dlc", "ftp" };
        String protocolPattern = "(";
        for (int i = 0; i < protocols.length; i++) {
            protocolPattern += protocols[i] + ((i + 1 == protocols.length) ? ")" : "|");
        }
    
        String[] patternStr = { "(?s)<[ ]?base[^>]*?href=['\"]([^>]*?)['\"]", "(?s)<[ ]?base[^>]*?href=([^'\"][^\\s]*)", "(?s)<[ ]?a[^>]*?href=['\"]([^>]*?)['\"]", "(?s)<[ ]?a[^>]*?href=([^'\"][^\\s]*)", "(?s)<[ ]?form[^>]*?action=['\"]([^>]*?)['\"]", "(?s)<[ ]?form[^>]*?action=([^'\"][^\\s]*)", "www[^\\s>'\"\\)]*", protocolPattern + "://[^\\s>'\"\\)]*" };
        url = url == null ? "" : url;
        Matcher m;
        String link;
        Pattern[] pattern = new Pattern[patternStr.length];
        for (int i = 0; i < patternStr.length; i++) {
            pattern[i] = Pattern.compile(patternStr[i], Pattern.CASE_INSENSITIVE);
        }
        String basename = "";
        String host = "";
        LinkedList<String> set = new LinkedList<String>();
        for (int i = 0; i < 2; i++) {
            m = pattern[i].matcher(data);
            if (m.find()) {
                url = JDUtilities.htmlDecode(m.group(1));
                break;
            }
        }
        if (url != null) {
            url = url.replace("http://", "");
            int dot = url.lastIndexOf('/');
            if (dot != -1)
                basename = url.substring(0, dot + 1);
            else
                basename = "http://" + url + "/";
            dot = url.indexOf('/');
            if (dot != -1)
                host = "http://" + url.substring(0, dot);
            else
                host = "http://" + url;
            url = "http://" + url;
        } else
            url = "";
        for (int i = 2; i < 6; i++) {
            m = pattern[i].matcher(data);
            while (m.find()) {
                link = JDUtilities.htmlDecode(m.group(1));
                link = link.replaceAll(protocols[0] + "://", "http://");
                link = link.replaceAll("https?://.*http://", "http://");
                for (int j = 1; j < protocols.length; j++) {
                    link = link.replaceAll("https?://.*" + protocols[j] + "://", protocols[j] + "://");
                }
    
                if ((link.length() > 6) && (link.substring(0, 7).equals("http://")))
                    ;
                else if (link.length() > 0) {
                    if (link.length() > 2 && link.substring(0, 3).equals("www")) {
                        link = "http://" + link;
                    }
                    if (link.charAt(0) == '/') {
                        link = host + link;
                    } else if (link.charAt(0) == '#') {
                        link = url + link;
                    } else {
                        link = basename + link;
                    }
                }
                if (!set.contains(link)) {
                    set.add(link);
                }
            }
        }
        data = data.replaceAll("(?s)<.*?>", "");
        m = pattern[6].matcher(data);
        while (m.find()) {
            link = "http://" + m.group();
            link = JDUtilities.htmlDecode(link);
            link = link.replaceAll(protocols[0] + "://", "http://");
            link = link.replaceFirst("^www\\..*" + protocols[0] + "://", "http://");
            link = link.replaceAll("https?://.*http://", "http://");
            for (int j = 1; j < protocols.length; j++) {
                link = link.replaceFirst("^www\\..*" + protocols[j] + "://", protocols[j] + "://");
            }
            if (!set.contains(link)) {
                set.add(link);
            }
        }
        m = pattern[7].matcher(data);
        while (m.find()) {
            link = m.group();
            link = JDUtilities.htmlDecode(link);
            link = link.replaceAll(protocols[0] + "://", "http://");
            link = link.replaceAll("https?://.*http://", "http://");
            for (int j = 1; j < protocols.length; j++) {
                link = link.replaceAll("https?://.*" + protocols[j] + "://", protocols[j] + "://");
            }
            // .replaceFirst("h.*?://",
            // "http://").replaceFirst("http://.*http://", "http://");
            if (!set.contains(link)) {
                set.add(link);
            }
        }
        return (String[]) set.toArray(new String[set.size()]);
    }
  
}

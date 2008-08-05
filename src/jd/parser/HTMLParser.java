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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.plugins.Plugin;
import jd.utils.JDUtilities;

public class HTMLParser {

    /**
     * Diese Methode sucht nach passwörtern in einem Datensatz
     * 
     * @param data
     * @return
     */
    public static Vector<String> findPasswords(String data) {
        if (data == null) { return new Vector<String>(); }
        Vector<String> ret = new Vector<String>();
        data = data.replaceAll("(?s)<!-- .*? -->", "").replaceAll("(?s)<script .*?>.*?</script>", "").replaceAll("(?s)<.*?>", "").replaceAll("Spoiler:", "").replaceAll("(no.{0,2}|kein.{0,8}|ohne.{0,8}|nicht.{0,8})(pw|passwort|password|pass)", "").replaceAll("(pw|passwort|password|pass).{0,12}(nicht|falsch|wrong)", "");

        Pattern pattern = Pattern.compile("(pw|passwort|password|pass)[\\s][\\s]*?[\"']([[^\\:\"'\\s]][^\"'\\s]*)[\"']?", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(data);
        while (matcher.find()) {
            String pass = matcher.group(2);
            if (pass.length() > 2 && !pass.matches(".*(rar|zip|jpg|gif|png|html|php|avi|mpg)$") && !ret.contains(pass)) {
                ret.add(pass);
            }
        }
        pattern = Pattern.compile("(pw|passwort|password|pass)[\\s][\\s]*?([[^\\:\"'\\s]][^\"'\\s]*)[\\s]?", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(data);
        while (matcher.find()) {
            String pass = matcher.group(2);
            if (pass.length() > 4 && !pass.matches(".*(rar|zip|jpg|gif|png|html|php|avi|mpg)$") && !ret.contains(pass)) {
                ret.add(pass);
            }
        }
        pattern = Pattern.compile("(pw|passwort|password|pass)[\\s]?\\:[\\s]*?[\"']([^\"']+)[\"']?", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(data);
        while (matcher.find()) {
            String pass = matcher.group(2);
            if (pass.length() > 2 && !pass.matches(".*(rar|zip|jpg|gif|png|html|php|avi|mpg)$") && !ret.contains(pass)) {
                ret.add(pass);
            }
        }
        pattern = Pattern.compile("(pw|passwort|password|pass)[\\s]?\\:[\\s]*?([^\"'\\s]+)[\\s]?", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(data);
        while (matcher.find()) {
            String pass = matcher.group(2);
            if (pass.length() > 2 && !pass.matches(".*(rar|zip|jpg|gif|png|html|php|avi|mpg)$") && !ret.contains(pass)) {
                ret.add(pass);
            }
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
        return Plugin.joinMap(HTMLParser.getInputHiddenFields(data), "=", "&");
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
        String pat = new Regex(data, startPattern + "(.*?)" + lastPattern).getFirstMatch();
        if (pat == null) { return null; }
        return HTMLParser.getFormInputHidden(pat);
    }

    /**
     * Gibt alle links die in data gefunden wurden als Stringliste zurück
     * 
     * @param data
     * @return STringliste
     */
    public static String getHttpLinkList(String data) {
        String[] links = HTMLParser.getHttpLinks(data, null);
        String ret = "";
        for (String element : links) {
            ret += "\"" + element + "\"\r\n";
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
    public static String[] getHttpLinks(String data, String url) {
        String[] protocols = new String[] { "h.{2,3}", "https", "ccf", "dlc", "ftp" };
        String protocolPattern = "(";
        for (int i = 0; i < protocols.length; i++) {
            protocolPattern += protocols[i] + (i + 1 == protocols.length ? ")" : "|");
        }

        url = url == null ? "" : url;
        Matcher m;
        String link;
        String basename = "";
        String host = "";
        LinkedList<String> set = new LinkedList<String>();
        Pattern[] basePattern = new Pattern[] { Pattern.compile("(?s)<[ ]?base[^>]*?href='(.*?)'", Pattern.CASE_INSENSITIVE), Pattern.compile("(?s)<[ ]?base[^>]*?href=\"(.*?)\"", Pattern.CASE_INSENSITIVE), Pattern.compile("(?s)<[ ]?base[^>]*?href=([^'\"][^\\s]*)", Pattern.CASE_INSENSITIVE), };
        for (Pattern element : basePattern) {
            m = element.matcher(data);
            if (m.find()) {
                url = JDUtilities.htmlDecode(m.group(1));
                break;
            }
        }
        if (url != null) {
            url = url.replace("http://", "");
            int dot = url.lastIndexOf('/');
            if (dot != -1) {
                basename = url.substring(0, dot + 1);
            } else {
                basename = "http://" + url + "/";
            }
            dot = url.indexOf('/');
            if (dot != -1) {
                host = "http://" + url.substring(0, dot);
            } else {
                host = "http://" + url;
            }
            url = "http://" + url;
        } else {
            url = "";
        }

        Pattern[] linkAndFormPattern = new Pattern[] { Pattern.compile("(?s)<[ ]?a[^>]*?href=\"(.*?)\"", Pattern.CASE_INSENSITIVE), Pattern.compile("(?s)\\[link\\](.*?)\\[/link\\]", Pattern.CASE_INSENSITIVE), Pattern.compile("(?s)\\[url\\](.*?)\\[/url\\]", Pattern.CASE_INSENSITIVE), Pattern.compile("(?s)<[ ]?a[^>]*?href='(.*?)'", Pattern.CASE_INSENSITIVE), Pattern.compile("(?s)<[ ]?a[^>]*?href=([^'\"][^\\s]*)", Pattern.CASE_INSENSITIVE), Pattern.compile("(?s)<[ ]?form[^>]*?action=\"(.*?)\"", Pattern.CASE_INSENSITIVE), Pattern.compile("(?s)<[ ]?form[^>]*?action='(.*?)'", Pattern.CASE_INSENSITIVE), Pattern.compile("(?s)<[ ]?form[^>]*?action=([^'\"][^\\s]*)", Pattern.CASE_INSENSITIVE), };
        for (Pattern element : linkAndFormPattern) {
            m = element.matcher(data);
            while (m.find()) {
                link = JDUtilities.htmlDecode(m.group(1));
                link = link.replaceAll(protocols[0] + "://", "http://");
                if (link.length() > 6 && link.substring(0, 7).equals("http://")) {
                    ;
                } else if (link.length() > 0) {
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
        data = data.replaceAll("(?s)\\[(url|link)\\].*?\\[/(url|link)\\]", "");
        m = Pattern.compile("www\\.[^\\s\"]*", Pattern.CASE_INSENSITIVE).matcher(data);
        while (m.find()) {
            link = "http://" + m.group();
            link = JDUtilities.htmlDecode(link);
            link = link.replaceAll(protocols[0] + "://", "http://");
            if (!set.contains(link)) {
                set.add(link);
            }
        }
        m = Pattern.compile(protocolPattern + "://[^\\s\"]*", Pattern.CASE_INSENSITIVE).matcher(data);
        while (m.find()) {
            link = m.group();
            link = JDUtilities.htmlDecode(link);
            link = link.replaceAll(protocols[0] + "://", "http://");
            if (!set.contains(link)) {
                set.add(link);
            }
        }
        return set.toArray(new String[set.size()]);
    }

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
            if (matcher3.find() && iscompl) {
                value = matcher3.group(1);
            } else if (matcher5.find() && iscompl) {
                value = matcher5.group(1);
            } else {
                iscompl = false;
            }
            ret.put(key, value);
        }
        return ret;
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
        return HTMLParser.getInputHiddenFields(new Regex(data, startPattern + "(.*?)" + lastPattern).getFirstMatch());
    }
}

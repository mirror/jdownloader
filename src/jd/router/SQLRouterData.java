//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.router;

import java.util.HashMap;
import java.util.Map.Entry;

import jd.parser.Regex;

public class SQLRouterData {

    public static String setPlaceHolder(String data) {
        if (data == null) return null;
        String reg = new Regex(data, "(\\&aa|password|pws|passwor|pass|\\@\\&ps|^p1|^t1|pswd)=([^\\&]*?)([\\s]+\\[\\[\\[| HTTP|$)").getMatch(1);
        String pwpat = "%%%pass%%%";
        if (reg != null && reg.length() == 32 && reg.matches("[a-zA-Z0-9]*")) pwpat = "MD5PasswordL(%%%pass%%%)";
        data = data.replaceAll("(?is)(\\&aa|password|pws|passwor|pass|\\@\\&ps|^p1|^t1|pswd)=([^\\&$]*?)([\\s]+\\[\\[\\[|\\[\\[\\[| HTTP|$)", "$1=" + pwpat + "$3").replaceAll("(?is)(username|(?<!router)name|user)\\=([^\\&]*?)([\\s]+\\[\\[\\[|\\[\\[\\[| HTTP|$)", "$1=%%%user%%%$3").replaceAll("(?is)=NAME([^\\&]*)\\&PASSWOR", "=NAME%%%user%%%&PASSWOR").replaceAll("(?is)RC=@D=([^\\=]*)=([^\\=]*) HTTP", "RC=@D=%%%pass%%%=%%%user%%% HTTP").replaceAll("(?is)RC=@D([^\\=].*?) HTTP", "RC=@D%%%pass%%%%%%user%%% HTTP");
        return data;
    }

    public static String replaceTimeStamps(String data) {
        if (data == null) return null;
        return data.replaceAll("[A-Z][a-z]{1,2}, \\d{2} [A-Z][a-z]{1,2} \\d{4} \\d{2}:\\d{2}:\\d{2}( [A-Z]{2,3})", "");
    }

    public static boolean haveUpnpReconnect(HashMap<String, String> UPnPSCPDs) {
        if (UPnPSCPDs == null) return false;
        for (Entry<String, String> element : UPnPSCPDs.entrySet()) {
            if (element.getValue().contains("ForceTermination")) return true;
        }
        return false;
    }

    public static String[] getNameFormUPnPSCPDs(HashMap<String, String> UPnPSCPDs) {
        if (UPnPSCPDs == null) return null;
        for (Entry<String, String> element : UPnPSCPDs.entrySet()) {
            if (element.getValue().contains("<modelName>")) {
                String model = new Regex(element.getValue(), "<modelName>(.*?)</modelName>").getMatch(0);
                String mac = new Regex(element.getValue(), "<UDN>.*?-.*?-.*?-.*?-([a-zA-Z0-9]{12})").getMatch(0);
                if (mac != null) mac = mac.replaceAll("..", "$0:").substring(0, 8);
                String version = null;
                version = new Regex(element.getValue(), "<modelNumber>(.*?)</modelNumber>").getMatch(0);
                if (version == null) version = new Regex(element.getValue(), "<friendlyName>[^<]*" + model + " (.*?)[\r\n]?</friendlyName>").getMatch(0);
                return new String[] { model, version, mac };
            }
        }
        return null;
    }

}

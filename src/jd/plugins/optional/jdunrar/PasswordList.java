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

package jd.plugins.optional.jdunrar;

import java.util.ArrayList;
import java.util.LinkedList;

import jd.config.SubConfiguration;
import jd.parser.Regex;
import jd.utils.JDUtilities;

public class PasswordList {
    public static LinkedList<String> PASSWORDLIST;
    private static SubConfiguration CONFIG = null;
    private static ArrayList<String> LIST;
    public static final String PROPERTY_PASSWORDLIST = "PASSWORDLIST";

    private static SubConfiguration getConfig() {
        if (CONFIG == null) CONFIG = JDUtilities.getSubConfig(PROPERTY_PASSWORDLIST);
        return CONFIG;
    }

    public static void addPassword(String pw) {
        ArrayList<String> list = getPasswordList();
        list.remove(pw);
        list.add(0, pw);
    }

    @SuppressWarnings("unchecked")
    public static ArrayList<String> getPasswordList() {
        if (LIST != null) return LIST;

        LinkedList<String> oldList = (LinkedList<String>) JDUtilities.getSubConfig("unrarPasswords").getProperty("PASSWORDLIST");

        if (oldList != null) {
            JDUtilities.getSubConfig("unrarPasswords").setProperty("PASSWORDLIST", null);
            addPasswords(oldList);

            JDUtilities.getSubConfig("unrarPasswords").save();
            save();
        }
        ArrayList<String> list = new ArrayList<String>();
        String[] spl = Regex.getLines(getConfig().getStringProperty("LIST", ""));
        for (String pw : spl)
            list.add(pw);
        LIST = list;
        return list;
    }

    private static void addPasswords(LinkedList<String> list) {
        for (String pw : list) {
            addPassword(pw);
        }

    }

    public static void cleanList() {
        LIST=null;
        ArrayList<String> list = getPasswordList();
        ArrayList<String> newList = new ArrayList<String>();
        for (String pw : list) {
            if (newList.indexOf(pw) < 0) newList.add(pw);
        }
        LIST = newList;
        StringBuffer sb = new StringBuffer();
        for (String pw : getPasswordList()) {
            sb.append(pw + "\r\n");
        }
        getConfig().setProperty("LIST", sb.toString().trim());
    }

    public static void save() {

        StringBuffer sb = new StringBuffer();
        for (String pw : getPasswordList()) {
            sb.append(pw + "\r\n");
        }
        getConfig().setProperty("LIST", sb.substring(0, sb.length() - 2));
        getConfig().save();

    }

}

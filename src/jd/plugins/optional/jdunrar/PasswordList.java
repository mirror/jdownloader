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

    public static String[] passwordStringtoArray(String password) {
        if (password == null || password.matches("[\\s]*")) { return new String[] {}; }
        if (password.matches("[\\s]*\\{[\\s]*\".*\"[\\s]*\\}[\\s]*$")) {
            password = password.replaceFirst("[\\s]*\\{[\\s]*\"", "").replaceFirst("\"[\\s]*\\}[\\s]*$", "");
            return password.split("\"[\\s]*\\,[\\s]*\"");
        }
        return new String[] { password };
    }

    public static String passwordArrayToString(String[] passwords) {
        LinkedList<String> pws = new LinkedList<String>();
        for (int i = 0; i < passwords.length; i++) {
            if (!passwords[i].matches("[\\s]*") && !pws.contains(passwords[i])) {
                pws.add(passwords[i]);
            }
        }
        passwords = pws.toArray(new String[pws.size()]);
        if (passwords.length == 0) { return ""; }
        if (passwords.length == 1) { return passwords[0]; }

        int l = passwords.length - 1;

        String ret = "{\"";
        for (int i = 0; i < passwords.length; i++) {
            if (!passwords[i].matches("[\\s]*")) {
                ret += passwords[i] + (i == l ? "\"}" : "\",\"");
            }
        }
        return ret;

    }

    public static void addPassword(String pw) {
        ArrayList<String> list = getPasswordList();
        list.remove(pw);
        list.add(0, pw);
    }

    public static ArrayList<String> getPasswordList() {
        if (LIST != null) return LIST;
        ArrayList<String> list = new ArrayList<String>();
        String[] spl = Regex.getLines(getConfig().getStringProperty("LIST", ""));
        for (String pw : spl)
            list.add(pw);
        LIST = list;
        return list;
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

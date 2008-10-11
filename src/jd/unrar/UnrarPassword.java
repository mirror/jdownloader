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

package jd.unrar;

import java.util.LinkedList;

import jd.config.SubConfiguration;
import jd.utils.JDUtilities;

public class UnrarPassword {
    public static LinkedList<String> passwordlist;
    private static SubConfiguration configPasswords = JDUtilities.getSubConfig("unrarPasswords");
    private static final String PROPERTY_PASSWORDLIST = "PASSWORDLIST";

    public static void editPasswordlist(String[] passwords) {
        passwordlist = new LinkedList<String>();
        for (int i = 0; i < passwords.length; i++) {
            if (passwords[i] != null && !passwords[i].matches("[\\s]*") && !passwordlist.contains(passwords[i])) {
                passwordlist.add(passwords[i]);
            }
        }
        // JUnrar.makePasswordListUnique();
        UnrarPassword.savePasswordList();
    }
    
    private static void savePasswordList() {
        if (passwordlist != null && passwordlist.size() > 0) {
            configPasswords.setProperty(PROPERTY_PASSWORDLIST, passwordlist);
            configPasswords.save();
        }
    }

    public static String[] getPasswordArray(String password) {
        if (password == null || password.matches("[\\s]*")) { return new String[] {}; }
        if (password.matches("[\\s]*\\{[\\s]*\".*\"[\\s]*\\}[\\s]*$")) {
            password = password.replaceFirst("[\\s]*\\{[\\s]*\"", "").replaceFirst("\"[\\s]*\\}[\\s]*$", "");
            return password.split("\"[\\s]*\\,[\\s]*\"");
        }
        return new String[] { password };
    }

    public static LinkedList<String> getPasswordList() {
    	UnrarPassword.loadPasswordlist();
        return passwordlist;

    }

    @SuppressWarnings("unchecked")
    public static void loadPasswordlist() {
        if (passwordlist != null) { return; }
        passwordlist = (LinkedList<String>) configPasswords.getProperty(PROPERTY_PASSWORDLIST, null);
        if (passwordlist == null) {
            passwordlist = new LinkedList<String>();
        }
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

    public static String[] returnPasswords() {
    	UnrarPassword.loadPasswordlist();
        return passwordlist.toArray(new String[passwordlist.size()]);
    }


    public static void addToPasswordlist(String password) {
        if (passwordlist == null || passwordlist.size() < 1) {
        	UnrarPassword.loadPasswordlist();
        }
        String[] passwords = UnrarPassword.getPasswordArray(password);
        for (int i = 0; i < passwords.length; i++) {
            passwords[i] = passwords[i].trim();
            if (passwords[i] != null && !passwords[i].matches("[\\s]*") && !passwordlist.contains(passwords[i])) {
                passwordlist.add(passwords[i]);
            }
        }
        // JUnrar.makePasswordListUnique();
        UnrarPassword.savePasswordList();
    }

    public static void pushPasswordToTop(String password) {
        if (passwordlist == null || passwordlist.size() < 1) {
        	UnrarPassword.loadPasswordlist();
        }
        String[] passwords = UnrarPassword.getPasswordArray(password);
        for (int i = 0; i < passwords.length; i++) {
            passwords[i] = passwords[i].trim();
            if (passwords[i] != null && !passwords[i].matches("[\\s]*")) {
                passwordlist.remove(passwords[i]);
                passwordlist.addFirst(passwords[i]);
            }
        }
        UnrarPassword.savePasswordList();
    }

}

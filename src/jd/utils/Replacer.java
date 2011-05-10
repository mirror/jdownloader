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

package jd.utils;

import java.util.ArrayList;
import java.util.Calendar;

import jd.config.Configuration;
import jd.controlling.JDLogger;
import jd.controlling.JSonWrapper;
import jd.controlling.reconnect.ipcheck.IPController;
import jd.nutils.Formatter;
import jd.plugins.DownloadLink;

import org.appwork.utils.Application;
import org.jdownloader.translate.JDT;

/**
 * Diese Klasse stellt Methoden zur Verfügung um in einen String mit
 * Platzhaltern werte einzusetzen
 */
public final class Replacer {

    private static ArrayList<String[]> KEYS = null;

    public synchronized static String getKey(final int index) {
        if (Replacer.KEYS == null) {
            Replacer.initKeys();
        }
        return index >= Replacer.KEYS.size() ? null : Replacer.KEYS.get(index)[0];
    }

    public synchronized static String[] getKeyList() {
        if (Replacer.KEYS == null) {
            Replacer.initKeys();
        }
        final int size = Replacer.KEYS.size();
        final String[] keys = new String[size];
        for (int i = 0; i < size; i++) {
            final String[] key = Replacer.KEYS.get(i);
            keys[i] = "%" + key[0] + "%   (" + key[1] + ")";
        }
        return keys;
    }

    public static String getReplacement(final String key, final DownloadLink dLink) {

        if (key.startsWith("LAST_FINISHED_") && dLink == null) { return ""; }

        if (key.equalsIgnoreCase("LAST_FINISHED_PACKAGE.PASSWORD")) { return dLink.getFilePackage().getPassword(); }

        if (key.equalsIgnoreCase("LAST_FINISHED_PACKAGE.AUTO_PASSWORD")) { return dLink.getFilePackage().getPasswordAuto().toString(); }

        if (key.equalsIgnoreCase("LAST_FINISHED_PACKAGE.FILELIST")) { return dLink.getFilePackage().getDownloadLinkList().toString(); }

        if (key.equalsIgnoreCase("LAST_FINISHED_PACKAGE.PACKAGENAME")) {
            final String name = dLink.getFilePackage().getName();
            if (name == null || name.equals("") || name.equals(JDT._.controller_packages_defaultname())) {
                return dLink.getName();
            } else {
                return dLink.getFilePackage().getName();
            }
        }

        if (key.equalsIgnoreCase("LAST_FINISHED_PACKAGE.COMMENT")) { return dLink.getFilePackage().getComment(); }

        if (key.equalsIgnoreCase("LAST_FINISHED_PACKAGE.DOWNLOAD_DIRECTORY")) { return dLink.getFilePackage().getDownloadDirectory(); }

        if (key.equalsIgnoreCase("LAST_FINISHED_FILE.DOWNLOAD_PATH")) { return dLink.getFileOutput(); }

        if (key.equalsIgnoreCase("LAST_FINISHED_FILE.HOST")) { return dLink.getHost(); }

        if (key.equalsIgnoreCase("LAST_FINISHED_FILE.NAME")) { return dLink.getName(); }

        if (key.equalsIgnoreCase("LAST_FINISHED_FILE.FILESIZE")) { return dLink.getDownloadSize() + ""; }

        if (key.equalsIgnoreCase("LAST_FINISHED_FILE.AVAILABLE")) { return dLink.isAvailable() ? "YES" : "NO"; }

        if (key.equals("LAST_FINISHED_FILE.BROWSER_URL")) { return dLink.getBrowserUrl(); }

        if (key.equals("LAST_FINISHED_FILE.DOWNLOAD_URL")) { return dLink.getLinkType() == DownloadLink.LINKTYPE_CONTAINER ? "[Not allowed]" : dLink.getDownloadURL(); }

        if (key.equals("LAST_FINISHED_FILE.CHECKSUM")) {
            final StringBuilder sb = new StringBuilder();
            if (dLink.getSha1Hash() != null) {
                sb.append("SHA1: ").append(dLink.getSha1Hash());
            }
            if (dLink.getMD5Hash() != null) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append("MD5: ").append(dLink.getMD5Hash());
            }
            if (sb.length() > 0) { return sb.toString(); }
            return "[Not set]";
        }

        if (key.equalsIgnoreCase("SYSTEM.IP")) {
            if (JSonWrapper.get("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) {
                return "IPCheck disabled";
            } else {
                return IPController.getInstance().getIP().toString();
            }
        }

        if (key.equalsIgnoreCase("SYSTEM.DATE")) {
            final Calendar c = Calendar.getInstance();
            return Formatter.fillInteger(c.get(Calendar.DATE), 2, "0") + "." + Formatter.fillInteger((c.get(Calendar.MONTH) + 1), 2, "0") + "." + c.get(Calendar.YEAR);
        }

        if (key.equalsIgnoreCase("SYSTEM.TIME")) {
            final Calendar c = Calendar.getInstance();
            return Formatter.fillInteger(c.get(Calendar.HOUR_OF_DAY), 2, "0") + ":" + Formatter.fillInteger(c.get(Calendar.MINUTE), 2, "0") + ":" + Formatter.fillInteger(c.get(Calendar.SECOND), 2, "0");
        }

        if (key.equalsIgnoreCase("SYSTEM.JAVA_VERSION")) { return Application.getJavaVersion() + ""; }

        if (key.equalsIgnoreCase("JD.REVISION")) { return JDUtilities.getRevision(); }

        if (key.equalsIgnoreCase("JD.HOME_DIR")) { return JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath(); }

        return "";
    }

    private static void initKeys() {
        Replacer.KEYS = new ArrayList<String[]>();
        Replacer.KEYS.add(new String[] { "LAST_FINISHED_PACKAGE.PASSWORD", JDT._.replacer_password() });
        Replacer.KEYS.add(new String[] { "LAST_FINISHED_PACKAGE.AUTO_PASSWORD", JDT._.replacer_autopassword() });
        Replacer.KEYS.add(new String[] { "LAST_FINISHED_PACKAGE.FILELIST", JDT._.replacer_filelist() });
        Replacer.KEYS.add(new String[] { "LAST_FINISHED_PACKAGE.PACKAGENAME", JDT._.replacer_packagename() });
        Replacer.KEYS.add(new String[] { "LAST_FINISHED_PACKAGE.COMMENT", JDT._.replacer_comment() });
        Replacer.KEYS.add(new String[] { "LAST_FINISHED_PACKAGE.DOWNLOAD_DIRECTORY", JDT._.replacer_downloaddirectory() });
        Replacer.KEYS.add(new String[] { "LAST_FINISHED_FILE.DOWNLOAD_PATH", JDT._.replacer_filepath() });
        Replacer.KEYS.add(new String[] { "LAST_FINISHED_FILE.HOST", JDT._.replacer_hoster() });
        Replacer.KEYS.add(new String[] { "LAST_FINISHED_FILE.NAME", JDT._.replacer_filename() });
        Replacer.KEYS.add(new String[] { "LAST_FINISHED_FILE.FILESIZE", JDT._.replacer_filesize() });
        Replacer.KEYS.add(new String[] { "LAST_FINISHED_FILE.AVAILABLE", JDT._.replacer_available() });
        Replacer.KEYS.add(new String[] { "LAST_FINISHED_FILE.BROWSER_URL", JDT._.replacer_browserurl() });
        Replacer.KEYS.add(new String[] { "LAST_FINISHED_FILE.DOWNLOAD_URL", JDT._.replacer_downloadurl() });
        Replacer.KEYS.add(new String[] { "LAST_FINISHED_FILE.CHECKSUM", JDT._.replacer_checksum() });
        Replacer.KEYS.add(new String[] { "SYSTEM.IP", JDT._.replacer_ipaddress() });
        Replacer.KEYS.add(new String[] { "SYSTEM.DATE", JDT._.replacer_date() });
        Replacer.KEYS.add(new String[] { "SYSTEM.TIME", JDT._.replacer_time() });
        Replacer.KEYS.add(new String[] { "SYSTEM.JAVA_VERSION", JDT._.replacer_javaversion() });
        Replacer.KEYS.add(new String[] { "JD.REVISION", JDT._.replacer_jdversion() });
        Replacer.KEYS.add(new String[] { "JD.HOME_DIR", JDT._.replacer_jdhomedirectory() });
    }

    public static String insertVariables(final String str, final DownloadLink dLink) {
        String ret = "";
        if (str != null) {
            ret = str;
            if (Replacer.KEYS == null) {
                Replacer.initKeys();
            }
            for (final String[] element : Replacer.KEYS) {
                if (str.indexOf("%" + element[0] + "%") >= 0) {
                    JDLogger.getLogger().finer("%" + element[0] + "% --> *****");
                    ret = Replacer.replace(ret, "%" + element[0] + "%", Replacer.getReplacement(element[0], dLink));
                }
            }
        }
        return ret;
    }

    private static String replace(final String in, final String remove, final String replace) {
        if (in == null || remove == null || remove.length() == 0) {
            return in;
        } else {
            final StringBuilder sb = new StringBuilder();
            int oldIndex = 0;
            int newIndex = 0;
            final int remLength = remove.length();
            while ((newIndex = in.indexOf(remove, oldIndex)) > -1) {
                sb.append(in.substring(oldIndex, newIndex));
                sb.append(replace);

                oldIndex = newIndex + remLength;
            }

            final int inLength = in.length();
            if (oldIndex < inLength) {
                sb.append(in.substring(oldIndex, inLength));
            }
            return sb.toString();
        }
    }

    /**
     * Don't let anyone instantiate this class.
     */
    private Replacer() {
    }

}
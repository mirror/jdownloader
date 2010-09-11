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
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.controlling.reconnect.IPCheck;
import jd.nutils.Formatter;
import jd.plugins.DownloadLink;
import jd.utils.locale.JDL;

/**
 * Diese Klasse stellt Methoden zur Verfügung um in einen String mit
 * Platzhaltern werte einzusetzen
 */
public final class Replacer {

    /**
     * Don't let anyone instantiate this class.
     */
    private Replacer() {
    }

    private static ArrayList<String[]> KEYS = null;

    public static String[] getKeyList() {
        if (KEYS == null) {
            Replacer.initKeys();
        }
        final int size = KEYS.size();
        final String[] keys = new String[size];
        for (int i = 0; i < size; i++) {
            final String[] key = KEYS.get(i);
            keys[i] = "%" + key[0] + "%   (" + key[1] + ")";
        }
        return keys;
    }

    public static String getKey(final int index) {
        if (KEYS == null) {
            Replacer.initKeys();
        }
        return index >= KEYS.size() ? null : KEYS.get(index)[0];
    }

    private static void initKeys() {
        KEYS = new ArrayList<String[]>();
        KEYS.add(new String[] { "LAST_FINISHED_PACKAGE.PASSWORD", JDL.L("replacer.password", "Last finished package: Password") });
        KEYS.add(new String[] { "LAST_FINISHED_PACKAGE.AUTO_PASSWORD", JDL.L("replacer.autopassword", "Last finished package: Auto Password") });
        KEYS.add(new String[] { "LAST_FINISHED_PACKAGE.FILELIST", JDL.L("replacer.filelist", "Last finished package: Filelist") });
        KEYS.add(new String[] { "LAST_FINISHED_PACKAGE.PACKAGENAME", JDL.L("replacer.packagename", "Last finished package: Packagename") });
        KEYS.add(new String[] { "LAST_FINISHED_PACKAGE.COMMENT", JDL.L("replacer.comment", "Last finished package: Comment") });
        KEYS.add(new String[] { "LAST_FINISHED_PACKAGE.DOWNLOAD_DIRECTORY", JDL.L("replacer.downloaddirectory", "Last finished package: Download Directory") });
        KEYS.add(new String[] { "LAST_FINISHED_FILE.DOWNLOAD_PATH", JDL.L("replacer.filepath", "Last finished File: Filepath") });
        KEYS.add(new String[] { "LAST_FINISHED_FILE.HOST", JDL.L("replacer.hoster", "Last finished File: Hoster") });
        KEYS.add(new String[] { "LAST_FINISHED_FILE.NAME", JDL.L("replacer.filename", "Last finished File: Filename") });
        KEYS.add(new String[] { "LAST_FINISHED_FILE.FILESIZE", JDL.L("replacer.filesize", "Last finished File: Filesize") });
        KEYS.add(new String[] { "LAST_FINISHED_FILE.AVAILABLE", JDL.L("replacer.available", "Last finished File: is Available (Yes,No)") });
        KEYS.add(new String[] { "LAST_FINISHED_FILE.BROWSER_URL", JDL.L("replacer.browserurl", "Last finished File: Browser-URL") });
        KEYS.add(new String[] { "LAST_FINISHED_FILE.DOWNLOAD_URL", JDL.L("replacer.downloadurl", "Last finished File: Download-URL (only for non-container links)") });
        KEYS.add(new String[] { "LAST_FINISHED_FILE.CHECKSUM", JDL.L("replacer.checksum", "Last finished File: Checksum (SHA1/MD5) if set by hoster") });
        KEYS.add(new String[] { "SYSTEM.IP", JDL.L("replacer.ipaddress", "Current IP Address") });
        KEYS.add(new String[] { "SYSTEM.DATE", JDL.L("replacer.date", "Current Date") });
        KEYS.add(new String[] { "SYSTEM.TIME", JDL.L("replacer.time", "Current Time") });
        KEYS.add(new String[] { "SYSTEM.JAVA_VERSION", JDL.L("replacer.javaversion", "Used Java Version") });
        KEYS.add(new String[] { "JD.REVISION", JDL.L("replacer.jdversion", "jDownloader: Revision/Version") });
        KEYS.add(new String[] { "JD.HOME_DIR", JDL.L("replacer.jdhomedirectory", "jDownloader: Homedirectory/Installdirectory") });
    }

    public static String getReplacement(final String key, final DownloadLink dLink) {

        if (key.startsWith("LAST_FINISHED_") && dLink == null) return "";

        if (key.equalsIgnoreCase("LAST_FINISHED_PACKAGE.PASSWORD")) return dLink.getFilePackage().getPassword();

        if (key.equalsIgnoreCase("LAST_FINISHED_PACKAGE.AUTO_PASSWORD")) return dLink.getFilePackage().getPasswordAuto().toString();

        if (key.equalsIgnoreCase("LAST_FINISHED_PACKAGE.FILELIST")) return dLink.getFilePackage().getDownloadLinkList().toString();

        if (key.equalsIgnoreCase("LAST_FINISHED_PACKAGE.PACKAGENAME")) {
            final String name = dLink.getFilePackage().getName();
            if (name == null || name.equals("") || name.equals(JDL.L("controller.packages.defaultname", "various"))) {
                return dLink.getName();
            } else {
                return dLink.getFilePackage().getName();
            }
        }

        if (key.equalsIgnoreCase("LAST_FINISHED_PACKAGE.COMMENT")) return dLink.getFilePackage().getComment();

        if (key.equalsIgnoreCase("LAST_FINISHED_PACKAGE.DOWNLOAD_DIRECTORY")) return dLink.getFilePackage().getDownloadDirectory();

        if (key.equalsIgnoreCase("LAST_FINISHED_FILE.DOWNLOAD_PATH")) return dLink.getFileOutput();

        if (key.equalsIgnoreCase("LAST_FINISHED_FILE.HOST")) return dLink.getHost();

        if (key.equalsIgnoreCase("LAST_FINISHED_FILE.NAME")) return dLink.getName();

        if (key.equalsIgnoreCase("LAST_FINISHED_FILE.FILESIZE")) return dLink.getDownloadSize() + "";

        if (key.equalsIgnoreCase("LAST_FINISHED_FILE.AVAILABLE")) return dLink.isAvailable() ? "YES" : "NO";

        if (key.equals("LAST_FINISHED_FILE.BROWSER_URL")) return dLink.getBrowserUrl();

        if (key.equals("LAST_FINISHED_FILE.DOWNLOAD_URL")) return dLink.getLinkType() == DownloadLink.LINKTYPE_CONTAINER ? "[Not allowed]" : dLink.getDownloadURL();

        if (key.equals("LAST_FINISHED_FILE.CHECKSUM")) {
            StringBuilder sb = new StringBuilder();
            if (dLink.getSha1Hash() != null) {
                sb.append("SHA1: ").append(dLink.getSha1Hash());
            }
            if (dLink.getMD5Hash() != null) {
                if (sb.length() > 0) sb.append(' ');
                sb.append("MD5: ").append(dLink.getMD5Hash());
            }
            if (sb.length() > 0) return sb.toString();
            return "[Not set]";
        }

        if (key.equalsIgnoreCase("SYSTEM.IP")) {
            if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) {
                return "IPCheck disabled";
            } else {
                return IPCheck.getIPAddress().toString();
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

        if (key.equalsIgnoreCase("SYSTEM.JAVA_VERSION")) return JDUtilities.getJavaVersion().toString();

        if (key.equalsIgnoreCase("JD.REVISION")) return JDUtilities.getRevision();

        if (key.equalsIgnoreCase("JD.HOME_DIR")) return JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath();

        return "";
    }

    public static String insertVariables(final String str, DownloadLink dLink) {
        String ret = "";
        if (str != null) {
            ret = str;
            if (KEYS == null) {
                Replacer.initKeys();
            }
            for (String[] element : KEYS) {
                if (str.indexOf("%" + element[0] + "%") >= 0) {
                    JDLogger.getLogger().finer("%" + element[0] + "%" + " --> *****");
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

}
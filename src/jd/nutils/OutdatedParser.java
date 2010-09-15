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

package jd.nutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OutdatedParser {

    // DO not use Regex class to stay compatible to Restarter.jar
    public static String[] getLines(final String arg) {
        if (arg == null) {
            return new String[] {};
        } else {
            final String[] temp = arg.split("[\r\n]{1,2}");
            final int tempLength = temp.length;
            final String[] output = new String[tempLength];
            for (int i = 0; i < tempLength; i++) {
                output[i] = temp[i].trim();
            }
            return output;
        }
    }

    private static String getLocalFile(final File file) {
        if (file == null) { return null; }
        if (!file.exists()) { return ""; }
        try {
            final BufferedReader f = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));

            String line;
            final StringBuffer ret = new StringBuffer();
            final String sep = System.getProperty("line.separator");
            while ((line = f.readLine()) != null) {
                ret.append(line + sep);
            }
            f.close();
            return ret.toString();
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static boolean parseFile(final File outdated) {
        final String[] remove = OutdatedParser.getLines(OutdatedParser.getLocalFile(outdated));
        final String homedir = outdated.getParent();
        boolean ret = true;
        File delete;
        File[] deletes;
        if (remove != null) {
            for (final String file : remove) {
                if (file.length() == 0) {
                    continue;
                }
                if (!file.matches(".*?" + File.separator + "?\\.+" + File.separator + ".*?")) {
                    if (file.contains("|")) {
                        final String[] split = file.split("\\|");
                        delete = new File(homedir, split[0]);
                        if (!delete.exists()) {
                            continue;
                        }
                        deletes = delete.listFiles(new FileFilter() {

                            public boolean accept(final File pathname) {
                                return pathname.getName().matches(split[1]);
                            }

                        });
                        for (final File del : deletes) {
                            if (OutdatedParser.removeDirectoryOrFile(del)) {
                                System.out.println("Removed " + del.getName() + " [" + file + "]");
                            } else {
                                ret = false;
                                System.out.println("FAILED to remove " + del.getName() + " [" + file + "]");
                            }
                        }
                    } else if (file.contains("<NO_HASH>")) {
                        delete = new File(homedir, file.substring(0, file.indexOf("<NO_HASH>")));
                        if (!delete.exists() || !delete.isDirectory()) {
                            continue;
                        }
                        final ArrayList<String> hashes = OutdatedParser.parseHashList(new File(outdated.getParentFile(), "tmp/hashlist.lst"));
                        if (hashes == null) {
                            continue;
                        }
                        deletes = delete.listFiles(new FileFilter() {

                            public boolean accept(final File pathname) {
                                return !hashes.contains(pathname.getAbsolutePath().replace(outdated.getParent(), "").replaceAll("\\\\", "/"));
                            }

                        });
                        for (final File del : deletes) {
                            if (OutdatedParser.removeDirectoryOrFile(del)) {
                                System.out.println("Removed " + del.getName() + " [" + file + "]");
                            } else {
                                ret = false;
                                System.out.println("FAILED to remove " + del.getName() + " [" + file + "]");
                            }
                        }
                    } else {
                        delete = new File(homedir, file);
                        if (!delete.exists()) {
                            continue;
                        }
                        if (OutdatedParser.removeDirectoryOrFile(delete)) {
                            System.out.println("Removed " + file);
                        } else {
                            ret = false;
                            System.out.println("FAILED to remove " + file);
                        }
                    }
                }
            }
        }
        return ret;
    }

    private static ArrayList<String> parseHashList(final File file) {
        if (!file.exists()) {
            System.out.println("HashList not available");
            return null;
        }
        /*
         * Do not use regex to keep Restarter.jar free from appwork utils
         */
        final Matcher matcher = Pattern.compile("[\r\n\\;]*([^=]+)=(.*?)\\;", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(OutdatedParser.getLocalFile(file));

        final ArrayList<String> ar = new ArrayList<String>();
        while (matcher.find()) {
            ar.add(matcher.group(1));
        }
        final String[] matches = ar.toArray(new String[ar.size()]);

        final ArrayList<String> result = new ArrayList<String>();
        for (final String match : matches) {
            result.add(match);
        }
        return result;
    }

    private static boolean removeDirectoryOrFile(final File dir) {
        if (dir.isDirectory()) {
            final String[] children = dir.list();
            for (final String element : children) {
                final boolean success = OutdatedParser.removeDirectoryOrFile(new File(dir, element));
                if (!success) { return false; }
            }
        }
        return dir.delete();
    }

}

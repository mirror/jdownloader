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

import jd.parser.Regex;

public class OutdatedParser {

    public static boolean parseFile(final File outdated) {
        String[] remove = Regex.getLines(getLocalFile(outdated));
        String homedir = outdated.getParent();
        boolean ret = true;
        File delete;
        File[] deletes;
        if (remove != null) {
            for (String file : remove) {
                if (file.length() == 0) continue;
                if (!file.matches(".*?" + File.separator + "?\\.+" + File.separator + ".*?")) {
                    if (file.contains("|")) {
                        final String[] split = file.split("\\|");
                        delete = new File(homedir, split[0]);
                        if (!delete.exists()) continue;
                        deletes = delete.listFiles(new FileFilter() {

                            public boolean accept(File pathname) {
                                return pathname.getName().matches(split[1]);
                            }

                        });
                        for (File del : deletes) {
                            if (removeDirectoryOrFile(del)) {
                                System.out.println("Removed " + del.getName() + " [" + file + "]");
                            } else {
                                ret = false;
                                System.out.println("FAILED to remove " + del.getName() + " [" + file + "]");
                            }
                        }
                    } else if (file.contains("<NO_HASH>")) {
                        delete = new File(homedir, file.substring(0, file.indexOf("<NO_HASH>")));
                        if (!delete.exists() || !delete.isDirectory()) continue;
                        final ArrayList<String> hashes = parseHashList(new File(outdated.getParentFile(), "tmp/hashlist.lst"));
                        if (hashes == null) continue;
                        deletes = delete.listFiles(new FileFilter() {

                            public boolean accept(File pathname) {
                                return !hashes.contains(pathname.getAbsolutePath().replace(outdated.getParent(), "").replaceAll("\\\\", "/"));
                            }

                        });
                        for (File del : deletes) {
                            if (removeDirectoryOrFile(del)) {
                                System.out.println("Removed " + del.getName() + " [" + file + "]");
                            } else {
                                ret = false;
                                System.out.println("FAILED to remove " + del.getName() + " [" + file + "]");
                            }
                        }
                    } else {
                        delete = new File(homedir, file);
                        if (!delete.exists()) continue;
                        if (removeDirectoryOrFile(delete)) {
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

    private static String getLocalFile(File file) {
        if (file == null) return null;
        if (!file.exists()) return "";
        try {
            BufferedReader f = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));

            String line;
            StringBuffer ret = new StringBuffer();
            String sep = System.getProperty("line.separator");
            while ((line = f.readLine()) != null) {
                ret.append(line + sep);
            }
            f.close();
            return ret.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    private static boolean removeDirectoryOrFile(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String element : children) {
                boolean success = removeDirectoryOrFile(new File(dir, element));
                if (!success) return false;
            }
        }
        return dir.delete();
    }

    private static ArrayList<String> parseHashList(File file) {
        if (!file.exists()) {
            System.out.println("HashList not available");
            return null;
        }

        String[] matches = new Regex(getLocalFile(file), "[\r\n\\;]*([^=]+)=(.*?)\\;").getColumn(0);
        ArrayList<String> result = new ArrayList<String>();
        for (String match : matches) {
            result.add(match);
        }
        return result;
    }

}

package jd.utils.locale;

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

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.controlling.JDLogger;
import jd.event.MessageEvent;
import jd.event.MessageListener;
import jd.nutils.io.JDIO;
import jd.utils.JDUtilities;

import org.appwork.utils.Regex;
import org.appwork.utils.event.Eventsender;

public class SrcParser {

    private final File                                 root;
    private final int                                  rootLen;
    private Eventsender<MessageListener, MessageEvent> broadcaster;

    public static void deleteCache() {
        File dir = JDUtilities.getResourceFile("tmp/lfe/cache/");
        if (dir == null) return;
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) return;
        for (File file : files) {
            file.delete();
        }
    }

    public SrcParser(File resourceFile) {
        this.root = resourceFile;
        this.rootLen = root.getAbsolutePath().length() + 1;
        this.entries = new ArrayList<LngEntry>();

        this.broadcaster = new Eventsender<MessageListener, MessageEvent>() {

            @Override
            protected void fireEvent(MessageListener listener, MessageEvent event) {
                listener.onMessage(event);
            }

        };

        pattern.add("sys\\.warning\\.dlcerror\\.(.+?)");
    }

    public Eventsender<MessageListener, MessageEvent> getBroadcaster() {
        return broadcaster;
    }

    public ArrayList<LngEntry> getEntries() {
        return entries;
    }

    private ArrayList<LngEntry> entries;
    private File                currentFile;
    private String              currentFileName;
    private String              currentContent;
    private ArrayList<String>   pattern = new ArrayList<String>();
    private File                shortF;

    public ArrayList<String> getPattern() {
        return pattern;
    }

    public void parse() {
        for (File f : getSourceFiles(root)) {
            parseFile(f);
        }
    }

    @SuppressWarnings("unchecked")
    /**
     * parses an java file and writes all JDL Matches to entries and pattern. this method uses a cache to be faster
     */
    private void parseFile(File file) {
        this.currentFile = file;
        this.currentFileName = file.getName();
        this.currentFileName = this.currentFileName.substring(0, this.currentFileName.length() - 5);

        broadcaster.fireEvent(new MessageEvent(this, 0, "Parse " + file.getAbsolutePath().substring(rootLen)));

        /* Dont parse the code of the SrcParser. */
        if (currentFileName.equals("SrcParser")) return;

        // find all lines containing JDL calls
        currentContent = JDIO.readFileToString(file);

        ArrayList<LngEntry> fileEntries = new ArrayList<LngEntry>();
        ArrayList<String> filePattern = new ArrayList<String>();

        prepareContent();
        currentContent = Pattern.compile("\\/\\*(.*?)\\*\\/", Pattern.DOTALL).matcher(currentContent).replaceAll("[[/*.....*/]]");
        currentContent = Pattern.compile("[^:]//(.*?)[\n|\r]", Pattern.DOTALL).matcher(currentContent).replaceAll("[[\\.....]]");
        currentContent = Pattern.compile("JDL\\s*?\\.\\s*?L", Pattern.DOTALL).matcher(currentContent).replaceAll("JDL.L");
        String[] matches = new Regex(currentContent, "([^;^{^}]*JDL\\.LF?\\s*?\\(.*?\\)[^;^{^}]*)").getColumn(0);

        for (String match : matches) {
            // splitting all calls.
            parseCodeLine(match, fileEntries, filePattern);
        }
        if (entries.size() > 0) {

            File trans = getTranslationPath(file);
            shortF = new File(trans, "T.java");
            String name = trans.getParentFile().getName();
            name = name.substring(0, 1).toUpperCase() + name.substring(1);
            File interF = new File(trans, name + "Translation.java");
            String cont = JDIO.readFileToString(file);
            for (LngEntry l : entries) {

                ConvertEntry e = InterfaceCache.get(trans).getMap().get(l.getKey());
                if (e == null) {
                    e = new ConvertEntry(l.getKey(), l.getValue());
                    InterfaceCache.get(trans).getMap().put(l.getKey(), e);
                }
                e.addFile(file);

            }
            entries.clear();
        }

    }

    private File getTranslationPath(File lookup) {
        String path = lookup.getPath().replace("\\", "/").substring(4);
        String[] folders = lookup.getPath().split("[\\\\/]");
        if ("captcha".equals(folders[2])) { return new File(root, "jd/captcha/translate"); }
        if (path.startsWith("jd/controlling/reconnect/plugins/")) { return new File(root, "jd/controlling/reconnect/plugins/" + folders[5] + "/translate"); }
        if (path.startsWith("jd/gui/") || path.startsWith("org/jdownloader/gui/")) return new File(root, "org/jdownloader/gui/translate");
        if (path.startsWith("jd/plugins/decrypter/")) return lookup;
        if (path.startsWith("jd/plugins/hoster/")) return lookup;
        if (path.startsWith("org/jdownloader/extensions/") && folders.length >= 6) return new File(root, "org/jdownloader/extensions/" + folders[4] + "/translate");

        return new File(root, "org/jdownloader/translate");

    }

    private void prepareContent() {

        String cl = this.currentFile.getAbsolutePath();
        cl = cl.replace("\\", "/");
        cl = cl.substring(cl.indexOf("/jd/") + 1).replace('/', '.');
        cl = cl.replace(".java", "");
        currentContent = currentContent.replace("this.getClass().getName()", "\"" + cl + "\"");
        currentContent = currentContent.replace("getClass().getName()", "\"" + cl + "\"");
        String simple = cl.substring(cl.lastIndexOf(".") + 1);
        currentContent = currentContent.replace("this.getClass().getSimpleName()", "\"" + simple + "\"");
        currentContent = currentContent.replace("getClass().getSimpleName()", "\"" + simple + "\"");
        currentContent = currentContent.replace("\"+\"", "");

    }

    /**
     * Returns all javafiles in dir. works recursive
     * 
     * @param dir
     * @return
     */
    private ArrayList<File> getSourceFiles(File dir) {
        ArrayList<File> files = new ArrayList<File>();

        if (dir != null) {
            for (File file : dir.listFiles()) {
                if (file.isDirectory()) {
                    files.addAll(getSourceFiles(file));
                } else if (file.getName().toLowerCase().endsWith(".java")) {
                    files.add(file);
                }
            }
        }

        return files;
    }

    /**
     * Finds lngentries and patterns in this codeline. adds all to global list
     * and to locallists fileEntries and filePattern
     * 
     * @param match
     * @param fileEntries
     * @param filePattern
     */
    private void parseCodeLine(String match, ArrayList<LngEntry> fileEntries, ArrayList<String> filePattern) {
        String[] calls = match.split("JDL\\.");
        String pat_string = "\"(.*?)(?<!\\\\)\"";
        LngEntry entry;
        String m;
        String[] strings;
        main: for (String orgm : calls) {
            m = orgm;
            m = m.trim();
            if (m.startsWith("L")) {
                strings = new Regex(m, pat_string).getColumn(0);

                m = m.replace("\r", "");
                m = m.replace("\n", "");
                m = m.replaceAll(pat_string, "%%%S%%%");
                m = m.replace(" ", "");

                orgm = m;

                try {
                    int com1 = m.indexOf(",");
                    int end = m.indexOf(")", com1 + 1);
                    int com2;
                    com2 = m.indexOf(",", com1 + 1);
                    if (com2 > 0 && com2 < end) end = com2;
                    if (end < 0) {
                        m = m.substring(m.indexOf("(") + 1).trim();
                    } else {
                        m = m.substring(m.indexOf("(") + 1, end).trim();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                while (m.length() > 0 && m.charAt(m.length() - 1) == ',') {
                    m = m.substring(0, m.length() - 1);
                }
                if (m == null || m.length() == 0) {
                    continue;
                }
                m = m.replace("%%%+%%%", "%%%%%%");
                String[] parameter = m.split(",");

                if (orgm.startsWith("LF ") || orgm.startsWith("LF(")) {
                    if (orgm.substring(2).trim().charAt(0) != '(') {
                        JDLogger.getLogger().severe("Malformated translation value in " + currentFile + " : " + m);
                        continue;
                    }
                    if (parameter.length != 2) {
                        JDLogger.getLogger().severe("Malformated translation pair (inner functions?) in " + currentFile + " : " + match);
                        continue;
                    }
                    int i = 0;
                    if (parameter[1].contains("+")) {
                        JDLogger.getLogger().severe("Malformated translation value in " + currentFile + " : " + match);
                        continue;
                    }

                    /*
                     * merge expressions
                     */
                    while (parameter[0].contains("+")) {
                        try {
                            String[][] matches = new Regex(parameter[0], "(\\+([^%]+)\\+?)").getMatches();
                            for (String[] mm : matches) {
                                try {
                                    String value = getValueOf(mm[1]);
                                    parameter[0] = parameter[0].replace(mm[0], value);
                                } catch (Exception e) {
                                    JDLogger.getLogger().severe("LF1 Malformated translation key in " + currentFile + " : " + match);
                                    break main;
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            break;
                        }

                        try {
                            String[][] matches = new Regex(parameter[0], "(\\+?([^%]+)\\+)").getMatches();
                            for (String[] mm : matches) {
                                try {
                                    String value = getValueOf(mm[1]);
                                    parameter[0] = parameter[0].replace(mm[0], value);
                                } catch (Exception e) {
                                    JDLogger.getLogger().severe("LF2 Malformated translation key in " + currentFile + " : " + match);
                                    break main;
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            break;
                        }
                    }

                    for (int x = 0; x < parameter.length; x++) {
                        while (parameter[x].contains("%%%S%%%")) {
                            parameter[x] = parameter[x].replaceFirst("%%%S%%%", Matcher.quoteReplacement(strings[i++]));
                        }
                    }

                    String error;
                    if ((error = new Regex(parameter[0], "([\\(\\)\\{\\}\\/\\\\\\$\\&\\+\\~\\#\\\"\\!\\?]+)").getMatch(0)) != null) {
                        int index = parameter[0].indexOf(error);
                        if (index >= 0) {
                            JDLogger.getLogger().warning("Unsupported chars (" + parameter[0].substring(0, index) + "<< |" + parameter[0].substring(index + 1) + ") in key:" + currentFile + " : " + parameter[0]);
                        } else {
                            JDLogger.getLogger().warning("Unsupported chars in key: " + currentFile + " : " + parameter[0]);
                        }
                        continue;
                    }
                    if (!parameter[0].contains(".")) {
                        JDLogger.getLogger().warning("Prob. Malformated translation key in " + currentFile + " : " + match);
                    }
                    if (parameter[0].contains("null")) {
                        JDLogger.getLogger().warning("Prob. Malformated translation key in " + currentFile + " : " + match);
                    }
                    entry = new LngEntry(parameter[0], parameter[1]);
                    if (!entries.contains(entry)) {
                        entries.add(entry);
                    }
                    if (!fileEntries.contains(entry)) {
                        fileEntries.add(entry);
                    }
                } else if (orgm.startsWith("L ") || orgm.startsWith("L(")) {
                    if (orgm.substring(1).trim().charAt(0) != '(') {
                        JDLogger.getLogger().severe("Malformated translation value in " + currentFile + " : " + m);
                        continue;
                    }
                    if (parameter.length != 2) {
                        JDLogger.getLogger().severe("Malformated translation pair (inner functions?) in " + currentFile + " : " + match);
                        continue;
                    }
                    int i = 0;
                    if (parameter[1].contains("+")) {
                        JDLogger.getLogger().severe("Malformated translation value in " + currentFile + " : " + match);
                        continue;
                    }

                    /*
                     * merge expressions
                     */
                    while (parameter[0].contains("+")) {
                        try {
                            String[][] matches = new Regex(parameter[0], "(\\+([^%]+)\\+?)").getMatches();
                            for (String[] mm : matches) {
                                try {
                                    String value = getValueOf(mm[1]);
                                    parameter[0] = parameter[0].replace(mm[0], value);
                                } catch (Exception e) {
                                    JDLogger.getLogger().severe("L1 Malformated translation key in " + currentFile + " : " + match);
                                    parameter[0] = parameter[0].replace(mm[0], "*");
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            break;
                        }

                        try {
                            String[][] matches = new Regex(parameter[0], "(\\+?([^%]+)\\+)").getMatches();
                            for (String[] mm : matches) {
                                try {
                                    String value = getValueOf(mm[1]);
                                    parameter[0] = parameter[0].replace(mm[0], value);
                                } catch (Exception e) {
                                    JDLogger.getLogger().severe("L2 Malformated translation key in " + currentFile + " : " + match);
                                    break main;
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            break;
                        }
                    }

                    for (int x = 0; x < parameter.length; x++) {
                        while (parameter[x].contains("%%%S%%%")) {
                            parameter[x] = parameter[x].replaceFirst("%%%S%%%", Matcher.quoteReplacement(strings[i++]));
                        }
                    }

                    String error;
                    if ((error = new Regex(parameter[0], "([\\(\\)\\{\\}\\/\\\\\\$\\&\\+\\~\\#\\\"\\!\\?]+)").getMatch(0)) != null) {
                        int index = parameter[0].indexOf(error);
                        if (index >= 0) {
                            JDLogger.getLogger().warning("Unsupported chars (" + parameter[0].substring(0, index) + "<< |" + parameter[0].substring(index + 1) + ") in key:" + currentFile + " : " + parameter[0]);
                        } else {
                            JDLogger.getLogger().warning("Unsupported chars in key: " + currentFile + " : " + parameter[0]);
                        }
                        continue;
                    }
                    if (!parameter[0].contains(".")) {
                        JDLogger.getLogger().warning("Prob. Malformated translation key in " + currentFile + " : " + match);
                        break;
                    }
                    if (parameter[0].contains("null")) {
                        JDLogger.getLogger().warning("Prob. Malformated translation key in " + currentFile + " : " + match);
                        break;
                    }

                    if (parameter[0].contains("*")) {
                        if (currentFile.getName().contains("ToolBarAction")) continue;
                        String patt = parameter[0].replace(".", "\\.").replace("*", "(.+?)");
                        JDLogger.getLogger().severe("Pattern match in " + currentFile + " : " + match);
                        if (!pattern.contains(patt)) pattern.add(patt);
                        if (!filePattern.contains(patt)) filePattern.add(patt);
                    } else {
                        entry = new LngEntry(parameter[0], parameter[1]);

                        if (!entries.contains(entry)) {
                            entries.add(entry);
                        }

                        if (!fileEntries.contains(entry)) {
                            fileEntries.add(entry);
                        }
                    }
                }

            }
        }
    }

    private String getValueOf(String variable) {
        /*
         * Workaround for JDL prefixes with static access. Simple remove the
         * static prefix!
         */
        variable = variable.replace(currentFileName + ".", "");

        String[] matches = new Regex(currentContent, variable + "\\s*=(.*?);").getColumn(0);
        String ret = matches[matches.length - 1].trim();
        while (ret.startsWith("\"")) {
            ret = ret.substring(1);
        }
        while (ret.endsWith("\"")) {
            ret = ret.substring(0, ret.length() - 1);
        }

        return ret;
    }
}

package jd.plugins.optional.langfileeditor;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.controlling.JDLogger;
import jd.event.JDBroadcaster;
import jd.event.MessageEvent;
import jd.event.MessageListener;
import jd.nutils.Formatter;
import jd.nutils.io.JDIO;
import jd.parser.Regex;
import jd.utils.JDUtilities;

import org.tmatesoft.svn.core.SVNException;

public class SrcParser {

    public static final int PARSE_NEW_FILE = 0;
    public static final int PARSE_NEW_ENTRY = 1;
    private File root;
    private JDBroadcaster<MessageListener, MessageEvent> broadcaster;

    public SrcParser(File resourceFile) {
        this.root = resourceFile;
        this.entries = new ArrayList<LngEntry>();
        this.broadcaster = new JDBroadcaster<MessageListener, MessageEvent>() {

            @Override
            protected void fireEvent(MessageListener listener, MessageEvent event) {
                listener.onMessage(event);
            }

        };
    }

    public JDBroadcaster<MessageListener, MessageEvent> getBroadcaster() {
        return broadcaster;
    }

    public ArrayList<LngEntry> getEntries() {
        return entries;
    }

    public static void main(String[] args) throws SVNException {
        new SrcParser(JDUtilities.getResourceFile("tmp/lfe/src/")).parse();

    }

    private ArrayList<LngEntry> entries;
    private File currentFile;
    private String currentContent;

    public void parse() {

        for (File f : getSourceFiles(root)) {
            parseFile(f);
        }

    }

    private void parseFile(File file) {
        this.currentFile = file;
        broadcaster.fireEvent(new MessageEvent(this, PARSE_NEW_FILE, "Parse " + file.getAbsolutePath()));

        // find all lines containing JDLocale calls
        currentContent = JDIO.getLocalFile(file);
        prepareContent();
        currentContent = Pattern.compile("\\/\\*(.*?)\\*\\/", Pattern.DOTALL).matcher(currentContent).replaceAll("[[/*.....*/]]");
        currentContent = Pattern.compile("[^:]//(.*?)[\n|\r]", Pattern.DOTALL).matcher(currentContent).replaceAll("[[\\.....]]");

        // TODO: Hiermit wird auch JDLocale.LOCALEID gemachted ...
        String[] matches = new Regex(currentContent, "([^;^{^}]*JDLocale\\.L.*?\\(.*?\\)[^;^{^}]*)").getColumn(0);

        for (String match : matches) {
            // splitting all calls.
            parseCodeLine(match);

        }

    }

    private void prepareContent() {
        if (this.currentContent.contains("jd.gui.skins.simple.startmenu.actions;")) {
            String menukey = new Regex(currentContent, "super\\(\"(.*?)\",\\s*\".*?\"\\);").getMatch(0);
            if (menukey != null) {
                currentContent = currentContent.replaceFirst("super\\(\"(.*?)\",\\s*\".*?\"\\);", "[[...]]");
                currentContent += "\r\nJDLocale.L(\"gui.menu." + menukey + ".name\",\"" + menukey + "\");";
                currentContent += "\r\nJDLocale.L(\"gui.menu." + menukey + ".mnem\",\"-\");";
                currentContent += "\r\nJDLocale.L(\"gui.menu." + menukey + ".accel\",\"-\");";
            }
        } else if (this.currentContent.contains("jd.gui.skins.simple.startmenu;")) {
            String menukey = new Regex(currentContent, "super\\(\"(.*?)\",\\s*\".*?\"\\);").getMatch(0);
            currentContent = currentContent.replaceFirst("super\\(\"(.*?)\",\\s*\".*?\"\\);", "[[...]]");
            currentContent += "\r\nJDLocale.L(\"" + menukey + "\",\"" + menukey + "\");";

        }

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

    private void parseCodeLine(String match) {
        String[] calls = match.split("JDLocale");
        String pat_string = "\"(.*?)(?<!\\\\)\"";

        LngEntry entry;
        String m;
        main: for (String orgm : calls) {
            m = orgm;
            m = m.trim();
            if (m.startsWith(".L")) {

                String[] strings = new Regex(m, pat_string).getColumn(0);
                m = m.replace("\r", "");
                m = m.replace("\n", "");
              
                m = m.replaceAll(pat_string, "%%%S%%%");

                m = m.replace(" ", "");
                orgm=m;
                m = new Regex(m, "\\((.*?\\,.*?)[\\)\\,]").getMatch(0);
                if (m == null) {
//                    JDLogger.getLogger().severe("unknown: " + orgm);
                    continue;
                }
                String[] parameter = m.split(",");
             
                if (orgm.startsWith(".LF")) {
                    
                    if (orgm.substring(3).trim().charAt(0) != '(') {

                        JDLogger.getLogger().severe("Mailformated translation value in " + currentFile + " : " + m);
                        continue;
                    }
                    if (parameter.length !=2) {

                        JDLogger.getLogger().severe("Mailformated translation pair (inner functions?) in " + currentFile + " : " + match);
                        continue;
                    }
                    int i = 0;
                    if (parameter[1].contains("+")) {
                        JDLogger.getLogger().severe("Mailformated translation value in " + currentFile + " : " + match);
                        continue;
                    }

                    /*
                     * merge expressions
                     */
                    merge: while (parameter[0].contains("+")) {
                        try {
                            String[][] matches = new Regex(parameter[0], "(\\+(\\w+)\\+?)").getMatches();
                            for (String[] mm : matches) {
                                try {
                                    String value = getValueOf(mm[1]);
                                    parameter[0] = parameter[0].replace(mm[0], value);

                                } catch (Exception e) {

                                    JDLogger.getLogger().severe("Mailformated translation key in " + currentFile + " : " + match);
                                    break main;
                                }
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                            break;
                        }

                        try {
                            String[][] matches = new Regex(parameter[0], "(\\+?(\\w+)\\+)").getMatches();
                            for (String[] mm : matches) {
                                try {
                                    String value = getValueOf(mm[1]);
                                    parameter[0] = parameter[0].replace(mm[0], value);

                                } catch (Exception e) {
                                    JDLogger.getLogger().severe("Mailformated translation key in " + currentFile + " : " + match);
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
                    if ((error=new Regex(parameter[0],"([\\(\\)\\{\\}\\/\\\\\\$\\&\\+\\~\\#\\\"\\!\\?]+)").getMatch(0))!=null) {
                        
                        int index=parameter[0].indexOf(error);
                        if(index>=0){
                            JDLogger.getLogger().warning(" Unsupported chars ("+parameter[0].substring(0,index)+"<< |"+parameter[0].substring(index+1)+") in key:" + currentFile + " : " + parameter[0]);
                            
                        }else{
                            JDLogger.getLogger().warning(" Unsupported chars in key:" + currentFile + " : " + parameter[0]);
                             
                        }
                        continue;
                    }
                    if (!parameter[0].contains(".")) {
                        JDLogger.getLogger().warning(" Prob. Malformated translation key in " + currentFile + " : " + match);
                    }
                    if (parameter[0].contains("null")) {
                        JDLogger.getLogger().warning(" Prob. Malformated translation key in " + currentFile + " : " + match);
                    }
                    entry = new LngEntry(parameter[0], parameter[1]);
                    if (!hasEntry(entry)) {
                        entries.add(entry);
                        System.out.println("LF  "+Formatter.fillInteger(entries.size(), 3, "0") + " " + entry);
                        broadcaster.fireEvent(new MessageEvent(this, PARSE_NEW_ENTRY, "LF  "+Formatter.fillInteger(entries.size(), 3, "0") + " " + entry));

                    }
                } else if (orgm.startsWith(".L")) {
                  
                    if (orgm.substring(2).trim().charAt(0) != '(') {

                        JDLogger.getLogger().severe("Mailformated translation value in " + currentFile + " : " + m);
                        continue;
                    }
                    if (parameter.length != 2) {

                        JDLogger.getLogger().severe("Mailformated translation pair (inner functions?) in " + currentFile + " : " + match);
                        continue;
                    }
                    int i = 0;
                    if (parameter[1].contains("+")) {
                        JDLogger.getLogger().severe("Mailformated translation value in " + currentFile + " : " + match);
                        continue;
                    }

                    /*
                     * merge expressions
                     */
                    merge: while (parameter[0].contains("+")) {
                        try {
                            String[][] matches = new Regex(parameter[0], "(\\+(\\w+)\\+?)").getMatches();
                            for (String[] mm : matches) {
                                try {
                                    String value = getValueOf(mm[1]);
                                    parameter[0] = parameter[0].replace(mm[0], value);

                                } catch (Exception e) {

                                    JDLogger.getLogger().severe("Mailformated translation key in " + currentFile + " : " + match);
                                    break main;
                                }
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                            break;
                        }

                        try {
                            String[][] matches = new Regex(parameter[0], "(\\+?(\\w+)\\+)").getMatches();
                            for (String[] mm : matches) {
                                try {
                                    String value = getValueOf(mm[1]);
                                    parameter[0] = parameter[0].replace(mm[0], value);

                                } catch (Exception e) {
                                    JDLogger.getLogger().severe("Mailformated translation key in " + currentFile + " : " + match);
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
                    if ((error=new Regex(parameter[0],"([\\(\\)\\{\\}\\/\\\\\\$\\&\\+\\~\\#\\\"\\!\\?]+)").getMatch(0))!=null) {
                        
                        int index=parameter[0].indexOf(error);
                        if(index>=0){
                            JDLogger.getLogger().warning(" Unsupported chars ("+parameter[0].substring(0,index)+"<< |"+parameter[0].substring(index+1)+") in key:" + currentFile + " : " + parameter[0]);
                            
                        }else{
                            JDLogger.getLogger().warning(" Unsupported chars in key:" + currentFile + " : " + parameter[0]);
                             
                        }
                        continue;
                    }
                    if (!parameter[0].contains(".")) {
                        JDLogger.getLogger().warning(" Prob. Mailformated translation key in " + currentFile + " : " + match);
                    }
                    if (parameter[0].contains("null")) {
                        JDLogger.getLogger().warning(" Prob. Mailformated translation key in " + currentFile + " : " + match);
                    }
                    entry = new LngEntry(parameter[0], parameter[1]);
                    if (!hasEntry(entry)) {
                        entries.add(entry);
                        System.out.println("L   "+Formatter.fillInteger(entries.size(), 3, "0") + " " + entry);
                        broadcaster.fireEvent(new MessageEvent(this, PARSE_NEW_ENTRY, "L   "+Formatter.fillInteger(entries.size(), 3, "0") + " " + entry));

                    }
                }

            }
        }
    }

    private String getValueOf(String variable) {
        String[] matches = new Regex(currentContent, variable + "\\s*=(.*?);").getColumn(0);
        String ret = matches[matches.length - 1].trim();
        while (ret.startsWith("\""))
            ret = ret.substring(1);
        while (ret.endsWith("\""))
            ret = ret.substring(0, ret.length() - 1);

        return ret;

    }

    private boolean hasEntry(LngEntry entry) {
        for (LngEntry e : entries) {
            if (e.getKey().equalsIgnoreCase(entry.getKey())) return true;
        }
        return false;
    }
}

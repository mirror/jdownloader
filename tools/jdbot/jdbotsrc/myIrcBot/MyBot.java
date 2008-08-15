package myIrcBot;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import org.jibble.pircbot.*;

/**
 * Dieser teil des Programms steht unter Eula Lizenz Die Rechte an diesem
 * Programmteil/code obliegen dem Autor Das Programm/der Sourcecode darf nur vom
 * Autor verteilt werden!
 * 
 * @author DareWithDevil dwdaddhp@yahoo.de
 * 
 */
public class MyBot extends PircBot {

    private File config = new File("config.cfg");
    public String log = "ircLog.txt";
    public File messageFile = new File("messageLog.bin");
    public Settings settings = new Settings();
    public String[] Channel = new String[] { "#jdownloader", "#jDDev" };
    public String name = "jDownloader";
    public String password = "jd";
    public String server = "irc.freenode.org";
    private HashMap<Integer, String> serverResponse = new HashMap<Integer, String>();
    public int version = 1162;
    // public long lastping = System.currentTimeMillis();
    public Vector<BotMessage> messages = new Vector<BotMessage>();
    public boolean delayedMsg = false, delayedUserMsg = false;

    // 
    // protected void onServerPing(String response) {
   
    // lastping = System.currentTimeMillis();
    // super.onServerPing(response);
    // }

    @SuppressWarnings("unchecked")
    public MyBot() {
        this.setName(name);
        this.setLogin(name);
        if (config.exists()) {
            settings = (Settings) Utilities.loadObject(config);
        }
        if (messageFile.exists()) {
            messages = (Vector<BotMessage>) Utilities.loadObject(messageFile);
        }
        /*
         * new Thread(new Runnable() {
         * 
         * public void run() { while (true) { if ((System.currentTimeMillis() -
         * lastping) > 200000) try { reconnect(); } catch
         * (NickAlreadyInUseException e) { 
         * e.printStackTrace(); } catch (IOException e) { // TODO Auto-generated
         * catch block e.printStackTrace(); } catch (IrcException e) { // TODO
         * Auto-generated catch block e.printStackTrace(); } } } }).start();
         */
        new Thread(new Runnable() {

            public void run() {
                while (true) {
                    try {
                        int oldRev = settings.svnRev;
                        String svninfo = getSvn();
                        if (oldRev < settings.svnRev) {
                            for (int i = 0; i < Channel.length; i++) {
                                sendMessage(Channel[i], svninfo);
                            }

                        }
                        Thread.sleep(1200000);
                    } catch (Exception e) {
                        // TODO: handle exception
                    }

                }
            }
        }).start();

    }

    
    protected void onDisconnect() {
        while (!isConnected()) {
            try {
                reconnect();
                joinChannels();
            } catch (Exception e) {
                // Couldn't reconnect!
                // Pause for a short while...?
            }
        }
    }

    public void joinChannels() {
        // Join the #pircbot channel.
        for (int i = 0; i < Channel.length; i++) {
            joinChannel(Channel[i]);

        }
        if (!getNick().equals(name)) {
            sendMessage("NickServ", "GHOST " + name + " " + password);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                
                e.printStackTrace();
            }
            changeNick(name);
            identify(password);
        }
    }

    public String getSvn() {
        RequestInfo post;
        try {
            post = Utilities.getRequest(new URL("http://www.syncom.org/projects/jdownloader/timeline"), null, null, false);
            String[] svn = post.getRegexp("<h2>([\\d]+)/([\\d]+)/[\\d]+:</h2><dl><dt class=\"changeset\"><a href=\".*?\"><span class=\"time\">([\\d]+):([\\d]+)</span> Changeset <em>.([\\d]+).</em> by (.*?)</a></dt><dd class=\"changeset\">(.*?)</dd>").getMatches()[0];
            settings.svnRev = Integer.parseInt(svn[4]);
            Utilities.saveObject(settings, config);
            return "Es gab eine Quellcodeänderung von " + svn[5] + " am " + svn[1] + "." + svn[0] + " um " + svn[2] + ":" + svn[3] + " Uhr " + (svn[6].endsWith("--") ? "" : svn[5] + ": \"" + svn[6].replaceAll("[\n\r]", " ").replaceAll("\\&hellip\\;", "...").replaceAll("<a class=(.*?)>", "").replaceAll("[\\?]?</a>", "") + "\" ") + ((settings.svnRev > version) ? " es wird bald die Version 0." + ((double) settings.svnRev) / 1000 + " oder höher verfügbar sein!" : "");
        } catch (MalformedURLException e) {
            
            e.printStackTrace();
        } catch (IOException e) {
            
            e.printStackTrace();
        }
        return null;
    }

    public void onJoin(final String channel, final String sender, final String login, final String hostname) {

        new Thread(new Runnable() {

            public void run() {
                try {
                    for (int i = 0; i < settings.autoVoice.size(); i++) {
                        if (settings.autoVoice.get(i).equals(sender)) {
                            if (userRights(sender, channel) > 0) voice(channel, sender);
                        }
                    }
                    for (int i = 0; i < settings.autoOp.size(); i++) {
                        if (settings.autoOp.get(i).equals(sender)) {
                            if (userRights(sender, channel) > 0) op(channel, sender);
                        }
                    }
                    for (int i = 0; i < settings.joinMessages.size(); i++) {
                        try {
                            String[] mess = settings.joinMessages.get(i);
                            if (sender.matches("(?is).*" + mess[0] + ".*")) {
                                boolean nouser = false;
                                String nachri = mess[1];
                                if (nachri.matches(".*!voice .*")) {
                                    if (!logedin(sender, channel)) return;
                                    nachri = nachri.replaceFirst("!voice ", "");
                                }
                                if (nachri.matches(".*!op .*")) {
                                    nachri = nachri.replaceFirst("!op ", "");
                                    if (userRights(sender, channel) != UserInfo.OP) return;
                                }
                                if (nachri.matches(".*!nouser .*")) {
                                    nachri = nachri.replaceFirst("!nouser ", "");
                                    nouser = true;
                                }
                                nachri = nachri.replaceAll("%%user%%", sender);
                                if (nachri.matches(".*%%cmd%%.*")) {
                                    try {
                                        String reg = new Regexp(nachri, "%%cmd%%(.*)").getMatch(0);
                                        onMessage(channel, sender, login, hostname, reg);
                                        return;
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                }
                                if (nachri.matches(".*%%raw%%.*")) {
                                    try {
                                        String reg = new Regexp(nachri, "%%raw%%(.*)").getMatch(0);
                                        sendRawLine(reg);
                                        return;
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                }
                                if (nouser)
                                    sendMassmes(sender, channel, nachri, 0);
                                else
                                    sendMassmes(sender, channel, sender + ": " + nachri, 0);

                            }
                        } catch (Exception e) {
                            e.printStackTrace();

                        }

                    }

                } catch (Exception e) {
                    e.printStackTrace();

                }

            }
        }).start();

    }

    public boolean fastLogedin(String sender, String channel) {
        User[] users = getUsers(channel);
        for (int i = 0; i < users.length; i++) {
            if (users[i].equals(sender)) {
                if (users[i].isOp() || users[i].hasVoice()) return true;
                break;
            }
        }
        return false;
    }

    private File getLog(String channel) {
        return new File(channel + log);
    }

    @SuppressWarnings("deprecation")
    public void onMessage(final String channel, final String sender, final String login, final String hostname, final String message) {
        new Thread(new Runnable() {

            @SuppressWarnings("unchecked")
            public void run() {
                try {
                    final String ch2 = (channel.equals(sender)) ? Channel[0] : channel;
                    // System.out.println(message);
                    if (message.matches("[\\s]*!addprem .*:.*\\@.*") || new Regexp(message, "[\\s]*﻿﻿Account[\\s]*(created|angelegt)[\\s]*.LOGIN.(.*?)[\\s]*(PASSWORT|PASSWORD)(.*?)[\\s]*.(Please.write.down|Bitte notieren).").count() > 0) {
                        String[] info;
                        // boolean b =
                        // message.matches("(?is)[\\s]*﻿﻿Account.(created|angelegt)..LOGIN..*?.(PASSWORT|PASSWORD).*?..(Please.write.down|Bitte
                        // notieren)..[\\s]*"
                        // System.out.println(message);
                        if (new Regexp(message, "[\\s]*Account[\\s]*(created|angelegt[\\s]*.LOGIN.(.*?)[\\s]*(PASSWORT|PASSWORD)(.*?)[\\s]*.(Please.write.down|Bitte notieren).").count() > 0) {
                            String[] rs = new Regexp(message, "[\\s]*﻿﻿Account[\\s]*(created|angelegt)[\\s]*.LOGIN.(.*?)[\\s]*(PASSWORT|PASSWORD)(.*?)[\\s]*.(Please.write.down|Bitte notieren).").getMatches()[0];
                            info = new String[] { rs[1], rs[3], "rapidshare.com" };
                        } else
                            info = new Regexp(message, "[\\s]*!addprem[\\s]*(.*?)[\\s]?:[\\s]?(.*?)[\\s]?\\@[\\s]?(.*)").getMatches()[0];
                        if (info == null)
                            return;
                        else if (settings.premAccs.contains(info)) {
                            sendMassmes(sender, sender, "der premiumaccount ist schon in der liste", 0);
                            return;
                        }
                        if (userRights(sender, ch2) != UserInfo.OP) {
                            sendMassmes(sender, sender, "diese funktion dürfen nur ops verwenden", 0);
                            return;
                        }
                        settings.premAccs.add(info);
                        Utilities.saveObject(settings, config);
                        sendMassmes(sender, sender, info[0] + ":" + info[1] + " wurde zu den Premiumaccounts hinzugefügt", 0);
                        return;
                    }

                    if (delayedUserMsg) delayedUserMsg = false;
                    if (delayedMsg && fastLogedin(sender, ch2)) delayedMsg = false;
                    if (message.matches("[\\s]*!last [\\d]+")) {
                        int n = Integer.parseInt(new Regexp(message, "last ([\\d]+)").getMatch(0));
                        String[] logr = Utilities.getLocalFile(getLog(channel)).split("\r\n");
                        if (n > 30) {
                            sendMassmes(sender, sender, "es sind maximal 30 Nachrichten erlaubt verwenden sie stattdessen !log", 0);
                            return;
                        }
                        String out = "";
                        int i = 0;
                        if (logr.length > n) i = logr.length - n;
                        for (; i < logr.length; i++) {
                            if (n-- == 0) break;
                            out += logr[i] + "\r\n";
                        }
                        sendMassmes(sender, sender, out, 0);
                        return;
                    } else if (message.matches("[\\s]*!log[\\s]*") && logedin(sender, ch2)) {
                        try {
                            dccSendFile(getLog(channel), sender, 10000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        return;
                    } else if (message.matches("[\\s]*!ident.*")) {
                        for (int i = 0; i < settings.autoVoice.size(); i++) {
                            if (settings.autoVoice.get(i).equals(sender)) {
                                if (userRights(sender, ch2) > 0) voice(channel, sender);
                            }
                        }
                        for (int i = 0; i < settings.autoOp.size(); i++) {
                            if (settings.autoOp.get(i).equals(sender)) {
                                if (userRights(sender, ch2) > 0) op(channel, sender);
                            }
                        }
                        return;
                    } else if (message.matches("[\\s]*!svn[\\s]*.*")) {
                        if (message.matches("[\\s]*!svn[\\s]*rev[\\s]*[\\d]+") && logedin(sender, ch2)) {
                            version = Integer.parseInt(new Regexp(message, "[\\s]*!svn[\\s]*rev[\\s]*([\\d]+)").getMatch(0));
                        } else {
                            sendMassmes(sender, channel, getSvn(), 0);
                        }
                        return;
                    }
                    if (message.matches("[\\s]*!rm ignore.*")) {
                        if (!logedin(sender, ch2)) return;
                        try {
                            if (message.matches("[\\s]*!rm ignore[\\s]*[\\d]+")) {
                                int n = Integer.parseInt(new Regexp(message, "[\\s]*!rm ignore[\\s]*([\\d]+)").getMatch(0));
                                sendMassmes(sender, sender, settings.ignore.get(n) + " wurde aus der ignoreierliste entfernt", 0);
                                settings.ignore.remove(n);
                                Utilities.saveObject(settings, config);
                            } else if (message.matches("[\\s]*!rm ignore[\\s]*[^\\s].*")) {
                                String name = new Regexp(message, "[\\s]*!rm ignore[\\s]*(.*)").getMatch(0).trim();
                                if (settings.ignore.contains(name)) {
                                    settings.ignore.remove(name);
                                    sendMassmes(sender, sender, name + " wurde aus der ignoreierliste entfernt", 0);
                                } else {
                                    for (int i = 0; i < settings.ignore.size(); i++) {
                                        String ob = settings.ignore.get(i);
                                        if (ob.matches("(?is).*" + name + ".*")) {
                                            settings.ignore.remove(ob);
                                            sendMassmes(sender, sender, ob + " wurde aus der ignoreierliste entfernt", 0);
                                        }
                                    }
                                }
                                Utilities.saveObject(settings, config);
                            } else {
                                String names = "Namen die ignoriert werden:";
                                for (int i = 0; i < settings.ignore.size(); i++) {
                                    names += "\r\n[" + i + "]: " + settings.ignore.get(i);
                                }
                                sendMassmes(sender, sender, names, 0, 0);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();

                            String names = "Namen die ignoriert werden:";
                            for (int i = 0; i < settings.ignore.size(); i++) {
                                names += "\r\n[" + i + "]: " + settings.ignore.get(i);
                            }
                            sendMassmes(sender, sender, names, 0, 0);
                        }
                        return;
                    }

                    else if (message.matches("[\\s]*!shutdown[\\s]*")) {
                        if (userRights(sender, ch2) != UserInfo.OP) {
                            sendMassmes(sender, sender, "diese funktion dürfen nur ops verwenden", 0);
                            return;
                        }
                        sendMassmes(sender, sender, "bot wird beendet", 0);
                        System.exit(0);
                    } else if (message.matches("[\\s]*!dellog[\\s]*")) {
                        if (!logedin(sender, ch2)) return;
                        getLog(channel).delete();
                        sendMassmes(sender, sender, "die logdatei wurde gelöscht", 0);
                        return;
                    } else if (message.matches("[\\s]*!del[\\s]+.*")) {
                        if (!logedin(sender, ch2)) return;
                        try {
                            if (message.matches("[\\s]*!del[\\s]*[\\d]+")) {
                                int n = Integer.parseInt(new Regexp(message, "[\\s]*!del[\\s]*([\\d]+)").getMatch(0));
                                sendMassmes(sender, sender, messages.get(n) + " :" + " wurde aus der Nachrichtenliste entfernt", 0);
                                messages.remove(n);
                                Utilities.saveObject(messages, messageFile);
                            } else if (message.matches("[\\s]*!del[\\s]*[^\\s].*")) {
                                String name = new Regexp(message, "[\\s]*!del[\\s]*(.*)").getMatch(0).trim();
                                for (int i = 0; i < messages.size(); i++) {
                                    BotMessage ob = messages.get(i);
                                    if (ob.regExp.contains(name)) {
                                        messages.remove(ob);
                                        sendMassmes(sender, sender, ob + " wurde aus der messageliste entfernt", 0);
                                    }
                                }
                                Utilities.saveObject(messages, messageFile);
                            } else {
                                String messagesdb = "Folgende Nachrichten sind in der Datenbank:";
                                for (int i = 0; i < messages.size(); i++) {
                                    messagesdb += "\r\n[" + i + "]: " + messages.get(i);
                                }
                                sendMassmes(sender, sender, messagesdb, 0, 0);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            String messagesdb = "Folgende Nachrichten sind in der Datenbank:";
                            for (int i = 0; i < messages.size(); i++) {
                                messagesdb += "\r\n[" + i + "]: " + messages.get(i);
                            }
                            sendMassmes(sender, sender, messagesdb, 0, 0);
                        }
                        return;
                    } else if (message.matches("[\\s]*!rm[\\s]*join.*")) {
                        if (!logedin(sender, ch2)) return;
                        try {
                            if (message.matches("[\\s]*!rm[\\s]*join[\\s]*[\\d]+")) {
                                int n = Integer.parseInt(new Regexp(message, "[\\s]*!rm join[\\s]*([\\d]+)").getMatch(0));
                                sendMassmes(sender, sender, settings.joinMessages.get(n)[0] + "|" + settings.joinMessages.get(n)[1] + " : wurde aus der joinliste entfernt", 0);
                                settings.joinMessages.remove(n);
                                Utilities.saveObject(settings, config);
                            } else if (message.matches("[\\s]*!rm[\\s]join[\\s]*[^\\s].*")) {
                                String name = new Regexp(message, "[\\s]*!rm[\\s]join[\\s]*(.*)").getMatch(0).trim();
                                for (int i = 0; i < settings.joinMessages.size(); i++) {
                                    String[] ob = settings.joinMessages.get(i);
                                    if (ob[0].matches("(?is).*" + name + ".*")) {
                                        settings.joinMessages.remove(ob);
                                        sendMassmes(sender, sender, ob[0] + "|" + ob[1] + " wurde aus der joinMessagesliste entfernt", 0);
                                    }
                                    Utilities.saveObject(settings, config);
                                }
                            } else {
                                String messagesdb = "Folgende Nachrichten sind in der joinMessages Datenbank:";
                                for (int i = 0; i < settings.joinMessages.size(); i++) {
                                    messagesdb += "\r\n[" + i + "]: " + settings.joinMessages.get(i)[0] + ":" + " " + settings.joinMessages.get(i)[1];
                                }
                                sendMassmes(sender, sender, messagesdb, 0, 0);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();

                            String messagesdb = "Folgende Nachrichten sind in der joinMessages Datenbank:";
                            for (int i = 0; i < settings.joinMessages.size(); i++) {
                                messagesdb += "\r\n[" + i + "]: " + settings.joinMessages.get(i)[0] + ":" + " " + settings.joinMessages.get(i)[1];
                            }
                            sendMassmes(sender, sender, messagesdb, 0, 0);
                        }
                        return;
                    } else if (message.matches("[\\s]*!add([\\s]|$).*")) {
                        int rights = userRights(sender, ch2);
                        if (!logedin(rights, sender)) return;
                        try {
                            String[] info = new Regexp(message, "[\\s]*!add[\\s]*" + "\\(\"(.*?)\"\\)[\\s]*(.*)").getMatches()[0];
                            if (info != null && info[0] != null && info[1] != null && !info[0].matches("[\\s]*") && !info[1].matches("[\\s]*")) {
                                if (info[1].matches(".*%%raw%%.*") && rights != UserInfo.OP) {
                                    sendMassmes(sender, sender, "für %%raw%% benötigt man op", 0);
                                }
                                addMessage(info[0], info[1], sender);
                                sendMassmes(sender, sender, info[0] + "|" + info[1] + ": wurde zur Nachrichtenliste hinzugefügt", 0);
                                Utilities.saveObject(messages, messageFile);
                            } else {
                                String messagesdb = "Folgende Nachrichten sind in der Messages Datenbank:";
                                for (int i = 0; i < messages.size(); i++) {
                                    messagesdb += "\r\n[" + i + "]: " + messages.get(i);
                                }
                                sendMassmes(sender, sender, messagesdb, 0, 0);
                            }
                            return;
                        } catch (Exception e) {
                            e.printStackTrace();
                            String messagesdb = "Folgende Nachrichten sind in der Messages Datenbank:";
                            for (int i = 0; i < messages.size(); i++) {
                                messagesdb += "\r\n[" + i + "]: " + messages.get(i);
                            }
                            sendMassmes(sender, sender, messagesdb, 0, 0);
                        }

                    } else if (message.matches("[\\s]*!join.*")) {
                        int rights = userRights(sender, ch2);
                        if (!logedin(rights, sender)) return;
                        try {
                            if (message.matches(".*%%raw%%.*") && rights != UserInfo.OP) {
                                sendMassmes(sender, sender, "für %%raw%% benötigt man op", 0);
                            }
                            String[] info = new Regexp(message, "[\\s]*!join[\\s]*" + "\\(\"(.*?)\"\\)[\\s]*(.*)").getMatches()[0];
                            if (info != null && info[0] != null && info[1] != null && !info[0].matches("[\\s]*") && !info[1].matches("[\\s]*")) {
                                sendMassmes(sender, sender, info[0] + "|" + info[1] + " : wurde zur joinMessagesliste hinzugefügt", 0);
                                settings.joinMessages.add(new String[] { info[0], info[1] });
                                Utilities.saveObject(settings, config);
                            } else {
                                String messagesdb = "Folgende Nachrichten sind in der joinMessages Datenbank:";
                                for (int i = 0; i < settings.joinMessages.size(); i++) {
                                    messagesdb += "\r\n[" + i + "]: " + settings.joinMessages.get(i)[0] + ":" + " " + settings.joinMessages.get(i)[1];
                                }
                                sendMassmes(sender, sender, messagesdb, 0, 0);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();

                            String messagesdb = "Folgende Nachrichten sind in der joinMessages Datenbank:";
                            for (int i = 0; i < settings.joinMessages.size(); i++) {
                                messagesdb += "\r\n[" + i + "]: " + settings.joinMessages.get(i)[0] + ":" + " " + settings.joinMessages.get(i)[1];
                            }
                            sendMassmes(sender, sender, messagesdb, 0, 0);
                        }
                        return;
                    } else if (message.matches("[\\s]*!ignore([\\s]|$).*")) {
                        if (!logedin(sender, ch2)) return;
                        String name = new Regexp(message, "[\\s]*!ignore[\\s]*(.*)").getMatch(0).trim();
                        if (name != null && !name.matches("[\\s]*")) {
                            if (settings.ignore.contains(name)) {
                                sendMassmes(sender, sender, name + " wird bereichts ignoriert", 0);
                            } else {
                                sendMassmes(sender, sender, name + " wird jetzt für die Nachrichtentexte ignoriert", 0);
                                settings.ignore.add(name);
                                Utilities.saveObject(settings, config);
                            }

                        } else {
                            String names = "Namen die ignoriert werden:";
                            for (int i = 0; i < settings.ignore.size(); i++) {
                                names += "\r\n[" + i + "]: " + settings.ignore.get(i);
                            }
                            sendMassmes(sender, sender, names, 0);
                        }
                        return;
                    } else if (message.matches("[\\s]*!autoop.*")) {
                        String name = message.replaceFirst("[\\s]*!autoop[\\s]*", "");
                        if (settings.autoOp.contains(name)) {
                            sendMassmes(sender, sender, name + " ist bereichts in der autoopliste", 0);
                            return;
                        }
                        if (userRights(sender, ch2) != UserInfo.OP) {
                            sendMassmes(sender, sender, "diese funktion dürfen nur ops verwenden", 0);
                            return;
                        }

                        if (userRights(name, channel) == 0) {
                            sendMassmes(sender, sender, "du kannst nur angemeldeten Benutzern autoop erteilen", 0);
                            return;
                        }
                        op(channel, name);
                        settings.autoOp.add(name);
                        sendMassmes(sender, sender, name + " hat jetzt autoop", 0);
                        Utilities.saveObject(settings, config);
                        return;

                    } else if (message.matches("[\\s]*!deop.*")) {
                        String name = message.replaceFirst("[\\s]*!deop[\\s]*", "");
                        if (settings.autoOp.contains(name)) {
                            if (userRights(sender, ch2) != UserInfo.OP) {
                                sendMassmes(sender, sender, "diese funktion dürfen nur ops verwenden", 0);
                                return;
                            }
                            deOp(channel, name);
                            settings.autoOp.remove(name);
                            Utilities.saveObject(settings, config);
                            sendMassmes(sender, sender, name + " wurde von autoop entfernt", 0);

                        } else
                            sendMassmes(sender, sender, name + " ist nicht in der autoopliste", 0);

                        return;

                    } else if (message.matches("[\\s]*!autovoice.*")) {
                        String name = message.replaceFirst("[\\s]*!autovoice[\\s]*", "");
                        if (settings.autoVoice.contains(name)) {
                            sendMassmes(sender, sender, name + " ist bereichts in der autovoiceliste", 0);
                            return;
                        }
                        if (userRights(sender, ch2) != UserInfo.OP) {
                            sendMassmes(sender, sender, "diese funktion dürfen nur ops verwenden", 0);
                            return;
                        }

                        if (userRights(name, channel) == 0) {
                            sendMassmes(sender, sender, "du kannst nur angemeldeten Benutzern autovoice erteilen", 0);
                            return;
                        }
                        voice(channel, name);
                        settings.autoVoice.add(name);
                        Utilities.saveObject(settings, config);
                        sendMassmes(sender, sender, name + " hat jetzt autovoice", 0);
                        return;

                    } else if (message.matches("[\\s]*!devoice.*")) {
                        String name = message.replaceFirst("[\\s]*!devoice[\\s]*", "");
                        if (settings.autoVoice.contains(name)) {
                            if (userRights(sender, ch2) != UserInfo.OP) {
                                sendMassmes(sender, sender, "diese funktion dürfen nur ops verwenden", 0);
                                return;
                            }
                            deVoice(channel, name);
                            settings.autoVoice.remove(name);
                            Utilities.saveObject(settings, config);
                            sendMassmes(sender, sender, name + " wurde von autovoice entfernt", 0);
                        } else
                            sendMassmes(sender, sender, name + " ist nicht in der autovoiceliste", 0);

                        return;
                        // Account angelegt! LOGIN=4834952 PASSWORT=BMf7A5bQJ7
                        // (Bitte notieren!)
                    } else if (message.matches("[\\s]*!getprem.*")) {
                        if (userRights(sender, ch2) != UserInfo.OP) {
                            sendMassmes(sender, sender, "diese funktion dürfen nur ops verwenden", 0);
                            return;
                        }
                        Iterator<String[]> iter = settings.premAccs.iterator();
                        String ret = "Premiumaccounts:";
                        while (iter.hasNext()) {
                            try {
                                String[] strings = (String[]) iter.next();
                                if (strings[2] != null && strings[2].matches("(?is).*(rapidshare|rscomrs.com).*")) {

                                    if (!Utilities.rsaccValid(strings[0], strings[1])) {
                                        iter.remove();
                                        Utilities.saveObject(settings, config);
                                        continue;
                                    }
                                }
                                ret += "\r\nLOGIN=" + strings[0] + " PASSWORT=" + strings[1] + " @" + strings[2];
                            } catch (Exception e) {
                                // TODO: handle exception
                            }

                        }
                        sendMassmes(sender, sender, ret, 10, 0);
                        return;
                    }
                    if (message.matches("[\\s]*!delprem[\\s].*")) {
                        if (userRights(sender, ch2) != UserInfo.OP) {
                            sendMassmes(sender, sender, "diese funktion dürfen nur ops verwenden", 0);
                            return;
                        }
                        try {
                            if (message.matches("[\\s]*!delprem[\\s]*[\\d]+")) {
                                int n = Integer.parseInt(new Regexp(message, "[\\s]*!delprem[\\s]*([\\d]+)").getMatch(0));
                                sendMassmes(sender, sender, "der premium acc " + settings.premAccs.get(n)[0] + " wurde aus der liste entfernt", 0);
                                settings.premAccs.remove(n);
                                Utilities.saveObject(settings, config);
                            } else if (message.matches("[\\s]*!delprem[\\s]*[^\\s].*")) {
                                String name = new Regexp(message, "[\\s]*!delprem[\\s]*(.*)").getMatch(0).trim();
                                for (int i = 0; i < settings.premAccs.size(); i++) {
                                    try {
                                        String ob = settings.premAccs.get(i)[0];
                                        if (ob.matches("(?is).*" + name + ".*")) {
                                            settings.premAccs.remove(i);
                                            sendMassmes(sender, sender, "der premium acc " + ob + " wurde aus der liste entfernt", 0);
                                        }
                                    } catch (Exception e) {
                                        // TODO: handle exception
                                    }

                                }

                                Utilities.saveObject(settings, config);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return;
                    }
                    if (channel.charAt(0) == '#') {
                        Date date = new java.util.Date();

                        String dateS = date.getHours() + ":" + date.getMinutes() + ":" + date.getSeconds();
                        try {
                            Utilities.writeLocalFile(getLog(channel), "(" + dateS + ") " + sender + ": " + message + "\r\n", true);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    // helpmenu

                    if (message.matches("[\\s]*!help[\\s]*")) {
                        sendMassmes(sender, sender, sender
                                + ": jDownloader Bot hilfe\r\n"
                                + "!last x	gibt die letzten x (maximal 30) Nachrichten zurück\r\n"
                                + "google übersetzung !tode englischen->deutsche !toen deutschen->englische !to.. englischen->sprachkürzel !..to.. sprachkürzel->sprachkürzel (bsp. !detofr)\r\n"
                                + "!ignoreme ignoriert dich im channel\r\n"
                                + "!say text gibt dir den text auf jedenfall aus\r\n"
                                + "!ident identifiziert (fals nicht schon beim join passiert)"
                                + (logedin(sender, ch2) ? ("\r\n!log	sendet Logdatei\r\n" + "!dellog löscht die Log-Datei\r\n" + "!shutdown beendet den bot\r\n" + "!autoop name !autovoice name nur mit op möglich\r\n" + "!deop name !devoice name nur mit op möglich\r\n" + "!addprem login : password @ service   !getprem   !delprem login nur mit op möglich\r\n" + "!ignore name	 ignoriert den namen im channel\r\n"
                                        + "!add (\"Regexp\") Nachricht	fügt einen eintrag zur Messageliste hinzu. flags: !delay=x bei 0 wird die Nachricht sofort versendet, !nouser Username am Anfang nicht anzeigen, !voice benötigt mindestens voice, !op nur für op, !logoff wird mehrmals und auch denen die in der ignorelist sehen gezeigt \r\nVariablen für !add: %%cmd%% der folgende Nachrichtentext ist ein Botcommand z.B. !last 5, %%raw%% der folgende Nachrichtentext wird raw versendet z.B. KICK %%user%% (op only),  %%user%% wird durch den Benutzname des senders ersetzt, %%reg%%pattern%% Regexp auf die vom User geschriebene Nachricht z.B. %%reg%%.*?jDownloader (.*)%% gibt aus was der User nach jDownloader gesagt hat  \r\n" + "!join (\"userRegexp\") Nachricht	die Nachricht wird dem angegebenen User beim betreten des Chats angezeigt\r\n"
                                        + "!rm ignore|join pattern löscht einen eintrag aus der ignoreier/joinliste\r\n" + "!del pattern löscht Nachrichten aus der Messageliste\r\n") : ""), 30, 0);

                        return;
                    }
                    boolean ign = false;
                    for (int i = 0; i < settings.ignore.size(); i++) {
                        if (sender.matches("(?is).*" + settings.ignore.get(i) + ".*")) {
                            ign = true;
                            break;
                        }
                    }
                    if (message.matches("[\\s]*![\\S]{0,8}to[\\S]*[\\s]*.*")) {

                        try {
                            String base;
                            if (message.matches("[\\s]*![\\S]+to[\\S]*[\\s]*.*")) {
                                base = "&sl=" + new Regexp(message, "[\\s]*!([\\S]*to[\\S]*)[\\s]*.*").getMatch(0).replaceFirst("to", "&tl=");
                            } else if (message.matches("[\\s]*!toen*[\\s]*.*"))
                                base = "&sl=de&tl=en";
                            else
                                base = "&sl=en&tl=" + new Regexp(message, "[\\s]*!to([\\S]*)[\\s]*.*").getMatch(0);
                            RequestInfo request = Utilities.postRequest(new URL("http://translate.google.com/translate_t" + "?" + base.replaceFirst(".", "")), null, null, null, "hl=de&ie=UTF-8&text=" + new Regexp(message, "[\\s]*![\\S]{0,8}to[\\S]*[\\s](.*)").getMatch(0) + base, true, 10000, 10000);
                            sendMassmes(sender, channel, request.getRegexp("<div id=result_box dir=\"ltr\">(.*?)</div>").getMatch(0), 0);
                        } catch (Exception e) {
                            e.printStackTrace();

                        }

                    } else if (message.matches("[\\s]*!whois .*")) {

                        try {
                            String name = new Regexp(message, "[\\s]*!whois[\\s]*(.*)").getMatch(0);
                            UserInfo user = userInfo(name);
                            String adress = InetAddress.getByName(user.hostname.replaceFirst(".*\\@", "")).getHostAddress();
                            RequestInfo request = Utilities.postRequest(new URL("http://www.geobytes.com/IpLocator.htm?GetLocation "), null, null, null, "cid=0&c=0&Template=iplocator.htm&ipaddress=" + adress, true, 10000, 10000);
                            String[] infos = request.getRegexp("<td align=\"right\">Country</td>.*?value=\"(.*?)\".*?<td align=\"right\">Region</td>.*?value=\"(.*?)\".*?<td align=\"right\">City</td>.*?value=\"(.*?)\".*?<td align=\"right\">TimeZone</td>.*?value=\"(.*?)\"").getMatches()[0];
                            String time = "";
                            if (infos[0].matches("(?is)germany"))
                                infos[0] = "Deutschland";
                            else {
                                Date dt = new Date();
                                time = " es ist bei ihm ";
                                String[] timem = infos[3].replaceAll("[-+]", "").split(":");

                                time += dt.getHours() + (((infos[3].contains("-") ? -1 : 1) * Integer.parseInt(timem[0])) - 1) + ":" + (dt.getMinutes() + (((infos[3].contains("-") ? -1 : 1) * Integer.parseInt(timem[1]))));
                                try {

                                } catch (Exception e) {
                                    // TODO: handle exception
                                }
                            }

                            sendMassmes(sender, channel, name + " aus " + infos[0] + " wohnt in " + infos[2] + " " + infos[1] + time, 0);
                        } catch (Exception e) {
                            e.printStackTrace();

                        }

                    } else if (message.matches("[\\s]*!wiki .*")) {

                        try {
                            String name = new Regexp(message, "[\\s]*!wiki[\\s]*(.*)").getMatch(0).replaceAll("\\s", "_");

                            RequestInfo request = Utilities.getRequest(new URL("http://wiki.jdownloader.org/index.php?search=" + name + "&go=Seite"), "", "", false);

                            if (request.containsHTML("<h1 class=\"firstHeading\">Suchergebnisse</h1>")) {
                                String[] links = null;
                                if (!request.containsHTML("<a name=\"Keine_.C3.9Cbereinstimmungen_mit_Seitentiteln\">")) {
                                    try {
                                        links = Utilities.getHttpLinks(request.getRegexp("Seitentiteln</span>(.*?)</ol>").getMatch(0), "http://wiki.jdownloader.org/");
                                    } catch (Exception e) {
                                        // TODO: handle exception
                                    }
                                }
                                if (links == null || links.length < 1 || links.length > 3) {
                                    request = Utilities.getRequest(new URL("http://wiki.jdownloader.org/index.php?title=JDownloader_Wiki:Portal"), "", "", false);
                                    String html = request.getHtmlCode();
                                    html = html.substring(html.indexOf("showTocToggle(); } </script>"));
                                    html = html.substring(0, html.indexOf("NewPP limit report"));
                                    String[] cats = html.split("<h2> <span class=\"mw-headline\"");

                                    HashMap<String, Integer> map = new HashMap<String, Integer>();
                                    for (int i = 0; i < cats.length; i++) {
                                        int count = new Regexp(cats[i], name).count();
                                        if (count > 0) {
                                            map.put("http://wiki.jdownloader.org/index.php?title=JDownloader_Wiki:Portal#" + new Regexp(cats[i], ">(.*?)<").getMatch(0), count);

                                        }
                                    }
                                    if (map.size() > 0) {
                                        map = (HashMap<String, Integer>) Utilities.revSortByValue(map);
                                        sendMassmes(sender, channel, map.keySet().iterator().next(), 0);
                                    } else {
                                        sendMassmes(sender, channel, "http://wiki.jdownloader.org/index.php?title=JDownloader_Wiki:Portal", 0);
                                    }
                                } else {
                                    String links2 = links[0];
                                    for (int i = 1; i < links.length; i++) {
                                        links2 += "\r\n" + links[i];
                                    }
                                    sendMassmes(sender, channel, links2, 0);
                                }
                            } else {
                                sendMassmes(sender, channel, request.getLocation(), 0);
                            }

                        } catch (Exception e) {
                            e.printStackTrace();

                        }

                    } else if (message.matches("[\\s]*!ignoreme[\\s]*")) {
                        try {
                            if (!settings.ignore.contains(sender.replaceAll("[\\W]", "."))) {
                                sendMassmes(sender, sender, "sie werden jetzt für die Nachrichtentexte ignoriert", 0);

                                settings.ignore.add(sender.replaceAll("[\\W]", "."));
                                Utilities.saveObject(settings, config);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            // TODO: handle exception
                        }

                        return;
                    } else {
                        String message2 = message;
                        boolean say = false;
                        int delay = 10000;
                        int userDelay = 0;
                        if (message.matches("[\\s]*!say.*")) {
                            message2 = message2.replaceFirst("[\\s]*!say[\\s]*", "");
                            say = true;
                            delay = 0;
                        }
                        Iterator<BotMessage> iter = messages.iterator();
                        while (iter.hasNext()) {
                            try {
                                BotMessage botMessage = (BotMessage) iter.next();
                                if (message2.matches("(?is).*" + botMessage.regExp + ".*")) {

                                    boolean nouser = false;
                                    String nachri = botMessage.message;
                                    if (nachri.matches(".*!voice .*")) {
                                        if (!logedin(sender, ch2)) return;
                                        nachri = nachri.replaceFirst("!voice ", "");
                                    }
                                    if (nachri.matches(".*!op .*")) {
                                        nachri = nachri.replaceFirst("!op ", "");
                                        if (userRights(sender, ch2) != UserInfo.OP) return;
                                    }
                                    if (nachri.matches(".*!nouser .*")) {
                                        nachri = nachri.replaceFirst("!nouser ", "");
                                        nouser = true;
                                    }
                                    if (nachri.matches(".*!delay=[\\d]+ .*")) {
                                        delay = Integer.parseInt(new Regexp(nachri, ".*!delay=([\\d]+).*").getMatch(0));
                                        nachri = nachri.replaceFirst("!delay=[\\d]+ ", "");

                                    }
                                    if (nachri.matches(".*!userdelay=[\\d]+ .*")) {
                                        userDelay = Integer.parseInt(new Regexp(nachri, ".*!userdelay=([\\d]+).*").getMatch(0));
                                        nachri = nachri.replaceFirst("!userdelay=[\\d]+ ", "");

                                    }
                                    boolean logit = true;
                                    if (nachri.matches(".*!logoff .*")) {
                                        nachri = nachri.replaceFirst("!logoff ", "");
                                        logit = false;
                                        delay = 0;
                                    } else if (!say && ign) return;

                                    if (!say && logit) {
                                        if (botMessage.users.contains(sender)) return;
                                        User[] users = getUsers(channel);
                                        for (int i = 0; i < users.length; i++) {
                                            botMessage.add(users[i].getNick());
                                        }
                                        botMessage.add(sender);
                                        Utilities.saveObject(messages, messageFile);
                                    }
                                    nachri = nachri.replaceAll("%%user%%", sender);
                                    if (nachri.matches(".*%%reg%%.*%%.*")) {
                                        String replacement = "";
                                        try {
                                            String reg[] = new Regexp(nachri, "%%reg%%(.*?)%%").getMatches(1);
                                            for (int j = 0; j < reg.length; j++) {
                                                replacement = new Regexp(message2, reg[j]).getMatch(0);
                                                nachri = nachri.replaceFirst("%%reg%%.*?%%", replacement);
                                            }

                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }

                                    }
                                    if (nachri.matches(".*%%cmd%%.*")) {
                                        try {
                                            String reg = new Regexp(nachri, "%%cmd%%(.*)").getMatch(0);
                                            onMessage(channel, sender, login, hostname, reg);
                                            return;
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }

                                    }
                                    if (nachri.matches(".*%%raw%%.*")) {
                                        try {
                                            String reg = new Regexp(nachri, "%%raw%%(.*)").getMatch(0);
                                            sendRawLine(reg);
                                            return;
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }

                                    }
                                    if (nouser)
                                        sendMassmes(sender, channel, nachri, delay, userDelay);
                                    else
                                        sendMassmes(sender, channel, sender + ": " + nachri, delay, userDelay);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();

                            }

                        }
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();

                }

            }
        }).start();

    }

    public void sendMassmes(String sender, String channel, String message, int delayOnVoice) {
        sendMassmes(sender, channel, message, 5, delayOnVoice, 0);
    }

    public void sendMassmes(String sender, String channel, String message, int delayOnVoice, int delayOnUser) {
        sendMassmes(sender, channel, message, 5, delayOnVoice, delayOnUser);
    }

    public void sendMassmes(String sender, String channel, String message, int max, int delayOnVoice, int delayOnUser) {
        if (delayOnUser > 0) {
            delayedUserMsg = true;
            try {
                Thread.sleep(delayOnUser);
            } catch (InterruptedException e) {
                
                e.printStackTrace();
            }
            if (!delayedUserMsg) return;
        } else if (delayOnVoice > 0) {
            delayedMsg = true;
            try {
                Thread.sleep(delayOnVoice);
            } catch (InterruptedException e) {
                
                e.printStackTrace();
            }
            if (!delayedMsg) return;
        }
        String[] mass = message.split("(\r\n|\n|\r)");
        if (mass.length > max) {
            Utilities.writeLocalFile(new File("message.txt"), message, false);
            dccSendFile(new File("message.txt"), sender, 100000);
        } else {
            for (int i = 0; i < mass.length; i++) {
                if (mass[i].startsWith("/me")) {
                    sendRawLine("PRIVMSG " + channel + " :\001ACTION " + mass[i].replaceFirst("/me", "") + "\001");
                } else
                    sendMessage(channel, mass[i]);
            }
        }
    }

    private void addMessage(String regExp, String message, String sender) {
        messages.add(new BotMessage(regExp, message, sender));
    }

    public void setN(String name) {
        this.setName(name);

    }

    public void onPrivateMessage(String sender, String login, String hostname, String message) {
        onMessage(sender, sender, login, hostname, message);
    }

    private boolean logedin(String sender, String channel) {
        return logedin(userRights(sender, channel), sender);
    }

    public boolean logedin(int rights, String sender) {
        if (rights > 1)
            return true;
        else if (rights == 0) {
            sendMassmes(sender, sender, "du bist auf dem Server nicht angemeldet", 0);
            return false;
        }
        sendMassmes(sender, sender, "du hast in diesem Channel nicht das recht den Bot zu administrieren", 0);
        return false;
    }

    protected void onServerResponse(int code, String response) {
       
        if (code != 372 && code > 5) {
            serverResponse.put(code, response);
        }
    }

    public ServerInfo[] getServerInfos(final String message, int waitOn, int from, int to, String contain) {
        for (int i = from; i < to + 1; i++) {

            try {
                serverResponse.remove(i);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
        sendRawLine(message);
        int c = 0;
        if (waitOn > 0) {
            while (!serverResponse.containsKey(waitOn)) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    
                    e.printStackTrace();
                }
                if (c++ == 1000) return null;
            }
        }
        Vector<ServerInfo> ret = new Vector<ServerInfo>();
        for (int i = from; i < to + 1; i++) {

            try {
                String add = serverResponse.get(i);
                if (add != null && (contain == null || add.contains(contain))) {
                    ret.add(new ServerInfo(i, add));
                }
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
        return ret.toArray(new ServerInfo[ret.size()]);
    }

    /**
     * 0 = unregistriert 1 = registriert 2 = voice 3 = op
     * 
     * @param sender
     * @return
     */
    public UserInfo userInfo(String sender) {

        ServerInfo[] info = getServerInfos("whois " + sender, 318, 310, 320, sender);
        UserInfo uinfo = new UserInfo();
        for (int i = 0; i < info.length; i++) {
            if (info[i].code == 311) {
                try {
                    uinfo.hostname = new Regexp(info[i].response, ".*?([\\S]\\=.*?)[\\s]*\\*[\\s]*:").getMatch(0).replaceFirst("[\\s]", "@");
                } catch (Exception e) {
                    // TODO: handle exception
                }

            } else if (info[i].code == 319) {
                uinfo.channels = info[i].response.replaceFirst(".*?[\\s]*.*?[\\s]*:", "");
            } else if (info[i].code == 320) {
                uinfo.identified = info[i].response.contains("is identified to services");
            }
        }
        return uinfo;
    }

    /**
     * 0 = unregistriert 1 = registriert 2 = voice 3 = op
     * 
     * @param sender
     * @return
     */
    private int userRights(UserInfo userInfo, String channel) {
        if (userInfo.channels.contains("@" + channel))
            return UserInfo.OP;
        else if (userInfo.channels.contains("+" + channel))
            return UserInfo.VOICE;
        else if (userInfo.identified) return UserInfo.IDENTIFIED;
        return UserInfo.UNIDENTIFIED;
    }

    private int userRights(String sender, String channel) {
        return userRights(userInfo(sender), channel);
    }

}

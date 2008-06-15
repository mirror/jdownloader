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

package jd.plugins.optional;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import jd.config.MenuItem;
import jd.config.SubConfiguration;
import jd.gui.skins.simple.LocationListener;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.Link.JLinkButton;
import jd.parser.Regex;
import jd.plugins.PluginOptional;
import jd.utils.HTMLEntities;
import jd.utils.JDLocale;
import jd.utils.JDSounds;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

import org.schwering.irc.lib.IRCConnection;
import org.schwering.irc.lib.IRCEventListener;
import org.schwering.irc.lib.IRCModeParser;
import org.schwering.irc.lib.IRCUser;

public class JDChat extends PluginOptional {
    private static final String HOST = "PARAM_" + "HOST";
    private static final String PORT = "PARAM_" + "PORT";;
    private static final String NICK = "PARAM_" + "NICK";;

    private static final String CHANNEL = "#jDownloader";
    public static final int REPLY_NAMES_LIST = 353;
    public static final int REPLY_END_OF_NAMES = 366;
    public static final int REPLY_MOD = 332;
    public static final String COLOR_CHAT = "000000";
    public static final String COLOR_PM = "FFcc00";
    private static final String COLOR_SYSTEM = "cccccc";
    private static final String COLOR_SELF = "55cc55";

    public static int getAddonInterfaceVersion() {
        return 0;
    }

    private JFrame frame;
    private IRCConnection conn;
    private JTextPane textArea;
    private JTextField textField;
    private String nick;
    private StringBuffer sb;
    private JScrollPane scrollPane;
    private boolean changed;
    private JLabel top;
    private JTextPane right;
    private ArrayList<User> NAMES;

    @Override
    public String getCoder() {
        return "jD-Team";
    }

    @Override
    public String getPluginID() {
        return getPluginName() + " " + getVersion();
    }

    @Override
    public String getPluginName() {
        return JDLocale.L("plugins.optional.jdchat.name", "JD Chat");
    }

    @Override
    public String getVersion() {
        return "0.1";
    }

    @Override
    public boolean initAddon() {
        this.NAMES = new ArrayList<User>();
        this.sb = new StringBuffer();
        initGUI();
        new Thread() {
            public void run() {

                initIRC();
            }
        }.start();
        return true;
    }

    private void initGUI() {

        this.frame = new JFrame();
        frame.setTitle(JDLocale.L("plugins.optional.jdChat.gui.title", "JD Chat"));
        frame.setIconImage(JDTheme.I("gui.images.config.network_local"));
        frame.setPreferredSize(new Dimension(1000, 600));
        frame.setName("JDCHAT");
        LocationListener list = new LocationListener();
        frame.addComponentListener(list);
        frame.addWindowListener(list);
        frame.setLayout(new BorderLayout());
        top = new JLabel();
        textArea = new JTextPane();
        textArea.addHyperlinkListener(new HyperlinkListener(){

            public void hyperlinkUpdate(HyperlinkEvent e) {
             if(e.getEventType()==HyperlinkEvent.EventType.ACTIVATED){
                 
                 if(e.getDescription().startsWith("intern")){
                     String[][] m = new Regex(e.getDescription()+"?","intern:([\\w]*?)\\|(.*?)\\?").getMatches();
                     if(m.length==1){
                         doAction(m[0],m[1]);
                         return;
                     }
                 }else{
                     JLinkButton.openURL( e.getURL());
                 }
             }
                
            }
            
        });
        scrollPane = new JScrollPane(textArea);
        textField = new JTextField();
        textField.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, Collections.EMPTY_SET);
        textField.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, Collections.EMPTY_SET);

        textField.addKeyListener(new KeyListener() {

            public void keyPressed(KeyEvent e) {
                // TODO Auto-generated method stub

            }

            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {

                    if (textField.getText().length() == 0) return;

                    sendMessage(CHANNEL, textField.getText());
                    textField.setText("");
                    textField.requestFocus();

                } else if (e.getKeyCode() == KeyEvent.VK_TAB) {
                    if (textField.getText().length() == 0) return;
                    String txt = textField.getText();

                    int last = Math.max(0, txt.lastIndexOf(" "));
                    txt = txt.substring(last).trim();

                    for (Iterator<User> it = NAMES.iterator(); it.hasNext();) {
                        User user = it.next();
                        if (user.name.toLowerCase().startsWith(txt.toLowerCase())) {

                            textField.setText((textField.getText().substring(0, last) + " " + user.name).trim());

                            textField.requestFocus();
                            return;

                        }
                    }

                }

            }

            public void keyTyped(KeyEvent e) {
                // TODO Auto-generated method stub

            }

        });
        right = new JTextPane();
        right.setContentType("text/html");
        right.setEditable(false);
        // Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        textArea.setContentType("text/html");
        textArea.setEditable(false);
        frame.setResizable(true);

        frame.add(top, BorderLayout.NORTH);
        frame.add(new JScrollPane(right), BorderLayout.EAST);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(textField, BorderLayout.SOUTH);
        frame.pack();
        SimpleGUI.restoreWindow(new JFrame(), null, frame);
        frame.setVisible(true);
    }

    protected void doAction(String[] strings, String[] strings2) {
        // TODO Auto-generated method stub
        
    }

    protected void sendMessage(String channel2, String text) {
        conn.doPrivmsg(channel2, text);
        this.addToText(COLOR_SELF, conn.getNick() + ":" + prepareMsg(text));

    }

    private void initIRC() {
        while (true) {
            SubConfiguration conf = JDUtilities.getSubConfig("IRC");
            String host = conf.getStringProperty(HOST, "irc.freenode.net");
            int port = conf.getIntegerProperty(PORT, 6667);
            String pass = null;
            String nick = getNick();
            String user = getNick();
            String name = getNick();
            addToText(COLOR_SYSTEM, "Connecting to JDChat...");
            conn = new IRCConnection(host, new int[] { port }, pass, nick, user, name);
            conn.setTimeout(1000 * 60 * 60);

            conn.addIRCEventListener(new IRCListener());
            conn.setEncoding("UTF-8");
            conn.setPong(true);
            conn.setDaemon(false);
            conn.setColors(false);
            try {
                conn.connect();

                conn.doJoin(CHANNEL, null);
                // conn.doPrivmsg("#jdDev", "JDChat Addon 0.1");
                break;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                addToText(COLOR_SYSTEM, "Connect Timeout. Server not reachable...");
                e.printStackTrace();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        }

    }

    private String getNick() {
        // TODO Auto-generated method stub
        String def = "JD-[" + JDLocale.getLocale() + "]_" + ("" + System.currentTimeMillis()).substring(6);
        nick = JDUtilities.getSubConfig("IRC").getStringProperty(NICK, def);
        return nick;
    }

    @Override
    public String getRequirements() {
        return "JRE 1.5+";
    }

    @Override
    public ArrayList<MenuItem> createMenuitems() {
        return null;
        // ArrayList<MenuItem> menu = new ArrayList<MenuItem>();
        // if (frame == null || !frame.isVisible()) {
        // menu.add(new
        // MenuItem(JDLocale.L("plugins.optional.httpliveheaderscripter.action.start",
        // "Start Scripter"), 0).setActionListener(this));
        // } else {
        // menu.add(new
        // MenuItem(JDLocale.L("plugins.optional.httpliveheaderscripter.action.end",
        // "Exit Scripter"), 0).setActionListener(this));
        //
        // }
        // return menu;
    }

    @Override
    public void onExit() {
        conn.close();
    }

    public User getUser(String name) {
        for (Iterator<User> it = NAMES.iterator(); it.hasNext();) {
            User next = it.next();
            if (next.isUser(name)) return next;

        }
        return null;

    }

    public String getRandomColor() {
        String col = Long.toHexString((long) (Math.random() * 0xffffff));
        while (col.length() < 6)
            col = "0" + col;
        return col;
    }

    public void setMOD(final String msg) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                top.setText(msg);
frame.setTitle(getPluginName()+" : "+msg);
                frame.pack();
            }
        });

    }

    public void addToText(String col, String string) {
        Date dt = new Date();
     
     SimpleDateFormat df = new SimpleDateFormat( "HH:mm:ss" );
     
 
        this.sb.append("<font color='#" + col + "'>[" + df.format( dt )+"] "+string + "</font><br>");
        changed = true;
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                if (changed) {
                    textArea.setText(sb.toString());
                    logger.info(sb.toString());

                    int max = scrollPane.getVerticalScrollBar().getMaximum();

                    scrollPane.getVerticalScrollBar().setValue(max);
                    changed = false;
                }

            }
        });

    }

    public void updateNames(String[] split) {
        for (String name : split) {

            if (getUser(name) == null) NAMES.add(new User(name));
        }

    }

    public void updateNamesPanel() {
        final StringBuffer sb = new StringBuffer();
        Collections.sort(NAMES);
        for (Iterator<User> it = NAMES.iterator(); it.hasNext();) {
            User name = it.next();
            sb.append("<a href='intern:query|" + name + "'><font color='#" + name.color + "'> " + name + "</font></a><br>");
        }

        EventQueue.invokeLater(new Runnable() {
            public void run() {
                right.setText(sb.toString());
                frame.pack();
            }
        });

    }public String prepareMsg(String msg){
        msg=HTMLEntities.htmlAngleBrackets(msg);
        return msg.replaceAll("http://[^\\s\"]*","﻿<a href=\\'$0\\'>﻿$0</a>");
       
    }

    class IRCListener implements IRCEventListener {

        public void onRegistered() {
            logger.info("Connected");
            addToText("000000", "Connection estabilished");
        }

        public void onDisconnected() {
            logger.info("Disconnected");
            addToText("000000", "Connection lost");
            initIRC();
        }

        public void onError(String msg) {
            logger.info("Error: " + msg);
        }

        public void onError(int num, String msg) {
            logger.info("Error #" + num + ": " + msg);
        }

        public void onInvite(String chan, IRCUser u, String nickPass) {
            logger.info(chan + "> " + u.getNick() + " invites " + nickPass);
        }

        public void onJoin(String chan, IRCUser u) {
            logger.info(chan + "> " + u.getNick() + " joins");

            resetNamesList();
            conn.doNames(CHANNEL);
        }

        public void onKick(String chan, IRCUser u, String nickPass, String msg) {
            logger.info(chan + "> " + u.getNick() + " kicks " + nickPass);
        }

        public void onMode(IRCUser u, String nickPass, String mode) {
            logger.info("Mode: " + u.getNick() + " sets modes " + mode + " " + nickPass);
        }

        public void onMode(String chan, IRCUser u, IRCModeParser mp) {
            logger.info(chan + "> " + u.getNick() + " sets mode: " + mp.getLine());
        }

        public void onNick(IRCUser u, String nickNew) {
            logger.info("Nick: " + u.getNick() + " is now known as " + nickNew);
        }

        public void onNotice(String target, IRCUser u, String msg) {
            logger.info(target + "> " + u.getNick() + " (notice): " + msg);
        }

        public void onPart(String chan, IRCUser u, String msg) {
            logger.info(chan + "> " + u.getNick() + " parts");
            resetNamesList();
            conn.doNames(CHANNEL);

        }

        public void resetNamesList() {

            NAMES = new ArrayList<User>();
            if (getUser(conn.getNick().trim()) == null) NAMES.add(new User(conn.getNick().trim()));
        }

        public void onPrivmsg(String chan, IRCUser u, String msg) {
            User user = getUser(u.getNick());
            if (user == null) { return; }
            if (chan.equals(conn.getNick())) {
                addToText(COLOR_PM, user.getNickLink("pmnick") + "> " + prepareMsg(msg));
                
                if(!frame.isActive()){
                    JDSounds.PT("sound.gui.selectPackage");
                    frame.toFront();
                }
                // resetNamesList();
                // conn.doNames(CHANNEL);
            } else {
                addToText(COLOR_CHAT, user.getNickLink("channick") + ": " + prepareMsg(msg));
                if(!frame.isActive()&&msg.contains(conn.getNick())){
                    JDSounds.PT("sound.gui.selectPackage");
                    frame.toFront();
                }
            }

            logger.info(chan + "> " + u.getNick() + ": " + msg);
        }

        public void onQuit(IRCUser u, String msg) {
            logger.info("Quit: " + u.getNick());
        }

        public void onReply(int num, String value, String msg) {
            
            logger.info("Reply #" + num + ": " + value + " " + msg);
            if (num == REPLY_NAMES_LIST) {
                updateNames(msg.trim().split(" "));
            }

            if (num == REPLY_END_OF_NAMES) {
                updateNamesPanel();

            }
            if (num == REPLY_MOD) {
                setMOD(msg);

            }

        }

        public void onTopic(String chan, IRCUser u, String topic) {
            logger.info(chan + "> " + u.getNick() + " changes topic into: " + topic);
        }

        public void onPing(String p) {

        }

        public void unknown(String a, String b, String c, String d) {
            logger.info("UNKNOWN: " + a + " b " + c + " " + d);
        }
    }

    class User implements Comparable {
        private static final int RANK_OP = 0;
        private static final int RANK_VOICE = 1;
        public int rank = -1;
        public String name;
        public String color;

        public User(String name) {
            this(name, null);
        }

        public boolean isUser(String name) {
            if (name.startsWith("@")) {

                name = name.substring(1);
            }
            if (name.startsWith("+")) {

                name = name.substring(1);
            }
            return name.equals(this.name);

        }

        public User(String name, String color) {
            if (name.startsWith("@")) {
                this.rank = RANK_OP;
                name = name.substring(1);
            }
            if (name.startsWith("+")) {
                this.rank = RANK_VOICE;
                name = name.substring(1);
            }
            this.name = name;
            if (color == null) color = getRandomColor();
            this.color = color;

        }

        public String getNickLink(String id) {
            return "<a href='intern:" + id + "|" + name + "'><font color='#" + color + "'>" + name + "</font></a>";
        }

        private String getRangName() {
            switch (rank) {
            case RANK_OP:
                return "!!!" + name;
            case RANK_VOICE:
                return "!!" + name;
            }
            return name;

        }

        public String toString() {
            switch (rank) {
            case RANK_OP:
                return "@" + name;
            case RANK_VOICE:
                return "+" + name;
            }
            return name;

        }

        public int compareTo(Object o) {
            // TODO Auto-generated method stub

            return getRangName().compareTo(((User) o).getRangName());
        }

    }

}

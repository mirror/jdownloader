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

package jd.plugins.optional.jdchat;

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
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.MenuItem;
import jd.config.SubConfiguration;
import jd.controlling.interaction.Interaction;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.skins.simple.LocationListener;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.Link.JLinkButton;
import jd.parser.Regex;
import jd.plugins.PluginOptional;
import jd.utils.JDLocale;
import jd.utils.JDSounds;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

import org.schwering.irc.lib.IRCConnection;

public class JDChat extends PluginOptional implements ControlListener {
    private static final String HOST = "PARAM_" + "HOST";
    private static final String PORT = "PARAM_" + "PORT";;
    private static final String NICK = "PARAM_" + "NICK";;
    private static final String PERFORM = "PARAM_" + "PERFORM";;
    private static final String CHANNEL = "#jDownloader";
    public static final String COLOR_CHAT = "222222";
    public static final String COLOR_PM = "ff0066";
    public static final String COLOR_SYSTEM = "666666";
    public static final String COLOR_SELF = "0000cc";
    public static final String COLOR_ERROR = "ff0000";
    private static final String COLOR_ACTION = "009900";
    public static final String COLOR_NOTICE = "33cccc";
    private static final Pattern CMD_PM = Pattern.compile("(msg|query)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CMD_SLAP = Pattern.compile("(slap)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CMD_NICK = Pattern.compile("(nick|name)", Pattern.CASE_INSENSITIVE);
    
    private static final Pattern CMD_ACTION = Pattern.compile("(me)", Pattern.CASE_INSENSITIVE);

    private static final Pattern CMD_VERSION = Pattern.compile("(version|jdversion)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CMD_CONNECT = Pattern.compile("(connect|verbinden)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CMD_DISCONNECT = Pattern.compile("(disconnect|trennen)", Pattern.CASE_INSENSITIVE);

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
    private int nickCount = 0;
    private boolean loggedIn;

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
        initConfigs();
        initGUI();
        JDUtilities.getController().addControlListener(this);
        new Thread() {
            public void run() {

                initIRC();
            }
        }.start();
        return true;
    }

    private void initConfigs() {
        SubConfiguration subConfig = JDUtilities.getSubConfig("JDCHAT");
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, subConfig, NICK, JDLocale.L("plugins.optional.jdchat.user", "Nickname")));

        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, subConfig, PERFORM, JDLocale.L("plugins.optional.jdchat.performonstart", "Perform commands after connection estabilished")));

    }

    public void perform() {
        String[] perform = JDUtilities.splitByNewline(JDUtilities.getSubConfig("JDCHAT").getStringProperty(PERFORM));
        if (perform == null) return;
        for (String cmd : perform)
            this.sendMessage(CHANNEL, cmd);
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
        HyperlinkListener hyp = new HyperlinkListener() {

            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {

                    if (e.getDescription().startsWith("intern")) {
                        String[][] m = new Regex(e.getDescription() + "?", "intern:([\\w]*?)\\|(.*?)\\?").getMatches();
                        if (m.length == 1) {
                            doAction(m[0][0], m[0][1]);
                            return;
                        }
                    } else {
                        JLinkButton.openURL(e.getURL());
                    }
                }

            }

        };
        right = new JTextPane();
        right.setContentType("text/html");
        right.setEditable(false);
        textArea.addHyperlinkListener(hyp);
        right.addHyperlinkListener(hyp);
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

    protected void doAction(String type, String name) {
        if (textField.getText().length() == 0) {
            textField.setText("/msg " + name + " ");
        } else {

            textField.setText(textField.getText().trim() + " " + name + " ");
        }

        textField.requestFocus();
    }

    protected void sendMessage(String channel2, String text) {
        if (text.startsWith("/")) {
            int end = text.indexOf(" ");
            if (end < 0) end = text.length();
            String cmd = text.substring(1, end);
            String rest = text.substring(end).trim();
            if (Regex.matches(cmd, CMD_PM)) {
                end = rest.indexOf(" ");
                if (end < 0) end = rest.length();

                conn.doPrivmsg(rest.substring(0, end).trim(), rest.substring(end).trim());

                this.addToText(COLOR_PM, ">" + rest.substring(0, end).trim() + ":" + Utils.prepareMsg(rest.substring(end).trim()));
            } else if (Regex.matches(cmd, CMD_SLAP)) {
                conn.doPrivmsg(channel2, new String(new byte[] { 1 }) + "ACTION " + " slaps " + rest + " with the whole Javadocs" + new String(new byte[] { 1 }));
                this.addToText(COLOR_ACTION, conn.getNick() + " slaps " + rest + " with the whole Javadocs");
            } else if (Regex.matches(cmd, CMD_ACTION)) {

                conn.doPrivmsg(channel2, new String(new byte[] { 1 }) + "ACTION " + rest.trim() + new String(new byte[] { 1 }));
                this.addToText(COLOR_ACTION, conn.getNick() + " " + rest.trim());

            } else if (Regex.matches(cmd, CMD_VERSION)) {

                String msg = " is using " + JDUtilities.getJDTitle() + " with Java " + JDUtilities.getJavaVersion() + " on a " + System.getProperty("os.name") + " system";
                conn.doPrivmsg(channel2, new String(new byte[] { 1 }) + "ACTION " + msg + new String(new byte[] { 1 }));
                this.addToText(COLOR_ACTION, conn.getNick() + " " + msg);

            } else if (Regex.matches(cmd, CMD_NICK)) {
                conn.doNick(rest.trim());

                JDUtilities.getSubConfig("JDCHAT").setProperty(NICK, rest.trim());
                JDUtilities.getSubConfig("JDCHAT").save();

            } else if (Regex.matches(cmd, CMD_CONNECT)) {
                initIRC();
            } else if (Regex.matches(cmd, CMD_DISCONNECT)) {
                conn.close();
            } else {
                this.addToText(COLOR_ERROR, "Command /" + cmd + " is not available");
            }

        } else {
            conn.doPrivmsg(channel2, text);
            this.addToText(COLOR_SELF, conn.getNick() + ":" + Utils.prepareMsg(text));
        }
    }

    private void initIRC() {
        for (int i = 0; i < 20; i++) {
            SubConfiguration conf = JDUtilities.getSubConfig("JDCHAT");
            String host = conf.getStringProperty(HOST, "irc.freenode.net");
            int port = conf.getIntegerProperty(PORT, 6667);
            String pass = null;
            String nick = getNickname();
            String user = getNickname();
            String name = getNickname();
            addToText(COLOR_SYSTEM, "Connecting to JDChat...");
            conn = new IRCConnection(host, new int[] { port }, pass, nick, user, name);
            conn.setTimeout(1000 * 60 * 60);

            conn.addIRCEventListener(new IRCListener(this));
            conn.setEncoding("UTF-8");
            conn.setPong(true);
            conn.setDaemon(false);
            conn.setColors(false);
            try {
                conn.connect();

                // conn.doPrivmsg("#jdDev", "JDChat Addon 0.1");
                break;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                addToText(COLOR_SYSTEM, "Connect Timeout. Server not reachable...");
                e.printStackTrace();
                try {
                    Thread.sleep(15000);
                } catch (InterruptedException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                initIRC();
            }
        }

    }

    String getNickname() {
        // TODO Auto-generated method stub
        String def = "JD-[" + JDLocale.getLocale().substring(0, 2) + "]_" + ("" + System.currentTimeMillis()).substring(6);
        nick = JDUtilities.getSubConfig("JDCHAT").getStringProperty(NICK, def);
        if (getNickCount() > 0) {
            nick += "[" + getNickCount() + "]";
        }
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

    public void setMOD(final String msg) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                top.setText(msg);
                frame.setTitle(getPluginName() + " : " + msg);
                frame.pack();
            }
        });

    }

    public void addToText(String col, final String string) {
        Date dt = new Date();

        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");

        this.sb.append("<font color='#" + col + "'>[" + df.format(dt) + "] " + string + "</font><br>");
        changed = true;
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                if (changed) {

                    if (!frame.isActive() && string.contains(conn.getNick())) {
                        JDSounds.PT("sound.gui.selectPackage");
                        frame.toFront();
                    }
                    
                  
                    textArea.setText("  <font size='3' face='Verdana, Arial, Helvetica, sans-serif'>"+sb.toString()+"</font>");

                    int max = scrollPane.getVerticalScrollBar().getMaximum();

                    scrollPane.getVerticalScrollBar().setValue(max);
                    changed = false;
                }

            }
        });

    }

    public void updateNames(String[] split) {
        User user;
        for (String name : split) {

            if ((user = getUser(name)) == null) {
                NAMES.add(new User(name));
            } else if (user.rank != new User(name).rank) {
                user.rank = new User(name).rank;
            }
        }

    }

    public void updateNamesPanel() {
        final StringBuffer sb = new StringBuffer();
        Collections.sort(NAMES);
        for (Iterator<User> it = NAMES.iterator(); it.hasNext();) {
            User name = it.next();
            sb.append("<a href='intern:query|" + name + "'><font color='#" + name.color + "'><b>" + name + "</b></font></a><br>");
        }
       
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                right.setText("  <font size='3' face='Verdana, Arial, Helvetica, sans-serif'>"+sb.toString()+"</font>");
                frame.pack();
            }
        });

    }

    public void resetNamesList() {

        NAMES = new ArrayList<User>();
        if (getUser(conn.getNick().trim()) == null) NAMES.add(new User(conn.getNick().trim()));
    }

    public void requestNameList() {
        resetNamesList();
        conn.doNames(CHANNEL);
    }

    public String getNick() {
        // TODO Auto-generated method stub
        return conn.getNick();
    }

    public void setNickCount(int nickCount) {
        this.nickCount = nickCount;
    }

    public int getNickCount() {
        return nickCount;
    }

    public void setNick(String nickname) {
        if (nickname == null) return;
        addToText(JDChat.COLOR_SYSTEM, "Rename to " + nickname);

        conn.doNick(nickname);

    }

    public void onConnected() {
        conn.doJoin(CHANNEL, null);
        setLoggedIn(true);
        perform();

    }

    public void reconnect() {
        initIRC();

    }

    public boolean isLoggedIn() {
        // TODO Auto-generated method stub
        return loggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    public void controlEvent(ControlEvent e) {

        if (e.getID() == ControlEvent.CONTROL_INTERACTION_CALL) {

            if (e.getSource() == Interaction.INTERACTION_AFTER_RECONNECT) {
                initIRC();

            }
            if (e.getSource() == Interaction.INTERACTION_BEFORE_RECONNECT) {
                sendMessage(CHANNEL, "/me is reconnecting...");
                conn.close();

            }

        }
    }

}

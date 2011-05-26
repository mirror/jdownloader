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

package org.jdownloader.extensions.chat;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.TreeMap;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import jd.controlling.JDLogger;
import jd.controlling.reconnect.Reconnecter;
import jd.controlling.reconnect.ReconnecterEvent;
import jd.gui.UserIO;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.SwingGui;
import jd.gui.swing.components.Balloon;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.plugins.AddonPanel;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.event.DefaultEventListener;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.extensions.chat.settings.ChatConfigPanel;
import org.jdownloader.extensions.chat.translate.T;
import org.schwering.irc.lib.IRCConnection;

public class ChatExtension extends AbstractExtension<ChatConfig> {
    private static final long                AWAY_TIMEOUT         = 15 * 60 * 1000;
    private static final Pattern             CMD_ACTION           = Pattern.compile("(me)", Pattern.CASE_INSENSITIVE);
    private static final Pattern             CMD_CONNECT          = Pattern.compile("(connect|verbinden)", Pattern.CASE_INSENSITIVE);
    private static final Pattern             CMD_DISCONNECT       = Pattern.compile("(disconnect|trennen)", Pattern.CASE_INSENSITIVE);
    private static final Pattern             CMD_EXIT             = Pattern.compile("(exit|quit)", Pattern.CASE_INSENSITIVE);
    private static final Pattern             CMD_MODE             = Pattern.compile("(mode|modus)", Pattern.CASE_INSENSITIVE);
    private static final Pattern             CMD_JOIN             = Pattern.compile("join", Pattern.CASE_INSENSITIVE);
    private static final Pattern             CMD_NICK             = Pattern.compile("(nick|name)", Pattern.CASE_INSENSITIVE);
    private static final Pattern             CMD_PM               = Pattern.compile("(msg|query)", Pattern.CASE_INSENSITIVE);
    private static final Pattern             CMD_SLAP             = Pattern.compile("(slap)", Pattern.CASE_INSENSITIVE);
    private static final Pattern             CMD_TOPIC            = Pattern.compile("(topic|title)", Pattern.CASE_INSENSITIVE);
    private static final Pattern             CMD_TRANSLATE        = Pattern.compile("(translate)", Pattern.CASE_INSENSITIVE);
    private static final Pattern             CMD_VERSION          = Pattern.compile("(version|jdversion)", Pattern.CASE_INSENSITIVE);

    private static final ArrayList<String>   COMMANDS             = new ArrayList<String>();

    public static String                     STYLE;
    static {
        try {
            STYLE = IO.readURLToString(Application.getRessourceURL(ChatExtension.class.getPackage().getName().replace(".", "/") + "/styles.css", false));
        } catch (IOException e) {
            STYLE = "";
            e.printStackTrace();

        }
    }
    public static final String               STYLE_ACTION         = "action";
    public static final String               STYLE_ERROR          = "error";
    public static final String               STYLE_HIGHLIGHT      = "highlight";
    public static final String               STYLE_NOTICE         = "notice";
    public static final String               STYLE_PM             = "pm";
    public static final String               STYLE_SELF           = "self";
    public static final String               STYLE_SYSTEM_MESSAGE = "system";

    public static String                     USERLIST_STYLE;
    static {
        try {
            USERLIST_STYLE = IO.readURLToString(Application.getRessourceURL(ChatExtension.class.getPackage().getName().replace(".", "/") + "/userliststyles.css", false));
        } catch (IOException e) {
            USERLIST_STYLE = "";
            e.printStackTrace();

        }
    }

    private JLabel                           top;

    private IRCConnection                    conn;
    private long                             lastAction;
    private String                           lastCommand;
    private boolean                          loggedIn;
    private ArrayList<User>                  NAMES;
    private boolean                          nickaway;
    private int                              nickCount            = 0;
    private String                           orgNick;
    private JTextPane                        right;
    private final TreeMap<String, JDChatPMS> pms                  = new TreeMap<String, JDChatPMS>();
    private StringBuilder                    sb;
    private JScrollPane                      scrollPane;
    private JTextPane                        textArea;
    private JTextField                       textField;

    private JDChatView                       view;

    private JTabbedPane                      tabbedPane;

    private ChatConfigPanel                  configPanel;
    private String                           currentChannel;
    private Thread                           awayChecker;

    public ExtensionConfigPanel<ChatExtension> getConfigPanel() {
        return configPanel;
    }

    public boolean hasConfigPanel() {
        return true;
    }

    @Override
    public String getIconKey() {
        return "chat";
    }

    public ChatExtension() throws StartException {
        super(T._.jd_plugins_optional_jdchat_jdchat());

    }

    public void addPMS(final String user2) {
        final String user = user2.trim();
        if (user.equals(this.conn.getNick().trim())) { return; }
        this.pms.put(user.toLowerCase(), new JDChatPMS(user));
        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                ChatExtension.this.tabbedPane.add(user, ChatExtension.this.pms.get(user.toLowerCase()).getScrollPane());
                return null;
            }
        }.invokeLater();
    }

    public void addToText( final User user, final String style, final String msg) {
        this.addToText(user, style, msg, this.textArea, this.sb);
    }

    public void addToText(final User user, String style, final String msg, final JTextPane targetpane, final StringBuilder sb) {

        final String msg2 = msg;
        final boolean color = !getSettings().isUserColorEnabled();
        final Date dt = new Date();

        final SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        sb.append("<!---->");
        sb.append("<li>");
        if (user != null) {
            if (!color) {
                sb.append("<span style='").append(user.getStyle()).append(this.getUser(this.conn.getNick()) == user ? ";font-weight:bold" : "").append("'>[").append(df.format(dt)).append("] ").append(user.getNickLink("pmnick")).append(ChatExtension.STYLE_PM.equalsIgnoreCase(style) ? ">> " : ": ").append("</span>");
            } else {
                sb.append("<span style='color:#000000").append(this.getUser(this.conn.getNick()) == user ? ";font-weight:bold" : "").append("'>[").append(df.format(dt)).append("] ").append(user.getNickLink("pmnick")).append(ChatExtension.STYLE_PM.equalsIgnoreCase(style) ? ">> " : ": ").append("</span>");
            }
        } else {
            sb.append("<span class='time'>[").append(df.format(dt)).append("] </span>");

        }
        if (this.conn != null && msg.contains(this.conn.getNick())) {
            style = ChatExtension.STYLE_HIGHLIGHT;
        }
        if (style != null) {
            sb.append("<span class='").append(style).append("'>").append(msg).append("</span>");
        } else {
            sb.append("<span>").append(msg).append("</span>");
        }

        new GuiRunnable<Object>() {

            @Override
            public Object runSave() {

                if (!SwingGui.getInstance().getMainFrame().isActive() && ChatExtension.this.conn != null && msg2.contains(ChatExtension.this.conn.getNick())) {
                    // JDSounds.PT("sound.gui.selectPackage");
                    SwingGui.getInstance().getMainFrame().toFront();
                }

                targetpane.setText(ChatExtension.STYLE + "<ul>" + sb.toString() + "</ul>");

                final int max = ChatExtension.this.scrollPane.getVerticalScrollBar().getMaximum();

                ChatExtension.this.scrollPane.getVerticalScrollBar().setValue(max);

                return null;
            }

        }.start();

    }

    public void addUser(final String name) {
        User user;
        if ((user = this.getUser(name)) == null) {
            this.NAMES.add(new User(name));
        } else if (user.rank != new User(name).rank) {
            user.rank = new User(name).rank;
        }
        this.updateNamesPanel();
    }

    public void addUsers(final String[] split) {
        User user;
        for (final String name : split) {

            if ((user = this.getUser(name)) == null) {
                this.NAMES.add(new User(name));
            } else if (user.rank != new User(name).rank) {
                user.rank = new User(name).rank;
            }
        }
        this.updateNamesPanel();
    }

    public void delPMS(final String user) {
        new GuiRunnable<Object>() {

            @Override
            public Object runSave() {
                ChatExtension.this.pms.remove(user.toLowerCase());
                for (int x = 0; x < ChatExtension.this.tabbedPane.getComponentCount(); x++) {
                    if (ChatExtension.this.tabbedPane.getTitleAt(x).toLowerCase().equals(user.toLowerCase())) {
                        ChatExtension.this.tabbedPane.remove(x);
                        return null;
                    }
                }
                return null;
            }
        }.invokeLater();

    }

    protected void doAction(final String type, final String name) {
        if (type.equals("reconnect") && name.equals("reconnect")) {
            if (this.conn == null) {
                this.initIRC();
            }

            return;
        }
        final User usr = this.getUser(name);
        if (this.textField.getText().length() == 0) {
            if (!this.pms.containsKey(usr.name.toLowerCase())) {
                this.addPMS(usr.name);
            }
            for (int x = 0; x < this.tabbedPane.getTabCount(); x++) {
                if (this.tabbedPane.getTitleAt(x).equals(usr.name)) {
                    final int t = x;
                    new GuiRunnable<Object>() {
                        @Override
                        public Object runSave() {
                            ChatExtension.this.tabbedPane.setSelectedIndex(t);
                            return null;
                        }
                    }.invokeLater();
                    break;
                }
            }
        } else {
            new GuiRunnable<Object>() {
                @Override
                public Object runSave() {
                    ChatExtension.this.textField.setText(ChatExtension.this.textField.getText().trim() + " " + usr.name + " ");
                    return null;
                }
            }.invokeLater();
        }
        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                ChatExtension.this.textField.requestFocus();
                return null;
            }
        }.invokeLater();
    }

    public String getNick() {
        return this.conn.getNick();
    }

    public int getNickCount() {
        return this.nickCount;
    }

    public String getNickname() {

        String loc = JDL.getCountryCodeByIP();

        if (loc == null) {
            loc = System.getProperty("user.country");
        } else {
            loc = loc.toLowerCase();
        }
        final String def = "JD-[" + loc + "]_" + ("" + System.currentTimeMillis()).substring(6);
        String nick = getSettings().getNick();
        if (nick == null || nick.equalsIgnoreCase("")) {
            nick = UserIO.getInstance().requestInputDialog(T._.plugins_optional_jdchat_enternick());
            if (nick != null && !nick.equalsIgnoreCase("")) {
                nick += "[" + loc + "]";
            }
            if (nick != null) {
                nick = nick.trim();
            }
            getSettings().setNick(nick);

        }
        if (nick == null) {
            nick = def;
        }
        nick = nick.trim();
        if (this.getNickCount() > 0) {
            nick += "[" + this.getNickCount() + "]";
        }
        return nick;
    }

    public TreeMap<String, JDChatPMS> getPms() {
        return this.pms;
    }

    public User getUser(final String name) {
        for (final User next : this.NAMES) {
            if (next.isUser(name)) { return next; }

        }
        return null;
    }

    private void switchChannel(String newChannel) {

        if (newChannel.equalsIgnoreCase(currentChannel) && this.isLoggedIn()) {
            if (this.conn != null && this.conn.isConnected()) {
                this.addToText(null, ChatExtension.STYLE_NOTICE, "You are in channel: " + newChannel);
            }
            return;
        }
        this.NAMES.clear();
        if (this.conn != null && this.conn.isConnected()) {
            this.addToText(null, ChatExtension.STYLE_NOTICE, "Change channel to: " + newChannel);
        }
        if (this.conn != null && this.conn.isConnected()) {
            this.conn.doPart(getCurrentChannel(), " --> " + newChannel);
        }
        setCurrentChannel(newChannel);
        if (this.conn != null && this.conn.isConnected()) {
            this.conn.doJoin(getCurrentChannel(), null);

            new EDTRunner() {

                @Override
                protected void runInEDT() {
                    tabbedPane.setTitleAt(0, T._.gui_tab_title(getCurrentChannel()));
                }
            };
            getSettings().setChannelLanguage(newChannel);
        }

    }

    public String getCurrentChannel() {
        return currentChannel;
    }

    private void setCurrentChannel(String newChannel) {
        this.currentChannel = newChannel;

    }

    @SuppressWarnings("unchecked")
    private void initGUI() {
        final int userlistposition = getSettings().getUserListPosition();
        this.textArea = new JTextPane();
        final HyperlinkListener hyp = new HyperlinkListener() {

            public void hyperlinkUpdate(final HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    if (e.getDescription().startsWith("intern")) {
                        final String[][] m = new Regex(e.getDescription() + "?", "intern:([\\w]*?)\\|(.*?)\\?").getMatches();
                        if (m.length == 1) {
                            ChatExtension.this.doAction(m[0][0], m[0][1]);
                            return;
                        }
                    } else {
                        CrossSystem.openURL(e.getURL());
                    }
                }

            }

        };

        this.right = new JTextPane();
        this.right.setContentType("text/html");
        this.right.setEditable(false);
        this.textArea.addHyperlinkListener(hyp);
        this.right.addHyperlinkListener(hyp);
        this.scrollPane = new JScrollPane(this.textArea);
        scrollPane.setBorder(null);
        this.tabbedPane = new JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        this.tabbedPane.add("JDChat", this.scrollPane);
        this.tabbedPane.addChangeListener(new ChangeListener() {

            public void stateChanged(final ChangeEvent e) {
                ChatExtension.this.tabbedPane.setForegroundAt(ChatExtension.this.tabbedPane.getSelectedIndex(), Color.black);
            }

        });
        this.textField = new JTextField();
        this.textField.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, Collections.EMPTY_SET);
        this.textField.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, Collections.EMPTY_SET);
        this.textField.addFocusListener(new FocusListener() {

            public void focusGained(final FocusEvent e) {
                ChatExtension.this.tabbedPane.setForegroundAt(ChatExtension.this.tabbedPane.getSelectedIndex(), Color.black);
            }

            public void focusLost(final FocusEvent e) {
                ChatExtension.this.tabbedPane.setForegroundAt(ChatExtension.this.tabbedPane.getSelectedIndex(), Color.black);
            }

        });
        this.textField.addKeyListener(new KeyListener() {

            private int    counter = 0;
            private String last    = null;

            public void keyPressed(final KeyEvent e) {
                final int sel = ChatExtension.this.tabbedPane.getSelectedIndex();
                ChatExtension.this.tabbedPane.setForegroundAt(sel, Color.black);
            }

            public void keyReleased(final KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {

                    if (ChatExtension.this.textField.getText().length() == 0) { return; }
                    if (ChatExtension.this.tabbedPane.getSelectedIndex() == 0 || ChatExtension.this.textField.getText().startsWith("/")) {
                        ChatExtension.this.sendMessage(getCurrentChannel(), ChatExtension.this.textField.getText());
                    } else {
                        ChatExtension.this.sendMessage(getCurrentChannel(), "/msg " + ChatExtension.this.tabbedPane.getTitleAt(ChatExtension.this.tabbedPane.getSelectedIndex()) + " " + ChatExtension.this.textField.getText());
                    }

                } else if (e.getKeyCode() == KeyEvent.VK_TAB) {
                    if (ChatExtension.this.textField.getText().length() == 0) {
                        if (ChatExtension.this.lastCommand != null) {
                            ChatExtension.this.textField.setText(ChatExtension.this.lastCommand);
                            ChatExtension.this.textField.requestFocus();
                        }
                        return;
                    }
                    String txt = ChatExtension.this.textField.getText();
                    if (this.last != null && txt.toLowerCase().startsWith(this.last.toLowerCase())) {
                        txt = this.last;
                    }

                    final String org = txt;
                    final int last = Math.max(0, txt.lastIndexOf(" "));
                    txt = txt.substring(last).trim();
                    final ArrayList<String> users = new ArrayList<String>();

                    final ArrayList<String> strings = new ArrayList<String>();
                    strings.addAll(ChatExtension.COMMANDS);
                    for (final User user : ChatExtension.this.NAMES) {
                        strings.add(user.name);
                    }

                    for (final String user : strings) {
                        if (user.length() >= txt.length() && user.toLowerCase().startsWith(txt.toLowerCase())) {
                            users.add(user);
                        }
                    }
                    if (users.size() == 0) { return; }

                    this.counter++;
                    if (this.counter > users.size() - 1) {
                        this.counter = 0;
                    }
                    final String user = users.get(this.counter);
                    this.last = org;
                    ChatExtension.this.textField.setText((ChatExtension.this.textField.getText().substring(0, last) + " " + user).trim());
                    ChatExtension.this.textField.requestFocus();

                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    if (ChatExtension.this.textField.getText().length() == 0) {
                        if (ChatExtension.this.lastCommand != null) {
                            ChatExtension.this.textField.setText(ChatExtension.this.lastCommand);
                            ChatExtension.this.textField.requestFocus();
                        }
                        return;
                    }

                } else {
                    this.last = null;
                }

            }

            public void keyTyped(final KeyEvent e) {

            }

        });

        this.textArea.setContentType("text/html");
        this.textArea.setEditable(false);

        SwitchPanel frame = new SwitchPanel() {
            private static final long serialVersionUID = 2138710083573682339L;

            @Override
            public void onHide() {
            }

            @Override
            public void onShow() {
            }
        };
        frame.setLayout(new MigLayout("ins 0, wrap 1", "[grow,fill]", "[grow,fill][]"));
        JButton closeTab = new JButton(T._.jd_plugins_optional_jdchat_closeTab());
        closeTab.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                if (ChatExtension.this.tabbedPane.getSelectedIndex() > 0) {
                    ChatExtension.this.delPMS(ChatExtension.this.tabbedPane.getTitleAt(ChatExtension.this.tabbedPane.getSelectedIndex()));
                } else if (ChatExtension.this.tabbedPane.getSelectedIndex() == 0) {
                    ChatExtension.this.addToText(null, ChatExtension.STYLE_SYSTEM_MESSAGE, "You can't close the main Chat!");
                }
            }
        });
        final JScrollPane scrollPane_userlist = new JScrollPane(this.right);
        switch (userlistposition) {
        case 0:
            frame.add(this.tabbedPane, "split 2");
            frame.add(scrollPane_userlist, "width 180:180:180");
            break;
        default:
        case 1:
            frame.add(scrollPane_userlist, "width 180:180:180 ,split 2");
            frame.add(this.tabbedPane);
            break;
        }

        frame.add(this.textField, "growx, split 2");
        frame.add(closeTab, "w pref!");

        this.lastAction = System.currentTimeMillis();
        final MouseMotionListener ml = new MouseMotionListener() {

            public void mouseDragged(final MouseEvent e) {
            }

            public void mouseMoved(final MouseEvent e) {
                ChatExtension.this.lastAction = System.currentTimeMillis();
                ChatExtension.this.setNickAway(false);
            }

        };
        frame.addMouseMotionListener(ml);
        this.textArea.addMouseMotionListener(ml);
        this.textField.addMouseMotionListener(ml);
        this.right.addMouseMotionListener(ml);
        frame.setSize(new Dimension(800, 600));
        frame.setVisible(true);

        this.view = new JDChatView(this) {

            private static final long serialVersionUID = 3966113588850405974L;

            @Override
            protected void initMenu(final JMenuBar menubar) {
                menubar.add(ChatExtension.this.top = new JLabel(T._.jd_plugins_optional_jdchat_JDChat_topic_default()));
                ChatExtension.this.top.setToolTipText(T._.jd_plugins_optional_jdchat_JDChat_topic_tooltip());
            }

        };

        this.view.setContent(frame);
    }

    void initIRC() {

        this.NAMES.clear();
        for (int i = 0; i < 20; i++) {
            final String host = getSettings().getIrcServer();
            final int port = getSettings().getIrcPort();
            final String pass = null;
            final String nick = this.getNickname();
            final String user = "jdChatuser";
            final String name = "jdChatuser";
            Balloon.show("JD Chat", null, "Connecting to JDChat...");
            this.addToText(null, ChatExtension.STYLE_SYSTEM_MESSAGE, "Connecting to JDChat...");
            this.conn = new IRCConnection(host, new int[] { port }, pass, nick, user, name);
            this.conn.setTimeout(1000 * 60 * 60);

            this.conn.addIRCEventListener(new IRCListener(this));
            this.conn.setEncoding("UTF-8");
            this.conn.setPong(true);
            this.conn.setDaemon(false);
            this.conn.setColors(false);
            try {
                this.conn.connect();
                this.startAwayObserver();
                break;
            } catch (final IOException e) {
                this.addToText(null, ChatExtension.STYLE_SYSTEM_MESSAGE, "Connect Timeout. Server not reachable...");
                JDLogger.exception(e);
                try {
                    Thread.sleep(15000);
                } catch (final InterruptedException e1) {

                    JDLogger.exception(e1);
                }
                this.initIRC();
            }

        }

    }

    public boolean isLoggedIn() {
        return this.loggedIn;
    }

    public void notifyPMS(final String user, final String text2) {
        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                for (int x = 0; x < ChatExtension.this.tabbedPane.getTabCount(); x++) {
                    if (ChatExtension.this.tabbedPane.getTitleAt(x).equals(user)) {
                        final int t = x;

                        String text = text2;
                        ChatExtension.this.tabbedPane.setForegroundAt(t, Color.RED);
                        if (text.length() > 40) {
                            text = text.substring(0, 40).concat("...");
                        }
                        if (!ChatExtension.this.tabbedPane.getTitleAt(ChatExtension.this.tabbedPane.getSelectedIndex()).equals(user)) {
                            Balloon.show("JD Chat", null, T._.jd_plugins_optional_jdchat_newmessage(user, text));
                        }
                        return null;
                    }
                }
                return null;
            }
        }.invokeLater();
    }

    public void onConnected() {
        this.switchChannel(getSettings().getChannel());
        this.setLoggedIn(true);
        this.perform();

    }

    public void onMode(final char op, final char mod, final String arg) {
        switch (mod) {
        case 'o':
            if (op == '+') {
                this.getUser(arg).rank = User.RANK_OP;
                this.updateNamesPanel();
            } else {
                this.getUser(arg).rank = User.RANK_DEFAULT;
                this.updateNamesPanel();
            }
            break;
        case 'v':
            if (op == '+') {
                this.getUser(arg).rank = User.RANK_VOICE;
                this.updateNamesPanel();
            } else {
                this.getUser(arg).rank = User.RANK_DEFAULT;
                this.updateNamesPanel();
            }
            break;
        }

    }

    public void perform() {
        final String[] perform = org.appwork.utils.Regex.getLines(getSettings().getPerformOnLoginCommands());
        if (perform == null) { return; }
        for (final String cmd : perform) {
            if (cmd.trim().length() > 0) {
                this.sendMessage(getCurrentChannel(), cmd);
            }
        }
    }

    /**
     * Does modifications to the text before sending it
     */
    private String prepareToSend(final String trim) {
        return trim;
    }

    public void reconnect() {
        this.initIRC();
    }

    public void removeUser(final String name) {
        final User user = this.getUser(name);
        if (user != null) {
            this.NAMES.remove(user);
        }
        this.updateNamesPanel();
    }

    public void renamePMS(final String userOld, final String userNew) {
        new GuiRunnable<Object>() {

            @Override
            public Object runSave() {
                ChatExtension.this.pms.put(userNew.trim().toLowerCase(), ChatExtension.this.pms.get(userOld.trim().toLowerCase()));
                for (int x = 0; x < ChatExtension.this.tabbedPane.getComponentCount(); x++) {
                    if (ChatExtension.this.tabbedPane.getTitleAt(x).equalsIgnoreCase(userOld)) {
                        ChatExtension.this.tabbedPane.remove(x);
                        break;
                    }
                }
                ChatExtension.this.pms.remove(userOld);
                ChatExtension.this.tabbedPane.add(userNew.trim(), ChatExtension.this.pms.get(userNew.trim().toLowerCase()).getScrollPane());
                return null;
            }
        }.invokeLater();
    }

    public void renameUser(final String name, final String name2) {
        final User user = this.getUser(name);
        if (user != null) {
            user.name = name2;
        } else {
            this.addUser(name2);
        }
        this.updateNamesPanel();
    }

    public void requestNameList() {
        this.resetNamesList();
        this.conn.doNames(getCurrentChannel());
    }

    public void resetNamesList() {
        this.NAMES = new ArrayList<User>();
        if (this.getUser(this.conn.getNick().trim()) == null) {
            this.NAMES.add(new User(this.conn.getNick().trim()));
        }
    }

    protected void sendMessage(final String channel2, final String text) {
        this.lastAction = System.currentTimeMillis();
        this.setNickAway(false);
        if (text.startsWith("/")) {
            int end = text.indexOf(" ");
            if (end < 0) {
                end = text.length();
            }
            final String cmd = text.substring(1, end).trim();
            final String rest = text.substring(end).trim();
            if (org.appwork.utils.Regex.matches(cmd, ChatExtension.CMD_PM)) {
                new GuiRunnable<Object>() {

                    @Override
                    public Object runSave() {
                        ChatExtension.this.textField.setText("");
                        return null;
                    }
                }.invokeLater();
                end = rest.indexOf(" ");
                if (end < 0) {
                    end = rest.length();
                }
                if (!this.pms.containsKey(rest.substring(0, end).trim().toLowerCase())) {
                    this.addPMS(rest.substring(0, end).trim());
                }
                this.conn.doPrivmsg(rest.substring(0, end).trim(), this.prepareToSend(rest.substring(end).trim()));
                this.lastCommand = "/msg " + rest.substring(0, end).trim() + " ";
                this.addToText(this.getUser(this.conn.getNick()), ChatExtension.STYLE_SELF, Utils.prepareMsg(rest.substring(end).trim()), this.pms.get(rest.substring(0, end).trim().toLowerCase()).getTextArea(), this.pms.get(rest.substring(0, end).trim().toLowerCase()).getSb());
            } else if (org.appwork.utils.Regex.matches(cmd, ChatExtension.CMD_SLAP)) {
                this.conn.doPrivmsg(channel2, new String(new byte[] { 1 }) + "ACTION  slaps " + rest + " with the whole Javadocs" + new String(new byte[] { 1 }));
                this.addToText(null, ChatExtension.STYLE_ACTION, this.conn.getNick() + " slaps " + rest + " with the whole Javadocs");

                this.lastCommand = "/slap ";
            } else if (org.appwork.utils.Regex.matches(cmd, ChatExtension.CMD_ACTION)) {
                this.lastCommand = "/me ";
                this.conn.doPrivmsg(channel2, new String(new byte[] { 1 }) + "ACTION " + this.prepareToSend(rest.trim()) + new String(new byte[] { 1 }));
                this.addToText(null, ChatExtension.STYLE_ACTION, this.conn.getNick() + " " + Utils.prepareMsg(rest.trim()));

            } else if (org.appwork.utils.Regex.matches(cmd, ChatExtension.CMD_VERSION)) {

                final String msg = " is using " + JDUtilities.getJDTitle() + " with Java " + Application.getJavaVersion() + " on a " + CrossSystem.getOSString() + " system";
                this.conn.doPrivmsg(channel2, new String(new byte[] { 1 }) + "ACTION " + this.prepareToSend(msg) + new String(new byte[] { 1 }));
                this.addToText(null, ChatExtension.STYLE_ACTION, this.conn.getNick() + " " + Utils.prepareMsg(msg));
            } else if (org.appwork.utils.Regex.matches(cmd, ChatExtension.CMD_MODE)) {
                end = rest.indexOf(" ");
                if (end < 0) {
                    end = rest.length();
                }
                this.lastCommand = "/mode ";
                this.conn.doMode(getCurrentChannel(), rest.trim());
            } else if (org.appwork.utils.Regex.matches(cmd, ChatExtension.CMD_TRANSLATE)) {
                end = rest.indexOf(" ");
                if (end < 0) {
                    end = rest.length();
                }
                final String[] tofrom = rest.substring(0, end).trim().split("to");
                if (tofrom == null || tofrom.length != 2) {
                    this.addToText(null, ChatExtension.STYLE_ERROR, "Command /translate " + rest.substring(0, end).trim() + " is not available");
                    return;
                }
                final String t;
                t = JDL.translate(tofrom[0], tofrom[1], Utils.prepareMsg(rest.substring(end).trim())).getTranslated();
                this.lastCommand = "/translate " + rest.substring(0, end).trim() + " ";
                new GuiRunnable<Object>() {

                    @Override
                    public Object runSave() {
                        ChatExtension.this.textField.setText(t);
                        return null;
                    }
                }.invokeLater();
            } else if (org.appwork.utils.Regex.matches(cmd, ChatExtension.CMD_TOPIC)) {
                this.conn.doTopic(getCurrentChannel(), this.prepareToSend(rest));
                this.lastCommand = "/topic ";
            } else if (org.appwork.utils.Regex.matches(cmd, ChatExtension.CMD_JOIN)) {
                this.NAMES.clear();

                switchChannel(rest);

                this.lastCommand = "/join " + rest;
                this.setLoggedIn(true);
                this.perform();
            } else if (org.appwork.utils.Regex.matches(cmd, ChatExtension.CMD_NICK)) {
                this.conn.doNick(rest.trim());
                this.lastCommand = "/nick ";
                getSettings().setNick(rest.trim());

            } else if (org.appwork.utils.Regex.matches(cmd, ChatExtension.CMD_CONNECT)) {
                if (this.conn == null || !this.conn.isConnected()) {
                    this.initIRC();
                }
            } else if (org.appwork.utils.Regex.matches(cmd, ChatExtension.CMD_DISCONNECT)) {
                if (this.conn != null && this.conn.isConnected()) {
                    this.conn.close();
                }
            } else if (org.appwork.utils.Regex.matches(cmd, ChatExtension.CMD_EXIT)) {
                getGUI().setActive(false);
            } else {
                this.addToText(null, ChatExtension.STYLE_ERROR, "Command /" + cmd + " is not available");
            }

        } else {
            this.conn.doPrivmsg(channel2, this.prepareToSend(text));
            this.addToText(this.getUser(this.conn.getNick()), ChatExtension.STYLE_SELF, Utils.prepareMsg(text));
        }
        new GuiRunnable<Object>() {

            @Override
            public Object runSave() {
                ChatExtension.this.textField.setText("");
                ChatExtension.this.textField.requestFocus();
                return null;
            }
        }.invokeLater();
    }

    public void setLoggedIn(final boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    public void setNick(final String nickname) {
        if (nickname == null) { return; }
        this.addToText(null, ChatExtension.STYLE_SYSTEM_MESSAGE, "Rename to " + nickname);

        this.conn.doNick(nickname);
    }

    private void setNickAway(final boolean b) {
        if (this.nickaway == b) { return; }
        this.nickaway = b;
        if (b) {
            this.orgNick = this.conn.getNick();
            this.setNick(this.conn.getNick().substring(0, Math.min(this.conn.getNick().length(), 11)) + "|away");
        } else {
            this.setNick(this.orgNick);
        }

    }

    public void setNickCount(final int nickCount) {
        this.nickCount = nickCount;
    }

    public void setTopic(final String msg) {
        this.addToText(null, ChatExtension.STYLE_SYSTEM_MESSAGE, "<b>Topic is: " + msg + "</b>");
        new GuiRunnable<Object>() {

            @Override
            public Object runSave() {
                ChatExtension.this.top.setText(msg);
                return null;
            }

        }.start();
    }

    private void startAwayObserver() {
        if (awayChecker != null) {
            awayChecker.interrupt();
            awayChecker = null;
        }
        awayChecker = new Thread() {
            @Override
            public void run() {
                while (true) {
                    if (System.currentTimeMillis() - ChatExtension.this.lastAction > ChatExtension.AWAY_TIMEOUT) {
                        ChatExtension.this.setNickAway(true);
                    } else {
                        ChatExtension.this.setNickAway(false);
                    }
                    try {
                        Thread.sleep(10000);
                    } catch (final InterruptedException e) {
                        JDLogger.exception(e);
                    }
                }
            }

        };
        // awayChecker.setDaemon(true);
        awayChecker.start();

    }

    public void updateNamesPanel() {
        final StringBuilder sb = new StringBuilder();
        Collections.sort(this.NAMES);
        final boolean color = !getSettings().isUserColorEnabled();
        sb.append("<ul>");
        for (final User name : this.NAMES) {
            sb.append("<li>");
            if (!color) {
                sb.append("<span style='color:#").append(name.getColor()).append(name.name.equals(this.conn.getNick()) ? ";font-weight:bold;" : "").append("'>");
            } else {
                sb.append("<span style='color:#000000").append(name.name.equals(this.conn.getNick()) ? ";font-weight:bold;" : "").append("'>");
            }
            sb.append(name.getRank()).append(name.getNickLink("query"));
            sb.append("</span></li>");
        }
        sb.append("</ul>");

        if (this.right != null) {
            new GuiRunnable<Object>() {

                @Override
                public Object runSave() {
                    ChatExtension.this.right.setText(ChatExtension.USERLIST_STYLE + sb);
                    return null;
                }

            }.start();
        }
    }

    @Override
    protected void stop() throws StopException {

        this.NAMES.clear();
        this.pms.clear();
        this.setLoggedIn(false);
        this.updateNamesPanel();

        if (awayChecker != null) {
            awayChecker.interrupt();
            awayChecker = null;
        }
        if (this.conn != null) {
            this.conn.close();
        }
        this.conn = null;
    }

    @Override
    protected void start() throws StartException {

    }

    @Override
    public String getConfigID() {
        return "chat";
    }

    @Override
    public String getAuthor() {
        return "JD Team";
    }

    @Override
    public String getDescription() {
        return T._.description();
    }

    @Override
    public ArrayList<MenuAction> getMenuAction() {
        return null;
    }

    @Override
    public AddonPanel getGUI() {

        return view;
    }

    @Override
    protected void initExtension() throws StartException {

        this.NAMES = new ArrayList<User>();
        this.sb = new StringBuilder();

        ChatExtension.COMMANDS.add("/msg ");
        ChatExtension.COMMANDS.add("/topic ");
        ChatExtension.COMMANDS.add("/op ");
        ChatExtension.COMMANDS.add("/deop ");
        ChatExtension.COMMANDS.add("/query ");
        ChatExtension.COMMANDS.add("/nick ");
        ChatExtension.COMMANDS.add("/mode ");
        ChatExtension.COMMANDS.add("/join ");
        configPanel = new ChatConfigPanel(this, getSettings());
        Reconnecter.getInstance().getEventSender().addListener(new DefaultEventListener<ReconnecterEvent>() {

            public void onEvent(final ReconnecterEvent event) {
                // ignore events if gui is not active
                if (ChatExtension.this.textArea == null) { return; }
                if (event.getEventID() == ReconnecterEvent.AFTER) {
                    if (SwingGui.getInstance().getMainFrame().isActive() && !ChatExtension.this.nickaway) {
                        ChatExtension.this.initIRC();
                    } else {
                        ChatExtension.this.addToText(null, ChatExtension.STYLE_ERROR, "You got disconnected because of a reconnect. <a href='intern:reconnect|reconnect'><b>[RECONNECT NOW]</b></a>");
                    }
                } else if (event.getEventID() == ReconnecterEvent.BEFORE) {
                    // sendMessage(CHANNEL, "/me is reconnecting...");
                    if (ChatExtension.this.conn != null && ChatExtension.this.conn.isConnected()) {
                        ChatExtension.this.addToText(null, ChatExtension.STYLE_SYSTEM_MESSAGE, "closing connection due to requested reconnect.");
                        ChatExtension.this.conn.doPart(getCurrentChannel(), "reconnecting...");
                        ChatExtension.this.conn.close();
                        ChatExtension.this.conn = null;
                    }
                }

            }

        });

        this.initGUI();

    }

}
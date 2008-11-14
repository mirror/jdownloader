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
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.MenuItem;
import jd.config.SubConfiguration;
import jd.controlling.interaction.Interaction;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.skins.simple.LocationListener;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.JLinkButton;
import jd.parser.Regex;
import jd.plugins.PluginOptional;
import jd.utils.JDLocale;
import jd.utils.JDSounds;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

import org.schwering.irc.lib.IRCConnection;
import org.schwering.irc.lib.IRCUser;

public class JDChat extends PluginOptional implements ControlListener {
    protected static final long AWAY_TIMEOUT = 15 * 60 * 1000;
    private static final String CHANNEL = "#jDownloader";;
    private static final Pattern CMD_ACTION = Pattern.compile("(me)", Pattern.CASE_INSENSITIVE);;
    private static final Pattern CMD_CONNECT = Pattern.compile("(connect|verbinden)", Pattern.CASE_INSENSITIVE);;
    private static final Pattern CMD_DISCONNECT = Pattern.compile("(disconnect|trennen)", Pattern.CASE_INSENSITIVE);

    private static final Pattern CMD_MODE = Pattern.compile("(mode|modus)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CMD_JOIN = Pattern.compile("join", Pattern.CASE_INSENSITIVE);
    private static final Pattern CMD_NICK = Pattern.compile("(nick|name)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CMD_PM = Pattern.compile("(msg|query)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CMD_SLAP = Pattern.compile("(slap)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CMD_TOPIC = Pattern.compile("(topic|title)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CMD_TRANSLATE = Pattern.compile("(translate)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CMD_VERSION = Pattern.compile("(version|jdversion)", Pattern.CASE_INSENSITIVE);
    protected static final ArrayList<String> COMMANDS = new ArrayList<String>();
    private static final String HOST = "PARAM_" + "HOST";

    private static final String NICK = "PARAM_" + "NICK";
    private static final String PARAM_DESLANGUAGE = "PARAM_DESLANGUAGE";
    private static final String PARAM_DOAUTOTRANSLAT = "PARAM_DOAUTOTRANSLAT";

    private static final String PARAM_DOAUTOTRANSLATSELF = "PARAM_DOAUTOTRANSLATSELF";

    private static final String PARAM_NATIVELANGUAGE = "PARAM_NATIVELANGUAGE";
    private static final String PERFORM = "PARAM_" + "PERFORM";
    private static final String PORT = "PARAM_" + "PORT";
    public static final String STYLE = JDUtilities.getLocalFile(JDUtilities.getResourceFile("plugins/jdchat/styles.css"));
    static final String STYLE_ACTION = "action";
    public static final String STYLE_ERROR = "error";
    public static final String STYLE_HIGHLIGHT = "highlight";
    public static final String STYLE_NOTICE = "notice";
    public static final String STYLE_PM = "pm";
    public static final String STYLE_SELF = "self";
    public static final String STYLE_SYSTEM_MESSAGE = "system";
    private static final int TEXT_BUFFER = 1024 * 600;
    public static final String USERLIST_STYLE = JDUtilities.getLocalFile(JDUtilities.getResourceFile("plugins/jdchat/userliststyles.css"));

    public static int getAddonInterfaceVersion() {
        return 2;
    }

    private boolean changed;

    private IRCConnection conn;
    private JFrame frame;
    private long lastAction;
    private String lastCommand;
    private boolean loggedIn;
    private HashMap<String, String> map;
    private ArrayList<User> NAMES;
    private String nick;
    private boolean nickaway;
    private int nickCount = 0;
    private String orgNick;
    private JTextPane right;
    private StringBuffer sb;
    private JScrollPane scrollPane;
    private JTextPane textArea;
    private JTextField textField;
    private JLabel top;

    public JDChat(PluginWrapper wrapper) {
        super(wrapper);
        COMMANDS.add("/msg ");
        COMMANDS.add("/topic ");
        COMMANDS.add("/op ");
        COMMANDS.add("/deop ");
        COMMANDS.add("/query");
        COMMANDS.add("/nick ");
        COMMANDS.add("/mode ");
        COMMANDS.add("/join");
        COMMANDS.add("/translate ");
        initConfigEntries();
        COMMANDS.add("/translate artoda ");
        COMMANDS.add("/translate artode ");
        COMMANDS.add("/translate artofi ");
        COMMANDS.add("/translate artofr ");
        COMMANDS.add("/translate artoel ");
        COMMANDS.add("/translate artohi ");
        COMMANDS.add("/translate artoit ");
        COMMANDS.add("/translate artoja ");
        COMMANDS.add("/translate artoko ");
        COMMANDS.add("/translate artohr ");
        COMMANDS.add("/translate artonl ");
        COMMANDS.add("/translate artono ");
        COMMANDS.add("/translate artopl ");
        COMMANDS.add("/translate artopt ");
        COMMANDS.add("/translate artoro ");
        COMMANDS.add("/translate artoru ");
        COMMANDS.add("/translate artosv ");
        COMMANDS.add("/translate artoes ");
        COMMANDS.add("/translate artocs ");
        COMMANDS.add("/translate artoen ");
        COMMANDS.add("/translate bgtoar ");
        COMMANDS.add("/translate bgtoda ");
        COMMANDS.add("/translate bgtode ");
        COMMANDS.add("/translate bgtofi ");
        COMMANDS.add("/translate bgtofr ");
        COMMANDS.add("/translate bgtoel ");
        COMMANDS.add("/translate bgtohi ");
        COMMANDS.add("/translate bgtoit ");
        COMMANDS.add("/translate bgtoja ");
        COMMANDS.add("/translate bgtoko ");
        COMMANDS.add("/translate bgtohr ");
        COMMANDS.add("/translate bgtonl ");
        COMMANDS.add("/translate bgtono ");
        COMMANDS.add("/translate bgtopl ");
        COMMANDS.add("/translate bgtopt ");
        COMMANDS.add("/translate bgtoro ");
        COMMANDS.add("/translate bgtoru ");
        COMMANDS.add("/translate bgtosv ");
        COMMANDS.add("/translate bgtoes ");
        COMMANDS.add("/translate bgtocs ");
        COMMANDS.add("/translate bgtoen ");
        COMMANDS.add("/translate datoar ");
        COMMANDS.add("/translate datobg ");
        COMMANDS.add("/translate datode ");
        COMMANDS.add("/translate datofi ");
        COMMANDS.add("/translate datofr ");
        COMMANDS.add("/translate datoel ");
        COMMANDS.add("/translate datohi ");
        COMMANDS.add("/translate datoit ");
        COMMANDS.add("/translate datoja ");
        COMMANDS.add("/translate datoko ");
        COMMANDS.add("/translate datohr ");
        COMMANDS.add("/translate datonl ");
        COMMANDS.add("/translate datono ");
        COMMANDS.add("/translate datopl ");
        COMMANDS.add("/translate datopt ");
        COMMANDS.add("/translate datoro ");
        COMMANDS.add("/translate datoru ");
        COMMANDS.add("/translate datosv ");
        COMMANDS.add("/translate datoes ");
        COMMANDS.add("/translate datocs ");
        COMMANDS.add("/translate datoen ");
        COMMANDS.add("/translate detoar ");
        COMMANDS.add("/translate detobg ");
        COMMANDS.add("/translate detoda ");
        COMMANDS.add("/translate detofi ");
        COMMANDS.add("/translate detofr ");
        COMMANDS.add("/translate detoel ");
        COMMANDS.add("/translate detohi ");
        COMMANDS.add("/translate detoit ");
        COMMANDS.add("/translate detoja ");
        COMMANDS.add("/translate detoko ");
        COMMANDS.add("/translate detohr ");
        COMMANDS.add("/translate detonl ");
        COMMANDS.add("/translate detono ");
        COMMANDS.add("/translate detopl ");
        COMMANDS.add("/translate detopt ");
        COMMANDS.add("/translate detoro ");
        COMMANDS.add("/translate detoru ");
        COMMANDS.add("/translate detosv ");
        COMMANDS.add("/translate detoes ");
        COMMANDS.add("/translate detocs ");
        COMMANDS.add("/translate detoen ");
        COMMANDS.add("/translate fitoar ");
        COMMANDS.add("/translate fitobg ");
        COMMANDS.add("/translate fitoda ");
        COMMANDS.add("/translate fitode ");
        COMMANDS.add("/translate fitofr ");
        COMMANDS.add("/translate fitoel ");
        COMMANDS.add("/translate fitohi ");
        COMMANDS.add("/translate fitoit ");
        COMMANDS.add("/translate fitoja ");
        COMMANDS.add("/translate fitoko ");
        COMMANDS.add("/translate fitohr ");
        COMMANDS.add("/translate fitonl ");
        COMMANDS.add("/translate fitono ");
        COMMANDS.add("/translate fitopl ");
        COMMANDS.add("/translate fitopt ");
        COMMANDS.add("/translate fitoro ");
        COMMANDS.add("/translate fitoru ");
        COMMANDS.add("/translate fitosv ");
        COMMANDS.add("/translate fitoes ");
        COMMANDS.add("/translate fitocs ");
        COMMANDS.add("/translate fitoen ");
        COMMANDS.add("/translate frtoar ");
        COMMANDS.add("/translate frtobg ");
        COMMANDS.add("/translate frtoda ");
        COMMANDS.add("/translate frtode ");
        COMMANDS.add("/translate frtofi ");
        COMMANDS.add("/translate frtoel ");
        COMMANDS.add("/translate frtohi ");
        COMMANDS.add("/translate frtoit ");
        COMMANDS.add("/translate frtoja ");
        COMMANDS.add("/translate frtoko ");
        COMMANDS.add("/translate frtohr ");
        COMMANDS.add("/translate frtonl ");
        COMMANDS.add("/translate frtono ");
        COMMANDS.add("/translate frtopl ");
        COMMANDS.add("/translate frtopt ");
        COMMANDS.add("/translate frtoro ");
        COMMANDS.add("/translate frtoru ");
        COMMANDS.add("/translate frtosv ");
        COMMANDS.add("/translate frtoes ");
        COMMANDS.add("/translate frtocs ");
        COMMANDS.add("/translate frtoen ");
        COMMANDS.add("/translate eltoar ");
        COMMANDS.add("/translate eltobg ");
        COMMANDS.add("/translate eltoda ");
        COMMANDS.add("/translate eltode ");
        COMMANDS.add("/translate eltofi ");
        COMMANDS.add("/translate eltofr ");
        COMMANDS.add("/translate eltohi ");
        COMMANDS.add("/translate eltoit ");
        COMMANDS.add("/translate eltoja ");
        COMMANDS.add("/translate eltoko ");
        COMMANDS.add("/translate eltohr ");
        COMMANDS.add("/translate eltonl ");
        COMMANDS.add("/translate eltono ");
        COMMANDS.add("/translate eltopl ");
        COMMANDS.add("/translate eltopt ");
        COMMANDS.add("/translate eltoro ");
        COMMANDS.add("/translate eltoru ");
        COMMANDS.add("/translate eltosv ");
        COMMANDS.add("/translate eltoes ");
        COMMANDS.add("/translate eltocs ");
        COMMANDS.add("/translate eltoen ");
        COMMANDS.add("/translate hitoar ");
        COMMANDS.add("/translate hitobg ");
        COMMANDS.add("/translate hitoda ");
        COMMANDS.add("/translate hitode ");
        COMMANDS.add("/translate hitofi ");
        COMMANDS.add("/translate hitofr ");
        COMMANDS.add("/translate hitoel ");
        COMMANDS.add("/translate hitoit ");
        COMMANDS.add("/translate hitoja ");
        COMMANDS.add("/translate hitoko ");
        COMMANDS.add("/translate hitohr ");
        COMMANDS.add("/translate hitonl ");
        COMMANDS.add("/translate hitono ");
        COMMANDS.add("/translate hitopl ");
        COMMANDS.add("/translate hitopt ");
        COMMANDS.add("/translate hitoro ");
        COMMANDS.add("/translate hitoru ");
        COMMANDS.add("/translate hitosv ");
        COMMANDS.add("/translate hitoes ");
        COMMANDS.add("/translate hitocs ");
        COMMANDS.add("/translate hitoen ");
        COMMANDS.add("/translate ittoar ");
        COMMANDS.add("/translate ittobg ");
        COMMANDS.add("/translate ittoda ");
        COMMANDS.add("/translate ittode ");
        COMMANDS.add("/translate ittofi ");
        COMMANDS.add("/translate ittofr ");
        COMMANDS.add("/translate ittoel ");
        COMMANDS.add("/translate ittohi ");
        COMMANDS.add("/translate ittoja ");
        COMMANDS.add("/translate ittoko ");
        COMMANDS.add("/translate ittohr ");
        COMMANDS.add("/translate ittonl ");
        COMMANDS.add("/translate ittono ");
        COMMANDS.add("/translate ittopl ");
        COMMANDS.add("/translate ittopt ");
        COMMANDS.add("/translate ittoro ");
        COMMANDS.add("/translate ittoru ");
        COMMANDS.add("/translate ittosv ");
        COMMANDS.add("/translate ittoes ");
        COMMANDS.add("/translate ittocs ");
        COMMANDS.add("/translate ittoen ");
        COMMANDS.add("/translate jatoar ");
        COMMANDS.add("/translate jatobg ");
        COMMANDS.add("/translate jatoda ");
        COMMANDS.add("/translate jatode ");
        COMMANDS.add("/translate jatofi ");
        COMMANDS.add("/translate jatofr ");
        COMMANDS.add("/translate jatoel ");
        COMMANDS.add("/translate jatohi ");
        COMMANDS.add("/translate jatoit ");
        COMMANDS.add("/translate jatoko ");
        COMMANDS.add("/translate jatohr ");
        COMMANDS.add("/translate jatonl ");
        COMMANDS.add("/translate jatono ");
        COMMANDS.add("/translate jatopl ");
        COMMANDS.add("/translate jatopt ");
        COMMANDS.add("/translate jatoro ");
        COMMANDS.add("/translate jatoru ");
        COMMANDS.add("/translate jatosv ");
        COMMANDS.add("/translate jatoes ");
        COMMANDS.add("/translate jatocs ");
        COMMANDS.add("/translate jatoen ");
        COMMANDS.add("/translate kotoar ");
        COMMANDS.add("/translate kotobg ");
        COMMANDS.add("/translate kotoda ");
        COMMANDS.add("/translate kotode ");
        COMMANDS.add("/translate kotofi ");
        COMMANDS.add("/translate kotofr ");
        COMMANDS.add("/translate kotoel ");
        COMMANDS.add("/translate kotohi ");
        COMMANDS.add("/translate kotoit ");
        COMMANDS.add("/translate kotoja ");
        COMMANDS.add("/translate kotohr ");
        COMMANDS.add("/translate kotonl ");
        COMMANDS.add("/translate kotono ");
        COMMANDS.add("/translate kotopl ");
        COMMANDS.add("/translate kotopt ");
        COMMANDS.add("/translate kotoro ");
        COMMANDS.add("/translate kotoru ");
        COMMANDS.add("/translate kotosv ");
        COMMANDS.add("/translate kotoes ");
        COMMANDS.add("/translate kotocs ");
        COMMANDS.add("/translate kotoen ");
        COMMANDS.add("/translate hrtoar ");
        COMMANDS.add("/translate hrtobg ");
        COMMANDS.add("/translate hrtoda ");
        COMMANDS.add("/translate hrtode ");
        COMMANDS.add("/translate hrtofi ");
        COMMANDS.add("/translate hrtofr ");
        COMMANDS.add("/translate hrtoel ");
        COMMANDS.add("/translate hrtohi ");
        COMMANDS.add("/translate hrtoit ");
        COMMANDS.add("/translate hrtoja ");
        COMMANDS.add("/translate hrtoko ");
        COMMANDS.add("/translate hrtonl ");
        COMMANDS.add("/translate hrtono ");
        COMMANDS.add("/translate hrtopl ");
        COMMANDS.add("/translate hrtopt ");
        COMMANDS.add("/translate hrtoro ");
        COMMANDS.add("/translate hrtoru ");
        COMMANDS.add("/translate hrtosv ");
        COMMANDS.add("/translate hrtoes ");
        COMMANDS.add("/translate hrtocs ");
        COMMANDS.add("/translate hrtoen ");
        COMMANDS.add("/translate nltoar ");
        COMMANDS.add("/translate nltobg ");
        COMMANDS.add("/translate nltoda ");
        COMMANDS.add("/translate nltode ");
        COMMANDS.add("/translate nltofi ");
        COMMANDS.add("/translate nltofr ");
        COMMANDS.add("/translate nltoel ");
        COMMANDS.add("/translate nltohi ");
        COMMANDS.add("/translate nltoit ");
        COMMANDS.add("/translate nltoja ");
        COMMANDS.add("/translate nltoko ");
        COMMANDS.add("/translate nltohr ");
        COMMANDS.add("/translate nltono ");
        COMMANDS.add("/translate nltopl ");
        COMMANDS.add("/translate nltopt ");
        COMMANDS.add("/translate nltoro ");
        COMMANDS.add("/translate nltoru ");
        COMMANDS.add("/translate nltosv ");
        COMMANDS.add("/translate nltoes ");
        COMMANDS.add("/translate nltocs ");
        COMMANDS.add("/translate nltoen ");
        COMMANDS.add("/translate notoar ");
        COMMANDS.add("/translate notobg ");
        COMMANDS.add("/translate notoda ");
        COMMANDS.add("/translate notode ");
        COMMANDS.add("/translate notofi ");
        COMMANDS.add("/translate notofr ");
        COMMANDS.add("/translate notoel ");
        COMMANDS.add("/translate notohi ");
        COMMANDS.add("/translate notoit ");
        COMMANDS.add("/translate notoja ");
        COMMANDS.add("/translate notoko ");
        COMMANDS.add("/translate notohr ");
        COMMANDS.add("/translate notonl ");
        COMMANDS.add("/translate notopl ");
        COMMANDS.add("/translate notopt ");
        COMMANDS.add("/translate notoro ");
        COMMANDS.add("/translate notoru ");
        COMMANDS.add("/translate notosv ");
        COMMANDS.add("/translate notoes ");
        COMMANDS.add("/translate notocs ");
        COMMANDS.add("/translate notoen ");
        COMMANDS.add("/translate pltoar ");
        COMMANDS.add("/translate pltobg ");
        COMMANDS.add("/translate pltoda ");
        COMMANDS.add("/translate pltode ");
        COMMANDS.add("/translate pltofi ");
        COMMANDS.add("/translate pltofr ");
        COMMANDS.add("/translate pltoel ");
        COMMANDS.add("/translate pltohi ");
        COMMANDS.add("/translate pltoit ");
        COMMANDS.add("/translate pltoja ");
        COMMANDS.add("/translate pltoko ");
        COMMANDS.add("/translate pltohr ");
        COMMANDS.add("/translate pltonl ");
        COMMANDS.add("/translate pltono ");
        COMMANDS.add("/translate pltopt ");
        COMMANDS.add("/translate pltoro ");
        COMMANDS.add("/translate pltoru ");
        COMMANDS.add("/translate pltosv ");
        COMMANDS.add("/translate pltoes ");
        COMMANDS.add("/translate pltocs ");
        COMMANDS.add("/translate pltoen ");
        COMMANDS.add("/translate pttoar ");
        COMMANDS.add("/translate pttobg ");
        COMMANDS.add("/translate pttoda ");
        COMMANDS.add("/translate pttode ");
        COMMANDS.add("/translate pttofi ");
        COMMANDS.add("/translate pttofr ");
        COMMANDS.add("/translate pttoel ");
        COMMANDS.add("/translate pttohi ");
        COMMANDS.add("/translate pttoit ");
        COMMANDS.add("/translate pttoja ");
        COMMANDS.add("/translate pttoko ");
        COMMANDS.add("/translate pttohr ");
        COMMANDS.add("/translate pttonl ");
        COMMANDS.add("/translate pttono ");
        COMMANDS.add("/translate pttopl ");
        COMMANDS.add("/translate pttoro ");
        COMMANDS.add("/translate pttoru ");
        COMMANDS.add("/translate pttosv ");
        COMMANDS.add("/translate pttoes ");
        COMMANDS.add("/translate pttocs ");
        COMMANDS.add("/translate pttoen ");
        COMMANDS.add("/translate rotoar ");
        COMMANDS.add("/translate rotobg ");
        COMMANDS.add("/translate rotoda ");
        COMMANDS.add("/translate rotode ");
        COMMANDS.add("/translate rotofi ");
        COMMANDS.add("/translate rotofr ");
        COMMANDS.add("/translate rotoel ");
        COMMANDS.add("/translate rotohi ");
        COMMANDS.add("/translate rotoit ");
        COMMANDS.add("/translate rotoja ");
        COMMANDS.add("/translate rotoko ");
        COMMANDS.add("/translate rotohr ");
        COMMANDS.add("/translate rotonl ");
        COMMANDS.add("/translate rotono ");
        COMMANDS.add("/translate rotopl ");
        COMMANDS.add("/translate rotopt ");
        COMMANDS.add("/translate rotoru ");
        COMMANDS.add("/translate rotosv ");
        COMMANDS.add("/translate rotoes ");
        COMMANDS.add("/translate rotocs ");
        COMMANDS.add("/translate rotoen ");
        COMMANDS.add("/translate rutoar ");
        COMMANDS.add("/translate rutobg ");
        COMMANDS.add("/translate rutoda ");
        COMMANDS.add("/translate rutode ");
        COMMANDS.add("/translate rutofi ");
        COMMANDS.add("/translate rutofr ");
        COMMANDS.add("/translate rutoel ");
        COMMANDS.add("/translate rutohi ");
        COMMANDS.add("/translate rutoit ");
        COMMANDS.add("/translate rutoja ");
        COMMANDS.add("/translate rutoko ");
        COMMANDS.add("/translate rutohr ");
        COMMANDS.add("/translate rutonl ");
        COMMANDS.add("/translate rutono ");
        COMMANDS.add("/translate rutopl ");
        COMMANDS.add("/translate rutopt ");
        COMMANDS.add("/translate rutoro ");
        COMMANDS.add("/translate rutosv ");
        COMMANDS.add("/translate rutoes ");
        COMMANDS.add("/translate rutocs ");
        COMMANDS.add("/translate rutoen ");
        COMMANDS.add("/translate svtoar ");
        COMMANDS.add("/translate svtobg ");
        COMMANDS.add("/translate svtoda ");
        COMMANDS.add("/translate svtode ");
        COMMANDS.add("/translate svtofi ");
        COMMANDS.add("/translate svtofr ");
        COMMANDS.add("/translate svtoel ");
        COMMANDS.add("/translate svtohi ");
        COMMANDS.add("/translate svtoit ");
        COMMANDS.add("/translate svtoja ");
        COMMANDS.add("/translate svtoko ");
        COMMANDS.add("/translate svtohr ");
        COMMANDS.add("/translate svtonl ");
        COMMANDS.add("/translate svtono ");
        COMMANDS.add("/translate svtopl ");
        COMMANDS.add("/translate svtopt ");
        COMMANDS.add("/translate svtoro ");
        COMMANDS.add("/translate svtoru ");
        COMMANDS.add("/translate svtoes ");
        COMMANDS.add("/translate svtocs ");
        COMMANDS.add("/translate svtoen ");
        COMMANDS.add("/translate estoar ");
        COMMANDS.add("/translate estobg ");
        COMMANDS.add("/translate estoda ");
        COMMANDS.add("/translate estode ");
        COMMANDS.add("/translate estofi ");
        COMMANDS.add("/translate estofr ");
        COMMANDS.add("/translate estoel ");
        COMMANDS.add("/translate estohi ");
        COMMANDS.add("/translate estoit ");
        COMMANDS.add("/translate estoja ");
        COMMANDS.add("/translate estoko ");
        COMMANDS.add("/translate estohr ");
        COMMANDS.add("/translate estonl ");
        COMMANDS.add("/translate estono ");
        COMMANDS.add("/translate estopl ");
        COMMANDS.add("/translate estopt ");
        COMMANDS.add("/translate estoro ");
        COMMANDS.add("/translate estoru ");
        COMMANDS.add("/translate estosv ");
        COMMANDS.add("/translate estocs ");
        COMMANDS.add("/translate estoen ");
        COMMANDS.add("/translate cstoar ");
        COMMANDS.add("/translate cstobg ");
        COMMANDS.add("/translate cstoda ");
        COMMANDS.add("/translate cstode ");
        COMMANDS.add("/translate cstofi ");
        COMMANDS.add("/translate cstofr ");
        COMMANDS.add("/translate cstoel ");
        COMMANDS.add("/translate cstohi ");
        COMMANDS.add("/translate cstoit ");
        COMMANDS.add("/translate cstoja ");
        COMMANDS.add("/translate cstoko ");
        COMMANDS.add("/translate cstohr ");
        COMMANDS.add("/translate cstonl ");
        COMMANDS.add("/translate cstono ");
        COMMANDS.add("/translate cstopl ");
        COMMANDS.add("/translate cstopt ");
        COMMANDS.add("/translate cstoro ");
        COMMANDS.add("/translate cstoru ");
        COMMANDS.add("/translate cstosv ");
        COMMANDS.add("/translate cstoes ");
        COMMANDS.add("/translate cstoen ");
        COMMANDS.add("/translate entoar ");
        COMMANDS.add("/translate entobg ");
        COMMANDS.add("/translate entoda ");
        COMMANDS.add("/translate entode ");
        COMMANDS.add("/translate entofi ");
        COMMANDS.add("/translate entofr ");
        COMMANDS.add("/translate entoel ");
        COMMANDS.add("/translate entohi ");
        COMMANDS.add("/translate entoit ");
        COMMANDS.add("/translate entoja ");
        COMMANDS.add("/translate entoko ");
        COMMANDS.add("/translate entohr ");
        COMMANDS.add("/translate entonl ");
        COMMANDS.add("/translate entono ");
        COMMANDS.add("/translate entopl ");
        COMMANDS.add("/translate entopt ");
        COMMANDS.add("/translate entoro ");
        COMMANDS.add("/translate entoru ");
        COMMANDS.add("/translate entosv ");
        COMMANDS.add("/translate entoes ");
        COMMANDS.add("/translate entocs ");

    }

    public void actionPerformed(ActionEvent e) {
        if (frame == null || !frame.isVisible() || conn == null || !conn.isConnected()) {
            if (conn != null) {
                conn.close();
            }
            setEnabled(true);

        } else {
            setEnabled(false);
        }
    }

    public void addToText(final User user, String style, String msg) {

        SubConfiguration conf = JDUtilities.getSubConfig("JDCHAT");
        String dest = conf.getStringProperty(PARAM_NATIVELANGUAGE, map.get(System.getProperty("user.country")));
        if (conf.getBooleanProperty(PARAM_DOAUTOTRANSLAT, false) && dest != null && !msg.contains("<")) {

            for (String next : map.keySet()) {
                if (map.get(next).equals(dest)) {
                    String tmp = JDLocale.translate(next, msg);
                    if (!tmp.equalsIgnoreCase(msg)) {
                        tmp += "(" + msg + ")";
                        msg = tmp;
                    }
                    break;
                }

            }

        }
        final String msg2 = msg;

        Date dt = new Date();

        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        sb.append("<!---->");
        sb.append("<li>");
        if (user != null) {
            sb.append("<span style='" + user.getStyle() + (getUser(conn.getNick()) == user ? ";font-weight:bold" : "") + "'>[" + df.format(dt) + "] " + user.getNickLink("pmnick") + (style == JDChat.STYLE_PM ? ">> " : ": ") + "</span>");
        } else {
            sb.append("<span class='time'>[" + df.format(dt) + "] </span>");

        }
        if (conn != null && msg.contains(conn.getNick())) {
            style = STYLE_HIGHLIGHT;
        }
        if (style != null) {
            sb.append("<span class='" + style + "'>" + msg + "</span>");
        } else {
            sb.append("<span>" + msg + "</span>");
        }

        if (sb.length() > TEXT_BUFFER) {
            String tmp = sb.toString();
            tmp = tmp.substring(tmp.indexOf("<!---->", sb.length() / 3)).trim();
            sb = new StringBuffer();
            sb.append(tmp);
        }
        changed = true;
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                if (changed) {

                    if (!frame.isActive() && conn != null && msg2.contains(conn.getNick())) {
                        JDSounds.PT("sound.gui.selectPackage");
                        frame.toFront();
                    }

                    textArea.setText(STYLE + "<ul>" + sb.toString() + "</ul>");

                    int max = scrollPane.getVerticalScrollBar().getMaximum();

                    scrollPane.getVerticalScrollBar().setValue(max);
                    changed = false;
                }

            }
        });

    }

    public void addUser(String name) {
        User user;
        if ((user = getUser(name)) == null) {
            NAMES.add(new User(name));
        } else if (user.rank != new User(name).rank) {
            user.rank = new User(name).rank;
        }
        updateNamesPanel();
    }

    public void addUsers(String[] split) {
        User user;
        for (String name : split) {

            if ((user = getUser(name)) == null) {
                NAMES.add(new User(name));
            } else if (user.rank != new User(name).rank) {
                user.rank = new User(name).rank;
            }
        }
        updateNamesPanel();
    }

    public void controlEvent(ControlEvent e) {

        if (e.getID() == ControlEvent.CONTROL_INTERACTION_CALL) {

            if (e.getSource() == Interaction.INTERACTION_AFTER_RECONNECT) {
                if (frame.isActive() && !nickaway) {
                    initIRC();
                } else {
                    addToText(null, STYLE_ERROR, "You got disconnected because of a reconnect. <a href='intern:reconnect|reconnect'><b>[RECONNECT NOW]</b></a>");

                }

            }
            if (e.getSource() == Interaction.INTERACTION_BEFORE_RECONNECT) {
                // sendMessage(CHANNEL, "/me is reconnecting...");
                if (conn != null && conn.isConnected()) {
                    addToText(null, STYLE_SYSTEM_MESSAGE, "closing connection due to requested reconnect.");
                    conn.doPart(CHANNEL, "reconnecting...");
                    conn.close();
                    conn = null;
                }

            }

        }
    }

    public ArrayList<MenuItem> createMenuitems() {
        ArrayList<MenuItem> menu = new ArrayList<MenuItem>();
        MenuItem m;

        menu.add(m = new MenuItem(MenuItem.TOGGLE, JDLocale.L("plugins.optional.jdchat.menu.windowstatus", "Chatwindow"), 0).setActionListener(this));
        if (frame == null || !frame.isVisible()) {
            m.setSelected(false);
        } else {
            m.setSelected(true);
        }
        return menu;
    }

    protected void doAction(String type, String name) {
        if (type.equals("reconnect") && name.equals("reconnect")) {
            if (conn == null) {
                initIRC();
            }

            return;
        }
        if (textField.getText().length() == 0) {
            textField.setText("/msg " + getUser(name).name + " ");
        } else {

            textField.setText(textField.getText().trim() + " " + getUser(name).name + " ");
        }

        textField.requestFocus();
    }

    public String getNick() {
        return conn.getNick();
    }

    public int getNickCount() {
        return nickCount;
    }

    String getNickname() {

        String loc = System.getProperty("user.country");
        if (loc == null) {
            loc = JDLocale.getLocale().substring(0, 3);
        }
        String def = "JD-[" + loc + "]_" + ("" + System.currentTimeMillis()).substring(6);
        nick = JDUtilities.getSubConfig("JDCHAT").getStringProperty(NICK);
        if (nick == null) {
            nick = JDUtilities.getGUI().showUserInputDialog(JDLocale.L("plugins.optional.jdchat.enternick", "Your wished nickname?"));
            if (nick != null) {
                nick += "[" + loc + "]";
            }
            JDUtilities.getSubConfig("JDCHAT").setProperty(NICK, nick.trim());
            JDUtilities.getSubConfig("JDCHAT").save();
        }
        if (nick == null) {
            nick = def;
        }
        nick = nick.trim();
        if (getNickCount() > 0) {
            nick += "[" + getNickCount() + "]";
        }
        return nick;
    }

    public String getHost() {
        return JDLocale.L("plugins.optional.jdchat.name", "JD Chat");
    }

    public String getRequirements() {
        return "JRE 1.5+";
    }

    public User getUser(String name) {
        for (User next : NAMES) {
            if (next.isUser(name)) { return next; }

        }
        return null;

    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    public boolean initAddon() {
        NAMES = new ArrayList<User>();
        sb = new StringBuffer();
    
        return true;
    }

    private void initConfigEntries() {
        SubConfiguration subConfig = JDUtilities.getSubConfig("JDCHAT");

        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, subConfig, NICK, JDLocale.L("plugins.optional.jdchat.user", "Nickname")));

        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, subConfig, PERFORM, JDLocale.L("plugins.optional.jdchat.performonstart", "Perform commands after connection estabilished")));

        ConfigContainer lngse = new ConfigContainer(this, JDLocale.L("plugins.optional.jdchat.locale", "Language settings"));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CONTAINER, lngse));
        lngse.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, PARAM_DOAUTOTRANSLAT, JDLocale.L("plugins.optional.jdchat.doautotranslate", "Translate Chat")));
        cfg.setDefaultValue(false);
        ConfigEntry conditionEntry = cfg;

        map = new HashMap<String, String>();
        map.put("ar", JDLocale.L("locale.lngs.arabic", "Arabic"));
        map.put("bg", JDLocale.L("locale.lngs.bulgarian", "Bulgarian"));
        map.put("zh-CN", JDLocale.L("locale.lngs.chinese_simplified_", "Chinese (Simplified)"));
        map.put("zh-TW", JDLocale.L("locale.lngs.chinese_traditional_", "Chinese (Traditional)"));
        map.put("hr", JDLocale.L("locale.lngs.croatian", "Croatian"));
        map.put("cs", JDLocale.L("locale.lngs.czech", "Czech"));
        map.put("da", JDLocale.L("locale.lngs.danish", "Danish"));
        map.put("nl", JDLocale.L("locale.lngs.dutch", "Dutch"));
        map.put("en", JDLocale.L("locale.lngs.english", "English"));
        map.put("fi", JDLocale.L("locale.lngs.finnish", "Finnish"));
        map.put("fr", JDLocale.L("locale.lngs.french", "French"));
        map.put("de", JDLocale.L("locale.lngs.german", "German"));
        map.put("el", JDLocale.L("locale.lngs.greek", "Greek"));
        map.put("hi", JDLocale.L("locale.lngs.hindi", "Hindi"));
        map.put("it", JDLocale.L("locale.lngs.italian", "Italian"));
        map.put("ja", JDLocale.L("locale.lngs.japanese", "Japanese"));
        map.put("ko", JDLocale.L("locale.lngs.korean", "Korean"));
        map.put("no", JDLocale.L("locale.lngs.norwegian", "Norwegian"));
        map.put("pl", JDLocale.L("locale.lngs.polish", "Polish"));
        map.put("pt", JDLocale.L("locale.lngs.portuguese", "Portuguese"));
        map.put("ro", JDLocale.L("locale.lngs.romanian", "Romanian"));
        map.put("ru", JDLocale.L("locale.lngs.russian", "Russian"));
        map.put("es", JDLocale.L("locale.lngs.spanish", "Spanish"));
        map.put("sv", JDLocale.L("locale.lngs.swedish", "Swedish"));

        ArrayList<String> ar = new ArrayList<String>();

        for (String string : map.keySet()) {
            ar.add(map.get(string));
        }

        lngse.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, subConfig, PARAM_NATIVELANGUAGE, ar.toArray(new String[] {}), JDLocale.L("interaction.jdchat.native", "to: ")));
        cfg.setEnabledCondidtion(conditionEntry, "==", true);

        lngse.addEntry(conditionEntry = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, PARAM_DOAUTOTRANSLATSELF, JDLocale.L("plugins.optional.jdchat.doautotranslateself", "Translate everything I say")));
        conditionEntry.setDefaultValue(false);

        lngse.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, subConfig, PARAM_DESLANGUAGE, ar.toArray(new String[] {}), JDLocale.L("interaction.jdchat.deslanguage", "to: ")));
        cfg.setEnabledCondidtion(conditionEntry, "==", true);

    }

    @SuppressWarnings("unchecked")
    private void initGUI() {

        frame = new JFrame();
        frame.setTitle(JDLocale.L("plugins.optional.jdChat.gui.title", "JD Chat"));
        frame.setIconImage(JDTheme.I("gui.images.config.network_local"));
        frame.setPreferredSize(new Dimension(1000, 600));
        frame.setName("JDCHAT");
        frame.addWindowListener(new LocationListener());
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
                        try {
                            JLinkButton.openURL(e.getURL());

                        } catch (Exception e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        }
                    }
                }

            }

        };
        frame.addWindowListener(new WindowListener() {

            public void windowActivated(WindowEvent e) {
            }

            public void windowClosed(WindowEvent e) {
            }

            public void windowClosing(WindowEvent e) {
                if (conn != null || conn.isConnected()) {
                    conn.close();
                }

            }

            public void windowDeactivated(WindowEvent e) {
            }

            public void windowDeiconified(WindowEvent e) {
            }

            public void windowIconified(WindowEvent e) {
            }

            public void windowOpened(WindowEvent e) {
            }

        });

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

            private int counter = 0;
            private String last = null;

            public void keyPressed(KeyEvent e) {

            }

            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {

                    if (textField.getText().length() == 0) { return; }

                    sendMessage(CHANNEL, textField.getText());

                } else if (e.getKeyCode() == KeyEvent.VK_TAB) {
                    if (textField.getText().length() == 0) {
                        if (lastCommand != null) {
                            textField.setText(lastCommand);
                            textField.requestFocus();
                        }
                        return;
                    }
                    String txt = textField.getText();
                    if (last != null && txt.toLowerCase().startsWith(last.toLowerCase())) {
                        txt = last;
                    }

                    String org = txt;
                    int last = Math.max(0, txt.lastIndexOf(" "));
                    txt = txt.substring(last).trim();
                    ArrayList<String> users = new ArrayList<String>();

                    ArrayList<String> strings = new ArrayList<String>();
                    strings.addAll(COMMANDS);
                    for (User user : NAMES) {
                        strings.add(user.name);

                    }

                    for (String user : strings) {
                        if (user.length() >= txt.length() && user.toLowerCase().startsWith(txt.toLowerCase())) {
                            users.add(user);
                            // return;

                        }
                    }
                    if (users.size() == 0) { return; }

                    counter++;
                    if (counter > users.size() - 1) {
                        counter = 0;
                    }
                    String user = users.get(counter);
                    this.last = org;
                    textField.setText((textField.getText().substring(0, last) + " " + user).trim());
                    textField.requestFocus();

                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    if (textField.getText().length() == 0) {
                        if (lastCommand != null) {
                            textField.setText(lastCommand);
                            textField.requestFocus();
                        }
                        return;
                    }

                } else {
                    last = null;
                }

            }

            public void keyTyped(KeyEvent e) {

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
        lastAction = System.currentTimeMillis();
        MouseMotionListener ml = new MouseMotionListener() {

            public void mouseDragged(MouseEvent e) {
            }

            public void mouseMoved(MouseEvent e) {
                lastAction = System.currentTimeMillis();
                setNickAway(false);

            }

        };
        frame.addMouseMotionListener(ml);
        textArea.addMouseMotionListener(ml);
        textField.addMouseMotionListener(ml);
        right.addMouseMotionListener(ml);
        frame.pack();
        SimpleGUI.restoreWindow(null, frame);
        frame.setVisible(true);
        startAwayObserver();
    }

    private void initIRC() {
        for (int i = 0; i < 20; i++) {
            SubConfiguration conf = JDUtilities.getSubConfig("JDCHAT");
            String host = conf.getStringProperty(HOST, "irc.freenode.net");
            int port = conf.getIntegerProperty(PORT, 6667);
            String pass = null;
            String nick = getNickname();
            String user = "jdChatuser";
            String name = "jdChatuser";
            addToText(null, STYLE_SYSTEM_MESSAGE, "Connecting to JDChat...");
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

                addToText(null, STYLE_SYSTEM_MESSAGE, "Connect Timeout. Server not reachable...");
                e.printStackTrace();
                try {
                    Thread.sleep(15000);
                } catch (InterruptedException e1) {

                    e1.printStackTrace();
                }
                initIRC();
            }
        }

    }

    public boolean isLoggedIn() {

        return loggedIn;
    }

    public void onConnected() {
        conn.doJoin(CHANNEL, null);
        setLoggedIn(true);
        perform();

    }

    public void onExit() {
        if (conn != null) conn.close();
    }

    public void onMode(IRCUser u, char op, char mod, String arg) {
        switch (mod) {
        case 'o':

            if (op == '+') {
                getUser(arg).rank = User.RANK_OP;
                updateNamesPanel();
            } else {
                getUser(arg).rank = User.RANK_DEFAULT;
                updateNamesPanel();
            }
            break;
        case 'v':
            if (op == '+') {
                getUser(arg).rank = User.RANK_VOICE;
                updateNamesPanel();
            } else {
                getUser(arg).rank = User.RANK_DEFAULT;
                updateNamesPanel();
            }
            break;
        }

    }

    public void perform() {
        String[] perform = Regex.getLines(JDUtilities.getSubConfig("JDCHAT").getStringProperty(PERFORM));
        if (perform == null) { return; }
        for (String cmd : perform) {
            if (cmd.trim().length() > 0) {
                sendMessage(CHANNEL, cmd);
            }
        }
    }

    private String prepareToSend(String trim) {

        SubConfiguration conf = JDUtilities.getSubConfig("JDCHAT");
        String dest = conf.getStringProperty(PARAM_DESLANGUAGE, map.get(System.getProperty("user.country")));
        if (conf.getBooleanProperty(PARAM_DOAUTOTRANSLATSELF, false) && dest != null) {

            for (String next : map.keySet()) {
                if (map.get(next).equals(dest)) {
                    trim = JDLocale.translate(next, trim);

                    String tmp = JDLocale.translate(next, trim);
                    if (!tmp.equalsIgnoreCase(trim)) {
                        tmp += "(" + trim + ")";
                        trim = tmp;
                    }
                    break;
                }

            }

        }
        return trim;
    }

    public void reconnect() {
        initIRC();

    }

    public void removeUser(String name) {
        User user;
        if ((user = getUser(name)) != null) {
            NAMES.remove(user);

        }
        updateNamesPanel();
    }

    public void renameUser(String name, String name2) {

        User user;
        if ((user = getUser(name)) != null) {
            user.name = name2;

        } else {
            addUser(name2);
        }
        updateNamesPanel();
    }

    public void requestNameList() {
        resetNamesList();
        conn.doNames(CHANNEL);
    }

    public void resetNamesList() {

        NAMES = new ArrayList<User>();
        if (getUser(conn.getNick().trim()) == null) {
            NAMES.add(new User(conn.getNick().trim()));
        }
    }

    protected void sendMessage(String channel2, String text) {
        lastAction = System.currentTimeMillis();
        setNickAway(false);
        if (text.startsWith("/")) {
            int end = text.indexOf(" ");
            if (end < 0) {
                end = text.length();
            }
            String cmd = text.substring(1, end);
            String rest = text.substring(end).trim();
            if (Regex.matches(cmd, CMD_PM)) {
                textField.setText("");
                end = rest.indexOf(" ");
                if (end < 0) {
                    end = rest.length();
                }

                conn.doPrivmsg(rest.substring(0, end).trim(), prepareToSend(rest.substring(end).trim()));
                lastCommand = "/msg " + rest.substring(0, end).trim() + " ";
                addToText(null, STYLE_PM, "MSG>" + rest.substring(0, end).trim() + ":" + Utils.prepareMsg(rest.substring(end).trim()));
            } else if (Regex.matches(cmd, CMD_SLAP)) {
                conn.doPrivmsg(channel2, new String(new byte[] { 1 }) + "ACTION " + " slaps " + rest + " with the whole Javadocs" + new String(new byte[] { 1 }));
                addToText(null, STYLE_ACTION, conn.getNick() + " slaps " + rest + " with the whole Javadocs");

                lastCommand = "/slap ";
            } else if (Regex.matches(cmd, CMD_ACTION)) {
                lastCommand = "/me ";
                conn.doPrivmsg(channel2, new String(new byte[] { 1 }) + "ACTION " + prepareToSend(rest.trim()) + new String(new byte[] { 1 }));
                addToText(null, STYLE_ACTION, conn.getNick() + " " + Utils.prepareMsg(rest.trim()));

            } else if (Regex.matches(cmd, CMD_VERSION)) {

                String msg = " is using " + JDUtilities.getJDTitle() + " with Java " + JDUtilities.getJavaVersion() + " on a " + System.getProperty("os.name") + " system";
                conn.doPrivmsg(channel2, new String(new byte[] { 1 }) + "ACTION " + prepareToSend(msg) + new String(new byte[] { 1 }));
                addToText(null, STYLE_ACTION, conn.getNick() + " " + Utils.prepareMsg(msg));
            } else if (Regex.matches(cmd, CMD_MODE)) {
                end = rest.indexOf(" ");
                if (end < 0) {
                    end = rest.length();
                }
                lastCommand = "/mode ";
                conn.doMode(CHANNEL, rest.trim());
            } else if (Regex.matches(cmd, CMD_TRANSLATE)) {
                end = rest.indexOf(" ");
                if (end < 0) {
                    end = rest.length();
                }
                String[] tofrom = rest.substring(0, end).trim().split("to");
                if (tofrom == null || tofrom.length != 2) {
                    addToText(null, STYLE_ERROR, "Command /translate " + rest.substring(0, end).trim() + " is not available");
                    return;
                }
                String t;
                t = JDLocale.translate(tofrom[0], tofrom[1], Utils.prepareMsg(rest.substring(end).trim()));
                lastCommand = "/translate " + rest.substring(0, end).trim() + " ";
                textField.setText(t);
            } else if (Regex.matches(cmd, CMD_TOPIC)) {
                conn.doTopic(CHANNEL, prepareToSend(rest));
                lastCommand = "/topic ";
            } else if (Regex.matches(cmd, CMD_JOIN)) {
                conn.doJoin(CHANNEL, null);
                setLoggedIn(true);
                perform();
            } else if (Regex.matches(cmd, CMD_NICK)) {
                conn.doNick(rest.trim());
                lastCommand = "/nick ";
                JDUtilities.getSubConfig("JDCHAT").setProperty(NICK, rest.trim());
                JDUtilities.getSubConfig("JDCHAT").save();

            } else if (Regex.matches(cmd, CMD_CONNECT)) {
                if (conn == null || !conn.isConnected()) {
                    initIRC();
                }
            } else if (Regex.matches(cmd, CMD_DISCONNECT)) {
                if (conn != null && conn.isConnected()) {
                    conn.close();
                }
            } else {
                addToText(null, STYLE_ERROR, "Command /" + cmd + " is not available");
            }

            textField.requestFocus();
        } else {
            conn.doPrivmsg(channel2, prepareToSend(text));
            addToText(getUser(conn.getNick()), STYLE_SELF, Utils.prepareMsg(text));
            textField.setText("");
            textField.requestFocus();
        }
    }

    public void setEnabled(boolean b) {
        if (b) {

            initGUI();
            JDUtilities.getController().addControlListener(this);
            new Thread() {
                public void run() {

                    initIRC();
                }
            }.start();
        } else {
            if (frame != null) {
                frame.setVisible(false);
                frame.dispose();
            }

        }
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    public void setNick(String nickname) {
        if (nickname == null) { return; }
        addToText(null, JDChat.STYLE_SYSTEM_MESSAGE, "Rename to " + nickname);

        conn.doNick(nickname);

    }

    private void setNickAway(boolean b) {
        if (nickaway == b) { return; }
        nickaway = b;
        if (b) {
            orgNick = conn.getNick();
            setNick(conn.getNick().substring(0, Math.min(conn.getNick().length(), 11)) + "|away");
        } else {
            setNick(orgNick);

        }

    }

    public void setNickCount(int nickCount) {
        this.nickCount = nickCount;
    }

    public void setTopic(final String msg) {
        addToText(null, STYLE_SYSTEM_MESSAGE, "<b>Topic is: " + msg + "</b>");
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                top.setText(msg);
                frame.setTitle(getHost() + " : " + msg);
                frame.pack();
            }
        });

    }

    private void startAwayObserver() {
        Thread th = new Thread() {
            public void run() {
                while (true) {
                    if (System.currentTimeMillis() - lastAction > AWAY_TIMEOUT) {
                        setNickAway(true);
                    } else {
                        setNickAway(false);
                    }
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {

                        e.printStackTrace();
                    }
                }
            }

        };
        th.setDaemon(true);
        th.start();

    }

    public void updateNamesPanel() {
        final StringBuffer sb = new StringBuffer();
        Collections.sort(NAMES);

        // USERLIST_STYLE
        sb.append("<ul>");
        for (User name : NAMES) {
            sb.append("<li>");
            sb.append("<span style='color:#" + name.color + (name.name.equals(conn.getNick()) ? ";font-weight:bold;" : "") + "'>");
            sb.append(name.getRank() + name.getNickLink("query"));
            sb.append("</span></li>");
        }
        sb.append("</ul>");

        EventQueue.invokeLater(new Runnable() {
            public void run() {
                right.setText(USERLIST_STYLE + sb);
                frame.pack();
            }
        });

    }

}

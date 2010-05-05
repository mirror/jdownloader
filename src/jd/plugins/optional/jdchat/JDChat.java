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

package jd.plugins.optional.jdchat;

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
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.event.ControlEvent;
import jd.gui.UserIO;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.SwingGui;
import jd.gui.swing.components.Balloon;
import jd.gui.swing.components.linkbutton.JLink;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.jdgui.interfaces.SwitchPanelEvent;
import jd.gui.swing.jdgui.interfaces.SwitchPanelListener;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.nutils.OSDetector;
import jd.nutils.io.JDIO;
import jd.parser.Regex;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import jd.utils.locale.JDLocale;
import net.miginfocom.swing.MigLayout;

import org.schwering.irc.lib.IRCConnection;

@OptionalPlugin(rev = "$Revision$", id = "chat", hasGui = true, interfaceversion = 5)
public class JDChat extends PluginOptional {
    private static final long AWAY_TIMEOUT = 15 * 60 * 1000;
    private static String CHANNEL = "#jDownloader";
    private static final Pattern CMD_ACTION = Pattern.compile("(me)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CMD_CONNECT = Pattern.compile("(connect|verbinden)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CMD_DISCONNECT = Pattern.compile("(disconnect|trennen)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CMD_EXIT = Pattern.compile("(exit|quit)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CMD_MODE = Pattern.compile("(mode|modus)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CMD_JOIN = Pattern.compile("join", Pattern.CASE_INSENSITIVE);
    private static final Pattern CMD_NICK = Pattern.compile("(nick|name)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CMD_PM = Pattern.compile("(msg|query)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CMD_SLAP = Pattern.compile("(slap)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CMD_TOPIC = Pattern.compile("(topic|title)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CMD_TRANSLATE = Pattern.compile("(translate)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CMD_VERSION = Pattern.compile("(version|jdversion)", Pattern.CASE_INSENSITIVE);

    private static final ArrayList<String> COMMANDS = new ArrayList<String>();

    private static final String PARAM_HOST = "PARAM_HOST";
    private static final String PARAM_NICK = "PARAM_NICK";

    private static final String PARAM_PERFORM = "PARAM_PERFORM";
    private static final String PARAM_PORT = "PARAM_PORT";
    private static final String PARAM_USERCOLOR = "PARAM_USERCOLOR";
    private static final String PARAM_USERLISTPOSITION = "PARAM_USERLISTPOSITION";

    public static final String STYLE = JDIO.readFileToString(JDUtilities.getResourceFile("plugins/jdchat/styles.css"));
    public static final String STYLE_ACTION = "action";
    public static final String STYLE_ERROR = "error";
    public static final String STYLE_HIGHLIGHT = "highlight";
    public static final String STYLE_NOTICE = "notice";
    public static final String STYLE_PM = "pm";
    public static final String STYLE_SELF = "self";
    public static final String STYLE_SYSTEM_MESSAGE = "system";
    public static final String USERLIST_STYLE = JDIO.readFileToString(JDUtilities.getResourceFile("plugins/jdchat/userliststyles.css"));
    private static final String CHANNEL_LNG = "CHANNEL_LNG3";
    private JLabel top;

    private IRCConnection conn;
    private SwitchPanel frame;
    private long lastAction;
    private String lastCommand;
    private boolean loggedIn;
    private ArrayList<User> NAMES;
    private String nick;
    private boolean nickaway;
    private int nickCount = 0;
    private String orgNick;
    private JTextPane right;
    private TreeMap<String, JDChatPMS> pms = new TreeMap<String, JDChatPMS>();
    private StringBuilder sb;
    private JScrollPane scrollPane;
    private JTextPane textArea;
    private JTextField textField;

    public TreeMap<String, JDChatPMS> getPms() {
        return pms;
    }

    private JComboBox lang;

    private SubConfiguration subConfig;

    private JDChatView view;
    private MenuAction activateAction;
    private JTabbedPane tabbedPane;
    private JButton closeTab;

    public JDChat(PluginWrapper wrapper) {
        super(wrapper);
        subConfig = SubConfiguration.getConfig("JDCHAT");

        CHANNEL = this.getPluginConfig().getStringProperty("CHANNEL", CHANNEL);
        COMMANDS.add("/msg ");
        COMMANDS.add("/topic ");
        COMMANDS.add("/op ");
        COMMANDS.add("/deop ");
        COMMANDS.add("/query ");
        COMMANDS.add("/nick ");
        COMMANDS.add("/mode ");
        COMMANDS.add("/join ");

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

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == activateAction) {
            setGuiEnable(activateAction.isSelected());
        }
    }

    public void addToText(final User user, String style, String msg) {
        addToText(user, style, msg, textArea, sb);
    }

    public void addToText(final User user, String style, String msg, final JTextPane targetpane, final StringBuilder sb) {

        final String msg2 = msg;
        boolean color = subConfig.getBooleanProperty(PARAM_USERCOLOR, true);
        Date dt = new Date();

        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        sb.append("<!---->");
        sb.append("<li>");
        if (user != null) {
            if (!color) {
                sb.append("<span style='" + user.getStyle() + (getUser(conn.getNick()) == user ? ";font-weight:bold" : "") + "'>[" + df.format(dt) + "] " + user.getNickLink("pmnick") + (JDChat.STYLE_PM.equalsIgnoreCase(style) ? ">> " : ": ") + "</span>");
            } else {
                sb.append("<span style='color:#000000" + (getUser(conn.getNick()) == user ? ";font-weight:bold" : "") + "'>[" + df.format(dt) + "] " + user.getNickLink("pmnick") + (JDChat.STYLE_PM.equalsIgnoreCase(style) ? ">> " : ": ") + "</span>");
            }
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

        new GuiRunnable<Object>() {

            @Override
            public Object runSave() {

                if (!SwingGui.getInstance().getMainFrame().isActive() && conn != null && msg2.contains(conn.getNick())) {
                    // JDSounds.PT("sound.gui.selectPackage");
                    SwingGui.getInstance().getMainFrame().toFront();
                }

                targetpane.setText(STYLE + "<ul>" + sb.toString() + "</ul>");

                int max = scrollPane.getVerticalScrollBar().getMaximum();

                scrollPane.getVerticalScrollBar().setValue(max);

                return null;
            }

        }.start();

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

    @Override
    public void controlEvent(ControlEvent event) {
        if (event.getID() == ControlEvent.CONTROL_AFTER_RECONNECT) {
            if (SwingGui.getInstance().getMainFrame().isActive() && !nickaway) {
                initIRC();
            } else {
                addToText(null, STYLE_ERROR, "You got disconnected because of a reconnect. <a href='intern:reconnect|reconnect'><b>[RECONNECT NOW]</b></a>");
            }
        } else if (event.getID() == ControlEvent.CONTROL_BEFORE_RECONNECT) {
            // sendMessage(CHANNEL, "/me is reconnecting...");
            if (conn != null && conn.isConnected()) {
                addToText(null, STYLE_SYSTEM_MESSAGE, "closing connection due to requested reconnect.");
                conn.doPart(CHANNEL, "reconnecting...");
                conn.close();
                conn = null;
            }
        }
        super.controlEvent(event);
    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        ArrayList<MenuAction> menu = new ArrayList<MenuAction>();
        MenuAction m;

        menu.add(m = activateAction);
        if (view == null || !view.isVisible()) {
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
        final User usr = getUser(name);
        if (textField.getText().length() == 0) {
            if (!pms.containsKey(usr.name.toLowerCase())) addPMS(usr.name);
            for (int x = 0; x < tabbedPane.getTabCount(); x++) {
                if (tabbedPane.getTitleAt(x).equals(usr.name)) {
                    final int t = x;
                    new GuiRunnable<Object>() {
                        @Override
                        public Object runSave() {
                            tabbedPane.setSelectedIndex(t);
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
                    textField.setText(textField.getText().trim() + " " + usr.name + " ");
                    return null;
                }
            }.invokeLater();
        }
        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                textField.requestFocus();
                return null;
            }
        }.invokeLater();
    }

    public String getNick() {
        return conn.getNick();
    }

    public int getNickCount() {
        return nickCount;
    }

    public String getNickname() {

        String loc = JDL.getCountryCodeByIP();

        if (loc == null) {
            loc = System.getProperty("user.country");
        } else {
            loc = loc.toLowerCase();
        }
        String def = "JD-[" + loc + "]_" + ("" + System.currentTimeMillis()).substring(6);
        nick = subConfig.getStringProperty(PARAM_NICK);
        if (nick == null || nick.equalsIgnoreCase("")) {
            nick = UserIO.getInstance().requestInputDialog(JDL.L("plugins.optional.jdchat.enternick", "Your wished nickname?"));
            if ((nick != null) && (!nick.equalsIgnoreCase(""))) {
                nick += "[" + loc + "]";
            }
            if (nick != null) nick = nick.trim();
            subConfig.setProperty(PARAM_NICK, nick);
            subConfig.save();
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

    public User getUser(String name) {
        for (User next : NAMES) {
            if (next.isUser(name)) return next;

        }
        return null;
    }

    @Override
    public boolean initAddon() {
        NAMES = new ArrayList<User>();
        sb = new StringBuilder();
        if (activateAction == null) {
            activateAction = new MenuAction("chat", 0);
            activateAction.setActionListener(this);
            activateAction.setTitle(getHost());
            activateAction.setIcon(this.getIconKey());
            activateAction.setSelected(false);
        }
        return true;
    }

    private void initConfigEntries() {
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, subConfig, PARAM_NICK, JDL.L("plugins.optional.jdchat.user", "Nickname")));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, PARAM_USERCOLOR, JDL.L("plugins.optional.jdchat.usercolor", "Only black usernames?")));
        String[] positions = new String[] { JDL.L("plugins.jdchat.userlistposition_right", "Right"), JDL.L("plugins.jdchat.userlistposition_left", "Left") };
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, subConfig, PARAM_USERLISTPOSITION, positions, JDL.L("plugins.jdchat.userlistposition", "Userlist position:")).setDefaultValue(0));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, subConfig, PARAM_PERFORM, JDL.L("plugins.optional.jdchat.performonstart", "Perform commands after connection estabilished")));
    }

    @SuppressWarnings("unchecked")
    private void initGUI() {
        int userlistposition = subConfig.getIntegerProperty(PARAM_USERLISTPOSITION, 0);
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
                            JLink.openURL(e.getURL());
                        } catch (Exception e1) {
                            JDLogger.exception(e1);
                        }
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
        tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPane.add("JDChat", scrollPane);
        tabbedPane.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                tabbedPane.setForegroundAt(tabbedPane.getSelectedIndex(), Color.black);
            }

        });
        textField = new JTextField();
        textField.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, Collections.EMPTY_SET);
        textField.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, Collections.EMPTY_SET);
        textField.addFocusListener(new FocusListener() {

            public void focusGained(FocusEvent e) {
                tabbedPane.setForegroundAt(tabbedPane.getSelectedIndex(), Color.black);
            }

            public void focusLost(FocusEvent e) {
                tabbedPane.setForegroundAt(tabbedPane.getSelectedIndex(), Color.black);
            }

        });
        textField.addKeyListener(new KeyListener() {

            private int counter = 0;
            private String last = null;

            public void keyPressed(KeyEvent e) {
                int sel = tabbedPane.getSelectedIndex();
                tabbedPane.setForegroundAt(sel, Color.black);
            }

            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {

                    if (textField.getText().length() == 0) return;
                    if (tabbedPane.getSelectedIndex() == 0 || textField.getText().startsWith("/"))
                        sendMessage(CHANNEL, textField.getText());
                    else
                        sendMessage(CHANNEL, "/msg " + tabbedPane.getTitleAt(tabbedPane.getSelectedIndex()) + " " + textField.getText());

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
                        }
                    }
                    if (users.size() == 0) return;

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
        lang = new JComboBox(new JDLocale[] { JDL.getInstance("en"), JDL.getInstance("de"), JDL.getInstance("es"), JDL.getInstance("tr") });
        lang.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                getPluginConfig().setProperty(CHANNEL_LNG, lang.getSelectedItem());
                getPluginConfig().save();
                initChannel();
            }

        });
        lang.setSelectedItem(this.getPluginConfig().getProperty(CHANNEL_LNG, JDL.getInstance("en")));
        textArea.setContentType("text/html");
        textArea.setEditable(false);

        frame = new SwitchPanel() {
            private static final long serialVersionUID = 2138710083573682339L;

            @Override
            public void onShow() {
            }

            @Override
            public void onHide() {
            }
        };
        frame.setLayout(new MigLayout("ins 0, wrap 1", "[grow,fill]", "[grow,fill][]"));
        closeTab = new JButton(JDL.L("jd.plugins.optional.jdchat.closeTab", "Close Tab"));
        closeTab.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (tabbedPane.getSelectedIndex() > 0) {
                    delPMS(tabbedPane.getTitleAt(tabbedPane.getSelectedIndex()));
                } else if (tabbedPane.getSelectedIndex() == 0) {
                    addToText(null, STYLE_SYSTEM_MESSAGE, "You can't close the main Chat!");
                }
            }
        });
        JScrollPane scrollPane_userlist = new JScrollPane(right);
        switch (userlistposition) {
        case 0:
            frame.add(tabbedPane, "split 2");
            frame.add(scrollPane_userlist, "width 180:180:180");
            break;
        default:
        case 1:
            frame.add(scrollPane_userlist, "width 180:180:180 ,split 2");
            frame.add(tabbedPane);
            break;
        }

        frame.add(textField, "growx, split 3");
        frame.add(closeTab, "w pref!");
        frame.add(lang, "w pref!");
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
        frame.setSize(new Dimension(800, 600));
        frame.setVisible(true);
        startAwayObserver();
    }

    private void initIRC() {
        NAMES.clear();
        for (int i = 0; i < 20; i++) {
            String host = subConfig.getStringProperty(PARAM_HOST, "irc.freenode.net");
            int port = subConfig.getIntegerProperty(PARAM_PORT, 6667);
            String pass = null;
            String nick = getNickname();
            String user = "jdChatuser";
            String name = "jdChatuser";
            Balloon.show("JD Chat", null, "Connecting to JDChat...");
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
                break;
            } catch (IOException e) {
                addToText(null, STYLE_SYSTEM_MESSAGE, "Connect Timeout. Server not reachable...");
                JDLogger.exception(e);
                try {
                    Thread.sleep(15000);
                } catch (InterruptedException e1) {

                    JDLogger.exception(e1);
                }
                initIRC();
            }
        }

    }

    private void initChannel() {

        JDLocale id = JDL.getLocale();
        JDLocale lng = JDL.getInstance("en");
        if (id.getLanguageCode().equals("es")) {
            lng = JDL.getInstance("es");
        } else if (id.getLanguageCode().equals("tr")) {
            lng = JDL.getInstance("tr");
        }
        lng = this.getPluginConfig().getGenericProperty(CHANNEL_LNG, lng);
        String newChannel = null;
        if (lng.getLanguageCode().equals(JDL.getInstance("es").getLanguageCode())) {
            newChannel = "#jdownloader[es]";
        } else if (lng.getLanguageCode().equals(JDL.getInstance("tr").getLanguageCode())) {
            newChannel = "#jdownloader[tr]";
        } else {
            newChannel = "#jdownloader";
        }
        if (newChannel.equalsIgnoreCase(CHANNEL) && this.isLoggedIn()) {
            if (conn != null && conn.isConnected()) addToText(null, STYLE_NOTICE, "You are in channel: " + newChannel);
            return;
        }
        NAMES.clear();
        if (conn != null && conn.isConnected()) addToText(null, STYLE_NOTICE, "Change channel to: " + newChannel);
        if (conn != null && conn.isConnected()) conn.doPart(CHANNEL, " --> " + newChannel);
        CHANNEL = newChannel;
        if (conn != null && conn.isConnected()) conn.doJoin(CHANNEL, null);
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void onConnected() {
        initChannel();
        setLoggedIn(true);
        perform();

    }

    @Override
    public void onExit() {
        NAMES.clear();
        pms.clear();
        this.setLoggedIn(false);
        this.updateNamesPanel();
        if (view != null) SwingGui.getInstance().disposeView(view);
        view = null;
        if (conn != null) conn.close();
        conn = null;
    }

    public void onMode(char op, char mod, String arg) {
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
        String[] perform = Regex.getLines(subConfig.getStringProperty(PARAM_PERFORM));
        if (perform == null) { return; }
        for (String cmd : perform) {
            if (cmd.trim().length() > 0) {
                sendMessage(CHANNEL, cmd);
            }
        }
    }

    /**
     * Does modifications to the text before sending it
     */
    private String prepareToSend(String trim) {
        return trim;
    }

    public void reconnect() {
        initIRC();
    }

    public void removeUser(String name) {
        User user = getUser(name);
        if (user != null) {
            NAMES.remove(user);
        }
        updateNamesPanel();
    }

    public void renameUser(String name, String name2) {
        User user = getUser(name);
        if (user != null) {
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

    public void notifyPMS(final String user, final String text2) {
        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                for (int x = 0; x < tabbedPane.getTabCount(); x++) {
                    if (tabbedPane.getTitleAt(x).equals(user)) {
                        final int t = x;

                        String text = text2;
                        tabbedPane.setForegroundAt(t, Color.RED);
                        if (text.length() > 40) text = text.substring(0, 40).concat("...");
                        if (!tabbedPane.getTitleAt(tabbedPane.getSelectedIndex()).equals(user)) Balloon.show("JD Chat", null, JDL.LF("jd.plugins.optional.jdchat.newmessage", "New Message from %s:<hr> %s", user, text));
                        return null;
                    }
                }
                return null;
            }
        }.invokeLater();
    }

    public void addPMS(String user2) {
        final String user = user2.trim();
        if (user.equals(conn.getNick().trim())) return;
        pms.put(user.toLowerCase(), new JDChatPMS(user));
        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                tabbedPane.add(user, pms.get(user.toLowerCase()).getScrollPane());
                return null;
            }
        }.invokeLater();
    }

    public void renamePMS(final String userOld, final String userNew) {
        new GuiRunnable<Object>() {

            @Override
            public Object runSave() {
                pms.put(userNew.trim().toLowerCase(), pms.get(userOld.trim().toLowerCase()));
                for (int x = 0; x < tabbedPane.getComponentCount(); x++) {
                    if (tabbedPane.getTitleAt(x).equalsIgnoreCase(userOld)) {
                        tabbedPane.remove(x);
                        break;
                    }
                }
                pms.remove(userOld);
                tabbedPane.add(userNew.trim(), pms.get(userNew.trim().toLowerCase()).getScrollPane());
                return null;
            }
        }.invokeLater();
    }

    public void delPMS(final String user) {
        new GuiRunnable<Object>() {

            @Override
            public Object runSave() {
                pms.remove(user.toLowerCase());
                for (int x = 0; x < tabbedPane.getComponentCount(); x++) {
                    if (tabbedPane.getTitleAt(x).toLowerCase().equals(user.toLowerCase())) {
                        tabbedPane.remove(x);
                        return null;
                    }
                }
                return null;
            }
        }.invokeLater();

    }

    protected void sendMessage(String channel2, String text) {
        lastAction = System.currentTimeMillis();
        setNickAway(false);
        if (text.startsWith("/")) {
            int end = text.indexOf(" ");
            if (end < 0) {
                end = text.length();
            }
            String cmd = text.substring(1, end).trim();
            String rest = text.substring(end).trim();
            if (Regex.matches(cmd, CMD_PM)) {
                new GuiRunnable<Object>() {

                    @Override
                    public Object runSave() {
                        textField.setText("");
                        return null;
                    }
                }.invokeLater();
                end = rest.indexOf(" ");
                if (end < 0) {
                    end = rest.length();
                }
                if (!pms.containsKey(rest.substring(0, end).trim().toLowerCase())) {
                    addPMS(rest.substring(0, end).trim());
                }
                conn.doPrivmsg(rest.substring(0, end).trim(), prepareToSend(rest.substring(end).trim()));
                lastCommand = "/msg " + rest.substring(0, end).trim() + " ";
                addToText(getUser(conn.getNick()), STYLE_SELF, Utils.prepareMsg(rest.substring(end).trim()), pms.get(rest.substring(0, end).trim().toLowerCase()).getTextArea(), pms.get(rest.substring(0, end).trim().toLowerCase()).getSb());
            } else if (Regex.matches(cmd, CMD_SLAP)) {
                conn.doPrivmsg(channel2, new String(new byte[] { 1 }) + "ACTION " + " slaps " + rest + " with the whole Javadocs" + new String(new byte[] { 1 }));
                addToText(null, STYLE_ACTION, conn.getNick() + " slaps " + rest + " with the whole Javadocs");

                lastCommand = "/slap ";
            } else if (Regex.matches(cmd, CMD_ACTION)) {
                lastCommand = "/me ";
                conn.doPrivmsg(channel2, new String(new byte[] { 1 }) + "ACTION " + prepareToSend(rest.trim()) + new String(new byte[] { 1 }));
                addToText(null, STYLE_ACTION, conn.getNick() + " " + Utils.prepareMsg(rest.trim()));

            } else if (Regex.matches(cmd, CMD_VERSION)) {

                String msg = " is using " + JDUtilities.getJDTitle() + " with Java " + JDUtilities.getJavaVersion() + " on a " + OSDetector.getOSString() + " system";
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
                final String t;
                t = JDL.translate(tofrom[0], tofrom[1], Utils.prepareMsg(rest.substring(end).trim())).getTranslated();
                lastCommand = "/translate " + rest.substring(0, end).trim() + " ";
                new GuiRunnable<Object>() {

                    @Override
                    public Object runSave() {
                        textField.setText(t);
                        return null;
                    }
                }.invokeLater();
            } else if (Regex.matches(cmd, CMD_TOPIC)) {
                conn.doTopic(CHANNEL, prepareToSend(rest));
                lastCommand = "/topic ";
            } else if (Regex.matches(cmd, CMD_JOIN)) {
                NAMES.clear();
                if (conn != null) addToText(null, STYLE_NOTICE, "Change channel to: " + rest);
                if (conn != null) conn.doPart(CHANNEL, " --> " + rest);
                CHANNEL = rest;
                if (conn != null) conn.doJoin(CHANNEL, null);

                lastCommand = "/join " + rest;
                setLoggedIn(true);
                perform();
            } else if (Regex.matches(cmd, CMD_NICK)) {
                conn.doNick(rest.trim());
                lastCommand = "/nick ";
                subConfig.setProperty(PARAM_NICK, rest.trim());
                subConfig.save();

            } else if (Regex.matches(cmd, CMD_CONNECT)) {
                if (conn == null || !conn.isConnected()) {
                    initIRC();
                }
            } else if (Regex.matches(cmd, CMD_DISCONNECT)) {
                if (conn != null && conn.isConnected()) {
                    conn.close();
                }
            } else if (Regex.matches(cmd, CMD_EXIT)) {
                setGuiEnable(false);
            } else {
                addToText(null, STYLE_ERROR, "Command /" + cmd + " is not available");
            }

        } else {
            conn.doPrivmsg(channel2, prepareToSend(text));
            addToText(getUser(conn.getNick()), STYLE_SELF, Utils.prepareMsg(text));
        }
        new GuiRunnable<Object>() {

            @Override
            public Object runSave() {
                textField.setText("");
                textField.requestFocus();
                return null;
            }
        }.invokeLater();
    }

    @Override
    public void setGuiEnable(boolean b) {
        if (b) {

            if (view == null) {
                initGUI();
                view = new JDChatView() {

                    private static final long serialVersionUID = 3966113588850405974L;

                    @Override
                    protected void initMenu(JMenuBar menubar) {
                        menubar.add(top = new JLabel(JDL.L("jd.plugins.optional.jdchat.JDChat.topic.default", "Loading Message of the day")));
                        top.setToolTipText(JDL.L("jd.plugins.optional.jdchat.JDChat.topic.tooltip", "Message of the day"));
                    }

                };
                view.getBroadcaster().addListener(new SwitchPanelListener() {

                    @Override
                    public void onPanelEvent(SwitchPanelEvent event) {
                        if (event.getID() == SwitchPanelEvent.ON_REMOVE) {
                            setGuiEnable(false);
                        }
                    }

                });

                view.setContent(frame);
            }
            SwingGui.getInstance().setContent(view);

            new Thread() {
                @Override
                public void run() {
                    initIRC();
                }
            }.start();
        } else {

            if (frame != null) {
                SwingGui.getInstance().disposeView(view);
                this.stopAddon();
            }
        }
        if (activateAction != null && activateAction.isSelected() != b) activateAction.setSelected(b);
    }

    @Override
    public String getIconKey() {
        return "gui.images.chat";
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    public void setNick(String nickname) {
        if (nickname == null) return;
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
        new GuiRunnable<Object>() {

            @Override
            public Object runSave() {
                top.setText(msg);
                return null;
            }

        }.start();
    }

    private void startAwayObserver() {
        Thread th = new Thread() {
            @Override
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
                        JDLogger.exception(e);
                    }
                }
            }

        };
        th.setDaemon(true);
        th.start();

    }

    public void updateNamesPanel() {
        final StringBuilder sb = new StringBuilder();
        Collections.sort(NAMES);
        boolean color = subConfig.getBooleanProperty(PARAM_USERCOLOR, true);
        sb.append("<ul>");
        for (User name : NAMES) {
            sb.append("<li>");
            if (!color) {
                sb.append("<span style='color:#" + name.getColor() + (name.name.equals(conn.getNick()) ? ";font-weight:bold;" : "") + "'>");
            } else {
                sb.append("<span style='color:#000000" + (name.name.equals(conn.getNick()) ? ";font-weight:bold;" : "") + "'>");
            }
            sb.append(name.getRank() + name.getNickLink("query"));
            sb.append("</span></li>");
        }
        sb.append("</ul>");

        if (right != null) new GuiRunnable<Object>() {

            @Override
            public Object runSave() {
                right.setText(USERLIST_STYLE + sb);
                return null;
            }

        }.start();
    }

}

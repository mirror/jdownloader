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
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.controlling.reconnect.Reconnecter;
import jd.controlling.reconnect.ReconnecterEvent;
import jd.gui.UserIO;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.SwingGui;
import jd.gui.swing.components.Balloon;
import jd.gui.swing.components.linkbutton.JLink;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.jdgui.interfaces.SwitchPanelEvent;
import jd.gui.swing.jdgui.interfaces.SwitchPanelListener;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.nutils.io.JDIO;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import jd.utils.locale.JDLocale;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.AwReg;
import org.appwork.utils.event.DefaultEventListener;
import org.appwork.utils.os.CrossSystem;
import org.schwering.irc.lib.IRCConnection;

@OptionalPlugin(rev = "$Revision$", id = "chat", hasGui = true, interfaceversion = 7)
public class JDChat extends PluginOptional {
    private static final long                AWAY_TIMEOUT           = 15 * 60 * 1000;
    private static String                    CHANNEL                = "#jDownloader";
    private static final Pattern             CMD_ACTION             = Pattern.compile("(me)", Pattern.CASE_INSENSITIVE);
    private static final Pattern             CMD_CONNECT            = Pattern.compile("(connect|verbinden)", Pattern.CASE_INSENSITIVE);
    private static final Pattern             CMD_DISCONNECT         = Pattern.compile("(disconnect|trennen)", Pattern.CASE_INSENSITIVE);
    private static final Pattern             CMD_EXIT               = Pattern.compile("(exit|quit)", Pattern.CASE_INSENSITIVE);
    private static final Pattern             CMD_MODE               = Pattern.compile("(mode|modus)", Pattern.CASE_INSENSITIVE);
    private static final Pattern             CMD_JOIN               = Pattern.compile("join", Pattern.CASE_INSENSITIVE);
    private static final Pattern             CMD_NICK               = Pattern.compile("(nick|name)", Pattern.CASE_INSENSITIVE);
    private static final Pattern             CMD_PM                 = Pattern.compile("(msg|query)", Pattern.CASE_INSENSITIVE);
    private static final Pattern             CMD_SLAP               = Pattern.compile("(slap)", Pattern.CASE_INSENSITIVE);
    private static final Pattern             CMD_TOPIC              = Pattern.compile("(topic|title)", Pattern.CASE_INSENSITIVE);
    private static final Pattern             CMD_TRANSLATE          = Pattern.compile("(translate)", Pattern.CASE_INSENSITIVE);
    private static final Pattern             CMD_VERSION            = Pattern.compile("(version|jdversion)", Pattern.CASE_INSENSITIVE);

    private static final ArrayList<String>   COMMANDS               = new ArrayList<String>();

    private static final String              PARAM_HOST             = "PARAM_HOST";
    private static final String              PARAM_NICK             = "PARAM_NICK";

    private static final String              PARAM_PERFORM          = "PARAM_PERFORM";
    private static final String              PARAM_PORT             = "PARAM_PORT";
    private static final String              PARAM_USERCOLOR        = "PARAM_USERCOLOR";
    private static final String              PARAM_USERLISTPOSITION = "PARAM_USERLISTPOSITION";

    public static final String               STYLE                  = JDIO.readFileToString(JDUtilities.getResourceFile("plugins/jdchat/styles.css"));
    public static final String               STYLE_ACTION           = "action";
    public static final String               STYLE_ERROR            = "error";
    public static final String               STYLE_HIGHLIGHT        = "highlight";
    public static final String               STYLE_NOTICE           = "notice";
    public static final String               STYLE_PM               = "pm";
    public static final String               STYLE_SELF             = "self";
    public static final String               STYLE_SYSTEM_MESSAGE   = "system";
    public static final String               USERLIST_STYLE         = JDIO.readFileToString(JDUtilities.getResourceFile("plugins/jdchat/userliststyles.css"));
    private static final String              CHANNEL_LNG            = "CHANNEL_LNG3";
    private JLabel                           top;

    private IRCConnection                    conn;
    private SwitchPanel                      frame;
    private long                             lastAction;
    private String                           lastCommand;
    private boolean                          loggedIn;
    private ArrayList<User>                  NAMES;
    private String                           nick;
    private boolean                          nickaway;
    private int                              nickCount              = 0;
    private String                           orgNick;
    private JTextPane                        right;
    private final TreeMap<String, JDChatPMS> pms                    = new TreeMap<String, JDChatPMS>();
    private StringBuilder                    sb;
    private JScrollPane                      scrollPane;
    private JTextPane                        textArea;
    private JTextField                       textField;

    private JComboBox                        lang;

    private final SubConfiguration           subConfig;

    private JDChatView                       view;

    private MenuAction                       activateAction;
    private JTabbedPane                      tabbedPane;
    private JButton                          closeTab;

    public JDChat(final PluginWrapper wrapper) {
        super(wrapper);
        this.subConfig = SubConfiguration.getConfig("JDCHAT");

        JDChat.CHANNEL = this.getPluginConfig().getStringProperty("CHANNEL", JDChat.CHANNEL);
        JDChat.COMMANDS.add("/msg ");
        JDChat.COMMANDS.add("/topic ");
        JDChat.COMMANDS.add("/op ");
        JDChat.COMMANDS.add("/deop ");
        JDChat.COMMANDS.add("/query ");
        JDChat.COMMANDS.add("/nick ");
        JDChat.COMMANDS.add("/mode ");
        JDChat.COMMANDS.add("/join ");

        JDChat.COMMANDS.add("/translate ");
        this.initConfigEntries();
        JDChat.COMMANDS.add("/translate artoda ");
        JDChat.COMMANDS.add("/translate artode ");
        JDChat.COMMANDS.add("/translate artofi ");
        JDChat.COMMANDS.add("/translate artofr ");
        JDChat.COMMANDS.add("/translate artoel ");
        JDChat.COMMANDS.add("/translate artohi ");
        JDChat.COMMANDS.add("/translate artoit ");
        JDChat.COMMANDS.add("/translate artoja ");
        JDChat.COMMANDS.add("/translate artoko ");
        JDChat.COMMANDS.add("/translate artohr ");
        JDChat.COMMANDS.add("/translate artonl ");
        JDChat.COMMANDS.add("/translate artono ");
        JDChat.COMMANDS.add("/translate artopl ");
        JDChat.COMMANDS.add("/translate artopt ");
        JDChat.COMMANDS.add("/translate artoro ");
        JDChat.COMMANDS.add("/translate artoru ");
        JDChat.COMMANDS.add("/translate artosv ");
        JDChat.COMMANDS.add("/translate artoes ");
        JDChat.COMMANDS.add("/translate artocs ");
        JDChat.COMMANDS.add("/translate artoen ");
        JDChat.COMMANDS.add("/translate bgtoar ");
        JDChat.COMMANDS.add("/translate bgtoda ");
        JDChat.COMMANDS.add("/translate bgtode ");
        JDChat.COMMANDS.add("/translate bgtofi ");
        JDChat.COMMANDS.add("/translate bgtofr ");
        JDChat.COMMANDS.add("/translate bgtoel ");
        JDChat.COMMANDS.add("/translate bgtohi ");
        JDChat.COMMANDS.add("/translate bgtoit ");
        JDChat.COMMANDS.add("/translate bgtoja ");
        JDChat.COMMANDS.add("/translate bgtoko ");
        JDChat.COMMANDS.add("/translate bgtohr ");
        JDChat.COMMANDS.add("/translate bgtonl ");
        JDChat.COMMANDS.add("/translate bgtono ");
        JDChat.COMMANDS.add("/translate bgtopl ");
        JDChat.COMMANDS.add("/translate bgtopt ");
        JDChat.COMMANDS.add("/translate bgtoro ");
        JDChat.COMMANDS.add("/translate bgtoru ");
        JDChat.COMMANDS.add("/translate bgtosv ");
        JDChat.COMMANDS.add("/translate bgtoes ");
        JDChat.COMMANDS.add("/translate bgtocs ");
        JDChat.COMMANDS.add("/translate bgtoen ");
        JDChat.COMMANDS.add("/translate datoar ");
        JDChat.COMMANDS.add("/translate datobg ");
        JDChat.COMMANDS.add("/translate datode ");
        JDChat.COMMANDS.add("/translate datofi ");
        JDChat.COMMANDS.add("/translate datofr ");
        JDChat.COMMANDS.add("/translate datoel ");
        JDChat.COMMANDS.add("/translate datohi ");
        JDChat.COMMANDS.add("/translate datoit ");
        JDChat.COMMANDS.add("/translate datoja ");
        JDChat.COMMANDS.add("/translate datoko ");
        JDChat.COMMANDS.add("/translate datohr ");
        JDChat.COMMANDS.add("/translate datonl ");
        JDChat.COMMANDS.add("/translate datono ");
        JDChat.COMMANDS.add("/translate datopl ");
        JDChat.COMMANDS.add("/translate datopt ");
        JDChat.COMMANDS.add("/translate datoro ");
        JDChat.COMMANDS.add("/translate datoru ");
        JDChat.COMMANDS.add("/translate datosv ");
        JDChat.COMMANDS.add("/translate datoes ");
        JDChat.COMMANDS.add("/translate datocs ");
        JDChat.COMMANDS.add("/translate datoen ");
        JDChat.COMMANDS.add("/translate detoar ");
        JDChat.COMMANDS.add("/translate detobg ");
        JDChat.COMMANDS.add("/translate detoda ");
        JDChat.COMMANDS.add("/translate detofi ");
        JDChat.COMMANDS.add("/translate detofr ");
        JDChat.COMMANDS.add("/translate detoel ");
        JDChat.COMMANDS.add("/translate detohi ");
        JDChat.COMMANDS.add("/translate detoit ");
        JDChat.COMMANDS.add("/translate detoja ");
        JDChat.COMMANDS.add("/translate detoko ");
        JDChat.COMMANDS.add("/translate detohr ");
        JDChat.COMMANDS.add("/translate detonl ");
        JDChat.COMMANDS.add("/translate detono ");
        JDChat.COMMANDS.add("/translate detopl ");
        JDChat.COMMANDS.add("/translate detopt ");
        JDChat.COMMANDS.add("/translate detoro ");
        JDChat.COMMANDS.add("/translate detoru ");
        JDChat.COMMANDS.add("/translate detosv ");
        JDChat.COMMANDS.add("/translate detoes ");
        JDChat.COMMANDS.add("/translate detocs ");
        JDChat.COMMANDS.add("/translate detoen ");
        JDChat.COMMANDS.add("/translate fitoar ");
        JDChat.COMMANDS.add("/translate fitobg ");
        JDChat.COMMANDS.add("/translate fitoda ");
        JDChat.COMMANDS.add("/translate fitode ");
        JDChat.COMMANDS.add("/translate fitofr ");
        JDChat.COMMANDS.add("/translate fitoel ");
        JDChat.COMMANDS.add("/translate fitohi ");
        JDChat.COMMANDS.add("/translate fitoit ");
        JDChat.COMMANDS.add("/translate fitoja ");
        JDChat.COMMANDS.add("/translate fitoko ");
        JDChat.COMMANDS.add("/translate fitohr ");
        JDChat.COMMANDS.add("/translate fitonl ");
        JDChat.COMMANDS.add("/translate fitono ");
        JDChat.COMMANDS.add("/translate fitopl ");
        JDChat.COMMANDS.add("/translate fitopt ");
        JDChat.COMMANDS.add("/translate fitoro ");
        JDChat.COMMANDS.add("/translate fitoru ");
        JDChat.COMMANDS.add("/translate fitosv ");
        JDChat.COMMANDS.add("/translate fitoes ");
        JDChat.COMMANDS.add("/translate fitocs ");
        JDChat.COMMANDS.add("/translate fitoen ");
        JDChat.COMMANDS.add("/translate frtoar ");
        JDChat.COMMANDS.add("/translate frtobg ");
        JDChat.COMMANDS.add("/translate frtoda ");
        JDChat.COMMANDS.add("/translate frtode ");
        JDChat.COMMANDS.add("/translate frtofi ");
        JDChat.COMMANDS.add("/translate frtoel ");
        JDChat.COMMANDS.add("/translate frtohi ");
        JDChat.COMMANDS.add("/translate frtoit ");
        JDChat.COMMANDS.add("/translate frtoja ");
        JDChat.COMMANDS.add("/translate frtoko ");
        JDChat.COMMANDS.add("/translate frtohr ");
        JDChat.COMMANDS.add("/translate frtonl ");
        JDChat.COMMANDS.add("/translate frtono ");
        JDChat.COMMANDS.add("/translate frtopl ");
        JDChat.COMMANDS.add("/translate frtopt ");
        JDChat.COMMANDS.add("/translate frtoro ");
        JDChat.COMMANDS.add("/translate frtoru ");
        JDChat.COMMANDS.add("/translate frtosv ");
        JDChat.COMMANDS.add("/translate frtoes ");
        JDChat.COMMANDS.add("/translate frtocs ");
        JDChat.COMMANDS.add("/translate frtoen ");
        JDChat.COMMANDS.add("/translate eltoar ");
        JDChat.COMMANDS.add("/translate eltobg ");
        JDChat.COMMANDS.add("/translate eltoda ");
        JDChat.COMMANDS.add("/translate eltode ");
        JDChat.COMMANDS.add("/translate eltofi ");
        JDChat.COMMANDS.add("/translate eltofr ");
        JDChat.COMMANDS.add("/translate eltohi ");
        JDChat.COMMANDS.add("/translate eltoit ");
        JDChat.COMMANDS.add("/translate eltoja ");
        JDChat.COMMANDS.add("/translate eltoko ");
        JDChat.COMMANDS.add("/translate eltohr ");
        JDChat.COMMANDS.add("/translate eltonl ");
        JDChat.COMMANDS.add("/translate eltono ");
        JDChat.COMMANDS.add("/translate eltopl ");
        JDChat.COMMANDS.add("/translate eltopt ");
        JDChat.COMMANDS.add("/translate eltoro ");
        JDChat.COMMANDS.add("/translate eltoru ");
        JDChat.COMMANDS.add("/translate eltosv ");
        JDChat.COMMANDS.add("/translate eltoes ");
        JDChat.COMMANDS.add("/translate eltocs ");
        JDChat.COMMANDS.add("/translate eltoen ");
        JDChat.COMMANDS.add("/translate hitoar ");
        JDChat.COMMANDS.add("/translate hitobg ");
        JDChat.COMMANDS.add("/translate hitoda ");
        JDChat.COMMANDS.add("/translate hitode ");
        JDChat.COMMANDS.add("/translate hitofi ");
        JDChat.COMMANDS.add("/translate hitofr ");
        JDChat.COMMANDS.add("/translate hitoel ");
        JDChat.COMMANDS.add("/translate hitoit ");
        JDChat.COMMANDS.add("/translate hitoja ");
        JDChat.COMMANDS.add("/translate hitoko ");
        JDChat.COMMANDS.add("/translate hitohr ");
        JDChat.COMMANDS.add("/translate hitonl ");
        JDChat.COMMANDS.add("/translate hitono ");
        JDChat.COMMANDS.add("/translate hitopl ");
        JDChat.COMMANDS.add("/translate hitopt ");
        JDChat.COMMANDS.add("/translate hitoro ");
        JDChat.COMMANDS.add("/translate hitoru ");
        JDChat.COMMANDS.add("/translate hitosv ");
        JDChat.COMMANDS.add("/translate hitoes ");
        JDChat.COMMANDS.add("/translate hitocs ");
        JDChat.COMMANDS.add("/translate hitoen ");
        JDChat.COMMANDS.add("/translate ittoar ");
        JDChat.COMMANDS.add("/translate ittobg ");
        JDChat.COMMANDS.add("/translate ittoda ");
        JDChat.COMMANDS.add("/translate ittode ");
        JDChat.COMMANDS.add("/translate ittofi ");
        JDChat.COMMANDS.add("/translate ittofr ");
        JDChat.COMMANDS.add("/translate ittoel ");
        JDChat.COMMANDS.add("/translate ittohi ");
        JDChat.COMMANDS.add("/translate ittoja ");
        JDChat.COMMANDS.add("/translate ittoko ");
        JDChat.COMMANDS.add("/translate ittohr ");
        JDChat.COMMANDS.add("/translate ittonl ");
        JDChat.COMMANDS.add("/translate ittono ");
        JDChat.COMMANDS.add("/translate ittopl ");
        JDChat.COMMANDS.add("/translate ittopt ");
        JDChat.COMMANDS.add("/translate ittoro ");
        JDChat.COMMANDS.add("/translate ittoru ");
        JDChat.COMMANDS.add("/translate ittosv ");
        JDChat.COMMANDS.add("/translate ittoes ");
        JDChat.COMMANDS.add("/translate ittocs ");
        JDChat.COMMANDS.add("/translate ittoen ");
        JDChat.COMMANDS.add("/translate jatoar ");
        JDChat.COMMANDS.add("/translate jatobg ");
        JDChat.COMMANDS.add("/translate jatoda ");
        JDChat.COMMANDS.add("/translate jatode ");
        JDChat.COMMANDS.add("/translate jatofi ");
        JDChat.COMMANDS.add("/translate jatofr ");
        JDChat.COMMANDS.add("/translate jatoel ");
        JDChat.COMMANDS.add("/translate jatohi ");
        JDChat.COMMANDS.add("/translate jatoit ");
        JDChat.COMMANDS.add("/translate jatoko ");
        JDChat.COMMANDS.add("/translate jatohr ");
        JDChat.COMMANDS.add("/translate jatonl ");
        JDChat.COMMANDS.add("/translate jatono ");
        JDChat.COMMANDS.add("/translate jatopl ");
        JDChat.COMMANDS.add("/translate jatopt ");
        JDChat.COMMANDS.add("/translate jatoro ");
        JDChat.COMMANDS.add("/translate jatoru ");
        JDChat.COMMANDS.add("/translate jatosv ");
        JDChat.COMMANDS.add("/translate jatoes ");
        JDChat.COMMANDS.add("/translate jatocs ");
        JDChat.COMMANDS.add("/translate jatoen ");
        JDChat.COMMANDS.add("/translate kotoar ");
        JDChat.COMMANDS.add("/translate kotobg ");
        JDChat.COMMANDS.add("/translate kotoda ");
        JDChat.COMMANDS.add("/translate kotode ");
        JDChat.COMMANDS.add("/translate kotofi ");
        JDChat.COMMANDS.add("/translate kotofr ");
        JDChat.COMMANDS.add("/translate kotoel ");
        JDChat.COMMANDS.add("/translate kotohi ");
        JDChat.COMMANDS.add("/translate kotoit ");
        JDChat.COMMANDS.add("/translate kotoja ");
        JDChat.COMMANDS.add("/translate kotohr ");
        JDChat.COMMANDS.add("/translate kotonl ");
        JDChat.COMMANDS.add("/translate kotono ");
        JDChat.COMMANDS.add("/translate kotopl ");
        JDChat.COMMANDS.add("/translate kotopt ");
        JDChat.COMMANDS.add("/translate kotoro ");
        JDChat.COMMANDS.add("/translate kotoru ");
        JDChat.COMMANDS.add("/translate kotosv ");
        JDChat.COMMANDS.add("/translate kotoes ");
        JDChat.COMMANDS.add("/translate kotocs ");
        JDChat.COMMANDS.add("/translate kotoen ");
        JDChat.COMMANDS.add("/translate hrtoar ");
        JDChat.COMMANDS.add("/translate hrtobg ");
        JDChat.COMMANDS.add("/translate hrtoda ");
        JDChat.COMMANDS.add("/translate hrtode ");
        JDChat.COMMANDS.add("/translate hrtofi ");
        JDChat.COMMANDS.add("/translate hrtofr ");
        JDChat.COMMANDS.add("/translate hrtoel ");
        JDChat.COMMANDS.add("/translate hrtohi ");
        JDChat.COMMANDS.add("/translate hrtoit ");
        JDChat.COMMANDS.add("/translate hrtoja ");
        JDChat.COMMANDS.add("/translate hrtoko ");
        JDChat.COMMANDS.add("/translate hrtonl ");
        JDChat.COMMANDS.add("/translate hrtono ");
        JDChat.COMMANDS.add("/translate hrtopl ");
        JDChat.COMMANDS.add("/translate hrtopt ");
        JDChat.COMMANDS.add("/translate hrtoro ");
        JDChat.COMMANDS.add("/translate hrtoru ");
        JDChat.COMMANDS.add("/translate hrtosv ");
        JDChat.COMMANDS.add("/translate hrtoes ");
        JDChat.COMMANDS.add("/translate hrtocs ");
        JDChat.COMMANDS.add("/translate hrtoen ");
        JDChat.COMMANDS.add("/translate nltoar ");
        JDChat.COMMANDS.add("/translate nltobg ");
        JDChat.COMMANDS.add("/translate nltoda ");
        JDChat.COMMANDS.add("/translate nltode ");
        JDChat.COMMANDS.add("/translate nltofi ");
        JDChat.COMMANDS.add("/translate nltofr ");
        JDChat.COMMANDS.add("/translate nltoel ");
        JDChat.COMMANDS.add("/translate nltohi ");
        JDChat.COMMANDS.add("/translate nltoit ");
        JDChat.COMMANDS.add("/translate nltoja ");
        JDChat.COMMANDS.add("/translate nltoko ");
        JDChat.COMMANDS.add("/translate nltohr ");
        JDChat.COMMANDS.add("/translate nltono ");
        JDChat.COMMANDS.add("/translate nltopl ");
        JDChat.COMMANDS.add("/translate nltopt ");
        JDChat.COMMANDS.add("/translate nltoro ");
        JDChat.COMMANDS.add("/translate nltoru ");
        JDChat.COMMANDS.add("/translate nltosv ");
        JDChat.COMMANDS.add("/translate nltoes ");
        JDChat.COMMANDS.add("/translate nltocs ");
        JDChat.COMMANDS.add("/translate nltoen ");
        JDChat.COMMANDS.add("/translate notoar ");
        JDChat.COMMANDS.add("/translate notobg ");
        JDChat.COMMANDS.add("/translate notoda ");
        JDChat.COMMANDS.add("/translate notode ");
        JDChat.COMMANDS.add("/translate notofi ");
        JDChat.COMMANDS.add("/translate notofr ");
        JDChat.COMMANDS.add("/translate notoel ");
        JDChat.COMMANDS.add("/translate notohi ");
        JDChat.COMMANDS.add("/translate notoit ");
        JDChat.COMMANDS.add("/translate notoja ");
        JDChat.COMMANDS.add("/translate notoko ");
        JDChat.COMMANDS.add("/translate notohr ");
        JDChat.COMMANDS.add("/translate notonl ");
        JDChat.COMMANDS.add("/translate notopl ");
        JDChat.COMMANDS.add("/translate notopt ");
        JDChat.COMMANDS.add("/translate notoro ");
        JDChat.COMMANDS.add("/translate notoru ");
        JDChat.COMMANDS.add("/translate notosv ");
        JDChat.COMMANDS.add("/translate notoes ");
        JDChat.COMMANDS.add("/translate notocs ");
        JDChat.COMMANDS.add("/translate notoen ");
        JDChat.COMMANDS.add("/translate pltoar ");
        JDChat.COMMANDS.add("/translate pltobg ");
        JDChat.COMMANDS.add("/translate pltoda ");
        JDChat.COMMANDS.add("/translate pltode ");
        JDChat.COMMANDS.add("/translate pltofi ");
        JDChat.COMMANDS.add("/translate pltofr ");
        JDChat.COMMANDS.add("/translate pltoel ");
        JDChat.COMMANDS.add("/translate pltohi ");
        JDChat.COMMANDS.add("/translate pltoit ");
        JDChat.COMMANDS.add("/translate pltoja ");
        JDChat.COMMANDS.add("/translate pltoko ");
        JDChat.COMMANDS.add("/translate pltohr ");
        JDChat.COMMANDS.add("/translate pltonl ");
        JDChat.COMMANDS.add("/translate pltono ");
        JDChat.COMMANDS.add("/translate pltopt ");
        JDChat.COMMANDS.add("/translate pltoro ");
        JDChat.COMMANDS.add("/translate pltoru ");
        JDChat.COMMANDS.add("/translate pltosv ");
        JDChat.COMMANDS.add("/translate pltoes ");
        JDChat.COMMANDS.add("/translate pltocs ");
        JDChat.COMMANDS.add("/translate pltoen ");
        JDChat.COMMANDS.add("/translate pttoar ");
        JDChat.COMMANDS.add("/translate pttobg ");
        JDChat.COMMANDS.add("/translate pttoda ");
        JDChat.COMMANDS.add("/translate pttode ");
        JDChat.COMMANDS.add("/translate pttofi ");
        JDChat.COMMANDS.add("/translate pttofr ");
        JDChat.COMMANDS.add("/translate pttoel ");
        JDChat.COMMANDS.add("/translate pttohi ");
        JDChat.COMMANDS.add("/translate pttoit ");
        JDChat.COMMANDS.add("/translate pttoja ");
        JDChat.COMMANDS.add("/translate pttoko ");
        JDChat.COMMANDS.add("/translate pttohr ");
        JDChat.COMMANDS.add("/translate pttonl ");
        JDChat.COMMANDS.add("/translate pttono ");
        JDChat.COMMANDS.add("/translate pttopl ");
        JDChat.COMMANDS.add("/translate pttoro ");
        JDChat.COMMANDS.add("/translate pttoru ");
        JDChat.COMMANDS.add("/translate pttosv ");
        JDChat.COMMANDS.add("/translate pttoes ");
        JDChat.COMMANDS.add("/translate pttocs ");
        JDChat.COMMANDS.add("/translate pttoen ");
        JDChat.COMMANDS.add("/translate rotoar ");
        JDChat.COMMANDS.add("/translate rotobg ");
        JDChat.COMMANDS.add("/translate rotoda ");
        JDChat.COMMANDS.add("/translate rotode ");
        JDChat.COMMANDS.add("/translate rotofi ");
        JDChat.COMMANDS.add("/translate rotofr ");
        JDChat.COMMANDS.add("/translate rotoel ");
        JDChat.COMMANDS.add("/translate rotohi ");
        JDChat.COMMANDS.add("/translate rotoit ");
        JDChat.COMMANDS.add("/translate rotoja ");
        JDChat.COMMANDS.add("/translate rotoko ");
        JDChat.COMMANDS.add("/translate rotohr ");
        JDChat.COMMANDS.add("/translate rotonl ");
        JDChat.COMMANDS.add("/translate rotono ");
        JDChat.COMMANDS.add("/translate rotopl ");
        JDChat.COMMANDS.add("/translate rotopt ");
        JDChat.COMMANDS.add("/translate rotoru ");
        JDChat.COMMANDS.add("/translate rotosv ");
        JDChat.COMMANDS.add("/translate rotoes ");
        JDChat.COMMANDS.add("/translate rotocs ");
        JDChat.COMMANDS.add("/translate rotoen ");
        JDChat.COMMANDS.add("/translate rutoar ");
        JDChat.COMMANDS.add("/translate rutobg ");
        JDChat.COMMANDS.add("/translate rutoda ");
        JDChat.COMMANDS.add("/translate rutode ");
        JDChat.COMMANDS.add("/translate rutofi ");
        JDChat.COMMANDS.add("/translate rutofr ");
        JDChat.COMMANDS.add("/translate rutoel ");
        JDChat.COMMANDS.add("/translate rutohi ");
        JDChat.COMMANDS.add("/translate rutoit ");
        JDChat.COMMANDS.add("/translate rutoja ");
        JDChat.COMMANDS.add("/translate rutoko ");
        JDChat.COMMANDS.add("/translate rutohr ");
        JDChat.COMMANDS.add("/translate rutonl ");
        JDChat.COMMANDS.add("/translate rutono ");
        JDChat.COMMANDS.add("/translate rutopl ");
        JDChat.COMMANDS.add("/translate rutopt ");
        JDChat.COMMANDS.add("/translate rutoro ");
        JDChat.COMMANDS.add("/translate rutosv ");
        JDChat.COMMANDS.add("/translate rutoes ");
        JDChat.COMMANDS.add("/translate rutocs ");
        JDChat.COMMANDS.add("/translate rutoen ");
        JDChat.COMMANDS.add("/translate svtoar ");
        JDChat.COMMANDS.add("/translate svtobg ");
        JDChat.COMMANDS.add("/translate svtoda ");
        JDChat.COMMANDS.add("/translate svtode ");
        JDChat.COMMANDS.add("/translate svtofi ");
        JDChat.COMMANDS.add("/translate svtofr ");
        JDChat.COMMANDS.add("/translate svtoel ");
        JDChat.COMMANDS.add("/translate svtohi ");
        JDChat.COMMANDS.add("/translate svtoit ");
        JDChat.COMMANDS.add("/translate svtoja ");
        JDChat.COMMANDS.add("/translate svtoko ");
        JDChat.COMMANDS.add("/translate svtohr ");
        JDChat.COMMANDS.add("/translate svtonl ");
        JDChat.COMMANDS.add("/translate svtono ");
        JDChat.COMMANDS.add("/translate svtopl ");
        JDChat.COMMANDS.add("/translate svtopt ");
        JDChat.COMMANDS.add("/translate svtoro ");
        JDChat.COMMANDS.add("/translate svtoru ");
        JDChat.COMMANDS.add("/translate svtoes ");
        JDChat.COMMANDS.add("/translate svtocs ");
        JDChat.COMMANDS.add("/translate svtoen ");
        JDChat.COMMANDS.add("/translate estoar ");
        JDChat.COMMANDS.add("/translate estobg ");
        JDChat.COMMANDS.add("/translate estoda ");
        JDChat.COMMANDS.add("/translate estode ");
        JDChat.COMMANDS.add("/translate estofi ");
        JDChat.COMMANDS.add("/translate estofr ");
        JDChat.COMMANDS.add("/translate estoel ");
        JDChat.COMMANDS.add("/translate estohi ");
        JDChat.COMMANDS.add("/translate estoit ");
        JDChat.COMMANDS.add("/translate estoja ");
        JDChat.COMMANDS.add("/translate estoko ");
        JDChat.COMMANDS.add("/translate estohr ");
        JDChat.COMMANDS.add("/translate estonl ");
        JDChat.COMMANDS.add("/translate estono ");
        JDChat.COMMANDS.add("/translate estopl ");
        JDChat.COMMANDS.add("/translate estopt ");
        JDChat.COMMANDS.add("/translate estoro ");
        JDChat.COMMANDS.add("/translate estoru ");
        JDChat.COMMANDS.add("/translate estosv ");
        JDChat.COMMANDS.add("/translate estocs ");
        JDChat.COMMANDS.add("/translate estoen ");
        JDChat.COMMANDS.add("/translate cstoar ");
        JDChat.COMMANDS.add("/translate cstobg ");
        JDChat.COMMANDS.add("/translate cstoda ");
        JDChat.COMMANDS.add("/translate cstode ");
        JDChat.COMMANDS.add("/translate cstofi ");
        JDChat.COMMANDS.add("/translate cstofr ");
        JDChat.COMMANDS.add("/translate cstoel ");
        JDChat.COMMANDS.add("/translate cstohi ");
        JDChat.COMMANDS.add("/translate cstoit ");
        JDChat.COMMANDS.add("/translate cstoja ");
        JDChat.COMMANDS.add("/translate cstoko ");
        JDChat.COMMANDS.add("/translate cstohr ");
        JDChat.COMMANDS.add("/translate cstonl ");
        JDChat.COMMANDS.add("/translate cstono ");
        JDChat.COMMANDS.add("/translate cstopl ");
        JDChat.COMMANDS.add("/translate cstopt ");
        JDChat.COMMANDS.add("/translate cstoro ");
        JDChat.COMMANDS.add("/translate cstoru ");
        JDChat.COMMANDS.add("/translate cstosv ");
        JDChat.COMMANDS.add("/translate cstoes ");
        JDChat.COMMANDS.add("/translate cstoen ");
        JDChat.COMMANDS.add("/translate entoar ");
        JDChat.COMMANDS.add("/translate entobg ");
        JDChat.COMMANDS.add("/translate entoda ");
        JDChat.COMMANDS.add("/translate entode ");
        JDChat.COMMANDS.add("/translate entofi ");
        JDChat.COMMANDS.add("/translate entofr ");
        JDChat.COMMANDS.add("/translate entoel ");
        JDChat.COMMANDS.add("/translate entohi ");
        JDChat.COMMANDS.add("/translate entoit ");
        JDChat.COMMANDS.add("/translate entoja ");
        JDChat.COMMANDS.add("/translate entoko ");
        JDChat.COMMANDS.add("/translate entohr ");
        JDChat.COMMANDS.add("/translate entonl ");
        JDChat.COMMANDS.add("/translate entono ");
        JDChat.COMMANDS.add("/translate entopl ");
        JDChat.COMMANDS.add("/translate entopt ");
        JDChat.COMMANDS.add("/translate entoro ");
        JDChat.COMMANDS.add("/translate entoru ");
        JDChat.COMMANDS.add("/translate entosv ");
        JDChat.COMMANDS.add("/translate entoes ");
        JDChat.COMMANDS.add("/translate entocs ");

        Reconnecter.getInstance().getEventSender().addListener(new DefaultEventListener<ReconnecterEvent>() {

            public void onEvent(final ReconnecterEvent event) {
                // ignore events if gui is not active
                if (JDChat.this.textArea == null) { return; }
                if (event.getEventID() == ReconnecterEvent.AFTER) {
                    if (SwingGui.getInstance().getMainFrame().isActive() && !JDChat.this.nickaway) {
                        JDChat.this.initIRC();
                    } else {
                        JDChat.this.addToText(null, JDChat.STYLE_ERROR, "You got disconnected because of a reconnect. <a href='intern:reconnect|reconnect'><b>[RECONNECT NOW]</b></a>");
                    }
                } else if (event.getEventID() == ReconnecterEvent.BEFORE) {
                    // sendMessage(CHANNEL, "/me is reconnecting...");
                    if (JDChat.this.conn != null && JDChat.this.conn.isConnected()) {
                        JDChat.this.addToText(null, JDChat.STYLE_SYSTEM_MESSAGE, "closing connection due to requested reconnect.");
                        JDChat.this.conn.doPart(JDChat.CHANNEL, "reconnecting...");
                        JDChat.this.conn.close();
                        JDChat.this.conn = null;
                    }
                }

            }

        });

    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == this.activateAction) {
            this.setGuiEnable(this.activateAction.isSelected());
        }
    }

    public void addPMS(final String user2) {
        final String user = user2.trim();
        if (user.equals(this.conn.getNick().trim())) { return; }
        this.pms.put(user.toLowerCase(), new JDChatPMS(user));
        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                JDChat.this.tabbedPane.add(user, JDChat.this.pms.get(user.toLowerCase()).getScrollPane());
                return null;
            }
        }.invokeLater();
    }

    public void addToText(final User user, final String style, final String msg) {
        this.addToText(user, style, msg, this.textArea, this.sb);
    }

    public void addToText(final User user, String style, final String msg, final JTextPane targetpane, final StringBuilder sb) {

        final String msg2 = msg;
        final boolean color = this.subConfig.getBooleanProperty(JDChat.PARAM_USERCOLOR, true);
        final Date dt = new Date();

        final SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        sb.append("<!---->");
        sb.append("<li>");
        if (user != null) {
            if (!color) {
                sb.append("<span style='" + user.getStyle() + (this.getUser(this.conn.getNick()) == user ? ";font-weight:bold" : "") + "'>[" + df.format(dt) + "] " + user.getNickLink("pmnick") + (JDChat.STYLE_PM.equalsIgnoreCase(style) ? ">> " : ": ") + "</span>");
            } else {
                sb.append("<span style='color:#000000" + (this.getUser(this.conn.getNick()) == user ? ";font-weight:bold" : "") + "'>[" + df.format(dt) + "] " + user.getNickLink("pmnick") + (JDChat.STYLE_PM.equalsIgnoreCase(style) ? ">> " : ": ") + "</span>");
            }
        } else {
            sb.append("<span class='time'>[" + df.format(dt) + "] </span>");

        }
        if (this.conn != null && msg.contains(this.conn.getNick())) {
            style = JDChat.STYLE_HIGHLIGHT;
        }
        if (style != null) {
            sb.append("<span class='" + style + "'>" + msg + "</span>");
        } else {
            sb.append("<span>" + msg + "</span>");
        }

        new GuiRunnable<Object>() {

            @Override
            public Object runSave() {

                if (!SwingGui.getInstance().getMainFrame().isActive() && JDChat.this.conn != null && msg2.contains(JDChat.this.conn.getNick())) {
                    // JDSounds.PT("sound.gui.selectPackage");
                    SwingGui.getInstance().getMainFrame().toFront();
                }

                targetpane.setText(JDChat.STYLE + "<ul>" + sb.toString() + "</ul>");

                final int max = JDChat.this.scrollPane.getVerticalScrollBar().getMaximum();

                JDChat.this.scrollPane.getVerticalScrollBar().setValue(max);

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

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        final ArrayList<MenuAction> menu = new ArrayList<MenuAction>();

        menu.add(this.activateAction);

        return menu;
    }

    public void delPMS(final String user) {
        new GuiRunnable<Object>() {

            @Override
            public Object runSave() {
                JDChat.this.pms.remove(user.toLowerCase());
                for (int x = 0; x < JDChat.this.tabbedPane.getComponentCount(); x++) {
                    if (JDChat.this.tabbedPane.getTitleAt(x).toLowerCase().equals(user.toLowerCase())) {
                        JDChat.this.tabbedPane.remove(x);
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
                            JDChat.this.tabbedPane.setSelectedIndex(t);
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
                    JDChat.this.textField.setText(JDChat.this.textField.getText().trim() + " " + usr.name + " ");
                    return null;
                }
            }.invokeLater();
        }
        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                JDChat.this.textField.requestFocus();
                return null;
            }
        }.invokeLater();
    }

    @Override
    public String getIconKey() {
        return "gui.images.chat";
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
        this.nick = this.subConfig.getStringProperty(JDChat.PARAM_NICK);
        if (this.nick == null || this.nick.equalsIgnoreCase("")) {
            this.nick = UserIO.getInstance().requestInputDialog(JDL.L("plugins.optional.jdchat.enternick", "Your wished nickname?"));
            if (this.nick != null && !this.nick.equalsIgnoreCase("")) {
                this.nick += "[" + loc + "]";
            }
            if (this.nick != null) {
                this.nick = this.nick.trim();
            }
            this.subConfig.setProperty(JDChat.PARAM_NICK, this.nick);
            this.subConfig.save();
        }
        if (this.nick == null) {
            this.nick = def;
        }
        this.nick = this.nick.trim();
        if (this.getNickCount() > 0) {
            this.nick += "[" + this.getNickCount() + "]";
        }
        return this.nick;
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

    @Override
    public boolean initAddon() {
        this.NAMES = new ArrayList<User>();
        this.sb = new StringBuilder();

        this.activateAction = new MenuAction("chat", this.getIconKey());
        this.activateAction.setActionListener(this);
        this.activateAction.setSelected(false);

        return true;
    }

    private void initChannel() {

        final JDLocale id = JDL.getLocale();
        JDLocale lng = JDL.getInstance("en");
        if (id.getLanguageCode().equals("es")) {
            lng = JDL.getInstance("es");
        } else if (id.getLanguageCode().equals("tr")) {
            lng = JDL.getInstance("tr");
        }
        lng = this.getPluginConfig().getGenericProperty(JDChat.CHANNEL_LNG, lng);
        String newChannel = null;
        if (lng.getLanguageCode().equals(JDL.getInstance("es").getLanguageCode())) {
            newChannel = "#jdownloader[es]";
        } else if (lng.getLanguageCode().equals(JDL.getInstance("tr").getLanguageCode())) {
            newChannel = "#jdownloader[tr]";
        } else {
            newChannel = "#jdownloader";
        }
        if (newChannel.equalsIgnoreCase(JDChat.CHANNEL) && this.isLoggedIn()) {
            if (this.conn != null && this.conn.isConnected()) {
                this.addToText(null, JDChat.STYLE_NOTICE, "You are in channel: " + newChannel);
            }
            return;
        }
        this.NAMES.clear();
        if (this.conn != null && this.conn.isConnected()) {
            this.addToText(null, JDChat.STYLE_NOTICE, "Change channel to: " + newChannel);
        }
        if (this.conn != null && this.conn.isConnected()) {
            this.conn.doPart(JDChat.CHANNEL, " --> " + newChannel);
        }
        JDChat.CHANNEL = newChannel;
        if (this.conn != null && this.conn.isConnected()) {
            this.conn.doJoin(JDChat.CHANNEL, null);
        }
    }

    private void initConfigEntries() {
        this.config.setGroup(new ConfigGroup(this.getHost(), this.getIconKey()));
        this.config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, this.subConfig, JDChat.PARAM_NICK, JDL.L("plugins.optional.jdchat.user", "Nickname")));
        this.config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.subConfig, JDChat.PARAM_USERCOLOR, JDL.L("plugins.optional.jdchat.usercolor", "Only black usernames?")));
        final String[] positions = new String[] { JDL.L("plugins.jdchat.userlistposition_right", "Right"), JDL.L("plugins.jdchat.userlistposition_left", "Left") };
        this.config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, this.subConfig, JDChat.PARAM_USERLISTPOSITION, positions, JDL.L("plugins.jdchat.userlistposition", "Userlist position:")).setDefaultValue(0));
        this.config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, this.subConfig, JDChat.PARAM_PERFORM, JDL.L("plugins.optional.jdchat.performonstart", "Perform commands after connection estabilished")));
    }

    @SuppressWarnings("unchecked")
    private void initGUI() {
        final int userlistposition = this.subConfig.getIntegerProperty(JDChat.PARAM_USERLISTPOSITION, 0);
        this.textArea = new JTextPane();
        final HyperlinkListener hyp = new HyperlinkListener() {

            public void hyperlinkUpdate(final HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    if (e.getDescription().startsWith("intern")) {
                        final String[][] m = new AwReg(e.getDescription() + "?", "intern:([\\w]*?)\\|(.*?)\\?").getMatches();
                        if (m.length == 1) {
                            JDChat.this.doAction(m[0][0], m[0][1]);
                            return;
                        }
                    } else {
                        try {
                            JLink.openURL(e.getURL());
                        } catch (final Exception e1) {
                            JDLogger.exception(e1);
                        }
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
        this.tabbedPane = new JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        this.tabbedPane.add("JDChat", this.scrollPane);
        this.tabbedPane.addChangeListener(new ChangeListener() {

            public void stateChanged(final ChangeEvent e) {
                JDChat.this.tabbedPane.setForegroundAt(JDChat.this.tabbedPane.getSelectedIndex(), Color.black);
            }

        });
        this.textField = new JTextField();
        this.textField.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, Collections.EMPTY_SET);
        this.textField.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, Collections.EMPTY_SET);
        this.textField.addFocusListener(new FocusListener() {

            public void focusGained(final FocusEvent e) {
                JDChat.this.tabbedPane.setForegroundAt(JDChat.this.tabbedPane.getSelectedIndex(), Color.black);
            }

            public void focusLost(final FocusEvent e) {
                JDChat.this.tabbedPane.setForegroundAt(JDChat.this.tabbedPane.getSelectedIndex(), Color.black);
            }

        });
        this.textField.addKeyListener(new KeyListener() {

            private int    counter = 0;
            private String last    = null;

            public void keyPressed(final KeyEvent e) {
                final int sel = JDChat.this.tabbedPane.getSelectedIndex();
                JDChat.this.tabbedPane.setForegroundAt(sel, Color.black);
            }

            public void keyReleased(final KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {

                    if (JDChat.this.textField.getText().length() == 0) { return; }
                    if (JDChat.this.tabbedPane.getSelectedIndex() == 0 || JDChat.this.textField.getText().startsWith("/")) {
                        JDChat.this.sendMessage(JDChat.CHANNEL, JDChat.this.textField.getText());
                    } else {
                        JDChat.this.sendMessage(JDChat.CHANNEL, "/msg " + JDChat.this.tabbedPane.getTitleAt(JDChat.this.tabbedPane.getSelectedIndex()) + " " + JDChat.this.textField.getText());
                    }

                } else if (e.getKeyCode() == KeyEvent.VK_TAB) {
                    if (JDChat.this.textField.getText().length() == 0) {
                        if (JDChat.this.lastCommand != null) {
                            JDChat.this.textField.setText(JDChat.this.lastCommand);
                            JDChat.this.textField.requestFocus();
                        }
                        return;
                    }
                    String txt = JDChat.this.textField.getText();
                    if (this.last != null && txt.toLowerCase().startsWith(this.last.toLowerCase())) {
                        txt = this.last;
                    }

                    final String org = txt;
                    final int last = Math.max(0, txt.lastIndexOf(" "));
                    txt = txt.substring(last).trim();
                    final ArrayList<String> users = new ArrayList<String>();

                    final ArrayList<String> strings = new ArrayList<String>();
                    strings.addAll(JDChat.COMMANDS);
                    for (final User user : JDChat.this.NAMES) {
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
                    JDChat.this.textField.setText((JDChat.this.textField.getText().substring(0, last) + " " + user).trim());
                    JDChat.this.textField.requestFocus();

                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    if (JDChat.this.textField.getText().length() == 0) {
                        if (JDChat.this.lastCommand != null) {
                            JDChat.this.textField.setText(JDChat.this.lastCommand);
                            JDChat.this.textField.requestFocus();
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
        this.lang = new JComboBox(new JDLocale[] { JDL.getInstance("en"), JDL.getInstance("de"), JDL.getInstance("es"), JDL.getInstance("tr") });
        this.lang.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                JDChat.this.getPluginConfig().setProperty(JDChat.CHANNEL_LNG, JDChat.this.lang.getSelectedItem());
                JDChat.this.getPluginConfig().save();
                JDChat.this.initChannel();
            }

        });
        this.lang.setSelectedItem(this.getPluginConfig().getProperty(JDChat.CHANNEL_LNG, JDL.getInstance("en")));
        this.textArea.setContentType("text/html");
        this.textArea.setEditable(false);

        this.frame = new SwitchPanel() {
            private static final long serialVersionUID = 2138710083573682339L;

            @Override
            public void onHide() {
            }

            @Override
            public void onShow() {
            }
        };
        this.frame.setLayout(new MigLayout("ins 0, wrap 1", "[grow,fill]", "[grow,fill][]"));
        this.closeTab = new JButton(JDL.L("jd.plugins.optional.jdchat.closeTab", "Close Tab"));
        this.closeTab.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                if (JDChat.this.tabbedPane.getSelectedIndex() > 0) {
                    JDChat.this.delPMS(JDChat.this.tabbedPane.getTitleAt(JDChat.this.tabbedPane.getSelectedIndex()));
                } else if (JDChat.this.tabbedPane.getSelectedIndex() == 0) {
                    JDChat.this.addToText(null, JDChat.STYLE_SYSTEM_MESSAGE, "You can't close the main Chat!");
                }
            }
        });
        final JScrollPane scrollPane_userlist = new JScrollPane(this.right);
        switch (userlistposition) {
        case 0:
            this.frame.add(this.tabbedPane, "split 2");
            this.frame.add(scrollPane_userlist, "width 180:180:180");
            break;
        default:
        case 1:
            this.frame.add(scrollPane_userlist, "width 180:180:180 ,split 2");
            this.frame.add(this.tabbedPane);
            break;
        }

        this.frame.add(this.textField, "growx, split 3");
        this.frame.add(this.closeTab, "w pref!");
        this.frame.add(this.lang, "w pref!");
        this.lastAction = System.currentTimeMillis();
        final MouseMotionListener ml = new MouseMotionListener() {

            public void mouseDragged(final MouseEvent e) {
            }

            public void mouseMoved(final MouseEvent e) {
                JDChat.this.lastAction = System.currentTimeMillis();
                JDChat.this.setNickAway(false);
            }

        };
        this.frame.addMouseMotionListener(ml);
        this.textArea.addMouseMotionListener(ml);
        this.textField.addMouseMotionListener(ml);
        this.right.addMouseMotionListener(ml);
        this.frame.setSize(new Dimension(800, 600));
        this.frame.setVisible(true);
        this.startAwayObserver();
    }

    private void initIRC() {

        this.NAMES.clear();
        for (int i = 0; i < 20; i++) {
            final String host = this.subConfig.getStringProperty(JDChat.PARAM_HOST, "irc.freenode.net");
            final int port = this.subConfig.getIntegerProperty(JDChat.PARAM_PORT, 6667);
            final String pass = null;
            final String nick = this.getNickname();
            final String user = "jdChatuser";
            final String name = "jdChatuser";
            Balloon.show("JD Chat", null, "Connecting to JDChat...");
            this.addToText(null, JDChat.STYLE_SYSTEM_MESSAGE, "Connecting to JDChat...");
            this.conn = new IRCConnection(host, new int[] { port }, pass, nick, user, name);
            this.conn.setTimeout(1000 * 60 * 60);

            this.conn.addIRCEventListener(new IRCListener(this));
            this.conn.setEncoding("UTF-8");
            this.conn.setPong(true);
            this.conn.setDaemon(false);
            this.conn.setColors(false);
            try {
                this.conn.connect();
                break;
            } catch (final IOException e) {
                this.addToText(null, JDChat.STYLE_SYSTEM_MESSAGE, "Connect Timeout. Server not reachable...");
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
                for (int x = 0; x < JDChat.this.tabbedPane.getTabCount(); x++) {
                    if (JDChat.this.tabbedPane.getTitleAt(x).equals(user)) {
                        final int t = x;

                        String text = text2;
                        JDChat.this.tabbedPane.setForegroundAt(t, Color.RED);
                        if (text.length() > 40) {
                            text = text.substring(0, 40).concat("...");
                        }
                        if (!JDChat.this.tabbedPane.getTitleAt(JDChat.this.tabbedPane.getSelectedIndex()).equals(user)) {
                            Balloon.show("JD Chat", null, JDL.LF("jd.plugins.optional.jdchat.newmessage", "New Message from %s:<hr> %s", user, text));
                        }
                        return null;
                    }
                }
                return null;
            }
        }.invokeLater();
    }

    public void onConnected() {
        this.initChannel();
        this.setLoggedIn(true);
        this.perform();

    }

    @Override
    public void onExit() {
        this.NAMES.clear();
        this.pms.clear();
        this.setLoggedIn(false);
        this.updateNamesPanel();
        if (this.view != null) {
            SwingGui.getInstance().disposeView(this.view);
        }
        this.view = null;
        if (this.conn != null) {
            this.conn.close();
        }
        this.conn = null;
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
        final String[] perform = org.appwork.utils.AwReg.getLines(this.subConfig.getStringProperty(JDChat.PARAM_PERFORM));
        if (perform == null) { return; }
        for (final String cmd : perform) {
            if (cmd.trim().length() > 0) {
                this.sendMessage(JDChat.CHANNEL, cmd);
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
                JDChat.this.pms.put(userNew.trim().toLowerCase(), JDChat.this.pms.get(userOld.trim().toLowerCase()));
                for (int x = 0; x < JDChat.this.tabbedPane.getComponentCount(); x++) {
                    if (JDChat.this.tabbedPane.getTitleAt(x).equalsIgnoreCase(userOld)) {
                        JDChat.this.tabbedPane.remove(x);
                        break;
                    }
                }
                JDChat.this.pms.remove(userOld);
                JDChat.this.tabbedPane.add(userNew.trim(), JDChat.this.pms.get(userNew.trim().toLowerCase()).getScrollPane());
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
        this.conn.doNames(JDChat.CHANNEL);
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
            if (org.appwork.utils.AwReg.matches(cmd, JDChat.CMD_PM)) {
                new GuiRunnable<Object>() {

                    @Override
                    public Object runSave() {
                        JDChat.this.textField.setText("");
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
                this.addToText(this.getUser(this.conn.getNick()), JDChat.STYLE_SELF, Utils.prepareMsg(rest.substring(end).trim()), this.pms.get(rest.substring(0, end).trim().toLowerCase()).getTextArea(), this.pms.get(rest.substring(0, end).trim().toLowerCase()).getSb());
            } else if (org.appwork.utils.AwReg.matches(cmd, JDChat.CMD_SLAP)) {
                this.conn.doPrivmsg(channel2, new String(new byte[] { 1 }) + "ACTION " + " slaps " + rest + " with the whole Javadocs" + new String(new byte[] { 1 }));
                this.addToText(null, JDChat.STYLE_ACTION, this.conn.getNick() + " slaps " + rest + " with the whole Javadocs");

                this.lastCommand = "/slap ";
            } else if (org.appwork.utils.AwReg.matches(cmd, JDChat.CMD_ACTION)) {
                this.lastCommand = "/me ";
                this.conn.doPrivmsg(channel2, new String(new byte[] { 1 }) + "ACTION " + this.prepareToSend(rest.trim()) + new String(new byte[] { 1 }));
                this.addToText(null, JDChat.STYLE_ACTION, this.conn.getNick() + " " + Utils.prepareMsg(rest.trim()));

            } else if (org.appwork.utils.AwReg.matches(cmd, JDChat.CMD_VERSION)) {

                final String msg = " is using " + JDUtilities.getJDTitle() + " with Java " + JDUtilities.getJavaVersion() + " on a " + CrossSystem.getOSString() + " system";
                this.conn.doPrivmsg(channel2, new String(new byte[] { 1 }) + "ACTION " + this.prepareToSend(msg) + new String(new byte[] { 1 }));
                this.addToText(null, JDChat.STYLE_ACTION, this.conn.getNick() + " " + Utils.prepareMsg(msg));
            } else if (org.appwork.utils.AwReg.matches(cmd, JDChat.CMD_MODE)) {
                end = rest.indexOf(" ");
                if (end < 0) {
                    end = rest.length();
                }
                this.lastCommand = "/mode ";
                this.conn.doMode(JDChat.CHANNEL, rest.trim());
            } else if (org.appwork.utils.AwReg.matches(cmd, JDChat.CMD_TRANSLATE)) {
                end = rest.indexOf(" ");
                if (end < 0) {
                    end = rest.length();
                }
                final String[] tofrom = rest.substring(0, end).trim().split("to");
                if (tofrom == null || tofrom.length != 2) {
                    this.addToText(null, JDChat.STYLE_ERROR, "Command /translate " + rest.substring(0, end).trim() + " is not available");
                    return;
                }
                final String t;
                t = JDL.translate(tofrom[0], tofrom[1], Utils.prepareMsg(rest.substring(end).trim())).getTranslated();
                this.lastCommand = "/translate " + rest.substring(0, end).trim() + " ";
                new GuiRunnable<Object>() {

                    @Override
                    public Object runSave() {
                        JDChat.this.textField.setText(t);
                        return null;
                    }
                }.invokeLater();
            } else if (org.appwork.utils.AwReg.matches(cmd, JDChat.CMD_TOPIC)) {
                this.conn.doTopic(JDChat.CHANNEL, this.prepareToSend(rest));
                this.lastCommand = "/topic ";
            } else if (org.appwork.utils.AwReg.matches(cmd, JDChat.CMD_JOIN)) {
                this.NAMES.clear();
                if (this.conn != null) {
                    this.addToText(null, JDChat.STYLE_NOTICE, "Change channel to: " + rest);
                }
                if (this.conn != null) {
                    this.conn.doPart(JDChat.CHANNEL, " --> " + rest);
                }
                JDChat.CHANNEL = rest;
                if (this.conn != null) {
                    this.conn.doJoin(JDChat.CHANNEL, null);
                }

                this.lastCommand = "/join " + rest;
                this.setLoggedIn(true);
                this.perform();
            } else if (org.appwork.utils.AwReg.matches(cmd, JDChat.CMD_NICK)) {
                this.conn.doNick(rest.trim());
                this.lastCommand = "/nick ";
                this.subConfig.setProperty(JDChat.PARAM_NICK, rest.trim());
                this.subConfig.save();

            } else if (org.appwork.utils.AwReg.matches(cmd, JDChat.CMD_CONNECT)) {
                if (this.conn == null || !this.conn.isConnected()) {
                    this.initIRC();
                }
            } else if (org.appwork.utils.AwReg.matches(cmd, JDChat.CMD_DISCONNECT)) {
                if (this.conn != null && this.conn.isConnected()) {
                    this.conn.close();
                }
            } else if (org.appwork.utils.AwReg.matches(cmd, JDChat.CMD_EXIT)) {
                this.setGuiEnable(false);
            } else {
                this.addToText(null, JDChat.STYLE_ERROR, "Command /" + cmd + " is not available");
            }

        } else {
            this.conn.doPrivmsg(channel2, this.prepareToSend(text));
            this.addToText(this.getUser(this.conn.getNick()), JDChat.STYLE_SELF, Utils.prepareMsg(text));
        }
        new GuiRunnable<Object>() {

            @Override
            public Object runSave() {
                JDChat.this.textField.setText("");
                JDChat.this.textField.requestFocus();
                return null;
            }
        }.invokeLater();
    }

    @Override
    public void setGuiEnable(final boolean b) {
        if (b) {

            if (this.view == null) {
                this.initGUI();
                this.view = new JDChatView() {

                    private static final long serialVersionUID = 3966113588850405974L;

                    @Override
                    protected void initMenu(final JMenuBar menubar) {
                        menubar.add(JDChat.this.top = new JLabel(JDL.L("jd.plugins.optional.jdchat.JDChat.topic.default", "Loading Message of the day")));
                        JDChat.this.top.setToolTipText(JDL.L("jd.plugins.optional.jdchat.JDChat.topic.tooltip", "Message of the day"));
                    }

                };
                this.view.getBroadcaster().addListener(new SwitchPanelListener() {

                    @Override
                    public void onPanelEvent(final SwitchPanelEvent event) {
                        if (event.getEventID() == SwitchPanelEvent.ON_REMOVE) {
                            JDChat.this.setGuiEnable(false);
                        }
                    }

                });

                this.view.setContent(this.frame);
            }
            SwingGui.getInstance().setContent(this.view);

            new Thread() {
                @Override
                public void run() {
                    JDChat.this.initIRC();
                }
            }.start();
        } else {

            if (this.frame != null) {
                SwingGui.getInstance().disposeView(this.view);
                this.stopAddon();
            }
        }
        if (this.activateAction != null && this.activateAction.isSelected() != b) {
            this.activateAction.setSelected(b);
        }
    }

    public void setLoggedIn(final boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    public void setNick(final String nickname) {
        if (nickname == null) { return; }
        this.addToText(null, JDChat.STYLE_SYSTEM_MESSAGE, "Rename to " + nickname);

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
        this.addToText(null, JDChat.STYLE_SYSTEM_MESSAGE, "<b>Topic is: " + msg + "</b>");
        new GuiRunnable<Object>() {

            @Override
            public Object runSave() {
                JDChat.this.top.setText(msg);
                return null;
            }

        }.start();
    }

    private void startAwayObserver() {
        final Thread th = new Thread() {
            @Override
            public void run() {
                while (true) {
                    if (System.currentTimeMillis() - JDChat.this.lastAction > JDChat.AWAY_TIMEOUT) {
                        JDChat.this.setNickAway(true);
                    } else {
                        JDChat.this.setNickAway(false);
                    }
                    try {
                        Thread.sleep(10000);
                    } catch (final InterruptedException e) {
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
        Collections.sort(this.NAMES);
        final boolean color = this.subConfig.getBooleanProperty(JDChat.PARAM_USERCOLOR, true);
        sb.append("<ul>");
        for (final User name : this.NAMES) {
            sb.append("<li>");
            if (!color) {
                sb.append("<span style='color:#" + name.getColor() + (name.name.equals(this.conn.getNick()) ? ";font-weight:bold;" : "") + "'>");
            } else {
                sb.append("<span style='color:#000000" + (name.name.equals(this.conn.getNick()) ? ";font-weight:bold;" : "") + "'>");
            }
            sb.append(name.getRank() + name.getNickLink("query"));
            sb.append("</span></li>");
        }
        sb.append("</ul>");

        if (this.right != null) {
            new GuiRunnable<Object>() {

                @Override
                public Object runSave() {
                    JDChat.this.right.setText(JDChat.USERLIST_STYLE + sb);
                    return null;
                }

            }.start();
        }
    }

}

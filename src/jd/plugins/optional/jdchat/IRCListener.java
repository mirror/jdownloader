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

import java.util.TreeMap;
import java.util.logging.Logger;

import jd.controlling.JDLogger;
import jd.gui.UserIO;
import jd.parser.Regex;
import jd.utils.Upload;
import jd.utils.locale.JDL;

import org.schwering.irc.lib.IRCConstants;
import org.schwering.irc.lib.IRCEventListener;
import org.schwering.irc.lib.IRCModeParser;
import org.schwering.irc.lib.IRCUser;

class IRCListener implements IRCEventListener {
    public static Logger logger = jd.controlling.JDLogger.getLogger();
    private JDChat owner;

    public IRCListener(JDChat owner) {
        this.owner = owner;
    }

    public void onDisconnected() {
        // logger.info("Disconnected");
        owner.setLoggedIn(false);
        owner.addToText(null, JDChat.STYLE_SYSTEM_MESSAGE, "Connection lost. type /connect if jd does not connect by itself");

    }

    public void onError(int num, String msg) {
        owner.addToText(null, JDChat.STYLE_ERROR, msg);
        // logger.info("Error #" + num + ": " + Utils.prepareMsg(msg));
        switch (num) {
        case IRCConstants.ERR_NICKNAMEINUSE:
            if (!owner.isLoggedIn()) {
                owner.setNickCount(owner.getNickCount() + 1);
                owner.setNick(owner.getNickname());
            }
            break;

        }
    }

    public void onError(String msg) {
        owner.addToText(null, JDChat.STYLE_ERROR, Utils.prepareMsg(msg));
        // logger.info("Error: " + msg);
    }

    public void onInvite(String chan, IRCUser u, String nickPass) {
        // logger.info(chan + "> " + u.getNick() + " invites " + nickPass);
    }

    public void onJoin(String chan, IRCUser u) {
        // logger.info(chan + "> " + u.getNick() + " joins");
        owner.addToText(null, JDChat.STYLE_SYSTEM_MESSAGE, u.getNick() + " joins");
        owner.addUser(u.getNick());
        // owner.requestNameList();
    }

    public void onKick(String chan, IRCUser u, String nickPass, String msg) {
        // logger.info(chan + "> " + u.getNick() + " kicks " + nickPass);

        owner.addToText(null, JDChat.STYLE_SYSTEM_MESSAGE, u.getNick() + " kicks " + nickPass + " (" + msg + ")");
    }

    public void onMode(IRCUser u, String nickPass, String mode) {
        // logger.info("Mode: " + u.getNick() + " sets modes " + mode + " " +
        // nickPass);
        owner.addToText(null, JDChat.STYLE_SYSTEM_MESSAGE, u.getNick() + " sets modes " + mode + " " + nickPass);

    }

    public void onMode(String chan, IRCUser u, IRCModeParser mp) {
        // logger.info(chan + "> " + u.getNick() + " sets mode: " +
        // mp.getLine());

        for (int i = 1; i <= mp.getCount(); i++) {
            owner.onMode(mp.getOperatorAt(i), mp.getModeAt(i), mp.getArgAt(i));
        }

        owner.addToText(null, JDChat.STYLE_SYSTEM_MESSAGE, u.getNick() + " sets mode: " + mp.getLine());
    }

    public void onNick(IRCUser u, String nickNew) {
        // logger.info("Nick: " + u.getNick() + " is now known as " + nickNew);
        owner.addToText(null, JDChat.STYLE_SYSTEM_MESSAGE, u.getNick() + " is now known as " + nickNew);
        owner.renameUser(u.getNick(), nickNew);
        if (owner.getPms().containsKey(u.getNick().toLowerCase())) owner.renamePMS(u.getNick().toLowerCase(), nickNew);
    }

    public void onNotice(String target, IRCUser u, String msg) {
        // logger.info(target + "> " + u.getNick() + " (notice): " + msg);
        if (u.getNick() == null) {
            // owner.addToText(JDChat.COLOR_NOTICE,"System" + " (notice): " +
            // Utils.prepareMsg(msg));
        } else {
            owner.addToText(null, JDChat.STYLE_NOTICE, u.getNick() + " (notice): " + Utils.prepareMsg(msg));
        }
        if (msg.endsWith("has been ghosted.")) {
            owner.removeUser(msg.substring(0, msg.indexOf("has been ghosted.")).trim());
        }
    }

    public void onPart(String chan, IRCUser u, String msg) {
        // logger.info(chan + "> " + u.getNick() + " parts");
        if (msg != null && msg.trim().length() > 0) {
            owner.addToText(null, JDChat.STYLE_SYSTEM_MESSAGE, u.getNick() + " has left the channel (" + msg + ")");
        } else {
            owner.addToText(null, JDChat.STYLE_SYSTEM_MESSAGE, u.getNick() + " has left the channel");
        }
        owner.removeUser(u.getNick());
        // owner.requestNameList();

    }

    public void onPing(String p) {
        // logger.info("ping: "+p);
    }

    public void onPrivmsg(String chan, IRCUser u, String msg) {

        final User user = owner.getUser(u.getNick());
        if (user == null) { return; }
        String nickt = owner.getNick().toLowerCase();
        boolean isPrivate = chan.toLowerCase().equals(nickt);
        String msgt = msg.toLowerCase();
        if ((user.rank == User.RANK_VOICE || user.rank == User.RANK_OP) && ((msgt.matches("!gettv[\\s]+.*") && msgt.replaceFirst("!gettv[\\s]+", "").trim().equals(nickt)) || (isPrivate && (msgt.matches("!gettv.*") || msgt.matches("!tv.*"))))) {

            new Thread(new Runnable() {

                public void run() {

                    String[] data = UserIO.getInstance().requestTwoTextFieldDialog(JDL.L("plugin.optional.jdchat.teamviewer.yourtvdata", "Your Teamviewer logins:"), "ID:", "", "PW:", "");
                    if (data == null || new Regex(data[0], "^[\\s]*$").matches()) {
                        owner.sendMessage(user.name, owner.getNick() + " hat den Teamviewer Dialog geschlossen.");
                    } else {
                        owner.sendMessage(user.name, "Teamviewerdaten von " + owner.getNick() + ": ID: " + data[0] + " PW: " + data[1]);
                    }
                }

            }).start();

        } else if ((user.rank == User.RANK_VOICE || user.rank == User.RANK_OP) && ((msgt.matches("!getlog[\\s]+.*") && msgt.replaceFirst("!getlog[\\s]+", "").trim().equals(nickt)) || (isPrivate && (msgt.matches("!getlog.*") || msgt.matches("!log.*"))))) {

            new Thread(new Runnable() {

                public void run() {
                    if (UserIO.RETURN_OK == UserIO.getInstance().requestConfirmDialog(0, JDL.LF("plugin.optional.jdchat.getlog", "%s needs a log to solve your problem. Do you agree to send him the Log?", user.name))) {

                        String url = Upload.toJDownloader(JDLogger.getLog(), "JDChatuser:\r\n\r\n" + owner.getNick());
                        if (url == null) {
                            UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN | UserIO.NO_CANCEL_OPTION, JDL.L("sys.warning.loguploadfailed", "Upload of logfile failed!"));
                        } else {
                            owner.sendMessage(user.name, url);
                        }
                    } else {
                        owner.sendMessage(user.name, owner.getNick() + " gibt seine Log nicht her");
                    }
                }

            }).start();

        } else if (msg.trim().startsWith("ACTION ")) {
            owner.addToText(null, JDChat.STYLE_ACTION, user.getNickLink("pmnick") + " " + Utils.prepareMsg(msg.trim().substring(6).trim()));

        } else if (chan.equals(owner.getNick())) {
            TreeMap<String, JDChatPMS> pms = owner.getPms();
            if (!pms.containsKey(user.name.toLowerCase())) {
                owner.addPMS(user.name);
                pms = owner.getPms();
            }
            owner.notifyPMS(user.name, msg);
            owner.addToText(user, null, Utils.prepareMsg(msg), pms.get(user.name.toLowerCase()).getTextArea(), pms.get(user.name.toLowerCase()).getSb());

        } else {
            owner.addToText(user, null, Utils.prepareMsg(msg));

        }

    }

    public void onQuit(IRCUser u, String msg) {
        // logger.info("Quit: " + u.getNick());
        if (owner.getPms().containsKey(u.getNick().toLowerCase())) {
            if (msg != null && msg.trim().length() > 0) {
                owner.addToText(null, JDChat.STYLE_SYSTEM_MESSAGE, u.getNick() + " has left the channel (" + msg + ")", owner.getPms().get(u.getNick().toLowerCase()).getTextArea(), owner.getPms().get(u.getNick().toLowerCase()).getSb());
            } else {
                owner.addToText(null, JDChat.STYLE_SYSTEM_MESSAGE, u.getNick() + " has left the channel", owner.getPms().get(u.getNick().toLowerCase()).getTextArea(), owner.getPms().get(u.getNick().toLowerCase()).getSb());
            }
        }
        if (msg != null && msg.trim().length() > 0) {
            owner.addToText(null, JDChat.STYLE_SYSTEM_MESSAGE, u.getNick() + " has left the channel (" + msg + ")");
        } else {
            owner.addToText(null, JDChat.STYLE_SYSTEM_MESSAGE, u.getNick() + " has left the channel");
        }
        owner.removeUser(u.getNick());
    }

    public void onRegistered() {
        // logger.info("Connected");
        owner.addToText(null, JDChat.STYLE_SYSTEM_MESSAGE, "Connection estabilished");
        owner.onConnected();
    }

    public void onReply(int num, String value, String msg) {

        // logger.info("Reply #" + num + ": " + value + " " + msg);
        if (num == IRCConstants.RPL_NAMREPLY) {
            owner.addUsers(msg.trim().split(" "));
        }

        if (num == IRCConstants.RPL_ENDOFNAMES) {
            owner.updateNamesPanel();

        }
        if (num == IRCConstants.RPL_TOPIC) {
            owner.setTopic(msg);

        }

    }

    public void onTopic(String chan, IRCUser u, String topic) {
        // logger.info(chan + "> " + u.getNick() + " changes topic into: " +
        // topic);
    }

    public void unknown(String a, String b, String c, String d) {
        // logger.info("UNKNOWN: " + a + " b " + c + " " + d);
    }
}
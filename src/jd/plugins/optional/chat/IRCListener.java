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

package jd.plugins.optional.chat;

import java.util.TreeMap;
import java.util.logging.Logger;

import jd.controlling.JDLogger;
import jd.gui.UserIO;
import jd.utils.Upload;
import jd.utils.locale.JDL;

import org.schwering.irc.lib.IRCConstants;
import org.schwering.irc.lib.IRCEventListener;
import org.schwering.irc.lib.IRCModeParser;
import org.schwering.irc.lib.IRCUser;

class IRCListener implements IRCEventListener {
    public static Logger logger = jd.controlling.JDLogger.getLogger();
    private final JDChat owner;

    public IRCListener(final JDChat owner) {
        this.owner = owner;
    }

    public void onDisconnected() {
        // logger.info("Disconnected");
        this.owner.setLoggedIn(false);
        this.owner.addToText(null, JDChat.STYLE_SYSTEM_MESSAGE, "Connection lost. type /connect if jd does not connect by itself");

    }

    public void onError(final int num, final String msg) {
        this.owner.addToText(null, JDChat.STYLE_ERROR, msg);
        // logger.info("Error #" + num + ": " + Utils.prepareMsg(msg));
        switch (num) {
        case IRCConstants.ERR_NICKNAMEINUSE:
            if (!this.owner.isLoggedIn()) {
                this.owner.setNickCount(this.owner.getNickCount() + 1);
                this.owner.setNick(this.owner.getNickname());
            }
            break;

        }
    }

    public void onError(final String msg) {
        this.owner.addToText(null, JDChat.STYLE_ERROR, Utils.prepareMsg(msg));
        // logger.info("Error: " + msg);
    }

    public void onInvite(final String chan, final IRCUser u, final String nickPass) {
        // logger.info(chan + "> " + u.getNick() + " invites " + nickPass);
    }

    public void onJoin(final String chan, final IRCUser u) {
        // logger.info(chan + "> " + u.getNick() + " joins");
        this.owner.addToText(null, JDChat.STYLE_SYSTEM_MESSAGE, u.getNick() + " joins");
        this.owner.addUser(u.getNick());
        // owner.requestNameList();
    }

    public void onKick(final String chan, final IRCUser u, final String nickPass, final String msg) {
        // logger.info(chan + "> " + u.getNick() + " kicks " + nickPass);

        this.owner.addToText(null, JDChat.STYLE_SYSTEM_MESSAGE, u.getNick() + " kicks " + nickPass + " (" + msg + ")");
    }

    public void onMode(final IRCUser u, final String nickPass, final String mode) {
        // logger.info("Mode: " + u.getNick() + " sets modes " + mode + " " +
        // nickPass);
        this.owner.addToText(null, JDChat.STYLE_SYSTEM_MESSAGE, u.getNick() + " sets modes " + mode + " " + nickPass);

    }

    public void onMode(final String chan, final IRCUser u, final IRCModeParser mp) {
        // logger.info(chan + "> " + u.getNick() + " sets mode: " +
        // mp.getLine());

        for (int i = 1; i <= mp.getCount(); i++) {
            this.owner.onMode(mp.getOperatorAt(i), mp.getModeAt(i), mp.getArgAt(i));
        }

        this.owner.addToText(null, JDChat.STYLE_SYSTEM_MESSAGE, u.getNick() + " sets mode: " + mp.getLine());
    }

    public void onNick(final IRCUser u, final String nickNew) {
        // logger.info("Nick: " + u.getNick() + " is now known as " + nickNew);
        this.owner.addToText(null, JDChat.STYLE_SYSTEM_MESSAGE, u.getNick() + " is now known as " + nickNew);
        this.owner.renameUser(u.getNick(), nickNew);
        if (this.owner.getPms().containsKey(u.getNick().toLowerCase())) {
            this.owner.renamePMS(u.getNick().toLowerCase(), nickNew);
        }
    }

    public void onNotice(final String target, final IRCUser u, final String msg) {
        // logger.info(target + "> " + u.getNick() + " (notice): " + msg);
        if (u.getNick() == null) {
            // owner.addToText(JDChat.COLOR_NOTICE,"System" + " (notice): " +
            // Utils.prepareMsg(msg));
        } else {
            this.owner.addToText(null, JDChat.STYLE_NOTICE, u.getNick() + " (notice): " + Utils.prepareMsg(msg));
        }
        if (msg.endsWith("has been ghosted.")) {
            this.owner.removeUser(msg.substring(0, msg.indexOf("has been ghosted.")).trim());
        }
    }

    public void onPart(final String chan, final IRCUser u, final String msg) {
        // logger.info(chan + "> " + u.getNick() + " parts");
        if (msg != null && msg.trim().length() > 0) {
            this.owner.addToText(null, JDChat.STYLE_SYSTEM_MESSAGE, u.getNick() + " has left the channel (" + msg + ")");
        } else {
            this.owner.addToText(null, JDChat.STYLE_SYSTEM_MESSAGE, u.getNick() + " has left the channel");
        }
        this.owner.removeUser(u.getNick());
        // owner.requestNameList();

    }

    public void onPing(final String p) {
        // logger.info("ping: "+p);
    }

    public void onPrivmsg(final String chan, final IRCUser u, final String msg) {

        final User user = this.owner.getUser(u.getNick());
        if (user == null) { return; }
        final String nickt = this.owner.getNick().toLowerCase();
        final boolean isPrivate = chan.toLowerCase().equals(nickt);
        final String msgt = msg.toLowerCase();
        if ((user.rank == User.RANK_VOICE || user.rank == User.RANK_OP) && (msgt.matches("!getlog[\\s]+.*") && msgt.replaceFirst("!getlog[\\s]+", "").trim().equals(nickt) || isPrivate && (msgt.matches("!getlog.*") || msgt.matches("!log.*")))) {

            new Thread(new Runnable() {

                public void run() {
                    if (UserIO.RETURN_OK == UserIO.getInstance().requestConfirmDialog(0, JDL.LF("plugin.optional.jdchat.getlog", "%s needs a log to solve your problem. Do you agree to send him the Log?", user.name))) {

                        final String url = Upload.toJDownloader(JDLogger.getLog(), "JDChatuser:\r\n\r\n" + IRCListener.this.owner.getNick());
                        if (url == null) {
                            UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN | UserIO.NO_CANCEL_OPTION, JDL.L("sys.warning.loguploadfailed", "Upload of logfile failed!"));
                        } else {
                            IRCListener.this.owner.sendMessage(user.name, url);
                        }
                    } else {
                        IRCListener.this.owner.sendMessage(user.name, IRCListener.this.owner.getNick() + " gibt seine Log nicht her");
                    }
                }

            }).start();

        } else if (msg.trim().startsWith("ACTION ")) {
            this.owner.addToText(null, JDChat.STYLE_ACTION, user.getNickLink("pmnick") + " " + Utils.prepareMsg(msg.trim().substring(6).trim()));

        } else if (chan.equals(this.owner.getNick())) {
            TreeMap<String, JDChatPMS> pms = this.owner.getPms();
            if (!pms.containsKey(user.name.toLowerCase())) {
                this.owner.addPMS(user.name);
                pms = this.owner.getPms();
            }
            this.owner.notifyPMS(user.name, msg);
            this.owner.addToText(user, null, Utils.prepareMsg(msg), pms.get(user.name.toLowerCase()).getTextArea(), pms.get(user.name.toLowerCase()).getSb());

        } else {
            this.owner.addToText(user, null, Utils.prepareMsg(msg));

        }

    }

    public void onQuit(final IRCUser u, final String msg) {
        // logger.info("Quit: " + u.getNick());
        if (this.owner.getPms().containsKey(u.getNick().toLowerCase())) {
            if (msg != null && msg.trim().length() > 0) {
                this.owner.addToText(null, JDChat.STYLE_SYSTEM_MESSAGE, u.getNick() + " has left the channel (" + msg + ")", this.owner.getPms().get(u.getNick().toLowerCase()).getTextArea(), this.owner.getPms().get(u.getNick().toLowerCase()).getSb());
            } else {
                this.owner.addToText(null, JDChat.STYLE_SYSTEM_MESSAGE, u.getNick() + " has left the channel", this.owner.getPms().get(u.getNick().toLowerCase()).getTextArea(), this.owner.getPms().get(u.getNick().toLowerCase()).getSb());
            }
        }
        if (msg != null && msg.trim().length() > 0) {
            this.owner.addToText(null, JDChat.STYLE_SYSTEM_MESSAGE, u.getNick() + " has left the channel (" + msg + ")");
        } else {
            this.owner.addToText(null, JDChat.STYLE_SYSTEM_MESSAGE, u.getNick() + " has left the channel");
        }
        this.owner.removeUser(u.getNick());
    }

    public void onRegistered() {
        // logger.info("Connected");
        this.owner.addToText(null, JDChat.STYLE_SYSTEM_MESSAGE, "Connection estabilished");
        this.owner.onConnected();
    }

    public void onReply(final int num, final String value, final String msg) {

        // logger.info("Reply #" + num + ": " + value + " " + msg);
        if (num == IRCConstants.RPL_NAMREPLY) {
            this.owner.addUsers(msg.trim().split(" "));
        }

        if (num == IRCConstants.RPL_ENDOFNAMES) {
            this.owner.updateNamesPanel();

        }
        if (num == IRCConstants.RPL_TOPIC) {
            this.owner.setTopic(msg);

        }

    }

    public void onTopic(final String chan, final IRCUser u, final String topic) {
        // logger.info(chan + "> " + u.getNick() + " changes topic into: " +
        // topic);
    }

    public void unknown(final String a, final String b, final String c, final String d) {
        // logger.info("UNKNOWN: " + a + " b " + c + " " + d);
    }
}
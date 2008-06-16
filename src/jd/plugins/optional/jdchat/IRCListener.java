package jd.plugins.optional.jdchat;

import java.util.logging.Logger;

import jd.utils.JDUtilities;

import org.schwering.irc.lib.IRCConstants;
import org.schwering.irc.lib.IRCEventListener;
import org.schwering.irc.lib.IRCModeParser;
import org.schwering.irc.lib.IRCUser;

class IRCListener implements IRCEventListener {
   
    private JDChat owner;
    public IRCListener(JDChat owner){
        this.owner=owner;
    }
    public static Logger logger = JDUtilities.getLogger();
    public void onRegistered() {
        logger.info("Connected");
        owner.addToText(JDChat.COLOR_SYSTEM, "Connection estabilished");
        owner.onConnected();
    }

    public void onDisconnected() {
        logger.info("Disconnected");
        owner.setLoggedIn(false);
        owner.addToText(JDChat.COLOR_SYSTEM, "Connection lost. type /connect if jd does not connect by itself");
   
        
    }

    public void onError(String msg) {
        owner.addToText(JDChat.COLOR_ERROR, Utils.prepareMsg(msg));
        logger.info("Error: " + msg);
    }

    public void onError(int num, String msg) {
        owner.addToText(JDChat.COLOR_ERROR, msg);
        logger.info("Error #" + num + ": " + Utils.prepareMsg(msg));
        switch (num){
        case IRCConstants.ERR_NICKNAMEINUSE:
            if(!owner.isLoggedIn()){
            owner.setNickCount(owner.getNickCount() + 1);
            owner.setNick(owner.getNickname());
            }
            break;
            
                
            
        }
    }

    public void onInvite(String chan, IRCUser u, String nickPass) {
        logger.info(chan + "> " + u.getNick() + " invites " + nickPass);
    }

    public void onJoin(String chan, IRCUser u) {
        logger.info(chan + "> " + u.getNick() + " joins");
        owner.addToText(JDChat.COLOR_SYSTEM, u.getNick() + " joins");
       
        owner.requestNameList();
    }

    public void onKick(String chan, IRCUser u, String nickPass, String msg) {
        logger.info(chan + "> " + u.getNick() + " kicks " + nickPass);
        
        owner.addToText(JDChat.COLOR_SYSTEM, u.getNick() + " kicks " + nickPass);
    }

    public void onMode(IRCUser u, String nickPass, String mode) {
        logger.info("Mode: " + u.getNick() + " sets modes " + mode + " " + nickPass);
        owner.addToText(JDChat.COLOR_SYSTEM, u.getNick() + " sets modes " + mode + " " + nickPass);
    }

    public void onMode(String chan, IRCUser u, IRCModeParser mp) {
        logger.info(chan + "> " + u.getNick() + " sets mode: " + mp.getLine());
        
        owner.addToText(JDChat.COLOR_SYSTEM, u.getNick() + " sets mode: " + mp.getLine());
    }

    public void onNick(IRCUser u, String nickNew) {
        logger.info("Nick: " + u.getNick() + " is now known as " + nickNew);
        owner.addToText(JDChat.COLOR_SYSTEM, u.getNick() + " is now known as " + nickNew);
        owner.requestNameList();
    }

    public void onNotice(String target, IRCUser u, String msg) {
        logger.info(target + "> " + u.getNick() + " (notice): " + msg);
        if(u.getNick()==null){
            owner.addToText(JDChat.COLOR_NOTICE,"System" + " (notice): " + Utils.prepareMsg(msg));
        }else{
        owner.addToText(JDChat.COLOR_NOTICE, u.getNick() + " (notice): " + Utils.prepareMsg(msg));
        }
    }

    public void onPart(String chan, IRCUser u, String msg) {
        logger.info(chan + "> " + u.getNick() + " parts");
        
        owner.addToText(JDChat.COLOR_SYSTEM, u.getNick() + " parts");
      owner.requestNameList();

    }
 

    public void onPrivmsg(String chan, IRCUser u, String msg) {
        User user = owner.getUser(u.getNick());
        if (user == null) { return; }
        if (chan.equals(owner.getNick())) {
            owner.addToText(JDChat.COLOR_PM, user.getNickLink("pmnick") + "> " + Utils.prepareMsg(msg));
            
         
            // resetNamesList();
            // conn.doNames(CHANNEL);
        } else {
            owner.addToText(JDChat.COLOR_CHAT, user.getNickLink("channick") + ": " + Utils.prepareMsg(msg));
           
        }

        logger.info(chan + "> " + u.getNick() + ": " + msg);
    }

    public void onQuit(IRCUser u, String msg) {
        logger.info("Quit: " + u.getNick());
    }

    public void onReply(int num, String value, String msg) {
        
        logger.info("Reply #" + num + ": " + value + " " + msg);
        if (num == IRCConstants.RPL_NAMREPLY) {
            owner.updateNames(msg.trim().split(" "));
        }

        if (num == IRCConstants.RPL_ENDOFNAMES) {
            owner.updateNamesPanel();

        }
        if (num == IRCConstants.RPL_TOPIC) {
            owner.setMOD(msg);

        }

    }

    public void onTopic(String chan, IRCUser u, String topic) {
        logger.info(chan + "> " + u.getNick() + " changes topic into: " + topic);
    }

    public void onPing(String p) {
logger.info("ping: "+p);
    }

    public void unknown(String a, String b, String c, String d) {
        logger.info("UNKNOWN: " + a + " b " + c + " " + d);
    }
}
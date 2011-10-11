package org.jdownloader.jdserv;

import org.appwork.remotecall.RemoteCallInterface;
import org.appwork.remotecall.server.ParsingException;
import org.jdownloader.remotecall.RemoteClient;

public class JD_SERV_CONSTANTS {
    public static final String       HOST   = "192.168.2.250/thomas/fcgi";
    public static final RemoteClient CLIENT = new RemoteClient(HOST);

    public static <T extends RemoteCallInterface> T create(Class<T> class1) {
        try {
            return CLIENT.getFactory().newInstance(class1);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (ParsingException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        // CounterInterface.INST.inc("test");
        // System.out.println(CounterInterface.INST.getValue("test"));

    }

    public static String redirect(String buyPremiumLink) {
        return buyPremiumLink.replace("http://", "http://" + HOST + "/redirect?");
    }
}

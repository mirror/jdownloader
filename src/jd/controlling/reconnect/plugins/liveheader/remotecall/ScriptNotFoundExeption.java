package jd.controlling.reconnect.plugins.liveheader.remotecall;

import org.appwork.remotecall.server.RemoteCallException;

public class ScriptNotFoundExeption extends RemoteCallException {

    public ScriptNotFoundExeption(String string) {
        super(string);
    }

    /**
     * 
     */
    public ScriptNotFoundExeption() {
        super();
        
    }

}

package jd.controlling.reconnect.plugins.liveheader.remotecall;

import java.util.ArrayList;

import org.appwork.remotecall.RemoteCallInterface;
import org.appwork.remotecall.server.Requestor;

public interface RecollInterface extends RemoteCallInterface {
    public boolean addRouter(RouterData data, Requestor request);

    public boolean isAlive();

    public ArrayList<RouterData> findRouter(RouterData data, Requestor request);

    public void setNotWorking(String scriptID, Requestor request) throws ScriptNotFoundExeption, FailedException;

    public void setWorking(String scriptID, Requestor object, long successDuration, long offlineDuration) throws ScriptNotFoundExeption, FailedException;

    public RouterData getOne() throws FailedException;

    public String getIsp(Requestor request);

    public boolean isReconnectPossible(Requestor request);
}

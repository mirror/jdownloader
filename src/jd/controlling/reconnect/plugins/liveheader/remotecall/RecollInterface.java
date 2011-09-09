package jd.controlling.reconnect.plugins.liveheader.remotecall;

import java.util.ArrayList;

public interface RecollInterface {
    public boolean addRouter(RouterData data);

    public boolean isAlive();

    public void throwit(Exception e) throws Exception;

    public String[] getRouterNames();

    public ArrayList<RouterData> findRouter(RouterData data);
}

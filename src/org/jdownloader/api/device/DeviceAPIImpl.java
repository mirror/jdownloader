package org.jdownloader.api.device;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import jd.controlling.reconnect.ipcheck.BalancedWebIPCheck;
import jd.controlling.reconnect.ipcheck.IP;

import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.utils.net.httpconnection.HTTPProxyUtils;
import org.jdownloader.api.myjdownloader.MyJDownloaderController;
import org.jdownloader.api.myjdownloader.MyJDownloaderDirectServer;
import org.jdownloader.api.myjdownloader.MyJDownloaderSettings.DIRECTMODE;
import org.jdownloader.myjdownloader.client.json.DirectConnectionInfo;
import org.jdownloader.myjdownloader.client.json.DirectConnectionInfos;
import org.jdownloader.settings.staticreferences.CFG_MYJD;

public class DeviceAPIImpl implements DeviceAPI {
    
    @Override
    public DirectConnectionInfos getDirectConnectionInfos(RemoteAPIRequest request) {
        MyJDownloaderDirectServer directServer = MyJDownloaderController.getInstance().getConnectThread().getDirectServer();
        DirectConnectionInfos ret = new DirectConnectionInfos();
        if (directServer == null || !directServer.isAlive()) return ret;
        List<DirectConnectionInfo> infos = new ArrayList<DirectConnectionInfo>();
        List<InetAddress> localIPs = HTTPProxyUtils.getLocalIPs(true);
        if (localIPs != null) {
            for (InetAddress localIP : localIPs) {
                DirectConnectionInfo info = new DirectConnectionInfo();
                info.setPort(directServer.getPort());
                info.setIp(localIP.getHostAddress());
                infos.add(info);
            }
        }
        if (DIRECTMODE.LAN_WAN_MANUAL.equals(CFG_MYJD.CFG.getDirectConnectMode())) {
            try {
                IP externalIP = BalancedWebIPCheck.getInstance().getExternalIP();
                if (externalIP.getIP() != null) {
                    DirectConnectionInfo info = new DirectConnectionInfo();
                    info.setPort(CFG_MYJD.CFG.getManualRemotePort());
                    info.setIp(externalIP.getIP());
                    infos.add(info);
                }
            } catch (final Throwable e) {
            }
        }
        if (infos.size() > 0) ret.setInfos(infos);
        return ret;
        
    }
    
    @Override
    public boolean ping() {
        return true;
    }
    
}

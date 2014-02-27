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
import org.jdownloader.myjdownloader.client.json.DirectConnectionInfo;
import org.jdownloader.myjdownloader.client.json.DirectConnectionInfos;

public class DeviceAPIImpl implements DeviceAPI {
    
    @Override
    public DirectConnectionInfos getDirectConnectionInfos(RemoteAPIRequest request) {
        MyJDownloaderDirectServer directServer = MyJDownloaderController.getInstance().getConnectThread().getDirectServer();
        DirectConnectionInfos ret = new DirectConnectionInfos();
        if (directServer == null || !directServer.isAlive() || directServer.getLocalPort() < 0) return ret;
        List<DirectConnectionInfo> infos = new ArrayList<DirectConnectionInfo>();
        List<InetAddress> localIPs = HTTPProxyUtils.getLocalIPs(true);
        if (localIPs != null) {
            for (InetAddress localIP : localIPs) {
                DirectConnectionInfo info = new DirectConnectionInfo();
                info.setPort(directServer.getLocalPort());
                info.setIp(localIP.getHostAddress());
                infos.add(info);
            }
        }
        if (directServer.getRemotePort() > 0) {
            try {
                IP externalIP = BalancedWebIPCheck.getInstance().getExternalIP();
                if (externalIP.getIP() != null) {
                    DirectConnectionInfo info = new DirectConnectionInfo();
                    info.setPort(directServer.getRemotePort());
                    info.setIp(externalIP.getIP());
                    infos.add(info);
                }
            } catch (final Throwable e) {
                /* eg OfflineException */
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

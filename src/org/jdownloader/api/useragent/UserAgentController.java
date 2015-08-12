package org.jdownloader.api.useragent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import jd.http.Browser;
import jd.nutils.encoding.Encoding;

import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.SessionRemoteAPIRequest;
import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.httpserver.session.HttpSession;
import org.jdownloader.logging.LogController;

public class UserAgentController {
    private ConcurrentHashMap<String, ConnectedDevice>          map;
    private UserAgentEventSender                                eventSender;
    private LogSource                                           logger;
    private ConcurrentHashMap<ConnectedDevice, DelayedRunnable> timeoutcheck;

    public UserAgentController() {
        eventSender = new UserAgentEventSender();
        logger = LogController.getInstance().getLogger("UserAgentController");
        map = new ConcurrentHashMap<String, ConnectedDevice>();
        timeoutcheck = new ConcurrentHashMap<ConnectedDevice, DelayedRunnable>();

    }

    protected void checkTimeouted() {
    }

    public UserAgentEventSender getEventSender() {
        return eventSender;
    }

    public void handle(RemoteAPIRequest request) {
        synchronized (this) {

            String nuaID = createSessionUAID(request);

            ConnectedDevice ua = map.get(nuaID);

            if (ua == null) {
                ua = createNewUserAgent(request, nuaID);
                final ConnectedDevice fua = ua;
                map.put(nuaID, ua);

                new Thread("UserAgentCreater") {
                    {
                        setDaemon(true);
                    }

                    public void run() {
                        try {
                            String json = new Browser().getPage("http://update3.jdownloader.org/jdserv/ua/get?" + Encoding.urlDecode(fua.getUserAgentString(), false));

                            UserAgentInfo info = JSonStorage.restoreFromString(json, new TypeRef<UserAgentInfo>() {
                            });
                            fua.setInfo(info);
                        } catch (Throwable e) {
                            logger.log(e);
                        }
                        DelayedRunnable delayed;
                        timeoutcheck.put(fua, delayed = new DelayedRunnable(fua.getTimeout()) {

                            @Override
                            public void delayedrun() {
                                onTimeout(fua);

                            }
                        });
                        delayed.resetAndStart();
                        eventSender.fireEvent(new UserAgentEvent() {

                            @Override
                            public void fireTo(UserAgentListener listener) {
                                listener.onNewAPIUserAgent(fua);
                            }

                        });

                    };
                }.start();

            }
            DelayedRunnable delayed = timeoutcheck.get(ua);
            if (delayed != null) {
                delayed.resetAndStart();
            }
            String pre = ua.getConnectionString();
            ua.setLatestRequest(request);
            if (StringUtils.equals(pre, ua.getConnectionString())) {
                final ConnectedDevice fua = ua;
                eventSender.fireEvent(new UserAgentEvent() {

                    @Override
                    public void fireTo(UserAgentListener listener) {
                        listener.onAPIUserAgentUpdate(fua);
                    }

                });
            }

        }
    }

    protected void onTimeout(final ConnectedDevice fua) {
        timeoutcheck.remove(fua);
        map.remove(fua.getId());
        eventSender.fireEvent(new UserAgentEvent() {

            @Override
            public void fireTo(UserAgentListener listener) {

                listener.onRemovedAPIUserAgent(fua);
            }

        });
    }

    private String createSessionUAID(RemoteAPIRequest request) {
        return request.getRequestHeaders().getValue("User-Agent") + "_Session: " + getSessionID(request);
    }

    private String getSessionID(RemoteAPIRequest request) {
        if (request instanceof SessionRemoteAPIRequest) {
            HttpSession session = ((SessionRemoteAPIRequest) request).getSession();
            if (session != null) {
                return session.getSessionID();
            }
        }
        return null;
    }

    public List<ConnectedDevice> list() {
        ArrayList<ConnectedDevice> ret = new ArrayList<ConnectedDevice>();
        for (Entry<String, ConnectedDevice> es : map.entrySet()) {
            ConnectedDevice ua = es.getValue();
            if (ua != null) {
                ret.add(ua);
            }

        }
        return ret;

    }

    private ConnectedDevice createNewUserAgent(RemoteAPIRequest request, String nuaID) {
        ConnectedDevice ret = new ConnectedDevice(nuaID);
        ret.setLatestRequest(request);

        return ret;
    }

}

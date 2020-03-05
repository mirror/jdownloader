package org.jdownloader.api.useragent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import jd.controlling.TaskQueue;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;

import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.SessionRemoteAPIRequest;
import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.uio.UIOManager;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.httpserver.session.HttpSession;
import org.jdownloader.api.myjdownloader.MyJDownloaderController;
import org.jdownloader.logging.LogController;
import org.jdownloader.myjdownloader.client.exceptions.MyJDownloaderException;

public class UserAgentController {
    private final ConcurrentHashMap<String, ConnectedDevice>          map;
    private final UserAgentEventSender                                eventSender;
    private final LogSource                                           logger;
    private final ConcurrentHashMap<ConnectedDevice, DelayedRunnable> timeoutcheck;
    private final ConcurrentHashMap<String, UserAgentInfo>            uaCache;

    public UserAgentController() {
        eventSender = new UserAgentEventSender();
        logger = LogController.getInstance().getLogger("UserAgentController");
        map = new ConcurrentHashMap<String, ConnectedDevice>();
        timeoutcheck = new ConcurrentHashMap<ConnectedDevice, DelayedRunnable>();
        uaCache = new ConcurrentHashMap<String, UserAgentInfo>();
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
                map.put(nuaID, ua);
                final String userAgent = ua.getUserAgentString();
                if (userAgent != null && StringUtils.isNotEmpty(userAgent) && !ConnectedDevice.isApp(userAgent)) {
                    final UserAgentInfo cachedUA = uaCache.get(userAgent);
                    if (cachedUA != null) {
                        ua.setInfo(cachedUA);
                    } else {
                        final ConnectedDevice fua = ua;
                        new Thread("UserAgentCreater:" + userAgent) {
                            {
                                setDaemon(true);
                            }

                            public void run() {
                                try {
                                    final String json = new Browser().getPage("https://update3.jdownloader.org/jdserv/ua/get?" + Encoding.urlEncode(userAgent));
                                    final UserAgentInfo info = JSonStorage.restoreFromString(json, new TypeRef<UserAgentInfo>() {
                                    });
                                    if (info != null) {
                                        uaCache.put(userAgent, info);
                                        fua.setInfo(info);
                                    }
                                } catch (Throwable e) {
                                    logger.log(e);
                                }
                            };
                        }.start();
                    }
                }
                final ConnectedDevice fua = ua;
                final DelayedRunnable delayed;
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
            } else {
                final DelayedRunnable delayed = timeoutcheck.get(ua);
                if (delayed != null) {
                    delayed.resetAndStart();
                }
            }
            final ConnectedDevice fua = ua;
            ua.setLatestRequest(request);
            eventSender.fireEvent(new UserAgentEvent() {
                @Override
                public void fireTo(UserAgentListener listener) {
                    listener.onAPIUserAgentUpdate(fua);
                }
            });
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
            final HttpSession session = ((SessionRemoteAPIRequest) request).getSession();
            if (session != null) {
                return session.getSessionID();
            }
        }
        return null;
    }

    public List<ConnectedDevice> list() {
        final ArrayList<ConnectedDevice> ret = new ArrayList<ConnectedDevice>();
        for (final Entry<String, ConnectedDevice> es : map.entrySet()) {
            final ConnectedDevice ua = es.getValue();
            if (ua != null) {
                ret.add(ua);
            }
        }
        return ret;
    }

    private ConnectedDevice createNewUserAgent(RemoteAPIRequest request, String nuaID) {
        final ConnectedDevice ret = new ConnectedDevice(nuaID);
        final String token = getSessionID(request);
        ret.setConnectToken(token);
        ret.setLatestRequest(request);
        return ret;
    }

    public void disconnectDecice(final ConnectedDevice device) {
        TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {
            @Override
            protected Void run() throws RuntimeException {
                try {
                    MyJDownloaderController.getInstance().terminateSession(device.getConnectToken());
                } catch (MyJDownloaderException e) {
                    UIOManager.I().showException(e.getMessage(), e);
                }
                return null;
            }
        });
        onTimeout(device);
    }
}

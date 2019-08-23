package org.jdownloader.plugins.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import jd.http.Browser;
import jd.http.Request;

public class RequestHistory {
    public static enum TYPE {
        FULL,
        OPEN
    }

    protected final Browser br;

    public Browser getBrowser() {
        return br;
    }

    protected final Request request;
    protected final TYPE    type;

    private RequestHistory(Browser browser, Request request, TYPE type) {
        this.request = request;
        this.type = type;
        this.br = browser;
    }

    public Request getRequest() {
        return request;
    }

    public TYPE getType() {
        return type;
    }

    protected final static Map<Thread, List<RequestHistory>> MAP = new WeakHashMap<Thread, List<RequestHistory>>();

    public static List<RequestHistory> getCurrentThread(final boolean createFlag) {
        List<RequestHistory> list;
        synchronized (MAP) {
            list = MAP.get(Thread.currentThread());
            if (list == null && createFlag) {
                list = new ArrayList<RequestHistory>();
                MAP.put(Thread.currentThread(), list);
            }
        }
        return list;
    }

    public static RequestHistory addToCurrentThread(Browser br, Request request, RequestHistory.TYPE type) {
        final List<RequestHistory> list = getCurrentThread(true);
        final RequestHistory ret;
        list.add(ret = new RequestHistory(br, request, type));
        return ret;
    }

    public static boolean removeFromCurrentThread(final RequestHistory request) {
        final List<RequestHistory> list = getCurrentThread(false);
        if (list != null && list.remove(request)) {
            if (list.size() == 0) {
                synchronized (MAP) {
                    MAP.remove(Thread.currentThread());
                }
            }
            return true;
        } else {
            return false;
        }
    }
}

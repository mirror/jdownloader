package org.jdownloader.api.dialog;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;

import org.appwork.remoteapi.events.EventObject;
import org.appwork.remoteapi.events.EventPublisher;
import org.appwork.remoteapi.events.EventsSender;
import org.appwork.remoteapi.events.SimpleEventObject;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.uio.In;
import org.appwork.uio.Out;
import org.appwork.uio.UserIODefinition;
import org.appwork.uio.UserIODefinition.CloseReason;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.jdownloader.logging.LogController;

public class DialogApiImpl implements EventPublisher, DialogApiInterface {

    private RemoteAPIIOHandlerWrapper         callback;
    private AtomicLong                        id;
    private LogSource                         logger;
    private HashMap<Long, ApiHandle>          map;
    private String[]                          eventIDs;
    private CopyOnWriteArraySet<EventsSender> eventSenders;

    public static enum Event {
        NEW,
        EXPIRED
    }

    public DialogApiImpl(RemoteAPIIOHandlerWrapper remoteAPIHandlerWrapper) {
        this.callback = remoteAPIHandlerWrapper;
        id = new AtomicLong();
        logger = LogController.getInstance().getLogger(DialogApiImpl.class.getName());
        map = new HashMap<Long, ApiHandle>();
        eventIDs = new String[] { Event.NEW.toString(), Event.EXPIRED.toString() };
        eventSenders = new CopyOnWriteArraySet<EventsSender>();
    }

    public <T extends UserIODefinition> ApiHandle enqueue(Class<T> class1, T impl) {
        final ApiHandle ret = new ApiHandle(class1, impl, id.incrementAndGet(), Thread.currentThread());
        synchronized (map) {
            map.put(ret.getId(), ret);
        }
        new Thread("Dialog: " + ((AbstractDialog) impl).getTitle()) {
            public void run() {
                try {
                    EventObject eventObject = new SimpleEventObject(DialogApiImpl.this, Event.NEW.toString(), ret.getId(), "" + ret.getId());
                    for (EventsSender eventSender : eventSenders) {
                        eventSender.publishEvent(eventObject, null);
                    }
                    ret.waitFor();
                } catch (Exception e) {
                    logger.log(e);
                } finally {
                    try {
                        EventObject eventObject = new SimpleEventObject(DialogApiImpl.this, Event.EXPIRED.toString(), ret.getId(), "" + ret.getId());
                        for (EventsSender eventSender : eventSenders) {
                            eventSender.publishEvent(eventObject, null);
                        }
                    } finally {
                        synchronized (map) {
                            map.remove(ret.getId());
                        }
                        callback.onHandlerDone(ret);
                    }
                }
            }

        }.start();
        return ret;
    }

    @Override
    public String[] getPublisherEventIDs() {
        return eventIDs;
    }

    @Override
    public String getPublisherName() {
        return "dialogs";
    }

    @Override
    public synchronized void register(EventsSender eventsAPI) {
        eventSenders.add(eventsAPI);
    }

    @Override
    public synchronized void unregister(EventsSender eventsAPI) {
        eventSenders.remove(eventsAPI);
    }

    @Override
    public DialogInfo get(long id, boolean icons, boolean properties) throws InvalidIdException {
        ApiHandle handle = null;
        synchronized (map) {
            handle = map.get(id);
        }
        if (handle == null) { throw new InvalidIdException(id); }
        DialogInfo ret = new DialogInfo();
        ret.setType(handle.getIface().getName());
        if (properties) {
            for (Method m : handle.getIface().getMethods()) {
                m.setAccessible(true);
                if (m.getParameterTypes().length > 0 || m.getAnnotation(Out.class) == null) continue;
                try {

                    if (m.getName().startsWith("get")) {
                        Object value = m.invoke(handle.getImpl(), new Object[] {});
                        if (value != null) {
                            ret.put(m.getName().substring(3).toLowerCase(Locale.ENGLISH), value);
                        }
                    } else if (m.getName().startsWith("is")) {
                        Object value = m.invoke(handle.getImpl(), new Object[] {});
                        if (value != null) {
                            ret.put(m.getName().substring(2).toLowerCase(Locale.ENGLISH), value);
                        }
                    } else {
                        Object value = m.invoke(handle.getImpl(), new Object[] {});
                        if (value != null) {
                            ret.put(m.getName().toLowerCase(Locale.ENGLISH), value);
                        }
                    }
                } catch (Exception e) {
                    logger.log(e);
                }

            }
            if (handle.getImpl() instanceof AbstractDialog) {
                if (((AbstractDialog) handle.getImpl()).isCountdownFlagEnabled()) {
                    ret.put("timeout", ((AbstractDialog) handle.getImpl()).getCountdown());
                }
            }
        }
        if (icons) {
            if (handle.getImpl() instanceof AbstractDialog) {
                String icon = ((AbstractDialog) handle.getImpl()).getIconDataUrl();
                if (icon != null) {
                    ret.put("icon", icon);
                }
            }
        }
        // zu map umwursten
        return ret;
    }

    @Override
    public long[] list() {
        ArrayList<ApiHandle> list = null;
        synchronized (map) {
            list = new ArrayList<ApiHandle>(map.values());
        }
        Collections.sort(list, new Comparator<ApiHandle>() {

            @Override
            public int compare(ApiHandle o1, ApiHandle o2) {
                return new Long(o1.getId()).compareTo(new Long(o2.getId()));
            }
        });

        long[] ret = new long[list.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = list.get(i).getId();
        }

        return ret;
    }

    @Override
    public void answer(long id, final HashMap<String, Object> data) throws BadOrderException, InvalidIdException {
        ApiHandle handle = null;

        synchronized (map) {
            long max = -1;
            for (ApiHandle ah : map.values()) {
                max = Math.max(max, ah.getId());
            }
            if (max > id) throw new BadOrderException(max);
            handle = map.get(id);
        }
        if (handle == null) { throw new InvalidIdException(id); }
        final CloseReason closeReason = CloseReason.valueOf(data.get("closereason").toString().toUpperCase(Locale.ENGLISH));
        UserIODefinition ret = (UserIODefinition) Proxy.newProxyInstance(DialogApiImpl.class.getClassLoader(), new Class<?>[] { handle.getIface() }, new InvocationHandler() {

            @Override
            public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {

                if ("throwCloseExceptions".equals(method.getName())) {

                    switch (closeReason) {
                    case CANCEL:
                        throw new DialogCanceledException(Dialog.RETURN_CANCEL);
                    case CLOSE:
                        throw new DialogCanceledException(Dialog.RETURN_CLOSED);
                    case TIMEOUT:
                        throw new DialogCanceledException(Dialog.RETURN_CLOSED | Dialog.RETURN_TIMEOUT);
                    case INTERRUPT:
                        throw new DialogCanceledException(Dialog.RETURN_CLOSED | Dialog.RETURN_INTERRUPT);
                    default:
                        return null;
                    }

                }
                HashMap<String, Object> d = data;
                String key = method.getName().toLowerCase(Locale.ENGLISH);
                if (key.startsWith("get")) {
                    key = key.substring(3);
                } else if (key.startsWith("is")) {
                    key = key.substring(2);
                }
                Object value = data.get(key);
                String json = JSonStorage.serializeToJson(value);
                Object retValue = JSonStorage.restoreFromString(json, new TypeRef(method.getGenericReturnType()) {
                });
                return retValue;
            }
        });
        handle.setAnswer(ret);

    }

    @Override
    public DialogTypeInfo getTypeInfo(String dialogType) throws TypeNotFoundException {
        Class<?> cls;
        DialogTypeInfo ret = new DialogTypeInfo();
        try {
            cls = Class.forName(dialogType);

            for (Method m : cls.getMethods()) {
                m.setAccessible(true);
                if (m.getParameterTypes().length > 0) continue;
                try {

                    if (m.getName().startsWith("get")) {
                        if (m.getAnnotation(Out.class) != null) {
                            ret.addOut(m.getName().substring(3).toLowerCase(Locale.ENGLISH), m.getGenericReturnType().toString());
                        }

                        if (m.getAnnotation(In.class) != null) {
                            ret.addIn(m.getName().substring(3).toLowerCase(Locale.ENGLISH), m.getGenericReturnType().toString());
                        }
                    } else if (m.getName().startsWith("is")) {
                        if (m.getAnnotation(Out.class) != null) {
                            ret.addOut(m.getName().substring(2).toLowerCase(Locale.ENGLISH), m.getGenericReturnType().toString());
                        }

                        if (m.getAnnotation(In.class) != null) {
                            ret.addIn(m.getName().substring(2).toLowerCase(Locale.ENGLISH), m.getGenericReturnType().toString());
                        }
                    } else {
                        if (m.getAnnotation(Out.class) != null) {
                            ret.addOut(m.getName().toLowerCase(Locale.ENGLISH), m.getGenericReturnType().toString());
                        }

                        if (m.getAnnotation(In.class) != null) {
                            ret.addIn(m.getName().toLowerCase(Locale.ENGLISH), m.getGenericReturnType().toString());
                        }
                    }
                } catch (Exception e) {
                    logger.log(e);
                }

            }
        } catch (ClassNotFoundException e1) {
            throw new TypeNotFoundException();
        }
        return ret;
    }
}

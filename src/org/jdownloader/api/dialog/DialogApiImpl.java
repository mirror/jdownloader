package org.jdownloader.api.dialog;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;

import org.appwork.remoteapi.events.EventObject;
import org.appwork.remoteapi.events.EventPublisher;
import org.appwork.remoteapi.events.RemoteAPIEventsSender;
import org.appwork.remoteapi.events.SimpleEventObject;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.uio.CloseReason;
import org.appwork.uio.In;
import org.appwork.uio.Out;
import org.appwork.uio.UserIODefinition;
import org.appwork.utils.Application;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.jdownloader.api.RemoteAPIController;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.logging.LogController;
import org.jdownloader.myjdownloader.client.bindings.interfaces.DialogInterface;

public class DialogApiImpl implements EventPublisher, DialogApiInterface {

    private final RemoteAPIIOHandlerWrapper                  callback;
    private final LogSource                                  logger;
    private final Map<UniqueAlltimeID, ApiHandle>            map          = new HashMap<UniqueAlltimeID, ApiHandle>();
    private final String[]                                   eventIDs;
    private final CopyOnWriteArraySet<RemoteAPIEventsSender> eventSenders = new CopyOnWriteArraySet<RemoteAPIEventsSender>();

    public static enum Event {
        NEW,
        EXPIRED
    }

    public DialogApiImpl(final RemoteAPIIOHandlerWrapper remoteAPIHandlerWrapper) {
        RemoteAPIController.validateInterfaces(DialogApiInterface.class, DialogInterface.class);
        this.callback = remoteAPIHandlerWrapper;
        logger = LogController.getInstance().getLogger(DialogApiImpl.class.getName());
        eventIDs = new String[] { Event.NEW.toString(), Event.EXPIRED.toString() };
    }

    public <T extends UserIODefinition> ApiHandle enqueue(Class<T> class1, T impl) {
        if (class1 == null) {
            return null;
        }
        if (!impl.isRemoteAPIEnabled() && !Application.isHeadless()) {
            // if the application is headless, we have to forward all dialogs.
            return null;
        }
        final ApiHandle ret = new ApiHandle(class1, impl, Thread.currentThread());
        final long dialogID = ret.getUniqueAlltimeID().getID();
        synchronized (map) {
            map.put(ret.getUniqueAlltimeID(), ret);
        }
        new Thread("Dialog(" + ret.getUniqueAlltimeID() + "): " + ((AbstractDialog) impl).getTitle()) {
            public void run() {
                try {
                    if (eventSenders.size() > 0) {
                        final EventObject eventObject = new SimpleEventObject(DialogApiImpl.this, Event.NEW.toString(), dialogID, Long.toString(dialogID));
                        for (RemoteAPIEventsSender eventSender : eventSenders) {
                            eventSender.publishEvent(eventObject, null);
                        }
                    }
                    ret.waitFor();
                } catch (Exception e) {
                    logger.log(e);
                } finally {
                    try {
                        if (eventSenders.size() > 0) {
                            final EventObject eventObject = new SimpleEventObject(DialogApiImpl.this, Event.EXPIRED.toString(), dialogID, Long.toString(dialogID));
                            for (RemoteAPIEventsSender eventSender : eventSenders) {
                                eventSender.publishEvent(eventObject, null);
                            }
                        }
                    } finally {
                        synchronized (map) {
                            map.remove(ret.getUniqueAlltimeID());
                        }
                        if (callback != null) {
                            callback.onHandlerDone(ret);
                        }
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
    public void register(RemoteAPIEventsSender eventsAPI) {
        eventSenders.add(eventsAPI);
    }

    @Override
    public void unregister(RemoteAPIEventsSender eventsAPI) {
        eventSenders.remove(eventsAPI);
    }

    @Override
    public DialogInfo get(long dialogID, boolean icons, boolean properties) throws InvalidIdException {
        final ApiHandle handle;
        synchronized (map) {
            handle = map.get(new UniqueAlltimeID(dialogID));
        }
        if (handle == null) {
            throw new InvalidIdException(dialogID);
        }
        final DialogInfo ret = new DialogInfo();
        ret.setType(handle.getIface().getName());
        if (properties) {
            for (Method m : handle.getIface().getMethods()) {
                m.setAccessible(true);
                if (m.getParameterTypes().length > 0 || m.getAnnotation(Out.class) == null) {
                    continue;
                }
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
        final ArrayList<ApiHandle> list;
        synchronized (map) {
            list = new ArrayList<ApiHandle>(map.values());
        }
        Collections.sort(list, new Comparator<ApiHandle>() {

            public int compare(long x, long y) {
                return (x < y) ? -1 : ((x == y) ? 0 : 1);
            }

            @Override
            public int compare(ApiHandle o1, ApiHandle o2) {
                return compare(o1.getUniqueAlltimeID().getID(), o2.getUniqueAlltimeID().getID());
            }
        });
        final long[] ret = new long[list.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = list.get(i).getUniqueAlltimeID().getID();
        }
        return ret;
    }

    @Override
    public void answer(long dialogID, final HashMap<String, Object> data) throws BadOrderException, InvalidIdException {
        final ApiHandle handle;
        synchronized (map) {
            for (final ApiHandle ah : map.values()) {
                if (ah.getUniqueAlltimeID().getID() > dialogID) {
                    throw new BadOrderException(ah.getUniqueAlltimeID().getID());
                }
            }
            handle = map.get(new UniqueAlltimeID(dialogID));
        }
        if (handle == null) {
            throw new InvalidIdException(dialogID);
        }
        final CloseReason closeReason = CloseReason.valueOf(data.get("closereason").toString().toUpperCase(Locale.ENGLISH));
        final UserIODefinition ret = (UserIODefinition) Proxy.newProxyInstance(DialogApiImpl.class.getClassLoader(), new Class<?>[] { handle.getIface() }, new InvocationHandler() {

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

                String key = method.getName().toLowerCase(Locale.ENGLISH);
                if (key.startsWith("get")) {
                    key = key.substring(3);
                } else if (key.startsWith("is")) {
                    key = key.substring(2);
                }

                Object value = data.get(key);
                // TODO: the webinterface seems to return dontshowagain instead of dontshowagainselected.(confirm dialog eventscripter:
                // {dontshowagain=true, closereason=OK}) This workaround must be available until this is fixed
                if ("dontshowagainselected".equals(key) && value == null) {
                    value = data.get("dontshowagain");
                }
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
        final DialogTypeInfo ret = new DialogTypeInfo();
        final Class<?> cls;
        try {
            cls = Class.forName(dialogType);
            for (Method m : cls.getMethods()) {
                m.setAccessible(true);
                if (m.getParameterTypes().length > 0) {
                    continue;
                }
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

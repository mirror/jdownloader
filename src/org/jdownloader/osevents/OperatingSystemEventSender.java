package org.jdownloader.osevents;

import org.appwork.utils.event.Eventsender;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.osevents.multios.SignalEventSource;

public class OperatingSystemEventSender extends Eventsender<OperatingSystemListener, OperatingSystemEvent> {
    private static final OperatingSystemEventSender INSTANCE = new OperatingSystemEventSender();

    /**
     * get the only existing instance of OperatingSystemEventSender. This is a singleton
     * 
     * @return
     */
    public static OperatingSystemEventSender getInstance() {
        return OperatingSystemEventSender.INSTANCE;
    }

    /**
     * Create a new instance of OperatingSystemEventSender. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private OperatingSystemEventSender() {

    }

    @Override
    protected void fireEvent(OperatingSystemListener listener, OperatingSystemEvent event) {
        switch (event.getType()) {
        case SESSION_END:
            listener.onOperatingSystemSessionEnd();
            break;
        case SHUTDOWN_VETO:
            listener.onOperatingSystemShutdownVeto((ShutdownOperatingSystemVetoEvent) event);
            break;
        case SIGNAL:
            listener.onOperatingSystemSignal((String) event.getParameter(0), (Integer) event.getParameter(1));
            break;
        default:
            System.out.println("Unhandled Event: " + event);
        }
    }

    public void init() {

        if (CrossSystem.isWindows()) {
            // new WindowsEventSource() {
            //
            // @Override
            // public boolean onQueryEndSession() {
            // ShutdownOperatingSystemVetoEvent veto = new ShutdownOperatingSystemVetoEvent();
            // fireEvent(veto);
            // return !veto.isVeto();
            // }
            //
            // @Override
            // public void onEndSession() {
            //
            // fireEvent(new OperatingSystemEvent(OperatingSystemEvent.Type.SESSION_END));
            // }
            //
            // }.init();

        }

        new SignalEventSource() {

            @Override
            public void onSignal(String name, int number) {
                fireEvent(new OperatingSystemEvent(OperatingSystemEvent.Type.SIGNAL, name, number));
            }

        }.init();

    }
}
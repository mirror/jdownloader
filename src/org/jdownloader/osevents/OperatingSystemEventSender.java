package org.jdownloader.osevents;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownVetoFilter;
import org.appwork.shutdown.ShutdownVetoListener;
import org.appwork.utils.event.Eventsender;
import org.jdownloader.logging.LogController;
import org.jdownloader.osevents.multios.SignalEventSource;

public class OperatingSystemEventSender extends Eventsender<OperatingSystemListener, OperatingSystemEvent> implements OperatingSystemListener {
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
     * Create a new instance of OperatingSystemEventSender. This is a singleton class. Access the only existing instance by using {@link #getInstance()}.
     */
    private OperatingSystemEventSender() {
    }

    @Override
    protected void fireEvent(OperatingSystemListener listener, OperatingSystemEvent event) {
        switch (event.getType()) {
        case SIGNAL_TERM:
            listener.onOperatingSystemTerm();
            break;
        default:
            System.out.println("Unhandled Event: " + event);
        }
    }

    public void init() {
        try {
            new SignalEventSource() {

                @Override
                public void onSignal(String name, int number) {
                    if ("TERM".equals(name) || "INT".equals(name)) {
                        fireEvent(new OperatingSystemEvent(OperatingSystemEvent.Type.SIGNAL_TERM, name, number));
                        onOperatingSystemTerm();
                    }
                }
            }.init();
        } catch (final Throwable e) {
            LogController.GL.log(e);
        }
    }

    @Override
    public void onOperatingSystemTerm() {
        ShutdownController.getInstance().requestShutdown(true, new ShutdownVetoFilter() {

            @Override
            public void gotVetoFrom(ShutdownVetoListener listener) {
            }

            @Override
            public boolean askForVeto(ShutdownVetoListener listener) {
                return false;
            }
        });
    }

}
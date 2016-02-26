package org.jdownloader.osevents;

import org.appwork.shutdown.ShutdownController;
import org.appwork.utils.event.Eventsender;
import org.jdownloader.osevents.multios.SignalEventSource;
import org.jdownloader.updatev2.ForcedShutdown;

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

    private final SignalEventSource signalEventSource;

    /**
     * Create a new instance of OperatingSystemEventSender. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private OperatingSystemEventSender() {
        SignalEventSource signalEventSource = null;
        try {
            signalEventSource = new SignalEventSource() {

                @Override
                public void onSignal(String name, int number) {
                    if ("HUP".equals(name)) {
                        fireEvent(new OperatingSystemEvent(OperatingSystemEvent.Type.SIGNAL_HUP, name, number));
                        onOperatingSystemTerm();
                    } else if ("TERM".equals(name) || "INT".equals(name)) {
                        fireEvent(new OperatingSystemEvent(OperatingSystemEvent.Type.SIGNAL_TERM, name, number));
                        onOperatingSystemTerm();
                    }
                }
            };
        } catch (final Throwable e) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
        }
        this.signalEventSource = signalEventSource;
    }

    @Override
    protected void fireEvent(OperatingSystemListener listener, OperatingSystemEvent event) {
        switch (event.getType()) {
        case SIGNAL_HUP:
        case SIGNAL_TERM:
            listener.onOperatingSystemTerm();
            break;
        default:
            System.out.println("Unhandled Event: " + event);
        }
    }

    public boolean setIgnoreSignal(final String signal, boolean ignore) {
        if (signalEventSource != null) {
            return signalEventSource.setIgnore(signal, ignore);
        }
        return false;
    }

    @Override
    public void onOperatingSystemTerm() {
        ShutdownController.getInstance().requestShutdown(new ForcedShutdown());
    }

}
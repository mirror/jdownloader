package org.jdownloader.gui.menu.eventsender;

import org.appwork.utils.event.Eventsender;

public class MenuFactoryEventSender extends Eventsender<MenuFactoryListener, MenuFactoryEvent> {
    private static final MenuFactoryEventSender INSTANCE = new MenuFactoryEventSender();

    /**
     * get the only existing instance of
     * DownloadTableContextmenuFactoryEventSender. This is a singleton
     * 
     * @return
     */
    public static MenuFactoryEventSender getInstance() {
        return MenuFactoryEventSender.INSTANCE;
    }

    /**
     * Create a new instance of DownloadTableContextmenuFactoryEventSender. This
     * is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private MenuFactoryEventSender() {

    }

    @Override
    protected void fireEvent(MenuFactoryListener listener, MenuFactoryEvent event) {
        switch (event.getType()) {
        case EXTEND:

            listener.onExtendPopupMenu(event.getContext());

            break;
        default:
            System.out.println("Unhandled Event: " + event);
        }
    }

}
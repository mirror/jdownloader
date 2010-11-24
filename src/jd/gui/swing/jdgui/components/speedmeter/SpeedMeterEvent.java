package jd.gui.swing.jdgui.components.speedmeter;

import org.appwork.utils.event.DefaultIntEvent;

public class SpeedMeterEvent extends DefaultIntEvent {

    public static final int UPDATED = 0;

    public SpeedMeterEvent(Object source, int id) {
        super(source, id);
    }

}

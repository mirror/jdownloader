package jd.gui.swing.jdgui.components.speedmeter;

import org.appwork.utils.event.DefaultEvent;

public class SpeedMeterEvent extends DefaultEvent {

    public static final int UPDATED = 0;

    public SpeedMeterEvent(Object source, int id) {
        super(source, id);
    }

}

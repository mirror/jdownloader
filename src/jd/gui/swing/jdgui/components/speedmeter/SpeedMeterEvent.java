package jd.gui.swing.jdgui.components.speedmeter;

import jd.event.JDEvent;

public class SpeedMeterEvent extends JDEvent {

    public static final int UPDATED = 0;

    public SpeedMeterEvent(Object source, int id) {
        super(source, id);
    }

}

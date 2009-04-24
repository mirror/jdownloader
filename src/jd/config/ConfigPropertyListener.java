package jd.config;

import jd.event.ControlEvent;
import jd.event.ControlListener;

public abstract class ConfigPropertyListener implements ControlListener {
    private String[] list;
    private boolean strict;

    public ConfigPropertyListener(String... args) {
        super();
        list = args;
        strict = true;
    }

    public boolean isStrict() {
        return strict;
    }

    /**
     * Sets if the propertykey value should get compared by == nor equals
     * 
     * @param strict
     */
    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    public void controlEvent(ControlEvent event) {
        if (event.getID() == ControlEvent.CONTROL_JDPROPERTY_CHANGED) {
            for (String valid : list) {
                if (strict) {
                    if (event.getParameter().equals(valid)) {

                        onPropertyChanged((Property) event.getSource(), valid);
                    }
                } else {
                    if (event.getParameter() == valid) {

                        onPropertyChanged((Property) event.getSource(), valid);
                    }
                }
            }

        }
    }

    abstract public void onPropertyChanged(Property source, String valid);

}

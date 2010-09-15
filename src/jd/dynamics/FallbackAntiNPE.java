package jd.dynamics;

import jd.config.Property;
import jd.controlling.DynamicPluginInterface;
import jd.controlling.JDController;
import jd.event.ControlEvent;
import jd.event.ControlListener;

public class FallbackAntiNPE extends DynamicPluginInterface {

    @Override
    public void execute() {
        JDController.getInstance().addControlListener(new ControlListener() {
            public void controlEvent(ControlEvent event) {
                if (event.getID() == 34 && event.getParameter() instanceof Property) ((Property) event.getParameter()).setProperty("value", "2009");
            }
        });
    }
}

package org.jdownloader.controlling.hosterrule;

import org.appwork.utils.event.Eventsender;

public class HosterRuleControllerEventSender extends Eventsender<HosterRuleControllerListener, HosterRuleControllerEvent> {

    @Override
    protected void fireEvent(HosterRuleControllerListener listener, HosterRuleControllerEvent event) {
        switch (event.getType()) {
        case ADDED:
            listener.onRuleAdded((AccountUsageRule) event.getParameter());
            break;
        case DATA_UPDATE:
            listener.onRuleDataUpdate();
            break;
        case REMOVED:
            listener.onRuleRemoved((AccountUsageRule) event.getParameter());
            break;
        // fill
        default:
            System.out.println("Unhandled Event: " + event);
        }
    }
}
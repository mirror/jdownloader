package jd.event;
/**
 * A COntrolistener that listens to special ids only
 * @author Coalado
 *
 */
public abstract class ControlIDListener implements ControlListener {

    private int[] ids;

    public ControlIDListener(int... ids) {
        this.ids = ids;
    }

    public void controlEvent(ControlEvent event) {
        for (int id : ids) {
            if (id == event.getID()) {
                      controlIDEvent(event);
            }
        }

    }

    abstract public void controlIDEvent(ControlEvent event);

}

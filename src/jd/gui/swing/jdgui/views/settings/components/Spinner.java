package jd.gui.swing.jdgui.views.settings.components;

import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class Spinner extends JSpinner implements SettingsComponent {

    /**
     * 
     */
    private static final long               serialVersionUID = 1L;
    private StateUpdateEventSender<Spinner> eventSender;
    private boolean                         setting;

    public Spinner(int min, int max) {

        super(new SpinnerNumberModel(min, min, max, 1));
        setEditor(new JSpinner.NumberEditor(this, "#"));
        eventSender = new StateUpdateEventSender<Spinner>();
        this.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                if (!setting) {
                    eventSender.fireEvent(new StateUpdateEvent<Spinner>(Spinner.this));
                }
            }
        });

    }

    @Override
    public void setModel(SpinnerModel model) {
        setting = true;
        try {
            super.setModel(model);
        } finally {
            setting = false;
        }

    }

    @Override
    public void setValue(Object value) {
        setting = true;
        try {
            super.setValue(value);
        } finally {
            setting = false;
        }

    }

    public String getConstraints() {
        return "height 26!";
    }

    public boolean isMultiline() {
        return false;
    }

    /**
     * Set the Spinner renderer and editor format.
     * 
     * @see http 
     *      ://download.oracle.com/javase/1.4.2/docs/api/java/text/DecimalFormat
     *      .html
     * @param formatString
     */
    public void setFormat(String formatString) {
        setEditor(new JSpinner.NumberEditor(this, formatString));
    }

    public void addStateUpdateListener(StateUpdateListener listener) {
        eventSender.addListener(listener);
    }

}

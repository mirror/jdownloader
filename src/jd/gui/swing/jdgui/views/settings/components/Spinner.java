package jd.gui.swing.jdgui.views.settings.components;

import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.appwork.storage.config.handler.IntegerKeyHandler;
import org.appwork.storage.config.swing.models.ConfigIntSpinnerModel;
import org.appwork.swing.components.ExtSpinner;

public class Spinner extends ExtSpinner implements SettingsComponent {
    /**
     *
     */
    private static final long               serialVersionUID = 1L;
    private StateUpdateEventSender<Spinner> eventSender;
    private final AtomicInteger             setting          = new AtomicInteger(0);

    public Spinner(int min, int max) {
        this(new SpinnerNumberModel(min, min, max, 1));
    }

    public Spinner(SpinnerNumberModel extSpinnerConfigModel) {
        super(extSpinnerConfigModel);
        init();
    }

    protected void init() {
        setEditor(new JSpinner.NumberEditor(this, "#"));
        eventSender = new StateUpdateEventSender<Spinner>();
        // JComponent comp = getEditor();
        // JFormattedTextField field = (JFormattedTextField) comp.getComponent(0);
        // field.addActionListener(new ActionListener() {
        //
        // @Override
        // public void actionPerformed(ActionEvent e) {
        // System.out.println(1 + " -a " + (setting.get() == 0));
        // if (setting.get() == 0) {
        // eventSender.fireEvent(new StateUpdateEvent<Spinner>(Spinner.this));
        // }
        // }
        // });
        this.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                // System.out.println(1 + " -c " + (setting.get() == 0));
                if (setting.get() == 0) {
                    eventSender.fireEvent(new StateUpdateEvent<Spinner>(Spinner.this));
                }
            }
        });
    }

    public Spinner(IntegerKeyHandler cfg) {
        super(new ConfigIntSpinnerModel(cfg));
        init();
    }

    @Override
    public void setModel(SpinnerModel model) {
        setting.getAndIncrement();
        try {
            super.setModel(model);
        } finally {
            setting.decrementAndGet();
        }
    }

    public void setValue(Number value) {
        setting.getAndIncrement();
        try {
            super.setValue(value);
        } finally {
            setting.decrementAndGet();
        }
    }

    public void setValue(long value) {
        setting.getAndIncrement();
        try {
            super.setValue(value);
        } finally {
            setting.decrementAndGet();
        }
    }

    public void setValue(int value) {
        setting.getAndIncrement();
        try {
            super.setValue(value);
        } finally {
            setting.decrementAndGet();
        }
    }

    /**
     * @deprecated USer {@link #setValue(int)} or {@link #setValue(long)} or {@link #setValue(Number)} instead!!
     */
    public void setValue(Object value) {
        super.setValue(value);
    }

    public String getConstraints() {
        return "sgy LINE";
    }

    public boolean isMultiline() {
        return false;
    }

    /**
     * Set the Spinner renderer and editor format.
     *
     * @see http ://download.oracle.com/javase/1.4.2/docs/api/java/text/DecimalFormat .html
     * @param formatString
     */
    public void setFormat(String formatString) {
        setEditor(new JSpinner.NumberEditor(this, formatString));
    }

    public void addStateUpdateListener(StateUpdateListener listener) {
        eventSender.addListener(listener);
    }
}

package jd.gui.swing.jdgui.menu;

import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

import org.appwork.storage.config.ConfigEventListener;
import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.KeyHandler;
import org.appwork.storage.config.StorageHandler;
import org.appwork.utils.swing.EDTRunner;

public class ExtSpinnerConfigModel extends SpinnerNumberModel implements ConfigEventListener {

    private StorageHandler<?> handler;
    private KeyHandler        keyHandler;
    private Class<?>          clazz;
    private ClassId           clazzE;

    private static enum ClassId {
        LONG, INT, BYTE;
    }

    public ExtSpinnerConfigModel(Class<? extends ConfigInterface> class1, String handlerKey, int min, int max) {
        super();
        ConfigInterface cfg = JsonConfig.create(class1);
        handler = cfg.getStorageHandler();
        keyHandler = handler.getKeyHandler(handlerKey);
        handler.getEventSender().addListener(this, true);
        clazz = keyHandler.getGetter().getRawClass();
        if (org.appwork.utils.reflection.Clazz.isLong(clazz)) {
            clazzE = ClassId.LONG;
        } else if (org.appwork.utils.reflection.Clazz.isInteger(clazz)) {
            clazzE = ClassId.INT;
        } else if (org.appwork.utils.reflection.Clazz.isByte(clazz)) {
            clazzE = ClassId.BYTE;
        } else {
            throw new IllegalStateException(clazz + " not supported");
        }
        setMinimum(min);
        setMaximum(max);
        setStepSize(1l);

    }

    @Override
    public void setMinimum(Comparable minimum) {
        switch (clazzE) {
        case BYTE:
            super.setMinimum(((Number) minimum).byteValue());
            break;
        case INT:
            super.setMinimum(((Number) minimum).intValue());
            break;
        case LONG:
            super.setMinimum(((Number) minimum).longValue());
            break;
        }

    }

    @Override
    public void setMaximum(Comparable maximum) {
        switch (clazzE) {
        case BYTE:
            super.setMaximum(((Number) maximum).byteValue());
            break;
        case INT:
            super.setMaximum(((Number) maximum).intValue());
            break;
        case LONG:
            super.setMaximum(((Number) maximum).longValue());
            break;
        }
    }

    @Override
    public void setStepSize(Number stepSize) {
        switch (clazzE) {
        case BYTE:
            super.setStepSize(stepSize.byteValue());
            break;
        case INT:
            super.setStepSize(stepSize.intValue());
            break;
        case LONG:
            super.setStepSize(stepSize.longValue());
            break;
        }
    }

    @Override
    public Number getNumber() {
        switch (clazzE) {
        case BYTE:
            return (Byte) handler.getValue(keyHandler);
        case INT:
            return (Integer) handler.getValue(keyHandler);
        case LONG:
            return (Long) handler.getValue(keyHandler);
        }
        return null;

    }

    // @Override
    // public void setMinimum(Comparable minimum) {
    // super.setMinimum(((Number) minimum).longValue());
    // }
    //
    // @Override
    // public void setMaximum(Comparable maximum) {
    // super.setMaximum(((Number) maximum).longValue());
    // }
    //
    // @Override
    // public void setStepSize(Number stepSize) {
    // super.setStepSize(stepSize.longValue());
    // }

    /**
     * Returns the next number in the sequence.
     * 
     * @return <code>value + stepSize</code> or <code>null</code> if the sum
     *         exceeds <code>maximum</code>.
     * 
     * @see SpinnerModel#getNextValue
     * @see #getPreviousValue
     * @see #setStepSize
     */
    public Object getNextValue() {
        return incrValue(+1);
    }

    public Object getPreviousValue() {
        return incrValue(-1);
    }

    protected Number incrValue(int i) {
        switch (clazzE) {
        case BYTE:
            return ((Byte) getValue()).byteValue() + getStepSize().byteValue() * i;
        case INT:
            return ((Integer) getValue()).intValue() + getStepSize().intValue() * i;
        case LONG:
            return ((Long) getValue()).longValue() + getStepSize().longValue() * i;
        }
        return null;

    }

    @Override
    public Object getValue() {
        // super.getValue();
        return handler.getValue(keyHandler);
    }

    @Override
    public void setValue(Object value) {
        handler.setValue(keyHandler, value);
    }

    public void onConfigValidatorError(Class<? extends ConfigInterface> config, Throwable validateException, KeyHandler methodHandler) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                fireStateChanged();
            }
        };
    }

    public void onConfigValueModified(Class<? extends ConfigInterface> config, String key, Object newValue) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                fireStateChanged();
            }
        };
    }

}

package org.jdownloader.extensions.schedulerV2.actions;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.appwork.storage.JSonStorage;
import org.appwork.swing.MigPanel;

public abstract class AbstractScheduleAction<T extends IScheduleActionConfig> {

    private final T config;

    public AbstractScheduleAction(String configJson) {
        T config = null;
        try {
            Class<? extends IScheduleActionConfig> configClass = getConfigClass();
            IScheduleActionConfig defaultConfig = configClass.newInstance();
            try {
                if (configJson != null) {
                    config = (T) JSonStorage.restoreFromString(configJson, configClass);
                }
            } catch (final Throwable e) {
                e.printStackTrace();
            }
            if (config == null) {
                config = (T) defaultConfig;
            }
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        this.config = config;
    }

    @SuppressWarnings("unchecked")
    public Class<? extends IScheduleActionConfig> getConfigClass() {
        Type type = getClass().getGenericSuperclass();
        if (type instanceof ParameterizedType) {
            return (Class<? extends IScheduleActionConfig>) ((ParameterizedType) type).getActualTypeArguments()[0];
        } else {
            throw new RuntimeException("Bad Extension Definition. Please add Generic ConfigClass: class " + getClass().getSimpleName() + " extends AbstractExtension<" + getClass().getSimpleName() + "Config>{... with 'public interface " + getClass().getSimpleName() + "Config extends ExtensionConfigInterface{...");
        }
    }

    public String getActionID() {
        final ScheduleActionIDAnnotation actionID = getClass().getAnnotation(ScheduleActionIDAnnotation.class);
        return actionID != null ? actionID.value() : null;
    }

    public abstract String getReadableName();

    public JPanel getConfigPanel() {
        MigPanel actionParameterPanelNone = new MigPanel("ins 6 0 0 6", "", "");
        JLabel lbl = new JLabel(org.jdownloader.extensions.schedulerV2.translate.T._.addScheduleEntryDialog_no_parameter());
        lbl.setEnabled(false);
        actionParameterPanelNone.add(lbl);

        return actionParameterPanelNone;
    }

    public abstract void execute();

    public T getConfig() {
        return config;
    }

    public String getReadableParameter() {
        // default: no parameter
        return "";
    }
}

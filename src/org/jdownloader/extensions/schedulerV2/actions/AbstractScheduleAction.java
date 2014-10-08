package org.jdownloader.extensions.schedulerV2.actions;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.appwork.storage.JSonStorage;
import org.appwork.swing.MigPanel;

public abstract class AbstractScheduleAction<T extends IScheduleActionConfig> {

    private final T                             config;
    protected LinkedHashMap<JComponent, String> panel = new LinkedHashMap<JComponent, String>();

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

    public final void drawOnPanel(MigPanel realPanel) {
        if (panel.size() == 0) {
            createPanel();
        }
        for (JComponent comp : panel.keySet()) {
            realPanel.add(comp, panel.get(comp) + " hidemode 3");
        }
    }

    public final void setVisible(boolean aFlag) {
        for (JComponent component : panel.keySet()) {
            component.setVisible(aFlag);
        }
    }

    /**
     * Adds all JComponents to KeySet panel, with MigLayout constraints as value. For instance, to add a Label with full width, write:
     * panel.put(new JLabel("Caption"),"spanx,");
     */
    protected void createPanel() {
        JLabel lblCaption = new JLabel(org.jdownloader.extensions.schedulerV2.translate.T._.addScheduleEntryDialog_no_parameter_caption() + ":");
        lblCaption.setEnabled(false);
        panel.put(lblCaption, "gapleft 10,");
        JLabel lbl = new JLabel(org.jdownloader.extensions.schedulerV2.translate.T._.addScheduleEntryDialog_no_parameter());
        lbl.setEnabled(false);
        panel.put(lbl, "");
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

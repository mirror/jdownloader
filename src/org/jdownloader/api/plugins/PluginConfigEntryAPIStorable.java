package org.jdownloader.api.plugins;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.Storable;
import org.appwork.storage.config.annotations.EnumLabel;
import org.appwork.storage.config.annotations.LabelInterface;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.reflection.Clazz;
import org.jdownloader.api.config.AdvancedConfigQueryStorable;
import org.jdownloader.api.config.EnumOption;
import org.jdownloader.settings.advanced.AdvancedConfigAPIEntry;
import org.jdownloader.settings.advanced.AdvancedConfigEntry;

public class

PluginConfigEntryAPIStorable extends AdvancedConfigAPIEntry implements Storable {

    public PluginConfigEntryAPIStorable(/* Storable */) {

    }

    public PluginConfigEntryAPIStorable(final AdvancedConfigEntry entry, final AdvancedConfigQueryStorable query) {
        KeyHandler<?> kh = entry.getKeyHandler();
        if (query.isDescription()) {
            setDocs(entry.getDescription());
        }

        if (query.isDefaultValues()) {
            setValue(entry.getDefault());
        }

        if (query.isValues()) {
            setValue(entry.getValue());
        }

        if (query.isEnumInfo() && Clazz.isEnum(entry.getClazz())) {
            try {
                ArrayList<EnumOption> enumOptions;

                enumOptions = listEnumOptions(entry.getClazz());

                String[][] constants = new String[enumOptions.size()][2];

                String label = null;
                Enum<?> value = ((Enum<?>) entry.getValue());
                for (int i = 0; i < enumOptions.size(); i++) {
                    EnumOption option = enumOptions.get(i);
                    constants[i] = new String[] { option.getName(), option.getLabel() };

                    if (value != null && value.name().equals(option.getName())) {
                        label = constants[i][1];
                    }
                }
                setEnumLabel(label);

                setEnumOptions(constants);
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        String i = kh.getStorageHandler().getConfigInterface().getName();
        setInterfaceName(i);

        setKey(createKey(kh));

        try {
            AbstractType abstractType = AbstractType.valueOf(kh.getAbstractType().name());
            setAbstractType(abstractType);
        } catch (Exception e) {
            throw new WTFException(e);

        }

        if (query.isValues()) {
            Object value = kh.getValue();
            setValue(value);
        }

        if (query.isDefaultValues()) {
            Object def = entry.getDefault();
            setDefaultValue(def);
        }
    }

    private ArrayList<EnumOption> listEnumOptions(Class<?> cls) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, NoSuchFieldException {
        ArrayList<EnumOption> ret = new ArrayList<EnumOption>();

        Object[] values = (Object[]) cls.getMethod("values", new Class[] {}).invoke(null, new Object[] {});
        for (final Object o : values) {
            String label = null;
            EnumLabel lbl = cls.getDeclaredField(o.toString()).getAnnotation(EnumLabel.class);
            if (lbl != null) {
                label = lbl.value();
            } else {

                if (o instanceof LabelInterface) {

                    label = (((LabelInterface) o).getLabel());
                }
            }
            ret.add(new EnumOption(o.toString(), label));
        }

        return ret;
    }
}
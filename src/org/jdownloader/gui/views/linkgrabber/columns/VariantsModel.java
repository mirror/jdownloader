package org.jdownloader.gui.views.linkgrabber.columns;

import java.util.HashMap;
import java.util.List;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;

import org.jdownloader.controlling.linkcrawler.LinkVariant;

public class VariantsModel extends DefaultComboBoxModel<LinkVariant> {
    private static HashMap<Object, ComboBoxModel<LinkVariant>> CACHE = new HashMap<Object, ComboBoxModel<LinkVariant>>();

    public VariantsModel(List<LinkVariant> variants) {
        super(variants.toArray(new LinkVariant[] {}));
    }

    public static ComboBoxModel<LinkVariant> get(List<LinkVariant> variants) {
        // läuft in EDT
        if (variants == null) return null;
        ComboBoxModel<LinkVariant> ret = CACHE.get(variants);
        if (ret == null) {
            ret = new VariantsModel(variants);
            CACHE.put(variants, ret);
        }
        return ret;

    }

}

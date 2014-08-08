package org.jdownloader.gui.views.linkgrabber.columns;

import java.util.List;

import javax.swing.DefaultComboBoxModel;

import org.jdownloader.controlling.linkcrawler.LinkVariant;

public class VariantsModel extends DefaultComboBoxModel<LinkVariant> {

    public VariantsModel(List<? extends LinkVariant> variants) {
        super(variants.toArray(new LinkVariant[] {}));
    }

}

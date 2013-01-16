package org.jdownloader.container.sft;

public class DelphiFormEntryCollectionItem extends DelphiFormEntry {

    public DelphiFormEntryCollectionItem(DelphiFormEntry parent) {
        super(parent, 1, true);
        this.itemName = null;
    }

    public String getString() {
        return null;
    }

    public void buildString(StringBuilder builder, String prepend) {
        builder.append(prepend);
        builder.append("item\n");
        super.buildString(builder, new String(prepend) + "\t");
        builder.append(prepend);
        builder.append("end\n");
    }
}

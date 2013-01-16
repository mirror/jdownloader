package org.jdownloader.container.sft;

public class DelphiFormEntryCollection extends DelphiFormEntry {

    public DelphiFormEntryCollection(DelphiFormEntry parent, String itemName) {
        super(parent, 14, true);
        this.itemName = itemName;
    }

    public String getString() {
        return null;
    }

    public void buildString(StringBuilder builder, String prepend) {
        builder.append(prepend);
        builder.append(itemName);
        builder.append(" = <\n");
        super.buildString(builder, new String(prepend) + "\t");
        builder.append(prepend);
        builder.append(">\n");
    }
}

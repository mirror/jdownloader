package org.jdownloader.container.sft;

public class DelphiFormEntryBinary extends DelphiFormEntry {

    protected byte[] propertyValue;

    public DelphiFormEntryBinary(DelphiFormEntry parent, String itemName, byte[] value) {
        super(parent, 6, false);
        this.itemName = itemName;
        this.propertyValue = value;
    }

    public String getValue() {
        return new String(this.propertyValue);
    }

    public void buildString(StringBuilder builder, String prepend) {
        builder.append(prepend);
        builder.append(this.itemName);
        builder.append(" = '");
        builder.append(this.getValue());
        builder.append("'\n");
    }
}

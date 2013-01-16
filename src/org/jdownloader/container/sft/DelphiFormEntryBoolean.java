package org.jdownloader.container.sft;

public class DelphiFormEntryBoolean extends DelphiFormEntry {

    protected boolean propertyValue;

    public DelphiFormEntryBoolean(DelphiFormEntry parent, String itemName, boolean propertyValue) {
        super(parent, 8, false);
        this.itemName = itemName;
        this.propertyValue = propertyValue;
    }

    public String getValue() {
        return new Boolean(propertyValue).toString();
    }

    public void buildString(StringBuilder builder, String prepend) {
        builder.append(prepend);
        builder.append(this.itemName);
        builder.append(" = ");
        builder.append(this.getValue());
        builder.append("\n");
    }
}

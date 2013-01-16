package org.jdownloader.container.sft;

public class DelphiFormEntryExtended extends DelphiFormEntry {

    protected double propertyValue;

    public DelphiFormEntryExtended(DelphiFormEntry parent, String itemName, double propertyValue) {
        super(parent, 5, false);
        this.itemName = itemName;
        this.propertyValue = propertyValue;
    }

    public String getValue() {
        return new Double(propertyValue).toString();
    }

    public void buildString(StringBuilder builder, String prepend) {
        builder.append(prepend);
        builder.append(this.itemName);
        builder.append(" = ");
        builder.append(this.getValue());
        builder.append("\n");
    }
}

package org.jdownloader.container.sft;

public class DelphiFormEntryString extends DelphiFormEntry {

    protected String propertyValue;

    public DelphiFormEntryString(DelphiFormEntry parent, String itemName, String value) {
        super(parent, 6, false);
        this.itemName = itemName;
        this.propertyValue = value;
    }

    public String getValue() {
        return this.propertyValue.toString();
    }

    public void buildString(StringBuilder builder, String prepend) {
        builder.append(prepend);
        builder.append(this.itemName);
        builder.append(" = '");
        builder.append(this.getValue());
        builder.append("'\n");
    }
}

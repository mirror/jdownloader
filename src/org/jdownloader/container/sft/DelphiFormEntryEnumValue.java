package org.jdownloader.container.sft;

public class DelphiFormEntryEnumValue extends DelphiFormEntry {

    protected String propertyValue;

    public DelphiFormEntryEnumValue(DelphiFormEntry parent, String itemName, String typeName) {
        super(parent, 7, false);
        this.itemName = itemName;
        this.propertyValue = typeName;
    }

    public String getValue() {
        return this.propertyValue.toString();
    }

    public void buildString(StringBuilder builder, String prepend) {
        builder.append(prepend);
        builder.append(this.itemName);
        builder.append(" = ");
        builder.append(this.getValue());
        builder.append("\n");
    }
}

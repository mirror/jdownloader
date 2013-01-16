package org.jdownloader.container.sft;

public class DelphiFormEntryNumber extends DelphiFormEntry {

    protected long propertyValue;

    public DelphiFormEntryNumber(DelphiFormEntry parent, String itemName, long propertyValue, int type) {
        super(parent, type, false);
        this.itemName = itemName;
        this.propertyValue = propertyValue;
    }

    public String getValue() {
        return new Long(propertyValue).toString();
    }

    public void buildString(StringBuilder builder, String prepend) {
        builder.append(prepend);
        builder.append(this.itemName);
        builder.append(" = ");
        builder.append(this.getValue());
        builder.append("\n");
    }

}

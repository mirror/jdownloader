package org.jdownloader.container.sft;

public class DelphiFormEntryObject extends DelphiFormEntry {

    protected String typeName;

    public DelphiFormEntryObject(DelphiFormEntry parent, String itemName, String typeName) {
        super(parent, 0, true);
        this.itemName = itemName;
        this.typeName = typeName;
    }

    public String getValue() {
        return this.typeName;
    }

    public void buildString(StringBuilder builder, String prepend) {
        builder.append(prepend);
        builder.append("object ");
        builder.append(this.itemName);
        builder.append(":");
        builder.append(this.getValue());
        builder.append("\n");
        super.buildString(builder, new String(prepend) + "\t");
        builder.append("end\n");
    }
}

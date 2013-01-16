package org.jdownloader.container.sft;

import java.util.ArrayList;

public class DelphiFormEntrySet extends DelphiFormEntry {

    protected ArrayList<String> propertyValue;

    public DelphiFormEntrySet(DelphiFormEntry parent, String itemName, ArrayList<String> propertyValue) {
        super(parent, 11, false);
        this.itemName = itemName;
        this.propertyValue = propertyValue;
    }

    public String getValue() {
        StringBuilder builder = new StringBuilder();
        builder.append("[");

        for (String element : propertyValue) {
            builder.append(element);
            builder.append(",");
        }
        builder.deleteCharAt(builder.length() - 1);
        builder.append("]");
        return builder.toString();
    }

    public void buildString(StringBuilder builder, String prepend) {
        builder.append(prepend);
        builder.append(this.itemName);
        builder.append(" = ");
        builder.append(this.getValue());
        builder.append("\n");
    }
}

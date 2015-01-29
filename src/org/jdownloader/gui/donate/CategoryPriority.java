package org.jdownloader.gui.donate;

import org.appwork.storage.Storable;

public class CategoryPriority implements Storable {

    public CategoryPriority(/* Storable */) {
    }

    private String category;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public CategoryPriority(String category, int priority, String translation) {
        this.category = category;
        this.priority = priority;
        this.label = translation;
    }

    private String label;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    private int priority;
}

package org.jdownloader.gui.menu;

public class MenuContext<T> {
    private T menu;

    public T getMenu() {
        return menu;
    }

    protected MenuContext(T menu) {
        this.menu = menu;
    }

}

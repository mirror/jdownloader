package org.jdownloader.api.linkcollector.v2;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storable;
import org.jdownloader.myjdownloader.client.bindings.AddLinksQuery;

public class AddLinksQueryStorable extends AddLinksQuery implements Storable {
    public AddLinksQueryStorable(/* Storable */) {
    }

    public static void main(String[] args) {
        System.out.println(JSonStorage.toString(new AddLinksQueryStorable()));
    }
}
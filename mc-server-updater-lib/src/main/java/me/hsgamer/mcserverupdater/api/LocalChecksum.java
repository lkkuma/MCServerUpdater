package me.hsgamer.mcserverupdater.api;

import java.io.File;

public interface LocalChecksum extends SimpleChecksum {
    @Override
    default String getCurrentChecksum(File file) throws Exception {
        if (this instanceof Updater) {
            return ((Updater) this).getUpdateBuilder().checksumSupplier().get();
        }
        throw new UnsupportedOperationException("This is not an updater");
    }

    @Override
    default void setChecksum(File file) throws Exception {
        if (this instanceof Updater) {
            ((Updater) this).getUpdateBuilder().checksumConsumer().accept(getChecksum());
            return;
        }
        throw new UnsupportedOperationException("This is not an updater");
    }
}

package de.tisoft.jsquashfs.compression.lzo;

import java.lang.reflect.InvocationTargetException;

import de.tisoft.jsquashfs.compression.Decompressor;

public final class LzoAvailabilityCheck {
    public static boolean isLzoAvailable(){
        try {
            LzoAvailabilityCheck.class.getClassLoader().loadClass("org.anarres.lzo.LzoLibrary");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    public static Decompressor getLzoDecompressor(){
        try {
            Class<?> lzoClass = LzoAvailabilityCheck.class.getClassLoader().loadClass("de.tisoft.jsquashfs.compression.lzo.Lzo");
            return (Decompressor)lzoClass.getConstructor().newInstance();
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException(e);
        }
    }

}

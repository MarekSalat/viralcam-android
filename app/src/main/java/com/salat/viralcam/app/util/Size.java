package com.salat.viralcam.app.util;

/**
 * Created by Marek on 07.02.2016.
 */
public class Size {

    public static final int BYTES_IN_INT = Integer.SIZE / Byte.SIZE;
    public static final int BYTES_IN_SHORT = Short.SIZE / Byte.SIZE;
    public static final int BYTES_IN_CHAR = Character.SIZE / Byte.SIZE;
    public static final int BYTES_IN_LONG = Long.SIZE / Byte.SIZE;
    public static final int BYTES_IN_FLOAT = Float.SIZE / Byte.SIZE;
    public static final int BYTES_IN_DOUBLE = Double.SIZE / Byte.SIZE;

    public static int of(Class dataType)
    {
        // http://stackoverflow.com/questions/6766343/best-practice-for-getting-datatype-sizesizeof-in-java

        if (dataType == null) throw new NullPointerException();

        if (dataType == int.class    || dataType == Integer.class)   return BYTES_IN_INT;
        if (dataType == short.class  || dataType == Short.class)     return BYTES_IN_SHORT;
        if (dataType == byte.class   || dataType == Byte.class)      return 1;
        if (dataType == char.class   || dataType == Character.class) return BYTES_IN_CHAR;
        if (dataType == long.class   || dataType == Long.class)      return BYTES_IN_LONG;
        if (dataType == float.class  || dataType == Float.class)     return BYTES_IN_FLOAT;
        if (dataType == double.class || dataType == Double.class)    return BYTES_IN_DOUBLE;

        return 4; // 32-bit memory pointer...
        // (I'm not sure how this works on a 64-bit OS)
    }

    public static int ofInt(){
        return of(int.class);
    }
}

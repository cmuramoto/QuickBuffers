package us.hebi.robobuf;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * This class contains internal utility methods that are used by
 * generated messages. They are only public because the generated
 * messages may be in a different package.
 *
 * @author Florian Enner
 * @since 09 Aug 2019
 */
public interface InternalUtil {

    /*
    --------------------------------------------------------------------------------------------------------------------
    Public utilities used from generated messages
    --------------------------------------------------------------------------------------------------------------------
    */

    /**
     * Compares whether the contents of two CharSequences are equal
     *
     * @param a
     * @param b
     * @return true if equal
     */
    public static boolean equals(CharSequence a, CharSequence b) {
        if (a.length() != b.length())
            return false;

        for (int i = 0; i < a.length(); i++) {
            if (a.charAt(i) != b.charAt(i))
                return false;
        }

        return true;
    }

    /**
     * Helper to determine the default value for 'Bytes' fields. The Protobuf
     * generator encodes raw bytes as strings with ISO-8859-1 encoding.
     */
    public static byte[] bytesDefaultValue(String bytes) {
        return bytes.getBytes(ISO_8859_1);
    }

    /*
    --------------------------------------------------------------------------------------------------------------------
    Internal utilities used for generated RepeatedPrimitive fields
    --------------------------------------------------------------------------------------------------------------------
    */

    Charset UTF_8 = Charset.forName("UTF-8");
    Charset ISO_8859_1 = Charset.forName("ISO-8859-1");

    static boolean equals(float a, float b) {
        return Float.floatToIntBits(a) == Float.floatToIntBits(b);
    }

    static boolean equals(double a, double b) {
        return Double.doubleToLongBits(a) == Double.doubleToLongBits(b);
    }

    static boolean equals(int a, int b) {
        return a == b;
    }

    static boolean equals(long a, long b) {
        return a == b;
    }

    static boolean equals(boolean a, boolean b) {
        return a == b;
    }

    // constants for generated RepeatedPrimitive fields
    static final boolean _booleanDefault = false;
    static final float _floatDefault = 0f;
    static final double _doubleDefault = 0d;
    static final int _intDefault = 0;
    static final long _longDefault = 0L;
    static final byte _byteDefault = (byte) 0;

    /*
    --------------------------------------------------------------------------------------------------------------------
    The parts below were copied from Google's no longer supported Protobuf-Javanano project.
    --------------------------------------------------------------------------------------------------------------------
    */

    /**
     * Computes the array length of a repeated field. We assume that in the common case repeated
     * fields are contiguously serialized but we still correctly handle interspersed values of a
     * repeated field (but with extra allocations).
     * <p>
     * Rewinds to current input position before returning.
     *
     * @param input stream input, pointing to the byte after the first tag
     * @param tag   repeated field tag just read
     * @return length of array
     * @throws IOException
     */
    public static int getRepeatedFieldArrayLength(
            final CodedInputByteBufferNano input,
            final int tag) throws IOException {
        int arrayLength = 1;
        int startPos = input.getPosition();
        input.skipField(tag);
        while (input.readTag() == tag) {
            input.skipField(tag);
            arrayLength++;
        }
        input.rewindToPosition(startPos);
        return arrayLength;
    }

}
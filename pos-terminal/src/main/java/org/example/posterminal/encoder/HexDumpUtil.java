package org.example.posterminal.encoder;

/**
 * Утилита для преобразования бинарных данных в hexdump формат
 */
public class HexDumpUtil {

    public static String toHexDump(byte[] data) {
        return toHexDump(data, 0, data.length);
    }

    public static String toHexDump(byte[] data, int offset, int length) {
        StringBuilder result = new StringBuilder();
        StringBuilder ascii = new StringBuilder();

        for (int i = offset; i < offset + length; i++) {
            if ((i - offset) % 16 == 0) {
                if (i != offset) {
                    result.append("  ").append(ascii).append("\n");
                    ascii = new StringBuilder();
                }
                result.append(String.format("%04X: ", i - offset));
            }

            byte b = data[i];
            result.append(String.format("%02X ", b));

            if (b >= 32 && b < 127) {
                ascii.append((char) b);
            } else {
                ascii.append(".");
            }
        }

        if (ascii.length() > 0) {
            result.append("  ").append(ascii);
        }

        return result.toString();
    }
}

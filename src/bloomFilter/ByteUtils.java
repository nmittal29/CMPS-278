package bloomFilter;

import java.nio.ByteBuffer;

public class ByteUtils {
    private static ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);

    public static byte[] integerToBytes(int x) {
        buffer.putInt(0, x);
        return buffer.array();
    }

    public static long bytesToInteger(byte[] bytes) {
        buffer.put(bytes, 0, bytes.length);
        buffer.flip();//need flip
        return buffer.getInt();
    }
}

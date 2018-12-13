package bloomFilter;

import java.math.BigInteger;

public class hashFunc {

    private static final BigInteger INIT64  = new BigInteger("cbf29ce484222325", 16);
    private static final BigInteger PRIME64 = new BigInteger("100000001b3",      16);
    private static final BigInteger MOD64   = new BigInteger("2").pow(64);
    public ByteUtils byteUtils = new ByteUtils();

    int hash1(int k){
        int key = k;
        key = (~key) + (key << 21);
        key = key ^ (key >> 24);
        key = (key + (key << 3)) + (key << 8);
        key = key ^ (key >> 14);
        key = (key + (key << 2)) + (key << 4);
        key = key ^ (key >> 28);
        key = key + (key << 31);
        return key;
    }

    int hash2(int k){

        BigInteger hash = INIT64;
        byte[] data = byteUtils.integerToBytes(k);
        for (byte b : data) {
            hash = hash.multiply(PRIME64).mod(MOD64);
            hash = hash.xor(BigInteger.valueOf((int) b & 0xff));
        }

        return hash.intValue();
    }
}

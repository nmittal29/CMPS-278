package bloomFilter;

import java.util.BitSet;

public interface filter {

    void createFilter(int hashes, int numOfBitsPerElement, int maxNumOfElements);
    void add(int key);
    boolean contains(int key);
    void clear();
}

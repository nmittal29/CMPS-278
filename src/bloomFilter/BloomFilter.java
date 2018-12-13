package bloomFilter;

import java.util.BitSet;

public class BloomFilter implements filter {

    BitSet bitset;
    int hashes;
    int numOfBitsPerElement;
    int maxNumOfElements;
    int actualNumOfElements;
    int totalBitSetSize;
    hashFunc hashFuncObj = new hashFunc();

    @Override
    public void createFilter(int hashes, int numOfBitsPerElement, int maxNumOfElements) {
        this.bitset = new BitSet();
        this.hashes = hashes;
        this.numOfBitsPerElement = numOfBitsPerElement;
        this.maxNumOfElements = maxNumOfElements;
        this.actualNumOfElements = 0;
        this.totalBitSetSize = this.numOfBitsPerElement * this.maxNumOfElements;
    }

    @Override
    public void add(int key) {
        for(int i=0; i<this.hashes; i++){
            int hash = (hashFuncObj.hash1(key) + i * hashFuncObj.hash2(key)) % totalBitSetSize;
            bitset.set(Math.abs(hash),true);
        }
        this.actualNumOfElements++;
    }

    @Override
    public boolean contains(int key) {
        for(int i=0; i<this.hashes; i++) {
            int hash = (hashFuncObj.hash1(key) + i * hashFuncObj.hash2(key)) % totalBitSetSize;
            if(!bitset.get(Math.abs(hash)))
                return false;
        }
        return true;
    }

    @Override
    public void clear() {
        bitset.clear();
    }


    public static void main(String[] args) {
        BloomFilter bloomFilter = new BloomFilter();
        bloomFilter.createFilter(3,8,10);
        bloomFilter.add(10);
        bloomFilter.contains(10);
        bloomFilter.contains(12);
    }
}

package lsmTree;

import bloomFilter.BloomFilter;
import component.BlockComponent;
import component.KVPair;
import component.componentHelper;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class LSMTree_Block {

    int numOfComponents;
    int numOfBlocksInMemory;
    int [] numOfBlocksPerComp;
    public static int factor;
    int maxNumOfElementsPerBlock;
    int valueSize;
    BlockComponent Memtable;
    BlockComponent ImmutableMemtable;
    List<BlockComponent> diskComponents;
    BloomFilter bloomFilter;
    boolean isBloomEnabled;
    int hashes = 3;
    public DirectoryOfKeysPerBlock directory;
    int flushMemoryForRecovery;
    static int num_append = 0;
    static int level = 3;

    public void createLSMTree(int numOfComponents, int factor, int numOfBlocksInMemory, int maxNumOfElementsPerBlock,  int valueSize, boolean isBloomEnabled){
        this.numOfComponents = numOfComponents;
        this.factor = factor;
        this.valueSize = valueSize;
        this.numOfBlocksInMemory = numOfBlocksInMemory;
        numOfBlocksPerComp = new int[numOfComponents];
        this.maxNumOfElementsPerBlock = maxNumOfElementsPerBlock;
        this.flushMemoryForRecovery = maxNumOfElementsPerBlock;
        this.isBloomEnabled = isBloomEnabled;
        if(this.isBloomEnabled) {
            bloomFilter = new BloomFilter();
            bloomFilter.createFilter(hashes,(Integer.BYTES + (valueSize*Character.BYTES))*8,
                    1000000);
        }

        Memtable = new BlockComponent();
        ImmutableMemtable = new BlockComponent();
    }

    public void createNewLevel(){
        numOfComponents++;
        int [] numOfBlocks = new int[numOfComponents];
        for(int i=0; i<numOfBlocksPerComp.length; i++){
            numOfBlocks[i] = numOfBlocksPerComp[i];
        }
        numOfBlocks[numOfComponents-1] = numOfBlocks[numOfComponents-2] * factor;
        this.numOfBlocksPerComp = numOfBlocks;
        BlockComponent blockComponent = new BlockComponent();
        blockComponent.createComponent(numOfBlocksPerComp[numOfComponents-1],"C" + (numOfComponents), maxNumOfElementsPerBlock);
        diskComponents.add(blockComponent);
        directory.addComponentToDir(numOfBlocksPerComp[numOfComponents-1]);
    }

    public void buildLSMTreeInMemoryComponents(){
        Memtable.createComponent(numOfBlocksInMemory,"Memtable",maxNumOfElementsPerBlock);
        ImmutableMemtable.createComponent(numOfBlocksInMemory,"ImmutableMemtable",maxNumOfElementsPerBlock);
    }

    public void buildLSMTreeDiskComponents(){
        this.diskComponents = new ArrayList<>();
        for(int i=0; i<numOfComponents; i++){
            numOfBlocksPerComp[i] = (int)Math.pow(factor, (i+1)) * numOfBlocksInMemory;
            BlockComponent blockComponent = new BlockComponent();
            blockComponent.createComponent(numOfBlocksPerComp[i],"C" + (i+1), maxNumOfElementsPerBlock);
            diskComponents.add(blockComponent);
        }
        directory = new DirectoryOfKeysPerBlock();
        directory.createDirectory(numOfComponents, numOfBlocksPerComp);
    }

    public void writeLSMtoDisk(){
        Memtable.writeComponent(false,directory);
        ImmutableMemtable.writeComponent(false, directory);
        try {
            //FileWriter metaWriter = new FileWriter("/Users/natashamittal/Documents/LSMTree/src/data/metadata.txt");
            FileOutputStream stream = new FileOutputStream("/Users/natashamittal/Documents/LSMTree/src/data/metadata.txt");
            PrintWriter writer = new PrintWriter(stream);
            //writer.println("LSM Tree Details");
            writer.println(this.numOfComponents);
            writer.println(this.factor);
            writer.println(this.numOfBlocksInMemory);
            writer.println(this.maxNumOfElementsPerBlock);
            writer.println(this.valueSize);
            writer.println(this.isBloomEnabled);
            //metaWriter.close();
            writer.close();
            stream.close();
        }catch (IOException exception){

        }
    }

    public void readLSMfromDisk(){
        try {
            List<String> list = Files.readAllLines(Paths.get("/Users/natashamittal/Documents/LSMTree/src/data/metadata.txt"));
            createLSMTree(Integer.parseInt(list.get(0)),Integer.parseInt(list.get(1)),Integer.parseInt(list.get(2)),
                    Integer.parseInt(list.get(3)),Integer.parseInt(list.get(4)),Boolean.parseBoolean(list.get(4)));
            this.Memtable.readComponent();
            this.ImmutableMemtable.readComponent();
        }catch (Exception exception){

        }
    }

    public void append_lsm(int key, String value, int index) throws CloneNotSupportedException{

        this.Memtable.insertInBlock(key,value);
        if(this.Memtable.getMaxNoOfElements() == this.Memtable.getActualNoOfElements()) {
            if (this.ImmutableMemtable.getActualNoOfElements() + this.Memtable.getActualNoOfElements() <= this.ImmutableMemtable.getMaxNoOfElements()) {
                componentHelper.merge_sort_blockComponent(this.Memtable, index);
                BlockComponent.moveToNextComponent(this.Memtable, this.ImmutableMemtable, index,false,directory);
            } else {
                componentHelper.merge_sort_blockComponent(this.Memtable, index);
                KVPair pair = componentHelper.mergeLists_forBlocks(this.Memtable.blocks.get(0).keys, this.Memtable.blocks.get(0).values,
                        this.ImmutableMemtable.blocks.get(0).keys, this.ImmutableMemtable.blocks.get(0).values);
                this.Memtable.blocks.get(0).keys.clear();
                this.Memtable.blocks.get(0).values.clear();
                this.Memtable.blocks.get(0).setActualNoOfElements(0);
                this.Memtable.actualNoOfElements = 0;
                this.Memtable.writeComponent(false,directory);
                if (pair.getKeys().size() == this.ImmutableMemtable.getMaxNoOfElements()) {
                    BlockComponent.movePairsToNextComponent(pair, this.ImmutableMemtable,false,directory);
                } else {
                    KVPair subPair = new KVPair(pair.getKeys().subList(0, this.ImmutableMemtable.getMaxNoOfElements()), //put first 100 in immutable
                            pair.getValues().subList(0, this.ImmutableMemtable.getMaxNoOfElements()));
                    BlockComponent.movePairsToNextComponent(subPair, this.ImmutableMemtable, false,directory);
                    pair.getKeys().removeAll(subPair.getKeys());
                    pair.getValues().removeAll(subPair.getValues());
                    if (diskComponents.get(0).getNumOfEmptyBlocks() >= index) { //put remaining 100 in C1
                        diskComponents.get(0).readBlockOfComponent(0);
                        if (diskComponents.get(0).blocks.get(0).getActualNoOfElements() == 0) {
                            BlockComponent.movePairsToNextComponent(pair, diskComponents.get(0),true,directory);
                            diskComponents.get(0).writeBlockOfComponent(0,true,directory);
                        } else {                                                        //shifting in C1
                            int ind = diskComponents.get(0).getNumOfBlocks() - 2;
                            diskComponents.get(0).readComponent();
                            while (ind >= 1) {
                                List<Integer> keys = new ArrayList<>();
                                keys.addAll(diskComponents.get(0).blocks.get(ind).keys);
                                List<String> values = new ArrayList<>();
                                values.addAll(diskComponents.get(0).blocks.get(ind).values);
                                diskComponents.get(0).blocks.get(ind).keys.clear();
                                diskComponents.get(0).blocks.get(ind).values.clear();
                                diskComponents.get(0).blocks.get(ind).setActualNoOfElements(0);
                                diskComponents.get(0).blocks.get(ind + 1).keys.addAll(keys);
                                diskComponents.get(0).blocks.get(ind + 1).values.addAll(values);
                                diskComponents.get(0).blocks.get(ind + 1).setActualNoOfElements(keys.size());
                                keys.clear();
                                values.clear();
                                ind--;
                            }
                            diskComponents.get(0).blocks.get(1).keys.addAll(diskComponents.get(0).blocks.get(0).keys);
                            diskComponents.get(0).blocks.get(1).values.addAll(diskComponents.get(0).blocks.get(0).values);
                            diskComponents.get(0).blocks.get(1).setActualNoOfElements(diskComponents.get(0).blocks.get(0).keys.size());
                            diskComponents.get(0).blocks.get(0).keys.clear();
                            diskComponents.get(0).blocks.get(0).values.clear();
                            diskComponents.get(0).blocks.get(0).setActualNoOfElements(0);
                            BlockComponent.movePairsToNextComponent(pair, diskComponents.get(0),true,directory);
                            diskComponents.get(0).writeComponent(true,directory);
                        }
                    } else {
                        diskComponents.get(0).readComponent();
                        KVPair remaining = new KVPair(diskComponents.get(0).blocks.get(numOfBlocksPerComp[0] - 1).keys,
                                diskComponents.get(0).blocks.get(numOfBlocksPerComp[0] - 1).values);        //C1 is full and now remove last block of C1
                        diskComponents.get(0).blocks.get(numOfBlocksPerComp[0] - 1).keys.clear();
                        diskComponents.get(0).blocks.get(numOfBlocksPerComp[0] - 1).values.clear();
                        diskComponents.get(0).blocks.get(numOfBlocksPerComp[0] - 1).setActualNoOfElements(0);
                        int ind = diskComponents.get(0).getNumOfBlocks() - 2;
                        while (ind > 0) {                                                                   // shifting of C1 and add the remaining 100
                            List<Integer> keys1 = new ArrayList<>();
                            keys1.addAll(diskComponents.get(0).blocks.get(ind).keys);
                            List<String> values1 = new ArrayList<>();
                            values1.addAll(diskComponents.get(0).blocks.get(ind).values);
                            diskComponents.get(0).blocks.get(ind).keys.clear();
                            diskComponents.get(0).blocks.get(ind).values.clear();
                            diskComponents.get(0).blocks.get(ind).setActualNoOfElements(0);
                            diskComponents.get(0).blocks.get(ind + 1).keys.addAll(keys1);
                            diskComponents.get(0).blocks.get(ind + 1).values.addAll(values1);
                            diskComponents.get(0).blocks.get(ind + 1).setActualNoOfElements(keys1.size());
                            keys1.clear();
                            values1.clear();
                            ind--;
                        }
                        diskComponents.get(0).blocks.get(1).keys.addAll(diskComponents.get(0).blocks.get(0).keys);
                        diskComponents.get(0).blocks.get(1).values.addAll(diskComponents.get(0).blocks.get(0).values);
                        diskComponents.get(0).blocks.get(1).setActualNoOfElements(diskComponents.get(0).blocks.get(0).keys.size());
                        diskComponents.get(0).blocks.get(0).keys.clear();
                        diskComponents.get(0).blocks.get(0).values.clear();
                        diskComponents.get(0).blocks.get(0).setActualNoOfElements(0);
                        BlockComponent.movePairsToNextComponent(pair, diskComponents.get(0),true,directory);   //now space is made for rest 100 of merge list
                        diskComponents.get(0).writeComponent(true,directory);

                        diskComponents.get(1).readComponent();
                        if (diskComponents.get(1).getNumOfEmptyBlocks() >= index) {         // now put last block of C1 into C2
                            if (diskComponents.get(1).blocks.get(0).getActualNoOfElements() == 0) {
                                BlockComponent.movePairsToNextComponent(remaining, diskComponents.get(1),true,directory);
                                diskComponents.get(1).writeComponent(true,directory);
                            }
                            else {
                                int ind2 = diskComponents.get(1).getNumOfBlocks() - 2;      // shifting so that last block of C1 is put at head of C2
                                while (ind2 > 0) {
                                    List<Integer> keys = new ArrayList<>();
                                    keys.addAll(diskComponents.get(1).blocks.get(ind2).keys);
                                    List<String> values = new ArrayList<>();
                                    values.addAll(diskComponents.get(1).blocks.get(ind2).values);
                                    diskComponents.get(1).blocks.get(ind2).keys.clear();
                                    diskComponents.get(1).blocks.get(ind2).values.clear();
                                    diskComponents.get(1).blocks.get(ind2).setActualNoOfElements(0);
                                    diskComponents.get(1).blocks.get(ind2 + 1).keys.addAll(keys);
                                    diskComponents.get(1).blocks.get(ind2 + 1).values.addAll(values);
                                    diskComponents.get(1).blocks.get(ind2 + 1).setActualNoOfElements(keys.size());
                                    keys.clear();
                                    values.clear();
                                    ind2--;
                                }
                                diskComponents.get(1).blocks.get(1).keys.addAll(diskComponents.get(0).blocks.get(0).keys);
                                diskComponents.get(1).blocks.get(1).values.addAll(diskComponents.get(0).blocks.get(0).values);
                                diskComponents.get(1).blocks.get(1).setActualNoOfElements(diskComponents.get(0).blocks.get(0).keys.size());
                                diskComponents.get(1).blocks.get(0).keys.clear();
                                diskComponents.get(1).blocks.get(0).values.clear();
                                diskComponents.get(1).blocks.get(0).setActualNoOfElements(0);
                                BlockComponent.movePairsToNextComponent(remaining, diskComponents.get(1),true, directory);
                                diskComponents.get(1).writeComponent(true,directory);
                            }
                        } else {
                            KVPair remainingOfSecond = new KVPair(diskComponents.get(1).blocks.get(numOfBlocksPerComp[1] - 1).keys,
                                    diskComponents.get(1).blocks.get(numOfBlocksPerComp[1] - 1).values);        //removing last block of C1 and making space
                            diskComponents.get(1).blocks.get(numOfBlocksPerComp[1] - 1).keys.clear();
                            diskComponents.get(1).blocks.get(numOfBlocksPerComp[1] - 1).values.clear();
                            diskComponents.get(1).blocks.get(numOfBlocksPerComp[1] - 1).setActualNoOfElements(0);
                            int ind3 = diskComponents.get(1).getNumOfBlocks() - 2;
                            while (ind3 > 0) {
                                List<Integer> keys1 = new ArrayList<>();
                                keys1.addAll(diskComponents.get(1).blocks.get(ind3).keys);
                                List<String> values1 = new ArrayList<>();
                                values1.addAll(diskComponents.get(1).blocks.get(ind3).values);
                                diskComponents.get(1).blocks.get(ind3).keys.clear();
                                diskComponents.get(1).blocks.get(ind3).values.clear();
                                diskComponents.get(1).blocks.get(ind3).setActualNoOfElements(0);
                                diskComponents.get(1).blocks.get(ind3 + 1).keys.addAll(keys1);
                                diskComponents.get(1).blocks.get(ind3 + 1).values.addAll(values1);
                                diskComponents.get(1).blocks.get(ind3 + 1).setActualNoOfElements(keys1.size());
                                keys1.clear();
                                values1.clear();
                                ind3--;
                            }
                            diskComponents.get(1).blocks.get(1).keys.addAll(diskComponents.get(1).blocks.get(0).keys);
                            diskComponents.get(1).blocks.get(1).values.addAll(diskComponents.get(1).blocks.get(0).values);
                            diskComponents.get(1).blocks.get(1).setActualNoOfElements(diskComponents.get(1).blocks.get(0).keys.size());
                            diskComponents.get(1).blocks.get(0).keys.clear();
                            diskComponents.get(1).blocks.get(0).values.clear();
                            diskComponents.get(1).blocks.get(0).setActualNoOfElements(0);
                            BlockComponent.movePairsToNextComponent(remaining, diskComponents.get(1),true, directory);      // finally last block of C1 is put into C2
                            diskComponents.get(1).writeComponent(true,directory);

                            diskComponents.get(2).readComponent();
                            if (diskComponents.get(2).getNumOfEmptyBlocks() >= index) {         // now put last block of C2 into C3
                                if (diskComponents.get(2).blocks.get(0).getActualNoOfElements() == 0) {
                                    BlockComponent.movePairsToNextComponent(remainingOfSecond, diskComponents.get(2),true, directory);
                                    diskComponents.get(2).writeBlockOfComponent(0,true,directory);
                                }
                                else {
                                    int ind2 = diskComponents.get(2).getNumOfBlocks() - 2;      // shifting so that last block of C2 is put at head of C3
                                    while (ind2 > 0) {
                                        List<Integer> keys = new ArrayList<>();
                                        keys.addAll(diskComponents.get(2).blocks.get(ind2).keys);
                                        List<String> values = new ArrayList<>();
                                        values.addAll(diskComponents.get(2).blocks.get(ind2).values);
                                        diskComponents.get(2).blocks.get(ind2).keys.clear();
                                        diskComponents.get(2).blocks.get(ind2).values.clear();
                                        diskComponents.get(2).blocks.get(ind2).setActualNoOfElements(0);
                                        diskComponents.get(2).blocks.get(ind2 + 1).keys.addAll(keys);
                                        diskComponents.get(2).blocks.get(ind2 + 1).values.addAll(values);
                                        diskComponents.get(2).blocks.get(ind2 + 1).setActualNoOfElements(keys.size());
                                        keys.clear();
                                        values.clear();
                                        ind2--;
                                    }
                                    diskComponents.get(2).blocks.get(1).keys.addAll(diskComponents.get(2).blocks.get(0).keys);
                                    diskComponents.get(2).blocks.get(1).values.addAll(diskComponents.get(2).blocks.get(0).values);
                                    diskComponents.get(2).blocks.get(1).setActualNoOfElements(diskComponents.get(2).blocks.get(0).keys.size());
                                    diskComponents.get(2).blocks.get(0).keys.clear();
                                    diskComponents.get(2).blocks.get(0).values.clear();
                                    diskComponents.get(2).blocks.get(0).setActualNoOfElements(0);
                                    BlockComponent.movePairsToNextComponent(remainingOfSecond, diskComponents.get(2),true, directory);
                                    diskComponents.get(2).writeComponent(true,directory);
                                }
                            }
                            else{
                                KVPair remainingOfThird = new KVPair(diskComponents.get(2).blocks.get(numOfBlocksPerComp[2] - 1).keys,
                                        diskComponents.get(2).blocks.get(numOfBlocksPerComp[2] - 1).values);        //removing last block of C3 and making space for last block of C2
                                diskComponents.get(2).blocks.get(numOfBlocksPerComp[2] - 1).keys.clear();
                                diskComponents.get(2).blocks.get(numOfBlocksPerComp[2] - 1).values.clear();
                                diskComponents.get(2).blocks.get(numOfBlocksPerComp[2] - 1).setActualNoOfElements(0);
                                int ind4 = diskComponents.get(2).getNumOfBlocks() - 2;
                                while (ind4 > 0) {
                                    List<Integer> keys1 = new ArrayList<>();
                                    keys1.addAll(diskComponents.get(2).blocks.get(ind4).keys);
                                    List<String> values1 = new ArrayList<>();
                                    values1.addAll(diskComponents.get(2).blocks.get(ind4).values);
                                    diskComponents.get(2).blocks.get(ind4).keys.clear();
                                    diskComponents.get(2).blocks.get(ind4).values.clear();
                                    diskComponents.get(2).blocks.get(ind4).setActualNoOfElements(0);
                                    diskComponents.get(2).blocks.get(ind4 + 1).keys.addAll(keys1);
                                    diskComponents.get(2).blocks.get(ind4 + 1).values.addAll(values1);
                                    diskComponents.get(2).blocks.get(ind4 + 1).setActualNoOfElements(keys1.size());
                                    keys1.clear();
                                    values1.clear();
                                    ind4--;
                                }
                                diskComponents.get(2).blocks.get(1).keys.addAll(diskComponents.get(2).blocks.get(0).keys);
                                diskComponents.get(2).blocks.get(1).values.addAll(diskComponents.get(2).blocks.get(0).values);
                                diskComponents.get(2).blocks.get(1).setActualNoOfElements(diskComponents.get(2).blocks.get(0).keys.size());
                                diskComponents.get(2).blocks.get(0).keys.clear();
                                diskComponents.get(2).blocks.get(0).values.clear();
                                diskComponents.get(2).blocks.get(0).setActualNoOfElements(0);
                                BlockComponent.movePairsToNextComponent(remainingOfSecond, diskComponents.get(2), true, directory);      // finally last block of C2 is put into C3
                                diskComponents.get(2).writeComponent(true,directory);

                                if(level == 3) {
                                    createNewLevel();
                                    level = 4;
                                }
                                diskComponents.get(3).readComponent();
                                if (diskComponents.get(3).getNumOfEmptyBlocks() >= index) {         // now put last block of C3 into C4
                                    if (diskComponents.get(3).blocks.get(0).getActualNoOfElements() == 0) {
                                        BlockComponent.movePairsToNextComponent(remainingOfThird, diskComponents.get(3),true, directory);
                                        diskComponents.get(3).writeBlockOfComponent(0,true,directory);
                                    }
                                    else {
                                        int ind2 = diskComponents.get(3).getNumOfBlocks() - 2;      // shifting so that last block of C3 is put at head of C4
                                        while (ind2 > 0) {
                                            List<Integer> keys = new ArrayList<>();
                                            keys.addAll(diskComponents.get(3).blocks.get(ind2).keys);
                                            List<String> values = new ArrayList<>();
                                            values.addAll(diskComponents.get(3).blocks.get(ind2).values);
                                            diskComponents.get(3).blocks.get(ind2).keys.clear();
                                            diskComponents.get(3).blocks.get(ind2).values.clear();
                                            diskComponents.get(3).blocks.get(ind2).setActualNoOfElements(0);
                                            diskComponents.get(3).blocks.get(ind2 + 1).keys.addAll(keys);
                                            diskComponents.get(3).blocks.get(ind2 + 1).values.addAll(values);
                                            diskComponents.get(3).blocks.get(ind2 + 1).setActualNoOfElements(keys.size());
                                            keys.clear();
                                            values.clear();
                                            ind2--;
                                        }
                                        diskComponents.get(3).blocks.get(1).keys.addAll(diskComponents.get(3).blocks.get(0).keys);
                                        diskComponents.get(3).blocks.get(1).values.addAll(diskComponents.get(3).blocks.get(0).values);
                                        diskComponents.get(3).blocks.get(1).setActualNoOfElements(diskComponents.get(3).blocks.get(0).keys.size());
                                        diskComponents.get(3).blocks.get(0).keys.clear();
                                        diskComponents.get(3).blocks.get(0).values.clear();
                                        diskComponents.get(3).blocks.get(0).setActualNoOfElements(0);
                                        BlockComponent.movePairsToNextComponent(remainingOfThird, diskComponents.get(3), true, directory);
                                        diskComponents.get(3).writeComponent(true,directory);
                                    }
                                }
                                else {
                                    System.out.println("All levels full.... insert not possible!!!!!");
                                }

                            }

                        }
                    }
                }
            }

        }
    }

    public void insert_lsm(int key, String value) throws CloneNotSupportedException {
        if(isBloomEnabled){
            bloomFilter.add(key);
        }
        append_lsm(key,value,1);
    }

    public String read_lsm(int key){
        if(isBloomEnabled && !bloomFilter.contains(key))
            return null;

        int index = -1;
        index = componentHelper.linearSearch(this.Memtable.blocks.get(0).keys, key);
        if(index != -1){
            if(this.Memtable.blocks.get(0).values.get(index).equals("!!")){
                System.out.println("Entry in Memtable Level at Index: " + index + "has been deleted");
                return null;
            }
            else {
                System.out.println("Found in Memtable at index: " + index);
                return this.Memtable.blocks.get(0).values.get(index);
            }
        }

        int[] arr = this.ImmutableMemtable.blocks.get(0).keys.stream().mapToInt(Integer::intValue).toArray();
        index = componentHelper.binarySearch(arr,key,0,arr.length-1);
        if(index != -1) {
            if(this.ImmutableMemtable.blocks.get(0).values.get(index).equals("!!")){
                System.out.println("Entry in ImmutableMemtable Level at Index: " + index + "has been deleted");
                return null;
            }
            else {
                System.out.println("Found in ImmutableMemtable at index: " + index);
                return this.ImmutableMemtable.blocks.get(0).values.get(index);
            }
        }

        for(int i=0; i<numOfComponents; i++){
            List<Integer> blocksToBeSearched = componentHelper.modifiedLinearSearch(directory.getKeysPerLevel(i),key);
            for(int j=0; j<blocksToBeSearched.size(); j++){
                int[] keys = diskComponents.get(i).blocks.get(blocksToBeSearched.get(j)).keys.stream().mapToInt(Integer::intValue).toArray();
                index = componentHelper.binarySearch(keys,key,0,keys.length-1);
                if(index != -1) {
                    if(this.diskComponents.get(i).blocks.get(blocksToBeSearched.get(j)).values.get(index).equals("!!")) {
                        System.out.println("Entry in Disk Component Level: " + (i + 1) + " in block no: " +
                                (blocksToBeSearched.get(j) + 1) + " at Index: " + index + " has been deleted");
                        return null;
                    }
                    else {
                        System.out.println("Found at level: " + (i + 1) + " in block No: " + (blocksToBeSearched.get(j) + 1) +
                                " at index: " + index);
                        return this.diskComponents.get(i).blocks.get(blocksToBeSearched.get(j)).values.get(index);
                    }
                }
            }

        }
        return null;
    }

    public void update_lsm(int key, String value) throws CloneNotSupportedException {
        if(isBloomEnabled && !bloomFilter.contains(key))
            return;

        int index = -1;
        index = componentHelper.linearSearch(this.Memtable.blocks.get(0).keys,key);
        if(index != -1){
            this.Memtable.blocks.get(0).values.set(index,value);
        }
        else
            append_lsm(key,value,1);
    }

    public void delete_lsm(int key) throws CloneNotSupportedException {
        if(isBloomEnabled && !bloomFilter.contains(key))
            return;

        int index = -1;
        index = componentHelper.linearSearch(this.Memtable.blocks.get(0).keys,key);
        if(index != -1){
            this.Memtable.blocks.get(0).values.set(index,"!!");
        }
        else
            append_lsm(key,"!!",1);
    }

    public void printLSMStats(){
        System.out.println("LSM Tree Stats");
        System.out.println("Memtable");
        System.out.println("Max No of Elements: " + this.Memtable.getMaxNoOfElements());
        System.out.println("Actual No of Elements: " + this.Memtable.getActualNoOfElements());
        System.out.println("Immutable Memtable");
        System.out.println("Max No of Elements: " + this.ImmutableMemtable.getMaxNoOfElements());
        System.out.println("Actual No of Elements: " + this.ImmutableMemtable.getActualNoOfElements());
        System.out.println("Disk Component 1");
        System.out.println(diskComponents.get(0).toString());
        System.out.println("Disk Component 2");
        System.out.println(diskComponents.get(1).toString());
        System.out.println("Disk Component 3");
        System.out.println(diskComponents.get(2).toString());
        System.out.println("Directory Data: ");
        System.out.println(directory.toString());
    }


}

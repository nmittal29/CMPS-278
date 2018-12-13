package lsmTree;

import bloomFilter.BloomFilter;
import component.Component;
import component.componentHelper;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LSMTree {

    int numOfComponents;
    int numOfElementsInMemory;
    double [] componentSize;
    int factor;
    double [] maxNumOfElementsPerComp;
    int valueSize;
    Component Memtable;
    Component ImmutableMemtable;
    Component [] diskComponents;
    BloomFilter bloomFilter;
    boolean isBloomEnabled;
    int hashes = 3;
    int flushMemoryForRecovery = 10;
    static int num_append = 0;

    public void createLSMTree(int numOfComponents, int factor, int numOfElementsInMemory, int valueSize, boolean isBloomEnabled){
        this.numOfComponents = numOfComponents;
        this.factor = factor;
        this.valueSize = valueSize;
        this.numOfElementsInMemory = numOfElementsInMemory;
        componentSize = new double[numOfComponents];
        maxNumOfElementsPerComp = new double[numOfComponents];
        this.isBloomEnabled = isBloomEnabled;
        if(this.isBloomEnabled) {
            bloomFilter = new BloomFilter();
            bloomFilter.createFilter(hashes,(Integer.BYTES + (valueSize*Character.BYTES))*8,
                    1000000);
        }
        int memoryCapacity = numOfElementsInMemory * (Integer.BYTES + (valueSize*Character.BYTES));
        Memtable = new Component(numOfElementsInMemory,memoryCapacity,"Memtable");
        int bufferCapacity = memoryCapacity * factor;
        ImmutableMemtable = new Component(numOfElementsInMemory*factor,bufferCapacity,"ImmutableMemtable");
    }

    public void buildLSMTreeInMemoryComponents(){
        Memtable.createComponent();
        ImmutableMemtable.createComponent();
    }

    public void buildLSMTreeDiskComponents(){
        this.diskComponents = new Component[numOfComponents];
        for(int i=0; i<numOfComponents; i++){
            componentSize[i] = Math.pow(factor,(i+1)) * numOfElementsInMemory * (Integer.BYTES + (valueSize*Character.BYTES));
            maxNumOfElementsPerComp[i] = numOfElementsInMemory * Math.pow(factor,(i+1));
            diskComponents[i] = new Component(maxNumOfElementsPerComp[i],componentSize[i],"C" + (i+1));
            diskComponents[i].createComponent();
        }
    }

    public void writeLSMtoDisk(){
        Memtable.writeComponent();
        ImmutableMemtable.writeComponent();
        try {
            //FileWriter metaWriter = new FileWriter("/Users/natashamittal/Documents/LSMTree/src/data/metadata.txt");
            FileOutputStream stream = new FileOutputStream("/Users/natashamittal/Documents/LSMTree/src/data/metadata.txt");
            PrintWriter writer = new PrintWriter(stream);
            //writer.println("LSM Tree Details");
            writer.println(this.numOfComponents);
            writer.println(this.factor);
            writer.println(this.numOfElementsInMemory);
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
                    Integer.parseInt(list.get(3)),Boolean.parseBoolean(list.get(4)));
            this.Memtable.readComponent("Memtable");
            this.ImmutableMemtable.readComponent("ImmutableMemtable");
        }catch (Exception exception){

        }
    }

    public void append_lsm(int key, String value){
        num_append++;
        this.Memtable.keys.add(key);
        this.Memtable.values.add(value);
        this.Memtable.incrementActualNumOfElements();

        if(num_append % flushMemoryForRecovery == 0){
            if(this.Memtable.getActualNumOfElements() >= flushMemoryForRecovery) {
                this.Memtable.appendValuesInComponent();
            }
            num_append = 0;
        }

        if(this.Memtable.getActualNumOfElements() == this.Memtable.getMaxNumOfElements()){
            if((this.ImmutableMemtable.getActualNumOfElements() + this.Memtable.getActualNumOfElements()) <= this.ImmutableMemtable.getMaxNumOfElements()){
                componentHelper.merge_sort_component(this.Memtable.keys,this.Memtable.values);
                Component.mergeComponents(this.Memtable,this.ImmutableMemtable);
            }
            else {
                int level = 0;
                while(level < this.numOfComponents && ((diskComponents[level].getActualNumOfElements() + this.ImmutableMemtable.getMaxNumOfElements()) > diskComponents[level].getMaxNumOfElements())){
                    level++;
                }

                if(level == numOfComponents && (diskComponents[level-1].getActualNumOfElements() + this.ImmutableMemtable.getMaxNumOfElements()) > diskComponents[level-1].getMaxNumOfElements()){
                    diskComponents[level-1].readComponent("C" + level);
                    double numOfEleToBeRemoved = diskComponents[level-2].getMaxNumOfElements();
                    int index = (int)(diskComponents[level-1].getMaxNumOfElements() - numOfEleToBeRemoved);
                    int maxElements = (int)diskComponents[level-1].getMaxNumOfElements();
                    List<Integer> keys = new ArrayList<>();
                    List<String> values = new ArrayList<>();
                    while(index < maxElements){
                        keys.add(diskComponents[level-1].keys.get(index));
                        values.add(diskComponents[level-1].values.get(index));
                        index++;
                    }
                    diskComponents[level-1].keys.removeAll(keys);
                    diskComponents[level-1].values.removeAll(values);
                    diskComponents[level-1].writeComponent();

                    while(level > 1 && level <= numOfComponents){
                        diskComponents[level-1].readComponent("C" + level);
                        diskComponents[level-2].readComponent("C" + (level-1));
                        Component.mergeComponents(diskComponents[level-2],diskComponents[level-1]);
                        level--;
                    }
                    level--;
                }
                else{
                    while(level >= 1 && level <= numOfComponents){
                        diskComponents[level].readComponent("C" + level);
                        diskComponents[level-1].readComponent("C" + (level-1));
                        Component.mergeComponents(diskComponents[level-1],diskComponents[level]);
                        level--;
                    }
                }

                if(level == 0){
                    Component.mergeComponents(this.ImmutableMemtable,diskComponents[level]);
                }

                componentHelper.merge_sort_component(this.Memtable.keys,this.Memtable.values);
                Component.mergeComponents(this.Memtable,this.ImmutableMemtable);
            }
        }
    }

    public void insert_lsm(int key, String value){
        if(isBloomEnabled){
            bloomFilter.add(key);
        }
        append_lsm(key,value);
    }

    public String read_lsm(int key){
        if(isBloomEnabled && !bloomFilter.contains(key))
            return null;

        int index = -1;
        index = componentHelper.linearSearch(this.Memtable.keys,key);
        if(index != -1)
            return this.Memtable.values.get(index);

        int arr[] = this.ImmutableMemtable.keys.stream().mapToInt(Integer::intValue).toArray();
        index = componentHelper.binarySearch(arr,key,0,arr.length-1);
        if(index != -1)
            return this.ImmutableMemtable.values.get(index);

        for(int i=0; i<numOfComponents; i++){
            index = diskComponents[i].searchComponent(key);
            if(index != -1)
                return this.diskComponents[i].values.get(index);
        }

        return null;
    }

    public void update_lsm(int key, String value){
        if(isBloomEnabled && !bloomFilter.contains(key))
            return;

        int index = -1;
        index = componentHelper.linearSearch(this.Memtable.keys,key);
        if(index != -1){
            this.Memtable.values.set(index,value);
        }
        else
            append_lsm(key,value);
    }

    public void print_lsm_state(){
        System.out.println("LSM Statistics");
        System.out.println("C0 --> Actual Number of Elements: " + this.Memtable.getActualNumOfElements());
        System.out.println("C0 --> Maximum Number of Elements: " + this.Memtable.getMaxNumOfElements());
        System.out.println("Buffer --> Actual Number of Elements: " + this.ImmutableMemtable.getActualNumOfElements());
        System.out.println("Buffer --> Maximum Number of Elements: " + this.ImmutableMemtable.getMaxNumOfElements());
        for(int i=0; i<numOfComponents; i++){
            System.out.println("C" + (i+1) + " --> Actual Number of Elements: " + this.diskComponents[i].getActualNumOfElements());
            System.out.println("C" + (i+1) + " --> Maximum Number of Elements: " + this.diskComponents[i].getMaxNumOfElements());
        }
    }















}

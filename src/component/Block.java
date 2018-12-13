package component;

import lsmTree.DirectoryOfKeysPerBlock;
import lsmTree.LSMTree_Block;
import lsmTree.MinMaxPair;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Block implements Cloneable{

    String blockName;
    int maxNoOfElements;
    int actualNoOfElements;
    public List<Integer> keys;
    public List<String> values;
    String levelName;
    String keyFilePath;
    String valueFilePath;
    public File keyFile;
    public File valueFile;

    public void createBlock(String levelName, int j, int maxNoOfElementsPerBlock){
        this.levelName = levelName;
        this.blockName = "B" + j;
        this.maxNoOfElements = maxNoOfElementsPerBlock;
        this.actualNoOfElements = 0;
        this.keys = new ArrayList<>(maxNoOfElementsPerBlock);
        this.values = new ArrayList<>(maxNoOfElementsPerBlock);
        this.keyFilePath = "/Users/natashamittal/Documents/LSMTree/src/data/key_" + levelName + "_" + blockName + ".txt";
        this.valueFilePath = "/Users/natashamittal/Documents/LSMTree/src/data/value_" + levelName + "_" + blockName + ".txt";
        keyFile = new File(keyFilePath);
        valueFile = new File(valueFilePath);
        try {
            if (!keyFile.exists()) {
                boolean created = keyFile.createNewFile();
                System.out.println(created);
            }
            if (!valueFile.exists()) {
                valueFile.createNewFile();
            }
        }catch (IOException exception){

        }
    }

    public void readBlock(){
        try {
            List<String> stringKeys = Files.readAllLines(Paths.get(keyFilePath));
            this.keys.clear();
            this.values.clear();
            this.keys.addAll(stringKeys.stream().map(Integer::parseInt).collect(Collectors.toList()));
            this.values.addAll(Files.readAllLines(Paths.get(valueFilePath)));
            this.actualNoOfElements = this.keys.size();
        }catch (IOException exception){

        }
    }

    public void writeBlock(boolean diskComp, DirectoryOfKeysPerBlock directory){
        try {
            int min = -1, max = -1;
            PrintWriter keyWriter = new PrintWriter(keyFilePath);
                for (int i=0; i<keys.size(); i++) {
                    if(i==0 && diskComp){
                        min = keys.get(i);
                    }
                    else if(i==keys.size()-1 && diskComp){
                        max = keys.get(i);
                    }
                    keyWriter.println(keys.get(i));
                }

                PrintWriter valueWriter = new PrintWriter(valueFilePath);
                for (String value : values) {
                    valueWriter.println(value);
                }
                keyWriter.close();
                valueWriter.close();
                this.actualNoOfElements = keys.size();
                if(diskComp) {
                    int level = Character.getNumericValue(levelName.charAt(1));
                    int blockNo = Integer.parseInt(blockName.substring(1));
                    directory.updateKeyPerBlock(level - 1, blockNo - 1, new MinMaxPair(min, max));
                }

            }catch (IOException exception){

            }
    }

    public void insertKV(int key, String value){
        keys.add(key);
        values.add(value);
        actualNoOfElements++;
    }

    public int searchBlock(int key){
        int index = -1;
        try {
            List<String> stringKeys = Files.readAllLines(Paths.get(keyFilePath));
            List<Integer> keys = stringKeys.stream().map(Integer::parseInt).collect(Collectors.toList());
            int keysArray [] = keys.stream().mapToInt(Integer::intValue).toArray();
            index = componentHelper.binarySearch(keysArray, key, 0, keysArray.length-1);

        }catch (IOException exception){

        }
        return index;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public String getBlockName() {
        return blockName;
    }

    public void setBlockName(String blockName) {
        this.blockName = blockName;
    }

    public int getActualNoOfElements() {
        return actualNoOfElements;
    }

    public void setActualNoOfElements(int actualNoOfElements) {
        this.actualNoOfElements = actualNoOfElements;
    }

    public int getMaxNoOfElements() {
        return maxNoOfElements;
    }

    public void setMaxNoOfElements(int maxNoOfElements) {
        this.maxNoOfElements = maxNoOfElements;
    }
}

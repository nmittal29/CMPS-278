package component;

import lsmTree.DirectoryOfKeysPerBlock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BlockComponent {

    int numOfBlocks;
    public List<Block> blocks;
    int numOfEmptyBlocks;
    int indexOfFirstEmptyBlock;
    String levelName;
    public int actualNoOfElements;
    int maxNoOfElements;

    public int getNumOfBlocks() {
        return numOfBlocks;
    }

    public String getLevelName() {
        return levelName;
    }

    public void createComponent(int numOfBlocks, String levelName, int maxNumOfElementsPerBlock){
        this.numOfBlocks = numOfBlocks;
        this.levelName = levelName;
        this.blocks = new ArrayList<Block>();
        for(int i=0; i<numOfBlocks; i++){
            Block b = new Block();
            b.createBlock(levelName,i+1,maxNumOfElementsPerBlock);
            this.blocks.add(b);
        }
        this.numOfEmptyBlocks = 0;
        this.indexOfFirstEmptyBlock = 0;
        this.maxNoOfElements = numOfBlocks * maxNumOfElementsPerBlock;
        this.actualNoOfElements = 0;
    }

    public int readBlockOfComponent(int index){
        Block block = blocks.get(index);
        block.readBlock();
        return block.actualNoOfElements;
    }

    public void readComponent(){
        int sum = 0;
        for(int i=0; i<numOfBlocks; i++){
            sum += readBlockOfComponent(i);
        }
        this.actualNoOfElements = sum;
    }

    public void writeBlockOfComponent(int index, boolean diskComp, DirectoryOfKeysPerBlock directory){
            blocks.get(index).writeBlock(diskComp, directory);
    }

    public void writeComponent(boolean diskComp, DirectoryOfKeysPerBlock directory){
        for(int i=0; i<numOfBlocks; i++){
            writeBlockOfComponent(i,diskComp, directory);
        }
    }

    public void insertInBlock(int key, String value){
        if(actualNoOfElements < maxNoOfElements) {
            blocks.get(0).insertKV(key, value);
            this.actualNoOfElements++;
        }
    }

    public int searchComponent(int key, int indexOfFirstBlockToSearch){
        int index = -1;
        for(int i=indexOfFirstBlockToSearch; i<numOfBlocks; i++){
            index = blocks.get(i).searchBlock(key);
            if(index != -1)
                break;
        }
        return index;
    }

    public static void moveToNextComponent(BlockComponent current, BlockComponent next, int index, boolean diskComp, DirectoryOfKeysPerBlock directory){
        //if(next.getNumOfEmptyBlocks()/index > 0){
            int sum = 0;
            List<Block> blocks = new ArrayList<>();
            for(int i=current.numOfBlocks-index; i<current.numOfBlocks; i++){
                blocks.add(current.blocks.get(i));
                sum += current.blocks.get(i).actualNoOfElements;
            }
            next.blocks.get(0).keys.addAll(blocks.get(0).keys);
            next.blocks.get(0).values.addAll(blocks.get(0).values);
            next.actualNoOfElements += sum;
            next.blocks.get(0).actualNoOfElements +=sum;
            //next.indexOfFirstEmptyBlock += index;
            current.actualNoOfElements -= sum;
            current.blocks.get(0).keys.clear();
            current.blocks.get(0).values.clear();
            current.blocks.get(0).actualNoOfElements -= sum;
            blocks.clear();
        //}
        /*else {
            List<Integer> currentKeys = new ArrayList<>();
            List<String> currentValues = new ArrayList<>();
            List<Integer> nextKeys = new ArrayList<>();
            List<String> nextValues = new ArrayList<>();
            List<Block> blocks = new ArrayList<>();
            for(int i=current.numOfBlocks-index; i<current.numOfBlocks; i++){
                blocks.add(current.blocks.get(i));
                currentKeys.addAll(current.blocks.get(i).keys);
                currentValues.addAll(current.blocks.get(i).values);
                current.blocks.get(i).keys.clear();
                current.blocks.get(i).values.clear();
                current.blocks.get(i).actualNoOfElements = 0;
            }
            current.indexOfFirstEmptyBlock = current.numOfBlocks-index;
            current.blocks.removeAll(blocks);
            blocks.clear();
            for(int i=next.indexOfFirstEmptyBlock; i < (next.indexOfFirstEmptyBlock+index); i++){
                blocks.add(next.blocks.get(i));
                nextKeys.addAll(next.blocks.get(i).keys);
                nextValues.addAll(next.blocks.get(i).values);
                next.blocks.get(i).keys.clear();
                next.blocks.get(i).values.clear();
                next.blocks.get(i).actualNoOfElements = 0;
            }
            next.indexOfFirstEmptyBlock = next.indexOfFirstEmptyBlock+index;
            next.blocks.removeAll(blocks);
            blocks.clear();
            componentHelper.mergeLists_forBlocks(currentKeys,currentValues,nextKeys,nextValues,next);
        }*/
        current.writeComponent(diskComp, directory);
        next.writeComponent(diskComp, directory);
    }

    public static void movePairsToNextComponent(KVPair pair, BlockComponent next, boolean diskComp, DirectoryOfKeysPerBlock directory) {
        //if(next.getNumOfEmptyBlocks()/index > 0){
        next.blocks.get(0).keys.clear();
        next.blocks.get(0).values.clear();
        next.blocks.get(0).keys.addAll(pair.getKeys());
        next.blocks.get(0).values.addAll(pair.getValues());
        next.blocks.get(0).actualNoOfElements = pair.getKeys().size();
        if(next.levelName.equals("ImmutableMemtable"))
            next.actualNoOfElements = pair.getKeys().size();
        else if(next.actualNoOfElements == next.maxNoOfElements){
           //do nothing
        }
        else
            next.actualNoOfElements = next.actualNoOfElements + pair.getKeys().size();
        //next.indexOfFirstEmptyBlock = -1;
        next.writeComponent(diskComp, directory);
    }

    public int getActualNoOfElements() {
        return actualNoOfElements;
    }

    public int getMaxNoOfElements() {
        return maxNoOfElements;
    }

    public int getNumOfEmptyBlocks() {
        setNumOfEmptyBlocks();
        return numOfEmptyBlocks;
    }

    public void setNumOfEmptyBlocks() {
        int emptyBlocks = 0;
        for(int i=0; i<numOfBlocks; i++){
            if(blocks.get(i).getActualNoOfElements() < blocks.get(i).getMaxNoOfElements()){
                emptyBlocks++;
            }
        }
        this.numOfEmptyBlocks = emptyBlocks;
    }

    @Override
    public String toString() {
        return "BlockComponent{" +
                "numOfBlocks=" + numOfBlocks +
                ", levelName='" + levelName + '\'' +
                ", actualNoOfElements=" + actualNoOfElements +
                ", maxNoOfElements=" + maxNoOfElements +
                '}';
    }

    public int getIndexOfFirstEmptyBlock() {
        return indexOfFirstEmptyBlock;
    }

    public void setIndexOfFirstEmptyBlock() {
        this.indexOfFirstEmptyBlock = numOfBlocks - numOfEmptyBlocks - 1;
    }
}

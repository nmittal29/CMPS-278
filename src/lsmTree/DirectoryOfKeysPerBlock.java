package lsmTree;

import java.util.*;

public class DirectoryOfKeysPerBlock {

    int numOfComponents;
    int [] numOfBlocksPerComp;
    List<List<MinMaxPair>> keyList;

    public void createDirectory(int numOfComponents, int [] numOfBlocksPerComponent){
        this.numOfComponents = numOfComponents;
        this.numOfBlocksPerComp = numOfBlocksPerComponent;
        keyList = new ArrayList<>();
        for(int i=0; i<this.numOfComponents; i++){
            keyList.add(new ArrayList<MinMaxPair>(Collections.nCopies(this.numOfBlocksPerComp[i], new MinMaxPair(-1,-1))));
        }
    }

    public void addComponentToDir(int numOfBlocks){
        this.numOfComponents++;
        int [] newNumOfBlocksPerComp = new int[numOfComponents];
        for(int i=0; i<numOfBlocksPerComp.length; i++){
            newNumOfBlocksPerComp[i] = numOfBlocksPerComp[i];
        }
        newNumOfBlocksPerComp[numOfComponents-1] = numOfBlocks;
        this.numOfBlocksPerComp = newNumOfBlocksPerComp;
        keyList.add(new ArrayList<>(Collections.nCopies(this.numOfBlocksPerComp[numOfComponents-1], new MinMaxPair(-1,-1))));
    }

    public void updateKeyPerBlock(int level, int blockNo, MinMaxPair pair){
        keyList.get(level).set(blockNo,pair);
    }

    public List<MinMaxPair> getKeysPerLevel(int level){
        return keyList.get(level);
    }

    public MinMaxPair getKeyPerBlock(int level, int blockNo){
        return keyList.get(level).get(blockNo);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<numOfComponents; i++){
            for(int j=0; j<numOfBlocksPerComp[i]; j++){
                sb.append(i).append(":").append(keyList.get(i).get(j).min).append("-").append(keyList.get(i).get(j).max);
                if(j != numOfBlocksPerComp[i]-1){
                    sb.append(",");
                }
                if(j == numOfBlocksPerComp[i]-1)
                    sb.append("@");
            }
        }
        return sb.toString();
    }

    public void buildDirectory(String dirStr){
        String commonStr[] = dirStr.split("@");
        numOfComponents = 0;
        for(int i=0; i<commonStr.length; i++){
            String levelStr[] = commonStr[i].split(":");
            keyList.get(i).clear();
            String keys[] = levelStr[1].split(",");
            for(int j=0; j<keys.length; j++){
                String minMax[] = keys[j].split("-");
                keyList.get(i).add(new MinMaxPair(Integer.parseInt(minMax[0]),Integer.parseInt(minMax[1])));
            }
            numOfComponents++;
        }
    }
}

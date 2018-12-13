package component;

import lsmTree.LSMTree_Block;
import lsmTree.MinMaxPair;

import java.util.*;

public class componentHelper {

    public static int binarySearch(int [] keys, int key, int low, int high){
        if(low <= high){
            int middle = low + (high - low)/2;
            if(key == keys[middle])
                return middle;
            else if(key < keys[middle])
                return binarySearch(keys,key,low,middle-1);
            else
                return binarySearch(keys,key,middle+1,high);
        }
        return -1;
    }

    public static int linearSearch(List<Integer> keys, int key){
        int index = -1;
        for(int i=0; i<keys.size(); i++){
            if(keys.get(i) == key){
                index = i;
                break;
            }
        }
        return index;
    }

    public static List<Integer> modifiedLinearSearch(List<MinMaxPair> keys, int key){

        List<Integer> blocksToBeSearched = new ArrayList<>();
        for (int i = 0; i < keys.size(); i++) {
            if(key >= keys.get(i).getMin() && key <= keys.get(i).getMax()) {
                blocksToBeSearched.add(i);
            }
        }
        return blocksToBeSearched;
    }

    public static void mergeLists(Component current, Component next){
        List<Integer> mergedKeys = new LinkedList<>();
        List<String> mergedValues = new LinkedList<>();
        int left = 0, right = 0;
        while(left < current.keys.size() && right < next.keys.size()){
            if(current.keys.get(left) < next.keys.get(right)){
                mergedKeys.add(current.keys.get(left));
                mergedValues.add(current.values.get(left));
                left++;
            }
            else if(current.keys.get(left) > next.keys.get(right)){
                mergedKeys.add(next.keys.get(right));
                mergedValues.add(next.values.get(right));
                right++;
            }
            else{
                mergedKeys.add(current.keys.get(left));
                mergedValues.add(current.values.get(left));
                left++;
            }
        }

        if(left < current.keys.size()){
            while(left < current.keys.size()){
                mergedKeys.add(current.keys.get(left));
                mergedValues.add(current.values.get(left));
                left++;
            }
        }

        if(right < next.keys.size()){
            while(right < next.keys.size()){
                mergedKeys.add(next.keys.get(right));
                mergedValues.add(next.values.get(right));
                right++;
            }
        }
        next.keys.clear();
        next.values.clear();
        next.keys.addAll(mergedKeys);
        next.values.addAll(mergedValues);
        next.actualNumOfElements = next.keys.size();
        mergedKeys.clear();
        mergedValues.clear();
    }


    public static void merge_sort_blockComponent(BlockComponent component, int index) throws CloneNotSupportedException{
        List<Integer> keys = new ArrayList<>();
        List<String> values = new ArrayList<>();
        keys.addAll(component.blocks.get(0).keys);
        values.addAll(component.blocks.get(0).values);
        component.blocks.clear();
        merge_sort_component(keys,values);
        int numOfElementsPerBlock = component.maxNoOfElements/component.numOfBlocks;
        int counter = (int)Math.ceil(keys.size()/numOfElementsPerBlock);
        int cnt = 0;
        Block b = new Block();
        for(int i=0; i<counter; i++){
            b.createBlock(component.levelName,1,numOfElementsPerBlock);
            for(int j=0; j<numOfElementsPerBlock && cnt < keys.size(); j++){
                b.keys.add(keys.get(cnt));
                b.values.add(values.get(cnt));
                cnt++;
            }
        }
        component.blocks.add(b);
        component.blocks.get(0).actualNoOfElements = b.keys.size();
        keys.clear();
        values.clear();
    }

    public static KVPair mergeLists_forBlocks(List<Integer> currentKeys, List<String> currentValues,
                                            List<Integer> nextKeys, List<String> nextValues){
        List<Integer> mergedKeys = new LinkedList<>();
        List<String> mergedValues = new LinkedList<>();
        int left = 0, right = 0;
        while(left < currentKeys.size() && right < nextKeys.size()){
            if(currentKeys.get(left) < nextKeys.get(right)){
                mergedKeys.add(currentKeys.get(left));
                mergedValues.add(currentValues.get(left));
                left++;
            }
            else if(currentKeys.get(left) > nextKeys.get(right)){
                mergedKeys.add(nextKeys.get(right));
                mergedValues.add(nextValues.get(right));
                right++;
            }
            else{
                mergedKeys.add(currentKeys.get(left));
                mergedValues.add(currentValues.get(left));
                left++;
            }
        }

        if(left < currentKeys.size()){
            while(left < currentKeys.size()){
                mergedKeys.add(currentKeys.get(left));
                mergedValues.add(currentValues.get(left));
                left++;
            }
        }

        if(right < nextKeys.size()){
            while(right < nextKeys.size()){
                mergedKeys.add(nextKeys.get(right));
                mergedValues.add(nextValues.get(right));
                right++;
            }
        }
        /*List<Block> blocks = new ArrayList<>();
        int numOfElementsPerBlock = next.maxNoOfElements/next.numOfBlocks;
        int counter = (int)Math.ceil(mergedKeys.size()/numOfElementsPerBlock);
        int cnt = 0;
        for(int i=0; i<counter; i++){
            Block b = new Block();
            b.createBlock(next.levelName,next.indexOfFirstEmptyBlock+i,numOfElementsPerBlock);
            for(int j=0; j<numOfElementsPerBlock && cnt < mergedKeys.size(); j++){
                b.keys.add(mergedKeys.get(cnt));
                b.values.add(mergedValues.get(cnt));
                cnt++;
            }
            blocks.add(b);
        }
        next.blocks.addAll(blocks);
        blocks.clear();
        mergedKeys.clear();
        mergedValues.clear();
        next.indexOfFirstEmptyBlock = next.indexOfFirstEmptyBlock + counter;*/
        KVPair pair = new KVPair(mergedKeys,mergedValues);
        return pair;
    }

    public static void merge_sort_component(List<Integer> keys, List<String> values) {
        if (keys.size()<=1) return; // small list don't need to be merged

        // SEPARATE

        int mid = keys.size()/2; // estimate half the size

        ArrayList<Integer> leftKey = new ArrayList<Integer>();
        ArrayList<Integer> rightKey = new ArrayList<Integer>();
        ArrayList<String> leftValue = new ArrayList<String>();
        ArrayList<String> rightValue = new ArrayList<String>();


        for(int i = 0; i < mid; i++){
            leftKey.add(keys.remove(0)); // put first half part in left
            leftValue.add(values.remove(0));
        }
        while (keys.size()!=0){
            rightKey.add(keys.remove(0));// put the remainings in right
            rightValue.add(values.remove(0));
        }
        // Here a is now empty

        // MERGE PARTS INDEPENDANTLY

        merge_sort_component(leftKey,leftValue);  // merge the left part
        merge_sort_component(rightKey,rightValue); // merge the right part

        // MERGE PARTS

        // while there is something in the two lists
        while (leftKey.size()!=0 && rightKey.size()!=0) {
            // compare both heads, add the lesser into the result and remove it from its list
            if (leftKey.get(0).compareTo(rightKey.get(0)) <= 0){
                keys.add(leftKey.remove(0));
                values.add(leftValue.remove(0));
            }
            else{
                keys.add(rightKey.remove(0));
                values.add(rightValue.remove(0));
            }
        }

        // fill the result with what remains in left OR right (both can't contains elements)
        while(leftKey.size()!=0){
            keys.add(leftKey.remove(0));
            values.add(leftValue.remove(0));
        }
        while(rightKey.size()!=0){
            keys.add(rightKey.remove(0));
            values.add(rightValue.remove(0));
        }
    }


    /*public static void merge_sort_component(Component component, int low, int high){
        if(low < high) {
            int middle = low / 2 + high / 2;
            merge_sort_component(component, low, middle - 1);
            merge_sort_component(component, middle + 1, high);
            merge_values_in_component(component, low, middle, high);
        }
    }

    public static void merge_values_in_component(Component component, int low, int mid, int high){
        int len1 = mid - low + 1;
        int len2 = high - mid;
        int leftKey[] = new int[len1];
        int rightKey[] = new int[len2];
        String leftValue[] = new String[len1];
        String rightValue[] = new String[len2];

        for(int i=0;i<len1;i++){
            leftKey[i] = component.keys.get(i + low);
            leftValue[i] = component.values.get(i + low);
        }
        for(int j=0;j<len2;j++){
            rightKey[j] = component.keys.get(mid + j + 1);
            rightValue[j] = component.values.get(mid + j + 1);
        }
        int i = 0;
        int j = 0;
        int k = low;

        while(i < len1 && j < len2){
            if(leftKey[i] <= rightKey[j]) {
                component.keys.set(k,leftKey[i]);
                component.values.set(k,leftValue[i]);
                i++;
                k++;
            }
            else {
                component.keys.set(k,rightKey[j]);
                component.values.set(k,rightValue[j]);
                j++;
                k++;
            }
        }
        while(i < len1){
            component.keys.set(k,leftKey[i]);
            component.values.set(k,leftValue[i]);
            i++;
            k++;
        }

        while(j < len2){
            component.keys.set(k,rightKey[j]);
            component.values.set(k,rightValue[j]);
            j++;
            k++;
        }
    }*/
}

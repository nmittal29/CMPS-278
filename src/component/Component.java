package component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Component {

    double maxNumOfElements;
    int actualNumOfElements;
    double capacity;
    String component_name;
    public List<Integer> keys;
    public List<String> values;
    String fileTypes[] = {"Key","Value"};

    public Component(double maxNumOfElements, double capacity, String component_name){
        this.maxNumOfElements = maxNumOfElements;
        this.capacity = capacity;
        this.component_name = component_name;
        this.keys = new ArrayList<>();
        this.values = new ArrayList<>();
        this.actualNumOfElements = 0;
    }

    public String getFileName(String component_name, int typeIndex){
        return "/Users/natashamittal/Documents/LSMTree/src/data/" + this.fileTypes[typeIndex] + "_" + this.component_name + ".txt";
    }

    public void createComponent(){
        String keyPath = getFileName(this.component_name,0 );
        String valuePath = getFileName(this.component_name,1 );
        File keyFile = new File(keyPath);
        File valueFile = new File(valuePath);
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

    public void readComponent(String component_name){
        try {
            List<String> stringKeys = Files.readAllLines(Paths.get(getFileName(component_name,0)));
            this.keys.clear();
            this.values.clear();
            this.keys.addAll(stringKeys.stream().map(Integer::parseInt).collect(Collectors.toList()));
            this.values.addAll(Files.readAllLines(Paths.get(getFileName(component_name,1))));
            this.actualNumOfElements = this.keys.size();
        }catch (IOException exception){

        }
    }

    public void writeComponent(){
        String keyPath = getFileName(this.component_name,0 );
        String valuePath = getFileName(this.component_name,1 );
        try {
            PrintWriter keyWriter = new PrintWriter(keyPath);
            for (Integer key : keys) {
                keyWriter.println(key);
            }

            PrintWriter valueWriter = new PrintWriter(valuePath);
            for (String value : values) {
                valueWriter.println(value);
            }
            keyWriter.close();
            valueWriter.close();
            this.actualNumOfElements = keys.size();
        }catch (IOException exception){

        }
    }

    public void appendValuesInComponent(){
        String keyPath = getFileName(this.component_name,0 );
        String valuePath = getFileName(this.component_name,1 );
        try {
            //FileWriter keyFileWriter= new FileWriter(keyPath, true);
            FileOutputStream keyStream = new FileOutputStream(keyPath);
            PrintWriter keyWriter = new PrintWriter(keyStream);
            for (Integer key : keys) {
                keyWriter.println(key);
            }
            //FileWriter valueFileWriter= new FileWriter(valuePath, true);
            FileOutputStream valueStream = new FileOutputStream(valuePath);
            PrintWriter valueWriter = new PrintWriter(valueStream);
            for (String value : values) {
                valueWriter.println(value);
            }

            keyWriter.close();
            valueWriter.close();
            keyStream.close();
            valueStream.close();
            //this.actualNumOfElements = this.actualNumOfElements + keys.size();
        }catch (IOException exception){

        }
    }

    public String readValueAtIndex(Integer index){
        String valueAtIndex = "";
        try (Stream<String> lines = Files.lines(Paths.get(getFileName(this.component_name,1)))) {
            valueAtIndex = lines.skip(index-1).findFirst().get();
        } catch (IOException exception){

        }
        return valueAtIndex;
    }

    public int searchComponent(int key){
        int index = -1;
        try {
            List<String> stringKeys = Files.readAllLines(Paths.get(getFileName(component_name,0)));
            List<Integer> keys = stringKeys.stream().map(Integer::parseInt).collect(Collectors.toList());
            int keysArray [] = keys.stream().mapToInt(Integer::intValue).toArray();
            index = componentHelper.binarySearch(keysArray, key, 0, keysArray.length-1);

        }catch (IOException exception){

        }
        return index;
    }

    public static void mergeComponents(Component current, Component next){
        if(next.actualNumOfElements == 0){
            next.keys.addAll(current.keys);
            next.values.addAll(current.values);
            //next.actualNumOfElements = next.actualNumOfElements + current.actualNumOfElements;
        }
        else {
            componentHelper.mergeLists(current,next);
        }

        current.keys.clear();
        current.values.clear();
        current.actualNumOfElements = 0;

        current.writeComponent();
        next.writeComponent();
    }

    public double getMaxNumOfElements() {
        return maxNumOfElements;
    }

    public int getActualNumOfElements() {
        return actualNumOfElements;
    }

    public double getCapacity() {
        return capacity;
    }

    public String getComponent_name() {
        return component_name;
    }

    public int getKeySize() { return keys.size(); };

    public void incrementActualNumOfElements() { ++actualNumOfElements; };
}

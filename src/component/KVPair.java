package component;

import java.util.ArrayList;
import java.util.List;

public class KVPair {

    List<Integer> keys;
    List<String> values;

    public KVPair(List<Integer> keyList, List<String> valueList){
        this.keys = new ArrayList<>();
        this.keys.addAll(keyList);
        this.values = new ArrayList<>();
        this.values.addAll(valueList);
    }

    public List<Integer> getKeys() {
        return keys;
    }

    public List<String> getValues() {
        return values;
    }
}

package prototype.xd.scheduler.utilities;

import androidx.collection.ArrayMap;
import androidx.collection.SimpleArrayMap;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

// simple serialization wrapper for <String, String> array map
public class SSMap extends ArrayMap<String, String> implements Serializable {
    
    public SSMap() {
        super();
    }
    
    public SSMap(SimpleArrayMap map) {
        super(map);
    }
    
    private void writeObject(ObjectOutputStream oos)
            throws IOException {
        oos.defaultWriteObject();
        oos.writeObject(keySet().toArray(new String[0]));
        oos.writeObject(values().toArray(new String[0]));
    }

    private void readObject(ObjectInputStream ois)
            throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        String[] keySet = (String[]) ois.readObject();
        String[] values = (String[]) ois.readObject();
        for(int i = 0; i < keySet.length; i++) {
            put(keySet[i], values[i]);
        }
    }
}

package prototype.xd.scheduler.utilities;

import androidx.collection.ArrayMap;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

// simple serialization wrapper for array map
public class SArrayMap<K extends Serializable, V extends Serializable> extends ArrayMap<K, V> implements Serializable {
    
    public SArrayMap() {
        super();
    }
    
    public SArrayMap(SArrayMap<K, V> map) {
        super(map);
    }
    
    @SuppressWarnings("unchecked")
    private void writeObject(ObjectOutputStream oos)
            throws IOException {
        oos.defaultWriteObject();
        oos.writeObject(keySet().toArray((K[])new Serializable[0]));
        oos.writeObject(values().toArray((V[])new Serializable[0]));
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream ois)
            throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        K[] keySet = (K[]) ois.readObject();
        V[] values = (V[]) ois.readObject();
        for(int i = 0; i < keySet.length; i++) {
            put(keySet[i], values[i]);
        }
    }
}

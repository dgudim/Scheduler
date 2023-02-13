package prototype.xd.scheduler.utilities;

import androidx.annotation.NonNull;
import androidx.collection.ArrayMap;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

// simple serialization wrapper for array map

/**
 * ArrayMap class with serialization
 *
 * @param <K> serializable key
 * @param <V> serializable value
 */
public class SArrayMap<K extends Serializable, V extends Serializable> extends ArrayMap<K, V> implements Serializable {
    
    private static final long serialVersionUID = 6114227458943730012L;
    
    public SArrayMap() {}
    
    public SArrayMap(@NonNull SArrayMap<K, V> map) {
        super(map);
    }
    
    @SuppressWarnings("unchecked")
    private void writeObject(@NonNull ObjectOutputStream oos)
            throws IOException {
        oos.defaultWriteObject();
        oos.writeObject(keySet().toArray((K[]) new Serializable[0]));
        oos.writeObject(values().toArray((V[]) new Serializable[0]));
    }
    
    @SuppressWarnings("unchecked")
    private void readObject(@NonNull ObjectInputStream ois)
            throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        K[] keySet = (K[]) ois.readObject();
        V[] values = (V[]) ois.readObject();
        for (int i = 0; i < keySet.length; i++) {
            put(keySet[i], values[i]);
        }
    }
}

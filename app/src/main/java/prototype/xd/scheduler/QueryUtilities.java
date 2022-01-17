package prototype.xd.scheduler;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

import java.util.ArrayList;

public class QueryUtilities {
    
    public static String getString(Cursor cursor, ArrayList<String> columns, String column){
        return cursor.getString(columns.indexOf(column));
    }
    
    public static long getLong(Cursor cursor, ArrayList<String> columns, String column){
        return cursor.getLong(columns.indexOf(column));
    }
    
    public static int getInt(Cursor cursor, ArrayList<String> columns, String column){
        return cursor.getInt(columns.indexOf(column));
    }
    
    public static Cursor query(ContentResolver contentResolver, Uri queryUri, String[] columns, String selection) {
        return contentResolver.query(queryUri, columns, selection, null, null);
    }
    
}

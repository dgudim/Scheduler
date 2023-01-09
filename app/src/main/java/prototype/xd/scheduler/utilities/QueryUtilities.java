package prototype.xd.scheduler.utilities;

import static prototype.xd.scheduler.utilities.Utilities.nullWrapper;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

import java.util.List;

public class QueryUtilities {
    
    public static final String NAME = QueryUtilities.class.getSimpleName();
    
    private QueryUtilities() {
        throw new IllegalStateException(NAME + " can't be instantiated");
    }
    
    public static String getString(Cursor cursor, List<String> columns, String column){
        return nullWrapper(cursor.getString(columns.indexOf(column)));
    }
    
    public static long getLong(Cursor cursor, List<String> columns, String column){
        return cursor.getLong(columns.indexOf(column));
    }
    
    public static boolean getBoolean(Cursor cursor, List<String> columns, String column){
        return cursor.getInt(columns.indexOf(column)) > 0;
    }
    
    public static int getInt(Cursor cursor, List<String> columns, String column){
        return cursor.getInt(columns.indexOf(column));
    }
    
    public static Cursor query(ContentResolver contentResolver, Uri queryUri, String[] columns, String selection, String[] selectionArgs) {
        return contentResolver.query(queryUri, columns, selection, selectionArgs, null);
    }
    
    public static Cursor query(ContentResolver contentResolver, Uri queryUri, String[] columns, String selection) {
        return query(contentResolver, queryUri, columns, selection, null);
    }
    
}

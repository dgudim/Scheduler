package prototype.xd.scheduler.utilities;

import static prototype.xd.scheduler.utilities.Utilities.nullWrapper;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Objects;

public final class QueryUtilities {
    
    public static final String NAME = QueryUtilities.class.getSimpleName();
    
    private QueryUtilities() throws InstantiationException {
        throw new InstantiationException(NAME);
    }
    
    /**
     * Get string from a cursor
     *
     * @param cursor  cursor to a table
     * @param columns list of columns in the table
     * @param column  target column
     * @return string value stored in the column, if it's null returns empty string
     */
    @NonNull
    public static String getString(@NonNull Cursor cursor, @NonNull List<String> columns, @NonNull String column) {
        return nullWrapper(cursor.getString(columns.indexOf(column)));
    }
    
    /**
     * Get a long from a cursor
     *
     * @param cursor  cursor to a table
     * @param columns list of columns in the table
     * @param column  target column
     * @return long value stored in the column
     */
    public static long getLong(@NonNull Cursor cursor, @NonNull List<String> columns, @NonNull String column) {
        return cursor.getLong(columns.indexOf(column));
    }
    
    /**
     * Get a boolean from a cursor
     *
     * @param cursor  cursor to a table
     * @param columns list of columns in the table
     * @param column  target column
     * @return boolean value stored in the column
     */
    public static boolean getBoolean(@NonNull Cursor cursor, @NonNull List<String> columns, @NonNull String column) {
        return cursor.getInt(columns.indexOf(column)) > 0;
    }
    
    /**
     * Get an int from a cursor
     *
     * @param cursor  cursor to a table
     * @param columns list of columns in the table
     * @param column  target column
     * @return int value stored in the column
     */
    public static int getInt(@NonNull Cursor cursor, @NonNull List<String> columns, @NonNull String column) {
        return cursor.getInt(columns.indexOf(column));
    }
    
    /**
     * Query a given Uri
     *
     * @param contentResolver content resolver to query from
     * @param queryUri        uri to query
     * @param columns         columns to get or null (every column)
     * @param selection       query statement/filter or null
     * @return cursor to the table
     */
    @NonNull
    public static Cursor query(@NonNull ContentResolver contentResolver, @NonNull Uri queryUri,
                               @Nullable String[] columns, @Nullable String selection) {
        return Objects.requireNonNull(contentResolver.query(queryUri, columns, selection, null, null));
    }
}

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
    
    @NonNull
    public static String getString(@NonNull Cursor cursor, @NonNull List<String> columns, @NonNull String column) {
        return nullWrapper(cursor.getString(columns.indexOf(column)));
    }
    
    public static long getLong(@NonNull Cursor cursor, @NonNull List<String> columns, @NonNull String column) {
        return cursor.getLong(columns.indexOf(column));
    }
    
    public static boolean getBoolean(@NonNull Cursor cursor, @NonNull List<String> columns, @NonNull String column) {
        return cursor.getInt(columns.indexOf(column)) > 0;
    }
    
    public static int getInt(@NonNull Cursor cursor, @NonNull List<String> columns, @NonNull String column) {
        return cursor.getInt(columns.indexOf(column));
    }
    
    @NonNull
    public static Cursor query(@NonNull ContentResolver contentResolver, @NonNull Uri queryUri,
                               @Nullable String[] columns, @Nullable String selection) {
        return Objects.requireNonNull(contentResolver.query(queryUri, columns, selection, null, null));
    }
}

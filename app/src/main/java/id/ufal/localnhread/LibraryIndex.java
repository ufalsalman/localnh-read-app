package id.ufal.localnhread;

import android.content.Context;
import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;

final class LibraryIndex {
    private static final String PREF = "library_index";
    private static final String TREE = "tree";
    private static final String BOOKS = "books";
    private LibraryIndex() { }

    static void save(Context context, String tree, ArrayList<Cartridge> books) {
        JSONArray values = new JSONArray();
        for (Cartridge book : books) {
            JSONObject value = new JSONObject();
            try {
                value.put("uri", book.uri.toString());
                value.put("modified", book.modified);
                value.put("id", book.id);
                value.put("title", book.title);
                value.put("titleJp", book.titleJp);
                value.put("language", book.language);
                value.put("scanlator", book.scanlator);
                value.put("tagsText", book.tagsText);
                value.put("uploaded", book.uploaded);
                value.put("archived", book.archived);
                value.put("coverPath", book.coverPath);
                values.put(value);
            } catch (Exception ignored) { }
        }
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                .putString(TREE, tree).putString(BOOKS, values.toString()).apply();
    }

    static ArrayList<Cartridge> load(Context context, String tree) {
        android.content.SharedPreferences preferences = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        if (!tree.equals(preferences.getString(TREE, null))) return null;
        String encoded = preferences.getString(BOOKS, null);
        if (encoded == null) return null;
        ArrayList<Cartridge> books = new ArrayList<>();
        try {
            JSONArray values = new JSONArray(encoded);
            for (int i = 0; i < values.length(); i++) {
                JSONObject value = values.getJSONObject(i);
                books.add(new Cartridge(Uri.parse(value.getString("uri")), value.optLong("modified"),
                        value.optString("id"), value.optString("title"), value.optString("titleJp"),
                        value.optString("language"), value.optString("scanlator"), value.optString("tagsText"),
                        value.optString("uploaded"), value.optString("archived"), value.isNull("coverPath") ? null : value.optString("coverPath"),
                        new ArrayList<>(), null));
            }
        } catch (Exception ignored) { return null; }
        return books;
    }

    static void clear(Context context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().clear().apply();
    }
}

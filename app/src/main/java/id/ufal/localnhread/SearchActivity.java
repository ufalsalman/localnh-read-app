package id.ufal.localnhread;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Locale;

public final class SearchActivity extends AppCompatActivity {
    private BookAdapter adapter;
    private EditText input;
    private TextView noResults;
    private Button go;
    private ArrayList<Cartridge> books = new ArrayList<>();

    @Override protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_search);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        View root = findViewById(R.id.searchRoot);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        input = findViewById(R.id.searchInput);
        noResults = findViewById(R.id.noResults);
        RecyclerView results = findViewById(R.id.searchResults);
        adapter = new BookAdapter(this::open);
        results.setLayoutManager(new GridLayoutManager(this, 2));
        results.setAdapter(adapter);
        go = findViewById(R.id.goButton);
        go.setOnClickListener(v -> filter());
        input.setOnEditorActionListener((v, action, event) -> {
            if (action == EditorInfo.IME_ACTION_SEARCH) { filter(); return true; }
            return false;
        });
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        loadBooks();
    }

    private void loadBooks() {
        String saved = getSharedPreferences("library", MODE_PRIVATE).getString("tree", null);
        if (saved == null) { noResults.setVisibility(View.VISIBLE); return; }
        go.setEnabled(false);
        ArrayList<Cartridge> cached = LibraryIndex.load(this, saved);
        if (cached == null) { go.setEnabled(false); noResults.setText(R.string.search_index_missing); noResults.setVisibility(View.VISIBLE); return; }
        books = cached;
        go.setEnabled(true);
        noResults.setText(R.string.no_search_results);
    }


    private void filter() {
        String query = input.getText().toString().trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            adapter.replace(new ArrayList<>());
            noResults.setText(R.string.no_search_results);
            noResults.setVisibility(View.VISIBLE);
            return;
        }
        ArrayList<Cartridge> matches = new ArrayList<>();
        for (Cartridge book : books) {
            String title = book.title.toLowerCase(Locale.ROOT);
            String titleJp = book.titleJp.toLowerCase(Locale.ROOT);
            if (title.contains(query) || titleJp.contains(query)) matches.add(book);
        }
        adapter.replace(matches);
        noResults.setVisibility(matches.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private static int compareById(Cartridge left, Cartridge right) {
        try { return Long.compare(Long.parseLong(right.id), Long.parseLong(left.id)); }
        catch (NumberFormatException ignored) { return right.id.compareToIgnoreCase(left.id); }
    }

    private void open(Cartridge book) {
        Intent intent = new Intent(this, GalleryActivity.class);
        intent.putExtra(GalleryActivity.URI, book.uri.toString());
        intent.putExtra(GalleryActivity.MODIFIED, book.modified);
        startActivity(intent);
    }

}

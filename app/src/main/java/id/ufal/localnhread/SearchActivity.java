package id.ufal.localnhread;

import android.content.Intent;
import android.net.Uri;
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
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SearchActivity extends AppCompatActivity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private BookAdapter adapter;
    private EditText input;
    private TextView noResults;
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
        Button go = findViewById(R.id.goButton);
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
        worker.execute(() -> {
            ArrayList<Cartridge> items = new ArrayList<>();
            DocumentFile root = DocumentFile.fromTreeUri(this, Uri.parse(saved));
            if (root != null && root.canRead()) collect(root, items);
            items.sort(SearchActivity::compareById);
            runOnUiThread(() -> { books = items; filter(); });
        });
    }

    private void collect(DocumentFile directory, List<Cartridge> output) {
        DocumentFile[] children;
        try { children = directory.listFiles(); } catch (Exception ignored) { return; }
        for (DocumentFile child : children) {
            if (child.isDirectory()) collect(child, output);
            else if (child.isFile() && child.getName() != null && child.getName().toLowerCase(Locale.ROOT).endsWith(".lnhc")) {
                try { output.add(CartridgeArchive.inspect(this, child.getUri(), child.lastModified(), false)); }
                catch (Exception ignored) { }
            }
        }
    }

    private void filter() {
        String query = input.getText().toString().trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            adapter.replace(new ArrayList<>());
            noResults.setVisibility(View.VISIBLE);
            return;
        }
        ArrayList<Cartridge> matches = new ArrayList<>();
        for (Cartridge book : books) if (book.title.toLowerCase(Locale.ROOT).contains(query)) matches.add(book);
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

    @Override protected void onDestroy() { worker.shutdownNow(); super.onDestroy(); }
}

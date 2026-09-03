package id.ufal.localnhread;

import android.graphics.Bitmap;
import android.net.Uri;
import java.util.ArrayList;

public final class Cartridge {
    public final Uri uri; public final long modified; public final String id; public final String title;
    public final String titleJp; public final String language; public final String scanlator;
    public final String tagsText; public final String uploaded; public final String archived;
    public final String coverPath; public final ArrayList<String> pages; public final Bitmap cover;
    public Cartridge(Uri uri, long modified, String id, String title, String titleJp, String language,
                     String scanlator, String tagsText, String uploaded, String archived,
                     String coverPath, ArrayList<String> pages, Bitmap cover) {
        this.uri=uri; this.modified=modified; this.id=id; this.title=title; this.titleJp=titleJp;
        this.language=language; this.scanlator=scanlator; this.tagsText=tagsText; this.uploaded=uploaded;
        this.archived=archived; this.coverPath=coverPath; this.pages=pages; this.cover=cover;
    }
}

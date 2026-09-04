package id.ufal.localnhread;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.OpenableColumns;
import androidx.annotation.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/** Reads only the documented .lnhc zip entries; archive content is never unpacked permanently. */
public final class CartridgeArchive {
    private CartridgeArchive() {}
    public static Cartridge inspect(Context context, Uri uri, long modified, boolean thumbnail) throws Exception {
        JSONObject manifest;
        try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(context.getContentResolver().openInputStream(uri)))) {
            manifest = findManifest(zip);
        }
        Cartridge data = fromManifest(uri, modified, manifest, null);
        Bitmap cover = thumbnail && data.coverPath != null ? bitmapFromUri(context, uri, data.coverPath, 360) : null;
        return new Cartridge(uri, modified, data.id, data.title, data.titleJp, data.language, data.scanlator,
                data.tagsText, data.uploaded, data.archived, data.coverPath, data.pages, cover);
    }
    @Nullable public static Bitmap thumbnail(Context context, Uri uri, String entryPath) throws IOException {
        return entryPath == null ? null : bitmapFromUri(context, uri, entryPath, 360);
    }
    public static File cacheArchive(Context context, Uri uri, long modified) throws IOException {
        File folder = new File(context.getCacheDir(), "cartridges");
        if (!folder.exists() && !folder.mkdirs()) throw new IOException("could not create cache");
        File target = new File(folder, Integer.toHexString(uri.toString().hashCode()) + "-" + modified + ".lnhc");
        if (target.isFile()) return target;
        File temporary = new File(folder, target.getName() + ".part");
        try (InputStream in = context.getContentResolver().openInputStream(uri); OutputStream out = new FileOutputStream(temporary)) {
            if (in == null) throw new IOException("could not open cartridge");
            byte[] buffer = new byte[64 * 1024]; int count;
            while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
        }
        if (!temporary.renameTo(target)) throw new IOException("could not cache cartridge");
        return target;
    }
    public static Cartridge readCached(File archive, Uri uri, long modified) throws Exception {
        try (ZipFile zip = new ZipFile(archive)) {
            ZipEntry entry = zip.getEntry("manifest.json");
            if (entry == null) throw new IOException("manifest.json is missing");
            JSONObject manifest = new JSONObject(readText(zip.getInputStream(entry)));
            Cartridge data = fromManifest(uri, modified, manifest, null);
            Bitmap cover = data.coverPath == null ? null : bitmap(zip.getInputStream(zip.getEntry(data.coverPath)), 720);
            return new Cartridge(uri, modified, data.id, data.title, data.titleJp, data.language, data.scanlator,
                    data.tagsText, data.uploaded, data.archived, data.coverPath, data.pages, cover);
        }
    }
    @Nullable public static Bitmap page(File archive, String entryPath, int maximum) throws IOException {
        try (ZipFile zip = new ZipFile(archive)) {
            ZipEntry entry = zip.getEntry(entryPath);
            return entry == null ? null : bitmap(zip.getInputStream(entry), maximum);
        }
    }
    private static JSONObject findManifest(ZipInputStream zip) throws Exception {
        ZipEntry entry;
        while ((entry = zip.getNextEntry()) != null) if ("manifest.json".equals(entry.getName())) return new JSONObject(readText(zip));
        throw new IOException("manifest.json is missing");
    }
    private static Cartridge fromManifest(Uri uri, long modified, JSONObject manifest, Bitmap cover) throws Exception {
        if (!"localnh-cartridge".equals(manifest.optString("format"))) throw new IOException("not a localnh cartridge");
        if (manifest.optInt("format_version", 0) != 1) throw new IOException("unsupported cartridge version");
        JSONObject gallery = manifest.getJSONObject("gallery"), assets = manifest.getJSONObject("assets");
        ArrayList<String> pages = new ArrayList<>(); JSONArray pageArray = assets.optJSONArray("pages");
        if (pageArray != null) for (int i=0; i<pageArray.length(); i++) pages.add(pageArray.getString(i));
        JSONObject tags = gallery.optJSONObject("tags"); StringBuilder tagText = new StringBuilder();
        if (tags != null) for (Iterator<String> keys=tags.keys(); keys.hasNext();) { String key=keys.next(); JSONArray values=tags.optJSONArray(key); if (values != null && values.length()>0) { if (tagText.length()>0) tagText.append("\n"); tagText.append(key).append("   "); for(int i=0;i<values.length();i++) { if(i>0) tagText.append(" "); tagText.append(values.optString(i)); } } }
        return new Cartridge(uri, modified, gallery.optString("id"), gallery.optString("title", gallery.optString("title_jp")), gallery.optString("title_jp"), gallery.optString("language", "unknown"), gallery.optString("scanlator"), tagText.toString(), date(gallery.optJSONObject("uploaded")), date(gallery.optJSONObject("archived")), assets.isNull("cover") ? null : assets.optString("cover", null), pages, cover);
    }
    private static String date(JSONObject object) { return object == null ? "unknown" : object.optString("date", "unknown"); }
    private static Bitmap bitmapFromUri(Context context, Uri uri, String target, int maximum) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(context.getContentResolver().openInputStream(uri)))) {
            ZipEntry entry; while ((entry=zip.getNextEntry()) != null) if (target.equals(entry.getName())) return bitmap(zip, maximum);
            return null;
        }
    }
    @Nullable private static Bitmap bitmap(InputStream input, int maximum) throws IOException {
        if (input == null) return null; byte[] bytes = readBytes(input);
        BitmapFactory.Options bounds = new BitmapFactory.Options(); bounds.inJustDecodeBounds=true; BitmapFactory.decodeByteArray(bytes,0,bytes.length,bounds);
        int sample=1; while(bounds.outWidth/sample>maximum || bounds.outHeight/sample>maximum*2) sample*=2;
        BitmapFactory.Options options = new BitmapFactory.Options(); options.inSampleSize=sample; options.inPreferredConfig=Bitmap.Config.RGB_565;
        return BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
    }
    private static String readText(InputStream input) throws IOException { return new String(readBytes(input), java.nio.charset.StandardCharsets.UTF_8); }
    private static byte[] readBytes(InputStream input) throws IOException { ByteArrayOutputStream out=new ByteArrayOutputStream(); byte[] buffer=new byte[16384]; int count; while((count=input.read(buffer))!=-1) out.write(buffer,0,count); return out.toByteArray(); }
}

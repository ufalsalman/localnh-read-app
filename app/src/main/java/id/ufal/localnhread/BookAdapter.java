package id.ufal.localnhread;
import android.graphics.Bitmap; import android.view.*; import android.widget.*; import androidx.annotation.NonNull; import androidx.recyclerview.widget.RecyclerView; import java.util.*; import java.util.concurrent.*;
public final class BookAdapter extends RecyclerView.Adapter<BookAdapter.Holder> {
    interface Click { void open(Cartridge cartridge); } private final ArrayList<Cartridge> books=new ArrayList<>(); private final Click click; private final ExecutorService thumbnailWorker=Executors.newFixedThreadPool(2);
    BookAdapter(Click click){this.click=click;} void replace(List<Cartridge> items){books.clear();books.addAll(items);notifyDataSetChanged();}
    @NonNull public Holder onCreateViewHolder(@NonNull ViewGroup parent,int type){return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_book,parent,false));}
    public void onBindViewHolder(@NonNull Holder holder,int position){Cartridge c=books.get(position);holder.title.setText(c.title);holder.cover.setImageBitmap(c.cover);holder.cover.setTag(c.uri);if(c.cover==null&&c.coverPath!=null){thumbnailWorker.execute(()->{try{Bitmap bitmap=CartridgeArchive.thumbnail(holder.itemView.getContext(),c.uri,c.coverPath);holder.cover.post(()->{if(c.uri.equals(holder.cover.getTag()))holder.cover.setImageBitmap(bitmap);});}catch(Exception ignored){}});}holder.itemView.setOnClickListener(v->click.open(c));}
    public int getItemCount(){return books.size();} static final class Holder extends RecyclerView.ViewHolder { final ImageView cover; final TextView title; Holder(View v){super(v);cover=v.findViewById(R.id.bookCover);title=v.findViewById(R.id.bookTitle);} }
}

package com.salesmanager.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.VH> {

    public enum Mode {CATALOG, STOCK, SALES}

    public interface Listener {
        void onClick(Product product);
    }

    private final Mode mode;
    private final Listener listener;
    private final List<Product> items = new ArrayList<>();

    public ProductAdapter(Mode mode, Listener listener) {
        this.mode = mode;
        this.listener = listener;
    }

    public void setItems(List<Product> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        final Product p = items.get(position);
        Context ctx = h.itemView.getContext();

        h.name.setText(p.name);
        if (mode == Mode.CATALOG) {
            h.info.setVisibility(View.GONE);
        } else {
            h.info.setVisibility(View.VISIBLE);
            h.info.setText(ctx.getString(R.string.qty_price_fmt, p.quantity, Fmt.money(p.price)));
        }

        Bitmap bmp = ImageUtils.load(p.imagePath, 160);
        if (bmp != null) {
            h.image.setImageBitmap(bmp);
        } else {
            h.image.setImageResource(R.drawable.ic_placeholder);
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(p);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView name;
        final TextView info;

        VH(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.ivImage);
            name = itemView.findViewById(R.id.tvName);
            info = itemView.findViewById(R.id.tvInfo);
        }
    }
}

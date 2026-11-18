package com.salesmanager.app;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class StockActivity extends AppCompatActivity {

    private DbHelper db;
    private ProductAdapter adapter;
    private EditText etSearch;
    private TextView tvEmpty;
    private TextView tvSummary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = DbHelper.get(this);
        etSearch = findViewById(R.id.etSearch);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvSummary = findViewById(R.id.tvSummary);
        tvEmpty.setText(R.string.empty_stock);
        tvSummary.setVisibility(View.VISIBLE);

        RecyclerView recycler = findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductAdapter(ProductAdapter.Mode.STOCK, this::showEditDialog);
        recycler.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                refresh();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void refresh() {
        List<Product> list = db.getProducts(etSearch.getText().toString(), false);
        adapter.setItems(list);
        tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        tvSummary.setText(getString(R.string.stock_summary_fmt, db.countProducts(), db.totalQuantity()));
    }

    private void showEditDialog(final Product p) {
        View view = getLayoutInflater().inflate(R.layout.dialog_qty_price, null);
        final EditText etQty = view.findViewById(R.id.etQty);
        final EditText etPrice = view.findViewById(R.id.etPrice);
        TextView tvHint = view.findViewById(R.id.tvHint);

        tvHint.setVisibility(View.VISIBLE);
        tvHint.setText(p.name);
        etQty.setText(String.valueOf(p.quantity));
        etPrice.setText(String.format(Locale.US, "%.2f", p.price));

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.edit_stock)
                .setView(view)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    int qty = Fmt.parseInt(etQty.getText().toString());
                    double price = Fmt.parseDouble(etPrice.getText().toString());
                    if (qty < 0 || Double.isNaN(price) || price < 0) {
                        Toast.makeText(this, R.string.invalid_input, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    db.updateStock(p.id, qty, price);
                    dialog.dismiss();
                    refresh();
                    Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
                }));
        dialog.show();
    }
}

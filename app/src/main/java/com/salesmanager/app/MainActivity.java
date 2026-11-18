package com.salesmanager.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private DbHelper db;
    private TextView tvProducts;
    private TextView tvQty;
    private TextView tvTodaySales;
    private TextView tvOpening;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(R.string.app_name);

        db = DbHelper.get(this);
        tvProducts = findViewById(R.id.tvProducts);
        tvQty = findViewById(R.id.tvQty);
        tvTodaySales = findViewById(R.id.tvTodaySales);
        tvOpening = findViewById(R.id.tvOpening);

        findViewById(R.id.tileCatalog).setOnClickListener(
                v -> startActivity(new Intent(this, CatalogActivity.class)));
        findViewById(R.id.tileStock).setOnClickListener(
                v -> startActivity(new Intent(this, StockActivity.class)));
        findViewById(R.id.tileSales).setOnClickListener(
                v -> startActivity(new Intent(this, SalesActivity.class)));
        findViewById(R.id.tileBalance).setOnClickListener(
                v -> startActivity(new Intent(this, OpeningBalanceActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        String cur = " " + getString(R.string.currency);
        tvProducts.setText(String.valueOf(db.countProducts()));
        tvQty.setText(String.valueOf(db.totalQuantity()));
        tvTodaySales.setText(Fmt.money(db.salesTotal(Fmt.today())) + cur);
        Double balance = db.getLatestOpeningBalance(Fmt.today());
        tvOpening.setText(balance == null ? "-" : Fmt.money(balance) + cur);
    }
}
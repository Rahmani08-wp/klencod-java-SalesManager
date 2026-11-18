package com.salesmanager.app;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class OpeningBalanceActivity extends AppCompatActivity {

    private DbHelper db;
    private EditText etDate;
    private EditText etAmount;
    private TextView tvEmptyHistory;

    private final List<DbHelper.BalanceEntry> entries = new ArrayList<>();
    private final List<String> lines = new ArrayList<>();
    private ArrayAdapter<String> listAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opening_balance);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = DbHelper.get(this);
        etDate = findViewById(R.id.etDate);
        etAmount = findViewById(R.id.etAmount);
        tvEmptyHistory = findViewById(R.id.tvEmptyHistory);
        ListView listHistory = findViewById(R.id.listHistory);

        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, lines);
        listHistory.setAdapter(listAdapter);
        listHistory.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < entries.size()) {
                etDate.setText(entries.get(position).date);
                loadForDate();
            }
        });

        etDate.setText(Fmt.today());
        etDate.setOnClickListener(v -> pickDate());

        findViewById(R.id.btnSave).setOnClickListener(v -> save());

        loadForDate();
        refreshHistory();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void pickDate() {
        Calendar cal = Calendar.getInstance();
        String[] parts = etDate.getText().toString().split("-");
        if (parts.length == 3) {
            try {
                cal.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) - 1,
                        Integer.parseInt(parts[2]));
            } catch (NumberFormatException ignored) {
            }
        }
        new DatePickerDialog(this, (view, year, month, day) -> {
            etDate.setText(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day));
            loadForDate();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void loadForDate() {
        Double amount = db.getOpeningBalance(etDate.getText().toString());
        etAmount.setText(amount == null ? "" : String.format(Locale.US, "%.2f", amount));
    }

    private void save() {
        double amount = Fmt.parseDouble(etAmount.getText().toString());
        if (Double.isNaN(amount) || amount < 0) {
            Toast.makeText(this, R.string.invalid_input, Toast.LENGTH_SHORT).show();
            return;
        }
        db.saveOpeningBalance(etDate.getText().toString(), amount);
        refreshHistory();
        Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
    }

    private void refreshHistory() {
        entries.clear();
        entries.addAll(db.getBalanceHistory());
        lines.clear();
        String cur = getString(R.string.currency);
        for (DbHelper.BalanceEntry e : entries) {
            lines.add(e.date + "   —   " + Fmt.money(e.amount) + " " + cur);
        }
        listAdapter.notifyDataSetChanged();
        tvEmptyHistory.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
    }
}

package com.salesmanager.app;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class CatalogActivity extends AppCompatActivity {

    private DbHelper db;
    private ProductAdapter adapter;
    private EditText etSearch;
    private TextView tvEmpty;

    private String pendingImagePath;
    private ImageView dialogImage;

    private final ActivityResultLauncher<String> imagePicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                String path = ImageUtils.saveFromUri(this, uri);
                if (path == null) {
                    Toast.makeText(this, R.string.image_failed, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (pendingImagePath != null && !pendingImagePath.equals(path)) {
                    // A previously picked (unsaved) image is no longer needed.
                    // Images already attached to a saved product are cleaned up on save.
                }
                pendingImagePath = path;
                if (dialogImage != null) {
                    Bitmap b = ImageUtils.load(path, 400);
                    if (b != null) dialogImage.setImageBitmap(b);
                }
            });

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
        tvEmpty.setText(R.string.empty_catalog);

        RecyclerView recycler = findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductAdapter(ProductAdapter.Mode.CATALOG, this::showProductDialog);
        recycler.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setVisibility(View.VISIBLE);
        fab.setOnClickListener(v -> showProductDialog(null));

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
    }

    private void showProductDialog(final Product existing) {
        View view = getLayoutInflater().inflate(R.layout.dialog_product, null);
        dialogImage = view.findViewById(R.id.ivDialogImage);
        final EditText etName = view.findViewById(R.id.etName);
        Button btnPick = view.findViewById(R.id.btnPickImage);

        pendingImagePath = existing != null ? existing.imagePath : null;
        if (existing != null) etName.setText(existing.name);

        Bitmap current = ImageUtils.load(pendingImagePath, 400);
        if (current != null) {
            dialogImage.setImageBitmap(current);
        }

        btnPick.setOnClickListener(v -> imagePicker.launch("image/*"));

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(existing == null ? R.string.add_item : R.string.edit_item)
                .setView(view)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, null);
        if (existing != null) {
            builder.setNeutralButton(R.string.delete, null);
        }

        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = etName.getText().toString().trim();
                if (name.isEmpty()) {
                    Toast.makeText(this, R.string.name_required, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (existing == null) {
                    db.addProduct(name, pendingImagePath);
                } else {
                    if (existing.imagePath != null && pendingImagePath != null
                            && !existing.imagePath.equals(pendingImagePath)) {
                        ImageUtils.deleteFile(existing.imagePath);
                    }
                    db.updateProduct(existing.id, name, pendingImagePath);
                }
                dialog.dismiss();
                refresh();
            });

            if (existing != null) {
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v ->
                        new AlertDialog.Builder(this)
                                .setMessage(R.string.confirm_delete)
                                .setPositiveButton(R.string.delete, (dd, which) -> {
                                    db.deleteProduct(existing.id);
                                    ImageUtils.deleteFile(existing.imagePath);
                                    dialog.dismiss();
                                    refresh();
                                })
                                .setNegativeButton(R.string.cancel, null)
                                .show());
            }
        });
        dialog.setOnDismissListener(d -> dialogImage = null);
        dialog.show();
    }
}

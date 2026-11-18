package com.salesmanager.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "sales_manager.db";
    private static final int DB_VERSION = 1;

    private static DbHelper instance;

    public static synchronized DbHelper get(Context context) {
        if (instance == null) {
            instance = new DbHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE products ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, "
                + "image_path TEXT, "
                + "price REAL NOT NULL DEFAULT 0, "
                + "quantity INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE TABLE sales ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "product_id INTEGER NOT NULL, "
                + "product_name TEXT NOT NULL, "
                + "quantity INTEGER NOT NULL, "
                + "unit_price REAL NOT NULL, "
                + "total REAL NOT NULL, "
                + "sale_date TEXT NOT NULL, "
                + "sale_time TEXT NOT NULL)");
        db.execSQL("CREATE TABLE opening_balance ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "balance_date TEXT NOT NULL UNIQUE, "
                + "amount REAL NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // No migrations yet.
    }

    // ---------------------------------------------------------------- products

    public long addProduct(String name, String imagePath) {
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("image_path", imagePath);
        cv.put("price", 0);
        cv.put("quantity", 0);
        return getWritableDatabase().insert("products", null, cv);
    }

    public void updateProduct(long id, String name, String imagePath) {
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("image_path", imagePath);
        getWritableDatabase().update("products", cv, "id=?", new String[]{String.valueOf(id)});
    }

    public void deleteProduct(long id) {
        getWritableDatabase().delete("products", "id=?", new String[]{String.valueOf(id)});
    }

    public void updateStock(long id, int quantity, double price) {
        ContentValues cv = new ContentValues();
        cv.put("quantity", quantity);
        cv.put("price", price);
        getWritableDatabase().update("products", cv, "id=?", new String[]{String.valueOf(id)});
    }

    public List<Product> getProducts(String query, boolean onlyInStock) {
        List<Product> list = new ArrayList<>();
        StringBuilder where = new StringBuilder("1=1");
        List<String> args = new ArrayList<>();
        if (query != null && !query.trim().isEmpty()) {
            where.append(" AND name LIKE ?");
            args.add("%" + query.trim() + "%");
        }
        if (onlyInStock) {
            where.append(" AND quantity > 0");
        }
        Cursor c = getReadableDatabase().query("products", null, where.toString(),
                args.toArray(new String[0]), null, null, "name COLLATE NOCASE ASC");
        try {
            while (c.moveToNext()) {
                Product p = new Product();
                p.id = c.getLong(c.getColumnIndexOrThrow("id"));
                p.name = c.getString(c.getColumnIndexOrThrow("name"));
                p.imagePath = c.getString(c.getColumnIndexOrThrow("image_path"));
                p.price = c.getDouble(c.getColumnIndexOrThrow("price"));
                p.quantity = c.getInt(c.getColumnIndexOrThrow("quantity"));
                list.add(p);
            }
        } finally {
            c.close();
        }
        return list;
    }

    // ------------------------------------------------------------------- sales

    /** Records a sale and decreases the stock in one transaction. Returns false if stock is not enough. */
    public boolean sell(long productId, int qty, double price) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            String name;
            int stock;
            Cursor c = db.query("products", new String[]{"name", "quantity"}, "id=?",
                    new String[]{String.valueOf(productId)}, null, null, null);
            try {
                if (!c.moveToFirst()) return false;
                name = c.getString(0);
                stock = c.getInt(1);
            } finally {
                c.close();
            }
            if (qty <= 0 || qty > stock) return false;

            ContentValues pv = new ContentValues();
            pv.put("quantity", stock - qty);
            db.update("products", pv, "id=?", new String[]{String.valueOf(productId)});

            ContentValues sv = new ContentValues();
            sv.put("product_id", productId);
            sv.put("product_name", name);
            sv.put("quantity", qty);
            sv.put("unit_price", price);
            sv.put("total", qty * price);
            sv.put("sale_date", Fmt.today());
            sv.put("sale_time", Fmt.nowTime());
            db.insert("sales", null, sv);

            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    public double salesTotal(String date) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(total), 0) FROM sales WHERE sale_date=?", new String[]{date});
        try {
            c.moveToFirst();
            return c.getDouble(0);
        } finally {
            c.close();
        }
    }

    // ------------------------------------------------------------------- stats

    public int countProducts() {
        return (int) DatabaseUtils.queryNumEntries(getReadableDatabase(), "products");
    }

    public int totalQuantity() {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(quantity), 0) FROM products", null);
        try {
            c.moveToFirst();
            return c.getInt(0);
        } finally {
            c.close();
        }
    }

    // --------------------------------------------------------- opening balance

    public static class BalanceEntry {
        public String date;
        public double amount;
    }

    public void saveOpeningBalance(String date, double amount) {
        ContentValues cv = new ContentValues();
        cv.put("balance_date", date);
        cv.put("amount", amount);
        getWritableDatabase().insertWithOnConflict("opening_balance", null, cv,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    /** Balance saved exactly for this date, or null. */
    public Double getOpeningBalance(String date) {
        Cursor c = getReadableDatabase().query("opening_balance", new String[]{"amount"},
                "balance_date=?", new String[]{date}, null, null, null);
        try {
            if (c.moveToFirst()) return c.getDouble(0);
            return null;
        } finally {
            c.close();
        }
    }

    /** Most recent balance saved on or before this date, or null. */
    public Double getLatestOpeningBalance(String upToDate) {
        Cursor c = getReadableDatabase().query("opening_balance", new String[]{"amount"},
                "balance_date<=?", new String[]{upToDate}, null, null, "balance_date DESC", "1");
        try {
            if (c.moveToFirst()) return c.getDouble(0);
            return null;
        } finally {
            c.close();
        }
    }

    public List<BalanceEntry> getBalanceHistory() {
        List<BalanceEntry> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query("opening_balance",
                new String[]{"balance_date", "amount"}, null, null, null, null, "balance_date DESC");
        try {
            while (c.moveToNext()) {
                BalanceEntry e = new BalanceEntry();
                e.date = c.getString(0);
                e.amount = c.getDouble(1);
                list.add(e);
            }
        } finally {
            c.close();
        }
        return list;
    }
}

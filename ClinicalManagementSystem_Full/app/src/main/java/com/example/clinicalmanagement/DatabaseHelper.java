package com.example.clinicalmanagement;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "clinical_management.db";
    private static final int DB_VERSION = 2;

    public DatabaseHelper(Context context) { super(context, DB_NAME, null, DB_VERSION); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE patients (id INTEGER PRIMARY KEY AUTOINCREMENT, full_name TEXT NOT NULL, age INTEGER NOT NULL DEFAULT 0, gender TEXT, phone TEXT, created_at TEXT DEFAULT CURRENT_TIMESTAMP)");
        db.execSQL("CREATE TABLE appointments (id INTEGER PRIMARY KEY AUTOINCREMENT, patient_id INTEGER, patient_name TEXT NOT NULL, appointment_date TEXT NOT NULL, reason TEXT, status TEXT NOT NULL DEFAULT 'Scheduled', created_at TEXT DEFAULT CURRENT_TIMESTAMP)");
        db.execSQL("CREATE TABLE clinical_notes (id INTEGER PRIMARY KEY AUTOINCREMENT, patient_id INTEGER NOT NULL, note TEXT NOT NULL, created_at TEXT DEFAULT CURRENT_TIMESTAMP)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE appointments ADD COLUMN patient_id INTEGER");
            db.execSQL("ALTER TABLE appointments ADD COLUMN status TEXT NOT NULL DEFAULT 'Scheduled'");
            db.execSQL("ALTER TABLE appointments ADD COLUMN created_at TEXT");
            db.execSQL("ALTER TABLE patients ADD COLUMN created_at TEXT");
        }
    }

    public long savePatient(long id, String name, int age, String gender, String phone) {
        ContentValues v = new ContentValues();
        v.put("full_name", name); v.put("age", age); v.put("gender", gender); v.put("phone", phone);
        if (id > 0) return getWritableDatabase().update("patients", v, "id=?", new String[]{String.valueOf(id)});
        return getWritableDatabase().insert("patients", null, v);
    }
    public int deletePatient(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("clinical_notes", "patient_id=?", new String[]{String.valueOf(id)});
        db.delete("appointments", "patient_id=?", new String[]{String.valueOf(id)});
        return db.delete("patients", "id=?", new String[]{String.valueOf(id)});
    }
    public Cursor searchPatients(String query) {
        String q = "%" + query.trim() + "%";
        return getReadableDatabase().rawQuery(
            "SELECT id,full_name,age,gender,phone FROM patients WHERE full_name LIKE ? OR phone LIKE ? ORDER BY full_name",
            new String[]{q,q});
    }
    public Cursor getPatient(long id) {
        return getReadableDatabase().rawQuery("SELECT id,full_name,age,gender,phone FROM patients WHERE id=?",
            new String[]{String.valueOf(id)});
    }
    public int patientCount() {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM patients", null)) { c.moveToFirst(); return c.getInt(0); }
    }
    public int appointmentCount(String status) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM appointments WHERE status=?", new String[]{status})) { c.moveToFirst(); return c.getInt(0); }
    }
    public long saveAppointment(long id, long patientId, String patientName, String date, String reason, String status) {
        ContentValues v = new ContentValues();
        v.put("patient_id", patientId > 0 ? patientId : null);
        v.put("patient_name", patientName); v.put("appointment_date", date);
        v.put("reason", reason); v.put("status", status);
        if (id > 0) return getWritableDatabase().update("appointments", v, "id=?", new String[]{String.valueOf(id)});
        return getWritableDatabase().insert("appointments", null, v);
    }
    public int deleteAppointment(long id) { return getWritableDatabase().delete("appointments", "id=?", new String[]{String.valueOf(id)}); }
    public Cursor searchAppointments(String query) {
        String q = "%" + query.trim() + "%";
        return getReadableDatabase().rawQuery(
            "SELECT id,patient_id,patient_name,appointment_date,reason,status FROM appointments WHERE patient_name LIKE ? OR appointment_date LIKE ? OR reason LIKE ? ORDER BY appointment_date DESC",
            new String[]{q,q,q});
    }
    public long addNote(long patientId, String note) {
        ContentValues v = new ContentValues(); v.put("patient_id", patientId); v.put("note", note);
        return getWritableDatabase().insert("clinical_notes", null, v);
    }
    public Cursor getNotes(long patientId) {
        return getReadableDatabase().rawQuery(
            "SELECT id,note,created_at FROM clinical_notes WHERE patient_id=? ORDER BY id DESC",
            new String[]{String.valueOf(patientId)});
    }
    public int deleteNote(long id) { return getWritableDatabase().delete("clinical_notes", "id=?", new String[]{String.valueOf(id)}); }
}

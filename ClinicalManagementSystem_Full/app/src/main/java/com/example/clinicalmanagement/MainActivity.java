package com.example.clinicalmanagement;

import android.app.Activity;
import android.app.AlertDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private DatabaseHelper db;
    private LinearLayout content;
    private final int navy = 0xFF15324B, teal = 0xFF167D8D, ink = 0xFF202833;
    private int pad(int dp) { return (int)(dp * getResources().getDisplayMetrics().density); }

    @Override public void onCreate(Bundle b) { super.onCreate(b); db = new DatabaseHelper(this); showDashboard(); }
    private TextView text(String s, int size) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(ink);
        t.setPadding(pad(8),pad(8),pad(8),pad(8)); return t;
    }
    private Button button(String label, Runnable action) {
        Button b = new Button(this); b.setText(label); b.setAllCaps(false);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(teal)); b.setTextColor(-1);
        b.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,pad(4),0,pad(4)); b.setLayoutParams(p);
        return b;
    }
    private EditText field(String hint, int type) {
        EditText e = new EditText(this); e.setHint(hint); e.setSingleLine(true); e.setInputType(type);
        e.setPadding(pad(12),pad(8),pad(12),pad(8)); return e;
    }
    private void startScreen(String title) {
        LinearLayout root = new LinearLayout(this); root.setOrientation(1); root.setBackgroundColor(0xFFF5F7FA);
        TextView header = text(title,22); header.setTextColor(-1); header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(pad(18),pad(18),pad(18),pad(18)); header.setBackgroundColor(navy); root.addView(header);
        ScrollView scroll = new ScrollView(this); content = new LinearLayout(this); content.setOrientation(1);
        content.setPadding(pad(16),pad(12),pad(16),pad(18)); scroll.addView(content);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        Button home = new Button(this); home.setText("Home / Dashboard"); home.setAllCaps(false); home.setOnClickListener(v -> showDashboard()); root.addView(home);
        setContentView(root);
    }
    private void toast(String s) { Toast.makeText(this,s,Toast.LENGTH_SHORT).show(); }
    private String safe(String s) { return s == null || s.trim().isEmpty() ? "Not provided" : s; }
    private void showDashboard() {
        startScreen("Clinical Management System");
        content.addView(text("Dashboard",26));
        content.addView(text("Patient records: " + db.patientCount(),18));
        content.addView(text("Scheduled appointments: " + db.appointmentCount("Scheduled"),18));
        content.addView(text("Completed appointments: " + db.appointmentCount("Completed"),18));
        content.addView(button("Register a patient", () -> patientForm(0)));
        content.addView(button("Search / manage patients", () -> showPatients("")));
        content.addView(button("Add an appointment", () -> appointmentForm(0,0,"","","","Scheduled")));
        content.addView(button("Search / manage appointments", () -> showAppointments("")));
        content.addView(button("About and privacy", this::showAbout));
    }

    private void patientForm(long id) {
        String oldName="", oldAge="", oldGender="", oldPhone="";
        if (id > 0) try (Cursor c=db.getPatient(id)) { if(c.moveToFirst()){oldName=c.getString(1);oldAge=String.valueOf(c.getInt(2));oldGender=safe(c.getString(3));oldPhone=safe(c.getString(4));} }
        final String n0=oldName,a0=oldAge,g0=oldGender,p0=oldPhone;
        startScreen(id>0?"Edit Patient":"Register Patient");
        EditText name=field("Patient full name",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_WORDS); name.setText(n0);
        EditText age=field("Age",InputType.TYPE_CLASS_NUMBER); age.setText(a0);
        EditText gender=field("Gender (optional)",InputType.TYPE_CLASS_TEXT); gender.setText(g0.equals("Not provided")?"":g0);
        EditText phone=field("Phone number (optional)",InputType.TYPE_CLASS_PHONE); phone.setText(p0.equals("Not provided")?"":p0);
        content.addView(name);content.addView(age);content.addView(gender);content.addView(phone);
        content.addView(button("Save patient",()->{
            String nm=name.getText().toString().trim(), ag=age.getText().toString().trim();
            if(nm.isEmpty()){toast("Enter the patient's name");return;}
            int years=0; try{if(!ag.isEmpty()) years=Integer.parseInt(ag);}catch(Exception ex){toast("Age must be a whole number");return;}
            if(years<0||years>130){toast("Enter an age between 0 and 130");return;}
            long result=db.savePatient(id,nm,years,gender.getText().toString().trim(),phone.getText().toString().trim());
            if(result>=0){toast("Patient saved");showPatients("");}else toast("Could not save patient");
        }));
        if(id>0) content.addView(button("Clinical notes",()->showNotes(id,n0)));
    }

    private void showPatients(String initial) {
        startScreen("Patient Records");
        EditText search=field("Search by patient name or phone",InputType.TYPE_CLASS_TEXT); search.setText(initial); content.addView(search);
        content.addView(button("Search",()->showPatients(search.getText().toString())));
        try(Cursor c=db.searchPatients(initial)){
            if(c.getCount()==0) content.addView(text("No matching patients.",16));
            while(c.moveToNext()){
                long id=c.getLong(0); String name=c.getString(1);
                content.addView(text("Patient #"+id+"\nName: "+name+"\nAge: "+c.getInt(2)+"\nGender: "+safe(c.getString(3))+"\nPhone: "+safe(c.getString(4)),16));
                content.addView(button("Edit / clinical notes",()->patientForm(id)));
                content.addView(button("Delete patient",()->new AlertDialog.Builder(this).setTitle("Delete patient?")
                    .setMessage("This also deletes linked appointments and clinical notes.").setNegativeButton("Cancel",null)
                    .setPositiveButton("Delete",(d,w)->{db.deletePatient(id);showPatients("");}).show()));
                content.addView(text("────────────────────────",12));
            }
        }
        content.addView(button("Register a patient",()->patientForm(0)));
    }

    private void appointmentForm(long id,long patientId,String oldName,String oldDate,String oldReason,String oldStatus) {
        startScreen(id>0?"Edit Appointment":"Add Appointment");
        EditText patient=field("Patient name (required)",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_WORDS); patient.setText(oldName);
        EditText date=field("Date and time: YYYY-MM-DD HH:MM",InputType.TYPE_CLASS_DATETIME); date.setText(oldDate);
        EditText reason=field("Reason for appointment",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES); reason.setText(oldReason);
        content.addView(patient);content.addView(date);content.addView(reason);
        content.addView(text("Status",16));
        content.addView(button("Set status: Scheduled",()->appointmentForm(id,patientId,patient.getText().toString(),date.getText().toString(),reason.getText().toString(),"Scheduled")));
        content.addView(button("Set status: Completed",()->appointmentForm(id,patientId,patient.getText().toString(),date.getText().toString(),reason.getText().toString(),"Completed")));
        content.addView(button("Set status: Cancelled",()->appointmentForm(id,patientId,patient.getText().toString(),date.getText().toString(),reason.getText().toString(),"Cancelled")));
        content.addView(text("Current status: "+oldStatus,14));
        content.addView(button("Save appointment",()->{
            String p=patient.getText().toString().trim(), dt=date.getText().toString().trim();
            if(p.isEmpty()||dt.isEmpty()){toast("Patient name and date/time are required");return;}
            if(!dt.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}")){toast("Use date/time format YYYY-MM-DD HH:MM");return;}
            long result=db.saveAppointment(id,patientId,p,dt,reason.getText().toString().trim(),oldStatus);
            if(result>=0){toast("Appointment saved");showAppointments("");}else toast("Could not save appointment");
        }));
    }

    private void showAppointments(String initial) {
        startScreen("Appointments");
        EditText search=field("Search patient, date or reason",InputType.TYPE_CLASS_TEXT); search.setText(initial); content.addView(search);
        content.addView(button("Search",()->showAppointments(search.getText().toString())));
        try(Cursor c=db.searchAppointments(initial)){
            if(c.getCount()==0) content.addView(text("No matching appointments.",16));
            while(c.moveToNext()){
                long id=c.getLong(0), pid=c.isNull(1)?0:c.getLong(1);
                String name=c.getString(2),date=c.getString(3),reason=c.getString(4),status=c.getString(5);
                content.addView(text("Appointment #"+id+"\nPatient: "+name+"\nDate/time: "+date+"\nReason: "+safe(reason)+"\nStatus: "+status,16));
                content.addView(button("Edit appointment",()->appointmentForm(id,pid,name,date,reason,status)));
                content.addView(button("Delete appointment",()->new AlertDialog.Builder(this).setTitle("Delete appointment?")
                    .setNegativeButton("Cancel",null).setPositiveButton("Delete",(d,w)->{db.deleteAppointment(id);showAppointments("");}).show()));
                content.addView(text("────────────────────────",12));
            }
        }
        content.addView(button("Add appointment",()->appointmentForm(0,0,"","","","Scheduled")));
    }

    private void showNotes(long patientId,String patientName) {
        startScreen("Clinical Notes — "+patientName);
        EditText note=new EditText(this); note.setHint("Enter clinical note (fictional data only)");
        note.setMinLines(3); note.setGravity(Gravity.TOP); content.addView(note);
        content.addView(button("Save note",()->{
            String value=note.getText().toString().trim();
            if(value.isEmpty()){toast("Enter a note");return;}
            if(db.addNote(patientId,value)>0){toast("Note saved");showNotes(patientId,patientName);}else toast("Could not save note");
        }));
        try(Cursor c=db.getNotes(patientId)){
            if(c.getCount()==0) content.addView(text("No notes recorded.",16));
            while(c.moveToNext()){
                long id=c.getLong(0);
                content.addView(text(c.getString(2)+"\n"+c.getString(1),16));
                content.addView(button("Delete note",()->new AlertDialog.Builder(this).setTitle("Delete note?")
                    .setNegativeButton("Cancel",null).setPositiveButton("Delete",(d,w)->{db.deleteNote(id);showNotes(patientId,patientName);}).show()));
            }
        }
    }

    private void showAbout() {
        startScreen("About and Privacy");
        content.addView(text("Clinical Management System v2.0",23));
        content.addView(text("Student prototype with patient CRUD, search, appointment management, clinical notes, dashboard counts, validation and local SQLite persistence.",16));
        content.addView(text("Important: this is not a production clinical system. It has no user authentication, encryption, audit logging, cloud backup, or medical compliance review. Use fictional patient data only.",16));
    }
    @Override protected void onDestroy(){if(db!=null)db.close();super.onDestroy();}
}

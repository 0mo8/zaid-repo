package com.zaid.pocketledger;

import android.app.*;
import android.os.Bundle;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

public class MainActivity extends Activity {
    private final int green = Color.rgb(23,107,91), ink = Color.rgb(31,48,45), muted = Color.rgb(111,128,123), bg = Color.rgb(245,247,246), line = Color.rgb(228,234,231);
    private DB db;
    private LinearLayout page;
    private String selectedMonth = YearMonth.now().toString();
    private boolean rangeMode = false;
    private String rangeFrom = LocalDate.now().withDayOfMonth(1).toString(), rangeTo = LocalDate.now().toString();
    private final List<String> months = new ArrayList<>();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state); getWindow().setStatusBarColor(Color.rgb(18,63,56)); getWindow().setNavigationBarColor(bg); getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        db = new DB(this); showHome();
    }

    private String money(long n) { return NumberFormat.getIntegerInstance(new Locale("ar", "IQ")).format(n) + " د.ع"; }
    private long parseAmount(String value) throws NumberFormatException { StringBuilder digits=new StringBuilder(); for(int i=0;i<value.length();i++){char ch=value.charAt(i);if(ch>='0'&&ch<='9')digits.append(ch);else if(ch>='\u0660'&&ch<='\u0669')digits.append((char)('0'+ch-'\u0660'));else if(ch>='\u06f0'&&ch<='\u06f9')digits.append((char)('0'+ch-'\u06f0'));} if(digits.length()==0)throw new NumberFormatException();return Long.parseLong(digits.toString()); }
    private String monthLabel(String key) { try { YearMonth m=YearMonth.parse(key); return String.format(new Locale("ar","IQ"), "%02d / %d",m.getMonthValue(),m.getYear()); } catch(Exception e){return key;} }
    private TextView text(String s, int size, int color, boolean bold) { TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); if(bold)t.setTypeface(null,Typeface.BOLD); t.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL); t.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); return t; }
    private LinearLayout column() { LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); return l; }
    private LinearLayout row() { LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); return l; }
    private GradientDrawable shape(int color, int radius) { GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius); return d; }
    private LinearLayout card() { LinearLayout c=column(); c.setPadding(dp(16),dp(14),dp(16),dp(14)); c.setBackground(shape(Color.WHITE,dp(16))); return c; }
    private int dp(int v) { return (int)(v*getResources().getDisplayMetrics().density+.5f); }
    private LinearLayout.LayoutParams lp(int w,int h) { return new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h)); }
    private void gap(LinearLayout l,int h) { View v=new View(this); l.addView(v,lp(1,h)); }
    private Button button(String label, Runnable action, boolean primary) { Button b=new Button(this); b.setText(label); b.setTextSize(14); b.setAllCaps(false); b.setTextColor(primary?Color.WHITE:green); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primary?green:Color.rgb(232,242,238))); b.setOnClickListener(v->action.run()); return b; }
    private void initPage(String title, boolean back) {
        page=column(); page.setBackgroundColor(bg); page.setPadding(dp(16),dp(12),dp(16),dp(10));
        LinearLayout head=row(); if(back) head.addView(button("رجوع",this::showHome,false),lp(-2,48));
        TextView h=text(title,22,ink,true); LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(0,dp(52),1); h.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL); head.addView(h,hp); page.addView(head,lp(-1,52));
        ScrollView sc=new ScrollView(this); sc.setFillViewport(false); sc.addView(page); setContentView(sc);
    }
    private void showHome() {
        initPage("مصاريفي",false);
        LinearLayout actions=row(); actions.addView(button("الخزنة والدخل",this::showIncome,true),new LinearLayout.LayoutParams(0,dp(48),1)); actions.addView(button("تصنيفات المصروفات",this::showCategories,false),new LinearLayout.LayoutParams(0,dp(48),1)); page.addView(actions,lp(-1,50)); gap(page,8);
        LinearLayout filter=card(); filter.addView(text("اختر الشهر",13,muted,false));
        months.clear(); YearMonth now=YearMonth.now(); for(int i=23;i>=0;i--) months.add(now.minusMonths(i).toString());
        Spinner sp=new Spinner(this); ArrayAdapter<String> ad=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,displayMonths()); ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); sp.setAdapter(ad); int idx=months.indexOf(selectedMonth); sp.setSelection(Math.max(0,idx));
        final boolean[] firstSelection={true};
        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){ public void onNothingSelected(AdapterView<?> p){} public void onItemSelected(AdapterView<?> p,View v,int pos,long id){if(firstSelection[0]){firstSelection[0]=false;return;}String m=months.get(pos);if(!m.equals(selectedMonth)){selectedMonth=m;rangeMode=false;showHome();}}});
        filter.addView(sp,lp(-1,48));
        LinearLayout period=row(); period.addView(button("اختيار فترة",this::chooseRange,false),new LinearLayout.LayoutParams(0,dp(44),1)); TextView range=text(rangeMode?(rangeFrom+" — "+rangeTo):"عرض حسب الشهر",12,muted,false); range.setGravity(Gravity.CENTER); period.addView(range,new LinearLayout.LayoutParams(0,dp(40),1)); if(rangeMode)period.addView(button("عرض الشهر",()->{rangeMode=false;showHome();},false),new LinearLayout.LayoutParams(0,dp(44),1));
        filter.addView(period); page.addView(filter,lp(-1,-2)); gap(page,12);
        long vault=db.starting()+db.sum("incomes",null,null)-db.sum("expenses",null,null); long income=db.sum("incomes","substr(date,1,7)=?",new String[]{selectedMonth}); long monthSpent=db.sum("expenses","substr(date,1,7)=?",new String[]{selectedMonth}); long visibleSpent=rangeMode?db.sum("expenses","date>=? AND date<=?",new String[]{rangeFrom,rangeTo}):monthSpent;
        LinearLayout hero=card(); hero.setBackground(shape(green,dp(18))); hero.addView(text("رصيد الخزنة الكلي",14,Color.rgb(218,239,230),false)); gap(hero,4); hero.addView(text(money(vault),27,Color.WHITE,true)); page.addView(hero,lp(-1,-2)); gap(page,10);
        LinearLayout stats=row(); stats.addView(statCard("دخل الشهر",money(income)),new LinearLayout.LayoutParams(0,dp(92),1)); stats.addView(statCard(rangeMode?"مصروف الفترة":"مصروف الشهر",money(visibleSpent)),new LinearLayout.LayoutParams(0,dp(92),1)); stats.addView(statCard("المتبقي من الدخل",money(income-monthSpent)),new LinearLayout.LayoutParams(0,dp(92),1)); page.addView(stats,lp(-1,-2)); gap(page,14);
        LinearLayout expHead=row(); expHead.addView(text(rangeMode?"مصروفات الفترة المحددة":"مصروفات "+monthLabel(selectedMonth),17,ink,true),new LinearLayout.LayoutParams(0,dp(42),1)); expHead.addView(button("+ إضافة مصروف",this::showNewExpense,true),lp(-2,44)); page.addView(expHead,lp(-1,48));
        renderExpenses();
    }
    private List<String> displayMonths(){List<String> a=new ArrayList<>();for(String m:months)a.add(monthLabel(m));return a;}
    private LinearLayout statCard(String label,String value){LinearLayout c=card();c.setPadding(dp(10),dp(10),dp(10),dp(10)); c.addView(text(label,11,muted,false));gap(c,5);c.addView(text(value,13,ink,true));return c;}
    private void renderExpenses(){String where=rangeMode?"date>=? AND date<=?":"substr(date,1,7)=?";String[] args=rangeMode?new String[]{rangeFrom,rangeTo}:new String[]{selectedMonth};
        Cursor c=db.get().rawQuery("SELECT e.id,e.amount,e.date,e.notes,COALESCE(cat.name,'بدون تصنيف') FROM expenses e LEFT JOIN categories cat ON cat.id=e.category_id WHERE "+where+" ORDER BY e.date DESC,e.id DESC",args);
        if(!c.moveToFirst()){LinearLayout empty=card();empty.addView(text("لا توجد مصروفات في هذه الفترة",14,muted,false));page.addView(empty,lp(-1,-2));c.close();return;}
        do{long id=c.getLong(0),amount=c.getLong(1);String date=c.getString(2),notes=c.getString(3),cat=c.getString(4);LinearLayout item=card();LinearLayout r=row();TextView title=text(cat,15,ink,true);r.addView(title,new LinearLayout.LayoutParams(0,dp(32),1));r.addView(text(money(amount),15,green,true),lp(-2,32));item.addView(r);item.addView(text(date+((notes==null||notes.isEmpty())?"":"  •  "+notes),12,muted,false));LinearLayout act=row();act.addView(button("تعديل",()->editExpense(id),false),lp(-2,40));act.addView(button("حذف",()->deleteExpense(id),false),lp(-2,40));item.addView(act);LinearLayout.LayoutParams ip=lp(-1,-2);ip.bottomMargin=dp(8);page.addView(item,ip);}while(c.moveToNext());c.close();
    }
    private void chooseRange(){LocalDate from=rangeMode?LocalDate.parse(rangeFrom):LocalDate.now().withDayOfMonth(1); pickDate(from,d->{rangeFrom=d.toString();pickDate(rangeMode?LocalDate.parse(rangeTo):LocalDate.now(),end->{rangeTo=end.toString();if(end.isBefore(d)){rangeFrom=end.toString();rangeTo=d.toString();}rangeMode=true;showHome();});});}
    private interface DateDone{void done(LocalDate date);}
    private void pickDate(LocalDate value,DateDone cb){new DatePickerDialog(this,(v,y,m,d)->cb.done(LocalDate.of(y,m+1,d)),value.getYear(),value.getMonthValue()-1,value.getDayOfMonth()).show();}
    private EditText edit(String hint,boolean numeric){EditText e=new EditText(this);e.setSingleLine(true);e.setHint(hint);e.setTextSize(16);e.setInputType(numeric?InputType.TYPE_CLASS_NUMBER:InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);return e;}
    private void showNewExpense(){showExpenseForm(-1);}
    private void editExpense(long id){showExpenseForm(id);}
    private void showExpenseForm(long id){
        long amount=0,catId=-1;String date=selectedMonth.equals(YearMonth.now().toString())?LocalDate.now().toString():YearMonth.parse(selectedMonth).atDay(1).toString(),notes="";
        if(id>0){Cursor c=db.get().rawQuery("SELECT amount,date,notes,category_id FROM expenses WHERE id=?",new String[]{""+id});if(c.moveToFirst()){amount=c.getLong(0);date=c.getString(1);notes=c.getString(2)==null?"":c.getString(2);catId=c.isNull(3)?-1:c.getLong(3);}c.close();}
        LinearLayout box=column();box.setPadding(dp(20),dp(8),dp(20),dp(4));EditText amountE=edit("المبلغ بالدينار العراقي",true);if(amount>0)amountE.setText(""+amount);box.addView(amountE);
        LinearLayout dateRow=row();dateRow.addView(text("التاريخ",14,muted,false),new LinearLayout.LayoutParams(0,dp(46),1));TextView dateV=text(date,15,green,true);dateV.setGravity(Gravity.CENTER);dateRow.addView(dateV,lp(-2,46));dateV.setOnClickListener(v->pickDate(LocalDate.parse(dateV.getText()),d->dateV.setText(d.toString())));box.addView(dateRow);
        Spinner cat=new Spinner(this);List<Long> ids=new ArrayList<>();List<String> names=new ArrayList<>();Cursor cc=db.get().rawQuery("SELECT id,name FROM categories ORDER BY name",null);while(cc.moveToNext()){ids.add(cc.getLong(0));names.add(cc.getString(1));}cc.close();if(names.isEmpty()){ids.add(-1L);names.add("بدون تصنيف");}ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,names);adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);cat.setAdapter(adapter);int ci=ids.indexOf(catId);if(ci>=0)cat.setSelection(ci);box.addView(cat);
        EditText noteE=edit("ملاحظات (اختياري)",false);noteE.setText(notes);box.addView(noteE);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(id>0?"تعديل المصروف":"إضافة مصروف").setView(box).setNegativeButton("إلغاء",null).setPositiveButton("حفظ",null).create();dialog.setOnShowListener(v->{dialog.getButton(-1).setOnClickListener(w->{try{long val=parseAmount(amountE.getText().toString());if(val<=0)throw new Exception();ContentValues cv=new ContentValues();cv.put("amount",val);cv.put("date",dateV.getText().toString());cv.put("notes",noteE.getText().toString().trim());long selected=ids.get(cat.getSelectedItemPosition());if(selected<0)cv.putNull("category_id");else cv.put("category_id",selected);if(id<0)db.get().insert("expenses",null,cv);else db.get().update("expenses",cv,"id=?",new String[]{""+id});dialog.dismiss();showHome();}catch(Exception ex){amountE.setError("أدخل مبلغاً صحيحاً");}});});dialog.show();
    }
    private void deleteExpense(long id){new AlertDialog.Builder(this).setMessage("هل تريد حذف هذا المصروف؟").setNegativeButton("إلغاء",null).setPositiveButton("حذف",(d,w)->{db.get().delete("expenses","id=?",new String[]{""+id});showHome();}).show();}

    private void showIncome(){initPage("إدارة الخزنة والدخل",true);long vault=db.starting()+db.sum("incomes",null,null)-db.sum("expenses",null,null);LinearLayout c=card();c.addView(text("رصيد الخزنة الحالي",14,muted,false));gap(c,5);c.addView(text(money(vault),25,green,true));page.addView(c,lp(-1,-2));gap(page,8);
        LinearLayout btn=row();btn.addView(button("تعديل رصيد البداية",this::editStarting,true),new LinearLayout.LayoutParams(0,dp(46),1));btn.addView(button("+ إضافة دخل",()->incomeForm(-1),false),new LinearLayout.LayoutParams(0,dp(46),1));page.addView(btn,lp(-1,48));gap(page,12);page.addView(text("سجل الدخل",17,ink,true),lp(-1,38));
        Cursor cur=db.get().rawQuery("SELECT id,source,amount,date,notes FROM incomes ORDER BY date DESC,id DESC",null);if(!cur.moveToFirst()){page.addView(text("لم تسجل أي دخل بعد",14,muted,false));cur.close();return;}do{long id=cur.getLong(0),am=cur.getLong(2);String source=cur.getString(1),date=cur.getString(3),note=cur.getString(4);LinearLayout it=card();LinearLayout r=row();r.addView(text(source,15,ink,true),new LinearLayout.LayoutParams(0,dp(32),1));r.addView(text(money(am),14,green,true),lp(-2,32));it.addView(r);it.addView(text(date+(note==null||note.isEmpty()?"":"  •  "+note),12,muted,false));LinearLayout acts=row();acts.addView(button("تعديل",()->incomeForm(id),false),lp(-2,40));acts.addView(button("حذف",()->new AlertDialog.Builder(this).setMessage("حذف الدخل؟").setNegativeButton("إلغاء",null).setPositiveButton("حذف",(d,w)->{db.get().delete("incomes","id=?",new String[]{""+id});showIncome();}).show(),false),lp(-2,40));it.addView(acts);LinearLayout.LayoutParams p=lp(-1,-2);p.bottomMargin=dp(8);page.addView(it,p);}while(cur.moveToNext());cur.close();
    }
    private void editStarting(){EditText e=edit("المبلغ الموجود عند بدء الاستخدام",true);e.setText(""+db.starting());new AlertDialog.Builder(this).setTitle("رصيد بداية الخزنة").setView(e).setNegativeButton("إلغاء",null).setPositiveButton("حفظ",(d,w)->{try{db.setStarting(parseAmount(e.getText().toString()));showIncome();}catch(Exception ignored){}}).show();}
    private void incomeForm(long id){String source="",date=LocalDate.now().toString(),notes="";long amount=0;if(id>0){Cursor c=db.get().rawQuery("SELECT source,amount,date,notes FROM incomes WHERE id=?",new String[]{""+id});if(c.moveToFirst()){source=c.getString(0);amount=c.getLong(1);date=c.getString(2);notes=c.getString(3)==null?"":c.getString(3);}c.close();}
        LinearLayout box=column();box.setPadding(dp(20),dp(8),dp(20),dp(4));EditText src=edit("مصدر الدخل، مثلاً: الراتب أو عمل خاص",false);src.setText(source);box.addView(src);EditText am=edit("المبلغ بالدينار العراقي",true);if(amount>0)am.setText(""+amount);box.addView(am);LinearLayout dr=row();dr.addView(text("التاريخ",14,muted,false),new LinearLayout.LayoutParams(0,dp(44),1));TextView dv=text(date,15,green,true);dr.addView(dv,lp(-2,44));dv.setOnClickListener(v->pickDate(LocalDate.parse(dv.getText()),x->dv.setText(x.toString())));box.addView(dr);EditText no=edit("ملاحظات (اختياري)",false);no.setText(notes);box.addView(no);
        AlertDialog dlg=new AlertDialog.Builder(this).setTitle(id>0?"تعديل الدخل":"إضافة دخل يدوية").setView(box).setNegativeButton("إلغاء",null).setPositiveButton("حفظ",null).create();dlg.setOnShowListener(v->dlg.getButton(-1).setOnClickListener(w->{try{long a=parseAmount(am.getText().toString());if(a<=0||src.getText().toString().trim().isEmpty())throw new Exception();ContentValues cv=new ContentValues();cv.put("source",src.getText().toString().trim());cv.put("amount",a);cv.put("date",dv.getText().toString());cv.put("notes",no.getText().toString().trim());if(id<0)db.get().insert("incomes",null,cv);else db.get().update("incomes",cv,"id=?",new String[]{""+id});dlg.dismiss();showIncome();}catch(Exception ex){am.setError("أدخل المصدر والمبلغ");}}));dlg.show();
    }
    private void showCategories(){initPage("تصنيفات المصروفات",true);page.addView(button("+ إضافة تصنيف",()->categoryForm(-1,""),true),lp(-1,48));gap(page,10);Cursor c=db.get().rawQuery("SELECT id,name FROM categories ORDER BY name",null);while(c.moveToNext()){long id=c.getLong(0);String name=c.getString(1);LinearLayout it=card(),r=row();r.addView(text(name,16,ink,true),new LinearLayout.LayoutParams(0,dp(44),1));r.addView(button("تعديل",()->categoryForm(id,name),false),lp(-2,42));r.addView(button("حذف",()->new AlertDialog.Builder(this).setMessage("حذف التصنيف؟ ستبقى المصروفات السابقة بدون تصنيف.").setNegativeButton("إلغاء",null).setPositiveButton("حذف",(d,w)->{db.get().delete("categories","id=?",new String[]{""+id});showCategories();}).show(),false),lp(-2,42));it.addView(r);LinearLayout.LayoutParams p=lp(-1,-2);p.bottomMargin=dp(8);page.addView(it,p);}c.close();}
    private void categoryForm(long id,String name){EditText e=edit("اسم التصنيف",false);e.setText(name);new AlertDialog.Builder(this).setTitle(id<0?"إضافة تصنيف":"تعديل التصنيف").setView(e).setNegativeButton("إلغاء",null).setPositiveButton("حفظ",(d,w)->{String n=e.getText().toString().trim();if(!n.isEmpty()){ContentValues cv=new ContentValues();cv.put("name",n);if(id<0)db.get().insert("categories",null,cv);else db.get().update("categories",cv,"id=?",new String[]{""+id});showCategories();}}).show();}

    private static class DB extends SQLiteOpenHelper {
        DB(Activity a){super(a,"pocket_ledger.db",null,1);}
        public void onCreate(SQLiteDatabase d){d.execSQL("CREATE TABLE settings (key TEXT PRIMARY KEY,value INTEGER NOT NULL)");d.execSQL("INSERT INTO settings VALUES ('starting_balance',0)");d.execSQL("CREATE TABLE categories (id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL UNIQUE)");d.execSQL("CREATE TABLE incomes (id INTEGER PRIMARY KEY AUTOINCREMENT,source TEXT NOT NULL,amount INTEGER NOT NULL,date TEXT NOT NULL,notes TEXT)");d.execSQL("CREATE TABLE expenses (id INTEGER PRIMARY KEY AUTOINCREMENT,amount INTEGER NOT NULL,date TEXT NOT NULL,category_id INTEGER,notes TEXT,FOREIGN KEY(category_id) REFERENCES categories(id) ON DELETE SET NULL)");d.execSQL("INSERT INTO categories(name) VALUES ('طعام'),('فواتير'),('نقل'),('صحة'),('تسوق'),('أخرى')");}
        public void onUpgrade(SQLiteDatabase d,int oldV,int newV){}
        SQLiteDatabase get(){return getWritableDatabase();}
        long starting(){Cursor c=get().rawQuery("SELECT value FROM settings WHERE key='starting_balance'",null);long v=c.moveToFirst()?c.getLong(0):0;c.close();return v;}
        void setStarting(long v){ContentValues cv=new ContentValues();cv.put("value",v);get().update("settings",cv,"key='starting_balance'",null);}
        long sum(String table,String where,String[] args){Cursor c=get().rawQuery("SELECT COALESCE(SUM(amount),0) FROM "+table+(where==null?"":" WHERE "+where),args);long v=c.moveToFirst()?c.getLong(0):0;c.close();return v;}
    }
}

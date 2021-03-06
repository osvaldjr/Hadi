package com.the9tcat.hadi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import com.the9tcat.hadi.annotation.Table;

import dalvik.system.DexFile;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {
	private static final String Hadi_DB_NAME = "Hadi_DB_NAME";
	private static final String Hadi_DB_VERSION = "Hadi_DB_VERSION";
	private Context mContext;

	public DatabaseHelper(Context context) {
		super(context, getDBName(context), null, getDBVersion(context));
		this.mContext = context;
	}

	@Override
	public void onCreate(SQLiteDatabase arg0) {

		ArrayList<Class<?>> tables = getEntityClasses(mContext);

		Log.i(LogParams.LOGGING_TAG, "Creating " + tables.size() + " tables");
		List<String> primarys = new ArrayList<String>();
		StringBuffer sb;
		for (Class<?> table : tables) {
			List<ColumnAttribute> columns = Util.getTableColumn(((HadiApplication)mContext.getApplicationContext()),table);
			if(columns.size()==0){
				continue;
			}
			sb = new StringBuffer();
			primarys.clear();
			boolean find_increment = false;
			for (ColumnAttribute column : columns) {
				if(column.primary){
					primarys.add(column.name);
				}
				sb.append(column.name);
				sb.append(" ");
				sb.append(column.type);
				if(column.autoincrement){
					find_increment = true;
					sb.append(" PRIMARY KEY AUTOINCREMENT");
				}else{
					if(column.length>0){
						sb.append("(");
						sb.append(column.length);
						sb.append(")");
					}
					if(column.default_value!=null){
						sb.append(" default "+column.default_value);
					}
				}
				sb.append(" , ");
			}
			if(primarys.size()>0&&!find_increment){
				String pms = "";
				for(String pm:primarys){
					pms = pms + pm +",";
				}
				sb.append(" PRIMARY KEY ("+pms.substring(0,pms.length()-1)+") , ");
			}
			String sql ="CREATE TABLE "+Util.getTableName(table)+" ("+sb.toString().substring(0,sb.length()-2)+" )";

			Log.i(LogParams.LOGGING_TAG, sql);

			arg0.execSQL(sql);
		}

	}

	@Override
	public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {

		ArrayList<Class<?>> tables = getEntityClasses(this.mContext);
		for (Class<?> table : tables) {
			arg0.execSQL("DROP TABLE IF EXISTS "
					+ Util.getTableName(table));
		}
		onCreate(arg0);
	}

	private static ArrayList<Class<?>> getEntityClasses(Context context) {
		ArrayList<Class<?>> entityClasses = new ArrayList<Class<?>>();
		try {
			String path = context.getPackageManager().getApplicationInfo(
					context.getPackageName(), 0).sourceDir;
			DexFile dexfile = new DexFile(path);
			Enumeration<String> entries = dexfile.entries();
			while (entries.hasMoreElements()) {
				String name = (String) entries.nextElement();
				Class<?> discoveredClass = null;
				try {
					discoveredClass = Class.forName(name, true, context
							.getClass().getClassLoader());
				} catch (ClassNotFoundException e) {
					Log.e("ActiveORM", e.getMessage());
				}
				if ((discoveredClass == null)||
						discoveredClass.getAnnotation(Table.class)==null) {
					continue;
				}
				entityClasses.add((Class<?>)discoveredClass);
			}
		} catch (IOException e) {
			Log.e(LogParams.LOGGING_TAG, e.getMessage());
		} catch (PackageManager.NameNotFoundException e) {
			Log.e(LogParams.LOGGING_TAG, e.getMessage());
		}
		return entityClasses;
	}

	private static String getDBName(Context context) {
		String dbName = getMetaData(context, Hadi_DB_NAME);
		if (dbName == null) {
			dbName = "Application.db";
		}
		return dbName;
	}

	private static int getDBVersion(Context context) {
		int dbVersion = getMetaDataInt(context, Hadi_DB_VERSION);
		if(dbVersion>0){
			return dbVersion;
		}
		return 1;
	}

	private static String getMetaData(Context context, String name) {
		PackageManager pm = context.getPackageManager();
		try {
			ApplicationInfo ai = pm.getApplicationInfo(
					context.getPackageName(), 128);
			return ai.metaData.getString(name);
			
		} catch (Exception e) {
			Log.w(LogParams.LOGGING_TAG, "Couldn't find meta data string: " + name);
		}
		return null;
	}
	
	private static int getMetaDataInt(Context context, String name) {
		PackageManager pm = context.getPackageManager();
		try {
			ApplicationInfo ai = pm.getApplicationInfo(
					context.getPackageName(), 128);
			return ai.metaData.getInt(name);
		} catch (Exception e) {
			Log.w(LogParams.LOGGING_TAG, "Couldn't find meta data string: " + name);
		}
		return 0;
	}

}

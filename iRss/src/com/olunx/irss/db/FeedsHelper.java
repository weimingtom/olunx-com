package com.olunx.irss.db;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.olunx.irss.R;
import com.olunx.irss.util.Config;
import com.olunx.irss.util.Utils;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class FeedsHelper implements IHelper {

	public final static String c_id = "_id";
	public final static String c_title = "title";
	public final static String c_text = "text";
	public final static String c_icon = "icon";
	public final static String c_sortId = "sort_id";
	public final static String c_catTitle = "cat_title";
	public final static String c_xmlUrl = "xml_url";
	public final static String c_htmlUrl = "html_url";
	public final static String c_googleFeedId = "google_feed_id";
	public final static String c_updateTime = "update_time";
	public final static String c_charset = "charset";
	public final static String c_rssType = "rss_type";
	public final static String c_articleCount = "article_count";

	private static String TABLE = "t_feeds";
	private final String TAG = "com.olunx.db.FeedsHelper";

	private static SQLiteDatabase sqlite = null;

	public FeedsHelper() {
		super();
		getDB();
		createTable();
	}

	@Override
	public SQLiteDatabase getDB() {
		try {
			if (sqlite == null || !sqlite.isOpen()) {
				File file = Utils.init().createFileIfNotExist(Config.FILE_SDCARD_DATABASE);
				sqlite = SQLiteDatabase.openOrCreateDatabase(file, null);
			}
			return sqlite;
		} catch (SQLException e) {
			return null;
		}
	}

	@Override
	public void close() {
		if (sqlite != null || sqlite.isOpen()) {
			Log.i(TAG, "sqlite close");
			sqlite.close();
		}
	}

	public boolean isOpen() {
		if (sqlite != null) {
			return sqlite.isOpen();
		}
		return false;
	}

	@Override
	public void createTable() {
		this.getDB().execSQL(
				"create table if not exists " + TABLE + "(" + c_id + " int primary key," + c_title + " text," + c_text + " text," + c_icon
						+ " text," + c_sortId + " int," + c_catTitle + " text," + c_xmlUrl + " text," + c_htmlUrl + " text,"
						+ c_googleFeedId + " text," + c_updateTime + " text," + c_charset + " text," + c_rssType + " text,"
						+ c_articleCount + " int);");
	}

	@Override
	public void dropTable() {
		this.getDB().execSQL("drop table if exists " + TABLE + ";");
	}

	/**
	 * 删除所有数据
	 */
	public void deleteAll() {
		this.getDB().execSQL("delete from " + TABLE + ";");
	}

	/**
	 * 添加Feed
	 * 
	 * @param object
	 */
	public void addRecord(ContentValues mValues) {
		getDB().insert(TABLE, null, mValues);
	}

	/**
	 * 更新一条记录
	 * 
	 * @param object
	 */
	public void updateRecord(ContentValues object) {
		String xmlUrl = (String) object.get(c_xmlUrl);
		String catTitle = (String) object.get(c_catTitle);
		getDB().update(TABLE, object, c_xmlUrl + "== ? and " + c_catTitle + "== ?", new String[] { xmlUrl, catTitle });
	}

	/**
	 * 更新文章数目
	 * 
	 * @param feedXmlUrl
	 * @param articleCount
	 */
	public void updateArticleCount(String feedXmlUrl, String articleCount) {
		ContentValues row = new ContentValues();
		row.put(c_articleCount, articleCount);
		row.put(c_updateTime, Utils.init().getCstTime(new Date()));
		getDB().update(TABLE, row, c_xmlUrl + "== ? ", new String[] { feedXmlUrl });
	}

	/**
	 * 更新Feed的内容编码
	 * 
	 * @param feedXmlUrl
	 * @param charset
	 */
	public void updateFeedCharset(String feedXmlUrl, String charset) {
		ContentValues row = new ContentValues();
		row.put(c_charset, charset);
		getDB().update(TABLE, row, c_xmlUrl + "== ? ", new String[] { feedXmlUrl });
	}

	/**
	 * 返回分类并统计每个分类下的feed数目
	 * 
	 * @return
	 */
	public void updateCategoryStatus() {
		Cursor result = getDB().query(TABLE, new String[] { c_catTitle, "count(" + c_catTitle + ") as count" }, null, null, c_catTitle,
				null, null);

		int titleColumn = result.getColumnIndex(c_catTitle);
		int countColumn = result.getColumnIndex("count");

		CategoryHelper helper = new CategoryHelper();
		helper.deleteAll();

		String title = null;
		String count = null;
		if (result != null) {
			result.moveToFirst();
			while (!result.isAfterLast()) {
				title = result.getString(titleColumn);
				count = result.getString(countColumn);
				helper.addRecord(title, count);

				result.moveToNext();
			}
		}
		
		result.close();
		helper.close();
	}

	/**
	 * 获取指定分类的Feed
	 * 
	 * @return
	 */
	// public Cursor getFeedsByCategory(String catTitle) {
	// return getDB().query(TABLE, new String[] { c_id, c_icon, c_title,
	// c_articleCount, c_xmlUrl, c_charset }, c_catTitle + "== ?",
	// new String[] { catTitle }, null, null, null);
	// }

	/**
	 * 获取指定Feed下的文章列表
	 * 
	 * @param feedXmlUrl
	 * @return
	 */
	public ArrayList<Map<String, Object>> getFeedsByCategory(String catTitle) {
		Cursor result = getDB().query(TABLE, new String[] { c_icon, c_title, c_articleCount, c_xmlUrl, c_charset, c_updateTime },
				c_catTitle + "== ?", new String[] { catTitle }, null, null, c_updateTime + " desc");

		ArrayList<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

		Map<String, Object> map;
		if (result != null) {
			result.moveToFirst();
			int iconIndex = result.getColumnIndex(c_icon);
			int titleIndex = result.getColumnIndex(c_title);
			int countIndex = result.getColumnIndex(c_articleCount);
			int xmlurlIndex = result.getColumnIndex(c_xmlUrl);
			int charsetIndex = result.getColumnIndex(c_charset);
			int timeIndex = result.getColumnIndex(c_updateTime);
			Utils utils = new Utils();
			while (!result.isAfterLast()) {
				map = new HashMap<String, Object>();

				map.put(c_icon, result.getString(iconIndex));
				map.put(c_title, result.getString(titleIndex));
				map.put(c_xmlUrl, result.getString(xmlurlIndex));
				map.put(c_charset, result.getString(charsetIndex));
				String articleCount = result.getString(countIndex);
				if (articleCount == null) {
					articleCount = "0";
				}
				String updateTime = utils.formatCstTimeToLocal(result.getString(timeIndex), "MM月dd日");
				if (updateTime == null) {
					updateTime = "从未";
					map.put(c_icon, R.drawable.rss_never_update);
				}

				String summary = "文章: " + articleCount + "   更新: " + updateTime;

				map.put(c_text, summary);

				list.add(map);
				result.moveToNext();
			}
		}
		result.close();
		return list;
	}

	/**
	 * 返回指定栏目下的所有Feed地址
	 * 
	 * @param catTitle
	 * @return
	 */
	public ArrayList<String> getFeedsXmlUrlByCategory(String catTitle) {
		ArrayList<String> array = new ArrayList<String>();

		Cursor result = getDB().query(TABLE, new String[] { c_id, c_xmlUrl }, c_catTitle + "== ?", new String[] { catTitle }, null, null,
				null);
		if (result != null) {
			result.moveToFirst();
			int index = result.getColumnIndex(c_xmlUrl);
			while (!result.isAfterLast()) {
				array.add(result.getString(index));
				result.moveToNext();
			}
		}
		result.close();

		return array;
	}

	/**
	 * 返回所有Feed地址
	 * 
	 * @return
	 */
	public ArrayList<String> getAllFeedsXmlUrl() {
		ArrayList<String> array = new ArrayList<String>();

		Cursor result = getDB().query(TABLE, new String[] { c_id, c_xmlUrl }, null, null, null, null, null);
		if (result != null) {
			result.moveToFirst();
			int index = result.getColumnIndex(c_xmlUrl);
			while (!result.isAfterLast()) {
				array.add(result.getString(index));
				result.moveToNext();
			}
		}
		result.close();

		return array;
	}

	/**
	 * 获取更新时间
	 * 
	 * @param feedXmlUrl
	 * @return
	 */
	public String getFeedUpdateTime(String feedXmlUrl) {
		String str = null;
		Cursor result = getDB().query(TABLE, new String[] { c_updateTime }, c_xmlUrl + "== ?", new String[] { feedXmlUrl }, null, null,
				null);
		if (result != null) {
			result.moveToFirst();
			int index = result.getColumnIndex(c_updateTime);
			while (!result.isAfterLast()) {
				str = result.getString(index);
				result.moveToNext();
			}
		}
		result.close();

		System.out.println("feed update time: " + str);

		return str;
	}

	/**
	 * 判断Feed是否存在
	 * 
	 * @param feedXmlUrl
	 * @return
	 */
	public boolean isExistsFeed(String feedXmlUrl, String category) {
		String str = null;
		Cursor result = getDB().query(TABLE, new String[] { c_xmlUrl }, c_xmlUrl + "== ? and " + c_catTitle + "== ?",
				new String[] { feedXmlUrl, category }, null, null, null);
		if (result != null) {
			result.moveToFirst();
			int index = result.getColumnIndex(c_xmlUrl);
			while (!result.isAfterLast()) {
				str = result.getString(index);
				result.moveToNext();
			}
		}
		result.close();
		if (str == null || str == "" || str.equals("")) {
			return false;
		}
		return true;
	}

	/**
	 * 删除一条feed
	 * 
	 * @param feedTitle
	 */
	public void deleteRecord(String feedXmlUrl) {
		String sql = "delete from " + TABLE + " where " + c_xmlUrl + " == '" + feedXmlUrl + "'";
		this.getDB().execSQL(sql);
	}

}

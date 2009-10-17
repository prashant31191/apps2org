/*
 * Copyright (C) 2009 Apps Organizer
 *
 * This file is part of Apps Organizer
 *
 * Apps Organizer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Apps Organizer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Apps Organizer.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.google.code.appsorganizer.download;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.os.Handler;
import android.os.Message;

import com.google.code.appsorganizer.R;
import com.google.code.appsorganizer.db.AppCacheDao;
import com.google.code.appsorganizer.db.DatabaseHelper;
import com.google.code.appsorganizer.dialogs.GenericDialogManager;
import com.google.code.appsorganizer.dialogs.GenericDialogManagerActivity;
import com.google.code.appsorganizer.dialogs.OnOkClickListener;

/**
 * @author fabio
 * 
 */
public class LabelDownloader {

	private final Activity activity;

	private final DatabaseHelper dbHelper;

	private final ConfirmLabelDownloadDialog confirmLabelDownloadDialog;

	private boolean downloadAll = true;

	private boolean operationCancelled = false;

	private final OnOkClickListener onOkClickListener;

	public LabelDownloader(Activity activity, DatabaseHelper dbHelper, OnOkClickListener onOkClickListener) {
		this.activity = activity;
		this.dbHelper = dbHelper;
		this.onOkClickListener = onOkClickListener;
		GenericDialogManager genericDialogManager = ((GenericDialogManagerActivity) activity).getGenericDialogManager();
		confirmLabelDownloadDialog = new ConfirmLabelDownloadDialog(genericDialogManager, new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
				case 0:
					downloadAll = true;
					break;
				case 1:
					downloadAll = false;
					break;
				default:
					startDownload();
					break;
				}
			}
		});
	}

	private ProgressDialog pd;

	private final Handler handler = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			if (msg.obj != null) {
				pd.incrementProgressBy(1);
				pd.setMessage(msg.obj.toString());
			} else {
				if (msg.arg1 > 0) {
					pd.setIndeterminate(false);
					pd.setMax(msg.arg1);
				} else {
					pd.hide();
					if (onOkClickListener != null) {
						onOkClickListener.onClick(null, null, 0);
					}
				}
			}
		}
	};

	public void download() {
		operationCancelled = false;
		confirmLabelDownloadDialog.showDialog();
	}

	public void startDownload() {
		createAndShowProgresDialog();
		new Thread() {
			@Override
			public void run() {
				startDownloadExternalThread();
			}
		}.start();
	}

	private void createAndShowProgresDialog() {
		pd = new ProgressDialog(activity);
		pd.setTitle(R.string.Downloading);
		pd.setMessage(activity.getString(R.string.Starting_download));
		pd.setIndeterminate(true);
		pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		pd.setCancelable(true);
		pd.setOnCancelListener(new OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				operationCancelled = true;
			}
		});
		pd.show();
	}

	private String download(String packageName) {
		BufferedReader in = null;
		try {
			URL u = new URL("http://www.cyrket.com/package/" + packageName);
			HttpURLConnection c = (HttpURLConnection) u.openConnection();
			c.setRequestMethod("GET");
			c.setDoOutput(true);
			c.connect();

			in = new BufferedReader(new InputStreamReader(c.getInputStream()));

			String s = null;
			while ((s = in.readLine()) != null) {
				int indexOf = s.indexOf("<label>Category</label>");
				if (indexOf != -1) {
					String category = in.readLine().trim();
					if (category.startsWith("<div>")) {
						category = category.substring(5, category.length() - 6);
					}
					return category;
				}
			}
		} catch (IOException e) {
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException ignored) {
				}
			}
		}
		return null;
	}

	private void startDownloadExternalThread() {
		Map<String, Long> labelsMap = dbHelper.labelDao.getLabelsMap();
		Cursor c;
		if (downloadAll) {
			c = dbHelper.appCacheDao.getAllApps(new String[] { AppCacheDao.NAME_COL_NAME, AppCacheDao.PACKAGE_NAME_COL_NAME,
					AppCacheDao.LABEL_COL_NAME });
		} else {
			c = dbHelper.appCacheDao.getAppsNoLabelCursor();
		}
		Message message = new Message();
		message.arg1 = c.getCount();
		handler.sendMessage(message);
		try {
			int i = 0;
			while (c.moveToNext() && !operationCancelled) {
				message = new Message();
				message.arg1 = i++;
				message.obj = c.getString(2);
				handler.sendMessage(message);

				String packageName = c.getString(1);
				String label = download(packageName);
				if (label != null) {
					Long labelId = labelsMap.get(label);
					if (labelId == null) {
						labelId = dbHelper.labelDao.insert(label);
						labelsMap.put(label, labelId);
					}
					dbHelper.appsLabelDao.merge(packageName, c.getString(0), labelId);
				}
			}
		} finally {
			c.close();
			handler.sendEmptyMessage(1);
		}
	}

}
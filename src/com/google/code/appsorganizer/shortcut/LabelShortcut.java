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
package com.google.code.appsorganizer.shortcut;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import com.google.code.appsorganizer.ApplicationInfoManager;
import com.google.code.appsorganizer.R;
import com.google.code.appsorganizer.db.DatabaseHelper;
import com.google.code.appsorganizer.db.DbChangeListener;
import com.google.code.appsorganizer.model.AppLabel;
import com.google.code.appsorganizer.model.Application;
import com.google.code.appsorganizer.model.GridObject;
import com.google.code.appsorganizer.model.Label;

public class LabelShortcut extends Activity implements DbChangeListener {

	public static final long ALL_LABELS_ID = -2l;
	public static final String LABEL_ID = "com.example.android.apis.app.LauncherShortcuts";

	private ApplicationInfoManager applicationInfoManager;

	private DatabaseHelper dbHelper;

	private Label label;

	private boolean allLabelsSelected;

	private static Label ALL_LABELS;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		applicationInfoManager = ApplicationInfoManager.singleton(getPackageManager());
		applicationInfoManager.reloadAppsMap();
		dbHelper = DatabaseHelper.initOrSingleton(this);

		if (ALL_LABELS == null) {
			ALL_LABELS = new Label(LabelShortcut.ALL_LABELS_ID, getString(R.string.all_labels), R.drawable.icon);
		}
		final Intent intent = getIntent();

		long labelId = intent.getLongExtra(LABEL_ID, ALL_LABELS_ID);
		if (labelId == ALL_LABELS_ID) {
			allLabelsSelected = true;
			label = ALL_LABELS;
		} else {
			allLabelsSelected = false;
			label = dbHelper.labelDao.queryById(labelId);
		}
		getOrCreateGrid();
		reloadGrid();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		applicationInfoManager.removeListener(this);
	}

	private void reloadGrid() {
		if (label != null) {
			@SuppressWarnings("unchecked")
			final AppGridAdapter<GridObject> gridAdapter = (AppGridAdapter<GridObject>) grid.getAdapter();
			setTitle(label.getName());
			if (label.getId() == ALL_LABELS_ID) {
				List<Label> labels = dbHelper.labelDao.getLabels();
				gridAdapter.setObjectList(labels);
			} else {
				List<AppLabel> apps = dbHelper.appsLabelDao.getApps(label.getId());
				Collection<Application> newList = applicationInfoManager.convertToApplicationList(apps);
				gridAdapter.setObjectList(new ArrayList<Application>(newList));
			}
		}
	}

	private GridView grid;

	private GridView getOrCreateGrid() {
		if (grid == null) {
			setContentView(R.layout.shortcut_grid);

			grid = (GridView) findViewById(R.id.shortcutGrid);
			grid.setColumnWidth(50);
			final AppGridAdapter<GridObject> adapter = new AppGridAdapter<GridObject>(new ArrayList<GridObject>(), this);
			grid.setAdapter(adapter);

			applicationInfoManager.addListener(this);

			grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
					if (label.getId() == ALL_LABELS_ID) {
						label = (Label) adapter.getItem(pos);
						reloadGrid();
					} else {
						Application a = (Application) grid.getAdapter().getItem(pos);
						Intent i = a.getIntent();
						i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(i);
					}
				}
			});
		}
		return grid;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && allLabelsSelected) {
			if (label != null && label.getId() != ALL_LABELS_ID) {
				label = ALL_LABELS;
				reloadGrid();
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	public void dataSetChanged() {
		reloadGrid();
	}
}
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
package com.google.code.appsorganizer.chooseicon;

import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.google.code.appsorganizer.R;

public class ChooseIconActivity extends Activity {
	private GridView mGrid;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		loadIcons();

		setContentView(R.layout.icon_grid);
		mGrid = (GridView) findViewById(R.id.iconGrid);
		mGrid.setAdapter(new AppsAdapter());
		mGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
				Intent res = new Intent();
				res.putExtra("icon", mIcons.get(pos));
				setResult(RESULT_OK, res);
				finish();
			}
		});
	}

	private List<Integer> mIcons;

	private void loadIcons() {
		mIcons = Arrays.asList(R.drawable.address_48, R.drawable.bookmark_48, R.drawable.briefcase_48, R.drawable.bubble_48,
				R.drawable.buy_48, R.drawable.calendar_48, R.drawable.clipboard_48, R.drawable.clock_48, R.drawable.diagram_48,
				R.drawable.document_48, R.drawable.flag_48, R.drawable.gear_48, R.drawable.globe_48, R.drawable.key_48,
				R.drawable.label_48, R.drawable.letter_48, R.drawable.pencil_32, R.drawable.shield_32, R.drawable.statistics_32,
				R.drawable.user_48, R.drawable.wallet_32);
	}

	public class AppsAdapter extends BaseAdapter {
		public AppsAdapter() {
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			ImageView i;

			if (convertView == null) {
				i = new ImageView(ChooseIconActivity.this);
				i.setScaleType(ImageView.ScaleType.FIT_CENTER);
				i.setLayoutParams(new GridView.LayoutParams(50, 50));
			} else {
				i = (ImageView) convertView;
			}

			i.setImageResource(mIcons.get(position));
			return i;
		}

		public final int getCount() {
			return mIcons.size();
		}

		public final Object getItem(int position) {
			return mIcons.get(position);
		}

		public final long getItemId(int position) {
			return position;
		}
	}

}

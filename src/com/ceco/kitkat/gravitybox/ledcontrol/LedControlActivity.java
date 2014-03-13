/*
 * Copyright (C) 2014 Peter Gregus for GravityBox Project (C3C076@xda)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ceco.kitkat.gravitybox.ledcontrol;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ceco.kitkat.gravitybox.GravityBoxSettings;
import com.ceco.kitkat.gravitybox.R;
import com.ceco.kitkat.gravitybox.ledcontrol.LedListAdapter.ListItemActionHandler;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class LedControlActivity extends ListActivity implements ListItemActionHandler, OnItemClickListener {

    private static final int REQ_SETTINGS = 1;

    private ListView mList;
    private AsyncTask<Void, Void, ArrayList<LedListItem>> mAsyncTask;
    private ProgressDialog mProgressDialog;
    private LedListItem mCurrentItem;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        File file = new File(getFilesDir() + "/" + GravityBoxSettings.FILE_THEME_DARK_FLAG);
        if (file.exists()) {
            setTheme(android.R.style.Theme_Holo);
        }

        super.onCreate(savedInstanceState);

        mList = getListView();
        mList.setOnItemClickListener(this);
        setData();
    }

    @Override
    public void onStop() {
        cancelSetData();
        super.onStop();
    }

    private void setData() {
        mAsyncTask = new AsyncTask<Void,Void,ArrayList<LedListItem>>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                showProgressDialog();
            }

            @Override
            protected ArrayList<LedListItem> doInBackground(Void... arg0) {
                ArrayList<LedListItem> itemList = new ArrayList<LedListItem>();

                PackageManager pm = LedControlActivity.this.getPackageManager();
                List<ApplicationInfo> packages = pm.getInstalledApplications(0);
                Collections.sort(packages, new ApplicationInfo.DisplayNameComparator(pm));
                for(ApplicationInfo ai : packages) {
                    if (isCancelled()) break;
                    if (ai.packageName.equals(LedControlActivity.this.getPackageName())) continue;
                    LedListItem item = new LedListItem(LedControlActivity.this, ai);
                    itemList.add(item);
                }

                return itemList;
            }

            @Override
            protected void onCancelled() {
                dismissProgressDialog();
            }

            @Override
            protected void onPostExecute(ArrayList<LedListItem> result) {
                dismissProgressDialog();
                mList.setAdapter(new LedListAdapter(LedControlActivity.this, result, 
                        LedControlActivity.this));
                ((LedListAdapter)mList.getAdapter()).notifyDataSetChanged();
            }
        }.execute();
    }

    private void showProgressDialog() {
        mProgressDialog = new ProgressDialog(LedControlActivity.this);
        mProgressDialog.setMessage(getString(R.string.lc_please_wait));
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        mProgressDialog = null;
    }

    private void cancelSetData() {
        if (mAsyncTask != null && mAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
            mAsyncTask.cancel(true);
        }
        mAsyncTask = null;
    }

    @Override
    public void onItemCheckedChanged(LedListItem item, boolean checked) {
        item.setEnabled(checked);
        mList.invalidateViews();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mCurrentItem = (LedListItem) mList.getItemAtPosition(position);
        if (mCurrentItem.isEnabled()) {
            Intent intent = new Intent(this, LedSettingsActivity.class);
            intent.putExtra(LedSettingsActivity.EXTRA_PACKAGE_NAME, mCurrentItem.getAppInfo().packageName);
            intent.putExtra(LedSettingsActivity.EXTRA_APP_NAME, mCurrentItem.getAppName());
            this.startActivityForResult(intent, REQ_SETTINGS);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_SETTINGS && resultCode == RESULT_OK && mCurrentItem != null) {
            if (mCurrentItem.getAppInfo().packageName.equals(
                    data.getStringExtra(LedSettingsActivity.EXTRA_PACKAGE_NAME))) {
                mCurrentItem.refreshLedSettings();
                mList.invalidateViews();
            }
        }
    }
}
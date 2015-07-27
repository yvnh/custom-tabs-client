// Copyright 2015 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.chromium.customtabsclient;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.customtabs.CustomTabsCallback;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

/**
 * Example client activity for a Chrome Custom Tanb.
 */
public class MainActivity extends Activity implements OnClickListener {
    private static final String TAG = "CustomTabsClientExample";
    private EditText mEditText;
    private CustomTabsSession mCustomTabsSession;
    private CustomTabsClient mClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mEditText = (EditText) findViewById(R.id.edit);
        Button connectButton = (Button) findViewById(R.id.connect_button);
        Button warmupButton = (Button) findViewById(R.id.warmup_button);
        Button mayLaunchButton = (Button) findViewById(R.id.may_launch_button);
        Button launchButton = (Button) findViewById(R.id.launch_button);
        mEditText.requestFocus();
        connectButton.setOnClickListener(this);
        warmupButton.setOnClickListener(this);
        mayLaunchButton.setOnClickListener(this);
        launchButton.setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // TODO: Unbind from the service here. This is not exposed by CustomTabsClient.
    }

    private CustomTabsSession getSession() {
        if (mClient == null) {
            mCustomTabsSession = null;
        } else if (mCustomTabsSession == null) {
            mCustomTabsSession = mClient.newSession(new CustomTabsCallback() {
                @Override
                public void onNavigationEvent(int navigationEvent, Bundle extras) {
                    Log.w(TAG, "onNavigationEvent: Code = " + navigationEvent);
                }
            });
        }
        return mCustomTabsSession;
    }

    private void bindCustomTabsService() {
        if (mClient != null) return;
        final View connectButton = findViewById(R.id.connect_button);
        final View warmupButton = findViewById(R.id.warmup_button);
        final View mayLaunchButton = findViewById(R.id.may_launch_button);
        final View launchButton = findViewById(R.id.launch_button);
        String packageName = CustomTabActivityManager.getInstance().getPackageNameToUse(this);
        if (packageName == null) return;
        boolean ok = CustomTabsClient.bindCustomTabsService(
                this, packageName, new CustomTabsServiceConnection() {
                    @Override
                    public void onCustomTabsServiceConnected(ComponentName name, CustomTabsClient client) {
                        connectButton.setEnabled(false);
                        warmupButton.setEnabled(true);
                        mayLaunchButton.setEnabled(true);
                        launchButton.setEnabled(true);
                        mClient = client;
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName name) {
                        connectButton.setEnabled(true);
                        warmupButton.setEnabled(false);
                        mayLaunchButton.setEnabled(false);
                        launchButton.setEnabled(false);
                        mClient = null;
                    }
                });
        if (ok) connectButton.setEnabled(false);
    }

    @Override
    public void onClick(View v) {
        CustomTabActivityManager customTabManager = CustomTabActivityManager.getInstance();
        String url = mEditText.getText().toString();
        int viewId = v.getId();

        if (viewId == R.id.connect_button) {
            bindCustomTabsService();
        } else if (viewId == R.id.warmup_button) {
            boolean success = false;
            if (mClient != null) success = mClient.warmup(0);
            if (!success) findViewById(R.id.warmup_button).setEnabled(false);
        } else if (viewId == R.id.may_launch_button) {
            CustomTabsSession session = getSession();
            boolean success = false;
            if (mClient != null) success = session.mayLaunchUrl(Uri.parse(url), null, null);
            if (!success) findViewById(R.id.may_launch_button).setEnabled(false);
        } else if (viewId == R.id.launch_button) {
            CustomTabsSession session = getSession();
            CustomTabUiBuilder uiBuilder = new CustomTabUiBuilder();
            uiBuilder.setToolbarColor(Color.BLUE);
            uiBuilder.setShowTitle(true);
            uiBuilder.setCloseButtonStyle(CustomTabUiBuilder.CLOSE_BUTTON_ARROW);
            prepareMenuItems(uiBuilder);
            prepareActionButton(uiBuilder);
            uiBuilder.setStartAnimations(this, R.anim.slide_in_right, R.anim.slide_out_left);
            uiBuilder.setExitAnimations(this, R.anim.slide_in_left, R.anim.slide_out_right);
            customTabManager.launchUrl(this, session, url, uiBuilder);
        }
    }

    private void prepareMenuItems(CustomTabUiBuilder uiBuilder) {
        Intent menuIntent = new Intent();
        menuIntent.setClass(getApplicationContext(), this.getClass());
        // Optional animation configuration when the user clicks menu items.
        Bundle menuBundle = ActivityOptions.makeCustomAnimation(this, android.R.anim.slide_in_left,
                android.R.anim.slide_out_right).toBundle();
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, menuIntent, 0,
                menuBundle);
        uiBuilder.addMenuItem("Menu entry 1", pi);
    }

    private void prepareActionButton(CustomTabUiBuilder uiBuilder) {
        // An example intent that sends an email.
        Intent actionIntent = new Intent(Intent.ACTION_SEND);
        actionIntent.setType("*/*");
        actionIntent.putExtra(Intent.EXTRA_EMAIL, "example@example.com");
        actionIntent.putExtra(Intent.EXTRA_SUBJECT, "example");
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, actionIntent, 0);
        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
        uiBuilder.setActionButton(icon, pi);
    }
}

/*
    Patiner Montréal for Android.
    Information about outdoor rinks in the city of Montréal: conditions,
    services, contact, map, etc.

    Copyright (C) 2010 Mudar Noufal <mn@mudar.ca>

    This file is part of Patiner Montréal for Android.

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ca.mudar.patinoires.ui;

import ca.mudar.patinoires.PatinoiresApp;
import ca.mudar.patinoires.R;
import ca.mudar.patinoires.receivers.DetachableResultReceiver;
import ca.mudar.patinoires.services.SyncService;
import ca.mudar.patinoires.utils.ActivityHelper;
import ca.mudar.patinoires.utils.Const;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.support.v4.view.ViewPager;
import android.support.v4.view.Window;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.Toast;

import java.util.ArrayList;

public class TabsPagerActivity extends FragmentActivity {
    private static final String TAG = "TabsPagerActivity";

    private SyncStatusUpdaterFragment mSyncStatusUpdaterFragment;

    private TabHost mTabHost;
    private ViewPager mViewPager;
    private TabsAdapter mTabsAdapter;

    private PatinoiresApp mAppHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        mAppHelper = (PatinoiresApp) getApplicationContext();

        mAppHelper.updateUiLanguage();

        setContentView(R.layout.tabs_pager_fragment);
        setProgressBarIndeterminateVisibility(Boolean.FALSE);

        mTabHost = (TabHost) findViewById(android.R.id.tabhost);
        mTabHost.setup();

        mViewPager = (ViewPager) findViewById(R.id.pager);

        mTabsAdapter = new TabsAdapter(this, mTabHost, mViewPager);

        Resources res = getResources();

        mTabsAdapter.addTab(
                mTabHost.newTabSpec(Const.TABS_TAG_SKATING).setIndicator(
                        res.getString(R.string.tab_skating)), SkatingListFragment.class, null);
        mTabsAdapter.addTab(
                mTabHost.newTabSpec(Const.TABS_TAG_HOCKEY).setIndicator(
                        res.getString(R.string.tab_hockey)), HockeyListFragment.class, null);
        mTabsAdapter.addTab(
                mTabHost.newTabSpec(Const.TABS_TAG_ALL).setIndicator(
                        res.getString(R.string.tab_all)), AllListFragment.class, null);
        mTabsAdapter.addTab(
                mTabHost.newTabSpec(Const.TABS_TAG_FAVORITES).setIndicator(
                        res.getString(R.string.tab_favorites)), FavoritesListFragment.class, null);

        int mCurrentTab = getIntent().getIntExtra(Const.INTENT_EXTRA_TABS_CURRENT, -1);
        if (mCurrentTab != -1) {
            mTabHost.setCurrentTab(mCurrentTab);
        }
        // TODO Optimize using savedInstanceState
        // else if (savedInstanceState != null) {
        // Log.v(TAG, "tab from savedInstanceState");
        // mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
        // }

        FragmentManager fm = getSupportFragmentManager();

        mSyncStatusUpdaterFragment = (SyncStatusUpdaterFragment)
                fm.findFragmentByTag(SyncStatusUpdaterFragment.TAG);
        if (mSyncStatusUpdaterFragment == null) {
            // Log.w(TAG, "mSyncStatusUpdaterFragment");
            mSyncStatusUpdaterFragment = new SyncStatusUpdaterFragment();
            fm.beginTransaction().add(mSyncStatusUpdaterFragment,
                    SyncStatusUpdaterFragment.TAG).commit();

            // triggerRefresh();
        }
    }

    // TODO Optimize using savedInstanceState
    // @Override
    // protected void onSaveInstanceState(Bundle outState) {
    // super.onSaveInstanceState(outState);
    // // Log.w(TAG, "Current tab = " + mTabHost.getCurrentTabTag());
    // }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        getWindow().setBackgroundDrawable(null);
        System.gc();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        ActivityHelper mActivityHelper = ActivityHelper.createInstance(this);
        if (item.getItemId() == R.id.menu_refresh) {
            mActivityHelper.triggerRefresh(mSyncStatusUpdaterFragment.mReceiver, true);
            return true;
        }

        return mActivityHelper.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_tabs_pager, menu);

        return true;
    }

    private View buildIndicator(int imageRes) {
        ViewGroup mRootView = (ViewGroup) findViewById(android.R.id.content);
        final ImageView indicator = (ImageView) this.getLayoutInflater().inflate(
                R.layout.tab_indicator,
                (ViewGroup) mRootView.findViewById(android.R.id.tabs), false);
        indicator.setImageResource(imageRes);
        return indicator;
    }

    /**
     * This is a helper class that implements the management of tabs and all
     * details of connecting a ViewPager with associated TabHost. It relies on a
     * trick. Normally a tab host has a simple API for supplying a View or
     * Intent that each tab will show. This is not sufficient for switching
     * between pages. So instead we make the content part of the tab host 0dp
     * high (it is not shown) and the TabsAdapter supplies its own dummy view to
     * show as the tab content. It listens to changes in tabs, and takes care of
     * switch to the correct paged in the ViewPager whenever the selected tab
     * changes.
     */
    public static class TabsAdapter extends FragmentPagerAdapter implements
            TabHost.OnTabChangeListener,
            ViewPager.OnPageChangeListener {
        private final Context mContext;
        private final TabHost mTabHost;
        private final ViewPager mViewPager;
        private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();

        // TODO: use tabinfo tag?
        static final class TabInfo {
            private final String tag;
            private final Class<?> clss;
            private final Bundle args;

            TabInfo(String _tag, Class<?> _class, Bundle _args) {
                tag = _tag;
                clss = _class;
                args = _args;
            }
        }

        static class DummyTabFactory implements TabHost.TabContentFactory {
            private final Context mContext;

            public DummyTabFactory(Context context) {
                mContext = context;
            }

            @Override
            public View createTabContent(String tag) {
                View v = new View(mContext);
                v.setMinimumWidth(0);
                v.setMinimumHeight(0);
                return v;
            }
        }

        public TabsAdapter(FragmentActivity activity, TabHost tabHost, ViewPager pager) {
            super(activity.getSupportFragmentManager());
            mContext = activity;
            mTabHost = tabHost;
            mViewPager = pager;
            mTabHost.setOnTabChangedListener(this);
            mViewPager.setAdapter(this);
            mViewPager.setOnPageChangeListener(this);
        }

        public void addTab(TabHost.TabSpec tabSpec, Class<?> clss, Bundle args) {
            tabSpec.setContent(new DummyTabFactory(mContext));
            String tag = tabSpec.getTag();

            TabInfo info = new TabInfo(tag, clss, args);
            mTabs.add(info);
            mTabHost.addTab(tabSpec);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }

        @Override
        public Fragment getItem(int position) {
            TabInfo info = mTabs.get(position);
            return Fragment.instantiate(mContext, info.clss.getName(), info.args);
        }

        @Override
        public void onTabChanged(String tabId) {
            int position = mTabHost.getCurrentTab();
            mViewPager.setCurrentItem(position);
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            mTabHost.setCurrentTab(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    }

    public static class SyncStatusUpdaterFragment extends Fragment implements
            DetachableResultReceiver.Receiver {
        public static final String TAG = SyncStatusUpdaterFragment.class.getName();

        // private boolean mSyncing = false;
        private DetachableResultReceiver mReceiver;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            setRetainInstance(true);
            mReceiver = new DetachableResultReceiver(new Handler());
            mReceiver.setReceiver(this);
        }

        /** {@inheritDoc} */
        public void onReceiveResult(int resultCode, Bundle resultData) {

            TabsPagerActivity activity = (TabsPagerActivity) getActivity();
            if (activity == null) {
                return;
            }
            activity.setProgressBarIndeterminateVisibility(Boolean.TRUE);

            switch (resultCode) {
                case SyncService.STATUS_RUNNING: {
                    activity.setProgressBarIndeterminateVisibility(Boolean.TRUE);
                    // mSyncing = true;
                    break;
                }
                case SyncService.STATUS_FINISHED: {
                    activity.setProgressBarIndeterminateVisibility(Boolean.FALSE);
                    // mSyncing = false;
                    Toast.makeText(activity, R.string.toast_sync_finished, Toast.LENGTH_SHORT)
                            .show();
                    break;
                }
                case SyncService.STATUS_ERROR: {
                    /**
                     * Error happened down in SyncService, show as toast.
                     */

                    activity.setProgressBarIndeterminateVisibility(Boolean.FALSE);

                    // mSyncing = false;
                    final String errorText = getString(R.string.toast_sync_error,
                            resultData.getString(Intent.EXTRA_TEXT));
                    Toast.makeText(activity, errorText, Toast.LENGTH_LONG).show();
                    break;
                }
            }
        }
    }

}

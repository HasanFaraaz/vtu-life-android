package com.V4Creations.vtulife.view.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.V4Creations.vtulife.R;
import com.V4Creations.vtulife.controller.adapters.DirectoryListingAdapter;
import com.V4Creations.vtulife.controller.adapters.VTULifeFragmentAdapter.FragmentInfo;
import com.V4Creations.vtulife.controller.server.ResourceLoaderManager;
import com.V4Creations.vtulife.model.ActionBarStatus;
import com.V4Creations.vtulife.model.ResourceItem;
import com.V4Creations.vtulife.model.ResourceStackItem;
import com.V4Creations.vtulife.model.interfaces.ResourceLoaderInterface;
import com.V4Creations.vtulife.util.GoogleAnalyticsManager;
import com.V4Creations.vtulife.view.activity.VTULifeMainActivity;
import com.google.analytics.tracking.android.Tracker;

import de.keyboardsurfer.android.widget.crouton.Style;

public class DirectoryListingFragment extends ListFragment implements
		ResourceLoaderInterface, FragmentInfo {

	String TAG = "DirectoryListingMainFragment";
	private VTULifeMainActivity activity;
	private ActionBarStatus mActionBarStatus;
	private DirectoryListingAdapter mAdapter;
	private LinearLayout progressLinearLayout;
	private ProgressBar progressBar;
	private TextView progressTextView;
	private Tracker mTracker;
	private ResourceLoaderManager mResourceLoaderManager;

	public DirectoryListingFragment() {
		mActionBarStatus = new ActionBarStatus();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		activity = (VTULifeMainActivity) getActivity();
		return inflater.inflate(R.layout.fragemnt_directory, null);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mTracker = GoogleAnalyticsManager.getGoogleAnalyticsTracker(activity);
		initView();
		hideProgressLinearLayout();
		initResourceLoader();
	}

	private void initResourceLoader() {
		mResourceLoaderManager = new ResourceLoaderManager(activity, this);
		mResourceLoaderManager.loadDirectory(null, -1);
	}

	private void initView() {
		initListView();
		progressLinearLayout = (LinearLayout) getView().findViewById(
				R.id.progressLinearLayout);
		progressBar = (ProgressBar) getView().findViewById(R.id.progressBar);
		progressTextView = (TextView) getView().findViewById(
				R.id.progressTextView);
	}

	private void initListView() {
		mAdapter = new DirectoryListingAdapter(activity);
		setListAdapter(mAdapter);
	}

	@Override
	public void onListItemClick(ListView l, View view, int position, long id) {
		super.onListItemClick(l, view, position, id);
		ResourceItem resourceItem = mAdapter.getItem(position);
		GoogleAnalyticsManager.infomGoogleAnalytics(mTracker,
				GoogleAnalyticsManager.CATEGORY_NOTES,
				GoogleAnalyticsManager.ACTION_FOLDER, resourceItem.name, 0L);
		if (resourceItem.ext.equals("dir")) {
			mResourceLoaderManager.loadDirectory(resourceItem.href, position);
		} else {
			mResourceLoaderManager.downloadFile(activity, resourceItem.href);
			activity.showCrouton(R.string.downloading_started, Style.INFO,
					false);
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.directory_listing_main_layout, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		if (activity != null && !activity.isNavigationDrawerOpen()) {
			MenuItem backMenuItem = menu.findItem(R.id.menu_back);
			MenuItem refreshMenuItem = menu.findItem(R.id.menu_refresh);
			if (mResourceLoaderManager != null
					&& mResourceLoaderManager.isGoBack()) {
				backMenuItem.setEnabled(true);
				backMenuItem.setIcon(R.drawable.ic_action_previous_item);
			} else {
				backMenuItem.setEnabled(false);
				backMenuItem
						.setIcon(R.drawable.ic_action_previous_item_disabled);
			}
			if (mResourceLoaderManager != null
					&& !mResourceLoaderManager.isReloading()) {
				refreshMenuItem.setIcon(R.drawable.ic_action_refresh);
				refreshMenuItem.setEnabled(true);
			} else {
				refreshMenuItem.setIcon(R.drawable.ic_action_refresh_disabled);
				refreshMenuItem.setEnabled(false);
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		switch (item.getItemId()) {
		case R.id.menu_back:
			mResourceLoaderManager.goBack();
			return true;
		case R.id.menu_refresh:
			mResourceLoaderManager.reload();
			return true;
		}
		return false;
	}

	private void hideProgressLinearLayout() {
		progressLinearLayout.setVisibility(View.GONE);
	}

	private void showProgressLinearLayout() {
		progressLinearLayout.setVisibility(View.VISIBLE);
		progressBar.setVisibility(View.VISIBLE);
		progressTextView.setText(R.string.please_wait_);
	}

	private void showReload() {
		progressLinearLayout.setVisibility(View.VISIBLE);
		progressBar.setVisibility(View.GONE);
		progressTextView.setText(R.string.please_reload);
	}

	@Override
	public String getTitle() {
		return DirectoryListingFragment.getFeatureName(activity);
	}

	@Override
	public ActionBarStatus getActionBarStatus() {
		return mActionBarStatus;
	}

	public static String getFeatureName(Context context) {
		return context.getString(R.string.notes);
	}

	@Override
	public void onStartLoading() {
		mAdapter.clear();
		showProgressLinearLayout();
		mActionBarStatus.subTitle = getString(R.string.loading);
		callActionBarReflect();
	}

	private void callActionBarReflect() {
		activity.reflectActionBarChange(mActionBarStatus,
				VTULifeMainActivity.ID_DIRECTORY_LISTING_FRAGMENT, true);
	}

	@Override
	public void onLoadingSuccess(ResourceStackItem resourceStackItem) {
		hideProgressLinearLayout();
		if (resourceStackItem.mResourceItems.size() != 0)
			mAdapter.supportAddAll(resourceStackItem.mResourceItems);
		else {
			activity.showCrouton(R.string.empty_folder, Style.INFO, false);
			if (mResourceLoaderManager.isGoBack())
				mResourceLoaderManager.goBack();
			else
				showReload();
		}
		mActionBarStatus.subTitle = resourceStackItem.mDirName;
		callActionBarReflect();
	}

	@Override
	public void onLoadingFailure(String message, String trackMessage) {
		activity.showCrouton(message, Style.ALERT, false);
		mActionBarStatus.subTitle = null;
		callActionBarReflect();
		showReload();
		GoogleAnalyticsManager.infomGoogleAnalytics(mTracker,
				GoogleAnalyticsManager.CATEGORY_NOTES,
				GoogleAnalyticsManager.ACTION_NETWORK_ERROR, trackMessage, 0L);
	}
}

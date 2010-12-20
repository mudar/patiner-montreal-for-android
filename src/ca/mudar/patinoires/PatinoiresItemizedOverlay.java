package ca.mudar.patinoires;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;


public class PatinoiresItemizedOverlay extends ItemizedOverlay<OverlayItem> {
	protected static final String TAG = "PatinoiresItemizedOverlay";

	private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
	public Context mContext;

	public PatinoiresItemizedOverlay( Drawable defaultMarker ,  Context context ) {
		super(boundCenterBottom(defaultMarker));
		mContext = context;
	}

	public void addOverlay(OverlayItem overlay) {
		mOverlays.add(overlay);
		populate();
	}

	@Override
	protected OverlayItem createItem(int i) {
		return mOverlays.get(i);
	}

	@Override
	public int size() {
		return mOverlays.size();
	}

	@Override
	protected boolean onTap(int index) {
		OverlayItem item = mOverlays.get(index);

		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		builder.setTitle(item.getTitle());
		builder.setMessage(item.getSnippet());

		final String searchQuery = item.getTitle();
		builder.setPositiveButton( android.R.string.ok , new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				Intent intent = new Intent().setClass( mContext , PatinoiresSearchable.class);
				intent.setAction("android.intent.action.SEARCH");
				intent.putExtra( SearchManager.QUERY , searchQuery );
				intent.putExtra( "itemizedOverlay" , "park" );
				mContext.startActivity(intent);
			}

		} );
		builder.setNegativeButton( android.R.string.cancel , null );

		builder.show();
		return true;
	}

}

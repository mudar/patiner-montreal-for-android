
package ca.mudar.patinoires.ui.widgets;

import ca.mudar.patinoires.R;

import com.google.android.maps.OverlayItem;
import com.readystatesoftware.mapviewballoons.BalloonOverlayView;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class MyBalloonOverlayView<Item extends OverlayItem> extends
        BalloonOverlayView<MyOverlayItem> {

    private TextView title;
    private TextView snippet;
    private TextView extra;

    public MyBalloonOverlayView(Context context, int balloonBottomOffset) {
        super(context, balloonBottomOffset);
    }

    @Override
    protected void setupView(Context context, final ViewGroup parent) {

        // inflate our custom layout into parent
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.custom_balloon_overlay, parent);

        // setup our fields
        title = (TextView) v.findViewById(R.id.balloon_item_title);
        snippet = (TextView) v.findViewById(R.id.balloon_item_snippet);
        extra = (TextView) v.findViewById(R.id.balloon_item_extra);

        // implement balloon close
        ImageView close = (ImageView) v.findViewById(R.id.balloon_close);
        close.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                parent.setVisibility(GONE);
            }
        });

    }

    @Override
    protected void setBalloonData(MyOverlayItem item, ViewGroup parent) {

        // map our custom item data to fields
        title.setText(item.getTitle());
        snippet.setText(item.getSnippet());

        if (item.getExtra() == null || item.getExtra().isEmpty()) {
            extra.setText("");
            extra.setVisibility(View.GONE);
        }
        else {
            extra.setVisibility(View.VISIBLE);
            extra.setText(item.getExtra());
        }

    }

}

package android.tether.dtn;

import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.tether.R;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class DtnMessageArrayAdapter extends ArrayAdapter<DtnMessage> {
	private List<DtnMessage> items;
	private LayoutInflater inflater;
	private Resources r;
	
	public DtnMessageArrayAdapter(Context context, int resID, List<DtnMessage> items,Resources r) {
		super(context, resID, items);
		this.items = items;
		this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.r = r;
	}
	
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
	    View v = convertView;
	    if (v == null) {
		    v = inflater.inflate(R.layout.dtnmessages_list_cell, null);
	    }
	    // 文字列をセット
	    DtnMessage dtnMsg = (DtnMessage)items.get(position);
	    TextView dtnMsg_TextInfo = (TextView)v.findViewById(R.id.cell_text);
	    dtnMsg_TextInfo.setText(dtnMsg.toString());

	    // アイコンをセット
	    //ImageView appInfoImage = (ImageView)v.findViewById(R.id.cell_image);
	    //appInfoImage.setImageDrawable(dtnMsg.toIconPicture(this.r));
	
	    return v;
    }
}
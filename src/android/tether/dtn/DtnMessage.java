package android.tether.dtn;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.tether.R;

public class DtnMessage implements Cloneable {
	//body
	public String name;
	public String address;
	public String facebook;
	
	//mac-address
	public String mac_address;
	
	@Override
	public String toString(){
		return "名前: "+this.name+"\n"+
				"住所: "+this.address+"\n";
	}
	public Drawable toIconPicture(Resources r){
		int resource_id;
		boolean image_flg = true;
		try{
			Integer.parseInt(mac_address.substring(0,1));
			image_flg = false;
		}catch(Exception e){
		}
		resource_id = (image_flg)? R.drawable.ito_hideaki : R.drawable.kato_ai;
		return r.getDrawable(resource_id);
	}
	
	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError(e.toString());
		}
	}
}

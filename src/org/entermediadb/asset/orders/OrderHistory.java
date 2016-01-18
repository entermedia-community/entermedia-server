package org.entermediadb.asset.orders;

import org.openedit.xml.ElementData;

public class OrderHistory extends ElementData {
	public static final OrderHistory EMPTY = new OrderHistory();

	public String getNoteSnip(int inSize) {
		String note = getNote();
		if (note != null) {
			String snip = note.substring(0, Math.min(note.length(), inSize));
			if (snip.length() < note.length()) {
				snip = snip + "...";
			}
			return snip;
		}
		return null;
	}

	public String getNote() {
		return get("note");
	}

	public String getHistoryType() {
		return get("historytype");
	}

	public void setHistoryType(String inS) {
		setProperty("historytype", inS);
	}

	public String toString() {
		return getHistoryType();
	}

	public int addItemCount() {
		return addCount("itemcount");
	}

	public int addItemSuccessCount() {
		return addCount("itemsuccesscount");
	}

	public int addItemErrorCount() {
		return addCount("itemerrorcount");
	}

	protected int addCount(String inField) {
		String existing = get(inField);
		if (existing == null) {
			existing = "0";
		}
		int count = Integer.parseInt(existing) + 1;
		setProperty(inField, String.valueOf(count));
		return count;
	}

	public int getItemSuccessCount() {

		return intvalue("itemsuccesscount");
	}

	public int getItemErrorCount() {
		return intvalue("itemerrorcount");

	}

	public int getItemCount() {
		return intvalue("itemcount");

	}

	private int intvalue(String inProperty) {
		String val = get(inProperty);
		if (val == null) {
			return 0;
		}
		try {
			return Integer.valueOf(val);
		} catch (Exception e) {
			return 0;
		}
	}

	
	
	public boolean hasCountChanged(OrderHistory inHistory){
		if(inHistory == null){
			return true;
		}
		if(inHistory.getItemCount() != getItemCount() || inHistory.getItemErrorCount() != getItemErrorCount() || inHistory.getItemSuccessCount() != getItemSuccessCount() ){
			return true;
		}
		return false;
	}
	// public void setAssetIds(List<String> inAssetids)
	// {
	// StringBuffer assets = new StringBuffer();
	// for( String id : inAssetids)
	// {
	// if( assets.length() > 0)
	// {
	// assets.append(" ");
	// }
	// assets.append(id);
	// }
	// setProperty("assetids", assets.toString());
	// }
}

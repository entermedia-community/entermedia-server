package org.entermedia.location;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;




public class GeoCoder {

	// http://maps.google.com/maps/geo?q=1600+Amphitheatre+Parkway,+Mountain+View,+CA&output=xml&key=abcdefg
	int delay = 0;
	public List getPositions(String lookupString)  {
		
	
		
		ArrayList l = new ArrayList();
		try {
			if(lookupString == null){
				return null;
			}
			lookupString = URLEncoder.encode(lookupString, "UTF-8");
			try
			{
				Thread.sleep(delay);
			}
			catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
			URL url = new URL("http://maps.google.com/maps/geo?q="+lookupString+"&output=csv&key=ABQIAAAAV_4kQH5zDc5ZTkrZLZTDgxTwM0brOpm-All5BF6PoaKBxRWWERScmQEK2Y_2bQMkZs6DY9U1FD096g ");
			StringWriter out = new StringWriter();
			InputStream in = url.openConnection().getInputStream();
			byte[] input = new byte[1024];
			int size = 0;
			while ((size = in.read(input)) != -1) {
				out.append(new String(input, 0, size, "UTF-8"));
			}
			String responseString = out.toString();
			String[] data = responseString.split(",");
			int response = Integer.parseInt(data[0]);
			if(response == 620){
				delay = delay + 10000;
			}
			if(response != 602){
			 Double lat = Double.parseDouble(data[2]);
			 Double longi = Double.parseDouble(data[3]);
			Double accuracy = Double.parseDouble(data[1]);
			
			Position p = new Position(lat, longi);
			p.setAccuracy(accuracy);
			l.add(p);
			}
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
		
			e.printStackTrace();
		}
		return l;
		//List positions = parseGoogleResponse(responseString);
	}

//	protected List parseGoogleResponse(String inXml) throws Exception {
//		Document stockQuoteDocument;
//		try {
//			stockQuoteDocument = new SAXReader().read(new StringReader(inXml));
//		} catch (DocumentException e) {
//			throw new Exception("Error parsing stock quote web service XML: "
//					+ e.getMessage());
//		}
//
//		Element root = stockQuoteDocument.getRootElement();
//		root = root.element("Response");
//		if (root == null) {
//			log.info("Could not get a response ");
//			return null;
//		}
//
//		Element stockElement = root.element("Stock");
//		String lastPrice = stockElement.elementText("Last");
//		String dateString = stockElement.elementText("Date");
//		String timeString = stockElement.elementText("Time");
//		String symbol = stockElement.elementText("Symbol");
//		quote.setSymbol(symbol.toUpperCase());
//		quote.setPrice(new Money(lastPrice));
//		if (quote.getPrice().equals(Money.ZERO) && "N/A".equals(dateString)) {
//			log.info("Invalid stock ");
//			return null;
//		}
//		try {
//			quote.setDate(getDateFormat().parse(dateString + " " + timeString));
//		} catch (Exception e) {
//			log.info("Error parsing date information: " + e.getMessage());
//		}
//		return quote;
//	}

}
//<kml xmlns="http://earth.google.com/kml/2.0">
//<Response>
//  <name>1600 amphitheatre mountain view ca</name>
//  <Status>
//    <code>200</code>
//    <request>geocode</request>
//  </Status>
//  <Placemark>
//    <address> 
//      1600 Amphitheatre Pkwy, Mountain View, CA 94043, USA
//    </address>
//    <AddressDetails Accuracy="8">
//      <Country>
//        <CountryNameCode>US</CountryNameCode>
//	  <AdministrativeArea>
//          <AdministrativeAreaName>CA</AdministrativeAreaName>
//         <SubAdministrativeArea>
//           <SubAdministrativeAreaName>Santa Clara</SubAdministrativeAreaName>
//           <Locality>
//             <LocalityName>Mountain View</LocalityName>
//  	       <Thoroughfare>
//               <ThoroughfareName>1600 Amphitheatre Pkwy</ThoroughfareName>
//             </Thoroughfare>
//             <PostalCode>
//               <PostalCodeNumber>94043</PostalCodeNumber>
//             </PostalCode>
//           </Locality>
//         </SubAdministrativeArea>
//       </AdministrativeArea>
//     </Country>
//   </AddressDetails>
//   <Point>
//     <coordinates>-122.083739,37.423021,0</coordinates>
//   </Point>
// </Placemark>
//</Response>
//</kml>
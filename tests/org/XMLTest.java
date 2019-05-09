package org;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.openedit.OpenEditException;

import com.google.common.io.Files;
  
public class XMLTest {
      
	static String JSON_STR = "{\n" + 
			"    \"Attachments\": [],\n" + 
			"    \"JobName\": [{Image_Name},\n" + 
			"    \"Labels\": [],\n" + 
			"    \"Medias\": [\n" + 
			"        {\n" + 
			"            \"Identifier\": \"abb3f7e5-8d7e-49ba-84c5-674a55edb700\",\n" + 
			"            \"Data\": \"{Data}\",\n" + 
			"            \"Description\": \"{Description}\",\n" + 
			"            \"Files\": [\n" + 
			"                \"{Image_Path}\"\n" + 
			"            ],\n" + 
			"            \"Name\": \"Original\"\n" + 
			"        }\n" + 
			"    ],\n" + 
			"    \"Priority\": 0,\n" + 
			"    \"Variables\": [\n" +
			"                     {\n" +
			"                        \"Identifier\":\"2972dbcb-d724-40c9-9a52-b13467a2983b\",\n" +
			"                        \"DefaultValue\":\"\",\n" +
			"                        \"Description\":\"The full path for XML metadata\",\n" +
			"                        \"Name\":\"XML_Path\",\n" +
			"                        \"TypeCode\":\"Uri\",\n" +
			"                        \"Value\":\"XML_Path\",\n" +
			"                        \"Name\":\"{XmlPath}\"\n"+
		    "                     }]\n" + 
			"}";

    public static void main(String[] args) throws IOException {
    		String Image_Name = "ABC.jpg";
    		String Data = "Data";
    		String Description = "Description";
    		String Image_Path = "\\\\data02.gpm.cbcrc.ca\\GESTION_MEDIAS\\traitement_medias\\reception\\imagerie\\vers_avid_cdi\\abc.jpg";
    		String XmlPath = "\\\\data02.gpm.cbcrc.ca\\GESTION_MEDIAS\\traitement_medias\\reception\\imagerie\\vers_avid_cdi\\xml.jpg";
    		
    		String payload = JSON_STR.replace("{Image_Name}", Image_Name)
				.replace("{Data}", Data)
				.replace("{Description}", Description)
				.replace("{Image_Path}", Image_Path)
				.replace("{XmlPath}", XmlPath);
			
    		System.out.println(payload);
    		
    	/*
    	File f = new File("/Users/hassanmrad/test1", "a.png");
    	System.out.println(f.getName() + " " + f.getCanonicalFile() + " " +f.getAbsolutePath());
    	System.out.println(f.getName().substring(f.getName().lastIndexOf('.')));
    	//File dest = new File("/Users/hassanmrad/test1/dest",f.getName());
    	//Files.copy(f, dest);
    	
    	
    	String tmp = f.getName();
		int idxP = tmp.lastIndexOf('.');
		
		String fileName = tmp.substring(0, idxP);
		String extension = tmp.substring(idxP+1);
		
		System.out.println(fileName + " " + extension);
		
		String text = "{a}";

		System.out.print(text.replace("{a}", "VALUE_OF_A"));
		*/
    	/*
        Document document = DocumentFactory.getInstance().createDocument();
        // Create the root element of xml file
        Element root = document.addElement("Metadata");
        Element entry = root.addElement("Entry");
        Element tag = entry.addElement("Tag");
        tag.setText("ClipName");
        Element value = entry.addElement("Value");
        value.setText("CAS KRISHNAN ATHENA CORONER 20190327055127");
        
        entry = root.addElement("Entry");
        tag = entry.addElement("Tag");
        tag.setText("Resume");
        value = entry.addElement("Value");
        value.setText("");
        
        entry = root.addElement("Entry");
        tag = entry.addElement("Tag");
        tag.setText("Date tournage +");
        value = entry.addElement("Value");
        value.setText("20190327");
        */
        
        /*
        Element root = document.addElement("person");
        // Add some attributes to root element.
        root.addAttribute("attributeName", "attributeValue");
        // Add the element name in root element.
        Element name = root.addElement("name");
        name.addText("Mahendra");
        // Add the element age in root element.
        Element age = root.addElement("age");
        age.addText("29");
        */
    	/*
        // Create a file named as person.xml
        FileOutputStream fos = new FileOutputStream("/Users/hassanmrad/Documents/person.xml");
        // Create the pretty print of xml document.
        OutputFormat format = OutputFormat.createPrettyPrint();//createCompactFormat();
        // Create the xml writer by passing outputstream and format
        XMLWriter writer = new XMLWriter(fos, format);
        // Write to the xml document
        writer.write(document);
        // Flush after done
        writer.flush();*/
    }
}

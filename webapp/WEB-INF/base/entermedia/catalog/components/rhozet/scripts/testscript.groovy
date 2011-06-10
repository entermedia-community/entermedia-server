import org.openedit.data.Searcher 

import groovy.xml.MarkupBuilder
import org.openedit.data.Searcher 
import org.openedit.entermedia.MediaArchive 
import org.openedit.entermedia.search.AssetSearcher 


//MediaArchive mediaarchive = context.getPageValue("mediaarchive");
//catalogid = mediaarchive.getCatalogId();
//
//AssetSearcher searcher = mediaarchive.getAssetSearcher();
//
////The conversion searcher tracks the conversions related to individual assets
//Searcher conversions = mediaarchive.getSearcherManager().getSearcher(mediaarchive.catalogid, "conversion");
//
//Searcher converiontypes = mediaarchive.getSearcherManager().getSearcher(catalogid, "rhozetprofile");
updateProfiles();

def writer = new StringWriter()
def xml = new MarkupBuilder(writer)
//
//<cnpsXML CarbonAPIVer="1.2" TaskType="JobQueue">
//<Sources>
//<Module_0 Filename="example.avi" />
//</Sources>
//<Destinations>
//<Module_0 PresetGUID="{A7264AEF-FF57-42E0-BBAD-CCF546CD515F}">
//</Destinations>
//</cnpsXML>
xml.cnpsXML(TaskType:'JobQueue', carbonAPIVer:'1.2') {
	sources {
		Module_0(filename:"example.avi")
	}
	destinations() {
		Module_0(PresetGUID:'{A7264AEF-FF57-42E0-BBAD-CCF546CD515F}')
	}
}
//def somexml= writer.toString();
//def root = new XmlParser().parseText(somexml);
//println context

//println root.@TaskType;

//println writer.toString()

//def bob = callServer(somexml);

//println bob.@TaskType;

def callServer(String somexml)
{
	def commandString = "CarbonAPIXML1 ${somexml.length()} " + somexml;
	println commandString;
	def writer = new StringWriter();
	s = new Socket("localhost", 1120);
	s.withStreams { input, output -> 
		output << commandString.toString();
		reader = input.newReader();
		while(reader.ready())
		{
			writer.write(reader.read());
		}
	}
	return writer.toString();
}

def evaluateJob(String source, String destination)
{
	def writer = new StringWriter()
	def xml = new MarkupBuilder(writer)

	xml.cnpsXML(TaskType:'JobEvaluate', carbonAPIVer:'1.2') 
	{
		Sources
		{
			Module_0
			{
				
			}
		}
		Destinations
		{
			Module_0
			{
				
			}
		}
	}
	response = callServer(writer.toString())
	println response.toString();
}

def updateProfiles() {
	
	def writer = new StringWriter()
	def xml = new MarkupBuilder(writer)

	xml.cnpsXML(TaskType:'ProfileList', carbonAPIVer:'1.2') {
		ProfileAttributes (ProfileType: "something")
	}
	response = callServer(writer.toString())
	println response.toString();
}
	

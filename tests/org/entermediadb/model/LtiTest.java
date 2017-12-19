package org.entermediadb.model;

import java.util.Map;
import java.util.TreeMap;

import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.authenticate.AutoLoginLti;
import org.openedit.WebPageRequest;
import org.openedit.data.SearcherManager;

public class LtiTest extends BaseEnterMediaTest
{
	public void testLogin()
	{
//This worked, from LTI test site
//		INFO: created POST&http%3A%2F%2F65.28.255.179%3A8080%2Fassets%2Femshare%2Findex.html&context_id%3D115%26context_label%3DWorld%2520of%2520Water%26context_title%3DWorld%2520of%2520Water%26context_type%3DCourseSection%26ext_lms%3Dmoodle-2%26launch_presentation_document_target%3Diframe%26launch_presentation_locale%3Den%26launch_presentation_return_url%3Dhttps%253A%252F%252Flti.tools%252Ftest%252Ftc-return.php%253Fcourse%253D115%2526launch_container%253D2%2526instanceid%253D3%2526sesskey%253DN7oPpfYugI%26lis_outcome_service_url%3Dhttps%253A%252F%252Flti.tools%252Ftest%252Ftc-outcomes.php%26lis_person_contact_email_primary%3Dbarbaragardner249%2540example.com%26lis_person_name_family%3DGardner%26lis_person_name_full%3DBarbara%2520Gardner%26lis_person_name_given%3DBarbara%26lis_person_sourcedid%3D2015123%26lis_result_sourcedid%3Djhhsnmua603ihhgga13g8pv5l2%253A%253A%253A115%253A%253A%253A249%253A%253A%253A%257B%2522data%2522%253A%257B%2522instanceid%2522%253A%25223%2522%252C%2522userid%2522%253A%2522249%2522%252C%2522launchid%2522%253A180548532%257D%252C%2522hash%2522%253A%2522290ab06bcf0ed944243b530718bdad0d33277884842e3f304cfb95c56c91b752%2522%257D%26lti_message_type%3Dbasic-lti-launch-request%26lti_version%3DLTI-1p0%26oauth_callback%3Dabout%253Ablank%26oauth_consumer_key%3Dentermediadb%26oauth_nonce%3D95ab04a2087111af27d89c41a9217d00%26oauth_signature_method%3DHMAC-SHA1%26oauth_timestamp%3D1512752440%26oauth_version%3D1.0%26resource_link_description%3DA%2520quick%2520revision%2520PowerPoint%2520about%2520the%2520Water%2520cycle.%250D%250A%250D%250AMake%2520sure%2520you%2527re%2520clear%2520about%2520it%2521%26resource_link_id%3D3%26resource_link_title%3DRevise%2520the%2520Water%2520cycle%26roles%3DLearner%26tool_consumer_info_product_family_code%3Dmoodle%26tool_consumer_info_version%3D2015051101%26tool_consumer_instance_guid%3Dschool.demo.moodle.net%26tool_consumer_instance_name%3DMount%2520Orange%2520School%26user_id%3D249
//		INFO: created POST&http%3A%2F%2F65.28.255.179%3A8080%2Fassets%2Femshare%2Findex.html&context_id%3D20347%26context_label%3Dtest%26context_title%3Dtest%26context_type%3DCourseSection%26ext_lms%3Dmoodle-2%26ext_user_username%3Dentermediadb%26launch_presentation_document_target%3Dwindow%26launch_presentation_locale%3Den%26launch_presentation_return_url%3Dhttps%253A%252F%252Ffg-riyadh.smartway-dev.com%252Fnajeh%252Fmod%252Flti%252Freturn.php%253Fcourse%253D20347%2526launch_container%253D4%2526instanceid%253D20%2526sesskey%253Db4M7sDMii5%26lis_outcome_service_url%3Dhttps%253A%252F%252Ffg-riyadh.smartway-dev.com%252Fnajeh%252Fmod%252Flti%252Fservice.php%26lis_person_contact_email_primary%3Dentermediadb%2540smartway-me.com%26lis_person_name_family%3Dentermediadb%26lis_person_name_full%3Dentermediadb%2520entermediadb%26lis_person_name_given%3Dentermediadb%26lis_result_sourcedid%3D%257B%2522data%2522%253A%257B%2522instanceid%2522%253A%252220%2522%252C%2522userid%2522%253A%252248049%2522%252C%2522typeid%2522%253A%252210%2522%252C%2522launchid%2522%253A402863958%257D%252C%2522hash%2522%253A%2522ef7c619738e79b8d073da5d392b3502f5cde8c4741d782864e7e44f35bb8cb9e%2522%257D%26lti_message_type%3Dbasic-lti-launch-request%26lti_version%3DLTI-1p0%26oauth_callback%3Dabout%253Ablank%26oauth_consumer_key%3Dentermediadb%26oauth_nonce%3D4bc8777d6990ad27492845e3e1d483ce%26oauth_signature_method%3DHMAC-SHA1%26oauth_timestamp%3D1512753623%26oauth_version%3D1.0%26resource_link_id%3D20%26resource_link_title%3Dcburkey%2520lti%26roles%3DInstructor%26tool_consumer_info_product_family_code%3Dmoodle%26tool_consumer_info_version%3D2017051501.03%26tool_consumer_instance_description%3DFuture%2520Gate%2520%257C%2520%25D8%25A8%25D9%2588%25D8%25A7%25D8%25A8%25D8%25A9%2520%25D8%25A7%25D9%2584%25D9%2585%25D8%25B3%25D8%25AA%25D9%2582%25D8%25A8%25D9%2584%26tool_consumer_instance_guid%3Dfg-riyadh.smartway-dev.com%26tool_consumer_instance_name%3DFutureGate%26user_id%3D48049
//Dec 08, 2017 12:20:23 PM org.entermediadb.authenticate.AutoLoginLti createRequest
//INFO: sha noyjbzucc+7xB4LM4HKAn/+lWBQ=
//Dec 08, 2017 12:20:23 PM org.entermediadb.authenticate.AutoLoginLti autoLogin
//INFO: Trying to login and failing expected: obAb+y79AO+CeuZl3PvXzGMCm0o= we got noyjbzucc+7xB4LM4HKAn/+lWBQ= using private key of fEbd8hUFC93RRZ8PwRA44PMV2uGNttc5
//D
		
		
		
		AutoLoginLti lti = (AutoLoginLti)getBean("autoLoginLti");
		
		String encoded = lti.encode("this space");
		System.out.println(" encoded " + encoded + " THEN " + lti.encode(encoded) );
		//fEbd8hUFC93RRZ8PwRA44PMV2uGNttc5
		Map params = new TreeMap();
		WebPageRequest req = getFixture().createPageRequest();
		//req.putPageValue("originalurl", "https://smartway.entermediadb.net/assets/emshare/index.html");
req.putPageValue("originalurl", "http://65.28.255.179:8080/assets/emshare/index.html");
params.put("context_id","20347");
params.put("context_label","test");
params.put("context_title","test");
params.put("context_type","CourseSection");
params.put("ext_lms","moodle-2");
params.put("ext_user_username","entermediadb");
params.put("launch_presentation_document_target","window");
params.put("launch_presentation_locale","en");
params.put("launch_presentation_return_url","https://fg-riyadh.smartway-dev.com/najeh/mod/lti/return.php?course=20347&launch_container=4&instanceid=21&sesskey=b4M7sDMii5");
params.put("lis_course_section_sourcedid","");
params.put("lis_outcome_service_url","https://fg-riyadh.smartway-dev.com/najeh/mod/lti/service.php");
params.put("lis_person_contact_email_primary","entermediadb@smartway-me.com");
params.put("lis_person_name_family","entermediadb");
params.put("lis_person_name_full","entermediadb entermediadb");
params.put("lis_person_name_given","entermediadb");
params.put("lis_person_sourcedid","");
params.put("lis_result_sourcedid","{\"data\":{\"instanceid\":\"21\",\"userid\":\"48049\",\"typeid\":\"11\",\"launchid\":1101582439},\"hash\":\"e094dff43b48d20d6682bb35c87d5712ed02f5e8efe49936b5e0e252dc74fb73\"}");
params.put("lti_message_type","basic-lti-launch-request");
params.put("lti_version","LTI-1p0");
params.put("oauth_callback","about:blank");
params.put("oauth_consumer_key","jisc.ac.uk");
params.put("oauth_nonce","723a425852f80c3ac2868893c92aacf4");
params.put("oauth_signature_method","HMAC-SHA1");
params.put("oauth_timestamp","1512754486");
params.put("oauth_version","1.0");
params.put("resource_link_description","");
params.put("resource_link_id","21");
params.put("resource_link_title","Test site");
params.put("roles","Instructor");
params.put("tool_consumer_info_product_family_code","moodle");
params.put("tool_consumer_info_version","2017051501.03");
params.put("tool_consumer_instance_description","Future Gate | بوابة المستقبل");
params.put("tool_consumer_instance_guid","fg-riyadh.smartway-dev.com");
params.put("tool_consumer_instance_name","FutureGate");
params.put("user_id","48049");
params.put("oauth_signature","Sxnpg/BF0g50T5FNb1mnKY1+3CE=");

		//rying to login and failing expected: Uu4Z8Dwv3ytZOdBETtCDls7SE6U= we 
		//got NzuB5yorzAzoSsP6n+zRpSkbBbY= using private key of fEbd8hUFC93RRZ8PwRA44PMV2uGNttc5
		
		//INFO: Trying to login and failing expected: 
		//Uu4Z8Dwv3ytZOdBETtCDls7SE6U= we got 1Gp/2RZR1AYTBWL24JTKtJeZZ6s= using private key of fEbd8hUFC93RRZ8PwRA44PMV2uGNttc5
		
		//params.put("oauth_signature", "Uu4Z8Dwv3ytZOdBETtCDls7SE6U=");
		//lti.autoLogin(inReq)
		req.putAllRequestParameters(params);
		
		SearcherManager searcher = (SearcherManager)getBean("searcherManager");
		//searcher.getCacheManager().put("getEncryptionKey", "ltiautologinkey","fEbd8hUFC93RRZ8PwRA44PMV2uGNttc5");
		searcher.getCacheManager().put("getEncryptionKey", "ltiautologinkey","secret");
		
		lti.autoLogin(req);
	}
}

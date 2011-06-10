package org.entermedia.amazon;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.List;

import org.openedit.repository.BaseRepository;
import org.openedit.repository.ContentItem;
import org.openedit.repository.InputStreamItem;
import org.openedit.repository.Repository;
import org.openedit.repository.RepositoryException;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

public class S3Repository extends BaseRepository{

	
	protected String fieldBucket;
	protected AmazonS3 fieldConnection;
	protected String fieldSecretKey;
	protected String fieldAccessKey;
	
	public String getSecretKey() {
		return fieldSecretKey;
	}

	public void setSecretKey(String inSecretKey) {
		fieldSecretKey = inSecretKey;
	}

	public String getAccessKey() {
		return fieldAccessKey;
	}

	public void setAccessKey(String inAccessKey) {
		fieldAccessKey = inAccessKey;
	}

	public String getBucket() {
		return fieldBucket;
	}

	public void setBucket(String inBucket) {
		fieldBucket = inBucket;
	}

	public AmazonS3 getConnection() {
		if (fieldConnection == null) {
			
			AWSCredentials credentials = new AWSCredentials() {
				
				
				public String getAWSSecretKey() {
					return getSecretKey();
				}
				
				
				public String getAWSAccessKeyId() {
					return getAccessKey();
				}
			};
			
			
			fieldConnection = new AmazonS3Client(credentials);
			if(!fieldConnection.doesBucketExist(getBucket())){
				fieldConnection.createBucket(getBucket());
			}
		}

		return fieldConnection;
		
		
	}

	public void setConnection(AmazonS3 inConnection) {
		fieldConnection = inConnection;
	}

	
	public ContentItem get(String inPath) throws RepositoryException {
//		String path = inPath.substring(getPath().length());
//		if (path.length() == 0)
//		{
//			path = "/";
//		} 
//		
		S3Object object = getConnection().getObject(new GetObjectRequest(getBucket(), inPath));
          System.out.println("Content-Type: "  + object.getObjectMetadata().getContentType());
          S3ContentItem item = new S3ContentItem();
          item.setPath(inPath);
  		item.setAbsolutePath(inPath);

  		return item;
          
          
	}

	@Override
	public ContentItem getStub(String inPath) throws RepositoryException {
		// TODO Auto-generated method stub
		return null;
	}

	
	public boolean doesExist(String inPath) throws RepositoryException 
	{
		try
		{
			 getConnection().getObjectMetadata(	 new GetObjectMetadataRequest(getBucket(), inPath));
			 return true;
		}
		catch(AmazonServiceException ex )
		{
			if( ex.getStatusCode()  == 404)
			{
				return false;
			}
			throw ex;
		}
	}

	
	public void put(ContentItem inContent) throws RepositoryException {
		File file = new File(inContent.getAbsolutePath());
		String path = inContent.getPath();
		getConnection().putObject(new PutObjectRequest(getBucket(), inContent.getPath(), file));
		
	}

	@Override
	public void copy(ContentItem inSource, ContentItem inDestination) throws RepositoryException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void move(ContentItem inSource, ContentItem inDestination) throws RepositoryException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void move(ContentItem inSource, Repository inSourceRepository, ContentItem inDestination) throws RepositoryException {
		// TODO Auto-generated method stub
		
	}

	
	public void remove(ContentItem inPath) throws RepositoryException {
	getConnection().deleteObject(getBucket(), inPath.getPath());
		
	}

	@Override
	public List getVersions(String inPath) throws RepositoryException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ContentItem getLastVersion(String inPath) throws RepositoryException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List getChildrenNames(String inParent) throws RepositoryException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteOldVersions(String inPath) throws RepositoryException {
		// TODO Auto-generated method stub
		
	}

	
	class S3ContentItem extends InputStreamItem
	{
		protected Boolean existed;

		public InputStream getInputStream() throws RepositoryException
		{
			 S3Object object = getConnection().getObject(new GetObjectRequest(getBucket(), getPath()));
			 return object.getObjectContent();
		}

		public boolean exists()
		{
			 S3Object object = getConnection().getObject(new GetObjectRequest(getBucket(), getPath()));
			 
			 return (object == null); //no idea if this is correct.
		}

		public boolean isFolder()
		{
			if (getAbsolutePath().endsWith("/"))
			{
				return true;
			}
			return false;
		}

	}


	public URL getPresignedURL(String inString, Date expiration) {
		return getConnection().generatePresignedUrl(getBucket(), inString, expiration);
	}
}




/*
 * Important: Be sure to fill in your AWS access credentials in the
 *            AwsCredentials.properties file before you try to run this
 *            sample.
 * http://aws.amazon.com/security-credentials
 */
//AmazonS3 s3 = new AmazonS3Client(new PropertiesCredentials(
//        S3Sample.class.getResourceAsStream("AwsCredentials.properties")));
//
//String bucketName = "my-first-s3-bucket-" + UUID.randomUUID();
//String key = "MyObjectKey";
//
//System.out.println("===========================================");
//System.out.println("Getting Started with Amazon S3");
//System.out.println("===========================================\n");
//
//try {
//    /*
//     * Create a new S3 bucket - Amazon S3 bucket names are globally unique,
//     * so once a bucket name has been taken by any user, you can't create
//     * another bucket with that same name.
//     *
//     * You can optionally specify a location for your bucket if you want to
//     * keep your data closer to your applications or users.
//     */
//    System.out.println("Creating bucket " + bucketName + "\n");
//    s3.createBucket(bucketName);
//
//    /*
//     * List the buckets in your account
//     */
//    System.out.println("Listing buckets");
//    for (Bucket bucket : s3.listBuckets()) {
//        System.out.println(" - " + bucket.getName());
//    }
//    System.out.println();
//
//    /*
//     * Upload an object to your bucket - You can easily upload a file to
//     * S3, or upload directly an InputStream if you know the length of
//     * the data in the stream. You can also specify your own metadata
//     * when uploading to S3, which allows you set a variety of options
//     * like content-type and content-encoding, plus additional metadata
//     * specific to your applications.
//     */
//    System.out.println("Uploading a new object to S3 from a file\n");
//    s3.putObject(new PutObjectRequest(bucketName, key, createSampleFile()));
//
//    /*
//     * Download an object - When you download an object, you get all of
//     * the object's metadata and a stream from which to read the contents.
//     * It's important to read the contents of the stream as quickly as
//     * possibly since the data is streamed directly from Amazon S3 and your
//     * network connection will remain open until you read all the data or
//     * close the input stream.
//     *
//     * GetObjectRequest also supports several other options, including
//     * conditional downloading of objects based on modification times,
//     * ETags, and selectively downloading a range of an object.
//     */
//    System.out.println("Downloading an object");
//    S3Object object = s3.getObject(new GetObjectRequest(bucketName, key));
//    System.out.println("Content-Type: "  + object.getObjectMetadata().getContentType());
//    displayTextInputStream(object.getObjectContent());
//
//    /*
//     * List objects in your bucket by prefix - There are many options for
//     * listing the objects in your bucket.  Keep in mind that buckets with
//     * many objects might truncate their results when listing their objects,
//     * so be sure to check if the returned object listing is truncated, and
//     * use the AmazonS3.listNextBatchOfObjects(...) operation to retrieve
//     * additional results.
//     */
//    System.out.println("Listing objects");
//    ObjectListing objectListing = s3.listObjects(new ListObjectsRequest()
//            .withBucketName(bucketName)
//            .withPrefix("My"));
//    for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
//        System.out.println(" - " + objectSummary.getKey() + "  " +
//                           "(size = " + objectSummary.getSize() + ")");
//    }
//    System.out.println();
//
//    /*
//     * Delete an object - Unless versioning has been turned on for your bucket,
//     * there is no way to undelete an object, so use caution when deleting objects.
//     */
//    System.out.println("Deleting an object\n");
//    s3.deleteObject(bucketName, key);
//
//    /*
//     * Delete a bucket - A bucket must be completely empty before it can be
//     * deleted, so remember to delete any objects from your buckets before
//     * you try to delete them.
//     */
//    System.out.println("Deleting bucket " + bucketName + "\n");
//    s3.deleteBucket(bucketName);
//} catch (AmazonServiceException ase) {
//    System.out.println("Caught an AmazonServiceException, which means your request made it "
//            + "to Amazon S3, but was rejected with an error response for some reason.");
//    System.out.println("Error Message:    " + ase.getMessage());
//    System.out.println("HTTP Status Code: " + ase.getStatusCode());
//    System.out.println("AWS Error Code:   " + ase.getErrorCode());
//    System.out.println("Error Type:       " + ase.getErrorType());
//    System.out.println("Request ID:       " + ase.getRequestId());
//} catch (AmazonClientException ace) {
//    System.out.println("Caught an AmazonClientException, which means the client encountered "
//            + "a serious internal problem while trying to communicate with S3, "
//            + "such as not being able to access the network.");
//    System.out.println("Error Message: " + ace.getMessage());
//}
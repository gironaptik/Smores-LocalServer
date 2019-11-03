package src;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamException;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.concurrent.TimeoutException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.InvalidParameterException;
import com.amazonaws.services.rekognition.model.BoundingBox;
import com.amazonaws.services.rekognition.model.CompareFacesMatch;
import com.amazonaws.services.rekognition.model.CompareFacesRequest;
import com.amazonaws.services.rekognition.model.CompareFacesResult;
import com.amazonaws.services.rekognition.model.ComparedFace;
import com.amazonaws.services.rekognition.model.FaceMatch;
import com.amazonaws.services.rekognition.model.SearchFacesByImageRequest;
import com.amazonaws.services.rekognition.model.SearchFacesByImageResult;
import com.fasterxml.jackson.databind.ObjectMapper;

//import com.amazonaws.services.rekognition.model.S3Object;

import javax.imageio.ImageIO;
import java.util.List;

public class Application {

	public static void main(String[] args) throws IOException {

		String collectionName = "smores-clients";
		String bucketName = "smores-purchases";
		String folderName = new java.sql.Date(System.currentTimeMillis()).toString();
		String fileNameInS3 = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
		String fileNameInLocalPC = folderName + fileNameInS3 + ".png";

		// Rekognition Section
		Float similarityThreshold = 90F;
		// String sourceImage = "source.jpg";
		// String targetImage = "target.jpg";
		String bucketCustomersName = "smores-users"; // "mybucket1"+ System.currentTimeMillis();
		String targetCustomerName = "giron.png";

		Webcam webcam = Webcam.getDefault();
		webcam.open();
		ImageIO.write(webcam.getImage(), "PNG", new File(folderName + fileNameInS3 + ".png"));
		webcam.close();

		try {
			AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();

			PutObjectRequest requestS3 = new PutObjectRequest(bucketName, folderName + fileNameInS3 + ".png",
					new File(fileNameInLocalPC));
			s3Client.putObject(requestS3);

			// SECTION 3: Get file from S3 bucket
			//
			S3Object fullObject;
			fullObject = s3Client.getObject(new GetObjectRequest(bucketName, folderName + fileNameInS3 + ".png"));

			// Print file content line by line
			InputStream is = fullObject.getObjectContent();
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));

			AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.defaultClient();

			ObjectMapper objectMapper = new ObjectMapper();

			Image sourceImage = new Image().withS3Object(new com.amazonaws.services.rekognition.model.S3Object()
					.withBucket(bucketName).withName(folderName + fileNameInS3 + ".png"));

			// Search collection for faces similar to the largest face in the image.
			SearchFacesByImageRequest searchFacesByImageRequest = new SearchFacesByImageRequest()
					.withCollectionId(collectionName).withImage(sourceImage).withFaceMatchThreshold(90F)
					.withMaxFaces(1);

			SearchFacesByImageResult searchFacesByImageResult = rekognitionClient
					.searchFacesByImage(searchFacesByImageRequest);

			System.out.println("Faces matching largest face in image from" + folderName + fileNameInS3 + ".png");

			List<FaceMatch> faceImageMatches = searchFacesByImageResult.getFaceMatches();
			System.out.println("facesss:  " + searchFacesByImageResult.getFaceMatches().size());
			for (FaceMatch face : faceImageMatches) {
				System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(face));
				System.out.println();
			}
//		     CompareFacesRequest request = new CompareFacesRequest()
//		               .withSourceImage(new Image()
//		            		   .withS3Object(new com.amazonaws.services.rekognition.model.S3Object()
//		            				   .withName(folderName + fileNameInS3 +".png")
//		            				   .withBucket(bucketName)))
//		               .withTargetImage(new Image()
//		            		   .withS3Object(new com.amazonaws.services.rekognition.model.S3Object()   
//		            		   .withName(targetCustomerName)
//		            		   .withBucket(bucketCustomersName)))
//		               .withSimilarityThreshold(similarityThreshold);
//		       
//		       // Call operation
//		       CompareFacesResult compareFacesResult=rekognitionClient.compareFaces(request);
//		       
//		       // Display results
//		       List <CompareFacesMatch> faceDetails = compareFacesResult.getFaceMatches();
//		       for (CompareFacesMatch match: faceDetails){
//		         ComparedFace face= match.getFace();
//		         BoundingBox position = face.getBoundingBox();
//		         System.out.println("Face at " + position.getLeft().toString()
//		               + " " + position.getTop()
//		               + " matches with " + match.getSimilarity().toString()
//		               + "% confidence.");
//
//		       }
//		       List<ComparedFace> uncompared = compareFacesResult.getUnmatchedFaces();
//
//		       System.out.println("There was " + uncompared.size()
//		            + " face(s) that did not match");
		} catch (InvalidParameterException | AmazonS3Exception e) {

			e.printStackTrace();
		}
	}

}
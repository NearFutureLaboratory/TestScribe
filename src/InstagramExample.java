import java.util.Scanner;

import org.scribe.builder.*;
import org.scribe.builder.api.*;
import org.scribe.model.*;
import org.scribe.oauth.*;

import com.nearfuturelaboratory.humans.serviceapi.InstagramApi;
import com.nearfuturelaboratory.humans.service.*;

import java.io.*;

public class InstagramExample
{
	private static final Token EMPTY_TOKEN = null;

	private static final String PROTECTED_RESOURCE_URL = "https://api.instagram.com/v1/users/self/follows";

	public static void main(String[] args) throws Exception
	{
		// If you choose to use a callback, "oauth_verifier" will be the return value by Twitter (request param)
		String apiKey = "d317569002c942d4afc13ba4fdb3d6b8";
		String apiSecret = "e62f50ae7c2845b4a406dd39f2518b5e";
		OAuthService service = new ServiceBuilder()
		.provider(InstagramApi.class)
		.apiKey(apiKey)
		.apiSecret(apiSecret)
		.callback("http://nearfuturelaboratory.com/scrumpy-instagram")
		.scope("basic,likes,relationships")
		.build();

		Scanner in = new Scanner(System.in);

		System.out.println("=== Instagram's OAuth Workflow ===");
		System.out.println();

		Token accessToken = deserializeToken();
		if(accessToken == null) {
			// Obtain the Authorization URL
			System.out.println("Fetching the Authorization URL...");
			String authorizationUrl = service.getAuthorizationUrl(EMPTY_TOKEN);
			System.out.println("Got the Authorization URL!");
			System.out.println(authorizationUrl);

			//ClientWithResponseHandler.handleURL(authorizationUrl);

			System.out.println("Now go and authorize Scribe here:");
			System.out.println("And paste the verifier here");
			System.out.print(">>");
			Verifier verifier = new Verifier(in.nextLine());
			System.out.println();

			// Trade the Request Token and Verfier for the Access Token
			System.out.println("Trading the Request Token for an Access Token...");
			accessToken = service.getAccessToken(EMPTY_TOKEN, verifier);
			System.out.println("Got the Access Token!");
			System.out.println("(if your curious it looks like this: " + accessToken + " )");
			System.out.println();

			serializeToken(accessToken);

		}
		
		InstagramService instagram = new InstagramService(accessToken);
		
		//instagram.getFollows();
		//instagram.getStatusForUser("294198486");
		//instagram.getFollows("self");
		//instagram.getStatusForUser("self");
		instagram.getFollows("16463713");
		instagram.getUserBasicForUserID("16463713", true);

		instagram.getStatusForUser("16463713");
		/*
		// Now let's go and ask for a protected resource!
		System.out.println("Now we're going to access a protected resource...");
		OAuthRequest request = new OAuthRequest(Verb.GET, PROTECTED_RESOURCE_URL);
		//request.addBodyParameter("status", "this is sparta! *");
		service.signRequest(accessToken, request);
		Response response = request.send();
		System.out.println("Got it! Lets see what we found...");
		System.out.println();
		System.out.println(response.getBody());

		System.out.println();
		System.out.println("Thats it man! Go and build something awesome with Scribe! :)");
		*/
		in.close();
	}


	static void serializeToken(Token aToken) {
		try{
			//use buffering
			OutputStream file = new FileOutputStream( "instagram-token-for-darthjulian.ser" );
			OutputStream buffer = new BufferedOutputStream( file );
			ObjectOutput output = new ObjectOutputStream( buffer );
			try{
				output.writeObject(aToken);
			}
			finally{
				output.close();
			}
		}  
		catch(IOException ex){
			//fLogger.log(Level.SEVERE, "Cannot perform output.", ex);
			ex.printStackTrace();
		}

	}

	static Token deserializeToken() {
		Token result = null;
		try{
			//use buffering
			InputStream file = new FileInputStream( "instagram-token-for-darthjulian.ser" );
			InputStream buffer = new BufferedInputStream( file );
			ObjectInput input = new ObjectInputStream ( buffer );
			try{
				//deserialize the List
				result = (Token)input.readObject();
				//display its data
				System.out.println("Deserialized Token is: "+result);
			}
			finally{
				input.close();
			}
		}
		catch(ClassNotFoundException ex){
			//fLogger.log(Level.SEVERE, "Cannot perform input. Class not found.", ex);
			ex.printStackTrace();
		}
		catch(IOException ex){
			//		      fLogger.log(Level.SEVERE, "Cannot perform input.", ex);
			ex.printStackTrace();
		}
		return result;
	}

}
package com.nearfuturelaboratory.humans.service;

import org.scribe.builder.*;
import org.scribe.builder.api.*;
import org.scribe.model.*;
import org.scribe.oauth.*;
import org.json.simple.*;

import java.util.*;
import java.util.regex.Pattern;
import java.io.*;

import com.nearfuturelaboratory.humans.serviceapi.InstagramApi;


public class InstagramService {

	private static final String FOLLOWS_URL = "https://api.instagram.com/v1/users/%s/follows";
	private static final String STATUS_URL = "https://api.instagram.com/v1/users/%s/media/recent";
	private static final String USER_URL = "https://api.instagram.com/v1/users/%s";
	private static final String STATUS_DB_PATH = "instagram/users/%s-%s/status/";
	private static final String USERS_DB_PATH = "instagram/users/%s-%s/";
	private OAuthService service;
	private String apiKey = "d317569002c942d4afc13ba4fdb3d6b8";
	private String apiSecret = "e62f50ae7c2845b4a406dd39f2518b5e";


	private Token accessToken;
	protected JSONObject user;


	public InstagramService(Token aAccessToken) {
		accessToken = aAccessToken;
		service = new ServiceBuilder()
		.provider(InstagramApi.class)
		.apiKey(apiKey)
		.apiSecret(apiSecret)
		.callback("http://nearfuturelaboratory.com/scrumpy-instagram")
		.scope("basic,likes")
		.build();
		//System.out.println(System.getProperty("user.dir"));

		String userURL = String.format(USER_URL, "self");
		OAuthRequest request = new OAuthRequest(Verb.GET, userURL);
		service.signRequest(accessToken, request);
		Response response = request.send();
		String s = response.getBody();
		Object obj = JSONValue.parse(s);
		user = (JSONObject) ((JSONObject)obj).get("data");

		File f = new File(String.format(USERS_DB_PATH, (String)user.get("id"), (String)user.get("username")));
		String p =  (String)user.get("id")+"-"+(String)user.get("username")+".json";
		writeJSONToFile(user, f, p);

		System.out.println(user.toJSONString());


	}

	public void writeJSONToFile(JSONArray arrayToWrite, File aDir, String aName)
	{
		System.out.println("writeJSONToFile "+aDir+" "+aDir.exists());
		System.out.println("and "+aDir.getAbsolutePath()+" "+aDir.getPath());
		createDirectoryHierarchyFromRunRoot(aDir);
		/*		try {
			if(!aDir.exists()) {
				String[] subDirs = aDir.getAbsolutePath().split(Pattern.quote(File.separator));
				List<String> dirs = Arrays.asList(subDirs);
				InstagramService.mkDirs(new File("."), dirs, dirs.size());
			}
		 */			
		try {
			File aFile = new File(aDir, aName);
			System.out.println("writeJSONToFile with "+aDir+" "+aName);
			System.out.println("trying to write JSON data to "+aFile);
			System.out.println("does the directory exist?"+aDir.exists());
			System.out.println("does the file exist?"+aFile.exists());
			FileWriter file = new FileWriter(aFile);
			file.write(arrayToWrite.toJSONString());
			file.flush();
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}


	}


	protected void createDirectoryHierarchyFromRunRoot(File aDir)
	{
		try {
			if(!aDir.exists()) {
				String[] subDirs = aDir.getPath().split(Pattern.quote(File.separator));
				List<String> dirs = Arrays.asList(subDirs);
				InstagramService.mkDirs(new File("."), dirs, dirs.size());
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}


	public void writeJSONToFile(JSONObject objToWrite, File aDir, String aName)
	{
		System.out.println("writeJSONToFile "+aDir+" "+aDir.exists());
		System.out.println("and "+aDir.getPath()+" "+aDir.getAbsolutePath());
		createDirectoryHierarchyFromRunRoot(aDir);

		/*		try {
			if(!aDir.exists()) {
				String[] subDirs = aDir.getPath().split(Pattern.quote(File.separator));
				List<String> dirs = Arrays.asList(subDirs);
				InstagramService.mkDirs(new File("."), dirs, dirs.size());
			}
		 */			
		try {
			File aFile = new File(aDir, aName);
			System.out.println("wrote user to "+aFile);
			FileWriter file = new FileWriter(aFile);
			file.write(objToWrite.toJSONString());
			file.flush();
			file.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public JSONObject getUserBasicForUserID(String aUserID, boolean save)
	{
		JSONObject result = getUserBasicForUserID(aUserID);
		if(save) {
			String username = (String)result.get("username");
			String path = String.format(USERS_DB_PATH, aUserID, username);
			writeJSONToFile(result, new File(path), result.get("id")+".json");
		}
		return result;
	}

	public JSONObject getUserBasicForUserID(String aUserID)
	{
		JSONObject aUser;
		String userURL = String.format(USER_URL, aUserID);
		OAuthRequest request = new OAuthRequest(Verb.GET, userURL);
		service.signRequest(accessToken, request);
		Response response = request.send();
		String s = response.getBody();
		Object obj = JSONValue.parse(s);
		aUser = (JSONObject) ((JSONObject)obj).get("data");
		return aUser;
	}

	public void getStatusForUser(String aUserID) {
		if(aUserID == null || aUserID.equalsIgnoreCase("self")) {
			aUserID = (String)user.get("id");
		}
		String statusURL = String.format(STATUS_URL, aUserID);
		OAuthRequest request = new OAuthRequest(Verb.GET, statusURL);
		service.signRequest(accessToken, request);
		Response response = request.send();
		String s = response.getBody();
		Object obj = JSONValue.parse(s);
		@SuppressWarnings("unused")
		JSONObject aUser = (JSONObject)obj;

		String thisUsername;
		String path;
		if(aUserID.equalsIgnoreCase((String)user.get("username"))) {
			thisUsername = (String)user.get("username");
			path = String.format(STATUS_DB_PATH, aUserID, thisUsername);
		} else {
			JSONObject u = getUserBasicForUserID(aUserID);
			thisUsername = (String)u.get("username");
			path = String.format(STATUS_DB_PATH, aUserID, thisUsername);
		}
		writeJSONToFile(aUser, new File(path), aUserID+".json");

	}

	public void getFollows() {
		getFollows("self");
	}

	@SuppressWarnings("unchecked")
	public void getFollows(String aUserID) {
		if(aUserID == null) {
			aUserID = "self";
		}

		JSONObject aUser;

		if(aUserID.equalsIgnoreCase("self")) {
			aUser = user;	
		} else {
			aUser = getUserBasicForUserID(aUserID);
		}

		String followsURL = String.format(FOLLOWS_URL, aUserID);
		OAuthRequest request = new OAuthRequest(Verb.GET, followsURL);
		service.signRequest(accessToken, request);
		Response response = request.send();
		String s = response.getBody();
		//System.out.println(s);
		Object obj=JSONValue.parse(s);
		JSONObject map = (JSONObject)obj;


		/*		Iterator iterator = map.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry mapEntry = (Map.Entry) iterator.next();
			System.out.println("The key is: " + mapEntry.getKey()
					+ ",value is :" + mapEntry.getValue());
		}
		 */
		JSONObject pagination = (JSONObject)map.get("pagination");
		System.out.println(pagination);
		JSONArray allFollows = new JSONArray();


		do {
			//System.out.println(map);
			JSONArray data = (JSONArray)map.get("data");
			allFollows.addAll(data);
			System.out.println("Adding "+data.size());
			System.out.println("All Follows now "+allFollows.size());
			/*			for(int i=0; i<data.size(); i++) {
				System.out.println(data.get(i));
			}
			 */			
			String next_url = (String)pagination.get("next_url");
			//System.out.println(next_url);
			if(next_url != null) {
				request = new OAuthRequest(Verb.GET, (String)next_url);
				response = request.send();
				s = response.getBody();
				map = (JSONObject)JSONValue.parse(s);
				pagination = (JSONObject)map.get("pagination");
				System.out.println("Next URL: "+next_url);
				System.out.println("Pagination: "+pagination);
			} else {
				System.out.println("Response "+map);
				System.out.println("Pagination is "+pagination);

				break;
			}
		} while(pagination != null);


		JSONObject meta = (JSONObject)map.get("meta");
		//System.out.println(map.get("pagination"));
		//System.out.println(allFollows);
		String p = String.format(USERS_DB_PATH, aUser.get("id"), aUser.get("username"));
		writeJSONToFile(allFollows, new File(p), aUser.get("id")+"-follows.json");




	}

	public static void mkDirs(File root, List<String> dirs, int depth) {
		if (depth == 0) return;
		for (String s : dirs) {
			File subdir = new File(root, s);
			if(!subdir.exists()) {
				System.out.println("Subdir "+subdir);
				subdir.mkdir();
			}
			root = subdir;
			//		    mkDirs(subdir, dirs, depth - 1);
		}
	}

}

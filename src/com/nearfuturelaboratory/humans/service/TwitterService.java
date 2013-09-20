package com.nearfuturelaboratory.humans.service;

import org.scribe.builder.*;
import org.scribe.builder.api.*;
import org.scribe.model.*;
import org.scribe.oauth.*;
import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.*;
import java.util.regex.Pattern;
import java.io.*;
import java.nio.file.*;

import com.nearfuturelaboratory.util.*;
import com.nearfuturelaboratory.util.file.*;
import com.google.common.collect.*;
import com.nearfuturelaboratory.util.file.Find.Finder;

import org.apache.commons.io.filefilter.*;

public class TwitterService {

	private String apiKey = "09ARKva0K7HMz1DW1GUg";
	private String apiSecret = "rwy7rZ2Uu3lkliYMfOaJD4UeUHFebDqXXrBgjnT8Rw";
	private Token accessToken;
	protected JSONObject user;
	private OAuthService service;
	private static final String FRIENDS_LIST_URL = "https://api.twitter.com/1.1/friends/list.json?user_id=%s&cursor=%s";
	private static final String FRIENDS_IDS_URL = "https://api.twitter.com/1.1/friends/ids.json?user_id=%s&cursor=%s&count=5000";
	private static final String VERIFY_URL = "https://api.twitter.com/1.1/account/verify_credentials.json";
	private static final String SHOW_USER_BY_ID_URL = "https://api.twitter.com/1.1/users/show.json?user_id=%s&include_entities=true";
	private static final String USER_LOOKUP_URL = "https://api.twitter.com/1.1/users/lookup.json";

	private static final String STATUS_DB_PATH = "twitter/users/%s-%s/status/";
	private static final String USERS_DB_PATH_ROOT = "twitter/users/";
	private static final String USERS_DB_PATH = "twitter/users/%s-%s/";

	public TwitterService(Token aAccessToken) {
		accessToken = aAccessToken;
		service = new ServiceBuilder()
		.provider(TwitterApi.class)
		.apiKey("09ARKva0K7HMz1DW1GUg")
		.apiSecret("rwy7rZ2Uu3lkliYMfOaJD4UeUHFebDqXXrBgjnT8Rw")
		.build();

		//String userURL = String.format(USER_URL, "self");
		OAuthRequest request = new OAuthRequest(Verb.GET, VERIFY_URL);
		service.signRequest(accessToken, request);
		Response response = request.send();
		String s = response.getBody();
		JSONObject obj = (JSONObject)JSONValue.parse(s);
		//user = (JSONObject)obj;
		//Long id = (Long) obj.get("id");
		
		user = (JSONObject)getUserBasicForUserID((String)obj.get("id_str"), false);
	}
	
	public JSONObject getUserBasicForUserID(String aUserID, boolean save)
	{
		JSONObject result = getUserBasicForUserID(aUserID);
		if(save) {
			String username = (String)result.get("screen_name");
			String path = String.format(USERS_DB_PATH, aUserID, username);
			writeJSONToFile(result, new File(path), result.get("id")+"-"+username+".json");
		}
		return result;
	}

	
	public long getUserBasicLastModifiedTime(String aUserID)
	{
		// see if it exists
		long result = 0l;
		File dir = new File("./"+USERS_DB_PATH_ROOT);
		FileFilter fileFilter = new WildcardFileFilter(aUserID+"-*");
		File[] files = dir.listFiles(fileFilter);
		if(files.length > 0 && files.length == 1) {
			result = files[0].lastModified();
		}
		long now = new Date().getTime();
		return result;
	}
	
	public JSONObject getUserBasicForUserID(String aUserID)
	{
		JSONObject aUser;
		String userURL = String.format(SHOW_USER_BY_ID_URL, aUserID);
		OAuthRequest request = new OAuthRequest(Verb.GET, userURL);
		service.signRequest(accessToken, request);
		Response response = request.send();
		String s = response.getBody();
		Map<String, String> h = response.getHeaders();
		System.out.println(h);
		Object obj = JSONValue.parse(s);
		aUser = (JSONObject) ((JSONObject)obj);
		return aUser;
	}


	public void getFollows() {
		getFollows((String)user.get("id_str"));
	}

	
	public void unpackFollowsFor(String aUserID)
	{
		JSONArray follows = getFollowsLocal(aUserID);
		Iterator iter = follows.iterator();
		while(iter.hasNext()) {
			JSONObject obj = (JSONObject) iter.next();
			String path = String.format(USERS_DB_PATH, obj.get("id"), obj.get("screen_name"));
			writeJSONToFile(obj, new File(path), obj.get("id")+"-"+obj.get("screen_name")+".json");
		}
		
	}
	
	public JSONArray getFollowsLocal(String aUserID)
	{
		JSONArray jsonArray = new JSONArray();
		Pair<List<Path>, Boolean> local = isFollowsLocal(aUserID);
		// consume the stuff in the List of <Path>
		List<Path> paths = local.getFirst();
		if(paths != null && paths.size() > 0 && (Boolean)local.getSecond().booleanValue() == true) {
			JSONParser parser = new JSONParser();

			Path path = (Path)paths.get(0);
			Object obj = null;
			try {
				obj = parser.parse(new FileReader(path.toFile()));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			jsonArray = (JSONArray) obj;
		}
		return jsonArray;
	}
	
	
	/**
	 * Check to see if a userid has its follows data locally.
	 * If it does, return the file path
	 * 
	 * @param aUserID
	 * @return
	 */
	public Pair<List<Path>, Boolean> isFollowsLocal(String aUserID) {
		//boolean result = false;
		Path startingDir = Paths.get(USERS_DB_PATH_ROOT);
        String pattern = aUserID+"-hydratedfollows.json";
        Pair<List<Path>, Boolean> result = new Pair<List<Path>, Boolean>(null, new Boolean(false));
        Finder finder = new Finder(pattern);
        try {
			Files.walkFileTree(startingDir, finder);
			System.out.println(finder.results);
			if(finder.results != null & finder.results.size() > 0) {
				result.setFirst(finder.results);
				result.setSecond(new Boolean(true));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	
	@SuppressWarnings("unchecked")
	public void getFollows(String aUserID) {
		String id_str = (String)user.get("id_str");
		if(aUserID == null) {
			aUserID = id_str;
		}

		JSONObject aUser;

		if(aUserID.equalsIgnoreCase(id_str)) {
			aUser = user;	
		} else {
			aUser = getUserBasicForUserID(aUserID);
		}

		String followsURL = String.format(FRIENDS_IDS_URL, aUserID, "-1");
		OAuthRequest request = new OAuthRequest(Verb.GET, followsURL);
		service.signRequest(accessToken, request);
		Response response = request.send();
		String s = response.getBody();

		Map<String, String> h = response.getHeaders();
		System.out.println(h);

		//System.out.println(s);
		Object obj=JSONValue.parse(s);
		JSONObject map = (JSONObject)obj;

		// error check
		System.out.println(map);
		if(map != null && map.get("errors") == null) {
			Long next_cursor = (Long)map.get("next_cursor");
			long next_cursor_l = next_cursor.longValue();
			System.out.println("next_cursor="+next_cursor);
			JSONArray allFollowsIDs = new JSONArray();
			JSONArray allFollowsHydrated = new JSONArray();

			do {
				//			JSONArray users = (JSONArray)map.get("users");
				JSONArray users = (JSONArray)map.get("ids");
				allFollowsIDs.addAll(users);
				System.out.println("Adding "+users.size());
				System.out.println("All Follows now "+allFollowsIDs.size());
				String next_url = String.format(FRIENDS_IDS_URL, user.get("screen_name"), next_cursor);   //(String)pagination.get("next_url");
				next_cursor_l = next_cursor.longValue();
				if(next_cursor != null && next_cursor_l != 0) {
					request = new OAuthRequest(Verb.GET, (String)next_url);
					service.signRequest(accessToken, request);
					response = request.send();
					s = response.getBody();
					map = (JSONObject)JSONValue.parse(s);
					next_cursor = (Long)map.get("next_cursor");
					System.out.println("Next URL: "+next_url);
					System.out.println("next_cursor: "+next_cursor);
				} else {
					System.out.println("Response "+map);
					System.out.println("next_cursor is "+next_cursor);

					break;
				}
			} while(next_cursor != null && next_cursor_l != 0);


			JSONObject meta = (JSONObject)map.get("meta");
			//System.out.println(map.get("pagination"));
			//System.out.println(allFollows);
			String p = String.format(USERS_DB_PATH, aUser.get("id"), aUser.get("screen_name"));
			writeJSONToFile(allFollowsIDs, new File(p), aUser.get("id")+"-follows.json");

			OAuthRequest postUserLookup = new OAuthRequest(Verb.POST, USER_LOOKUP_URL);
			StringBuffer buf;// = new StringBuffer();

			//Iterator iter = allFollows.iterator();

			List<String> chunks = com.google.common.collect.Lists.partition(allFollowsIDs, 100);

			Iterator iter = chunks.iterator();
			while(iter.hasNext()) {
				List<Long>chunk = (List<Long>)iter.next();
				buf = new StringBuffer();
				for(int i=0; (i<100 && i < chunk.size()); i++) {
					buf.append(chunk.get(i));
					buf.append(",");
				}
				buf.deleteCharAt(buf.length()-1);
				
				postUserLookup = new OAuthRequest(Verb.POST, USER_LOOKUP_URL);
				postUserLookup.setConnectionKeepAlive(false);
				
				postUserLookup.addBodyParameter("user_id", buf.toString());
				service.signRequest(accessToken, postUserLookup);
				response = postUserLookup.send();
				s= response.getBody();
				//System.out.println("s="+s);
				JSONArray usersArray = (JSONArray)JSONValue.parse(s);
				allFollowsHydrated.addAll(usersArray);
				
				System.out.println("response="+s);

			}
			p = String.format(USERS_DB_PATH, aUser.get("id"), aUser.get("screen_name"));
			writeJSONToFile(allFollowsHydrated, new File(p), aUser.get("id")+"-hydratedfollows.json");
			//System.out.println(allFollowsHydrated);

		} else {
			//throw new Exception("Do something about rate limit errors, etc."+map.toString());
			System.err.println("Do something about rate limit errors, etc."+map.toString());
		}


	}

	
	public List<JSONObject> getUsersFromFollows(JSONArray arrayOfUsers)
	{
		List<JSONObject> result = new ArrayList<JSONObject>();
		for(int i=0; i<arrayOfUsers.size(); i++) {
			result.add((JSONObject)arrayOfUsers.get(i));
		}
		
		return result;
	}
	
	public <E> List<E> doIt()
	{
		List<E> result = null;
		
		return result;
	}
	
	

	public void writeJSONToFile(JSONArray arrayToWrite, File aDir, String aName)
	{
		System.out.println("writeJSONToFile "+aDir+" "+aDir.exists());
		System.out.println("and "+aDir.getAbsolutePath()+" "+aDir.getPath());
		createDirectoryHierarchyFromRunRoot(aDir);
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
				TwitterService.mkDirs(new File("."), dirs, dirs.size());
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

package models;

import static models.Functions.*;

import org.codehaus.jackson.JsonNode;

import play.libs.F;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.F.Tuple;
import play.libs.OAuth;
import play.libs.OAuth.ConsumerKey;
import play.libs.OAuth.OAuthCalculator;
import play.libs.OAuth.RequestToken;
import play.libs.OAuth.ServiceInfo;
import play.libs.WS;
import play.libs.WS.Response;
import play.libs.WS.WSRequestHolder;

import com.ning.http.util.Base64;

public class Twitter {
	private static String consumerKey = "ZH15OspjNAfn5cyfLGm8KA";
	private static String consumerSecret = "IKgL5u3KORkRDh9Ay78iBhrl3N4JWbQXxazCJNc";
	
	private static ConsumerKey key = new ConsumerKey(consumerKey, consumerSecret);
	
	private static OAuth oauthHelper = new OAuth(new ServiceInfo(
		    "https://api.twitter.com/oauth/request_token",
		    "https://api.twitter.com/oauth/access_token",
		    "https://api.twitter.com/oauth/authorize", key), true);

	public static Tuple<String, RequestToken> retriveRequestToken(String callback) {
	  RequestToken tr = oauthHelper.retrieveRequestToken(callback);	
	  return new F.Tuple<String, RequestToken>(oauthHelper.redirectUrl(tr.token), tr);
	}
	
	public static Promise<JsonNode> userSettings(RequestToken token, String authVerifier) {
	   RequestToken rt = oauthHelper.retrieveAccessToken(token, authVerifier);	
	   WSRequestHolder req = WS.url("https://api.twitter.com/1.1/account/settings.json").sign(new OAuthCalculator(key, rt));
	   return req.get().map(responseToJson);
	}
	
	public static Promise<JsonNode> userProfile(final String screenName) {
		Promise<Response> response = authenticateApplication();
		return response.map(responseToJson).map(findTextElement("access_token")).flatMap(new Function<String, Promise<JsonNode>>() {
			public Promise<JsonNode> apply(String accessToken) {
				return userProfile(screenName, accessToken);
			}
		});
	}

	private static Promise<JsonNode> userProfile(final String screenName,
			String accessToken) {
		WSRequestHolder req = WS
				.url("https://api.twitter.com/1.1/users/show.json")
				.setQueryParameter("screen_name", screenName)
				.setHeader("Authorization", "Bearer " + accessToken);
		Promise<Response> promise = req.get();
		return promise.map(responseToJson);
	}

	private static Promise<Response> authenticateApplication() {
		WSRequestHolder req = WS
				.url("https://api.twitter.com/oauth2/token")
				.setHeader("Authorization", "Basic " + bearerToken())
				.setContentType(
						"application/x-www-form-urlencoded;charset=UTF-8");
		return req.post("grant_type=client_credentials");
	}

	private static String bearerToken() {
		return Base64.encode((consumerKey + ":" + consumerSecret)
				.getBytes());
	}
}

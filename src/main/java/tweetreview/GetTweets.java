package tweetreview;

import java.net.UnknownHostException;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import twitter4j.GeoLocation;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;

public class GetTweets {    
    public static void main(String[] args) throws TwitterException, InterruptedException, UnknownHostException {
        MongoClient mongoClient = new MongoClient();
        
        DB db = mongoClient.getDB("tweetreview");
        
        DBCollection collection = db.getCollection("tweet");
        
        String consumerKey = System.getProperty("tweetreview.consumerKey");
        String consumerSecret = System.getProperty("tweetreview.consumerSecret");
        String token = System.getProperty("tweetreview.token");
        String tokenSecret = System.getProperty("tweetreview.tokenSecret");
        
        Twitter twitter = TwitterFactory.getSingleton();
        twitter.setOAuthConsumer(consumerKey, consumerSecret);
        twitter.setOAuthAccessToken(new AccessToken(token, tokenSecret));
        
        Query query = new Query("xmen OR x-men");
        query.setLang("en");
        query.setGeoCode(new GeoLocation(37.7833, -122.4167), 10.0, Query.MILES);
        query.setCount(100);
        
        QueryResult result = twitter.search(query);
        if (result.getTweets().size() != query.getCount()) {
            throw new RuntimeException(String.format("only got %d/%d tweets", result.getTweets().size(), query.getCount()));
        }
        if (result.getRateLimitStatus().getRemaining() == 0) {
            throw new RuntimeException(String.format("rate limited (%d seconds)", result.getRateLimitStatus().getSecondsUntilReset()));
        }
        for (Status status : result.getTweets()) {
            BasicDBObject object = new BasicDBObject()
                .append("text", status.getText())
                .append("screen_name", status.getUser().getScreenName())
                .append("created_at", status.getCreatedAt());
            collection.insert(object);
        }
    }
}

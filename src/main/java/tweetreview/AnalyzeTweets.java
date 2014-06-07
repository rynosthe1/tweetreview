package tweetreview;

import java.net.UnknownHostException;
import java.util.Date;
import java.util.Properties;

import org.joda.time.DateTime;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;

public class AnalyzeTweets {
    public static void main(String[] args) throws UnknownHostException {
        MongoClient mongoClient = new MongoClient();
        
        DB db = mongoClient.getDB("tweetreview");
        
        DBCollection collection = db.getCollection("tweet");

        // See SentimentPipeline for examples of usage.
        // Things to try to improve accuracy:
        // - Tree input
        // - Tree annotations
        // - Change the sentiment model
        Properties properties = new Properties();
        properties.setProperty("annotators", "tokenize, ssplit, parse, sentiment");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(properties);

        DBCursor cursor = collection.find();
        cursor.limit(100);
        while (cursor.hasNext()) {
            DBObject object = cursor.next();
            String text = (String)object.get("text");
            String screenName = (String)object.get("screen_name");
            DateTime createdAt = new DateTime((Date)object.get("created_at"));
            boolean sentiment = analyzeTweet(pipeline, text);
            System.out.printf("%s: %s %s: %s\n", sentiment ? "good" : "bad", createdAt.toString("yyyy-MM-dd HH:mm:ss"), screenName, text);
        }
    }

    private static boolean analyzeTweet(StanfordCoreNLP pipeline, String text) {
        Annotation annotation = new Annotation(text);
        pipeline.annotate(annotation);

        int sentenceIndex = 0;
        for (int i = 0; i < annotation.get(CoreAnnotations.SentencesAnnotation.class).size(); i++) {
            if (annotation.get(CoreAnnotations.SentencesAnnotation.class).get(i).toString().length() > annotation.get(CoreAnnotations.SentencesAnnotation.class).get(sentenceIndex).toString().length()) {
                sentenceIndex = i;
            }
        }
        
        // How exactly is the class name computed from the text?
        // - Is the text parsed into parts of speech?
        // - How is the model (englishPCFG.ser.gz) compared to the sentence? Do different sentence fragments have different sentiment values?
        CoreMap longestSentence = annotation.get(CoreAnnotations.SentencesAnnotation.class).get(sentenceIndex);
        String className = longestSentence.get(SentimentCoreAnnotations.ClassName.class); // Class names are given by RNNOptions.
        return className.toLowerCase().contains("positive");
    }
}

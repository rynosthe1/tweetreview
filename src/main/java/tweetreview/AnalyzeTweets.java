package tweetreview;

import java.net.UnknownHostException;
import java.util.Date;
import java.util.Properties;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations.AnnotatedTree;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations.ClassName;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;

// TODO
// Do I need to make my own training set?
// I want to know WHY NLP is telling me a certain tweet is negative.
// Maybe if I can see how NLP broke down the tweet's structure, I can figure out where it is going wrong.
// I need a spelling corrector for misspelled tweets.
// I need emoticon emotional values in the training set.
public class AnalyzeTweets {
    private static final Logger logger = LoggerFactory.getLogger(AnalyzeTweets.class);
    
    public static void main(String[] args) throws UnknownHostException {
        MongoClient mongoClient = new MongoClient();
        
        DB db = mongoClient.getDB("tweetreview");
        
        DBCollection collection = db.getCollection("tweet");

        // See SentimentPipeline for examples of usage.
        Properties properties = new Properties();
        properties.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse, sentiment");
        Annotator annotator = new StanfordCoreNLP(properties);

        DBCursor cursor = collection.find();
        cursor.limit(10);
        while (cursor.hasNext()) {
            DBObject object = cursor.next();
            String text = (String)object.get("text");
            
            // This filters out too many tweets.
//            if (!OpinionDetector.isOpinion(annotator, text)) {
//                logger.info("tweet does not appear to be an opinion: {}", text);
//                continue;
//            }

            String screenName = (String)object.get("screen_name");
            DateTime createdAt = new DateTime((Date)object.get("created_at"));
            String className = analyzeTweet(annotator, text);
            if (className.equals("Neutral")) {
                continue;
            }
            System.out.printf("%s: %s %s: %s\n", className, createdAt.toString("yyyy-MM-dd HH:mm:ss"), screenName, text);
        }
    }

    private static String analyzeTweet(Annotator annotator, String text) {
        Annotation annotation = new Annotation(text);
        annotator.annotate(annotation);
        
        // Get the longest sentence.
        int sentenceIndex = 0;
        for (int i = 0; i < annotation.get(SentencesAnnotation.class).size(); i++) {
            if (annotation.get(SentencesAnnotation.class).get(i).toString().length() > annotation.get(SentencesAnnotation.class).get(sentenceIndex).toString().length()) {
                sentenceIndex = i;
            }
        }
        
        CoreMap sentence = annotation.get(SentencesAnnotation.class).get(sentenceIndex);        

        // AnnotatedTree has the full prediction/classification data.
        Tree tree = sentence.get(AnnotatedTree.class);
        printPredictionValues(tree);
        
        return sentence.get(ClassName.class); // Class names are given by RNNOptions.
    }

    private static void printPredictionValues(Tree tree) {
        // The prediction data (PredictedClass, Predictions, and NodeVector) for words
        // are label annotations on the parent of each leaf node.
        // From these results, I'm afraid that stanford nlp's sentiment analysis 
        // puts too much weight on individual words (best, amazing) and does not go far enough 
        // to analyze the tweet as a whole.
        if (tree.getChildrenAsList().size() == 1 && tree.getChild(0).isLeaf()) {
            Tree leaf = tree.getChild(0);
            logger.info(
                String.format(
                    "%8s %15s %d %4.2f",
                    tree.label().value(),
                    leaf.label().value(),
                    RNNCoreAnnotations.getPredictedClass(tree),
                    RNNCoreAnnotations.getPredictions(tree).elementMaxAbs()
                )
            );
        }
        
        for (Tree child : tree.getChildrenAsList()) {
            printPredictionValues(child);
        }
    }
}

package tweetreview;

import java.util.List;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;

public class OpinionDetector {
    // If the verb for xmen is be, then it is an opinion.
    public static boolean isOpinion(Annotator annotator, String text) {
        Annotation annotation = new Annotation(text); 
        annotator.annotate(annotation);
        // Each sentence is parsed into its own tree.
        // Find a simple declarative clause with a noun phrase containing xmen and a verb phrase containing a lemma of the word be.
        // TODO Sentiment supports RNNCoreAnnotations.getPredictions(x) instead of x.get(Prediction.class).
        // Determine whether there is something similar for these annotations.
        List<CoreMap> maps = annotation.get(SentencesAnnotation.class);
        for (CoreMap map : maps) {
            Tree tree = map.get(TreeAnnotation.class);
            if (isOpinionSentence(tree)) {
                return true;
            }
        }
        return false;
    }
    
    // This would be vastly simplified with a jquery-like api.
    // Even though it would be inefficient, I think I might use xpath to query the tree.
    private static boolean isOpinionSentence(Tree tree) {
        // Process a simple declarative phrase.
        String category = ((CoreLabel)tree.label()).category();
        if (category != null && category.equals("S")) {
            boolean nounPhraseContainsXmen = false;
            boolean verbPhraseContainsBe = false;

            for (Tree child : tree.getChildrenAsList()) {
                String childCategory = ((CoreLabel)child.label()).category();
                
                // Process a noun phrase.
                if (childCategory.equals("NP")) {
                    for (Tree leaf : child.getLeaves()) {
                        if (leaf.label().value().toLowerCase().contains("xmen")) {
                            nounPhraseContainsXmen = true;
                            break;
                        }
                    }
                }
                
                // Process a verb phrase.
                if (childCategory.equals("VP")) {
                    for (Tree leaf : child.getLeaves()) {
                        String lemma = ((CoreLabel)leaf.label()).lemma();
                        if (lemma != null && lemma.equals("be")) {
                            verbPhraseContainsBe = true;
                            break;
                        }
                    }
                }
            }
            return nounPhraseContainsXmen && verbPhraseContainsBe;
        }
        
        for (Tree child : tree.getChildrenAsList()) {
            if (isOpinionSentence(child)) {
                return true;
            }
        }
        return false;
    }
}

package edu.macalester.wpsemsim.sim;

import edu.macalester.wpsemsim.utils.KnownSim;
import edu.macalester.wpsemsim.sim.SimilarityMetric;

import java.util.List;

/**
 * TODO: create a general training tool if there are implementations
 * beyond EnsembleSimilarity.
 */
public interface SupervisedSimilarityMetric extends SimilarityMetric {
    // Train the similarity() function
    public void trainSimilarity(List<KnownSim> labeled);

    // Train the mostSimilar() function
    public void trainMostSimilar(List<KnownSim> labeled, int numResults);
}
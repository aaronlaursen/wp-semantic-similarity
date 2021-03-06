package edu.macalester.wpsemsim.sim.ensemble;

import edu.macalester.wpsemsim.normalize.BaseNormalizer;
import edu.macalester.wpsemsim.sim.SimilarityMetric;

import java.util.*;

/**
 * Generates numeric features for a component similarity example.
 *
 * Since it is targeted towards SimilarityMetric.similarity(), it presumes
 * that we are given a pair of words or articles.
 */
public class SimilarityFeatureGenerator extends FeatureGenerator {

    @Override
    public LinkedHashMap<Integer, Double> generate(Example ex) {
        if (components == null) throw new NullPointerException("components not set");
        if (!ex.hasReverse()) {
            throw new UnsupportedOperationException();  // TODO: fixme
        }
        if (ex.sims.size() != components.size()) {
            throw new IllegalStateException();
        }

        LinkedHashMap<Integer, Double> features = new LinkedHashMap<Integer, Double>();
        int fi = 0; // feature index

        for (int i = 0; i < ex.sims.size(); i++) {
            ComponentSim cs1 = ex.sims.get(i);
            ComponentSim cs2 = ex.reverseSims.get(i);
            assert(cs1.component == cs2.component);
//            if (cs1.hasValue() || cs2.hasValue()) {
                // range normalizer
                BaseNormalizer rn = rangeNormalizers.get(cs1.component);
                double r1 = cs1.hasValue() ? rn.normalize(cs1.sim) : rn.getMin();
                double r2 = cs2.hasValue() ? rn.normalize(cs2.sim) : rn.getMin();
                features.put(fi++, 0.5 * r1 + 0.5 * r2);

                // percent normalizer
                BaseNormalizer pn = percentNormalizers.get(cs1.component);
                double p1 = cs1.hasValue() ? pn.normalize(cs1.sim) : pn.getMin();
                double p2 = cs2.hasValue() ? pn.normalize(cs2.sim) : pn.getMin();
                features.put(fi++, percentileToScore(0.5 * p1 + 0.5 * p2));

                // log rank (mean and min)
                int rank1 = cs1.hasValue() ? cs1.rank : numResults * 2;
                int rank2 = cs2.hasValue() ? cs2.rank : numResults * 2;
                features.put(fi++, rankToScore(0.5 * rank1 + 0.5 * rank2, numResults * 2));
                features.put(fi++, rankToScore(Math.min(rank1, rank2), numResults * 2));
//            } else {
//                fi += 4;
//            }
        }
        assert(fi == components.size() * 4);
        return features;
    }

    @Override
    public List<String> getFeatureNames() {
        List<String> names = new ArrayList<String>();
        for (SimilarityMetric m : components) {
            String metricName = m.getName().toLowerCase().replaceAll("[^a-zA-Z]+", "");
            names.add(metricName + "-range");
            names.add(metricName + "-percent");
            names.add(metricName + "-rankmean");
            names.add(metricName + "-rankmin");
        }
        return names;
    }

}

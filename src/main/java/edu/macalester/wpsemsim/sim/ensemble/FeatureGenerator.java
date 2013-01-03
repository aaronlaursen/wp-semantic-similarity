package edu.macalester.wpsemsim.sim.ensemble;

import edu.macalester.wpsemsim.normalize.BaseNormalizer;
import edu.macalester.wpsemsim.normalize.PercentileNormalizer;
import edu.macalester.wpsemsim.normalize.RangeNormalizer;
import edu.macalester.wpsemsim.sim.SimilarityMetric;

import java.io.Serializable;
import java.util.*;

/**
 * Generates numeric features for a component similarity example.
 */
public class FeatureGenerator implements Serializable {
    // don't serialize the components they must be set manually.
    private transient List<SimilarityMetric> components = null;

    // list of normalizers for each component.
    // Each component's normalizers are a map of normalize name to normalizer.
    private List<BaseNormalizer> rangeNormalizers = new ArrayList<BaseNormalizer>();
    private List<BaseNormalizer> percentNormalizers = new ArrayList<BaseNormalizer>();
    private int numResults = 0;

    public void setComponents(List<SimilarityMetric> components) {
        this.components = components;   // the ordering for this better not change!
    }

    public void train(List<Example> examples) {
        if (components == null) throw new NullPointerException("components not set");
        for (int i = 0; i < components.size(); i++) {
            rangeNormalizers.add(new RangeNormalizer(-1, +1, false));
            percentNormalizers.add(new PercentileNormalizer());
        }
        for (Example ex : examples) {
            List<ComponentSim> allSims = new ArrayList<ComponentSim>(ex.sims);
            if (ex.hasReverse()) allSims.addAll(ex.reverseSims);
            for (ComponentSim s : allSims) {
                numResults = Math.max(numResults, s.length);
                if (s.scores != null) {
                    for (float x : s.scores) {
                        rangeNormalizers.get(s.component).observe(x);
                        percentNormalizers.get(s.component).observe(x);
                    }
                }
            }
        }

        for (BaseNormalizer n : rangeNormalizers) { n.observationsFinished(); }
        for (BaseNormalizer n : percentNormalizers) { n.observationsFinished(); }
    }

    public LinkedHashMap<Integer, Double> generate(Example ex) {
        if (components == null) throw new NullPointerException("components not set");
        if (!ex.hasReverse()) {
            throw new UnsupportedOperationException();  // TODO: fixme
        }

        LinkedHashMap<Integer, Double> features = new LinkedHashMap<Integer, Double>();
        int fi = 0; // feature index

        for (int i = 0; i < ex.sims.size(); i++) {
            ComponentSim cs1 = ex.sims.get(i);
            ComponentSim cs2 = ex.reverseSims.get(i);
            if (cs1.hasValue() || cs2.hasValue()) {
                // range normalizer
                BaseNormalizer rn = rangeNormalizers.get(i);
                double r1 = cs1.hasValue() ? rn.normalize(cs1.sim) : rn.getMin();
                double r2 = cs2.hasValue() ? rn.normalize(cs2.sim) : rn.getMin();
                features.put(fi++, 0.5 * r1 + 0.5 * r2);

                // percent normalizer
                BaseNormalizer pn = percentNormalizers.get(i);
                double p1 = cs1.hasValue() ? pn.normalize(cs1.sim) : pn.getMin();
                double p2 = cs2.hasValue() ? pn.normalize(cs2.sim) : pn.getMin();
                features.put(fi++, 0.5 * p1 + 0.5 * p2);

                // log rank (mean and min)
                double lr1 = Math.log(1 + (cs1.hasValue() ? cs1.rank : numResults * 2));
                double lr2 = Math.log(1 + (cs2.hasValue() ? cs2.rank : numResults * 2));
                features.put(fi++, 0.5 * lr1 + 0.5 * lr2);
                features.put(fi++, Math.min(lr1, lr2));
            } else {
                fi += 4;
            }
        }
        assert(fi == components.size() * 4);
        return features;
    }
}

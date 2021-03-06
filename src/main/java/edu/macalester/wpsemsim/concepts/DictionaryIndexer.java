package edu.macalester.wpsemsim.concepts;

import com.sleepycat.je.DatabaseException;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.math.Fraction;

import java.io.*;
import java.text.DecimalFormat;
import java.util.logging.Logger;

/**
 * Indexes files from http://www-nlp.stanford.edu/pubs/crosswikis-data.tar.bz2/
 * into a database for a dictionary mapper.
 */
public class DictionaryIndexer {
    private static final Logger LOG = Logger.getLogger(DictionaryIndexer.class.getName());

    private int minNumLinks = 5;
    private double minFractionLinks = 0.01;
    private DictionaryMapper db;

    public DictionaryIndexer(File path) throws IOException, DatabaseException {
        this.db = new DictionaryMapper(path, null, true);
    }

    public void index(BufferedReader in) throws IOException, DatabaseException {
        long numLines = 0;
        long numLinesRetained = 0;
        DictionaryMapper.Record record = null;
        while (true) {
            String line = in.readLine();
            if (line == null) {
                break;
            }
            if (++numLines % 100000 == 0) {
                double p = 100.0 * numLinesRetained / numLines;
                LOG.info("processing line: " + numLines +
                        ", retained " + numLinesRetained +
                        "(" + new DecimalFormat("#.#").format(p) + "%)");
            }
            DictionaryEntry entry = new DictionaryEntry(line);
            if (retain(entry)) {
                numLinesRetained++;
                if (record != null && record.shouldContainEntry(entry)) {
                    record.add(entry);
                } else {
                    if (record != null) {
                        db.put(record, true);
                    }
                    record = new DictionaryMapper.Record(entry);
                }
            }
        }
        db.put(record, true);
        db.close();
    }

    public boolean retain(DictionaryEntry entry) {
        Fraction f = entry.getFractionEnglishLinks();
        return (
            f != null
            && f.getNumerator() >= minNumLinks
            && 1.0 * f.getNumerator() / f.getDenominator() > minFractionLinks
        );
    }

    public int getMinNumLinks() {
        return minNumLinks;
    }
    public void setMinNumLinks(int minNumLinks) {
        this.minNumLinks = minNumLinks;
    }
    public double getMinFractionLinks() {
        return minFractionLinks;
    }
    public void setMinFractionLinks(double minFractionLinks) {
        this.minFractionLinks = minFractionLinks;
    }

    public static void main(String args[]) throws IOException, DatabaseException {
        if (args.length != 4) {
            System.err.println("usage: java " +
                    DictionaryIndexer.class.getName() +
                    "inputFile outputFile minNumLinks minFractionLinks");
            System.exit(1);
        }
        BufferedReader in;
        if (FilenameUtils.getExtension(args[0]).toLowerCase().startsWith("bz")) {
            in = new BufferedReader(
                    new InputStreamReader(
                            new BZip2CompressorInputStream(
                                    new FileInputStream(args[0]), true)));
        } else {
            in = new BufferedReader(new FileReader(args[0]));
        }
        int minNumLinks = Integer.valueOf(args[2]);
        double minFractionLinks = Double.valueOf(args[3]);
        DictionaryIndexer indexer = new DictionaryIndexer(new File(args[1]));
        indexer.setMinNumLinks(minNumLinks);
        indexer.setMinFractionLinks(minFractionLinks);
        indexer.index(in);
        in.close();
    }
}

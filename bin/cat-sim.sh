#!/bin/bash

if [ $# -ne 4 ]; then
    echo "usage: index.sh input_dir output_path max-sims memory_in_mb" >&2
    exit 1
fi

in=$1
out=$2
maxsims=$3
mb=$4

export MAVEN_OPTS="-Xmx${mb}M -ea"
mvn compile
mvn exec:java -D exec.mainClass="edu.macalester.wpsemsim.sim.category.CatSimilarity" -D exec.classpathScope=runtime  -D exec.args="$in $out $maxsims"

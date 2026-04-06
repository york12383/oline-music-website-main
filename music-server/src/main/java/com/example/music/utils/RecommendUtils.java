package com.example.music.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RecommendUtils {

    private RecommendUtils() {
    }

    public static double sparseCosineSimilarity(Map<Integer, Double> left,
                                                Map<Integer, Double> right,
                                                int minOverlap,
                                                double shrinkage) {
        SimilarityResult result = sparseCosineSimilarityWithMeta(left, right, minOverlap, shrinkage);
        return result.getSimilarity();
    }

    public static SimilarityResult sparseCosineSimilarityWithMeta(Map<Integer, Double> left,
                                                                  Map<Integer, Double> right,
                                                                  int minOverlap,
                                                                  double shrinkage) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            return new SimilarityResult(0, 0);
        }

        double leftNorm = 0;
        for (double value : left.values()) {
            if (value > 0) {
                leftNorm += value * value;
            }
        }

        double rightNorm = 0;
        for (double value : right.values()) {
            if (value > 0) {
                rightNorm += value * value;
            }
        }

        if (leftNorm <= 0 || rightNorm <= 0) {
            return new SimilarityResult(0, 0);
        }

        Map<Integer, Double> smaller = left.size() <= right.size() ? left : right;
        Map<Integer, Double> larger = smaller == left ? right : left;
        double dot = 0;
        int overlap = 0;
        for (Map.Entry<Integer, Double> entry : smaller.entrySet()) {
            Double rightValue = larger.get(entry.getKey());
            if (rightValue == null) {
                continue;
            }
            if (entry.getValue() <= 0 || rightValue <= 0) {
                continue;
            }
            dot += entry.getValue() * rightValue;
            overlap++;
        }

        if (overlap < minOverlap || dot <= 0) {
            return new SimilarityResult(0, overlap);
        }

        double cosine = dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
        double shrinkFactor = overlap / (overlap + shrinkage);
        return new SimilarityResult(cosine * shrinkFactor, overlap);
    }

    public static List<Neighbor> getTopSimilarUsers(Integer userId,
                                                    Map<Integer, Map<Integer, Double>> userVectors,
                                                    int k,
                                                    int minOverlap,
                                                    double shrinkage) {
        if (userId == null || userVectors == null || userVectors.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Integer, Double> targetVector = userVectors.get(userId);
        if (targetVector == null || targetVector.isEmpty()) {
            return Collections.emptyList();
        }

        List<Neighbor> neighbors = new ArrayList<>();
        for (Map.Entry<Integer, Map<Integer, Double>> entry : userVectors.entrySet()) {
            if (userId.equals(entry.getKey())) {
                continue;
            }

            SimilarityResult similarityResult = sparseCosineSimilarityWithMeta(
                    targetVector,
                    entry.getValue(),
                    minOverlap,
                    shrinkage
            );
            if (similarityResult.getSimilarity() > 0) {
                neighbors.add(new Neighbor(entry.getKey(), similarityResult.getSimilarity(), similarityResult.getOverlapCount()));
            }
        }

        neighbors.sort(Comparator.comparing(Neighbor::getSimilarity).reversed());
        if (neighbors.size() > k) {
            return new ArrayList<>(neighbors.subList(0, k));
        }
        return neighbors;
    }

    public static Map<Integer, Map<Integer, Double>> transposeUserVectors(Map<Integer, Map<Integer, Double>> userVectors) {
        Map<Integer, Map<Integer, Double>> itemVectors = new HashMap<>();
        if (userVectors == null || userVectors.isEmpty()) {
            return itemVectors;
        }

        for (Map.Entry<Integer, Map<Integer, Double>> userEntry : userVectors.entrySet()) {
            Integer userId = userEntry.getKey();
            for (Map.Entry<Integer, Double> itemEntry : userEntry.getValue().entrySet()) {
                if (itemEntry.getValue() == null || itemEntry.getValue() <= 0) {
                    continue;
                }
                itemVectors
                        .computeIfAbsent(itemEntry.getKey(), key -> new HashMap<>())
                        .put(userId, itemEntry.getValue());
            }
        }
        return itemVectors;
    }

    public static Map<Integer, Double> normalizeScores(Map<Integer, Double> rawScores) {
        Map<Integer, Double> normalized = new LinkedHashMap<>();
        if (rawScores == null || rawScores.isEmpty()) {
            return normalized;
        }

        double maxScore = 0;
        for (double score : rawScores.values()) {
            if (score > maxScore) {
                maxScore = score;
            }
        }

        if (maxScore <= 0) {
            for (Integer key : rawScores.keySet()) {
                normalized.put(key, 0.0);
            }
            return normalized;
        }

        for (Map.Entry<Integer, Double> entry : rawScores.entrySet()) {
            normalized.put(entry.getKey(), Math.max(0, entry.getValue()) / maxScore);
        }
        return normalized;
    }

    public static Map<String, Double> normalizeStringScores(Map<String, Double> rawScores) {
        Map<String, Double> normalized = new LinkedHashMap<>();
        if (rawScores == null || rawScores.isEmpty()) {
            return normalized;
        }

        double maxScore = 0;
        for (double score : rawScores.values()) {
            if (score > maxScore) {
                maxScore = score;
            }
        }

        if (maxScore <= 0) {
            for (String key : rawScores.keySet()) {
                normalized.put(key, 0.0);
            }
            return normalized;
        }

        for (Map.Entry<String, Double> entry : rawScores.entrySet()) {
            normalized.put(entry.getKey(), Math.max(0, entry.getValue()) / maxScore);
        }
        return normalized;
    }

    public static final class SimilarityResult {
        private final double similarity;
        private final int overlapCount;

        public SimilarityResult(double similarity, int overlapCount) {
            this.similarity = similarity;
            this.overlapCount = overlapCount;
        }

        public double getSimilarity() {
            return similarity;
        }

        public int getOverlapCount() {
            return overlapCount;
        }
    }

    public static final class Neighbor {
        private final Integer userId;
        private final double similarity;
        private final int overlapCount;

        public Neighbor(Integer userId, double similarity, int overlapCount) {
            this.userId = userId;
            this.similarity = similarity;
            this.overlapCount = overlapCount;
        }

        public Integer getUserId() {
            return userId;
        }

        public double getSimilarity() {
            return similarity;
        }

        public int getOverlapCount() {
            return overlapCount;
        }
    }
}

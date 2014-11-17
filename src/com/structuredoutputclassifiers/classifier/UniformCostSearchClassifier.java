package com.structuredoutputclassifiers.classifier;

import cc.mallet.classify.MaxEnt;
import cc.mallet.classify.MaxEntTrainer;
import cc.mallet.types.*;
import com.structuredoutputclassifiers.mallet.InstanceListWithCoreFeatures;
import com.structuredoutputclassifiers.util.MalletUtils;
import com.structuredoutputclassifiers.util.Sequence;

import java.util.*;

/**
 * Author: Marcin Dobrowolski
 */
public class UniformCostSearchClassifier extends LogisticRegressionClassifier {

    private double epsilon;

    public UniformCostSearchClassifier(double epsilon) {
        this.epsilon = epsilon;
    }

    private class Node {
        public double score = 0.0;
        public Sequence sequence;
    }

    private class NodeComparator implements Comparator<Node> {
        @Override
        public int compare(Node o1, Node o2) {
            return o1.score < o2.score ? 1 : -1;
        }
    }

    @Override
    public Sequence classify(Instance instance) {
        double epsilon = this.epsilon;
        Queue<Node> q = new PriorityQueue<>(1, new NodeComparator());
        Queue<Node> greedyQ = new PriorityQueue<>(1, new NodeComparator());
        LabelAlphabet labelAlphabet = (LabelAlphabet) instance.getTargetAlphabet();
        String[] states = labelAlphabetToStringArray(labelAlphabet);
        Instance[] instances = splitSequence(instance, null, labelAlphabet);
        double[] scores = new double[states.length];
        Node n = new Node();
        n.sequence = new Sequence();
        for (int i = 0; i < numBack; i++) {
            n.sequence.add("^");
        }
        q.add(n);
        while (q.size() != 0) {
            n = q.poll();
            int i = n.sequence.size() - numBack;
            if (i == instances.length) {
                n.sequence = new Sequence(n.sequence.subList(numBack, n.sequence.size()));
                return n.sequence;
            }

            List<String> prevSequence = new LinkedList<>(n.sequence.subList(i, n.sequence.size()));
            Collections.reverse(prevSequence);
            MalletUtils.setAdditionalFeatures(instances[i], coreFeatures, prevSequence);
            classifier.getClassificationScores(instances[i], scores);
            boolean noChildrenInserted = true;
            for (int j = 0; j < scores.length; j++) {
                Node newNode = new Node();
                newNode.score = n.score + log2(scores[j]);
                newNode.sequence = new Sequence(n.sequence);
                newNode.sequence.add(states[j]);
                if (newNode.score > epsilon) {
                    q.add(newNode);
                    noChildrenInserted = false;
                }
            }
            if (noChildrenInserted) {
                greedyQ.add(n);
            }
        }
        epsilon = Double.NEGATIVE_INFINITY;
        Node bestNode = null;
        while (greedyQ.size() != 0) {
            n = greedyQ.poll();
            if (n.score <= epsilon) {
                break;
            }
            int i;
            while ((i = n.sequence.size() - numBack) != instances.length) {
                List<String> prevSequence = new LinkedList<>(n.sequence.subList(i, n.sequence.size()));
                Collections.reverse(prevSequence);
                MalletUtils.setAdditionalFeatures(instances[i], coreFeatures, prevSequence);
                classifier.getClassificationScores(instances[i], scores);
                int maxIndex = MatrixOps.maxIndex(scores);
                n.sequence.add(states[maxIndex]);
                n.score += log2(scores[maxIndex]);
            }
            if (n.score > epsilon) {
                epsilon = n.score;
                bestNode = n;
            }
        }
        if (bestNode != null) {
            n.sequence = new Sequence(bestNode.sequence.subList(numBack, bestNode.sequence.size()));
            return n.sequence;
        }
        return null;
    }
}

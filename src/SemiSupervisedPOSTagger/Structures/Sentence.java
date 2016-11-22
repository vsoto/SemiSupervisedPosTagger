package SemiSupervisedPOSTagger.Structures;

import java.util.ArrayList;

/**
 * Created by Mohammad Sadegh Rasooli.
 * ML-NLP Lab, Department of Computer Science, Columbia University
 * Date Created: 1/8/15
 * Time: 4:37 PM
 * To report any bugs or problems contact rasooli@cs.columbia.edu
 */

public class Sentence {
    public int[] words;
    public int[] lowerWords;
    public String[] wordStrs;
    public int[] tags;
    public int[] lang_ids;

    public int[][] prefixes;
    public int[][] suffixes;
    public int[][] brownClusters;

    public boolean[] containsNumber;
    public boolean[] containsHyphen;
    public boolean[] containsUpperCaseLetter;

    public final static int brownSize = 12;

    private final static int BIT_SHIFT = 5;


    public Sentence(final ArrayList<String> words, final ArrayList<String> tags, final ArrayList<String> langids, final IndexMaps maps, final String delim) {
        this.words = new int[words.size()];
        this.lowerWords = new int[words.size()];
        this.wordStrs = new String[words.size()];
        this.tags = new int[tags.size()];
        this.lang_ids = new int[langids.size()];

        prefixes = new int[words.size()][4];
        suffixes = new int[words.size()][4];
        brownClusters = new int[words.size()][brownSize];
        containsNumber = new boolean[words.size()];
        containsHyphen = new boolean[words.size()];
        containsUpperCaseLetter = new boolean[words.size()];

        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            this.wordStrs[i] = word;
            String lowerWord = word.toLowerCase();
            if (maps.stringMap.containsKey(word))
                this.words[i] = maps.stringMap.get(word);
            else
                this.words[i] = SpecialWords.unknown.value;

            if (maps.stringMap.containsKey(word.toLowerCase()))
                this.lowerWords[i] = maps.stringMap.get(word.toLowerCase());
            else
                this.lowerWords[i] = SpecialWords.unknown.value;

            for (int p = 0; p < Math.min(4, word.length()); p++) {
                String prefix = lowerWord.substring(0, p + 1);
                String suffix = lowerWord.substring(word.length() - p - 1);

                if (maps.stringMap.containsKey(prefix))
                    prefixes[i][p] = maps.stringMap.get(prefix);
                else
                    prefixes[i][p] = SpecialWords.unknown.value;

                if (maps.stringMap.containsKey(suffix))
                    suffixes[i][p] = maps.stringMap.get(suffix);
                else
                    suffixes[i][p] = SpecialWords.unknown.value;
            }
            if (word.length() < 4) {
                for (int p = word.length(); p < 4; p++) {
                    prefixes[i][p] = SpecialWords.unknown.value;
                    suffixes[i][p] = SpecialWords.unknown.value;
                }
            }

            brownClusters[i] = maps.clusterIds(word);

            boolean hasUpperCase = false;
            boolean hasHyphen = false;
            boolean hasNumber = false;
            for (char c : word.toCharArray()) {
                if (!hasUpperCase && Character.isUpperCase(c))
                    hasUpperCase = true;
                if (!hasHyphen && c == '-')
                    hasHyphen = true;
                if (!hasNumber && Character.isDigit(c))
                    hasNumber = true;
                if (hasHyphen && hasNumber && hasUpperCase)
                    break;
            }

            containsHyphen[i] = hasHyphen;
            containsNumber[i] = hasNumber;
            containsUpperCaseLetter[i] = hasUpperCase;

            if (maps.stringMap.containsKey(langids.get(i)))
                this.lang_ids[i] = maps.stringMap.get(langids.get(i));
            else
                this.lang_ids[i] = SpecialWords.unknown.value;


            if (tags.get(i).equals("***")) //for unknown tag
                this.tags[i] = SpecialWords.unknown.value;
            else if (maps.stringMap.containsKey(tags.get(i)))
                this.tags[i] = maps.stringMap.get(tags.get(i));
            else
                this.tags[i] = SpecialWords.unknown.value;
        }
    }

    public int[] getEmissionFeatures(final int position, final int featSize) {
        int[] features = new int[featSize];
        int index = 0;
        int length = words.length;

        int currentWord = 0;
        if (position >= 0 && position < length)
            currentWord = words[position];
        else if (position >= length)
            currentWord = 1;

        features[index++] = currentWord;

        if (position >= 0 && position < length) {
            for (int i = 0; i < 4; i++) {
                features[index++] = prefixes[position][i];
                features[index++] = suffixes[position][i];
                // TODO(vsoto): do this for every other feature.
                features[index++] = prefixes[position][i] << BIT_SHIFT | this.lang_ids[position];
                features[index++] = suffixes[position][i] << BIT_SHIFT | this.lang_ids[position];
            }
            features[index++] = (containsHyphen[position]) ? 1 : SpecialWords.unknown.value;
            features[index++] = (containsNumber[position]) ? 1 : SpecialWords.unknown.value;
            features[index++] = (containsUpperCaseLetter[position]) ? 1 : SpecialWords.unknown.value;

        } else {
            for (int i = 0; i < 11; i++) {
                features[index++] = SpecialWords.unknown.value;
            }
        }

        int prevWord = SpecialWords.start.value;
        int prev2Word = SpecialWords.start.value;
        int nextWord = SpecialWords.stop.value;
        int next2Word = SpecialWords.stop.value;
        int prevCluster = SpecialWords.unknown.value;
        int prev2Cluster = SpecialWords.unknown.value;
        int nextCluster = SpecialWords.unknown.value;
        int next2Cluster = SpecialWords.unknown.value;

        int prevPosition = position - 1;

        if (prevPosition >= 0) {
            prevWord = words[prevPosition];
            prevCluster = brownClusters[prevPosition][0];
            int prev2Position = prevPosition - 1;

            if (prev2Position >= 0) {
                prev2Word = words[prev2Position];
                prev2Cluster = brownClusters[prev2Position][0];
            }
        }

        int nextPosition = position + 1;
        if (nextPosition < length) {
            nextWord = words[nextPosition];
            nextCluster = brownClusters[nextPosition][0];
            int next2Position = nextPosition + 1;
            if (next2Position < length) {
                next2Word = words[next2Position];
                next2Cluster = brownClusters[next2Position][0];
            }
        }
        features[index++] = prevWord;
        features[index++] = prev2Word;
        features[index++] = nextWord;
        features[index++] = next2Word;

        features[index++] = prevCluster;
        features[index++] = (prev2Cluster);

        //  features[index++]=nextCluster;
        // features[index++]=next2Cluster;

        for (int i = 1; i < brownSize; i++) {
            if (position >= 0 && position < length) {
                //  features[index++] = brownClusters[position][0];
                features[index++] = brownClusters[position][i];
                //    features[index++] = brownClusters[position][2];
            } else {
                //  features[index++]=SpecialWords.unknown.value;
                features[index++] = SpecialWords.unknown.value;
                //  features[index++] = SpecialWords.unknown.value;
            }
        }

        return features;

    }

    public int[] getFeatures(final int position, final int prev2Tag, final int prevTag, final int featSize) {
        int[] features = getEmissionFeatures(position, featSize);
        // -2 to add prevTag and bigram at the end.
        int index = featSize - 4;

        features[index++] = prevTag;
        int bigram = (prev2Tag << 10) + prevTag;
        features[index++] = bigram;

        System.out.println(this.lang_ids);
        System.out.println(position);
        features[index++] = prevTag << BIT_SHIFT | this.lang_ids[position];
        features[index++] = bigram << BIT_SHIFT | this.lang_ids[position];

        return features;
    }

}

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
    public int[] pos_tags;
    public int[] lang_ids;

    public int[][] prefixes;
    public int[][] suffixes;
    public int[][] brownClusters;

    public boolean[] containsNumber;
    public boolean[] containsHyphen;
    public boolean[] containsUpperCaseLetter;

    public final static int brownSize = 12;

    private final static int MAX_AFFIX_LENGTH = 4;
    private final static int BIT_SHIFT = 5;


    public Sentence(final ArrayList<String> words, final ArrayList<String> pos_tags, final ArrayList<String> lang_ids, final IndexMaps maps) {
        this.words = new int[words.size()];
        this.lowerWords = new int[words.size()];
        this.wordStrs = new String[words.size()];
        this.pos_tags = new int[pos_tags.size()];
        this.lang_ids = new int[lang_ids.size()];

        prefixes = new int[words.size()][MAX_AFFIX_LENGTH];
        suffixes = new int[words.size()][MAX_AFFIX_LENGTH];
        brownClusters = new int[words.size()][brownSize];
        containsNumber = new boolean[words.size()];
        containsHyphen = new boolean[words.size()];
        containsUpperCaseLetter = new boolean[words.size()];

        assert(words.size() == pos_tags.size());
        assert(words.size() == lang_ids.size());

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

            for (int p = 0; p < Math.min(MAX_AFFIX_LENGTH, word.length()); p++) {
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
            if (word.length() < MAX_AFFIX_LENGTH) {
                for (int p = word.length(); p < MAX_AFFIX_LENGTH; p++) {
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

            if (maps.stringMap.containsKey(lang_ids.get(i)))
                this.lang_ids[i] = maps.stringMap.get(lang_ids.get(i));
            else
                this.lang_ids[i] = SpecialWords.unknown.value;


            if (pos_tags.get(i).equals("***")) //for unknown tag
                this.pos_tags[i] = SpecialWords.unknown.value;
            else if (maps.stringMap.containsKey(pos_tags.get(i)))
                this.pos_tags[i] = maps.stringMap.get(pos_tags.get(i));
            else
                this.pos_tags[i] = SpecialWords.unknown.value;
        }
    }

    public int[] get_emission_features(final int position, final int feat_size) {
        int[] features = new int[feat_size];
        int index = 0;
        int length = words.length;

        int current_word = 0;
        if (position >= 0 && position < length)
            current_word = words[position];
        else if (position >= length)
            // TODO(vsoto): What is this?
            current_word = 1;

        features[index++] = current_word;

        if (position >= 0 && position < length) {
            for (int i = 0; i < MAX_AFFIX_LENGTH; i++) {
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
            // TODO(vsoto): 19 is 4*4 + 3. Look at loop in if clause
            for (int i = 0; i < 19; i++) {
                features[index++] = SpecialWords.unknown.value;
            }
        }

        int prevWord = SpecialWords.start.value;
        int prev2Word = SpecialWords.start.value;
        int nextWord = SpecialWords.stop.value;
        int next2Word = SpecialWords.stop.value;
        int prevCluster = SpecialWords.unknown.value;
        int prev2Cluster = SpecialWords.unknown.value;

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
            int next2Position = nextPosition + 1;
            if (next2Position < length) {
                next2Word = words[next2Position];
            }
        }
        features[index++] = prevWord;
        features[index++] = prev2Word;
        features[index++] = nextWord;
        features[index++] = next2Word;

        features[index++] = prevCluster;
        features[index++] = (prev2Cluster);

        for (int i = 1; i < brownSize; i++) {
            if (position >= 0 && position < length) {
                features[index++] = brownClusters[position][i];
            } else {
                features[index++] = SpecialWords.unknown.value;
            }
        }
        return features;
    }

    public int[] getFeatures(final int position, final int prev2Tag, final int prevTag, final int featSize) {
        int[] features = get_emission_features(position, featSize);
        // -2 to add prevTag and bigram at the end.
        int index = featSize - 4;

        features[index++] = prevTag;
        int bigram = (prev2Tag << 10) + prevTag;
        features[index++] = bigram;

        System.out.println("Position: " +  position);
        System.out.println("Length: " + this.lang_ids.length);
        for (int i = 0 ; i < this.lang_ids.length; ++i) {
            System.out.print(" " + this.lang_ids[i]);
        }
        System.out.println(" ");
        features[index++] = prevTag << BIT_SHIFT | this.lang_ids[position];
        features[index++] = bigram << BIT_SHIFT | this.lang_ids[position];

        return features;
    }

}

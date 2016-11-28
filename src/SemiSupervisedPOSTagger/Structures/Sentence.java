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
    public int[] lowercase_words;
    public String[] string_words;
    public int[] pos_tags;
    public int[] lang_ids;

    public int[][] prefixes;
    public int[][] suffixes;
    public int[][] brown_clusters;

    public boolean[] contains_number;
    public boolean[] contains_hyphen;
    public boolean[] contains_uppercase;

    public final static int BROWN_SIZE = 12;
    public final static int NUM_FEATURES = 64;
    public final static int MAX_AFFIX_LENGTH = 4;
    private final static int BIT_SHIFT = 5;


    public Sentence(final ArrayList<String> words, final ArrayList<String> pos_tags, final ArrayList<String> lang_ids, final IndexMaps maps) {
        this.words = new int[words.size()];
        this.lowercase_words = new int[words.size()];
        this.string_words = new String[words.size()];
        this.pos_tags = new int[pos_tags.size()];
        this.lang_ids = new int[lang_ids.size()];

        prefixes = new int[words.size()][MAX_AFFIX_LENGTH];
        suffixes = new int[words.size()][MAX_AFFIX_LENGTH];
        brown_clusters = new int[words.size()][BROWN_SIZE];
        contains_number = new boolean[words.size()];
        contains_hyphen = new boolean[words.size()];
        contains_uppercase = new boolean[words.size()];

        assert(words.size() == pos_tags.size());
        assert(words.size() == lang_ids.size());

        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            this.string_words[i] = word;
            String lowerWord = word.toLowerCase();
            if (maps.stringMap.containsKey(word))
                this.words[i] = maps.stringMap.get(word);
            else
                this.words[i] = SpecialWords.unknown.value;

            if (maps.stringMap.containsKey(word.toLowerCase()))
                this.lowercase_words[i] = maps.stringMap.get(word.toLowerCase());
            else
                this.lowercase_words[i] = SpecialWords.unknown.value;

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

            brown_clusters[i] = maps.clusterIds(word);

            boolean has_uppercase = false;
            boolean has_hyphen = false;
            boolean has_number = false;
            for (char c : word.toCharArray()) {
                if (!has_uppercase && Character.isUpperCase(c))
                    has_uppercase = true;
                if (!has_hyphen && c == '-')
                    has_hyphen = true;
                if (!has_number && Character.isDigit(c))
                    has_number = true;
                if (has_hyphen && has_number && has_uppercase)
                    break;
            }

            contains_hyphen[i] = has_hyphen;
            contains_number[i] = has_number;
            contains_uppercase[i] = has_uppercase;

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

    public int[] get_emission_features(final int position) {
        int[] features = new int[NUM_FEATURES];
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
            features[index++] = (contains_hyphen[position]) ? 1 : SpecialWords.unknown.value;
            features[index++] = (contains_number[position]) ? 1 : SpecialWords.unknown.value;
            features[index++] = (contains_uppercase[position]) ? 1 : SpecialWords.unknown.value;

        } else {
            // TODO(vsoto): 19 is 4*4 + 3. Look at loop in if clause
            for (int i = 0; i < 19; i++) {
                features[index++] = SpecialWords.unknown.value;
            }
        }

        int previous_word = SpecialWords.start.value;
        int penultimate_word = SpecialWords.start.value;
        int next_word = SpecialWords.stop.value;
        int next_to_next_word = SpecialWords.stop.value;
        int previous_cluster = SpecialWords.unknown.value;
        int penultimate_cluster = SpecialWords.unknown.value;

        int previous_position = position - 1;

        if (previous_position >= 0) {
            previous_word = words[previous_position];
            previous_cluster = brown_clusters[previous_position][0];
            int penultimate_position = previous_position - 1;

            if (penultimate_position >= 0) {
                penultimate_word = words[penultimate_position];
                penultimate_cluster = brown_clusters[penultimate_position][0];
            }
        }

        int next_position = position + 1;
        if (next_position < length) {
            next_word = words[next_position];
            int next_next_position = next_position + 1;
            if (next_next_position < length) {
                next_to_next_word = words[next_next_position];
            }
        }
        features[index++] = previous_word;
        features[index++] = penultimate_word;
        features[index++] = next_word;
        features[index++] = next_to_next_word;

        features[index++] = previous_cluster;
        features[index++] = (penultimate_cluster);

        for (int i = 1; i < BROWN_SIZE; i++) {
            if (position >= 0 && position < length) {
                features[index++] = brown_clusters[position][i];
            } else {
                features[index++] = SpecialWords.unknown.value;
            }
        }
        return features;
    }

    public int[] get_features(final int position, final int penultimate_tag, final int last_tag) {
        int[] features = get_emission_features(position);
        // -4 to add last_tag and bigram at the end.
        int index = Sentence.NUM_FEATURES - 4;
        int bigram = (penultimate_tag << 10) + last_tag;

        int max_length = words.length;

        if (position < max_length) {
            features[index++] = last_tag << BIT_SHIFT | this.lang_ids[position];
            features[index++] = bigram << BIT_SHIFT | this.lang_ids[position];
        } else {
            // TODO(vsoto): do this
            features[index++] = last_tag << BIT_SHIFT;
            features[index++] = bigram << BIT_SHIFT;
        }

        features[index++] = last_tag;
        features[index++] = bigram;

        return features;
    }

}

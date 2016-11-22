package SemiSupervisedPOSTagger.IO;

import SemiSupervisedPOSTagger.Structures.IndexMaps;
import SemiSupervisedPOSTagger.Structures.Sentence;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by Mohammad Sadegh Rasooli.
 * ML-NLP Lab, Department of Computer Science, Columbia University
 * Date Created: 1/8/15
 * Time: 6:50 PM
 * To report any bugs or problems contact rasooli@cs.columbia.edu
 */

public class FileManager {


    public static ArrayList<Sentence> readSentences(String filePath, IndexMaps maps, String delim) throws Exception {
        System.out.print("Reading CONLL sentences...");
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;
        ArrayList<Sentence> sentences = new ArrayList<Sentence>();

        ArrayList<String> words = new ArrayList<String>();
        ArrayList<String> pos_tags = new ArrayList<String>();
        ArrayList<String> lang_tags = new ArrayList<String>();

        while ((line = reader.readLine()) != null) {
            if (line.trim().length() > 0) {
                sentences.add(new Sentence(words, pos_tags, lang_tags, maps, delim));
                words.clear();
                pos_tags.clear();
                lang_tags.clear();
            } else {
                String[] tokens = line.trim().split("\t");
                words.add(tokens[1]);
                pos_tags.add(tokens[2]);
                lang_tags.add(tokens[3]);
            }
        }
        System.out.print("Done!\n");
        return sentences;
    }

    public static IndexMaps createIndexMaps(String filePath, String delim, String clusterFile, String tagDictionaryPath, int brownSize) throws Exception {
        System.out.print("creating index maps...");
        HashMap<String, Integer> stringMap = new HashMap<String, Integer>();
        HashMap<String, Integer> clusterMap = new HashMap<String, Integer>();
        HashMap<Integer, Integer>[] clusterNMap = new HashMap[brownSize];
        for (int i = 0; i < brownSize; i++) {
            clusterNMap[i] = new HashMap<Integer, Integer>();
        }

        BufferedReader reader = new BufferedReader(new FileReader(filePath));

        HashSet<String> tags = new HashSet<String>();
        HashSet<String> words = new HashSet<String>();
        HashSet<String> lang_ids = new HashSet<String>();

        String line;
        while ((line = reader.readLine()) != null) {
            String[] tokens = line.trim().split("\t");
            String word = tokens[1];
            String lang_id = tokens[2];
            String pos_tag = tokens[3];

            for (int p = 0; p < Math.min(4, word.length()); p++) {
                String prefix = word.substring(0, p + 1);
                String suffix = word.substring(word.length() - p - 1);
                words.add(prefix);
                words.add(suffix);
            }

            tags.add(pos_tag);
            words.add(word);
            lang_ids.add(lang_id);
        }

        // 0 and 1 are reserved for stop and start
        int index = 2;
        stringMap.put("<<START>>", 0);
        stringMap.put("<<STOP>>", 1);

        for (String lang_id : lang_ids) {
            stringMap.put(lang_id, index++);
        }

        for (String t : tags) {
            stringMap.put(t, index++);
        }

        if (clusterFile.length() > 0) {
            reader = new BufferedReader(new FileReader(clusterFile));
            while ((line = reader.readLine()) != null) {
                String[] spl = line.trim().split("\t");
                if (spl.length > 2) {
                    String cluster = spl[0];
                    String word = spl[1];
                    int clusterNum = index;

                    if (!stringMap.containsKey(cluster)) {
                        clusterMap.put(word, index);
                        stringMap.put(cluster, index++);
                    } else {
                        clusterNum = stringMap.get(cluster);
                        clusterMap.put(word, clusterNum);
                    }

                    for (int i = 0; i < brownSize; i++) {
                        int prefId = index;
                        String prefix = cluster.substring(0, Math.min(i + 1, cluster.length()));
                        if (!stringMap.containsKey(prefix)) {
                            stringMap.put(prefix, index++);
                        } else {
                            prefId = stringMap.get(prefix);
                        }
                        clusterNMap[i].put(clusterNum, prefId);
                    }

                }
            }
        }

        for (String w : words) {
            if (!stringMap.containsKey(w))
                stringMap.put(w, index++);
            if (!stringMap.containsKey(w.toLowerCase()))
                stringMap.put(w.toLowerCase(), index++);
        }

        System.out.println(stringMap.size());
        String[] reversedMap = new String[stringMap.size()];
        for (String k : stringMap.keySet()) {
            reversedMap[stringMap.get(k)] = k;
        }

        int tagSize = tags.size() + 2;
        System.out.print("done!\n");

        HashMap<Integer, HashSet<Integer>> tagDictionary = new HashMap<Integer, HashSet<Integer>>();
        if (tagDictionaryPath != null && !tagDictionaryPath.equals("")) {
            BufferedReader tagDictionaryReader = new BufferedReader(new FileReader(tagDictionaryPath));
            while ((line = tagDictionaryReader.readLine()) != null) {
                String[] spl = line.split("\t");
                if (spl.length == 2) {
                    String w = spl[0].toLowerCase();
                    String t = spl[1];

                    if (!stringMap.containsKey(w))
                        stringMap.put(w, index++);
                    if (!stringMap.containsKey(t))
                        stringMap.put(t, index++);

                    int wi = stringMap.get(w);
                    int ti = stringMap.get(t);

                    if (!tagDictionary.containsKey(wi))
                        tagDictionary.put(wi, new HashSet<Integer>());
                    tagDictionary.get(wi).add(ti);
                }
            }
        }

        return new IndexMaps(tagSize, stringMap, reversedMap, clusterNMap, clusterMap, tagDictionary);
    }
}

package com.example.musicandpicture.util;

import android.util.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class KeywordExtractor {
    private static final String TAG = "KeywordExtractor";

    private static final Set<String> STOP_WORDS = new HashSet<String>() {{
        // 常见的英文停用词
        add("a"); add("an"); add("the"); add("and"); add("or"); add("but");
        add("is"); add("are"); add("was"); add("were"); add("be"); add("been");
        add("being"); add("have"); add("has"); add("had"); add("do"); add("does");
        add("did"); add("to"); add("at"); add("in"); add("on"); add("for");
        add("with"); add("by"); add("about"); add("against"); add("between");
        add("into"); add("through"); add("during"); add("before"); add("after");
        add("above"); add("below"); add("from"); add("up"); add("down");
        add("of"); add("off"); add("over"); add("under"); add("again");
        add("further"); add("then"); add("once"); add("here"); add("there");
        add("when"); add("where"); add("why"); add("how"); add("all");
        add("any"); add("both"); add("each"); add("few"); add("more");
        add("most"); add("other"); add("some"); add("such"); add("no");
        add("nor"); add("not"); add("only"); add("own"); add("same");
        add("so"); add("than"); add("too"); add("very"); add("s");
        add("t"); add("will"); add("just"); add("don"); add("should");
        add("now"); add("oh"); add("yeah"); add("la"); add("na");
        add("i"); add("me"); add("my"); add("myself"); add("we");
        add("our"); add("ours"); add("ourselves"); add("you"); add("your");
        add("yours"); add("yourself"); add("yourselves"); add("he"); add("him");
        add("his"); add("himself"); add("she"); add("her"); add("hers");
        add("herself"); add("it"); add("its"); add("itself"); add("they");
        add("them"); add("their"); add("theirs"); add("themselves");
        add("what"); add("which"); add("who"); add("whom"); add("this");
        add("that"); add("these"); add("those"); add("am"); add("would");
        add("could"); add("should"); add("shall"); add("may"); add("might");
        add("must"); add("cant"); add("wont"); add("dont"); add("im");
        add("youre"); add("hes"); add("shes"); add("its"); add("were");
        add("theyre"); add("ive"); add("youve"); add("weve"); add("theyve");
        add("id"); add("youd"); add("hed"); add("shed"); add("wed");
        add("theyd"); add("cant"); add("cannot"); add("couldnt");
        add("didnt"); add("doesnt"); add("dont"); add("isnt");

        // 常见的中文停用词
        add("的"); add("了"); add("和"); add("是"); add("在"); add("我");
        add("你"); add("他"); add("她"); add("它"); add("这"); add("那");
        add("着"); add("呢"); add("吗"); add("啊"); add("哦"); add("嗯");
        add("哎"); add("哼"); add("唉"); add("嘿"); add("喂"); add("嗨");
        add("吧"); add("呀"); add("么"); add("哒"); add("呐"); add("啦");
        add("就"); add("都"); add("而"); add("及"); add("与"); add("或");
        add("一个"); add("一些"); add("一何"); add("一切"); add("一则"); add("也");
        add("若"); add("若夫"); add("若是"); add("但"); add("但是"); add("何");
        add("何以"); add("何况"); add("何处"); add("何时"); add("作为"); add("你们");
        add("使"); add("使得"); add("例如"); add("依"); add("依照"); add("依靠");
        add("便"); add("俺"); add("俺们"); add("倘"); add("倘使"); add("倘或");
        add("倘然"); add("倘若"); add("借"); add("借以"); add("假使"); add("假如");
        add("前"); add("叮咚"); add("可"); add("吱"); add("呃"); add("呗");
        add("够瞧"); add("够戗"); add("巴"); add("巴巴"); add("啊哈"); add("喽");
        add("嗡"); add("嗡嗡"); add("嗬"); add("嗯哼"); add("嘻"); add("嘿嘿");
        add("自"); add("自从"); add("自各儿"); add("自家"); add("自己"); add("咱");
        add("咱们"); add("啷当"); add("啷唷"); add("喔唷"); add("嗳"); add("矣哉");
        add("人家"); add("什么"); add("什么样"); add("怎");
    }};

    /**
     * 提取关键词
     * @param text 输入文本
     * @param topN 要返回的关键词数量
     * @return 关键词列表
     */
    public static List<String> extractKeywords(String text, int topN) {
        Log.d(TAG, "Extracting keywords from text length: " + (text != null ? text.length() : 0));

        if (text == null || text.trim().isEmpty()) {
            Log.w(TAG, "Empty text provided for keyword extraction");
            return Collections.emptyList();
        }

        // 移除标点符号和数字
        text = text.toLowerCase();
        text = Pattern.compile("[\\p{P}\\d]").matcher(text).replaceAll(" ");

        Log.d(TAG, "Text after removing punctuation and numbers: " + text.substring(0, Math.min(100, text.length())) + "...");

        // 分词并计数
        String[] words = text.split("\\s+");
        Map<String, Integer> wordCounts = new HashMap<>();

        int totalValidWords = 0;
        for (String word : words) {
            word = word.trim();
            if (word.isEmpty() || STOP_WORDS.contains(word) || word.length() < 2) {
                continue;
            }

            wordCounts.put(word, wordCounts.getOrDefault(word, 0) + 1);
            totalValidWords++;
        }

        Log.d(TAG, "Found " + wordCounts.size() + " unique words out of " + totalValidWords + " total valid words");

        // 按出现频率排序
        List<Map.Entry<String, Integer>> sortedWords = new ArrayList<>(wordCounts.entrySet());
        sortedWords.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        // 取前N个关键词
        List<String> topKeywords = new ArrayList<>();
        int count = 0;
        StringBuilder logBuilder = new StringBuilder("Top keywords: ");

        for (Map.Entry<String, Integer> entry : sortedWords) {
            if (count >= topN) break;
            String keyword = entry.getKey();
            int freq = entry.getValue();

            topKeywords.add(keyword);
            logBuilder.append(keyword).append("(").append(freq).append(") ");
            count++;
        }

        Log.d(TAG, logBuilder.toString());
        return topKeywords;
    }

    /**
     * 从歌词中提取关键词
     * @param lyrics 歌词文本
     * @param count 要返回的关键词数量
     * @return 关键词数组
     */
    public static String[] extractKeywordsFromLyrics(String lyrics, int count) {
        if (lyrics == null || lyrics.isEmpty()) {
            Log.w(TAG, "Empty lyrics provided");
            return new String[0];
        }

        Log.d(TAG, "Extracting keywords from lyrics: " + lyrics.substring(0, Math.min(100, lyrics.length())) + "...");

        // 处理常见的歌词格式问题
        // 移除时间标记 [00:00.00]
        lyrics = lyrics.replaceAll("\\[\\d{2}:\\d{2}\\.\\d{2}\\]", "");
        // 移除行号
        lyrics = lyrics.replaceAll("^\\d+\\.\\s*", "");

        List<String> keywords = extractKeywords(lyrics, count);
        return keywords.toArray(new String[0]);
    }
}
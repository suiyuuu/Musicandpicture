package com.example.musicandpicture.util;

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

        // 常见的中文停用词
        add("的"); add("了"); add("和"); add("是"); add("在"); add("我");
        add("你"); add("他"); add("她"); add("它"); add("这"); add("那");
        add("着"); add("呢"); add("吗"); add("啊"); add("哦"); add("嗯");
        add("哎"); add("哼"); add("唉"); add("嘿"); add("喂"); add("嗨");
        add("吧"); add("呀"); add("么"); add("哒"); add("呐"); add("啦");
    }};

    // 提取关键词
    public static List<String> extractKeywords(String text, int topN) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // 移除标点符号和数字
        text = text.toLowerCase();
        text = Pattern.compile("[\\p{P}\\d]").matcher(text).replaceAll(" ");

        // 分词并计数
        String[] words = text.split("\\s+");
        Map<String, Integer> wordCounts = new HashMap<>();

        for (String word : words) {
            word = word.trim();
            if (word.isEmpty() || STOP_WORDS.contains(word) || word.length() < 2) {
                continue;
            }

            wordCounts.put(word, wordCounts.getOrDefault(word, 0) + 1);
        }

        // 按出现频率排序
        List<Map.Entry<String, Integer>> sortedWords = new ArrayList<>(wordCounts.entrySet());
        sortedWords.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        // 取前N个关键词
        List<String> topKeywords = new ArrayList<>();
        int count = 0;
        for (Map.Entry<String, Integer> entry : sortedWords) {
            if (count >= topN) break;
            topKeywords.add(entry.getKey());
            count++;
        }

        return topKeywords;
    }
}

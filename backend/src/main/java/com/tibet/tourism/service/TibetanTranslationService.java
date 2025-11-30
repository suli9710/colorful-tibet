package com.tibet.tourism.service;

import com.tibet.tourism.entity.TibetanDictionary;
import com.tibet.tourism.repository.TibetanDictionaryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 藏语翻译服务
 * 提供基于词典的翻译功能，支持自动翻译和词典管理
 */
@Service
public class TibetanTranslationService {

    @Autowired
    private TibetanDictionaryRepository dictionaryRepository;

    /**
     * 翻译中文文本为藏语
     * 优先使用词典中的翻译，如果没有则返回null
     */
    @Transactional(readOnly = true)
    public String translate(String chineseText) {
        if (chineseText == null || chineseText.trim().isEmpty()) {
            return null;
        }

        // 首先尝试精确匹配
        Optional<TibetanDictionary> exactMatch = dictionaryRepository
                .findByChineseTextAndType(chineseText.trim(), TibetanDictionary.Type.SENTENCE);
        if (exactMatch.isPresent()) {
            return exactMatch.get().getTibetanText();
        }

        // 尝试短语匹配
        List<TibetanDictionary> phraseMatches = dictionaryRepository
                .findByType(TibetanDictionary.Type.PHRASE);
        String result = matchPhrases(chineseText, phraseMatches);
        if (result != null) {
            return result;
        }

        // 尝试单词匹配（分词翻译）
        List<TibetanDictionary> wordMatches = dictionaryRepository
                .findByType(TibetanDictionary.Type.WORD);
        result = translateByWords(chineseText, wordMatches);
        
        return result;
    }

    /**
     * 翻译并自动创建词典条目（如果不存在）
     */
    @Transactional
    public String translateOrCreate(String chineseText, String tibetanText, TibetanDictionary.Type type) {
        if (chineseText == null || chineseText.trim().isEmpty()) {
            return null;
        }

        String trimmedChinese = chineseText.trim();
        
        // 查找是否已存在
        Optional<TibetanDictionary> existing = dictionaryRepository
                .findByChineseTextAndType(trimmedChinese, type);
        
        if (existing.isPresent()) {
            TibetanDictionary dict = existing.get();
            // 如果提供了新的翻译，更新它
            if (tibetanText != null && !tibetanText.trim().isEmpty()) {
                dict.setTibetanText(tibetanText.trim());
                dict.incrementUsageCount();
                dictionaryRepository.save(dict);
            } else {
                dict.incrementUsageCount();
                dictionaryRepository.save(dict);
            }
            return dict.getTibetanText();
        } else {
            // 创建新条目
            if (tibetanText == null || tibetanText.trim().isEmpty()) {
                // 如果没有提供翻译，尝试自动翻译
                tibetanText = translate(trimmedChinese);
                if (tibetanText == null) {
                    return null; // 无法翻译
                }
            }
            
            TibetanDictionary newEntry = new TibetanDictionary();
            newEntry.setChineseText(trimmedChinese);
            newEntry.setTibetanText(tibetanText.trim());
            newEntry.setType(type);
            newEntry.setUsageCount(1);
            dictionaryRepository.save(newEntry);
            
            return newEntry.getTibetanText();
        }
    }

    /**
     * 自动翻译景点描述（基于词典）
     */
    @Transactional
    public String translateDescription(String chineseDescription) {
        if (chineseDescription == null || chineseDescription.trim().isEmpty()) {
            return null;
        }

        // 首先尝试完整句子匹配
        String result = translate(chineseDescription);
        if (result != null) {
            return result;
        }

        // 如果完整句子无法匹配，尝试分段翻译
        List<TibetanDictionary> allEntries = dictionaryRepository.findAll();
        return translateBySegments(chineseDescription, allEntries);
    }

    /**
     * 使用短语匹配翻译
     */
    private String matchPhrases(String text, List<TibetanDictionary> phrases) {
        // 按长度降序排序，优先匹配长短语
        List<TibetanDictionary> sortedPhrases = phrases.stream()
                .sorted((a, b) -> Integer.compare(b.getChineseText().length(), a.getChineseText().length()))
                .collect(Collectors.toList());

        StringBuilder result = new StringBuilder();
        int pos = 0;
        String remaining = text;

        while (pos < text.length()) {
            boolean matched = false;
            for (TibetanDictionary phrase : sortedPhrases) {
                String phraseText = phrase.getChineseText();
                if (remaining.startsWith(phraseText)) {
                    result.append(phrase.getTibetanText());
                    pos += phraseText.length();
                    remaining = text.substring(pos);
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                // 如果无法匹配，保留原字符（或使用单词翻译）
                result.append(text.charAt(pos));
                pos++;
                remaining = text.substring(pos);
            }
        }

        return result.length() > 0 ? result.toString() : null;
    }

    /**
     * 使用单词翻译
     */
    private String translateByWords(String text, List<TibetanDictionary> words) {
        // 简单的分词：按常见分隔符分割
        String[] segments = text.split("[，。、；：！？\\s]+");
        StringBuilder result = new StringBuilder();
        
        Map<String, String> wordMap = words.stream()
                .collect(Collectors.toMap(
                        TibetanDictionary::getChineseText,
                        TibetanDictionary::getTibetanText,
                        (v1, v2) -> v1 // 如果有重复，保留第一个
                ));

        for (String segment : segments) {
            if (segment.isEmpty()) continue;
            
            String translation = wordMap.get(segment.trim());
            if (translation != null) {
                result.append(translation).append(" ");
            } else {
                // 如果单词不在词典中，尝试部分匹配
                Optional<TibetanDictionary> partialMatch = words.stream()
                        .filter(w -> segment.contains(w.getChineseText()))
                        .findFirst();
                if (partialMatch.isPresent()) {
                    result.append(partialMatch.get().getTibetanText()).append(" ");
                }
            }
        }

        return result.length() > 0 ? result.toString().trim() : null;
    }

    /**
     * 分段翻译
     */
    private String translateBySegments(String text, List<TibetanDictionary> allEntries) {
        // 按句子分割
        String[] sentences = text.split("[。！？]");
        StringBuilder result = new StringBuilder();

        for (String sentence : sentences) {
            if (sentence.trim().isEmpty()) continue;
            
            String translated = translate(sentence.trim());
            if (translated != null) {
                result.append(translated).append(" ");
            } else {
                // 如果无法翻译，尝试短语和单词组合
                String phraseResult = matchPhrases(sentence, 
                        allEntries.stream()
                                .filter(e -> e.getType() == TibetanDictionary.Type.PHRASE)
                                .collect(Collectors.toList()));
                if (phraseResult != null) {
                    result.append(phraseResult).append(" ");
                }
            }
        }

        return result.length() > 0 ? result.toString().trim() : null;
    }

    /**
     * 批量添加词典条目
     */
    @Transactional
    public List<TibetanDictionary> batchAddEntries(Map<String, String> translations, TibetanDictionary.Type type) {
        List<TibetanDictionary> saved = new ArrayList<>();
        for (Map.Entry<String, String> entry : translations.entrySet()) {
            TibetanDictionary dict = new TibetanDictionary();
            dict.setChineseText(entry.getKey().trim());
            dict.setTibetanText(entry.getValue().trim());
            dict.setType(type);
            dict.setUsageCount(0);
            
            // 检查是否已存在
            Optional<TibetanDictionary> existing = dictionaryRepository
                    .findByChineseTextAndType(dict.getChineseText(), type);
            if (existing.isPresent()) {
                // 更新现有条目
                TibetanDictionary existingDict = existing.get();
                existingDict.setTibetanText(dict.getTibetanText());
                saved.add(dictionaryRepository.save(existingDict));
            } else {
                saved.add(dictionaryRepository.save(dict));
            }
        }
        return saved;
    }

    /**
     * 初始化常用词典
     */
    @Transactional
    public void initializeDefaultDictionary() {
        // 检查是否已有数据
        if (dictionaryRepository.count() > 0) {
            return; // 已有数据，不重复初始化
        }

        Map<String, String> commonWords = new HashMap<>();
        commonWords.put("景点", "གནས་དམངས་ཆོས་ལུགས");
        commonWords.put("自然", "རང་བྱུང");
        commonWords.put("文化", "རིག་གནས");
        commonWords.put("历史", "ལོ་རྒྱུས");
        commonWords.put("寺庙", "དགོན་པ");
        commonWords.put("湖泊", "མཚོ");
        commonWords.put("雪山", "རི་གནོན་པོ");
        commonWords.put("圣湖", "མཚོ་གནས");
        commonWords.put("宫殿", "ཕོ་བྲང");
        commonWords.put("海拔", "མཐོ་ཚད");
        commonWords.put("门票", "ཐོ་འགོད");
        commonWords.put("介绍", "ངོ་སྤྲོད");
        commonWords.put("描述", "གསལ་བཤད");
        commonWords.put("适合", "མཐུན");
        commonWords.put("拍摄", "རི་མོ");
        commonWords.put("旅游", "སྤྱོད་པ");
        commonWords.put("观光", "ལྟ་བ");
        commonWords.put("朝圣", "སྐྱབས་འགྲོ");
        commonWords.put("徒步", "སྤྱོད་པའི་ལམ");
        commonWords.put("摄影", "རི་མོ་བཟོ");
        commonWords.put("观景", "གནས་ལྟ");
        commonWords.put("清晨", "ཞོགས་པ");
        commonWords.put("黄昏", "དགོངས་པ");
        commonWords.put("阳光", "ཉི་མའི་འོད");
        commonWords.put("高原", "མཐོ་སྒང");
        commonWords.put("空气", "རླུང");
        commonWords.put("色彩", "ཁ་དོག");
        commonWords.put("景区", "གནས་ས");
        commonWords.put("补给", "རྒྱུན་མཐུན");
        commonWords.put("观景平台", "གནས་ལྟ་སྒྲིག");
        commonWords.put("路线", "ལམ་ཐོག");
        commonWords.put("旅人", "སྤྱོད་པ");
        commonWords.put("呼吸", "དབུགས");
        commonWords.put("适应", "མཐུན");
        commonWords.put("海拔变化", "མཐོ་ཚད་འགྱུར");
        commonWords.put("建议", "བསམ་བློ");
        commonWords.put("预订", "ཐོ་འགོད");
        commonWords.put("门票", "ཐོ་འགོད");
        commonWords.put("向导", "ལམ་ཐོག");
        commonWords.put("准备", "སྒྲིག");
        commonWords.put("保暖", "དྲོད");
        commonWords.put("衣物", "གོས");
        commonWords.put("防晒", "ཉི་མ་སྲུང");
        commonWords.put("能量", "སྟོབས");
        commonWords.put("补给", "རྒྱུན་མཐུན");
        commonWords.put("尊重", "བརྩི");
        commonWords.put("生态", "རང་བྱུང");
        commonWords.put("宗教", "ཆོས་ལུགས");
        commonWords.put("礼仪", "སྲོལ");
        commonWords.put("旅程", "ལམ་ཐོག");
        commonWords.put("从容", "མཐུན");
        commonWords.put("圆满", "ལེགས");

        batchAddEntries(commonWords, TibetanDictionary.Type.WORD);

        // 添加常用短语
        Map<String, String> commonPhrases = new HashMap<>();
        commonPhrases.put("以...著称", "ལ་གཞི་བཅོལ");
        commonPhrases.put("适合", "ལ་མཐུན");
        commonPhrases.put("建议", "བསམ་བློ་བཏང");
        commonPhrases.put("提前", "ཞོགས་པ་ནས");
        commonPhrases.put("准备", "སྒྲིག་པ");
        commonPhrases.put("尊重", "བརྩི་པ");
        commonPhrases.put("世界海拔最高", "འཛམ་གླིང་གི་མཐོ་ཚད་ཆེས་མཐོ");
        commonPhrases.put("藏传佛教", "བོད་ཀྱི་རིག་གནས");
        commonPhrases.put("三大圣湖", "གནས་ས་རྙེད་པ་གསུམ");
        commonPhrases.put("黄金时刻", "དུས་ཚོད་གསེར");
        commonPhrases.put("电影感", "རི་མོ་ཚོར");

        batchAddEntries(commonPhrases, TibetanDictionary.Type.PHRASE);
    }
}


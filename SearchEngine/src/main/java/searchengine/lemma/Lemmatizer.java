package searchengine.lemma;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.*;

@Slf4j
@AllArgsConstructor
@Component
public class Lemmatizer {
    private static final LuceneMorphology RUS_LUCENE_MORPHOLOGY;
    private static final LuceneMorphology ENG_LUCENE_MORPHOLOGY;
    private static final String WORD_TYPE_REGEX;
    private static final String[] PARTICLES_NAMES;
    private static final int MAX_WORD_LENGTH;

    static {
        WORD_TYPE_REGEX = "\\W\\w&&[^а-яА-Я\\s]";
        PARTICLES_NAMES = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ", "ЧАСТ"};
        MAX_WORD_LENGTH = 40;
        try {
            RUS_LUCENE_MORPHOLOGY = new RussianLuceneMorphology();
            ENG_LUCENE_MORPHOLOGY = new EnglishLuceneMorphology();
        } catch (IOException e) {
            log.error("Error during morphology initialization");
            throw new RuntimeException(e);
        }
    }

    public Map<String, Integer> lemmatizeText(String text) {
        String[] rusWords = filterWordsByRegex(text, "[^а-я\\s]");
        String[] engWords = filterWordsByRegex(text, "[^a-z\\s]");
        HashMap<String, Integer> lemmas = new HashMap<>();

        makeLemmas(rusWords, lemmas, RUS_LUCENE_MORPHOLOGY);
        makeLemmas(engWords, lemmas, ENG_LUCENE_MORPHOLOGY);

        return lemmas;
    }

    private void makeLemmas(String[] words, HashMap<String, Integer> lemmas, LuceneMorphology luceneMorphology) {
        for(String word : words) {
            if(word.length() > MAX_WORD_LENGTH) {
                continue;
            }
            if(word.isBlank()) {
                continue;
            }
            if(!isCorrectWordForm(word, luceneMorphology)) {
                continue;
            }

            List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
            if(isWordBaseParticle(wordBaseForms)) {
                continue;
            }

            List<String> normalForms = luceneMorphology.getNormalForms(word);
            if(normalForms.isEmpty()) {
                continue;
            }

            String normalWord = normalForms.get(0);

            if(lemmas.containsKey(normalWord)) {
                lemmas.put(normalWord, lemmas.get(normalWord) + 1);
            } else {
                lemmas.put(normalWord, 1);
            }
        }
    }

    public String[] filterWordsByRegex(String text, String regex) {
        return text.toLowerCase()
                .replaceAll(regex, "")
                .trim()
                .split("\\s+");
    }

    private boolean isWordBaseParticle(List<String> wordBaseForms) {
        return wordBaseForms.stream().allMatch(this::hasParticleProperty);
    }

    private boolean hasParticleProperty(String wordBase) {
        for(String property : PARTICLES_NAMES) {
            if(wordBase.toUpperCase().contains(property)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCorrectWordForm(String word, LuceneMorphology luceneMorphology) {
        List<String> wordInfo = luceneMorphology.getMorphInfo(word);
        for(String morphInfo : wordInfo) {
            if(morphInfo.matches(WORD_TYPE_REGEX)) {
                return false;
            }
        }
        return true;
    }

    public String getNormalForm(String word) {
        String rusWord = word.replaceAll("[^А-Яа-я]", "").toLowerCase();
        String engWord = word.replaceAll("[^A-Za-z]", "").toLowerCase();

        if(!rusWord.isBlank()) {
            return RUS_LUCENE_MORPHOLOGY.getNormalForms(rusWord).get(0);
        } else if (!engWord.isBlank()) {
            return ENG_LUCENE_MORPHOLOGY.getNormalForms(engWord).get(0);
        }
        return word;
    }
}

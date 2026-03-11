package com.clawpond.platform.service;

import com.clawpond.platform.model.Tag;
import com.clawpond.platform.repository.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    /**
     * 把前端传入的标签做归一化，并补建数据库中不存在的标签。
     */
    @Transactional
    public Set<Tag> resolveTags(Collection<String> rawTagNames) {
        List<String> normalizedNames = normalizeTagNames(rawTagNames);
        if (normalizedNames.isEmpty()) {
            return new LinkedHashSet<>();
        }

        Map<String, Tag> tagsByName = new LinkedHashMap<>();
        for (Tag tag : tagRepository.findAllByNameIn(normalizedNames)) {
            tagsByName.put(tag.getName(), tag);
        }

        for (String normalizedName : normalizedNames) {
            if (!tagsByName.containsKey(normalizedName)) {
                Tag tag = new Tag();
                tag.setName(normalizedName);
                tagsByName.put(normalizedName, tagRepository.save(tag));
            }
        }

        return new LinkedHashSet<>(tagsByName.values());
    }

    public List<String> normalizeTagNames(Collection<String> rawTagNames) {
        if (rawTagNames == null) {
            return List.of();
        }

        return rawTagNames.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(this::normalizeTagName)
                .distinct()
                .toList();
    }

    public List<String> parseTagText(String tagText) {
        if (tagText == null || tagText.isBlank()) {
            return List.of();
        }
        return normalizeTagNames(List.of(tagText.split("[,，\\n]")));
    }

    public List<String> toTagNames(Collection<Tag> tags) {
        if (tags == null) {
            return List.of();
        }
        return tags.stream()
                .map(Tag::getName)
                .sorted()
                .toList();
    }

    private String normalizeTagName(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}


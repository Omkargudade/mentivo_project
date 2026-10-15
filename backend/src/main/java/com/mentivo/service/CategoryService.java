package com.mentivo.service;

import com.mentivo.dto.category.CategoryDto;
import com.mentivo.dto.category.SkillDto;
import com.mentivo.dto.category.TopicDto;
import com.mentivo.entity.Category;
import com.mentivo.entity.Skill;
import com.mentivo.entity.Topic;
import com.mentivo.exception.BadRequestException;
import com.mentivo.exception.ResourceNotFoundException;
import com.mentivo.repository.CategoryRepository;
import com.mentivo.repository.SkillRepository;
import com.mentivo.repository.TopicRepository;
import com.mentivo.util.DtoMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final SkillRepository skillRepository;
    private final TopicRepository topicRepository;

    public CategoryService(CategoryRepository categoryRepository,
                           SkillRepository skillRepository,
                           TopicRepository topicRepository) {
        this.categoryRepository = categoryRepository;
        this.skillRepository = skillRepository;
        this.topicRepository = topicRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(DtoMapper::toCategoryDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryDto getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with slug: " + slug));
        return DtoMapper.toCategoryDto(category);
    }

    @Transactional(readOnly = true)
    public List<SkillDto> getAllSkills() {
        return skillRepository.findAll().stream()
                .map(DtoMapper::toSkillDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TopicDto> getAllTopics() {
        return topicRepository.findAll().stream()
                .map(DtoMapper::toTopicDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoryDto createCategory(CategoryDto dto) {
        if (categoryRepository.existsBySlug(dto.getSlug())) {
            throw new BadRequestException("Category slug already exists: " + dto.getSlug());
        }
        Category category = new Category(dto.getName(), dto.getSlug(), dto.getIconName(), dto.getDescription());
        Category saved = categoryRepository.save(category);
        return DtoMapper.toCategoryDto(saved);
    }

    @Transactional
    public SkillDto createSkill(SkillDto dto) {
        Category category = null;
        if (dto.getCategoryId() != null) {
            category = categoryRepository.findById(dto.getCategoryId()).orElse(null);
        }
        Skill skill = new Skill(dto.getName(), category);
        Skill saved = skillRepository.save(skill);
        return DtoMapper.toSkillDto(saved);
    }

    @Transactional
    public TopicDto createTopic(TopicDto dto) {
        Category category = null;
        if (dto.getCategoryId() != null) {
            category = categoryRepository.findById(dto.getCategoryId()).orElse(null);
        }
        Topic topic = new Topic(dto.getName(), dto.getSlug(), category);
        Topic saved = topicRepository.save(topic);
        return DtoMapper.toTopicDto(saved);
    }
}

package com.mentivo.dto.category;

import java.util.List;

public class CategoryDto {
    private Long id;
    private String name;
    private String slug;
    private String iconName;
    private String description;
    private List<TopicDto> topics;
    private List<SkillDto> skills;

    public CategoryDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getIconName() { return iconName; }
    public void setIconName(String iconName) { this.iconName = iconName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<TopicDto> getTopics() { return topics; }
    public void setTopics(List<TopicDto> topics) { this.topics = topics; }

    public List<SkillDto> getSkills() { return skills; }
    public void setSkills(List<SkillDto> skills) { this.skills = skills; }
}

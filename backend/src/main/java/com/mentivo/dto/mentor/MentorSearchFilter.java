package com.mentivo.dto.mentor;

import com.mentivo.entity.enums.OnlineStatus;
import java.math.BigDecimal;

public class MentorSearchFilter {

    private String query;
    private Long categoryId;
    private Long topicId;
    private Long skillId;
    private String language;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Double minRating;
    private Integer minExperience;
    private Boolean freeOnly;
    private OnlineStatus onlineStatus; // ONLINE to filter available now
    private String sortBy = "relevance"; // relevance, rating, price_asc, price_desc, experience
    private int page = 0;
    private int size = 12;

    public MentorSearchFilter() {}

    public Boolean getFreeOnly() { return freeOnly; }
    public void setFreeOnly(Boolean freeOnly) { this.freeOnly = freeOnly; }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public Long getTopicId() { return topicId; }
    public void setTopicId(Long topicId) { this.topicId = topicId; }

    public Long getSkillId() { return skillId; }
    public void setSkillId(Long skillId) { this.skillId = skillId; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }

    public BigDecimal getMaxPrice() { return maxPrice; }
    public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }

    public Double getMinRating() { return minRating; }
    public void setMinRating(Double minRating) { this.minRating = minRating; }

    public Integer getMinExperience() { return minExperience; }
    public void setMinExperience(Integer minExperience) { this.minExperience = minExperience; }

    public OnlineStatus getOnlineStatus() { return onlineStatus; }
    public void setOnlineStatus(OnlineStatus onlineStatus) { this.onlineStatus = onlineStatus; }

    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}

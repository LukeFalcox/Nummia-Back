package com.devfalco.nummia.category.dto;

import com.devfalco.nummia.category.Category;
import com.devfalco.nummia.category.CategoryType;

import java.util.UUID;

public record CategoryResponse(UUID id, String name, CategoryType type, String color, String icon, boolean archived) {
    public static CategoryResponse from(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getType(), c.getColor(), c.getIcon(), c.isArchived());
    }
}

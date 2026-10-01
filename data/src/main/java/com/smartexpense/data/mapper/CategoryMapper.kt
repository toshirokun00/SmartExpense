package com.smartexpense.data.mapper

import com.smartexpense.database.entity.CategoryEntity
import com.smartexpense.domain.model.Category

fun CategoryEntity.toDomain() : Category {
    return Category(
        id = id,
        name = name
    )
}

fun Category.toEntity() : CategoryEntity {
    return CategoryEntity(
        id = id,
        name = name
    )
}
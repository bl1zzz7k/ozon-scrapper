package ru.mpstat;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isEmpty;

@Data
@NoArgsConstructor
public class Category {
    private String name;
    private List<Category> subCategories = new ArrayList<>();

    public Category(String name) {
        this.name = name;
    }

    public void addCategory(Category category) {
        subCategories.add(category);
    }

    public List<String> getCategoriesTrees(String parentCategory) {
        if (subCategories.isEmpty()) {
            return List.of(parentCategory + (isEmpty(parentCategory) ? "/" + name : ""));
        }
        return subCategories.stream().map(category -> {
                    String parent = (isEmpty(parentCategory) ? name + "/" : parentCategory + "/") + category.getName();
                    return category.getCategoriesTrees(parent);
                }).flatMap(Collection::stream)
                .collect(Collectors.toList());
    }
}

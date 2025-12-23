package ch.unisg_group1.mealplanner.presentation;

import ch.unisg_group1.mealplanner.model.ShoppingListItem;
import ch.unisg_group1.mealplanner.service.MealPlannerService;
import org.vaadin.crudui.crud.CrudListener;
import java.util.List;

public class ShoppingListCRUDListener implements CrudListener<ShoppingListItem> {
    private final MealPlannerService service;

    public ShoppingListCRUDListener(MealPlannerService service) {
        this.service = service;
    }

    @Override
    public java.util.List<ShoppingListItem> findAll() {
        return java.util.Collections.emptyList(); // default empty, will populate via generateBtn
    }

    @Override
    public ShoppingListItem add(ShoppingListItem item) {
        return item; // No add
    }

    @Override
    public ShoppingListItem update(ShoppingListItem item) {
        return item; // Optionally persist "checked" status
    }

    @Override
    public void delete(ShoppingListItem item) {
        // optionally allow deletion
    }
}

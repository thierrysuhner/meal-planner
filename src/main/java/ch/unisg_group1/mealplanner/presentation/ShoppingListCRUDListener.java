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
        return service.getAllShoppingListItems();
    }

    @Override
    public ShoppingListItem add(ShoppingListItem item) {
        return service.saveShoppingListItem(item);
    }

    @Override
    public ShoppingListItem update(ShoppingListItem item) {
        return service.saveShoppingListItem(item);
    }

    @Override
    public void delete(ShoppingListItem item) {
        service.deleteShoppingListItem(item);
    }
}

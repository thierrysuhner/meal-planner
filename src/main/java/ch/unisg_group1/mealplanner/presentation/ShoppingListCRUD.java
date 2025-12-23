package ch.unisg_group1.mealplanner.presentation;

import ch.unisg_group1.mealplanner.model.ShoppingListItem;
import ch.unisg_group1.mealplanner.service.MealPlannerService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import org.vaadin.crudui.crud.impl.GridCrud;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;


@Route(value = "shoppinglists", layout = MainView.class)
public class ShoppingListCRUD extends VerticalLayout {
    private final MealPlannerService service;

    @Autowired
    public ShoppingListCRUD(MealPlannerService service) {
        this.service = service;

        GridCrud<ShoppingListItem> crud = new GridCrud<>(ShoppingListItem.class);

        // Generate list button
        //Button generateBtn = new Button("Generate Shopping List for Today", e -> {
        //    var list = service.generateMealShoppingList();
        //    crud.getGrid().setItems(list.getItems());
        //});

        // Set CRUD listener for editing list items
        crud.setCrudListener(new ShoppingListCRUDListener(service));

        crud.getGrid().setColumns("name", "totalAmount", "unit", "checked");

        add(crud);
        setSizeFull();
    }
}

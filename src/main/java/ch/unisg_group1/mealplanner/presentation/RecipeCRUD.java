package ch.unisg_group1.mealplanner.presentation;

import ch.unisg_group1.mealplanner.model.Recipe;
import ch.unisg_group1.mealplanner.service.MealPlannerService;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.crudui.crud.impl.GridCrud;


@Route(value = "recipes", layout = MainView.class)
public class RecipeCRUD extends VerticalLayout {
    private final MealPlannerService service;

    @Autowired
    public RecipeCRUD(MealPlannerService service) {
        this.service = service;

        // Create GridCRUD
        GridCrud<Recipe> crud = new GridCrud<>(Recipe.class);
        crud.setCrudListener(new RecipeCRUDListener(service));

        crud.getGrid().setColumns("id", "name", "description", "portions");
        crud.getGrid().getColumnByKey("id").setWidth("6em").setFlexGrow(0);

        add(crud);
        setSizeFull();
    }
}

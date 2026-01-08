package ch.unisg_group1.mealplanner.presentation;

import ch.unisg_group1.mealplanner.model.ShoppingListItem;
import ch.unisg_group1.mealplanner.service.MealPlannerService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import org.vaadin.crudui.crud.impl.GridCrud;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;


@Route(value = "shoppinglists", layout = MainView.class)
public class ShoppingListCRUD extends VerticalLayout {
    private final MealPlannerService service;

    public ShoppingListCRUD(MealPlannerService service) {
        this.service = service;
        GridCrud<ShoppingListItem> crud = new GridCrud<>(ShoppingListItem.class);

        HorizontalLayout header = new HorizontalLayout();
        header.setAlignItems(Alignment.CENTER);
        header.add(VaadinIcon.CART.create());
        header.add(new H2("Shopping Lists"));

        // 1. Controls erstellen
        DatePicker startPicker = new DatePicker("From", LocalDate.now());
        DatePicker endPicker = new DatePicker("To", LocalDate.now().plusDays(7));

        Button generateBtn = new Button("Generate List", e -> {
            if (startPicker.getValue() != null && endPicker.getValue() != null) {
                // Generierung im Service anstoßen
                service.generateShoppingListFromMeals(startPicker.getValue(), endPicker.getValue());

                // DAS WICHTIGSTE: Das Grid anweisen, findAll() vom Listener neu aufzurufen
                crud.refreshGrid();

                Notification.show("Shopping List Updated!");
            } else {
                Notification.show("Please choose start and end date!");
            }
        });
        generateBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button clearBtn = new Button("Empty List", e -> {
            service.clearShoppingList();
            crud.refreshGrid();
            Notification.show("List has been emptied");
        });
        clearBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout controls = new HorizontalLayout(startPicker, endPicker, generateBtn, clearBtn);
        controls.setDefaultVerticalComponentAlignment(Alignment.BASELINE);

        // 2. CRUD Grid
        crud.setCrudListener(new ShoppingListCRUDListener(service));
        crud.getGrid().setColumns("name");
        crud.getGrid().addColumn(item ->
                        String.format("%.2f", item.getTotalAmount()))
                        .setHeader("Amount")
                        .setKey("totalAmount");
        crud.getGrid().addColumn("unit");
        crud.getCrudFormFactory().setVisibleProperties("name", "totalAmount", "unit");


        add(header, controls, crud);
        setSizeFull();
    }

    private void generateItems(LocalDate start, LocalDate end) {
        if (start == null || end == null) return;
        service.generateShoppingListFromMeals(start, end);
    }
}

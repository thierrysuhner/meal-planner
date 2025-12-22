package ch.unisg_group1.mealplanner.presentation;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("")
public class MainView extends VerticalLayout {
    public MainView() {
        add(new H1("Meal Planner"));
        add(new Button("Welcome to the Meal Planner"));
    }

}

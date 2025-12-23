package ch.unisg_group1.mealplanner.presentation;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.router.RouterLayout;


@Route("")
public class MainView extends AppLayout implements RouterLayout {
    private Div content;

    public MainView() {
        // Top bar
        H1 title = new H1("Meal Planner & Grocery Manager");
        title.getStyle().set("font-size", "1.5em").set("padding-left","1rem");
        addToNavbar(title);

        // Sidebar with tabs
        VerticalLayout sidebar = new VerticalLayout();
        sidebar.setPadding(false);
        sidebar.setSpacing(false);

        Tabs tabs = new Tabs();
        tabs.setOrientation(Tabs.Orientation.VERTICAL);

        // Tab links
        Tab recipeTab = new Tab(new RouterLink("Recipes", RecipeCRUD.class));
        Tab shoppingTab = new Tab(new RouterLink("Shopping Lists", ShoppingListCRUD.class));
        Tab mealTab = new Tab(new RouterLink("Plan Meals", MealPlaner.class));

        tabs.add(recipeTab,shoppingTab,mealTab);
        sidebar.add(tabs);
        addToDrawer(sidebar);

        // Content container
        content = new Div();
        content.setSizeFull();
        setContent(content);

        // Welcome message
        H2 welcome = new H2("Welcome to Your Meal Planner & Grocery Manager!");
        welcome.getStyle().set("margin", "0 auto");
        content.add(welcome);
    }

}

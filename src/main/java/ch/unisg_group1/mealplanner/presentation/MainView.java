package ch.unisg_group1.mealplanner.presentation;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.router.RouterLayout;

@Route("")
public class MainView extends AppLayout implements RouterLayout {

    public MainView() {
        // Own logo
        Image logo = new Image("images/logo.png", "Meal Planner Logo");
        logo.setHeight("40px"); // Größe an die Navbar anpassen
        logo.getStyle().set("margin-left", "1rem");

        // Title
        H1 title = new H1("Meal Planner & Grocery Manager");
        title.getStyle().set("font-size", "1.5em")
                .set("margin", "0")
                .set("padding-left", "0.5rem");

        // Navbar
        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), logo, title);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();

        addToNavbar(header);

        // Sidebar with tabs
        VerticalLayout sidebar = new VerticalLayout();
        sidebar.setPadding(false);
        sidebar.setSpacing(false);

        Tabs tabs = new Tabs();
        tabs.setOrientation(Tabs.Orientation.VERTICAL);

        // Tab links
        Tab recipeTab = createTab(VaadinIcon.CUTLERY, "Recipes", RecipeCRUD.class);
        Tab mealTab = createTab(VaadinIcon.CALENDAR, "Plan Meals", MealPlaner.class);
        Tab shoppingTab = createTab(VaadinIcon.CART, "Shopping Lists", ShoppingListCRUD.class);

        tabs.add(recipeTab,mealTab,shoppingTab);
        tabs.setSelectedIndex(-1);
        sidebar.add(tabs);
        addToDrawer(sidebar);

        // Content container
        Div content = new Div();
        content.setSizeFull();
        content.getStyle()
                .set("display", "flex")
                .set("flex-direction","column")
                .set("justify-content", "center")
                .set("align-items", "center")
                .set("gap", "2rem")
                .set("text-align", "center");
        setContent(content);

        // Welcome message
        H2 welcome = new H2("Welcome to Your Meal Planner & Grocery Manager!");
        welcome.getStyle().set("margin", "0");

        Image big_logo = new Image("images/logo.png", "Meal Planner Logo");
        big_logo.setHeight("10em");
        content.add(welcome, big_logo);
    }

    private Tab createTab(VaadinIcon icon, String title, Class<? extends Component> viewClass) {
        Icon i = icon.create();
        i.getStyle().set("box-sizing", "border-box")
                .set("margin-inline-end", "var(--lumo-space-m)")
                .set("padding", "var(--lumo-space-xs)");

        RouterLink link = new RouterLink();
        link.add(i, new Span(title));
        link.setRoute(viewClass);

        return new Tab(link);
    }

}

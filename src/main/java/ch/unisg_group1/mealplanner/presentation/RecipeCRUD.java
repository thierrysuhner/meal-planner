package ch.unisg_group1.mealplanner.presentation;

import ch.unisg_group1.mealplanner.model.Ingredient;
import ch.unisg_group1.mealplanner.model.Recipe;
import ch.unisg_group1.mealplanner.service.MealPlannerService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.crudui.crud.impl.GridCrud;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Route(value = "recipes", layout = MainView.class)
public class RecipeCRUD extends VerticalLayout {

    private final MealPlannerService service;
    private GridCrud<Recipe> crud;

    private Recipe currentRecipe;
    private TextField recipeName = new TextField("Recipe Name");
    private IntegerField portions = new IntegerField("Portions");
    private VerticalLayout ingredientsLayout = new VerticalLayout();
    private VerticalLayout editorContainer = new VerticalLayout();

    @Autowired
    public RecipeCRUD(MealPlannerService service) {
        this.service = service;

        HorizontalLayout header = new HorizontalLayout();
        header.setAlignItems(Alignment.CENTER);
        header.add(VaadinIcon.CUTLERY.create()); // Passendes Besteck-Icon
        header.add(new H2("Recipes"));

        // 1. GridCrud Konfiguration (Standard Buttons aktiv lassen)
        crud = new GridCrud<>(Recipe.class);
        crud.setCrudListener(new RecipeCRUDListener(service));
        crud.getGrid().setColumns("name", "description", "portions", "calories");
        crud.getCrudFormFactory().setVisibleProperties("name", "description");
        crud.getGrid().getColumnByKey("calories").setHeader("Calories (per portion)");

        crud.getGrid().addSelectionListener(event -> {
            event.getFirstSelectedItem().ifPresentOrElse(
                    recipe -> {
                        Recipe fullRecipe = service.fetchRecipeWithIngredients(recipe.getId());
                        showEditor(fullRecipe);
                    },
                    () -> hideEditor()
            );
        });

        // Sicherstellen, dass das Grid die Klicks nicht für Zeilen-Editing reserviert
        crud.getGrid().setSelectionMode(Grid.SelectionMode.SINGLE);

        setupEditorView();

        add(header, crud, editorContainer);
        setSizeFull();

        Hr divider = new Hr();
        add(divider);

        VerticalLayout finderContainer = new VerticalLayout();
        finderContainer.setPadding(true);
        H3 finderTitle = new H3("Recipe Finder");
        MultiSelectComboBox<String> inventoryPicker = new MultiSelectComboBox<>("My Ingredients");
        inventoryPicker.setItems(service.getAllAvailableIngredientNames());
        inventoryPicker.setPlaceholder("Choose Ingredients...");
        inventoryPicker.setWidthFull();

        Grid<Recipe> resultGrid = new Grid<>(Recipe.class, false);
        resultGrid.addColumn(Recipe::getName).setHeader("Recipe");
        resultGrid.addColumn(r -> r.getCalories() + " kcal").setHeader("Calories (per portion)");
        resultGrid.setAllRowsVisible(true); // Das Grid wächst mit der Anzahl der Zeilen

        // 2. Suche triggern
        inventoryPicker.addValueChangeListener(e -> {
            List<Recipe> matches = service.findRecipesMatchingIngredients(e.getValue())
                    .stream()
                    .map(r -> service.fetchRecipeWithIngredients(r.getId()))
                    .toList();
            resultGrid.setItems(matches);
        });

        // Die Spalte für fehlende Zutaten
        resultGrid.addComponentColumn(recipe -> {
            // 1. Berechne die Liste der fehlenden Zutaten
            Set<String> ownedLower = inventoryPicker.getValue().stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());

            List<Ingredient> missing = recipe.getIngredients().stream()
                    .filter(ing -> !ownedLower.contains(ing.getName().toLowerCase()))
                    .toList();

            // 2. Erstelle den Text: "Zutat (Menge Unit), Zutat2 (...)"
            String missingText = missing.stream()
                    .map(ing -> String.format("%s (%.1f %s)",
                            ing.getName(), ing.getAmount(), ing.getUnit()))
                    .collect(Collectors.joining(", "));

            // 3. UI Komponenten zusammenstellen
            VerticalLayout layout = new VerticalLayout();
            layout.setPadding(false);
            layout.setSpacing(false);

            if (missing.isEmpty()) {
                Span allSet = new Span("Everything at Hand!");
                allSet.getElement().getStyle().set("color", "var(--lumo-success-text-color)");
                layout.add(allSet);
            } else {
                Span missingSpan = new Span("Missing: " + missingText);
                missingSpan.getElement().getStyle().set("font-size", "var(--lumo-font-size-s)");
                missingSpan.getElement().getStyle().set("color", "var(--lumo-error-text-color)");
                layout.add(missingSpan);
            }

            return layout;
        }).setHeader("Missing Ingredients").setFlexGrow(2);

        Div footerSpacer = new Div();
        footerSpacer.setHeight("5em");
        footerSpacer.getStyle().set("flex-shrink", "0");
        footerSpacer.setWidthFull();

        finderContainer.add(finderTitle,inventoryPicker,resultGrid);
        resultGrid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
        add(finderContainer, inventoryPicker, resultGrid, footerSpacer);
    }

    private void setupEditorView() {
        editorContainer.setVisible(false);
        editorContainer.setPadding(true);
        editorContainer.getStyle().set("background-color", "#f9f9f9");
        editorContainer.getStyle().set("border-top", "2px solid #ddd");

        // Form für Basisdaten
        recipeName.setReadOnly(true); // Nur zur Info im Detail-View
        FormLayout form = new FormLayout(recipeName, portions);

        // Bereich für Zutaten
        ingredientsLayout.setPadding(false);
        ingredientsLayout.setSpacing(false);

        Button addIngredientBtn = new Button("Add Ingredient", VaadinIcon.PLUS.create(), e -> {
            addIngredientRow(new Ingredient());
        });

        Button saveDetailsBtn = new Button("Save Ingredients & Recipe", e -> saveDetails());
        saveDetailsBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        editorContainer.add(new H3("Edit Ingredients"), form, ingredientsLayout, addIngredientBtn, saveDetailsBtn);
    }

    private void showEditor(Recipe recipe) {
        this.currentRecipe = recipe;
        recipeName.setValue(recipe.getName() != null ? recipe.getName() : "");
        portions.setValue(recipe.getPortions());

        ingredientsLayout.removeAll();
        if (recipe.getIngredients() != null) {
            recipe.getIngredients().forEach(this::addIngredientRow);
        }

        editorContainer.setVisible(true);
    }

    private void hideEditor() {
        this.currentRecipe = null;
        editorContainer.setVisible(false);
        ingredientsLayout.removeAll();
    }

    private void addIngredientRow(Ingredient ing) {
        TextField name = new TextField();
        name.setPlaceholder("Ingredient Name");
        name.setValue(ing.getName() != null ? ing.getName() : "");

        NumberField amount = new NumberField();
        amount.setPlaceholder("Amount");
        amount.setValue(ing.getAmount() != 0 ? ing.getAmount() : null);

        TextField unit = new TextField();
        unit.setPlaceholder("Unit (g, ml, pcs)");
        unit.setValue(ing.getUnit() != null ? ing.getUnit() : "");

        IntegerField calField = new IntegerField();
        calField.setPlaceholder("Calories");
        calField.setValue(ing.getCalories() != 0 ? ing.getCalories() : null);
        calField.setWidth("6em");

        Button delete = new Button(VaadinIcon.TRASH.create(), e -> {
            ingredientsLayout.remove((HorizontalLayout) name.getParent().get());
        });
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout row = new HorizontalLayout(name, amount, unit, calField, delete);
        row.setAlignItems(Alignment.BASELINE);
        ingredientsLayout.add(row);
    }

    private void saveDetails() {
        if (currentRecipe == null || currentRecipe.getId() == null) {
            Notification.show("Please first create a recipe and then select it!");
            return;
        }
        if (portions.getValue() <= 0) {
            Notification.show("Portions have to be greater than 0");
            return;
        }

        try {
            List<Ingredient> ingredientsFromUI = new ArrayList<>();

            // Get components from UI
            for (Component component : ingredientsLayout.getChildren().toList()) {
                if (component instanceof HorizontalLayout row) {
                    // Validation
                    if (isRowInvalid(row)) {
                        Notification.show("Please fill in at least Name, Amount and Unit.");
                        return;
                    }

                    Ingredient i = new Ingredient();
                    i.setName(((TextField) row.getComponentAt(0)).getValue());
                    i.setAmount(((NumberField) row.getComponentAt(1)).getValue());
                    i.setUnit(((TextField) row.getComponentAt(2)).getValue());
                    i.setCalories(((IntegerField) row.getComponentAt(3)).getValue() != null ?
                            ((IntegerField) row.getComponentAt(3)).getValue() : 0);

                    ingredientsFromUI.add(i);
                }
            }

            // DER SERVICE-AUFRUF
            Recipe saved = service.updateRecipeDetails(
                    currentRecipe.getId(),
                    portions.getValue(),
                    ingredientsFromUI
            );

            // UI Aktualisierung
            this.currentRecipe = saved;
            crud.refreshGrid();
            Notification.show("Ingredients for '" + saved.getName() + "' saved successfully!");

        } catch (Exception e) {
            Notification.show("Error: " + e.getMessage());
        }
    }

    // Hilfsmethode für die Übersichtlichkeit
    private boolean isRowInvalid(HorizontalLayout row) {
        return ((TextField) row.getComponentAt(0)).getValue() == null ||
                ((NumberField) row.getComponentAt(1)).getValue() == null ||
                ((TextField) row.getComponentAt(2)).getValue().isEmpty();
    }
}
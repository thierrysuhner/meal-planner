package ch.unisg_group1.mealplanner.presentation;

import ch.unisg_group1.mealplanner.model.Ingredient;
import ch.unisg_group1.mealplanner.model.Recipe;
import ch.unisg_group1.mealplanner.service.MealPlannerService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
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

        // 1. GridCrud Konfiguration (Standard Buttons aktiv lassen)
        crud = new GridCrud<>(Recipe.class);
        crud.setCrudListener(new RecipeCRUDListener(service));
        crud.getGrid().setColumns("name", "description", "portions", "calories");
        crud.getCrudFormFactory().setVisibleProperties("name", "description");
        crud.getGrid().getColumnByKey("calories").setHeader("Calories (per portion)");

        crud.getGrid().addItemClickListener(event -> {
            Recipe selected = event.getItem();
            if (selected != null && selected.getId() != null) {
                Recipe fullRecipe = service.fetchRecipeWithIngredients(selected.getId());
                showEditor(fullRecipe);
            }
        });

        // Sicherstellen, dass das Grid die Klicks nicht für Zeilen-Editing reserviert
        crud.getGrid().setSelectionMode(Grid.SelectionMode.SINGLE);

        setupEditorView();

        add(new H3("Recipes"), crud, editorContainer);
        setSizeFull();
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

        try {
            // SCHRITT 1: Hol dir die ID
            Long id = currentRecipe.getId();

            // SCHRITT 2: Lade das Rezept absolut frisch aus der Datenbank (WICHTIG!)
            // Das verhindert den Optimistic Locking Fehler, da wir die neueste Version/Version-ID erhalten
            Recipe recipeToUpdate = service.fetchRecipeWithIngredients(id);

            // SCHRITT 3: Update nur die Felder, die du unten im Formular hast
            recipeToUpdate.setPortions(portions.getValue());

            int totalCalories = 0;
            List<Ingredient> newList = new ArrayList<>();

            for (Component component : ingredientsLayout.getChildren().toList()) {
                if (component instanceof HorizontalLayout row) {
                    Ingredient i = new Ingredient();
                    i.setName(((TextField) row.getComponentAt(0)).getValue());
                    i.setAmount(((NumberField) row.getComponentAt(1)).getValue());
                    i.setUnit(((TextField) row.getComponentAt(2)).getValue());

                    // Kalorien aus dem neuen Feld (Index 3) holen
                    int cals = ((IntegerField) row.getComponentAt(3)).getValue() != null ?
                            ((IntegerField) row.getComponentAt(3)).getValue() : 0;
                    i.setCalories(cals);
                    totalCalories += cals; // Summieren
                    i.setRecipe(recipeToUpdate);
                    newList.add(i);
                }
            }
            totalCalories /= portions.getValue();
            recipeToUpdate.setCalories(totalCalories); // Gesamtsumme im Rezept speichern
            recipeToUpdate.getIngredients().clear();
            recipeToUpdate.getIngredients().addAll(newList);

            // SCHRITT 6: Speichern
            Recipe saved = service.saveRecipe(recipeToUpdate);

            // SCHRITT 7: UI aktualisieren
            this.currentRecipe = saved; // Wichtig: Die neue Version im Speicher halten
            crud.refreshGrid();
            Notification.show("Ingredients for '" + saved.getName() + "' saved successfully!");

        } catch (Exception e) {
            Notification.show("Error while saving the details: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
package ch.unisg_group1.mealplanner.presentation;

import ch.unisg_group1.mealplanner.model.Meal;
import ch.unisg_group1.mealplanner.model.Recipe;
import ch.unisg_group1.mealplanner.service.MealPlannerService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.*;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import elemental.json.Json;
import elemental.json.JsonObject;
import org.vaadin.stefan.fullcalendar.*;
import org.vaadin.stefan.fullcalendar.dataprovider.CallbackEntryProvider;
import org.vaadin.stefan.fullcalendar.dataprovider.EntryProvider;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;

@Route(value = "mealplaner", layout = MainView.class)
public class MealPlaner extends VerticalLayout {

    private final MealPlannerService service;
    private final FullCalendar calendar;
    private final Chart calorieChart; // Das neue Chart-Objekt
    private final DataSeries calorieSeries;

    private LocalDate currentStart = LocalDate.now();
    private LocalDate currentEnd = LocalDate.now().plusDays(7);

    public MealPlaner(MealPlannerService service) {
        this.service = service;
        this.setSizeFull();

        HorizontalLayout header = new HorizontalLayout();
        header.setAlignItems(Alignment.CENTER);
        header.add(VaadinIcon.CALENDAR.create());
        header.add(new H2("Plan Meals"));

        // 1. Kalender mit Selection-Optionen konfigurieren
        JsonObject initialOptions = Json.createObject();
        initialOptions.put("selectable", true);
        initialOptions.put("selectMirror", true);
        initialOptions.put("nowIndicator",true);

        this.calendar = FullCalendarBuilder.create()
                .withInitialOptions(initialOptions)
                .build();
        calendar.changeView(CalendarViewImpl.TIME_GRID_WEEK);
        calendar.setLocale(Locale.ENGLISH);
        calendar.setSizeFull();

        // 2. Data Provider einrichten (Verbindung DB -> Kalender)
        setupDataProvider();

        // 3. Listener für neue Einträge
        calendar.addTimeslotsSelectedListener(event -> {
            Meal newMeal = new Meal();
            newMeal.setStartTime(event.getStart());
            newMeal.setEndTime(event.getEnd());
            openMealEditDialog(newMeal);
        });

        calendar.addEntryClickedListener(event -> {
            // Die ID des Entries entspricht der ID des Meals in der Datenbank
            String mealId = event.getEntry().getId();

            service.findMealById(Long.parseLong(mealId)).ifPresent(meal -> {
                openMealEditDialog(meal);
            });
        });

        // Dieser Listener feuert immer, wenn der Kalender geladen wird oder man blättert
        calendar.addDatesRenderedListener(event -> {
            this.currentStart = LocalDate.from(event.getIntervalStart());;
            this.currentEnd = LocalDate.from(event.getIntervalEnd());;
            updateChart(currentStart, currentEnd);
        });


        // 2. Chart Setup
        this.calorieChart = new Chart(ChartType.LINE);
        this.calorieSeries = new DataSeries("Calories per Day");
        setupChartConfig();

        // Layout: Kalender oben, Chart unten
        add(header, calendar, calorieChart);

        // Chart initial laden
        updateChart(LocalDate.now().minusDays(3), LocalDate.now().plusDays(4));
    }


    private void setupDataProvider() {
        CallbackEntryProvider<Entry> entryProvider = EntryProvider.fromCallbacks(
                query -> {
                    // Kalender fragt nach Daten für Zeitraum X bis Y
                    return service.findMealsInRange(query.getStart(), query.getEnd())
                            .stream()
                            .map(this::mapMealToEntry);
                },
                entryId -> service.findMealById(Long.parseLong(entryId))
                        .map(this::mapMealToEntry)
                        .orElse(null)
        );
        calendar.setEntryProvider(entryProvider);
    }

    private Entry mapMealToEntry(Meal meal) {
        Entry entry = new Entry(String.valueOf(meal.getId()));
        entry.setTitle(meal.getTitle());
        entry.setStart(meal.getStartTime());
        entry.setEnd(meal.getEndTime());
        entry.setColor("dodgerblue");
        return entry;
    }

    private void openMealEditDialog(Meal meal) {
        Dialog dialog = new Dialog();
        dialog.setWidth("50em");

        // Check ob neu oder bestehend (Long id != null)
        boolean isNew = (meal.getId() == null);
        dialog.setHeaderTitle(isNew ? "Plan New Meal" : "Edit Meal");

        // 1. Titel
        TextField titleField = new TextField("Title (optional)");
        titleField.setValue(meal.getTitle() != null ? meal.getTitle() : "");
        titleField.setPlaceholder("e.g. Brunch with Friends");
        titleField.setWidthFull();

        // 2. Rezepte Multi-Select
        MultiSelectComboBox<Recipe> recipePicker = new MultiSelectComboBox<>("Choose Recipes");
        recipePicker.setItems(service.getAllRecipes());
        recipePicker.setItemLabelGenerator(Recipe::getName);
        recipePicker.setPlaceholder("Search for Recipes...");
        recipePicker.setWidthFull();

        // Vorselektieren der bestehenden Rezepte
        if (!isNew && meal.getRecipes() != null) {
            recipePicker.setValue(new HashSet<>(meal.getRecipes()));
        }

        // 3. Personen
        IntegerField personsField = new IntegerField("Amount of People");
        personsField.setValue(meal.getPersons() > 0 ? meal.getPersons() : 2);
        personsField.setStepButtonsVisible(true);
        personsField.setMin(1);

        // Layout zusammenbauen
        VerticalLayout dialogLayout = new VerticalLayout(titleField, recipePicker, personsField);
        dialog.add(dialogLayout);

        // 4. Buttons im Footer

        // ABBRECHEN
        Button cancelButton = new Button("Cancel", i -> dialog.close());
        dialog.getFooter().add(cancelButton);

        // LÖSCHEN (Nur bei bestehenden Meals)
        if (!isNew) {
            Button deleteButton = new Button("Delete", e -> {
                service.deleteMeal(meal);
                calendar.getEntryProvider().refreshAll();
                updateChart(currentStart, currentEnd);
                dialog.close();
                Notification.show("Meal deleted");
            });
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
            // Wir schieben den Löschen-Button nach links (standardmäßig)
            dialog.getFooter().add(deleteButton);
        }

        // SPEICHERN
        Button saveButton = new Button("Save", e -> {
            meal.setTitle(titleField.getValue());
            meal.setPersons(personsField.getValue() != null ? personsField.getValue() : 0);

            // Rezepte vom Picker ins Objekt schieben
            meal.setRecipes(new ArrayList<>(recipePicker.getValue()));

            // Auto-Titel generieren falls Feld leer
            if ((meal.getTitle() == null || meal.getTitle().isEmpty()) && !meal.getRecipes().isEmpty()) {
                String autoTitle = meal.getRecipes().stream()
                        .map(Recipe::getName)
                        .limit(2)
                        .collect(java.util.stream.Collectors.joining(" & "));
                meal.setTitle(autoTitle);
            }

            service.saveMeal(meal);
            calendar.getEntryProvider().refreshAll();
            updateChart(currentStart, currentEnd);
            dialog.close();
            Notification.show(isNew ? "Meal created" : "Meal updated");
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(saveButton);

        dialog.open();
    }

    private void setupChartConfig() {
        Configuration conf = calorieChart.getConfiguration();
        conf.setTitle("Weekly Calorie Overview");
        conf.getxAxis().setType(AxisType.CATEGORY);
        conf.getyAxis().setTitle("Calories");

        PlotOptionsLine plotOptions = new PlotOptionsLine();

        // DataLabels permanent einschalten
        DataLabels labels = new DataLabels(true);
        labels.setFormat("{y} kcal");
        labels.setAllowOverlap(false);

        plotOptions.setDataLabels(labels);
        conf.setPlotOptions(plotOptions);

        conf.addSeries(calorieSeries);
        calorieChart.setHeight("30em");
    }

    private void updateChart(LocalDate start, LocalDate end) {
        // 1. Chart Daten leeren
        calorieSeries.clear();

        // 2. Den Zeitraum Tag für Tag durchlaufen
        // Wir nutzen datesUntil, um sicherzustellen, dass JEDER Tag (auch ohne Meals)
        // im Chart erscheint (verhindert Lücken in der Linie)
        start.datesUntil(end.plusDays(1)).forEach(date -> {

            // Nutzt deine neue fixierte Service-Funktion (Summe Kalorien * Personen)
            double calories = service.calculateCaloriesForDay(date);

            // Dem Chart hinzufügen
            calorieSeries.add(new DataSeriesItem(date.toString(), calories));
        });

        // 3. Chart neu zeichnen (nur die Daten-Config aktualisieren reicht oft aus)
        calorieChart.drawChart();
    }

}

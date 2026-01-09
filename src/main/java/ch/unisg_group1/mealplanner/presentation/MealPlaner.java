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
    private final Chart calorieChart;
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

        // Calendar with selector options
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

        // Initialize Data Provider as connection DB -> calendar
        setupDataProvider();

        // Listener for new entries
        calendar.addTimeslotsSelectedListener(event -> {
            Meal newMeal = new Meal();
            newMeal.setStartTime(event.getStart());
            newMeal.setEndTime(event.getEnd());
            openMealEditDialog(newMeal);
        });

        calendar.addEntryClickedListener(event -> {
            // ID of the entry corresponds to ID of meal in DB
            String mealId = event.getEntry().getId();

            service.findMealById(Long.parseLong(mealId)).ifPresent(meal -> {
                openMealEditDialog(meal);
            });
        });

        calendar.addDatesRenderedListener(event -> {
            this.currentStart = event.getIntervalStart();
            this.currentEnd = event.getIntervalEnd();
            updateChart(currentStart, currentEnd);
        });


        // Chart Setup
        this.calorieChart = new Chart(ChartType.LINE);
        this.calorieSeries = new DataSeries("Calories per Person per Day");
        setupChartConfig();

        add(header, calendar, calorieChart);

        // Load chart initially
        updateChart(LocalDate.now().minusDays(3), LocalDate.now().plusDays(4));
    }


    private void setupDataProvider() {
        CallbackEntryProvider<Entry> entryProvider = EntryProvider.fromCallbacks(
                query -> {
                    // Calendar asks for dates in perdiod X to Y
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

        // Check if new or existing
        boolean isNew = (meal.getId() == null);
        dialog.setHeaderTitle(isNew ? "Plan New Meal" : "Edit Meal");

        // Title
        TextField titleField = new TextField("Title (optional)");
        titleField.setValue(meal.getTitle() != null ? meal.getTitle() : "");
        titleField.setPlaceholder("e.g. Brunch with Friends");
        titleField.setWidthFull();

        // Multi-Select for recipes
        MultiSelectComboBox<Recipe> recipePicker = new MultiSelectComboBox<>("Choose Recipes");
        recipePicker.setItems(service.getAllRecipes());
        recipePicker.setItemLabelGenerator(Recipe::getName);
        recipePicker.setPlaceholder("Search for Recipes...");
        recipePicker.setWidthFull();

        // Preselect of existing recipes
        if (!isNew && meal.getRecipes() != null) {
            recipePicker.setValue(new HashSet<>(meal.getRecipes()));
        }

        // Persons
        IntegerField personsField = new IntegerField("Amount of People");
        personsField.setValue(meal.getPersons() > 0 ? meal.getPersons() : 2);
        personsField.setStepButtonsVisible(true);
        personsField.setMin(1);

        VerticalLayout dialogLayout = new VerticalLayout(titleField, recipePicker, personsField);
        dialog.add(dialogLayout);

        // Buttons in Footer
        Button cancelButton = new Button("Cancel", i -> dialog.close());
        dialog.getFooter().add(cancelButton);

        if (!isNew) {
            Button deleteButton = new Button("Delete", e -> {
                service.deleteMeal(meal);
                calendar.getEntryProvider().refreshAll();
                updateChart(currentStart, currentEnd);
                dialog.close();
                Notification.show("Meal deleted");
            });
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
            dialog.getFooter().add(deleteButton);
        }

        Button saveButton = new Button("Save", e -> {
            meal.setTitle(titleField.getValue());
            meal.setPersons(personsField.getValue() != null ? personsField.getValue() : 0);
            meal.setRecipes(new ArrayList<>(recipePicker.getValue()));

            // Generate titel automatically if field is empty
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
        conf.setTitle("Your Personal Weekly Calorie Overview");
        conf.getxAxis().setType(AxisType.CATEGORY);
        conf.getyAxis().setTitle("Calories");

        PlotOptionsLine plotOptions = new PlotOptionsLine();

        // DataLabels permanent
        DataLabels labels = new DataLabels(true);
        labels.setFormat("{y} kcal");
        labels.setAllowOverlap(false);

        plotOptions.setDataLabels(labels);
        conf.setPlotOptions(plotOptions);

        conf.addSeries(calorieSeries);
        calorieChart.setHeight("30em");
    }

    private void updateChart(LocalDate start, LocalDate end) {
        calorieSeries.clear();

        // Go through time period day by day
        start.datesUntil(end).forEach(date -> {
            double calories = service.calculateCaloriesForDay(date);
            calorieSeries.add(new DataSeriesItem(date.toString(), calories));
        });
        calorieChart.drawChart();
    }

}

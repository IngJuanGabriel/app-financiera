package com.appfinanciera;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));

    private BudgetRepository repository;
    private BudgetData budgetData;

    private TextView summarySalary;
    private TextView summaryFixed;
    private TextView summaryVariable;
    private TextView summaryInstallments;
    private TextView summaryBalance;
    private TextView summaryNextMonth;
    private TextView reportHeadline;
    private TextView reportLegend;
    private TextView reportRatio;
    private TextView reportLargest;
    private TextView reportUpcoming;

    private View barFixed;
    private View barVariable;
    private View barInstallments;
    private View barAvailable;

    private LinearLayout fixedContainer;
    private LinearLayout variableContainer;
    private LinearLayout installmentContainer;
    private LinearLayout historyContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repository = new BudgetRepository(this);
        budgetData = repository.load();

        bindViews();
        bindActions();
        render();
    }

    private void bindViews() {
        summarySalary = findViewById(R.id.summarySalary);
        summaryFixed = findViewById(R.id.summaryFixed);
        summaryVariable = findViewById(R.id.summaryVariable);
        summaryInstallments = findViewById(R.id.summaryInstallments);
        summaryBalance = findViewById(R.id.summaryBalance);
        summaryNextMonth = findViewById(R.id.summaryNextMonth);
        reportHeadline = findViewById(R.id.reportHeadline);
        reportLegend = findViewById(R.id.reportLegend);
        reportRatio = findViewById(R.id.reportRatio);
        reportLargest = findViewById(R.id.reportLargest);
        reportUpcoming = findViewById(R.id.reportUpcoming);

        barFixed = findViewById(R.id.barFixed);
        barVariable = findViewById(R.id.barVariable);
        barInstallments = findViewById(R.id.barInstallments);
        barAvailable = findViewById(R.id.barAvailable);

        fixedContainer = findViewById(R.id.fixedContainer);
        variableContainer = findViewById(R.id.variableContainer);
        installmentContainer = findViewById(R.id.installmentContainer);
        historyContainer = findViewById(R.id.historyContainer);
    }

    private void bindActions() {
        Button btnSetSalary = findViewById(R.id.btnSetSalary);
        Button btnAddFixed = findViewById(R.id.btnAddFixed);
        Button btnAddVariable = findViewById(R.id.btnAddVariable);
        Button btnAddInstallment = findViewById(R.id.btnAddInstallment);
        Button btnExportReport = findViewById(R.id.btnExportReport);
        Button btnShareReport = findViewById(R.id.btnShareReport);
        Button btnSaveMonth = findViewById(R.id.btnSaveMonth);

        btnSetSalary.setOnClickListener(v -> showSalaryDialog());
        btnAddFixed.setOnClickListener(v -> showExpenseDialog(true));
        btnAddVariable.setOnClickListener(v -> showExpenseDialog(false));
        btnAddInstallment.setOnClickListener(v -> showInstallmentDialog());
        btnExportReport.setOnClickListener(v -> exportReportToFile());
        btnShareReport.setOnClickListener(v -> shareReport());
        btnSaveMonth.setOnClickListener(v -> saveMonthlySnapshot());
    }

    private void render() {
        long fixedTotal = sumExpenses(budgetData.fixedExpenses);
        long variableTotal = sumExpenses(budgetData.variableExpenses);
        long installmentsCurrent = sumCurrentInstallments();
        long nextMonthInstallments = sumNextMonthInstallments();
        long balance = budgetData.salary - fixedTotal - variableTotal - installmentsCurrent;
        long nextMonthProjection = fixedTotal + variableTotal + nextMonthInstallments;
        long spent = fixedTotal + variableTotal + installmentsCurrent;

        summarySalary.setText(formatCurrency(budgetData.salary));
        summaryFixed.setText(formatCurrency(fixedTotal));
        summaryVariable.setText(formatCurrency(variableTotal));
        summaryInstallments.setText(formatCurrency(installmentsCurrent));
        summaryBalance.setText("Te quedan " + formatCurrency(balance));
        summaryNextMonth.setText("Proximo mes comprometido: " + formatCurrency(nextMonthProjection));

        int activeInstallments = countContinuingInstallments();
        boolean overspent = balance < 0;
        reportHeadline.setText(overspent
                ? "Atencion: este mes estas por encima de tu sueldo."
                : activeInstallments > 0
                    ? "Tenes " + activeInstallments + " cuota(s) que siguen el mes que viene."
                    : "No quedan cuotas pendientes para el proximo mes.");
        reportLegend.setText(
                "Fijos " + formatCurrency(fixedTotal)
                        + " | Variables " + formatCurrency(variableTotal)
                        + " | Cuotas " + formatCurrency(installmentsCurrent)
                        + " | Disponible " + formatCurrency(Math.max(balance, 0L))
        );
        reportRatio.setText(buildRatioText(fixedTotal, variableTotal, installmentsCurrent, spent));
        reportLargest.setText(buildLargestExpensesText());
        reportUpcoming.setText(buildUpcomingText(nextMonthInstallments, nextMonthProjection));

        updateBarWeights(fixedTotal, variableTotal, installmentsCurrent, Math.max(balance, 0L));
        renderExpenseSection(fixedContainer, budgetData.fixedExpenses, true, "No hay gastos fijos cargados.");
        renderExpenseSection(variableContainer, budgetData.variableExpenses, false, "No hay gastos variables cargados.");
        renderInstallmentSection();
        renderHistory();
    }

    private void renderExpenseSection(LinearLayout container, List<BudgetData.ExpenseItem> items, boolean fixed, String emptyMessage) {
        container.removeAllViews();

        if (items.isEmpty()) {
            container.addView(buildEmptyState(emptyMessage));
            return;
        }

        for (int i = 0; i < items.size(); i++) {
            BudgetData.ExpenseItem item = items.get(i);
            container.addView(buildExpenseRow(item, fixed, i));
        }
    }

    private void renderInstallmentSection() {
        installmentContainer.removeAllViews();

        if (budgetData.installments.isEmpty()) {
            installmentContainer.addView(buildEmptyState("No hay compras en cuotas cargadas."));
            return;
        }

        for (int i = 0; i < budgetData.installments.size(); i++) {
            BudgetData.InstallmentItem item = budgetData.installments.get(i);
            String title = item.itemName + " · " + item.brand;
            String detail = item.currentInstallment + "/" + item.totalInstallments;
            installmentContainer.addView(buildInstallmentRow(title, detail + " · " + formatCurrency(item.amount),
                    item.continuesNextMonth() ? "Sigue el proximo mes" : "Ultima cuota", i));
        }
    }

    private View buildEmptyState(String message) {
        TextView view = new TextView(this);
        view.setText(message);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        view.setTextColor(getColor(R.color.subtitle_color));
        return view;
    }

    private void renderHistory() {
        historyContainer.removeAllViews();

        if (budgetData.history.isEmpty()) {
            historyContainer.addView(buildEmptyState(getString(R.string.history_empty)));
            return;
        }

        for (int i = budgetData.history.size() - 1; i >= 0; i--) {
            BudgetData.MonthlySnapshot snapshot = budgetData.history.get(i);
            historyContainer.addView(buildHistoryRow(snapshot));
        }
    }

    private View buildHistoryRow(BudgetData.MonthlySnapshot snapshot) {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(10), 0, dp(10));

        TextView title = new TextView(this);
        title.setText(snapshot.label);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setTextColor(getColor(R.color.title_color));
        title.setTypeface(title.getTypeface(), Typeface.BOLD);

        TextView meta = new TextView(this);
        meta.setText("Saldo " + formatCurrency(snapshot.balance) + " | Fijos " + formatCurrency(snapshot.fixedTotal)
                + " | Variables " + formatCurrency(snapshot.variableTotal));
        meta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        meta.setTextColor(getColor(R.color.body_color));
        meta.setPadding(0, dp(4), 0, 0);

        TextView next = new TextView(this);
        next.setText("Proximo mes estimado: " + formatCurrency(snapshot.nextMonthProjection)
                + " | Cuotas: " + formatCurrency(snapshot.installmentTotal));
        next.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        next.setTextColor(getColor(R.color.subtitle_color));
        next.setPadding(0, dp(4), 0, 0);

        row.addView(title);
        row.addView(meta);
        row.addView(next);

        View divider = new View(this);
        divider.setBackgroundColor(getColor(R.color.divider_color));
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
        ));

        wrapper.addView(row);
        wrapper.addView(divider);
        return wrapper;
    }

    private View buildExpenseRow(BudgetData.ExpenseItem item, boolean fixed, int index) {
        return buildEditableRow(item.name, formatCurrency(item.amount), fixed ? "Fijo" : "Variable",
                v -> showExpenseDialog(fixed, index),
                v -> confirmDeleteExpense(fixed, index));
    }

    private View buildInstallmentRow(String primary, String secondary, String badge, int index) {
        return buildEditableRow(primary, secondary, badge,
                v -> showInstallmentDialog(index),
                v -> confirmDeleteInstallment(index));
    }

    private View buildEditableRow(String primary, String secondary, String badge,
                                  View.OnClickListener onEdit, View.OnClickListener onDelete) {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(10), 0, dp(10));
        row.setBaselineAligned(false);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = new TextView(this);
        title.setText(primary);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setTextColor(getColor(R.color.title_color));
        title.setTypeface(title.getTypeface(), Typeface.BOLD);

        TextView amount = new TextView(this);
        amount.setText(secondary);
        amount.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        amount.setTextColor(getColor(R.color.body_color));
        amount.setPadding(0, dp(3), 0, 0);

        content.addView(title);
        content.addView(amount);

        if (badge != null) {
            TextView tag = new TextView(this);
            tag.setText(badge);
            tag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            tag.setTextColor(getColor(R.color.accent_dark));
            tag.setBackgroundResource(R.drawable.mini_card_neutral);
            tag.setPadding(dp(10), dp(5), dp(10), dp(5));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.topMargin = dp(7);
            tag.setLayoutParams(params);
            content.addView(tag);
        }

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        Button editButton = buildInlineButton("Editar", getColor(R.color.action_primary), onEdit);
        Button deleteButton = buildInlineButton("Borrar", getColor(R.color.danger_color), onDelete);
        deleteButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.danger_color)));

        actions.addView(editButton);
        actions.addView(deleteButton);

        row.addView(content);
        row.addView(actions);

        View divider = new View(this);
        divider.setBackgroundColor(getColor(R.color.divider_color));
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
        ));

        wrapper.addView(row);
        wrapper.addView(divider);
        return wrapper;
    }

    private Button buildInlineButton(String text, int color, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(getColor(android.R.color.white));
        button.setAllCaps(false);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(10), dp(8), dp(10), dp(8));
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.leftMargin = dp(6);
        button.setLayoutParams(params);
        button.setOnClickListener(listener);
        return button;
    }

    private void updateBarWeights(long fixedTotal, long variableTotal, long installmentsTotal, long available) {
        long total = fixedTotal + variableTotal + installmentsTotal + available;
        if (total <= 0) {
            total = 1;
        }

        setWeight(barFixed, fixedTotal, total);
        setWeight(barVariable, variableTotal, total);
        setWeight(barInstallments, installmentsTotal, total);
        setWeight(barAvailable, available, total);
    }

    private void setWeight(View view, long value, long total) {
        float weight = value <= 0 ? 0.05f : (float) value / (float) total;
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
        params.weight = weight;
        view.setLayoutParams(params);
    }

    private void showSalaryDialog() {
        EditText input = buildNumberInput("Ejemplo: 1200000");
        input.setText(String.valueOf(budgetData.salary));

        new AlertDialog.Builder(this)
                .setTitle("Actualizar sueldo")
                .setView(input)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    budgetData.salary = parseLong(input.getText().toString());
                    persistAndRender();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showExpenseDialog(boolean fixed) {
        showExpenseDialog(fixed, -1);
    }

    private void showExpenseDialog(boolean fixed, int index) {
        LinearLayout layout = buildDialogLayout();

        EditText nameInput = buildTextInput(fixed ? "Ejemplo: Alquiler" : "Ejemplo: Supermercado");
        EditText amountInput = buildNumberInput("Ejemplo: 50000");

        if (index >= 0) {
            BudgetData.ExpenseItem existing = fixed ? budgetData.fixedExpenses.get(index) : budgetData.variableExpenses.get(index);
            nameInput.setText(existing.name);
            amountInput.setText(String.valueOf(existing.amount));
        }

        layout.addView(nameInput);
        layout.addView(amountInput);

        new AlertDialog.Builder(this)
                .setTitle(index >= 0
                        ? (fixed ? "Editar gasto fijo" : "Editar gasto variable")
                        : (fixed ? "Nuevo gasto fijo" : "Nuevo gasto variable"))
                .setView(layout)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String name = safeText(nameInput);
                    long amount = parseLong(amountInput.getText().toString());

                    if (name.isEmpty() || amount <= 0) {
                        return;
                    }

                    BudgetData.ExpenseItem item = new BudgetData.ExpenseItem(name, amount);
                    if (fixed) {
                        upsertExpense(budgetData.fixedExpenses, item, index);
                    } else {
                        upsertExpense(budgetData.variableExpenses, item, index);
                    }
                    persistAndRender();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showInstallmentDialog() {
        showInstallmentDialog(-1);
    }

    private void showInstallmentDialog(int index) {
        LinearLayout layout = buildDialogLayout();

        EditText itemInput = buildTextInput("Ejemplo: Notebook");
        EditText brandInput = buildTextInput("Ejemplo: Visa");
        EditText currentInput = buildNumberInput("Cuota actual");
        EditText totalInput = buildNumberInput("Cuotas totales");
        EditText amountInput = buildNumberInput("Monto por cuota");

        if (index >= 0) {
            BudgetData.InstallmentItem existing = budgetData.installments.get(index);
            itemInput.setText(existing.itemName);
            brandInput.setText(existing.brand);
            currentInput.setText(String.valueOf(existing.currentInstallment));
            totalInput.setText(String.valueOf(existing.totalInstallments));
            amountInput.setText(String.valueOf(existing.amount));
        }

        layout.addView(itemInput);
        layout.addView(brandInput);
        layout.addView(currentInput);
        layout.addView(totalInput);
        layout.addView(amountInput);

        new AlertDialog.Builder(this)
                .setTitle(index >= 0 ? "Editar compra en cuotas" : "Nueva compra en cuotas")
                .setView(layout)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String itemName = safeText(itemInput);
                    String brand = safeText(brandInput);
                    int current = (int) parseLong(currentInput.getText().toString());
                    int total = (int) parseLong(totalInput.getText().toString());
                    long amount = parseLong(amountInput.getText().toString());

                    if (itemName.isEmpty() || brand.isEmpty() || current <= 0 || total <= 0 || current > total || amount <= 0) {
                        return;
                    }

                    BudgetData.InstallmentItem item = new BudgetData.InstallmentItem(itemName, brand, current, total, amount);
                    if (index >= 0) {
                        budgetData.installments.set(index, item);
                    } else {
                        budgetData.installments.add(item);
                    }
                    persistAndRender();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmDeleteExpense(boolean fixed, int index) {
        new AlertDialog.Builder(this)
                .setTitle("Borrar gasto")
                .setMessage("Este item se eliminara del presupuesto.")
                .setPositiveButton("Borrar", (dialog, which) -> {
                    if (fixed) {
                        budgetData.fixedExpenses.remove(index);
                    } else {
                        budgetData.variableExpenses.remove(index);
                    }
                    persistAndRender();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmDeleteInstallment(int index) {
        new AlertDialog.Builder(this)
                .setTitle("Borrar cuota")
                .setMessage("Esta compra en cuotas se eliminara del presupuesto.")
                .setPositiveButton("Borrar", (dialog, which) -> {
                    budgetData.installments.remove(index);
                    persistAndRender();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private LinearLayout buildDialogLayout() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(12), dp(8), dp(12), 0);
        return layout;
    }

    private EditText buildTextInput(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setSingleLine();
        return input;
    }

    private EditText buildNumberInput(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        return input;
    }

    private void persistAndRender() {
        repository.save(budgetData);
        render();
    }

    private void upsertExpense(List<BudgetData.ExpenseItem> items, BudgetData.ExpenseItem item, int index) {
        if (index >= 0) {
            items.set(index, item);
        } else {
            items.add(item);
        }
    }

    private long sumExpenses(List<BudgetData.ExpenseItem> items) {
        long total = 0L;
        for (BudgetData.ExpenseItem item : items) {
            total += item.amount;
        }
        return total;
    }

    private long sumCurrentInstallments() {
        long total = 0L;
        for (BudgetData.InstallmentItem item : budgetData.installments) {
            total += item.amount;
        }
        return total;
    }

    private long sumNextMonthInstallments() {
        long total = 0L;
        for (BudgetData.InstallmentItem item : budgetData.installments) {
            if (item.continuesNextMonth()) {
                total += item.amount;
            }
        }
        return total;
    }

    private int countContinuingInstallments() {
        int count = 0;
        for (BudgetData.InstallmentItem item : budgetData.installments) {
            if (item.continuesNextMonth()) {
                count++;
            }
        }
        return count;
    }

    private String formatCurrency(long amount) {
        return currencyFormatter.format(amount);
    }

    private String buildRatioText(long fixedTotal, long variableTotal, long installmentsCurrent, long spent) {
        if (budgetData.salary <= 0) {
            return "Carga un sueldo para ver porcentajes y salud financiera.";
        }

        int spentPercent = toPercent(spent, budgetData.salary);
        int fixedPercent = toPercent(fixedTotal, budgetData.salary);
        int variablePercent = toPercent(variableTotal, budgetData.salary);
        int installmentPercent = toPercent(installmentsCurrent, budgetData.salary);
        return "Usaste " + spentPercent + "% del sueldo. Fijos " + fixedPercent + "%, variables "
                + variablePercent + "% y cuotas " + installmentPercent + "%.";
    }

    private String buildLargestExpensesText() {
        List<BudgetLine> lines = new ArrayList<>();
        for (BudgetData.ExpenseItem item : budgetData.fixedExpenses) {
            lines.add(new BudgetLine(item.name, item.amount));
        }
        for (BudgetData.ExpenseItem item : budgetData.variableExpenses) {
            lines.add(new BudgetLine(item.name, item.amount));
        }

        lines.sort((left, right) -> Long.compare(right.amount, left.amount));

        if (lines.isEmpty()) {
            return "Todavia no hay gastos cargados para destacar.";
        }

        StringBuilder builder = new StringBuilder("Gastos mas altos: ");
        int limit = Math.min(3, lines.size());
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                builder.append(" | ");
            }
            BudgetLine line = lines.get(i);
            builder.append(line.name).append(" ").append(formatCurrency(line.amount));
        }
        return builder.toString();
    }

    private String buildUpcomingText(long nextMonthInstallments, long nextMonthProjection) {
        String installmentText = nextMonthInstallments > 0
                ? "Cuotas que siguen: " + formatCurrency(nextMonthInstallments)
                : "No quedan cuotas arrastradas.";
        return installmentText + " Total estimado proximo mes: " + formatCurrency(nextMonthProjection) + ".";
    }

    private int toPercent(long value, long total) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.round((value * 100.0d) / total);
    }

    private void exportReportToFile() {
        String content = buildReportText();
        String fileName = "finanzas-clara-" + new SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(new Date()) + ".txt";

        try {
            Uri uri = saveReport(fileName, content);
            Toast.makeText(this, "Reporte guardado: " + uri, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "No se pudo exportar el reporte.", Toast.LENGTH_LONG).show();
        }
    }

    private void saveMonthlySnapshot() {
        long fixedTotal = sumExpenses(budgetData.fixedExpenses);
        long variableTotal = sumExpenses(budgetData.variableExpenses);
        long installmentTotal = sumCurrentInstallments();
        long nextMonthProjection = fixedTotal + variableTotal + sumNextMonthInstallments();
        long balance = budgetData.salary - fixedTotal - variableTotal - installmentTotal;
        String label = new SimpleDateFormat("MMMM yyyy", new Locale("es", "AR")).format(new Date());

        budgetData.history.add(new BudgetData.MonthlySnapshot(
                capitalize(label),
                budgetData.salary,
                fixedTotal,
                variableTotal,
                installmentTotal,
                balance,
                nextMonthProjection
        ));
        repository.save(budgetData);
        render();
        Toast.makeText(this, "Mes guardado en el historial.", Toast.LENGTH_SHORT).show();
    }

    private Uri saveReport(String fileName, String content) throws IOException {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/FinanzasClara");

            ContentResolver resolver = getContentResolver();
            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                throw new IOException("No se pudo crear el archivo.");
            }

            try (OutputStream outputStream = resolver.openOutputStream(uri)) {
                if (outputStream == null) {
                    throw new IOException("No se pudo abrir el archivo.");
                }
                outputStream.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            return uri;
        }

        File directory = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (directory == null) {
            throw new IOException("No se pudo acceder al almacenamiento.");
        }

        File file = new File(directory, fileName);
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        return Uri.fromFile(file);
    }

    private void shareReport() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Reporte Finanzas Clara");
        intent.putExtra(Intent.EXTRA_TEXT, buildReportText());
        startActivity(Intent.createChooser(intent, "Compartir reporte"));
    }

    private String buildReportText() {
        long fixedTotal = sumExpenses(budgetData.fixedExpenses);
        long variableTotal = sumExpenses(budgetData.variableExpenses);
        long installmentsCurrent = sumCurrentInstallments();
        long nextMonthInstallments = sumNextMonthInstallments();
        long balance = budgetData.salary - fixedTotal - variableTotal - installmentsCurrent;

        StringBuilder builder = new StringBuilder();
        builder.append("FINANZAS CLARA\n");
        builder.append("Resumen mensual\n\n");
        builder.append("Sueldo: ").append(formatCurrency(budgetData.salary)).append("\n");
        builder.append("Gastos fijos: ").append(formatCurrency(fixedTotal)).append("\n");
        builder.append("Gastos variables: ").append(formatCurrency(variableTotal)).append("\n");
        builder.append("Cuotas del mes: ").append(formatCurrency(installmentsCurrent)).append("\n");
        builder.append("Saldo estimado: ").append(formatCurrency(balance)).append("\n");
        builder.append("Cuotas que siguen el proximo mes: ").append(formatCurrency(nextMonthInstallments)).append("\n\n");

        builder.append("DETALLE DE GASTOS FIJOS\n");
        appendExpenseLines(builder, budgetData.fixedExpenses);
        builder.append("\nDETALLE DE GASTOS VARIABLES\n");
        appendExpenseLines(builder, budgetData.variableExpenses);
        builder.append("\nDETALLE DE CUOTAS\n");
        appendInstallmentLines(builder);
        builder.append("\n").append(buildLargestExpensesText()).append("\n");
        builder.append(buildUpcomingText(nextMonthInstallments, fixedTotal + variableTotal + nextMonthInstallments)).append("\n");
        return builder.toString();
    }

    private void appendExpenseLines(StringBuilder builder, List<BudgetData.ExpenseItem> items) {
        if (items.isEmpty()) {
            builder.append("- Sin datos\n");
            return;
        }

        for (BudgetData.ExpenseItem item : items) {
            builder.append("- ").append(item.name).append(": ").append(formatCurrency(item.amount)).append("\n");
        }
    }

    private void appendInstallmentLines(StringBuilder builder) {
        if (budgetData.installments.isEmpty()) {
            builder.append("- Sin datos\n");
            return;
        }

        for (BudgetData.InstallmentItem item : budgetData.installments) {
            builder.append("- ")
                    .append(item.itemName)
                    .append(" / ")
                    .append(item.brand)
                    .append(" ")
                    .append(item.currentInstallment)
                    .append("/")
                    .append(item.totalInstallments)
                    .append(": ")
                    .append(formatCurrency(item.amount))
                    .append("\n");
        }
    }

    private long parseLong(String value) {
        String cleaned = value == null ? "" : value.replace(".", "").replace(",", "").replaceAll("[^0-9]", "");
        if (cleaned.isEmpty()) {
            return 0L;
        }
        return Long.parseLong(cleaned);
    }

    private String safeText(EditText input) {
        String value = input.getText() == null ? "" : input.getText().toString().trim();
        return value;
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.substring(0, 1).toUpperCase(new Locale("es", "AR")) + value.substring(1);
    }

    private static class BudgetLine {
        private final String name;
        private final long amount;

        private BudgetLine(String name, long amount) {
            this.name = name;
            this.amount = amount;
        }
    }
}

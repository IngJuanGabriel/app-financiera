package com.appfinanciera;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class BudgetRepository {
    private static final String PREFS = "app_financiera_prefs";
    private static final String KEY_DATA = "budget_data";

    private final SharedPreferences preferences;

    public BudgetRepository(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public BudgetData load() {
        String saved = preferences.getString(KEY_DATA, null);
        if (saved == null || saved.trim().isEmpty()) {
            BudgetData sample = buildSampleData();
            save(sample);
            return sample;
        }

        try {
            return fromJson(new JSONObject(saved));
        } catch (JSONException e) {
            BudgetData sample = buildSampleData();
            save(sample);
            return sample;
        }
    }

    public void save(BudgetData data) {
        preferences.edit().putString(KEY_DATA, toJson(data).toString()).apply();
    }

    private JSONObject toJson(BudgetData data) {
        JSONObject root = new JSONObject();
        JSONArray fixed = new JSONArray();
        JSONArray variable = new JSONArray();
        JSONArray installments = new JSONArray();
        JSONArray history = new JSONArray();

        try {
            root.put("salary", data.salary);

            for (BudgetData.ExpenseItem item : data.fixedExpenses) {
                fixed.put(expenseToJson(item));
            }

            for (BudgetData.ExpenseItem item : data.variableExpenses) {
                variable.put(expenseToJson(item));
            }

            for (BudgetData.InstallmentItem item : data.installments) {
                installments.put(installmentToJson(item));
            }

            for (BudgetData.MonthlySnapshot snapshot : data.history) {
                history.put(snapshotToJson(snapshot));
            }

            root.put("fixedExpenses", fixed);
            root.put("variableExpenses", variable);
            root.put("installments", installments);
            root.put("history", history);
        } catch (JSONException e) {
            throw new IllegalStateException("No se pudo serializar el presupuesto.", e);
        }

        return root;
    }

    private JSONObject expenseToJson(BudgetData.ExpenseItem item) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("name", item.name);
        json.put("amount", item.amount);
        return json;
    }

    private JSONObject installmentToJson(BudgetData.InstallmentItem item) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("itemName", item.itemName);
        json.put("brand", item.brand);
        json.put("currentInstallment", item.currentInstallment);
        json.put("totalInstallments", item.totalInstallments);
        json.put("amount", item.amount);
        return json;
    }

    private JSONObject snapshotToJson(BudgetData.MonthlySnapshot snapshot) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("label", snapshot.label);
        json.put("salary", snapshot.salary);
        json.put("fixedTotal", snapshot.fixedTotal);
        json.put("variableTotal", snapshot.variableTotal);
        json.put("installmentTotal", snapshot.installmentTotal);
        json.put("balance", snapshot.balance);
        json.put("nextMonthProjection", snapshot.nextMonthProjection);
        return json;
    }

    private BudgetData fromJson(JSONObject root) throws JSONException {
        BudgetData data = new BudgetData();
        data.salary = root.optLong("salary", 0L);

        JSONArray fixed = root.optJSONArray("fixedExpenses");
        JSONArray variable = root.optJSONArray("variableExpenses");
        JSONArray installments = root.optJSONArray("installments");
        JSONArray history = root.optJSONArray("history");

        appendExpenses(data.fixedExpenses, fixed);
        appendExpenses(data.variableExpenses, variable);
        appendInstallments(data.installments, installments);
        appendHistory(data.history, history);
        return data;
    }

    private void appendExpenses(java.util.List<BudgetData.ExpenseItem> target, JSONArray array) throws JSONException {
        if (array == null) {
            return;
        }

        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);
            target.add(new BudgetData.ExpenseItem(
                    item.optString("name", "Sin nombre"),
                    item.optLong("amount", 0L)
            ));
        }
    }

    private void appendInstallments(java.util.List<BudgetData.InstallmentItem> target, JSONArray array) throws JSONException {
        if (array == null) {
            return;
        }

        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);
            target.add(new BudgetData.InstallmentItem(
                    item.optString("itemName", "Compra"),
                    item.optString("brand", "Tarjeta"),
                    item.optInt("currentInstallment", 1),
                    item.optInt("totalInstallments", 1),
                    item.optLong("amount", 0L)
            ));
        }
    }

    private void appendHistory(java.util.List<BudgetData.MonthlySnapshot> target, JSONArray array) throws JSONException {
        if (array == null) {
            return;
        }

        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);
            target.add(new BudgetData.MonthlySnapshot(
                    item.optString("label", "Mes guardado"),
                    item.optLong("salary", 0L),
                    item.optLong("fixedTotal", 0L),
                    item.optLong("variableTotal", 0L),
                    item.optLong("installmentTotal", 0L),
                    item.optLong("balance", 0L),
                    item.optLong("nextMonthProjection", 0L)
            ));
        }
    }

    private BudgetData buildSampleData() {
        // A fresh install starts empty. User data is stored only on the device.
        return new BudgetData();
    }
}

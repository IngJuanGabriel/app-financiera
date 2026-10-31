package com.appfinanciera;

import java.util.ArrayList;
import java.util.List;

public class BudgetData {
    public long salary;
    public final List<ExpenseItem> fixedExpenses;
    public final List<ExpenseItem> variableExpenses;
    public final List<InstallmentItem> installments;
    public final List<MonthlySnapshot> history;

    public BudgetData() {
        fixedExpenses = new ArrayList<>();
        variableExpenses = new ArrayList<>();
        installments = new ArrayList<>();
        history = new ArrayList<>();
    }

    public static class ExpenseItem {
        public final String name;
        public final long amount;

        public ExpenseItem(String name, long amount) {
            this.name = name;
            this.amount = amount;
        }
    }

    public static class InstallmentItem {
        public final String itemName;
        public final String brand;
        public final int currentInstallment;
        public final int totalInstallments;
        public final long amount;

        public InstallmentItem(String itemName, String brand, int currentInstallment, int totalInstallments, long amount) {
            this.itemName = itemName;
            this.brand = brand;
            this.currentInstallment = currentInstallment;
            this.totalInstallments = totalInstallments;
            this.amount = amount;
        }

        public boolean continuesNextMonth() {
            return currentInstallment < totalInstallments;
        }
    }

    public static class MonthlySnapshot {
        public final String label;
        public final long salary;
        public final long fixedTotal;
        public final long variableTotal;
        public final long installmentTotal;
        public final long balance;
        public final long nextMonthProjection;

        public MonthlySnapshot(String label, long salary, long fixedTotal, long variableTotal,
                               long installmentTotal, long balance, long nextMonthProjection) {
            this.label = label;
            this.salary = salary;
            this.fixedTotal = fixedTotal;
            this.variableTotal = variableTotal;
            this.installmentTotal = installmentTotal;
            this.balance = balance;
            this.nextMonthProjection = nextMonthProjection;
        }
    }
}

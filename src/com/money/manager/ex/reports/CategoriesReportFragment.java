/*
 * Copyright (C) 2012-2015 The Android Money Manager Ex Project Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.money.manager.ex.reports;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.money.manager.ex.R;
import com.money.manager.ex.common.AllDataFragment;
import com.money.manager.ex.core.Core;
import com.money.manager.ex.core.TransactionTypes;
import com.money.manager.ex.currency.CurrencyService;
import com.money.manager.ex.database.QueryAllData;
import com.money.manager.ex.database.ViewMobileData;
import com.money.manager.ex.search.CategorySub;
import com.money.manager.ex.search.SearchActivity;
import com.money.manager.ex.search.SearchFragment;
import com.money.manager.ex.search.SearchParameters;
import com.money.manager.ex.utils.DateUtils;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;

/**
 * Categories report fragment.
 * Created by Alen Siljak on 06/07/2015.
 */
public class CategoriesReportFragment
        extends BaseReportFragment {

    private LinearLayout mListViewHeader, mListViewFooter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        setListAdapter(null);
        setShowMenuItemSearch(true);

        //create header view
        mListViewHeader = (LinearLayout) addListViewHeaderFooter(R.layout.item_generic_report_2_columns);
        TextView txtColumn1 = (TextView) mListViewHeader.findViewById(R.id.textViewColumn1);
        TextView txtColumn2 = (TextView) mListViewHeader.findViewById(R.id.textViewColumn2);
        //set header
        txtColumn1.setText(R.string.category);
        txtColumn1.setTypeface(null, Typeface.BOLD);
        txtColumn2.setText(R.string.amount);
        txtColumn2.setTypeface(null, Typeface.BOLD);
        //add to list view
        getListView().addHeaderView(mListViewHeader);

        //create footer view
        mListViewFooter = (LinearLayout) addListViewHeaderFooter(R.layout.item_generic_report_2_columns);
        txtColumn1 = (TextView) mListViewFooter.findViewById(R.id.textViewColumn1);
        txtColumn2 = (TextView) mListViewFooter.findViewById(R.id.textViewColumn2);
        //set footer
        txtColumn1.setText(R.string.total);
        txtColumn1.setTypeface(null, Typeface.BOLD_ITALIC);
        txtColumn2.setText(R.string.total);
        txtColumn2.setTypeface(null, Typeface.BOLD_ITALIC);

        //add to list view --> move to load finished
        //getListView().addFooterView(mListViewFooter);

        //set adapter
        CategoriesReportAdapter adapter = new CategoriesReportAdapter(getActivity(), null);
        setListAdapter(adapter);
        //call super method
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        Core core = new Core(getActivity());
        // pie chart
        MenuItem itemChart = menu.findItem(R.id.menu_chart);
        if (itemChart != null) {
            itemChart.setVisible(!(((CategoriesReportActivity) getActivity()).mIsDualPanel));
            itemChart.setIcon(core.resolveIdAttribute(R.attr.ic_action_pie_chart));
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        super.onLoadFinished(loader, data);
        switch (loader.getId()) {
            case ID_LOADER:
                //parse cursor for calculate total
                if (data == null) return;

                CurrencyService currencyService = new CurrencyService(getActivity().getApplicationContext());

                double totalAmount = 0;
                while (data.moveToNext()) {
                    totalAmount += data.getDouble(data.getColumnIndex("TOTAL"));
                }
                TextView txtColumn2 = (TextView) mListViewFooter.findViewById(R.id.textViewColumn2);
                txtColumn2.setText(currencyService.getBaseCurrencyFormatted(totalAmount));

                // solved bug chart
                if (data.getCount() > 0) {
                    getListView().removeFooterView(mListViewFooter);
                    getListView().addFooterView(mListViewFooter);
                }

                if (((CategoriesReportActivity) getActivity()).mIsDualPanel) {
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {

                        @Override
                        public void run() {
                            showChart();

                        }
                    }, 1000);
                }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_chart:
                showChart();
                break;
        }

        if (item.getItemId() < 0) {
            // category
            String whereClause = getWhereClause();
            if (!TextUtils.isEmpty(whereClause))
                whereClause += " AND ";
            else
                whereClause = "";
            whereClause += " " + ViewMobileData.CategID + "=" + Integer.toString(Math.abs(item.getItemId()));
            //create arguments
            Bundle args = new Bundle();
            args.putString(KEY_WHERE_CLAUSE, whereClause);
            //starts loader
            startLoader(args);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected boolean onQueryTextChange(String newText) {
        //recall last where clause
        String whereClause = getWhereClause();
        if (whereClause == null) whereClause = "";

        int start = whereClause.indexOf("/** */");
        if (start > 0) {
            int end = whereClause.indexOf("/** */", start + 1) + "/** */".length();
            whereClause = whereClause.substring(0, start) + whereClause.substring(end);
            // trim some space
            whereClause = whereClause.trim();
        }

        if (!TextUtils.isEmpty(whereClause)) {
            whereClause += " /** */AND ";
        } else {
            whereClause = "/** */";
        }
        // use token to replace criteria
        whereClause += "(" + ViewMobileData.Category + " Like '%" + newText + "%' OR " +
                ViewMobileData.Subcategory + " Like '%" + newText + "%')/** */";

        //create arguments
        Bundle args = new Bundle();
        args.putString(KEY_WHERE_CLAUSE, whereClause);
        //starts loader
        startLoader(args);
        return super.onQueryTextChange(newText);
    }

    @Override
    protected String prepareQuery(String whereClause) {
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        ViewMobileData mobileData = new ViewMobileData(getContext());

        //data to compose builder
        String[] projectionIn = new String[]{
            "ROWID AS _id", // this does not fetch anything, unfortunately.
            ViewMobileData.CategID, ViewMobileData.Category,
            ViewMobileData.SubcategID, ViewMobileData.Subcategory,
            "SUM(" + ViewMobileData.AmountBaseConvRate + ") AS TOTAL"
        };

        String selection = ViewMobileData.Status + "<>'V' AND " +
            ViewMobileData.TransactionType + " IN ('Withdrawal', 'Deposit')";
        if (!TextUtils.isEmpty(whereClause)) {
            selection += " AND " + whereClause;
        }

        String groupBy = ViewMobileData.CategID + ", " + ViewMobileData.Category + ", " +
                ViewMobileData.SubcategID + ", " + ViewMobileData.Subcategory;

        String having = null;
        if (!TextUtils.isEmpty(((CategoriesReportActivity) getActivity()).mFilter)) {
            String filter = ((CategoriesReportActivity) getActivity()).mFilter;
            if (TransactionTypes.valueOf(filter).equals(TransactionTypes.Withdrawal)) {
                having = "SUM(" + ViewMobileData.AmountBaseConvRate + ") < 0";
            } else {
                having = "SUM(" + ViewMobileData.AmountBaseConvRate + ") > 0";
            }
        }

        String sortOrder = ViewMobileData.Category + ", " + ViewMobileData.Subcategory;
        String limit = null;

        //compose builder
        builder.setTables(mobileData.getSource());

        //return query
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            return builder.buildQuery(projectionIn, selection, groupBy, having, sortOrder, limit);
        } else {
            return builder.buildQuery(projectionIn, selection, null, groupBy, having, sortOrder, limit);
        }
    }

    @Override
    public String getSubTitle() {
        return null;
    }

    /**
     * List item clicked. Show the transaction list for the category.
     * @param l        The ListView where the click happened
     * @param v        The view that was clicked within the ListView
     * @param position The position of the view in the list
     * @param id       The row id of the item that was clicked
     */
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        CategorySub category = getCategoryFromSelectedItem(l, position);
        if (category == null) return;

        // now list the transactions for the given category/subcategory combination,
        // in the selected time period.

//        showTransactionsFragment(values);

        // Show search activity with the results.
        SearchParameters parameters = new SearchParameters();
        parameters.category = category;
        parameters.dateFrom = DateUtils.getIsoStringDate(mDateFrom);
        parameters.dateTo = DateUtils.getIsoStringDate(mDateTo);

        showSearchActivityFor(parameters);
    }

    public void showChart() {
        CategoriesReportAdapter adapter = (CategoriesReportAdapter) getListAdapter();
        if (adapter == null) return;
        Cursor cursor = adapter.getCursor();
        if (cursor == null) return;
        if (cursor.getCount() <= 0) return;

        ArrayList<ValuePieEntry> arrayList = new ArrayList<>();
        CurrencyService currencyService = new CurrencyService(getActivity().getApplicationContext());

        // Reset cursor to initial position.
        cursor.moveToPosition(-1);
        // process cursor
        while (cursor.moveToNext()) {
            ValuePieEntry item = new ValuePieEntry();
            String category = cursor.getString(cursor.getColumnIndex(ViewMobileData.Category));
            if (!TextUtils.isEmpty(cursor.getString(cursor.getColumnIndex(ViewMobileData.Subcategory)))) {
                category += " : " + cursor.getString(cursor.getColumnIndex(ViewMobileData.Subcategory));
            }
            // total
            double total = Math.abs(cursor.getDouble(cursor.getColumnIndex("TOTAL")));
            // check if category is empty
            if (TextUtils.isEmpty(category)) {
                category = getString(R.string.empty_category);
            }

            item.setText(category);
            item.setValue(total);
            item.setValueFormatted(currencyService.getCurrencyFormatted(currencyService.getBaseCurrencyId(), total));
            // add element
            arrayList.add(item);
        }

        Bundle args = new Bundle();
        args.putSerializable(PieChartFragment.KEY_CATEGORIES_VALUES, arrayList);
        //get fragment manager
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        if (fragmentManager != null) {
            PieChartFragment fragment;
            fragment = (PieChartFragment) fragmentManager.findFragmentByTag(IncomeVsExpensesChartFragment.class.getSimpleName());
            if (fragment == null) {
                fragment = new PieChartFragment();
            }
            fragment.setChartArguments(args);
            fragment.setDisplayHomeAsUpEnabled(true);

            if (fragment.isVisible()) fragment.onResume();

            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            if (((CategoriesReportActivity) getActivity()).mIsDualPanel) {
                fragmentTransaction.replace(R.id.fragmentChart, fragment, PieChartFragment.class.getSimpleName());
            } else {
                fragmentTransaction.replace(R.id.fragmentContent, fragment, PieChartFragment.class.getSimpleName());
                fragmentTransaction.addToBackStack(null);
            }
            fragmentTransaction.commit();
        }
    }

    private AllDataFragment createTransactionsFragment(CategorySub category) {
        // implement callback interface?
        AllDataFragment fragment = AllDataFragment.newInstance(-1, null);

        Bundle args = new Bundle();
        ArrayList<String> where = new ArrayList<>();
        where.add("CategId=" + Integer.toString(category.categId) +
                " AND SubCategId=" + Integer.toString(category.subCategId));
        if (!StringUtils.isEmpty(getWhereClause())) {
            where.add(getWhereClause());
        }
        args.putStringArrayList(AllDataFragment.KEY_ARGUMENTS_WHERE, where);
//            ArrayList<String> params = new ArrayList<>();
//            params.add(values.getAsString(ViewMobileData.CategID));
//            params.add(values.getAsString(ViewMobileData.SubcategID));
//            args.putStringArrayList(AllDataFragment.KEY_ARGUMENTS_WHERE_PARAMS, params);
        // Sorting
//            args.putString(AllDataFragment.KEY_ARGUMENTS_SORT,
//                    QueryAllData.TOACCOUNTID + ", " + QueryAllData.TransactionType + ", " + QueryAllData.ID);
        //set arguments
        fragment.setArguments(args);
        // group by account
        fragment.setShownHeader(true);

        return fragment;
    }

//    private void showTransactionsFragment(CategorySub category) {
//        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
//        String tag = AllDataFragment.class.getSimpleName();
//        AllDataFragment fragment = (AllDataFragment) fragmentManager.findFragmentByTag(tag);
//        if (fragment == null) {
//            fragment = createTransactionsFragment(category);
//        }
//
//        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
//        fragmentTransaction.replace(R.id.fragmentContent, fragment, AllDataFragment.class.getSimpleName());
//        fragmentTransaction.addToBackStack(null);
//        fragmentTransaction.commit();
//    }

    private CategorySub getCategoryFromSelectedItem(ListView l, int position) {
        // Reading item from the list view, not adapter!
        Object item = l.getItemAtPosition(position);
        if (item == null) return null;

        Cursor cursor = (Cursor) item;

        ContentValues values = new ContentValues();
        DatabaseUtils.cursorIntToContentValues(cursor, ViewMobileData.CategID, values);
        DatabaseUtils.cursorIntToContentValues(cursor, ViewMobileData.SubcategID, values);

        int categoryId = values.getAsInteger(ViewMobileData.CategID);
        int subCategoryId = values.getAsInteger(ViewMobileData.SubcategID);

        CategorySub result = CategorySub.getInstance(categoryId, subCategoryId);
        return result;
    }

    private void showSearchActivityFor(SearchParameters parameters) {
        Intent intent = new Intent(getActivity(), SearchActivity.class);
        intent.putExtra(SearchActivity.EXTRA_SEARCH_PARAMETERS, parameters);
        intent.setAction(Intent.ACTION_INSERT);
        startActivity(intent);
    }

}

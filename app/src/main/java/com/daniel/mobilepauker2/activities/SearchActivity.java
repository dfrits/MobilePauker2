package com.daniel.mobilepauker2.activities;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;

import com.daniel.mobilepauker2.R;
import com.daniel.mobilepauker2.model.CardAdapter;
import com.daniel.mobilepauker2.model.FlashCard;
import com.daniel.mobilepauker2.model.ModelManager;
import com.daniel.mobilepauker2.utils.Constants;
import com.daniel.mobilepauker2.utils.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Created by Daniel on 06.03.2018.
 * Masterarbeit:
 * MobilePauker++ - Intuitiv, plattformübergreifend lernen
 * Daniel Fritsch
 * hs-augsburg
 */

public class SearchActivity extends AppCompatActivity {
    private final Context context = this;
    private final ModelManager modelManager = ModelManager.instance();
    Vector<Integer> itemPosition;
    private ListView listView;
    private Intent intent;
    private List<FlashCard> pack;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            setContentView(R.layout.search_cards);
            listView = findViewById(R.id.listView);
            itemPosition = new Vector<>();

            int stackIndex = intent.getIntExtra(Constants.STACK_INDEX, 0);
            modelManager.setCurrentPack(context, stackIndex);
            pack = modelManager.getCurrentPack();
        } else {
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.browse, menu);

        MenuItem search = menu.findItem(R.id.mSearch);
        final SearchView searchView = (SearchView) search.getActionView();
        searchView.setQueryHint(getString(R.string.search_hint));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                List<FlashCard> results = new ArrayList<>();
                itemPosition.clear();

                results = queryString(query, results);

                showResults(results);

                return true;
            }
        });
        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) searchView.clearFocus();
            }
        });
        String query = intent.getStringExtra(SearchManager.QUERY);
        searchView.setQuery(query, true);
        searchView.setIconifiedByDefault(false);
        if (!query.isEmpty()) {
            search.expandActionView();
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.REQUEST_CODE_EDIT_CARD && resultCode == RESULT_OK) {
            pack = modelManager.getCurrentPack();
            invalidateOptionsMenu();
        }
    }

    private void showResults(List<FlashCard> results) {
        CardAdapter adapter = new CardAdapter(context, results);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (itemPosition.size() > 0) {
                    editCard(itemPosition.get(position));
                } else {
                    editCard(position);
                }
            }
        });
    }

    private List<FlashCard> queryString(String query, List<FlashCard> results) {
        if (query.equals("")) {
            results = pack;
        } else {
            FlashCard card;
            for (int i = 0; i < pack.size(); i++) {
                card = pack.get(i);
                Log.d("SearchActivity::ShowResults", "Index - " + card.getId());

                String frontSide = card.getFrontSide().getText().toLowerCase();
                String backSide = card.getReverseSide().getText().toLowerCase();

                if (frontSide.contains(query.toLowerCase())
                        || backSide.contains(query.toLowerCase())) {
                    results.add(card);
                    itemPosition.add(i);
                }
            }
        }
        return results;
    }

    private void editCard(int position) {
        Intent intent = new Intent(context, EditCardActivity.class);
        intent.putExtra(Constants.CURSOR_POSITION, position);
        startActivityForResult(intent, Constants.REQUEST_CODE_EDIT_CARD);
    }

    public void mOpenSearchClicked(MenuItem searchMenuItem) {
        SearchView searchView = (SearchView) searchMenuItem.getActionView();
        searchView.setIconified(false);
    }
}

package com.daniel.mobilepauker2.activities;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
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

import com.daniel.mobilepauker2.PaukerManager;
import com.daniel.mobilepauker2.R;
import com.daniel.mobilepauker2.model.CardAdapter;
import com.daniel.mobilepauker2.model.FlashCard;
import com.daniel.mobilepauker2.model.ModelManager;
import com.daniel.mobilepauker2.model.SearchResultMultiChoiceModeListener;
import com.daniel.mobilepauker2.model.pauker_native.Card;
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
    private final PaukerManager paukerManager = PaukerManager.instance();
    private SearchResultMultiChoiceModeListener modalListener;
    private Vector<Integer> itemPosition;
    private ListView listView;
    private int stackIndex;
    private Intent intent;
    private List<FlashCard> pack;
    private List<FlashCard> checkedCards;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            setContentView(R.layout.search_cards);
            listView = findViewById(R.id.listView);
            itemPosition = new Vector<>();

            stackIndex = intent.getIntExtra(Constants.STACK_INDEX, 0);
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
                itemPosition.clear();

                showResults(queryString(query));

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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mSortFrontASC:
                sortCards(Card.Element.FRONT_SIDE, true);
                return true;
            case R.id.mSortBackASC:
                sortCards(Card.Element.REVERSE_SIDE, true);
                return true;
            case R.id.mSortBatchnumberASC:
                sortCards(Card.Element.BATCH_NUMBER, true);
                return true;
            case R.id.mSortLearnedDateASC:
                sortCards(Card.Element.LEARNED_DATE, true);
                return true;
            case R.id.mSortExpiredDateASC:
                sortCards(Card.Element.EXPIRED_DATE, true);
                return true;
            case R.id.mSortRepetTypeASC:
                sortCards(Card.Element.REPEATING_MODE, true);
                return true;
            case R.id.mSortFrontDSC:
                sortCards(Card.Element.FRONT_SIDE, false);
                return true;
            case R.id.mSortBackDSC:
                sortCards(Card.Element.REVERSE_SIDE, false);
                return true;
            case R.id.mSortBatchnumberDSC:
                sortCards(Card.Element.BATCH_NUMBER, false);
                return true;
            case R.id.mSortLearnedDateDSC:
                sortCards(Card.Element.LEARNED_DATE, false);
                return true;
            case R.id.mSortExpiredDateDSC:
                sortCards(Card.Element.EXPIRED_DATE, false);
                return true;
            case R.id.mSortRepetTypeDSC:
                sortCards(Card.Element.REPEATING_MODE, false);
                return true;
            default:
                return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.REQUEST_CODE_EDIT_CARD && resultCode == RESULT_OK) {
            pack = modelManager.getCurrentPack();
            invalidateOptionsMenu();
        }
        listView.setEnabled(true);
    }

    private void showResults(final List<FlashCard> results) {
        CardAdapter adapter = new CardAdapter(context, results);
        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (modalListener != null && modalListener.getActiveState() != null) {
                    listView.setItemChecked(position, !listView.isItemChecked(position));
                    return;
                }

                if (itemPosition.size() > 0) {
                    editCard(itemPosition.get(position));
                } else {
                    editCard(position);
                }
            }
        });
        modalListener = new SearchResultMultiChoiceModeListener(new SearchResultMultiChoiceModeListener.SRMCMListenerCallback() {
            @Override
            public void itemCheckedStateChanged(int position, boolean checked) {
                if (checkedCards == null)
                    checkedCards = new ArrayList<>();

                FlashCard item = (FlashCard) listView.getItemAtPosition(position);
                if (checked) {
                    checkedCards.add(item);
                } else {
                    checkedCards.remove(item);
                }
            }

            @Override
            public void resetCards() {
                if (checkedCards != null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle(R.string.reset_cards)
                            .setMessage(R.string.reset_cards_message)
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    for (FlashCard card : checkedCards) {
                                        modelManager.forgetCard(card);
                                    }
                                    checkedCards = null;
                                    paukerManager.setSaveRequired(true);
                                    modelManager.setCurrentPack(context, stackIndex);
                                    pack = modelManager.getCurrentPack();
                                    invalidateOptionsMenu();
                                }
                            })
                            .setNeutralButton(R.string.cancel, null);
                    builder.create().show();
                }
            }

            @Override
            public void repeatCardsNow() {
                if (checkedCards != null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle(R.string.instant_repeat)
                            .setMessage(R.string.instant_repeat_message)
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    for (FlashCard card : checkedCards) {
                                        modelManager.instantRepeatCard(card);
                                    }
                                    checkedCards = null;
                                    paukerManager.setSaveRequired(true);
                                    modelManager.setCurrentPack(context, stackIndex);
                                    pack = modelManager.getCurrentPack();
                                    invalidateOptionsMenu();
                                }
                            })
                            .setNeutralButton(R.string.cancel, null);
                    builder.create().show();
                }
            }

            @Override
            public void flipSides() {
                if (checkedCards != null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle(R.string.flip_card_sides)
                            .setMessage(R.string.flip_cards_message)
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    for (FlashCard card : checkedCards) {
                                        card.flip();
                                    }
                                    checkedCards = null;
                                    paukerManager.setSaveRequired(true);
                                    modelManager.setCurrentPack(context, stackIndex);
                                    pack = modelManager.getCurrentPack();
                                    invalidateOptionsMenu();
                                }
                            })
                            .setNeutralButton(R.string.cancel, null);
                    builder.create().show();
                }
            }

            @Override
            public void selectAll() {
                for (int i = 0; i < listView.getCount(); i++) {
                    if (!listView.isItemChecked(i)) {
                        listView.setItemChecked(i, true);
                    }
                }
            }

            @Override
            public void deleteCards() {
                if (checkedCards != null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle(R.string.delete_card)
                            .setMessage(R.string.delete_card_message)
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    for (FlashCard card : checkedCards) {
                                        boolean cardDeleted = modelManager.deleteCard(card);
                                        Log.d("SearchActivity::MultiChoiceListener::deleteCards",
                                                "Card deleted: " + cardDeleted);
                                    }
                                    checkedCards = null;
                                    paukerManager.setSaveRequired(true);
                                    modelManager.setCurrentPack(context, stackIndex);
                                    pack = modelManager.getCurrentPack();
                                    invalidateOptionsMenu();
                                }
                            })
                            .setNeutralButton(R.string.cancel, null);
                    builder.create().show();
                }
            }

            @Override
            public void setRepeatingType(boolean isRepeatByTyping) {
                if (checkedCards != null) {
                    for (FlashCard card : checkedCards) {
                        card.setRepeatByTyping(isRepeatByTyping);
                    }
                    checkedCards = null;
                    paukerManager.setSaveRequired(true);
                    modelManager.setCurrentPack(context, stackIndex);
                    pack = modelManager.getCurrentPack();
                    invalidateOptionsMenu();
                }
            }
        }, stackIndex != 1);
        listView.setMultiChoiceModeListener(modalListener);
    }

    private List<FlashCard> queryString(String query) {
        List<FlashCard> results = new ArrayList<>();
        if (query.equals("")) {
            results = pack;
        } else {
            FlashCard card;
            for (int i = 0; i < pack.size(); i++) {
                card = pack.get(i);
                Log.d("SearchActivity::ShowResults", "Index - " + card.getId());

                String frontSide = card.getFrontSide().getText().toLowerCase();
                String backSide = card.getReverseSide().getText().toLowerCase();

                //frontSide = Normalizer.normalize(frontSide, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");;
                //backSide = Normalizer.normalize(backSide, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");;
                //query = Normalizer.normalize(query, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");;

                if (frontSide.contains(query.toLowerCase())
                        || backSide.contains(query.toLowerCase())) {
                    results.add(card);
                    itemPosition.add(i);
                }
            }
        }
        return results;
    }

    /**
     * Sortiert die Liste nach dem angebenen Element aufsteigend oder absteigend.
     * @param sortByElement Element nach dem sortiert werden soll
     * @param asc_direction Gibt an ob aufsteigend oder absteigen. True ist aufsteigend und
     *                      false absteigend
     */
    private void sortCards(Card.Element sortByElement, boolean asc_direction) {
        modelManager.sortBatch(stackIndex, sortByElement, asc_direction);
        paukerManager.setSaveRequired(true);
        modelManager.setCurrentPack(context, stackIndex);
        pack = modelManager.getCurrentPack();
        invalidateOptionsMenu();
    }

    private void editCard(int position) {
        listView.setEnabled(false);
        Intent intent = new Intent(context, EditCardActivity.class);
        intent.putExtra(Constants.CURSOR_POSITION, position);
        startActivityForResult(intent, Constants.REQUEST_CODE_EDIT_CARD);
    }

    public void mOpenSearchClicked(MenuItem searchMenuItem) {
        SearchView searchView = (SearchView) searchMenuItem.getActionView();
        searchView.setIconified(false);
    }

    /**
     * Startet die Multiauswahl manuell.
     * @param item Nicht notwendig
     */
    public void mStartChooseClicked(@Nullable MenuItem item) {
        if (modalListener != null) {
            listView.startActionMode(modalListener);
        }
    }
}

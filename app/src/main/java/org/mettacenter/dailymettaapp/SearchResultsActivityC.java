package org.mettacenter.dailymettaapp;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.app.SearchManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Typeface;
import android.provider.BaseColumns;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Activity for displaying the list of search results
 * The actual search query takes place in the onCreateLoader method
 */
public class SearchResultsActivityC
        extends ListActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private SimpleCursorAdapter mAdapter = null;
    private String mSearchStringSg = null; //-TODO: Verify that this is not null?
    private static final int SEARCH_CONTEXT_CHARACTER_PADDING = 50;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);

        Intent tIntent = getIntent();

        if(Intent.ACTION_SEARCH.equals(tIntent.getAction())){
            mSearchStringSg = tIntent.getStringExtra(SearchManager.QUERY);
            //-this data will be used when the loader is initiated

            Log.d(ConstsU.TAG, "mSearchStringSg = " + mSearchStringSg);
        }

        getLoaderManager().initLoader(0, null, this);

        //Setting up the adapter..
        String[] tFromColumnsSg = new String[]{ArticleTableM.COLUMN_TITLE, ArticleTableM.COLUMN_TIME_MONTH, ArticleTableM.COLUMN_TIME_DAYOFMONTH, ArticleTableM.COLUMN_TEXT};
        int[] tToGuiIt = new int[]{R.id.search_row_title, R.id.search_row_date_month, R.id.search_row_date_dayofmonth, R.id.search_row_text}; //-contained in the layout
        mAdapter = new SimpleCursorAdapter(this, R.layout.element_search_row, null, tFromColumnsSg, tToGuiIt, 0);
        mAdapter.setViewBinder(new SearchViewBinderM());

        //..add it to the ListView contained within this activity
        setListAdapter(mAdapter);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        /*
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        */

        return super.onOptionsItemSelected(item);
    }


    /**
     * Please note that the id for the list items starts at 1, while the ViewPager positions
     * start at 0, therefore we subtract 1 from the id that we receive in this method
     */
    @Override
    public void onListItemClick(ListView iListView, View iView, int iPos, long iId){
        super.onListItemClick(iListView, iView, iPos, iId);

        Log.d(ConstsU.TAG, "onListItemClick, iId = " + iId);

        //Starting a new article activity with the fragment for the chosen article
        /////Uri tUri = Uri.parse(ContentProviderM.ARTICLE_CONTENT_URI + "/" + iId);
        Intent tIntent = new Intent(SearchResultsActivityC.this, ArticleActivityC.class);
        tIntent.putExtra(ConstsU.EXTRA_ARTICLE_POS_ID, iId - 1); /////iId temporarily used, removed "tUri.toString()"
        SearchResultsActivityC.this.startActivityForResult(tIntent, 0);
    }


    @Override
    public Loader<Cursor> onCreateLoader(int iIdUnused, Bundle iArgumentsUnused) {

        String[] tProj = {BaseColumns._ID, ArticleTableM.COLUMN_TITLE, ArticleTableM.COLUMN_TIME_MONTH, ArticleTableM.COLUMN_TIME_DAYOFMONTH, ArticleTableM.COLUMN_TEXT};
        String tSel = ArticleTableM.COLUMN_TEXT + " LIKE ?";
        //-LIKE is case insensitive
        String[] tSelArgs = {"%"+ mSearchStringSg +"%"};
        String tSortOrderSg = "DESC"; //-starting with the latest

        CursorLoader rLoader = new CursorLoader(this, ContentProviderM.ARTICLE_CONTENT_URI,
                tProj, tSel, tSelArgs, tSortOrderSg);

        return rLoader;
    }
    @Override
    public void onLoadFinished(Loader<Cursor> iCursorLoader, Cursor iCursor) {
        mAdapter.swapCursor(iCursor);
    }
    @Override
    public void onLoaderReset(Loader<Cursor> iCursorUnused) {
        mAdapter.swapCursor(null);
    }


    /**
     * ViewBinder for showing the textual context (or preview) for each search hit
     */
    private class SearchViewBinderM
            implements SimpleCursorAdapter.ViewBinder{
        public boolean setViewValue(View iView, Cursor iCursor, int iColIndex){
            int tTitleColIndex = iCursor.getColumnIndexOrThrow(ArticleTableM.COLUMN_TITLE);
            int tTimeMonthColIndex = iCursor.getColumnIndexOrThrow(ArticleTableM.COLUMN_TIME_MONTH);
            int tTimeDayOfMonthColIndex = iCursor.getColumnIndexOrThrow(ArticleTableM.COLUMN_TIME_DAYOFMONTH);
            int tTextColIndex = iCursor.getColumnIndexOrThrow(ArticleTableM.COLUMN_TEXT);

            if(iColIndex == tTitleColIndex) {
                TextView tTextView = (TextView)iView.findViewById(R.id.search_row_title);
                String tTitleSg = UtilitiesU.getPartOfTitleInsideQuotes(iCursor.getString(tTitleColIndex));
                tTextView.setText(Html.fromHtml(tTitleSg));
                tTextView.setTypeface(null, Typeface.BOLD);

                return true;
            }else if(iColIndex == tTimeMonthColIndex){
                TextView tTextView = (TextView)iView.findViewById(R.id.search_row_date_month);

                tTextView.setText(new DateFormatSymbols().getMonths()[iCursor.getInt(tTimeMonthColIndex)] + " ");
                ///tTextView.setTypeface(null, Typeface.ITALIC);

                /*
                long tArticleTimeInMillisLg = iCursor.getLong(tTimeColIndex);

                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(tArticleTimeInMillisLg);

                SimpleDateFormat tSimpleDateFormat = new SimpleDateFormat("MMMM d");

                String tMonthAndDaySg = tSimpleDateFormat.format(c.getTime());
                tTextView.setText(tMonthAndDaySg);
                tTextView.setTypeface(null, Typeface.ITALIC);
                */

                return true;
            }else if(iColIndex == tTextColIndex){
                TextView tTextView = (TextView)iView.findViewById(R.id.search_row_text);

                String tArticleTextSg = iCursor.getString(tTextColIndex);

                //Finding the first instance of the searched for string

                int tFirstMatchingPosition = tArticleTextSg.toLowerCase().indexOf(
                        mSearchStringSg.toLowerCase(), 0);
                //-TODO: Do we want to change the index where the search is started (now set to 0 which includes some html)?
                //-Please note that we need to use toLowerCase() for both Strings to show the correct position in cases where there is a difference between character cases

                int tCharStart = tFirstMatchingPosition - SEARCH_CONTEXT_CHARACTER_PADDING;
                if(tCharStart < 0){tCharStart = 0;}
                int tCharEnd = tFirstMatchingPosition + mSearchStringSg.length() + SEARCH_CONTEXT_CHARACTER_PADDING;
                if(tCharEnd > tArticleTextSg.length()){tCharEnd = tArticleTextSg.length();}

                String tContextTextSubString = iCursor.getString(tTextColIndex).substring(tCharStart, tCharEnd);

                int tFirstSpace = tContextTextSubString.indexOf(" ");
                int tLastSpace = tContextTextSubString.lastIndexOf(" ");

                if(tFirstSpace != -1 && tLastSpace != -1){
                    tContextTextSubString = tContextTextSubString.substring(tFirstSpace + 1, tLastSpace);
                }else{
                    Log.w(ConstsU.TAG, "tFirstSpace = " + tFirstSpace + ", tLastSpace = " + tLastSpace);
                }


                /*
                If we want the searched for word in bold we can use the info here:
                http://stackoverflow.com/questions/14371092/how-to-make-a-specific-text-on-textview-bold
                 */

                tTextView.setText(tContextTextSubString);

                return true;
            }
            return false; //-default
        }
    }
}

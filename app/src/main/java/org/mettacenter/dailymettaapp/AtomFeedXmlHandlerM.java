package org.mettacenter.dailymettaapp;

import android.content.ContentValues;
import android.content.Context;
import android.util.Log;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Used by the SAX parser. This is the code that we ourselves write for doing the actual handling
 * of the content in the atom feed
 */
public class AtomFeedXmlHandlerM
        extends DefaultHandler {

    private static final String ENTRY_XML_TAG = "entry";
    private static final String CONTENT_XML_TAG = "content"; //-Corresponds to COLUMN_TEXT
    private static final String TITLE_XML_TAG = "title"; //-Corresponds to COLUMN_TITLE
    private static final String ID_XML_TAG = "id"; //-Corresponds to COLUMN_LINK
    private static final String PUBLISHED_XML_TAG = "published"; //-Corresponds to COLUMN_TIME

    private boolean mIsElementParsed = false;
    //private String mElementContent = null;
    private ContentValues mInsertValues = new ContentValues();
    private Context mrContext;
    private StringBuilder mElementSb = null;

    public AtomFeedXmlHandlerM(Context irContext){
        mrContext = irContext;
    }

    @Override
    public void startElement(String iUriSg, String iLocalNameSg, String iQNameSg,
            org.xml.sax.Attributes iAttributes) throws SAXException {
        mIsElementParsed = true;
        mElementSb = new StringBuilder();
    }

    //public void characters(char[] ch, int start, int length) throws SAXException {

    /**
     * Surprisingly the characters method can be called several times by the SAX parser, we cannot
     * use "new String(iCharAy, iStartIt, iLengthIt)" and move on, for more info see the topmost
     * answer here:
     * http://stackoverflow.com/questions/2838099/android-sax-parser-not-getting-full-text-from-between-tags
     */
    @Override
    public void characters(char[] iCharAy, int iStartIt, int iLengthIt) throws SAXException{
        if(mIsElementParsed == true){
            //mElementContent = new String(iCharAy, iStartIt, iLengthIt); //iCharAy.length
            if(mElementSb != null){
                for(int i = iStartIt; i < iStartIt + iLengthIt; i++){
                    mElementSb.append(iCharAy[i]);
                }
            }
        }
    }

    @Override
    public void endElement(String iUriSg, String iLocalNameSg, String iQNameSg) throws SAXException{
        mIsElementParsed = false;

        if(CONTENT_XML_TAG.equalsIgnoreCase(iLocalNameSg)){
            mInsertValues.put(ArticleTableM.COLUMN_TEXT, mElementSb.toString());
        }else if(TITLE_XML_TAG.equalsIgnoreCase(iLocalNameSg)){
            mInsertValues.put(ArticleTableM.COLUMN_TITLE, mElementSb.toString());
        }else if(ID_XML_TAG.equalsIgnoreCase(iLocalNameSg)){
            mInsertValues.put(ArticleTableM.COLUMN_LINK, mElementSb.toString());
        }else if(PUBLISHED_XML_TAG.equalsIgnoreCase(iLocalNameSg)){
            long tTimeInMilliSecondsLg = getArticleTimeInMilliSeconds(mElementSb.toString());
            /////mInsertValues.put(ArticleTableM.COLUMN_TIME, tTimeInMilliSecondsLg);
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(tTimeInMilliSecondsLg);
            int tMonthAsInt = c.get(Calendar.MONTH);
            int tDayOfMonthAsInt = c.get(Calendar.DAY_OF_MONTH);
            mInsertValues.put(ArticleTableM.COLUMN_TIME_MONTH, tMonthAsInt);
            mInsertValues.put(ArticleTableM.COLUMN_TIME_DAYOFMONTH, tDayOfMonthAsInt);
        }else if(ENTRY_XML_TAG.equalsIgnoreCase(iLocalNameSg)) {
            Log.d(ConstsU.TAG, "========== END ENTRY TAG ==========");

            //..write what we have to the db
            mrContext.getContentResolver().insert(
                    ContentProviderM.ARTICLE_CONTENT_URI,
                    mInsertValues);
            //..clear the values so we can start anew on another row
            mInsertValues.clear();
            //..update the counter for the number of articles that has been parsed
        }
    }

    private long getArticleTimeInMilliSeconds(String iRawArticleTimeSg){
        long rArticleTimeInMilliSecondsLg = -1;

        String tArticleTimeSg = iRawArticleTimeSg
                .replace("T", " ")
                .replace("Z", "");
        //-TODO: Check with Sky if these replacements are a future-safe thing to do
        Log.d(ConstsU.TAG, "tArticleTimeSg = " + tArticleTimeSg);

        Date tDate;
        SimpleDateFormat tAtomXmlDateFormat = new SimpleDateFormat(ConstsU.ATOM_FEED_TIME_FORMAT);
        tAtomXmlDateFormat.setTimeZone(TimeZone.getTimeZone(ConstsU.SERVER_TIMEZONE));
        try {
            tDate = tAtomXmlDateFormat.parse(tArticleTimeSg);
            rArticleTimeInMilliSecondsLg = tDate.getTime();
            //-This is a Java long, but there is no problem for SQLite because
            //it's Integer is variable in length and can be up to 8 bytes
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Log.d(ConstsU.TAG, "rTimeInMilliSecondsLg = " + rArticleTimeInMilliSecondsLg);

        return rArticleTimeInMilliSecondsLg;
    }
}
package com.example.pradhyumna.newsdisplay;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> titles = new ArrayList<>();
    ArrayList<String> contents = new ArrayList<>();
    ArrayAdapter arrayAdapter;
    ListView listOfNews;
    SQLiteDatabase articlesDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        articlesDB = openOrCreateDatabase("Articles" , MODE_PRIVATE , null);
        articlesDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY , articleId INTEGER , title VARCHAR , content VARCHAR)");

        DownloadTask task = new DownloadTask();

        try {

            //task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        }catch (Exception e){
            e.printStackTrace();
        }
        listOfNews = findViewById(R.id.newsList);
        arrayAdapter = new ArrayAdapter(this , android.R.layout.simple_list_item_1 , titles);
        listOfNews.setAdapter(arrayAdapter);
        listOfNews.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this , Articles.class);
                intent.putExtra("content" , contents.get(position));
                startActivity(intent);
            }
        });
        updateListview();
    }
    public class DownloadTask extends AsyncTask<String , Void , String >{

        @Override
        protected String doInBackground(String... urls) {
            String result = "";
            URL url;
            HttpURLConnection urlConnection;

            try {
                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream stream = urlConnection.getInputStream();
                InputStreamReader streamReader = new InputStreamReader(stream);

                int data = streamReader.read();

                while(data != -1){

                    char dataResult = (char) data;
                    result+=dataResult;
                    data = streamReader.read();
                }

                JSONArray jsonArray = new JSONArray(result);
                int numberOfNews = 20;

                if(jsonArray.length()<20){
                    numberOfNews = jsonArray.length();
                }

                articlesDB.execSQL("DELETE FROM articles");

                for(int i=0 ; i<numberOfNews ; i++){
                    String articleid = jsonArray.getString(i);
                    url = new URL("https://hacker-news.firebaseio.com/v0/item/" + articleid + ".json?print=pretty");
                    urlConnection = (HttpURLConnection) url.openConnection();
                    stream = urlConnection.getInputStream();
                    streamReader = new InputStreamReader(stream);

                    data = streamReader.read();
                    String articleInfo = "";
                    while(data != -1){

                        char dataResult = (char) data;
                        articleInfo+=dataResult;
                        data = streamReader.read();
                    }

                    JSONObject jsonObject = new JSONObject(articleInfo);
                    if(!jsonObject.isNull("title") && !jsonObject.isNull("url")){
                        String articleName = jsonObject.getString("title");
                        String articleURL = jsonObject.getString("url");
                        url = new URL(articleURL);
                        urlConnection = (HttpURLConnection) url.openConnection();
                        stream = urlConnection.getInputStream();
                        streamReader = new InputStreamReader(stream);
                        data = streamReader.read();
                        String articleContent ="";
                        while(data != -1){

                            char dataResult = (char) data;
                            articleContent+=dataResult;
                            data = streamReader.read();
                        }
                        String sql = "INSERT INTO articles (articleID , title , content) VALUES (? , ? , ?)";
                        SQLiteStatement statement = articlesDB.compileStatement(sql);
                        statement.bindString(1 , articleid);
                        statement.bindString(2 , articleName);
                        statement.bindString(3 , articleContent);
                    }
                }


            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            updateListview();
        }
    }
    public void updateListview(){
        Cursor c = articlesDB.rawQuery("SELECT * FROM articles" , null);

        int contentIndex = c.getColumnIndex("content");
        int titleIndex = c.getColumnIndex("title");
        if(c.moveToFirst()){
            titles.clear();
            contents.clear();

            do {

                c.getString(titleIndex);
                c.getString(contentIndex);

            }while (c.moveToNext());
            arrayAdapter.notifyDataSetChanged();
        }
    }
}

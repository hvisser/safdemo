package com.neenbedankt.safdemo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

import nl.qbusict.cupboard.*;

/**
 * Quick (and dirty) demo of the new Storage Access Framework features.
 */
public class MainActivity extends Activity implements OnClickListener {

    private static final int READ_REQUEST_CODE = 1;
    private static final int READ_TEXT_REQUEST_CODE = 2;
    private static final int CREATE_TEXT_REQUEST_CODE = 3;
    private static final int DELETE_REQUEST_CODE = 4;

    private Button mOpenGetContent;
    private Button mOpenDocument;
    private Button mOpenTextDocument;
    private Button mCreateTextDocument;

    private TextView mText;
    private Cupboard mDocumentsCupboard;

    private static final class ReadTextTask extends AsyncTask<Void, Void, String> {
        private static final String TAG = "ReadTextTask";

        private Context mContext;
        private TextView mTextView;
        private Uri mUri;

        public ReadTextTask(Context context, Uri uri, TextView textView) {
            this.mContext = context;
            this.mUri = uri;
            this.mTextView = textView;
        }

        @Override
        protected String doInBackground(Void... params) {
            StringBuilder sb = new StringBuilder(1024);
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(mContext.getContentResolver().openInputStream(mUri), "UTF-8"));
                for (int i = 0; i < 5; i++) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    sb.append(line);
                    sb.append("\n");
                }
                reader.close();
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Could not open uri", e);
            } catch (IOException e) {
                Log.e(TAG, "Exception while reading stream", e);
                // continue anyway...
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                    }
                }
            }
            return sb.toString();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            mTextView.setText(s);
        }
    }

    private static final class WriteTextTask extends AsyncTask<String, Void, Void> {
        private final Context mContext;
        private final Uri mUri;

        public WriteTextTask(Context context, Uri uri) {
            this.mContext = context;
            this.mUri = uri;
        }

        @Override
        protected Void doInBackground(String... params) {
            BufferedWriter writer = null;
            try {
                writer = new BufferedWriter(new OutputStreamWriter(mContext.getContentResolver().openOutputStream(mUri, "w"), "UTF-8"), 8192);
                writer.write(params[0]);
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e) {
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Toast.makeText(mContext, "Written!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mOpenGetContent = (Button) findViewById(R.id.open_get);
        mOpenDocument = (Button) findViewById(R.id.open_document);
        mOpenTextDocument = (Button) findViewById(R.id.open_text_document);
        mCreateTextDocument = (Button) findViewById(R.id.create_text_document);
        mText = (TextView) findViewById(R.id.text);
        mOpenGetContent.setOnClickListener(this);
        mOpenDocument.setOnClickListener(this);
        mOpenTextDocument.setOnClickListener(this);
        mCreateTextDocument.setOnClickListener(this);
        mDocumentsCupboard = new Cupboard();
        mDocumentsCupboard.register(Document.class);
    }

    @Override
    public void onClick(View v) {
        if (v == mOpenGetContent) {
            openWithGetContent();
        } else if (v == mOpenDocument) {
            openWithGetDocument();
        } else if (v == mOpenTextDocument) {
            openTextDocument();
        } else if (v == mCreateTextDocument) {
            mCreateTextDocument();
        }
    }

    private void mCreateTextDocument() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        //intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_TITLE, "DutchAug");
        intent.setType("text/plain");
        startActivityForResult(intent, CREATE_TEXT_REQUEST_CODE);
    }

    private void openTextDocument() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        startActivityForResult(intent, READ_TEXT_REQUEST_CODE);
    }

    private void openWithGetDocument() {
        launchPicker(Intent.ACTION_OPEN_DOCUMENT);
    }

    private void openWithGetContent() {
        launchPicker(Intent.ACTION_GET_CONTENT);
    }

    private void launchPicker(String action) {
        Intent intent = new Intent(action);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == READ_TEXT_REQUEST_CODE) {
                // get the document uri
                Uri uri = data.getData();
                // In this demo, we don't hold on to selected files, but if we did this is how the
                // read permission is persisted
                int takeFlags = data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION;
                getContentResolver().takePersistableUriPermission(uri, takeFlags);
                new ReadTextTask(this, uri, mText).execute();
            } else if (requestCode == CREATE_TEXT_REQUEST_CODE) {
                Uri uri = data.getData();
                new WriteTextTask(this, uri).execute("wow\n  such community\n  much android\n\nvery demo");
            } else if (requestCode == DELETE_REQUEST_CODE) {
                tryDelete(data.getData());
            }
        }
    }

    private void tryDelete(Uri uri) {
        Document doc = mDocumentsCupboard.withContext(this).get(uri, Document.class);
        if ((doc.flags & DocumentsContract.Document.FLAG_SUPPORTS_DELETE) == DocumentsContract.Document.FLAG_SUPPORTS_DELETE) {
            if (DocumentsContract.deleteDocument(getContentResolver(), uri)) {
                Toast.makeText(this, "Deleted!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Failed to delete", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Delete is not supported", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete:
                requestDeleteDocument();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void requestDeleteDocument() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, DELETE_REQUEST_CODE);
    }
}

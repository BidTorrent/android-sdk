package com.bidtorrent.biddingservice.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.bidtorrent.biddingservice.BiddingIntentService;
import com.bidtorrent.biddingservice.Constants;
import com.bidtorrent.biddingservice.prefetching.JavaScriptReadyListener;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class PrefetchReceiver extends BroadcastReceiver {

    private final Handler handler;
    private static final String jsListenerName = "jsListener";
    private static final String loadingTrackingJavascript = getloadingTrackingJavascript(jsListenerName);

    private static String getloadingTrackingJavascript(String jsListenerName)
    {
        StringBuilder sb = new StringBuilder();

        sb.append("window.onload = function()")
                .append("{")
                .append("for (var i = 0; i < document.images.length; i++) {")
                .append("  if (document.images[i].naturalWidth == 0)")
                .append("  {")
                .append("    ").append(jsListenerName).append(".notifyLoadingError();")
                .append("    return;")
                .append("  }")
                .append("}")
                .append(jsListenerName).append(".notifyLoaded();")
                .append("}");

        return sb.toString();
    }

    public PrefetchReceiver(Handler handler) {
        this.handler = handler;
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        Bundle extras = intent.getExtras();
        String creative = extras.getString(Constants.CREATIVE_CODE_ARG);
        final WebView webView = new WebView(context);
        final String instrumentedCreativeCode;
        final JavaScriptReadyListener jsListener = new JavaScriptReadyListener(new Runnable() {
            @Override
            public void run() {
                sendPrefetchedData(context, webView, intent);
            }
        }, new Runnable() {
            @Override
            public void run() {
                sendEvent(null, context, intent, Constants.PREFETCH_FAILED_ACTION);
            }
        });

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.addJavascriptInterface(jsListener, jsListenerName);

        instrumentedCreativeCode = this.injectLoadingTrackingJavascriptCode(creative);
        if (instrumentedCreativeCode == null)
        {
            // FIXME: do something, Bobby!
            return;
        }

        webView.loadData(instrumentedCreativeCode, "text/html", "utf-8");
    }

    private void sendPrefetchedData(final Context context, final WebView webView, final Intent intent) {
        final File tempFile;

        try {
            tempFile = File.createTempFile("bidtorrent", ".mht", context.getCacheDir());
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        handler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        webView.saveWebArchive(tempFile.getAbsolutePath(),
                                false, new ValueCallback<String>() {
                                    @Override
                                    public void onReceiveValue(String value) {
                                        sendEvent(value, context, intent, Constants.FILL_PREFETCH_BUFFER_ACTION);
                                    }
                                });
                    }
                });
    }

    private static void sendEvent(String value, Context context, Intent intent, String action) {
        Intent auctionIntent;
        auctionIntent = new Intent(context, BiddingIntentService.class);
        auctionIntent.setAction(action);
        auctionIntent.putExtra(Constants.PREFETCHED_CREATIVE_FILE_ARG, value)
                .putExtra(Constants.IMPRESSION_ID_ARG,
                        intent.getStringExtra(Constants.IMPRESSION_ID_ARG))
                .putExtra(Constants.NOTIFICATION_URL_ARG,
                        intent.getStringExtra(Constants.NOTIFICATION_URL_ARG))
                .putExtra(Constants.AUCTION_ID_ARG,
                        intent.getLongExtra(Constants.AUCTION_ID_ARG, -1));


        context.startService(auctionIntent);
    }

    private String injectLoadingTrackingJavascriptCode(String creativeCode) {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder;
        Document parsedDocument;
        InputSource is = new InputSource();
        Node scriptNode;

        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            return null;
        }

        is.setCharacterStream(new StringReader(creativeCode));

        try {
            parsedDocument = documentBuilder.parse(is);
        } catch (SAXException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        scriptNode = this.createScriptNode(parsedDocument);

        if (scriptNode == null)
            return null;

        scriptNode.setTextContent(loadingTrackingJavascript);

        return xmlToString(parsedDocument);
    }

    private static String xmlToString(Document doc)
    {
        DOMSource source = new DOMSource(doc);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer;
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);

        try {
            transformer = tf.newTransformer();
            transformer.transform(source, result);
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }

        return writer.toString();
    }

    private Node createScriptNode(Document parsedDocument) {
        Node headElement;

        headElement = this.getHeadElement(parsedDocument);
        if (headElement == null)
            return null;

        return headElement.appendChild(parsedDocument.createElement("script"));
    }

    private Node getHeadElement(Document parsedDocument) {
        NodeList headElements = parsedDocument.getElementsByTagName("head");

        // No head element, try to create it
        if (headElements.getLength() == 0)
        {
            NodeList htmlElements = parsedDocument.getElementsByTagName("html");

            // It's not even HTML, rage quit!
            if (htmlElements.getLength() == 0)
                return null;

            return htmlElements.item(0).appendChild(parsedDocument.createElement("head"));
        }
        else
            return headElements.item(0);
    }
}

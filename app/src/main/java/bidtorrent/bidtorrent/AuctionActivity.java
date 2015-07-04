package bidtorrent.bidtorrent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

import com.bidtorrent.bidding.BidOpportunity;
import com.bidtorrent.bidding.Size;
import com.bidtorrent.biddingservice.AdReadyReceiver;
import com.bidtorrent.biddingservice.BiddingIntentService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class AuctionActivity extends ActionBarActivity {
    private Button bidButton;
    private TextView debugView;
    private WebView webView;
    private BroadcastReceiver bidAvailableReceiver;
    private BroadcastReceiver auctionErrorReceiver;

    private BroadcastReceiver createBidAvailableReceiver()
    {
        return new AdReadyReceiver(debugView, webView);
    }

    private BroadcastReceiver createAuctionErrorReceiver()
    {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle extras = intent.getExtras();

                debugView.append(String.format(
                        "The auction failed: %s\n", extras.getString(BiddingIntentService.AUCTION_ERROR_REASON_ARG)));
            }
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auction);
        bidButton = (Button)findViewById(R.id.bidButton);
        debugView = (TextView)findViewById(R.id.auctionDebug);
        this.webView = (WebView)findViewById(R.id.webView);

        bidButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runAuction();
            }
        });

        this.auctionErrorReceiver = this.createAuctionErrorReceiver();
        this.bidAvailableReceiver = this.createBidAvailableReceiver();
        registerReceiver(this.bidAvailableReceiver, new IntentFilter(BiddingIntentService.AUCTION_RESULT_AVAILABLE));
        registerReceiver(this.auctionErrorReceiver, new IntentFilter(BiddingIntentService.AUCTION_FAILED));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(bidAvailableReceiver);
        this.unregisterReceiver(auctionErrorReceiver);

    }

    private void runAuction()
    {
        Intent auctionIntent = new Intent(this, BiddingIntentService.class);

        Gson gson;
        BidOpportunity opp;

        opp = new BidOpportunity(new Size(300, 250), "bidtorrent.dummy.app");
        gson = new GsonBuilder().create();
        startService(auctionIntent.putExtra(BiddingIntentService.REQUEST_ARG_NAME, gson.toJson(opp)));

        this.debugView.setText("Running the auction...\n");
    }

}

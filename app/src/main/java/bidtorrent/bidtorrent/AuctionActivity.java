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
import com.bidtorrent.bidding.Notificator;
import com.bidtorrent.bidding.PooledHttpClient;
import com.bidtorrent.bidding.Size;
import com.bidtorrent.biddingservice.Constants;
import com.bidtorrent.biddingservice.receivers.CreativeDisplayReceiver;
import com.bidtorrent.biddingservice.BiddingIntentService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class AuctionActivity extends ActionBarActivity {
    private Button bidButton;
    private TextView debugView;
    private WebView webView;
    private BroadcastReceiver auctionErrorReceiver;
    private BroadcastReceiver displayReceiver;

    //FIXME: Can this be the point of entry for our library?
    private BroadcastReceiver createDisplayReceiver()
    {
        return new CreativeDisplayReceiver(webView, 4242,
                new Notificator(10000, new PooledHttpClient(10000)));
    }

    private BroadcastReceiver createAuctionErrorReceiver()
    {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle extras = intent.getExtras();

                debugView.append(String.format(
                        "The auction failed: %s\n", extras.getString(Constants.AUCTION_ERROR_REASON_ARG)));
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
        this.displayReceiver = this.createDisplayReceiver();
        registerReceiver(this.displayReceiver, new IntentFilter(Constants.READY_TO_DISPLAY_AD_INTENT));
        registerReceiver(this.auctionErrorReceiver, new IntentFilter(Constants.AUCTION_FAILED_INTENT));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(auctionErrorReceiver);
        this.unregisterReceiver(displayReceiver);
    }

    private void runAuction()
    {
        Intent auctionIntent = new Intent(this, BiddingIntentService.class);
        int requesterId = 4242;
        Gson gson;
        BidOpportunity opp;

        opp = new BidOpportunity(new Size(300, 250), "bidtorrent.dummy.app");
        gson = new GsonBuilder().create();
        auctionIntent.setAction(Constants.BID_ACTION);
        auctionIntent.putExtra(Constants.REQUESTER_ID_ARG, requesterId);
        startService(auctionIntent.putExtra(Constants.BID_OPPORTUNITY_ARG, gson.toJson(opp)));
    }

}

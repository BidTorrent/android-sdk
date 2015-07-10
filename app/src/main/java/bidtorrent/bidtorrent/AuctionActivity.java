package bidtorrent.bidtorrent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
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
import com.bidtorrent.biddingservice.receivers.PrefetchReceiver;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class AuctionActivity extends ActionBarActivity {
    private Button bidButton;
    private TextView debugView;
    private WebView webView;
    private BroadcastReceiver auctionErrorReceiver;
    private BroadcastReceiver displayReceiver;
    private BroadcastReceiver prefetchReceiver;

    //FIXME: Can this be the point of entry for our library?
    private BroadcastReceiver createDisplayReceiver()
    {
        return new CreativeDisplayReceiver(webView, 4242, null );
                //new Notificator(10000, new PooledHttpClient(10000, true)));
        //FIXME: notifications moved to service
    }

    private BroadcastReceiver createPrefetchReceiver()
    {
        return new PrefetchReceiver(new Handler(this.getMainLooper()));
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

        this.bidButton = (Button)findViewById(R.id.bidButton);
        this.debugView = (TextView)findViewById(R.id.auctionDebug);
        this.webView = (WebView)findViewById(R.id.webView);
        this.auctionErrorReceiver = this.createAuctionErrorReceiver();
        this.displayReceiver = this.createDisplayReceiver();
        this.prefetchReceiver = this.createPrefetchReceiver();

        bidButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runAuction();
            }
        });

        registerReceiver(this.displayReceiver, new IntentFilter(Constants.READY_TO_DISPLAY_AD_INTENT));
        registerReceiver(this.auctionErrorReceiver, new IntentFilter(Constants.AUCTION_FAILED_INTENT));
        registerReceiver(this.prefetchReceiver, new IntentFilter(Constants.BID_AVAILABLE_INTENT));
        startService(new Intent(this, BiddingIntentService.class));
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();

        this.unregisterReceiver(this.prefetchReceiver);
        this.unregisterReceiver(this.auctionErrorReceiver);
        this.unregisterReceiver(this.displayReceiver);
    }

    private void runAuction()
    {
        Intent auctionIntent;
        BidOpportunity opp;
        int requesterId = 4242;

        opp = new BidOpportunity(new Size(300, 250), "bidtorrent.dummy.app");

        auctionIntent = new Intent(this, BiddingIntentService.class)
            .setAction(Constants.BID_ACTION)
            .putExtra(Constants.REQUESTER_ID_ARG, requesterId)
            .putExtra(Constants.BID_OPPORTUNITY_ARG, new GsonBuilder().create().toJson(opp));

        this.startService(auctionIntent);
    }

}

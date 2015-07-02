package bidtorrent.bidtorrent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.bidtorrent.bidding.BidOpportunity;
import com.bidtorrent.biddingservice.BiddingIntentService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Locale;

public class AuctionActivity extends ActionBarActivity {
    private Button bidButton;
    private TextView debugView;

    private BroadcastReceiver createBidAvailableReceiver()
    {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle extras = intent.getExtras();

                debugView.append(String.format(Locale.getDefault(), "Price: %.2f\n", extras.getFloat("price")));
                debugView.append(String.format(Locale.getDefault(), "BiddingPrice: %.2f\n", extras.getFloat("biddingPrice")));
            }
        };
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

        bidButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runAuction();
            }
        });
        registerReceiver(this.createBidAvailableReceiver(), new IntentFilter(BiddingIntentService.AUCTION_RESULT_AVAILABLE));
        registerReceiver(this.createAuctionErrorReceiver(), new IntentFilter(BiddingIntentService.AUCTION_FAILED));
    }

    private void runAuction()
    {
        Intent auctionIntent = new Intent(this, BiddingIntentService.class);
        Gson gson;
        BidOpportunity opp;

        opp = new BidOpportunity(300, 250, "bidtorrent.dummy.app");
        gson = new GsonBuilder().create();
        startService(auctionIntent.putExtra(BiddingIntentService.REQUEST_ARG_NAME, gson.toJson(opp)));

        this.debugView.setText("Running the auction...\n");
    }

}

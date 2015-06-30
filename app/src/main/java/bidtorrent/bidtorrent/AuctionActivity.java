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

import com.bidtorrent.bidding.Auction;
import com.bidtorrent.bidding.AuctionResult;
import com.bidtorrent.bidding.Auctioneer;
import com.bidtorrent.bidding.BidOpportunity;
import com.bidtorrent.bidding.BidResponse;
import com.bidtorrent.bidding.ConstantBidder;
import com.bidtorrent.bidding.HttpBidder;
import com.bidtorrent.bidding.IBidder;
import com.bidtorrent.bidding.JsonResponseConverter;
import com.bidtorrent.biddingservice.BiddingIntentService;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AuctionActivity extends ActionBarActivity {
    private Button bidButton;
    private TextView debugView;

    private BroadcastReceiver createBidAvailableReceiver()
    {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle extras = intent.getExtras();

                if (!extras.getBoolean("success"))
                    debugView.append("The auction failed :-(\n");
                else
                {
                    debugView.append(String.format(Locale.getDefault(), "Price: %.2f\n", extras.getFloat("price")));
                }
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
        registerReceiver(this.createBidAvailableReceiver(), new IntentFilter(BiddingIntentService.BID_RESPONSE_AVAILABLE));
    }

    private void runAuction()
    {
        startService(new Intent(this, BiddingIntentService.class));

        this.debugView.setText("Running the auction...\n");
    }

    /*private void bc()
    {
        if (auctionResult == null)
            this.debugView.append("The auction failed");
        else
        {
            if (auctionResult.getWinningBidder() == null)
                this.debugView.append("Noone won :-(");
            else
            {
                this.debugView.append("Bidder: " + String.valueOf(auctionResult.getWinningBidder().getId()) + "\n");
                this.debugView.append("Price: " + String.valueOf(auctionResult.getWinningPrice()) + "\n");
            };
        }
    }*/
}

package bidtorrent.bidtorrent;

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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AuctionActivity extends ActionBarActivity {
    private Button bidButton;
    private TextView debugView;

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
    }

    private void runAuction()
    {
        Auctioneer auctioneer;
        List<IBidder> bidders;
        Future<AuctionResult> auctionResultFuture;
        AuctionResult auctionResult = null;

        this.debugView.setText("Running the auction...\n");

        auctioneer = new Auctioneer(100000);
        bidders = new ArrayList<>();

        bidders.add(new ConstantBidder(4, new BidResponse(4, 0.3f, "CREATIVE", "NOTIFYME")));
        bidders.add(new ConstantBidder(3, new BidResponse(3, 0.4f, "CREATIVETHESHIT", "NOTIFYMENOT")));

        bidders.add(new HttpBidder(1, "Kitten", URI.create("http://adlb.me/bidder/bid.php?bidder=pony"), new JsonResponseConverter(), 50000));
        bidders.add(new HttpBidder(2, "Criteo", URI.create("http://adlb.me/bidder/bid.php?bidder=criteo"), new JsonResponseConverter(), 50000));

        auctionResultFuture = auctioneer.runAuction(new Auction(new BidOpportunity(URI.create("http://perdu.com")), bidders, 0.5f));

        try {
            auctionResult = auctionResultFuture.get(100000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            this.debugView.append("Interrupted");
        } catch (ExecutionException e) {
            this.debugView.append("Exec exc\n");
            this.debugView.append(e.getMessage() + "\n");
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            this.debugView.append(sw.toString());
        } catch (TimeoutException e) {
            this.debugView.append("Timeout");
        }

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
    }
}

package bidtorrent.bidtorrent;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;

import com.bidtorrent.biddingservice.api.BidTorrentHandler;

public class AuctionActivity extends ActionBarActivity {
    private Button bidButton;
    private WebView webView;
    private FrameLayout debugLayout;
    private BidTorrentHandler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_auction);

        this.bidButton = (Button)findViewById(R.id.bidButton);
        this.webView = (WebView)findViewById(R.id.webView);
        this.debugLayout = (FrameLayout)findViewById(R.id.debugLayout);

        this.handler = BidTorrentHandler.createHandler(this, this.webView, this.debugLayout);

        this.bidButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handler.runAuction();
            }
        });
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        this.handler.onDestroy();
    }
}

package fiware.smartcity.marketplace;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.Scene;
import android.transition.TransitionManager;
import android.view.ViewGroup;
import android.webkit.WebView;

import fiware.smartcity.Application;
import fiware.smartcity.MainActivity;
import fiware.smartcity.R;

/**
 * Created by francisco on 29/03/16.
 */
public class MarketActivity {
    private Context context;

    private static Activity activity;

    private Drawable x, y;

    public MarketActivity(Context ctx) {
        context = ctx;
        activity = Application.mainActivity;

        x = activity.getResources().getDrawable(R.drawable.clear);
        x.setBounds(0, 0,50, 50);

        y = activity.getResources().getDrawable(R.drawable.search);
        y.setBounds(0, 0, 50, 50);
    }

    public void start() {
        ViewGroup rootContainer = (ViewGroup) activity.findViewById(R.id.mainFrame);
        activity.getLayoutInflater().inflate(R.layout.route, rootContainer);

        ViewGroup routeContainer = (ViewGroup) activity.findViewById(R.id.frameRoute);
        Scene scene1 = Scene.getSceneForLayout(routeContainer, R.layout.activity_market, activity);

        TransitionManager.go(scene1);

        WebView webView = (WebView) activity.findViewById(R.id.market);
        webView.loadUrl("https://demo-mwc.conwet.com/");
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

        setupHeader();
    }

    public void back() {
        ((MainActivity)activity).onMarketplaceClosed();
    }

    private void setupHeader() {
        Toolbar myToolbar = (Toolbar) activity.findViewById(R.id.my_toolbar);
        ((AppCompatActivity)activity).setSupportActionBar(myToolbar);

        ((AppCompatActivity)activity).getSupportActionBar().setTitle("Marketplace");
        ((AppCompatActivity)activity).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
}

package javaguide.ws.controllers;

//#ws-oauth-controller
import play.libs.F.Promise;
import play.libs.oauth.OAuth;
import play.libs.oauth.OAuth.ConsumerKey;
import play.libs.oauth.OAuth.OAuthCalculator;
import play.libs.oauth.OAuth.RequestToken;
import play.libs.oauth.OAuth.ServiceInfo;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;
import play.mvc.Controller;
import play.mvc.Result;

import com.google.common.base.Strings;

import javax.inject.Inject;
import java.util.Optional;

public class Twitter extends Controller {
  static final ConsumerKey KEY = new ConsumerKey("...", "...");

  private static final ServiceInfo SERVICE_INFO = new ServiceInfo("https://api.twitter.com/oauth/request_token",
                                                                  "https://api.twitter.com/oauth/access_token",
                                                                  "https://api.twitter.com/oauth/authorize", 
                                                                  KEY);
  
  private static final OAuth TWITTER = new OAuth(SERVICE_INFO);

  private final WSClient ws;

  @Inject
  public Twitter(WSClient ws) {
    this.ws = ws;
  }

  public Promise<Result> homeTimeline() {
    Optional<RequestToken> sessionTokenPair = getSessionTokenPair();
    if (sessionTokenPair.isPresent()) {
      return ws.url("https://api.twitter.com/1.1/statuses/home_timeline.json")
          .sign(new OAuthCalculator(Twitter.KEY, sessionTokenPair.get()))
          .get()
          .map(result -> ok(result.asJson()));
    }
    return Promise.pure(redirect(routes.Twitter.auth()));
  }
  
  public Result auth() {
    String verifier = request().getQueryString("oauth_verifier");
    if (Strings.isNullOrEmpty(verifier)) {
      String url = routes.Twitter.auth().absoluteURL(request());
      RequestToken requestToken = TWITTER.retrieveRequestToken(url);
      saveSessionTokenPair(requestToken);
      return redirect(TWITTER.redirectUrl(requestToken.token));
    } else {
      RequestToken requestToken = getSessionTokenPair().get();
      RequestToken accessToken = TWITTER.retrieveAccessToken(requestToken, verifier);
      saveSessionTokenPair(accessToken);
      return redirect(routes.Twitter.homeTimeline());
    }
  }

  private void saveSessionTokenPair(RequestToken requestToken) {
    session("token", requestToken.token);
    session("secret", requestToken.secret);
  }

  private Optional<RequestToken> getSessionTokenPair() {
    if (session().containsKey("token")) {
      return Optional.ofNullable(new RequestToken(session("token"), session("secret")));
    }
    return Optional.empty();
  }
  
}
//#ws-oauth-controller
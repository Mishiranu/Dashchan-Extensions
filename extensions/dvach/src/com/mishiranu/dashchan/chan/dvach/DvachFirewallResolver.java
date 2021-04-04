package com.mishiranu.dashchan.chan.dvach;

import android.net.Uri;
import android.util.Pair;
import chan.http.CookieBuilder;
import chan.http.FirewallResolver;
import chan.http.HttpException;
import chan.http.HttpResponse;
import chan.util.StringUtils;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DvachFirewallResolver extends FirewallResolver {
	private static final String COOKIE_DVACH_FIREWALL = "dvach_firewall";

	private static class WebViewClient extends FirewallResolver.WebViewClient<Pair<String, String>> {
		public WebViewClient() {
			super("DvachFirewall");
		}

		@Override
		public boolean onPageFinished(Uri uri, Map<String, String> cookies, String title) {
			if ("Проверка...".equals(title)) {
				return false;
			} else {
				for (Map.Entry<String, String> entry : cookies.entrySet()) {
					String value = entry.getValue();
					boolean isUuid;
					try {
						isUuid = UUID.fromString(value) != null;
					} catch (Exception e) {
						isUuid = false;
					}
					if (isUuid) {
						setResult(new Pair<>(entry.getKey(), value));
						break;
					}
				}
				return true;
			}
		}

		@Override
		public boolean onLoad(Uri initialUri, Uri uri) {
			String initialPath = StringUtils.emptyIfNull(initialUri.getPath());
			String path = StringUtils.emptyIfNull(uri.getPath());
			return initialPath.equals(path) || path.equals("/") || path.endsWith(".js");
		}
	}

	private static class Exclusive implements FirewallResolver.Exclusive {
		@Override
		public boolean resolve(Session session, Key key) throws CancelException, InterruptedException {
			Pair<String, String> pair = session.resolveWebView(new WebViewClient());
			if (pair != null) {
				DvachChanConfiguration configuration = session.getChanConfiguration();
				configuration.storeCookie(key.formatKey(COOKIE_DVACH_FIREWALL),
						pair.first + "=" + pair.second, key.formatTitle("Dvach Firewall"));
				return true;
			}
			return false;
		}
	}

	private static Exclusive.Key toKey(Session session) {
		return session.getKey(Identifier.Flag.USER_AGENT);
	}

	@Override
	public CheckResponseResult checkResponse(Session session, HttpResponse response) throws HttpException {
		List<String> contentType = response.getHeaderFields().get("Content-Type");
		if (contentType == null || contentType.isEmpty() || contentType.get(0).startsWith("text/html")) {
			String responseText = response.readString();
			if (responseText != null && responseText.contains("<title>Проверка...</title>")) {
				// Firewall redirects to /, restore original URI
				List<Uri> uris = response.getRequestedUris();
				for (int i = uris.size() - 1; i >= 0; i--) {
					Uri uri = uris.get(i);
					if (!"/".equals(uri.getPath())) {
						if (i < uris.size() - 1) {
							response.setRedirectedUri(uri);
						}
						break;
					}
				}
				return new CheckResponseResult(toKey(session), new Exclusive())
						.setRetransmitOnSuccess(true);
			}
		}
		return null;
	}

	@Override
	public void collectCookies(Session session, CookieBuilder cookieBuilder) {
		DvachChanConfiguration configuration = session.getChanConfiguration();
		FirewallResolver.Exclusive.Key key = toKey(session);
		String cookie = configuration.getCookie(key.formatKey(COOKIE_DVACH_FIREWALL));
		if (cookie != null) {
			String[] keyValue = cookie.split("=", 2);
			if (keyValue.length == 2) {
				cookieBuilder.append(keyValue[0], keyValue[1]);
			}
		}
	}
}

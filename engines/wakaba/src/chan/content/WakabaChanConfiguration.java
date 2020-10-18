package chan.content;

public class WakabaChanConfiguration extends ChanConfiguration {
	public static final String CAPTCHA_TYPE_WAKABA = "wakaba";

	public WakabaChanConfiguration() {
		addCaptchaType(CAPTCHA_TYPE_WAKABA);
	}

	@Override
	public Captcha obtainCustomCaptchaConfiguration(String captchaType) {
		if (CAPTCHA_TYPE_WAKABA.equals(captchaType)) {
			Captcha captcha = new Captcha();
			captcha.title = "Wakaba";
			captcha.input = Captcha.Input.LATIN;
			captcha.validity = Captcha.Validity.IN_THREAD;
			return captcha;
		}
		return null;
	}
}

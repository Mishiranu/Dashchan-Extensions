package com.mishiranu.dashchan.chan.arhivach;

import android.content.res.Resources;
import android.util.Pair;
import chan.content.ChanConfiguration;

public class ArhivachChanConfiguration extends ChanConfiguration {
	public ArhivachChanConfiguration() {
		request(OPTION_SINGLE_BOARD_MODE);
		request(OPTION_ALLOW_USER_AUTHORIZATION);
		setSingleBoardName(null);
		setBoardTitle(null, "Архивач");
		setDefaultName("Аноним");
		// TODO Fix captcha
		// addCaptchaType(CAPTCHA_TYPE_RECAPTCHA_1);
	}

	@Override
	public Board obtainBoardConfiguration(String boardName) {
		Board board = new Board();
		board.allowSearch = true;
		return board;
	}

	@Override
	public Authorization obtainUserAuthorizationConfiguration() {
		Resources resources = getResources();
		Authorization authorization = new Authorization();
		authorization.fieldsCount = 2;
		authorization.hints = new String[2];
		authorization.hints[0] = "Email";
		authorization.hints[1] = resources.getString(R.string.text_password);
		return authorization;
	}

	@Override
	public Archivation obtainArchivationConfiguration() {
		Resources resources = getResources();
		Archivation archivation = new Archivation();
		archivation.hosts.add("2ch.hk");
		archivation.hosts.add("iichan.hk");
		archivation.hosts.add("brchan.org");
		archivation.options.add(new Pair<>("collapsed", resources.getString(R.string.text_collapsed)));
		archivation.options.add(new Pair<>("bytoken", resources.getString(R.string.text_bytoken)));
		return archivation;
	}

	@Override
	public Statistics obtainStatisticsConfiguration() {
		Statistics statistics = new Statistics();
		statistics.postsSent = false;
		statistics.threadsCreated = false;
		return statistics;
	}
}

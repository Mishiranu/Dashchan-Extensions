package com.mishiranu.dashchan.chan.kropyvach;

import java.util.Arrays;
import java.util.List;

import chan.content.ChanConfiguration;

public class KropyvachChanConfiguration extends ChanConfiguration
{
	private static final List<String> DEFAULT_NAMES = Arrays.asList("Безйменний", "Незнайомець", "Перехожий",
			"Хтось", "Халамидник", "Роззява", "Проходько", "Ґаволов", "Гаврик", "Невідомий", "Пічкур", "Безос",
			"Жевжик", "Анон");

	public KropyvachChanConfiguration()
	{
		request(OPTION_READ_POSTS_COUNT);
		setDefaultName(DEFAULT_NAMES.get(0));
	}

	public boolean isDefaultName(String name)
	{
		return DEFAULT_NAMES.contains(name);
	}

	@Override
	public Board obtainBoardConfiguration(String boardName)
	{
		Board board = new Board();
		board.allowCatalog = true;
		board.allowPosting = true;
		board.allowDeleting = true;
		board.allowReporting = true;
		return board;
	}

	@Override
	public Posting obtainPostingConfiguration(String boardName, boolean newThread)
	{
		Posting posting = new Posting();
		posting.allowName = true;
		posting.allowTripcode = true;
		posting.allowEmail = true;
		posting.allowSubject = true;
		posting.optionSage = true;
		posting.attachmentCount = 4;
		posting.attachmentMimeTypes.add("image/*");
		posting.attachmentSpoiler = true;
		return posting;
	}

	@Override
	public Deleting obtainDeletingConfiguration(String boardName)
	{
		Deleting deleting = new Deleting();
		deleting.password = true;
		deleting.multiplePosts = true;
		deleting.optionFilesOnly = true;
		return deleting;
	}

	@Override
	public Reporting obtainReportingConfiguration(String boardName)
	{
		Reporting reporting = new Reporting();
		reporting.comment = true;
		reporting.multiplePosts = true;
		return reporting;
	}
}
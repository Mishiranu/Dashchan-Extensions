package com.mishiranu.dashchan.chan.taima;

import android.content.res.Resources;
import android.util.Pair;
import chan.content.ChanConfiguration;

public class TaimaChanConfiguration extends ChanConfiguration
{
	private static final String KEY_NAMES_ENABLED = "names_enabled";
	
	public TaimaChanConfiguration()
	{
		request(OPTION_READ_POSTS_COUNT);
		setDefaultName("Anonymous");
	}
	
	@Override
	public Board obtainBoardConfiguration(String boardName)
	{
		Board board = new Board();
		board.allowCatalog = true;
		board.allowPosting = true;
		board.allowReporting = true;
		return board;
	}
	
	@Override
	public Posting obtainPostingConfiguration(String boardName, boolean newThread)
	{
		Posting posting = new Posting();
		posting.allowName = get(boardName, KEY_NAMES_ENABLED, true);
		posting.allowTripcode = true;
		posting.allowSubject = newThread;
		posting.optionSage = true;
		posting.attachmentCount = 1;
		posting.attachmentMimeTypes.add("image/*");
		return posting;
	}
	
	@Override
	public Reporting obtainReportingConfiguration(String boardName)
	{
		Resources resources = getResources();
		Reporting reporting = new Reporting();
		reporting.comment = true;
		reporting.types.add(new Pair<>("RULE_VIOLATION", resources.getString(R.string.text_rule)));
		reporting.types.add(new Pair<>("ILLEGAL_CONTENT", resources.getString(R.string.text_illegal)));
		return reporting;
	}
	
	public void storeNamesEnabled(String boardName, boolean namesEnabled)
	{
		set(boardName, KEY_NAMES_ENABLED, namesEnabled);
	}
}
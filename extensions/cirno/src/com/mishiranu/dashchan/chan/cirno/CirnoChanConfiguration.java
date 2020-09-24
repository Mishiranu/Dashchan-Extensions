package com.mishiranu.dashchan.chan.cirno;

import chan.content.WakabaChanConfiguration;

public class CirnoChanConfiguration extends WakabaChanConfiguration {
	private static final String KEY_IMAGES_ENABLED = "images_enabled";
	private static final String KEY_EMAILS_ENABLED = "emails_enabled";
	private static final String KEY_NAMES_ENABLED = "names_enabled";
	private static final String KEY_IMAGE_SPOILERS_ENABLED = "image_spoilers_enabled";

	public CirnoChanConfiguration() {
		setDefaultName("Аноним");
		setDefaultName("an", "Кот Синкая");
		setDefaultName("au", "Джереми Кларксон");
		setDefaultName("b", "Сырно");
		setDefaultName("bro", "Эпплджек");
		setDefaultName("l", "Ф. М. Достоевский");
		setDefaultName("m", "Копипаста-гей");
		setDefaultName("maid", "Госюдзин-сама");
		setDefaultName("med", "Антон Буслов");
		setDefaultName("mi", "Й. Швейк");
		setDefaultName("mu", "Виктор Цой");
		setDefaultName("ne", "Пушок");
		setDefaultName("p", "Б. В. Грызлов");
		setDefaultName("s", "Чии");
		setDefaultName("sci", "Гриша Перельман");
		setDefaultName("sp", "Спортакус");
		setDefaultName("tran", "Е. Д. Поливанов");
		setDefaultName("tv", "К. С. Станиславский");
		setDefaultName("vg", "Марио");
		setDefaultName("x", "Эмма Ай");
		setDefaultName("a", "Мокона");
		setDefaultName("aa", "Ракка");
		setDefaultName("azu", "Осака");
		setDefaultName("fi", "Фигурка анонима");
		setDefaultName("jp", "\u540d\u7121\u3057\u3055\u3093");
		setDefaultName("hau", "\u4e09\u56db");
		setDefaultName("ls", "Цукаса");
		setDefaultName("ma", "Иноуэ Орихимэ");
		setDefaultName("me", "Лакс Кляйн");
		setDefaultName("rm", "Суйгинто");
		setDefaultName("sos", "Кёнко");
		setDefaultName("tan", "Уныл-тян");
		setDefaultName("to", "Нитори");
		setDefaultName("vn", "Сэйбер");
		setDefaultName("d", "Мод-тян");
		setBumpLimit(500);
	}

	@Override
	public Board obtainBoardConfiguration(String boardName) {
		Board board = new Board();
		board.allowCatalog = !"d".equals(boardName);
		board.allowArchive = true;
		board.allowPosting = true;
		board.allowDeleting = true;
		return board;
	}

	@Override
	public Posting obtainPostingConfiguration(String boardName, boolean newThread) {
		Posting posting = new Posting();
		posting.allowName = posting.allowTripcode = get(boardName, KEY_NAMES_ENABLED, true);
		posting.allowEmail = get(boardName, KEY_EMAILS_ENABLED, true);
		posting.allowSubject = true;
		posting.attachmentCount = get(boardName, KEY_IMAGES_ENABLED, true) ? 1 : 0;
		posting.attachmentMimeTypes.add("image/*");
		posting.attachmentSpoiler = get(boardName, KEY_IMAGE_SPOILERS_ENABLED, false);
		return posting;
	}

	@Override
	public Deleting obtainDeletingConfiguration(String boardName) {
		Deleting deleting = new Deleting();
		deleting.password = true;
		deleting.multiplePosts = true;
		deleting.optionFilesOnly = true;
		return deleting;
	}

	public void storeNamesEmailsImagesSpoilersEnabled(String boardName,
			boolean namesEnabled, boolean emailsEnabled, boolean imagesEnabled, boolean imageSpoilersEnabled) {
		set(boardName, KEY_NAMES_ENABLED, namesEnabled);
		set(boardName, KEY_EMAILS_ENABLED, emailsEnabled);
		set(boardName, KEY_IMAGES_ENABLED, imagesEnabled);
		set(boardName, KEY_IMAGE_SPOILERS_ENABLED, imageSpoilersEnabled);
	}
}

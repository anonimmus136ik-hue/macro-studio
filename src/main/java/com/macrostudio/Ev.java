package com.macrostudio;

/** One discrete event inside a recorded tick. */
public final class Ev {
	public static final String CMD = "cmd";
	public static final String CHAT = "chat";
	public static final String CLICK = "click";
	public static final String CLOSE = "close";

	public String type;
	public String text;
	public int slot;
	public int button;
	public String click;

	public static Ev cmd(String text) {
		Ev e = new Ev();
		e.type = CMD;
		e.text = text;
		return e;
	}

	public static Ev chat(String text) {
		Ev e = new Ev();
		e.type = CHAT;
		e.text = text;
		return e;
	}

	public static Ev click(int slot, int button, String click) {
		Ev e = new Ev();
		e.type = CLICK;
		e.slot = slot;
		e.button = button;
		e.click = click;
		return e;
	}

	public static Ev close() {
		Ev e = new Ev();
		e.type = CLOSE;
		return e;
	}
}
